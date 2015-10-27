package orca.controllers.xmlrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.pubsub.PublishQueue;
import orca.controllers.xmlrpc.pubsub.SliceState;
import orca.embed.cloudembed.controller.InterCloudHandler;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.beans.UnitMng;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

/**
 * This object stores everything we know about a slice.
 * It also maintains various mappings e.g. slice urn to id and back
 * @author ibaldin
 *
 */
public class XmlrpcControllerSlice implements RequestWorkflow.WorkflowRecoverySetter {
	// no save
	protected SliceOperationLock lock;
	// restored from sm reservation state
	protected SliceStateMachine stateMachine;
	// only needed in createSlice logic and defer thread, not used afterwards, no saving needed
	protected List<TicketReservationMng> computedReservations;
	// restored by constructor
	protected String sliceUrn;
	// restored by constructor
	protected String userDN;
	// restored by constructor
	protected List<Map<String, ?>> users;
	// restored by constructor
	protected RequestWorkflow workflow;
	// restored by constructor
	protected SliceMng slice;
	// restored by constructor
	protected ReservationConverter orc;
	// restored by slice workflow
	protected Date firstDeleteAttempt = null;

	// static maps for mapping between slice names and guids
	// restored from constructor
	protected static Map<String, String> urnToSlice = new HashMap<String, String>();
	protected static Map<String, String> sliceToUrn = new HashMap<String, String>();

	/**
	 * Create a new controller slice for a given SM, based on a given SliceMng object, with name, user DN and users list (SSH credentials)
	 * @param sm - SM
	 * @param s - SliceMng structure from SM
	 * @param name - slice name
	 * @param dn - creator DN/URN
	 * @param u - SSH credentials list for slice users
	 * @param recover - is this slice being recovered or created new
	 */
	public XmlrpcControllerSlice(IOrcaServiceManager sm, SliceMng s, String name, String dn, List<Map<String, ?>> u, Boolean recover) {
		userDN = dn;
		users = u;
		slice = s;
		sliceUrn = name;

		lock = new SliceOperationLock();

		stateMachine = new SliceStateMachine(slice.getSliceID(), recover);

		// initialize the workflow
		try {
			InterCloudHandler h = null;
			
			if (recover) {
				h = new InterCloudHandler(Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "idm-" + s.getSliceID(), recover);
			} else {
				h = new InterCloudHandler(null, 
						Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "idm-" + s.getSliceID());
			}
			workflow = new RequestWorkflow(h);
			
			orc = new ReservationConverter(users, sm, this); 
			orc.logger=NdlCommons.getNdlLogger();
		} catch (IOException ie) {
			throw new RuntimeException("Unable to create TDB IDM database due to I/O error: " + ie);
		} catch (NdlException ne) {
			throw new RuntimeException("Unable to create TDB IDM database due to NDL error: " + ne);
		} catch (ReservationConverter.ReservationConverterException re) {
			throw new RuntimeException("Unable to create ReservationConverter due to error: " + re);
		}

		synchronized (urnToSlice) {
			urnToSlice.put(name, s.getSliceID());
			sliceToUrn.put(s.getSliceID(), name);
		}
	}

	public void lock() throws InterruptedException {
		lock.acquire();
	}

	public void unlock() {
		lock.release();
	}
	
	public boolean locked() {
		return (lock.availablePermits() == 0);
	}

	/**
	 * Set a list of computed reservations for this slice
	 * @param l
	 */
	public void setComputedReservations(List<TicketReservationMng> l) {
		computedReservations = l;
	}
	
	/**
	 * get the computed reservations (may or may not have been submitted to orca yet)
	 * @return
	 */
	public List<TicketReservationMng> getComputedReservations() {
		return computedReservations;
	}
	
	/**
	 * Get the actual reservations
	 * @return
	 */
	public List<ReservationMng> getAllReservations(IOrcaServiceManager sm) {

		if (sm == null)
			return null;
		return sm.getReservations(new SliceID(slice.getSliceID()));
	}
	
	/**
	 * Get reservations in specific states
	 * @param sm
	 * @param state
	 * @return
	 */
	public List<ReservationMng> getReservationsByState(IOrcaServiceManager sm, int ... states) {
		if (sm == null)
			return null;
		List<ReservationMng> res = new ArrayList<ReservationMng>();
		for(int i = 0; i< states.length; i++) {
			res.addAll(sm.getReservations(new SliceID(slice.getSliceID()), states[i]));
		}
		return res;
	}

	/**
	 * Get a reservation units, belonging to this slice based on reservation id (or null)
	 * @param sm
	 * @param res
	 * @return
	 */
	public List<UnitMng> getUnits(IOrcaServiceManager sm, String res) {
		List<ReservationMng> allRes = getAllReservations(sm);
		try {
			if (allRes != null) {
				for(ReservationMng r: allRes) {
					if (r.getReservationID().trim().equals(res.trim())) {
						return sm.getUnits(new ReservationID(res));
					}
				}
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Unable to get units for reservation " + res + " due to " + e);
		}
	}
	
	public RequestWorkflow getWorkflow() {
		return workflow;
	}

	public String getUserDN() {
		return userDN;
	}
	
	/**
	 * Get all SSH logins concatenated as a comma-separated string
	 * @return
	 */
	public String getLoginsAsString() {
		StringBuilder r = new StringBuilder();
		boolean first = true;
    	for(Map<String, ?> e: users) {
    		try {
    			String userName = (String)e.get(ReservationConverter.LOGIN_FIELD);
        		if (!first)
        			r.append(",");
        		
    			r.append(userName);
        		first = false;
    		} catch (ClassCastException cce) {
    			continue;
    		}
    	}
		return r.toString();
	}
	
	public String getSliceUrn() {
		return sliceUrn;
	}

	public String getSliceID() {
		return slice.getSliceID();
	}

	public static String getSliceIDForUrn(String urn) {
		synchronized (urnToSlice) {
			return urnToSlice.get(urn);
		}
	}
	
	public static String getSliceUrnForId(String id) {
		synchronized(sliceToUrn) {
			return sliceToUrn.get(id);
		}
	}
	
	public ReservationConverter getOrc() {
		return orc;
	}

	public void setOrc(ReservationConverter orc) {
		this.orc = orc;
	}

	// free up space from this slice if it is DEAD
	private boolean closeExecuted = false;
	private boolean globalAssignmentCleared = false;
	
	/**
	 * Close all reservations in the sice.
	 * It is expected that the slice lock is held when calling this function.
	 */
	public void close() {
		// global label assignment should be cleared preemptively
		// to maintain consistent view
		if ((!globalAssignmentCleared) && 
				((stateMachine.getState() == SliceStateMachine.SliceState.DEAD) || 
						(stateMachine.getState() == SliceStateMachine.SliceState.CLOSING))){
			workflow.clearGlobalControllerAssignedLabel();
			workflow.clearSharedIPSet();
			globalAssignmentCleared = true;
		} 

		if (closeExecuted)
			return;
		
		closeExecuted = true;
		workflow.close();
		synchronized(sliceToUrn) {
			sliceToUrn.remove(slice.getSliceID());
			urnToSlice.remove(sliceUrn);
		}
		if (computedReservations != null){
			computedReservations.clear();
			computedReservations = null;
		}
	}
	
	public SliceStateMachine getStateMachine() {
		return stateMachine;
	}
	
	//
	// Pub-sub related
	//

	/**
	 * This method is called when deleteSlice is called; 
         * The urn of the deleted slice is pushed to the deleted slices Q 
	 * @param slice_urn
	 * @param logger
	 */
	public void deleteFromPublishQ(Logger logger){
		// check if publish is enabled in xmlrpc.controller.properties
		Properties orcaPubsubProps = new Properties();
		File f = new File(OrcaController.ConfigDirectory + OrcaXmlrpcHandler.PUBSUB_PROPS_FILE_NAME);
		if (f.exists()) {
			try {
				orcaPubsubProps.load(new FileInputStream(f));
				logger.info("Successfully loaded orcapubsub properties");
			} catch (IOException ex) {
				logger.info("Error loading orcapubsub properties file; Can't delete slice from publish Q");
				return;
			}
		} else {
			logger.info("No orcapubsub properties; Can't delete slice from publish Q");
			return;
		}

		if (orcaPubsubProps.isEmpty()) {
			logger.error("Unable to load properties file. Make sure orcapubsub properties is in $ORCA_HOME/config; Can't delete slice from publish Q");
			return;
		}

		String pubManifestEnabled = orcaPubsubProps.getProperty(OrcaXmlrpcHandler.PUBSUB_ENABLED_PROP);
		if (pubManifestEnabled == null){
			logger.info("ORCA.publish.manifest property needs to be set to true for publishing manifests; Can't publish manifest");
			return;
		}

		if(pubManifestEnabled.equalsIgnoreCase("true")){
			//logger.info("Deleting " + sliceUrn + " from PubQ");
                        logger.info("Adding " + sliceUrn + " to DeletedSlicesQ");
			//PublishQueue.getInstance().deleteFromPubQ(sliceUrn);
                        PublishQueue.getInstance().addToDeletedSlicesQ(sliceUrn);
		}
		else {
			logger.info("ORCA.publish.manifest property needs to be set to true; Can't delete slice from publish Q ");
			return;
		}

	}

	/**
	 * Pushes the slice with slice_urn in the queue for publishing manifests
	 * @param slice_urn
	 * @param logger
	 */
	public void publishManifest(Logger logger){
		// if pubsub property is set, push the slice_urn to the publish Q
		Properties orcaPubsubProps = new Properties();
		File f = new File(OrcaController.ConfigDirectory + OrcaXmlrpcHandler.PUBSUB_PROPS_FILE_NAME);
		if (f.exists()) {
			try {
				orcaPubsubProps.load(new FileInputStream(f));
				logger.info("Successfully loaded orcapubsub properties");
			} catch (IOException ex) {
				logger.info("Error loading orcapubsub properties file; Can't publish manifest");
				return;
			}
		} else {
			logger.info("No orcapubsub properties; Can't publish manifest");
			return;
		}

		if (orcaPubsubProps.isEmpty()) {
			logger.error("Unable to load properties file. Make sure orcapubsub properties is in $ORCA_HOME/config; Can't publish manifest");
			return;
		}

		String pubManifestEnabled = orcaPubsubProps.getProperty(OrcaXmlrpcHandler.PUBSUB_ENABLED_PROP);
		if(pubManifestEnabled == null){
			logger.info("ORCA.publish.manifest property needs to be set to true for publishing manifests; Can't publish manifest");
			return;
		}

		if(pubManifestEnabled.equalsIgnoreCase("true")){
			Date start = workflow.getTerm().getStart(), end = workflow.getTerm().getEnd();
			logger.info("Adding " + sliceUrn + " to newSlicesQ");
			PublishQueue.getInstance().addToNewSlicesQ(new SliceState(sliceUrn, slice.getSliceID(),
					SliceState.PubSubState.SUBMITTED, start, end, 0, userDN));
		}
		else {
			logger.info("ORCA.publish.manifest property needs to be set to true for publishing manifests; Can't publish manifest");
			return;
		}
	}

	//
	// Helper functions to evaluate state
	//
	public boolean isStableOK() throws SliceStateMachine.SliceTransitionException {

		SliceStateMachine.SliceState ss = reevaluate();
		if (ss == SliceStateMachine.SliceState.STABLE_OK) 
			return true;
		return false;
	}

	public boolean isStableError() throws SliceStateMachine.SliceTransitionException {

		SliceStateMachine.SliceState ss = reevaluate();
		if (ss == SliceStateMachine.SliceState.STABLE_ERROR) 
			return true;
		return false;

	}
	
	public boolean isDeadOrClosing() throws SliceStateMachine.SliceTransitionException {
		SliceStateMachine.SliceState ss = reevaluate();
		if ((ss == SliceStateMachine.SliceState.DEAD) || (ss == SliceStateMachine.SliceState.CLOSING)) 
			return true;
		return false;
	}
	
	public boolean isDead() throws SliceStateMachine.SliceTransitionException {
		SliceStateMachine.SliceState ss = reevaluate();
		if (ss == SliceStateMachine.SliceState.DEAD)
			return true;
		return false;
	}

	/**
	 * Does the slice consist of only failed reservations
	 * @return
	 * @throws SliceStateMachine.SliceTransitionException
	 */
	public boolean allFailed() throws SliceStateMachine.SliceTransitionException {
		return stateMachine.allFailed();
	}
	
	/**
	 * Mark a delete attempt. Only the first attempt is recorded.
	 */
	public void markDeleteAttempt() {
		if (firstDeleteAttempt == null)
			firstDeleteAttempt = new Date();
	}
	
	/**
	 * When was the first attempt to delete/gc this slice (null possible)
	 * @return
	 */
	public Date getDeleteAttempt() {
		return firstDeleteAttempt;
	}
	
	/**
	 * Re-evaluate the state of the slice
	 * @throws SliceStateMachine.SliceTransitionException
	 */
	private SliceStateMachine.SliceState reevaluate() throws SliceStateMachine.SliceTransitionException {
		return stateMachine.transitionSlice(SliceStateMachine.SliceCommand.REEVALUATE);
	}
	
	/**
	 * Restore inner workings of the slice (workflow, reservation converter etc)
	 */
	public void recover(Logger logger, HashMap<String, BitSet> globalAssignedLabels, HashMap <String,LinkedList<String>> shared_IP_set) {
		logger.info("Restoring inner fields of slice " + slice.getSliceID() + "/" + sliceUrn );
		try {
			SliceStateMachine.SliceState ss = reevaluate();
			logger.info("Slice " + slice.getSliceID() + "/" + sliceUrn  + " is in state " + ss);
		} catch(SliceStateMachine.SliceTransitionException ssme) {
			logger.error("Unable to evaluate the state of the slice");
		}
		workflow.setShared_IP_set(shared_IP_set);
		workflow.recover(logger, globalAssignedLabels, this);
		
		orc.recover(workflow);
		
		// republish the manifest
		publishManifest(logger);
	}
	
	@Override
	public String getId() {
		return slice.getSliceID();
	}
	
	/**
	 * Use recovered reservation info to get restorable information into the recovered slice
	 * @param r
	 * @param logger
	 */
	public void addRecoveredReservation(ReservationMng r, Logger logger) {
		logger.info("Slice " + slice.getSliceID() + "/" + sliceUrn + " seeking additional recovery information from " + r.getReservationID());
		String label = null, domain = null;
		PropertiesMng confProps = r.getConfigurationProperties();

		if (confProps == null)
			return;
		
		// get label and domain for controller assigned labels
		for(PropertyMng p: confProps.getProperty()) {
			if (ReservationConverter.PropertyConfigUnitTag.equals(p.getName())) {
				label = p.getValue().trim();
			} else {
				if (UnitProperties.UnitDomain.equals(p.getName())) {
					domain = p.getValue().trim();
				}
			}
			if ((label != null) && (domain != null)) {
				try {
					int iLabel = Integer.parseInt(label);
					workflow.setControllerLabel(domain, iLabel);
					domain = null;
					label = null;
				} catch (NumberFormatException nfe) {
					logger.error("Encountered non-numeric label " + label + " for domain " + domain + " unable to restore, skipping");
					label = null;
					domain = null;
				}
				break;
			}
		}
	}
}