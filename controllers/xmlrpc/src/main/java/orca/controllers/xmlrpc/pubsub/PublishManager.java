/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc.pubsub;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import orca.controllers.OrcaController;
import orca.controllers.OrcaControllerException;
import orca.controllers.xmlrpc.SliceStateMachine.SliceTransitionException;
import orca.controllers.xmlrpc.XmlrpcControllerSlice;
import orca.controllers.xmlrpc.XmlrpcHandlerHelper;
import orca.controllers.xmlrpc.XmlrpcOrcaState;
import orca.manage.IOrcaServiceManager;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

/**
 *
 * @author anirban
 */
public class PublishManager {

	private static final int PUBLISHER_PERIOD = 30;

	protected XmlrpcOrcaState instance = null;
	protected static Logger logger = OrcaController.getLogger(PublishManager.class.getSimpleName());
	protected String actor_guid = null;
	protected String actor_name = null;
	private Set<SliceState> sliceList = new HashSet<SliceState>(); // list of active slices that need to be published (aka sliceList for blowhole consumption)
	
	// scheduler that creates daemon threads
	protected static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
		   public Thread newThread(Runnable runnable) {
			      Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			      thread.setDaemon(true);
			      return thread;
			   }
		});
	protected static final List<ScheduledFuture<?>> futures = new ArrayList<>();
	protected static boolean noStart = false;

	public PublishManager(){

		initialize();
		//expunge sliceList pubsub node on startup
		doExpungeSliceList();
		
		if (noStart)
			return;
		synchronized(futures) {
			futures.add(scheduler.scheduleAtFixedRate(new PublisherTask(), PUBLISHER_PERIOD, PUBLISHER_PERIOD, TimeUnit.SECONDS));
		}
	}

	/**
	 * Since we use daemon threads, this isn't needed /ib
	 */
	public static void allStop() {
		logger.info("Shutting down pubsub thread");
		noStart = true;
		synchronized(futures) {
			for(ScheduledFuture<?> f: futures)
				f.cancel(false);
		}
	}

	private void initialize() {
		// Getting actor name and actor guid
		// in 4.0 this pubsub is per one SM, so
		// we can cache the name and guid of the SM
		instance = XmlrpcOrcaState.getInstance();
		IOrcaServiceManager sm = null;

		// NOTE: this loop implicitly makes sure that the SM is up
		// NOTE: This loop blocks the controller thread because the controller calls new PublishManager(), which in turn calls initialize()
		// This is a way to make sure that the controller deosn't return until SM is up, but not necessarily the best way.

		while (actor_guid == null || actor_name == null){
			try {
				if (instance == null) 
					throw new Exception("unable to get XMLRPCOrcaState instance");

				sm = instance.getSM();

				if (sm == null)
					throw new Exception("SM is null");
				
				actor_guid = sm.getGuid().toString();
				actor_name = sm.getName();
				logger.info("SM actor name: " + actor_name + " | SM actor guid: " + actor_guid);
			} catch (Exception e) {
				logger.error("initialize(): unable to get a connection to SM due to: " + e + ", waiting 1 sec.");
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
					;
				}
			} finally {
				if (sm != null)
					instance.returnSM(sm);
			}
		}
	}

	private void doExpungeSliceList() {

		ManifestPublisher mPublisher = new ManifestPublisher(logger);
		logger.info("Deleting node : " + actor_name + "---" + actor_guid + "/" + "sliceList");

		try {
			mPublisher.createAccountAndDisconnect();
			mPublisher.prepareXMPP();
			mPublisher.expungeNode(actor_name + "---" + actor_guid + "/" + "sliceList");
			mPublisher.disconnectXMPP();
		} catch (Exception e){
			logger.error("Exception in PublishManager:doExpungeSliceList() : " + e );
		}
	}

	class PublisherTask implements Runnable {

		public void run() {

			ManifestPublisher mPublisher = new ManifestPublisher(logger);
			try{

				// Find if Orca container is still running
				// If it is not running any more, stop this timer thread

				//                OrcaState state = OrcaState.getInstance();
				//                if(!state.checkOrcaRunning()){
				//                    logger.info("Orca container no longer active");
				//                    allStop();
				//                    return;
				//                }
				//                else {
				//                    logger.info("Orca container is active.....");
				//                }

				logger.info("Orca container is active..... Publishing thread:START");

				PublishQueue pubQ = PublishQueue.getInstance();

				// notice this reaches deep into the pubQ object to get slice list and allows
				// modifying individual slice states in it directly (as done below) /ib
				Set<SliceState> currSliceStateQ = pubQ.getCurrentQ();
				ArrayList<SliceState> toRemoveSliceStateQ = new ArrayList<SliceState>();

				// Process new slices Q
				pubQ.drainNew();

				// Process deleted slices Q
				pubQ.drainDeleted();
				
				// process modified slices
				pubQ.drainModified();

				// initialize XMPP. This is inefficient, but helps when XMPP server gets wiped and restarted
				mPublisher.createAccountAndDisconnect();
				mPublisher.prepareXMPP();
				
				synchronized(pubQ){

					if ((currSliceStateQ == null) || (currSliceStateQ.size() <= 0)){
						return; // nothing to do if there are no slices to watch
					}

					boolean publishSliceList = false;
					Iterator<SliceState> it = currSliceStateQ.iterator();
					while(it.hasNext()){ // go through all the slicestates

						SliceState currSliceState = (SliceState) it.next();
						String currSliceUrn = currSliceState.getSlice_urn();
						String currSliceID = currSliceState.getSlice_ID();
						int currWaitTime = currSliceState.getWaitTime();

						if (currSliceState.getState() == SliceState.PubSubState.SUBMITTED){ // State 0, just submitted
							logger.info("Slice: " + currSliceUrn + "/" + currSliceID + " in state SUBMITTED, going to state INPROGRESS, publishing manifest");
							doPublish(mPublisher, currSliceUrn);
							currSliceState.setState(SliceState.PubSubState.INPROGRESS);
							// add currSliceState to sliceList
							addToSliceList(currSliceState);
							// pub sliceList
							publishSliceList = true;
						}
						else if(currSliceState.getState() == SliceState.PubSubState.INPROGRESS){ // State 1, in progress
							logger.info("Slice: " + currSliceUrn + " in state INPROGRESS");
							boolean checkDone = checkDone(currSliceUrn);
							boolean checkClosed = checkClosed(currSliceUrn);
							if(checkDone == true){
								logger.info("Publishing Final manifest for slice: " + currSliceUrn + ", slice going to DONEACTIVE");
								doPublish(mPublisher, currSliceUrn);
								currSliceState.setState(SliceState.PubSubState.DONEACTIVE);
								currSliceState.setWaitTime(2*60*24); // wait for one day, since publish thread executed every 30 seconds
							}
							else if(checkClosed == true){
								logger.info("Slice: " + currSliceUrn + " is cloded, final manifest is not published, going to DONECLOSED");
								currSliceState.setState(SliceState.PubSubState.DONECLOSED);
								Date now = new Date();
								currSliceState.setEndTime(now);
								currSliceState.setWaitTime(2*60*24); // wait for one day, since publish thread executed every 30 seconds
								// modify sliceList for currSliceState
								setPropsForSliceInSliceList(currSliceUrn, currSliceState.getState(), currSliceState.getEndTime(), currSliceState.getWaitTime());
								// publish sliceList
								publishSliceList = true;
							}
							else{
								logger.info("Publishing Intermediate manifest for slice: " + currSliceUrn);
								doPublish(mPublisher, currSliceUrn);
								currSliceState.setState(SliceState.PubSubState.WAITINPROGRESS);
								currSliceState.setWaitTime(2); // wait for one minute, since the Publish thread executes every 30 seconds
								logger.info("Slice: " + currSliceUrn + " going to state WAITINPROGRESS");
							}
						}
						else if(currSliceState.getState() == SliceState.PubSubState.WAITINPROGRESS){ // state 2, wait while inprogress
							logger.info("Slice: " + currSliceUrn + " in state WAITINPROGRESS");
							if(currWaitTime > 0){ // Still need to wait
								logger.info("Slice: " + currSliceUrn + " needs to wait longer in WAITINPROGRESS");
								currSliceState.setWaitTime(currWaitTime - 1);
								currSliceState.setState(SliceState.PubSubState.WAITINPROGRESS);
							}
							else{ // wait is over
								currSliceState.setState(SliceState.PubSubState.INPROGRESS);
								logger.info("Slice: " + currSliceUrn + " going to state INPROGRESS");
							}
							// If slice is pending and never goes to active or closed state (failed slices ??)
						}
						else if(currSliceState.getState() == SliceState.PubSubState.DONEACTIVE){ 
							logger.info("Slice: " + currSliceUrn + " in state DONEACTIVE");
							// check if leases expired; if so go to DONECLOSED
							boolean checkClosed = checkClosed(currSliceUrn);
							if(checkClosed == true){
								currSliceState.setState(SliceState.PubSubState.DONECLOSED);
								Date now = new Date();
								currSliceState.setEndTime(now);
								currSliceState.setWaitTime(2*60*24);
								logger.info("Slice: " + currSliceUrn + " going to state in DONECLOSED");
								// modify sliceList for currSliceState
								setPropsForSliceInSliceList(currSliceUrn, currSliceState.getState(), currSliceState.getEndTime(), currSliceState.getWaitTime());
								// publish sliceList
								publishSliceList = true;
							}
							else {
								if(currWaitTime > 0){
									// Still need to wait in DONEACTIVE state
									logger.info("Slice: " + currSliceUrn + " needs to wait longer in DONEACTIVE");
									currSliceState.setWaitTime(currWaitTime - 1);
									currSliceState.setState(SliceState.PubSubState.DONEACTIVE);
								}
								else{ // wait is over
									currSliceState.setState(SliceState.PubSubState.EXPUNGE);
									logger.info("Slice: " + currSliceUrn + " going to state EXPUNGE");
								}
							}
						}
						else if(currSliceState.getState() == SliceState.PubSubState.DONECLOSED){ 
							logger.info("Slice: " + currSliceUrn + " in state DONECLOSED");
							if(currWaitTime > 0){
								// Still need to wait in DONECLOSED state
								logger.info("Slice: " + currSliceUrn + " needs to wait longer in DONECLOSED");
								currSliceState.setWaitTime(currWaitTime - 1);
								currSliceState.setState(SliceState.PubSubState.DONECLOSED);
							}
							else{ // wait is over
								currSliceState.setState(SliceState.PubSubState.EXPUNGE);
								logger.info("Slice: " + currSliceUrn + " going to state EXPUNGE");
							}
						}
						else if(currSliceState.getState() == SliceState.PubSubState.DELETED) {
							logger.info("Slice: " + currSliceUrn + " in state DELETED");
							Date now = new Date();
							currSliceState.setEndTime(now);
							currSliceState.setWaitTime(2*60*24);
							currSliceState.setState(SliceState.PubSubState.DONECLOSED);
							logger.info("Slice: " + currSliceUrn + " going to state DONECLOSED");
							// modify sliceList entry for currSliceState
							setPropsForSliceInSliceList(currSliceUrn, currSliceState.getState(), currSliceState.getEndTime(), currSliceState.getWaitTime());
							// publish sliceList
							publishSliceList = true;
						}
						else if(currSliceState.getState() == SliceState.PubSubState.EXPUNGE){ // end of lifecycle, delete xmpp pubsub node
							logger.info("Slice: " + currSliceUrn + " in state EXPUNGE");
							doExpunge(mPublisher, currSliceUrn, currSliceState.getOrcaSliceID(), "manifest"); // delete the manifest
							toRemoveSliceStateQ.add(currSliceState);
							// remove toRemoveSliceState entry from sliceList
							deleteFromSliceList(currSliceUrn);
							// publish sliceList
							publishSliceList = true;
						}
						else {
							logger.info("PublishManager: slice state in unknown");
						}

					}// end while
					
					if (publishSliceList)
						doPublishSliceList(mPublisher);

					if(!toRemoveSliceStateQ.isEmpty()){
						for (SliceState s : toRemoveSliceStateQ){
							logger.info("Removing " + s.getSlice_urn() + " from the PubQ after end of manifest lifecycle");
							currSliceStateQ.remove(s);
						}
					}
				} // end synchronized

			} catch(Exception e){ // this is to catch all exceptions in the run() method for the timer thread; prevents timer thread from getting killed by exceptions
				logger.error("Exception occured during Timer thread execution for handling PubSub: Stack trace follows... " , e);
				e.printStackTrace();
			} finally {
				try {
					mPublisher.disconnectXMPP();
				} catch (Exception ex) {
					logger.error("Unable disconnect from XMPP: " + ex);
				}
			}
		}


		private void doPublish(ManifestPublisher mPublisher, String slice_urn){

			String manifest = null;
			try {
				manifest = XmlrpcHandlerHelper.getSliceManifest(instance, slice_urn, logger);
			} catch (OrcaControllerException oce) {
				logger.error("doPublish(): unable to get slice manifest for " + slice_urn + ": " + oce);
				return;
			}
			String orcaSliceID = XmlrpcControllerSlice.getSliceIDForUrn(slice_urn);

			logger.info("Publishing manifest for : " + actor_name + "---" + actor_guid + " | " + slice_urn + "---" + orcaSliceID);
			//logger.info("ORCA Manifest:" + manifest);

			try {
				mPublisher.publishManifest(actor_name + "---" + actor_guid, slice_urn + "---" + orcaSliceID, manifest);
			} catch( Exception e){
				logger.error("Exception in PublishManager:doPublish() : " + e );
			}
		}

		private void doPublishSliceList(ManifestPublisher mPublisher){

			String sliceListString = buildSliceListString();
			if(sliceListString == null){
				logger.error("doPublishSliceList(): Null sliceList; Can't publish sliceList");
				return;
			}
			if(sliceListString.equalsIgnoreCase("")){
				logger.info("doPublishSliceList(): sliceList is empty");
			}

			logger.info("doPublishSliceList(): Publishing sliceList for : " + actor_name + "---" + actor_guid);
			logger.info("doPublishSliceList(): Current sliceList:" + sliceListString);

			try {
				mPublisher.publishSliceList(actor_name + "---" + actor_guid, sliceListString);
			} catch( Exception e){
				logger.error("Exception in PublishManager:doPublishSliceList() : " + e );
			}

		}

		private String buildSliceListString(){

			PublishQueue pubQ = PublishQueue.getInstance();
			String sliceListString = null;
			Set<SliceState> currSliceStateList = getCurrentSliceList();
			
			synchronized(pubQ){

				if(currSliceStateList == null){
					logger.error("buildSliceListString(): slicestatelist = null");
					return null; // can't have a null sliceStateList
				}
				if(currSliceStateList.size() <= 0){ // nothing in the queue
					logger.info("buildSliceListString(): slicestatelist has no elements");
					return ""; // return an empty string
				}

				StringBuilder sliceListBuilder = new StringBuilder();
				Iterator<SliceState> it = currSliceStateList.iterator();
				while(it.hasNext()){ // go through all the slicestates

					SliceState currSliceState = (SliceState) it.next();
					String slice_urn = currSliceState.getSlice_urn();
					String orcaSliceIDString = currSliceState.getOrcaSliceID().toString();
					String slice_owner = currSliceState.getSliceOwner();
					Date startTime = currSliceState.getStartTime();
					String startTimeString = startTime.toString();
					Date endTime = currSliceState.getEndTime();
					String endTimeString = null;
					if(endTime == null){
						endTimeString = "Not Applicable";
					}
					else{
						endTimeString = endTime.toString();
					}

					sliceListBuilder.append(slice_urn);
					sliceListBuilder.append(" / ");
					sliceListBuilder.append(orcaSliceIDString);
					sliceListBuilder.append(" / ");
					sliceListBuilder.append(slice_owner);
					sliceListBuilder.append(" / ");
					sliceListBuilder.append(startTimeString);
					sliceListBuilder.append(" / ");
					sliceListBuilder.append(endTimeString);
					sliceListBuilder.append("\n");
				}

				sliceListString = sliceListBuilder.toString();
			}

			if (sliceListString != null) {
				logger.debug("buildSliceListString(): sliceListString[first 100 chars] = " + sliceListString.substring(1, 100));
			}
			else {
				logger.debug("buildSliceListString(): Null sliceList");
			}

			return sliceListString;

		}

		private void doExpunge(ManifestPublisher mPublisher, String slice_urn, String orcaSliceID, String suffix){

			logger.info("Deleting node : " + actor_name + "---" + actor_guid + " / " + slice_urn + "---" + orcaSliceID.toString());

			try {
				mPublisher.expungeNode(actor_name + "---" + actor_guid, slice_urn + "---" + orcaSliceID.toString(), suffix);
			}
			catch (Exception e){
				logger.error("Exception in PublishManager:doExpunge() : " + e );
			}

		}

		private boolean checkDone(String slice_urn){

			XmlrpcControllerSlice xcs = instance.getSlice(slice_urn);

			if(xcs == null){
				logger.info("checkDone(): slice doesn't exist");
				return false;
			}

			boolean status = false;

			try {
				status = ( xcs.isStable() );  // if (all active) or (all either active or failed)
			} catch (SliceTransitionException ex) {
				logger.error("Exception while checking status of slice in ckeckDone() " + ex);
			} catch (Exception e){
				logger.error("Exception occured in checkDone()" + e);
				return false;
			}


			return status;
		}

		private boolean checkClosed(String slice_urn){

			XmlrpcControllerSlice xcs = instance.getSlice(slice_urn);

			if(xcs == null){ // if the xmlrpccontrollerslice is gone, the reservations must have closed
				logger.info("checkClosed(): slice doesn't exist");
				return true;
			} 

			boolean status = false;

			try {
				status = ( xcs.isDeadOrClosing() );  // if (all closed) or (all either closing or closed)
			} catch (SliceTransitionException ex) {
				logger.error("Exception while checking status of slice in ckeckClosed() " + ex);
			} catch (Exception e){
				logger.error("Exception occured in checkClosed()" + e);
				return false;
			}

			return status;
		}


		/*
        private boolean checkPubSubStateFileContainsZero()throws IOException{

            boolean checkPubSubStateFileContainsZero = false;
            BufferedReader reader = new BufferedReader(new FileReader(pubSubStateFileName));
            String line  = null;
            StringBuilder stringBuilder = new StringBuilder();
            line = reader.readLine(); // just read the first line
            if(line != null){
                    stringBuilder.append( line );
            }
            if(stringBuilder.toString().equalsIgnoreCase("0")){
                checkPubSubStateFileContainsZero = true;
            }

            return checkPubSubStateFileContainsZero;
        }
		 */

	}
	
	
	/**
	 * Manage slicelist functions 
	 */
	
    public Set<SliceState> getCurrentSliceList(){
    	return sliceList;
    }

    public void addToSliceList(SliceState slice){
    	sliceList.add(slice);
    }

    public void deleteFromSliceList(String slice_urn){
    	SliceState toRemoveSliceState = null;
    	if(sliceList != null){
    		Iterator<SliceState> it = sliceList.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceUrn = currSliceState.getSlice_urn();
    			if(currSliceUrn.equalsIgnoreCase(slice_urn)){
    				toRemoveSliceState = currSliceState;
    			}
    		}
    		if(toRemoveSliceState != null){
    			try{
    				Globals.Log.info("PublishQueue: Removing " + toRemoveSliceState.getSlice_urn() + " from sliceList");
    				sliceList.remove(toRemoveSliceState);
    			}
    			catch(Exception e){
    				Globals.Log.error("PublishQueue: Exception while deleting entry from Slice List : " + e);
    			}
    		}
    	}
    }

    public void setPropsForSliceInSliceList(String slice_urn, SliceState.PubSubState newState, Date newEndTime, int newWaitTime){
    	if(sliceList != null){
    		Iterator<SliceState> it = sliceList.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceUrn = currSliceState.getSlice_urn();
    			if(currSliceUrn.equalsIgnoreCase(slice_urn)){
    				currSliceState.setEndTime(newEndTime);
    				currSliceState.setState(newState);
    				currSliceState.setWaitTime(newWaitTime);
    			}
    		}
    	}
    }



}
