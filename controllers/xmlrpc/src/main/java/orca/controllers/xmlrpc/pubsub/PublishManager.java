/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc.pubsub;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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

import org.apache.log4j.Logger;

/**
 *
 * @author anirban
 */
public class PublishManager {

	private static final int PUBLISHER_PERIOD = 30;

	protected XmlrpcOrcaState instance = null;
	protected static Logger logger = OrcaController.getLogger(PublishManager.class.getName());
	protected String actor_guid = null;
	protected String actor_name = null;
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
				sm = instance.getSM();
				if (sm == null) {
					logger.error("initialize(): SM instance is null.");
				}
				actor_guid = sm.getGuid().toString();
				actor_name = sm.getName();
				logger.info("SM actor name: " + actor_name + " | SM actor guid: " + actor_guid);
			} catch (Exception e) {
				logger.error("initialize(): unable to get a connection to SM due to: " + e);
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
			mPublisher.expungeNode(actor_name + "---" + actor_guid + "/" + "sliceList");
		} catch (Exception e){
			logger.error("Exception in PublishManager:doExpungeSliceList() : " + e );
		}
	}

	class PublisherTask implements Runnable {

		public void run() {

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

				ArrayList<SliceState> currSliceStateQ = pubQ.getCurrentQ();
				ArrayList<SliceState> toRemoveSliceStateQ = new ArrayList<SliceState>();

				// Process new slices Q
				pubQ.drainNew();

				// Process deleted slices Q
				pubQ.drainDeleted();

				synchronized(pubQ){

					if(currSliceStateQ == null){
						return; // nothing to do if there are no slices to watch
					}
					if(currSliceStateQ.size() <= 0){ // nothing in the queue
						return;
					}

					Iterator<SliceState> it = currSliceStateQ.iterator();
					while(it.hasNext()){ // go through all the slicestates

						SliceState currSliceState = (SliceState) it.next();
						String currSliceUrn = currSliceState.getSlice_urn();
						int currWaitTime = currSliceState.getWaitTime();

						if(currSliceState.getState() == SliceState.PubSubState.SUBMITTED){ // State 0, just submitted
							logger.info("Slice: " + currSliceUrn + " in state SUBMITTED");
							logger.info("Publishing Initial manifest for slice: " + currSliceUrn);
							doPublish(currSliceUrn);
							currSliceState.setState(SliceState.PubSubState.INPROGRESS);
							logger.info("Slice: " + currSliceUrn + " going to state INPROGRESS");
							// add currSliceState to sliceList
							pubQ.addToSliceList(currSliceState);
							// pub sliceList
							doPublishSliceList();
						}
						else if(currSliceState.getState() == SliceState.PubSubState.INPROGRESS){ // State 1, in progress
							logger.info("Slice: " + currSliceUrn + " in state INPROGRESS");
							boolean checkDone = checkDone(currSliceUrn);
							boolean checkClosed = checkClosed(currSliceUrn);
							if(checkDone == true){
								logger.info("Publishing Final manifest for slice: " + currSliceUrn);
								doPublish(currSliceUrn);
								currSliceState.setState(SliceState.PubSubState.DONEACTIVE);
								currSliceState.setWaitTime(2*60*24); // wait for one day, since publish thread executed every 30 seconds
								logger.info("Slice: " + currSliceUrn + " going to state DONEACTIVE");
							}
							else if(checkClosed == true){
								logger.info("Slice: " + currSliceUrn + " went to Closed state");
								logger.info("Final manifest for slice: " + currSliceUrn + " not published ");
								currSliceState.setState(SliceState.PubSubState.DONECLOSED);
								Date now = new Date();
								currSliceState.setEndTime(now);
								currSliceState.setWaitTime(2*60*24); // wait for one day, since publish thread executed every 30 seconds
								logger.info("Slice: " + currSliceUrn + " going to state DONECLOSED");
								// modify sliceList for currSliceState
								pubQ.setPropsForSliceInSliceList(currSliceUrn, currSliceState.getState(), currSliceState.getEndTime(), currSliceState.getWaitTime());
								// publish sliceList
								doPublishSliceList();
							}
							else{
								logger.info("Publishing Intermediate manifest for slice: " + currSliceUrn);
								doPublish(currSliceUrn);
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
								pubQ.setPropsForSliceInSliceList(currSliceUrn, currSliceState.getState(), currSliceState.getEndTime(), currSliceState.getWaitTime());
								// publish sliceList
								doPublishSliceList();
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
							pubQ.setPropsForSliceInSliceList(currSliceUrn, currSliceState.getState(), currSliceState.getEndTime(), currSliceState.getWaitTime());
							// publish sliceList
							doPublishSliceList();
						}
						else if(currSliceState.getState() == SliceState.PubSubState.EXPUNGE){ // end of lifecycle, delete xmpp pubsub node
							logger.info("Slice: " + currSliceUrn + " in state EXPUNGE");
							doExpunge(currSliceUrn, currSliceState.getOrcaSliceID(), "manifest"); // delete the manifest
							toRemoveSliceStateQ.add(currSliceState);
							// remove toRemoveSliceState entry from sliceList
							pubQ.deleteFromSliceList(currSliceUrn);
							// publish sliceList
							doPublishSliceList();
						}
						else {
							logger.info("PublishManager: slice state in unknown");
						}

					}// end while

					if(!toRemoveSliceStateQ.isEmpty()){
						for (SliceState s : toRemoveSliceStateQ){
							logger.info("Removing " + s.getSlice_urn() + " from the PubQ after end of manifest lifecycle");
							currSliceStateQ.remove(s);
						}
					}
				} // end synchronized

			} catch(Exception e){ // this is to catch all exceptions in the run() method for the timer thread; prevents timer thread from getting killed by exceptions
				logger.error("Exception occured during Timer thread execution for handling PubSub: Stack trace follows... " , e);
				logger.error("Continuing execution beyond Exception");
				System.out.println("There was an exception during Timer thread execution for handling PubSub");
			}
		}


		private void doPublish(String slice_urn){

			String manifest = null;
			try {
				manifest = XmlrpcHandlerHelper.getSliceManifest(instance, slice_urn, logger);
			} catch (OrcaControllerException oce) {
				logger.error("doPublish(): unable to get slice manifest for " + slice_urn + ": " + oce);
				return;
			}
			String orcaSliceID = XmlrpcControllerSlice.getSliceIDForUrn(slice_urn);

			ManifestPublisher mPublisher = new ManifestPublisher(logger);
			logger.info("Publishing manifest for : " + actor_name + "---" + actor_guid + " | " + slice_urn + "---" + orcaSliceID);
			//logger.info("ORCA Manifest:" + manifest);

			try {
				mPublisher.publishManifest(actor_name + "---" + actor_guid, slice_urn + "---" + orcaSliceID, manifest);
			} catch( Exception e){
				logger.error("Exception in PublishManager:doPublish() : " + e );
			}
		}

		private void doPublishSliceList(){

			String sliceListString = buildSliceListString();
			if(sliceListString == null){
				logger.error("doPublishSliceList(): Null sliceList; Can't publish sliceList");
				return;
			}
			if(sliceListString.equalsIgnoreCase("")){
				logger.info("doPublishSliceList(): sliceList is empty");
			}

			ManifestPublisher mPublisher = new ManifestPublisher(logger);
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
			ArrayList<SliceState> currSliceStateList = pubQ.getCurrentSliceList();
			
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
				logger.info("buildSliceListString(): sliceListString = " + sliceListString);
			}
			else {
				logger.info("buildSliceListString(): Null sliceList");
			}

			return sliceListString;

		}

		private void doExpunge(String slice_urn, String orcaSliceID, String suffix){

			ManifestPublisher mPublisher = new ManifestPublisher(logger);
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
				status = ( xcs.isStableOK() || xcs.isStableError() );  // if (all active) or (all either active or failed)
			} catch (SliceTransitionException ex) {
				logger.error("Exception while checking status of slice in ckeckDone() " + ex);
			} catch (Exception e){
				logger.error("Exception occured in checkDone()" + e);
				return false;
			}


			return status;

			/*

            boolean isDone = false;

            instance = XmlrpcOrcaState.getInstance();
            IServiceManager sm = instance.getSM();
            Logger logger = sm.getLogger();

            IReservation [] allRes = null;
            SliceID orcaSliceID = XmlrpcControllerSlice.getSliceIDForUrn(slice_urn);
            allRes = (IReservation[]) sm.getReservations(orcaSliceID);

            boolean allReady = true;
            boolean allFailed = true;

            for (int j=0; j<allRes.length; j++ ){
                if(!(allRes[j].getReservationState().getStateName().equalsIgnoreCase("Active"))){
                    allReady = false;
                }
                if(!(allRes[j].getReservationState().getStateName().equalsIgnoreCase("Failed"))){
                    allFailed = false;
                }
            }

            isDone = (allReady || allFailed) ;

            return isDone;

			 */

		}

		private boolean checkClosed(String slice_urn){

			XmlrpcControllerSlice xcs = instance.getSlice(slice_urn);

			if(xcs == null){ // if the xmlrpccontrollerslice is gone, the reservations must have closed
				logger.info("checkClosed(): slice doesn't exist");
				return true;
			} 

			boolean status = false;

			try {
				status = ( xcs.isDead() || xcs.isDeadOrClosing() );  // if (all closed) or (all either closing or closed)
			} catch (SliceTransitionException ex) {
				logger.error("Exception while checking status of slice in ckeckClosed() " + ex);
			} catch (Exception e){
				logger.error("Exception occured in checkClosed()" + e);
				return false;
			}

			return status;

			/*
            instance = XmlrpcOrcaState.getInstance();
            IServiceManager sm = instance.getSM();
            Logger logger = sm.getLogger();

            IReservation [] allRes = null;
            SliceID orcaSliceID = XmlrpcControllerSlice.getSliceIDForUrn(slice_urn);
            allRes = (IReservation[]) sm.getReservations(orcaSliceID);

            boolean allClosed = true;

            for (int j=0; j<allRes.length; j++ ){
                if(!(allRes[j].getReservationState().getStateName().equalsIgnoreCase("Closed"))){
                    allClosed = false;
                }
            }

            return allClosed ;
			 */


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


}
