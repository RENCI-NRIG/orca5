package orca.controllers.xmlrpc;


import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.pubsub.PublishManager;
import orca.controllers.xmlrpc.statuswatch.ReservationStatusUpdateThread;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.UnitMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

public final class XmlrpcOrcaState implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String XMLRPC_USER_DN = "xmlrpc.user.dn";
	
	// this needs to restore from reservations
	private HashMap<String,BitSet> controllerAssignedLabel = new HashMap<String,BitSet>();

	protected HashMap <String,LinkedList<String>> shared_IP_set = new HashMap <String,LinkedList<String>>();
	
	// nothing to restore/save
	private XmlRpcController controller;

	// restored from reservations
	private HashSet <String> usedMac = new HashSet<String>();
	
	// restored from reservations
	private HashMap<String, XmlrpcControllerSlice> slices = new HashMap<String, XmlrpcControllerSlice>();

	private Logger logger = OrcaController.getLogger(this.getClass().getSimpleName());
	
	// use output compression
	private static boolean compressOutput = true;

	private static final XmlrpcOrcaState fINSTANCE =  new XmlrpcOrcaState();

	// patterns of properties to match for restoration
	private static final Pattern macNamePattern = Pattern.compile(UnitProperties.UnitEthPrefix + "[\\d]+" + UnitProperties.UnitEthMacSuffix);
	
	// thread for deferred slices due to interdomain complexity
	protected static SliceDeferThread sdt = null; 
	protected static Thread sdtThread = null;
	
	// thread for processing status updates (modify and state)
	protected static ReservationStatusUpdateThread sut = null;
	protected static ScheduledFuture<?> sutFuture = null;
	
	// thread for syncing tags from existing reservations
	protected static LabelSyncThread lst = null;
	protected static ScheduledFuture<?> lstFuture = null;
	
	// PublishManager
	protected static PublishManager pubManager = null;

	// start the threads (called from the controller startup code)
	public static void startThreads () {
		// slice defer thread
		Globals.Log.info("Starting SliceDeferThread");
		sdt = new SliceDeferThread();
		sdtThread = new Thread(sdt);
		sdtThread.setDaemon(true);
		sdtThread.setName("SliceDeferThread");
		sdtThread.start();
		

		// create service for various periodic threads that are daemon threads
		// that way we don't have to kill them on exit
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
			   public Thread newThread(Runnable runnable) {
			      Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			      thread.setDaemon(true);
			      thread.setName("PeriodicPoolThread");
			      return thread;
			   }
		});
		
		// modify status thread
		Globals.Log.info("Scheduling periodic ReservationStatusUpdateThread at " + ReservationStatusUpdateThread.getPeriod() + " sec.");
		sut = new ReservationStatusUpdateThread();
		sutFuture = scheduler.scheduleAtFixedRate(sut, ReservationStatusUpdateThread.getPeriod(), 
				ReservationStatusUpdateThread.getPeriod(), TimeUnit.SECONDS);
		
		// label sync thread
		Globals.Log.info("Scheduling periodic LabelSyncThread at " + LabelSyncThread.getPeriod() + " sec.");
		lst = new LabelSyncThread();
		lstFuture = scheduler.scheduleAtFixedRate(lst, LabelSyncThread.getPeriod(), 
				LabelSyncThread.getPeriod(), TimeUnit.SECONDS);
		
		// Pubsub thread
		if ("true".equalsIgnoreCase(OrcaController.getProperty(OrcaXmlrpcHandler.PropertyPublishManifest))) {
			Globals.Log.info("Starting pubsub thread");
			pubManager = new PublishManager(); // This will start the publisher thread
		} else {
			Globals.Log.info("Not starting pubsub thread, no configuration found");
			pubManager = null;
		}
	}
	
	/**
	 * Get hold of the Update Thread object
	 * @return
	 */
	public static ReservationStatusUpdateThread getSUT() {
		return sut;
	}
	
	/**
	 * Get hold of Slice Defer Thread object
	 * @return
	 */
	public static SliceDeferThread getSDT() {
		return sdt;
	}
	
	private XmlrpcOrcaState(){
		// Can't call this constructor
	}

	// don't save
	protected String broker;

	public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

	public static XmlrpcOrcaState getInstance() {
		return fINSTANCE;
	}

	/**
	 * Passes in by reference, so it can be modified
	 * @return
	 */
	public synchronized HashMap<String, BitSet> getControllerAssignedLabel() {
		return controllerAssignedLabel;
	}
	
	public HashMap<String, LinkedList<String>> getShared_IP_set() {
		return shared_IP_set;
	}

	public synchronized XmlRpcController getController(){
		return controller;
	}

	public synchronized void setController(XmlRpcController controller){
		this.controller = controller;
	}

	// manage state of compression of output 
	public boolean getCompression() {
		return compressOutput;
	}

	public synchronized void setCompression(boolean f) {
		compressOutput = f;
	}

	public synchronized void setUsedMac(String new_mac){
		usedMac.add(new_mac);
	}

	public synchronized void removeUsedMac(String new_mac){
		if(new_mac!=null)
			usedMac.remove(new_mac);
	}

	public synchronized boolean existingUsedMac(String mac){
		return usedMac.contains(mac);
	}

	/**
	 * Add new previously constructed slice
	 * @param s
	 */
	 public void addSlice(XmlrpcControllerSlice s) {
		 if (s != null) {
			 synchronized(this) {
				 slices.put(s.getSliceID(), s);
			 }
		 }
	 }

	 /**
	  * Remove slice form Orca state by slice object (slice lock must be held)
	  * @param s
	  */
	 public void removeSlice(XmlrpcControllerSlice s) {
		 if (s != null) {
			 // this also unmaps it from various mappings
			 synchronized(this) {
				 slices.remove(s.getSliceID());
			 }
			 s.close();
		 }
	 }

	 /**
	  * Remove slice from Orca state by id (slice lock must be held)
	  * @param sid
	  */
	 public void removeSlice(SliceID sid) {
		 if (sid != null) {
			 XmlrpcControllerSlice s = null;
			 synchronized(this) {
				 s = slices.get(sid.toString());
			 }
			 removeSlice(s);
		 }   
	 }

	 /**
	  * Get an existing controller slice for this ID
	  * @param sid
	  * @return
	  */
	 public XmlrpcControllerSlice getSlice(SliceID sid) {
		 if (sid == null)
			 return null;

		 synchronized(this) {
			 return slices.get(sid.toString());
		 }
	 }

	 /**
	  * Get an existing controller slice for this urn
	  * @param urn
	  * @return
	  */
	 public XmlrpcControllerSlice getSlice(String urn) {
		 String sid = XmlrpcControllerSlice.getSliceIDForUrn(urn);
		 logger.debug("getSlice(): this slice ID="+sid+" for urn="+urn);
		 if (sid != null) {
			 //return getSlice(new SliceID(sid));
			 synchronized (this) {
				 return slices.get(sid);
			 }

		 }
		 return null;
	 }

	 public void releaseAddressAssignment(ReservationMng r){
		 // mac address on the VM data interfaces
		 String parent_num_interface = UnitProperties.UnitNumberInterface;
		 String parent_mac_addr = UnitProperties.UnitEthPrefix;

		 String num_interface_str = OrcaConverter.getLocalProperty(r, parent_num_interface);
		 if(num_interface_str!=null){
			 int num_interface = Integer.valueOf(num_interface_str);
			 String mac_addr_property=null,mac_addr=null;
			 for(int i=0;i<num_interface;i++){
				 mac_addr_property = parent_mac_addr+String.valueOf(i)+".mac";
				 mac_addr=OrcaConverter.getLocalProperty(r, mac_addr_property);
				 removeUsedMac(mac_addr);
			 }
		 }
	 }
	 
	 //This happens when there is mismatch in passed in tags and the available tags in the site, by a speciall error message
	 //Normally, it means the tag was not released properly in the site controller 
	 //To mark off this tag in order to make the controller work
	 //needs further debugging in the control policy code
	 public void markFailedMissedTag(ReservationMng  r){
		 String notice = r.getNotices();
		 int tag=-1;
		 String domain=null;
		 if(notice== null)
			 return;
		 if(notice.contains("Passed in static label is not in the available labelset:static=")){
			 int index1 = notice.indexOf('=');
			 int index2 = notice.indexOf(";set");
			 tag = Integer.valueOf(notice.substring(index1+1, index2)).intValue();
		 }
		 
		 domain = r.getResourceType();
		 if(domain == null)
			 return;
		 domain = domain.split("\\.")[0];
		 if(tag>0){	 
			 if(this.controllerAssignedLabel.get(domain)!=null)
				 this.controllerAssignedLabel.get(domain).set(tag);
		 }
		 logger.warn("Marked failed tag to move on:"+domain+";tag="+tag);
	 }

	 /**
	  * Close dead slices and free up workflows/models. 
	  */
	 public void closeDeadSlices() {
		 
		 List<XmlrpcControllerSlice> remove_slices = new ArrayList<XmlrpcControllerSlice> ();

		 // don't allow concurrent close dead slices operations
		 IOrcaServiceManager sm = null;
		 try {
			 sm = getSM();
			 synchronized(this) {
				 for(Entry <String, XmlrpcControllerSlice> urn_wf : slices.entrySet() ){
					 XmlrpcControllerSlice sl;
					 sl = urn_wf.getValue();

					 if (sl != null) {
						 try {
							 logger.debug("XmlrpcOrcaState.closeDeadSlices(): slice  " + sl.getSliceUrn() + "/" + sl.getSliceID() + 
									 "; isDeadOrClosing=" + sl.isDeadOrClosing() + "; last delete=" + sl.getDeleteAttempt());
							 if (sl.isDeadOrClosing()) {
								 // if a slice consists of only failed reservations,
								 // delete if we've been trying to delete it for a while
								 // the delay means to help with debugging slices that fail completely /ib
								 if (sl.allFailed()) {
									 if (sl.getDeleteAttempt() != null) {
										 Calendar cc = Calendar.getInstance();
										 cc.setTime(sl.getDeleteAttempt());
										 Calendar cc1 = Calendar.getInstance();
										 // if we've been trying to delete it for 24 hours
										 cc.add(Calendar.HOUR, 24);
										 if (cc1.after(cc)) {
											 logger.info("XmlrpcOrcaState.closeDeadSlices(): deleting all failed slice " + sl.getSliceID() + "/" + sl.getSliceUrn() + " after 24 hours");
											 remove_slices.add(sl);
										 } 
									 } else
										 sl.markDeleteAttempt();
								 } else {
									 logger.info("XmlrpcOrcaState.closeDeadSlices(): deleting slice " + sl.getSliceID() + "/" + sl.getSliceUrn());
									 remove_slices.add(sl);
								 }
							 }
						 } catch (SliceStateMachine.SliceTransitionException e) {
							 logger.error("closeDeadSlices(): Slice " + sl.getSliceUrn() + "/" + sl.getSliceID() + " owned by " + sl.getUserDN() + " has encountered a state transition problem at garbage collection.");
						 }
					 }
				 }
			 }

			 // now remove
			 for(XmlrpcControllerSlice sl: remove_slices) {
				 try {
					 sl.lock();
					 sl.deleteFromPublishQ(logger);
					 List<ReservationMng> allR = sl.getAllReservations(sm);
					 if (allR != null) {
						 for(ReservationMng res: allR) {
							 releaseAddressAssignment(res);
							 markFailedMissedTag(res);
						 }
					 }
					 removeSlice(sl);
				 } catch (InterruptedException ie) {
					 ;
				 } finally {
					 sl.unlock();
				 }
			 }
		 } catch (Exception e) {
			 logger.error("closeDeadSlices(): unable to close slices due to " + e);
		 } finally {
			 if (sm != null)
				 XmlrpcOrcaState.getInstance().returnSM(sm);
		 }

	 }

	 /**
	  * Get all non-dead or closing slices for this user dn.  
	  * @param userDn
	  * @return
	  */
	 public List<String> getSlices(String userDn) {

		 if (userDn == null)
			 return null;

		 List<String> ret = new ArrayList<String>();

		 synchronized(this) {
			 for(Entry <String, XmlrpcControllerSlice> entry : slices.entrySet() ) {
				 XmlrpcControllerSlice sl = entry.getValue();
				 if (sl == null)
					 continue;
				 try {
					 if ((!sl.isDeadOrClosing()) && (sl.getUserDN().trim().equals(userDn.trim())))
						 ret.add(sl.getSliceID());
				 } catch (SliceStateMachine.SliceTransitionException e) {
					 logger.error("Slice " + sl.getSliceUrn() + " experienced a state transition exception");
				 }
			 }
		 }

		 return ret;
	 }

	 /**
	  * If the singleton implements Serializable, then this
	  * method must be supplied.
	  */
	 private Object readResolve() throws ObjectStreamException {
		 return fINSTANCE;
	 }
	 
     public synchronized IOrcaServiceManager getSM() throws Exception {
    	 if (controller == null)
    		 throw new Exception("Unable to get SM - controller not set yet.");
         return controller.orca.getServiceManager();
     }
     
     public synchronized void returnSM(IOrcaServiceManager sm) {
    	 if (sm != null)
    		 controller.orca.returnServiceManager(sm);
     }
     
     /**
      * Recover by querying the SM
      */
     public synchronized void recover() throws Exception {
    	 logger.info("Recovering XmlrpcOrcaState");
    	 IOrcaServiceManager sm = null;
    	 try {
    		 sm = getSM();

    		 logger.debug("Querying SM for active reservations");
    		 // get a list of all active-like reservations from the SM
    		 List<ReservationMng> actives = new ArrayList<ReservationMng>();
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateActive));
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateActiveTicketed));
    		 //actives.addAll(sm.getReservations(OrcaConstants.ReservationStateNascent));
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateTicketed));
    		 
    		 // build a list of slices we need to restore
    		 logger.debug("Searching for recoverable slices among " + actives.size() + " active/ticketed reservations");
    		 Set<String> recoveredSlices = new HashSet<String>();
    		 XmlrpcControllerSlice ndlSlice = null;
    		 for(ReservationMng a: actives) {
    			 logger.debug("Inspecting reservation " + a.getReservationID() + " of type " + a.getResourceType() + " from slice " + a.getSliceID());
    			 String sid = a.getSliceID();
    			 PropertiesMng confProps = a.getConfigurationProperties();

    			 // get slice name/urn and user DN from a property of one of reservations
    			 if (confProps == null)
    				 continue;
    			 
    			 String dn = null, sName = null, domain = null, label = null;
    			 // walk the properties, look for interesting ones
    			 for(PropertyMng p: confProps.getProperty()) {
    				 /**
    				  * Property restoration
    				  */
    				 // mac addresses
    				 Matcher m = macNamePattern.matcher(p.getName());
    				 if (m.matches()) {
    					 setUsedMac(p.getValue().trim());
    					 continue;
    				 } else {
    					 if (ReservationConverter.PropertyConfigUnitTag.equals(p.getName())) {
    						 label = p.getValue().trim();
    					 } else {
    						 if (UnitProperties.UnitDomain.equals(p.getName())) {
    							 domain = p.getValue().trim();
    						 }
    					 }
    				 }
    				 if ((label != null) && (domain != null)) {
    					 try {
    						 int iLabel = Integer.parseInt(label);
    						 logger.debug("Setting XmlrpcOrcaState.controllerAssignedLabel " + label + " for domain " + domain);
    						 RequestWorkflow.setBitsetLabel(controllerAssignedLabel, domain, iLabel);
    						 domain = null;
    						 label = null;
    						 continue;
    					 } catch (NumberFormatException nfe) {
    						 logger.error("Encountered non-numeric label " + label + " for domain " + domain + " unable to restore, skipping");
    						 label = null;
    						 domain = null;
    						 continue;
    					 }
    				 }
    				 /**
    				  * Slice restoration
    				  */
    				 if (p.getName().equals(UnitProperties.UserDN))
    					 dn = p.getValue();
    				 else if (p.getName().equals(UnitProperties.UnitSliceName))
    					 sName = p.getValue();
    				 
    				 if ((!recoveredSlices.contains(sid)) && (dn != null) && (sName != null)) {
    					 // recover the slice
    					 List<Map<String, ?>> users = ReservationConverter.restoreUsers(confProps.getProperty());
    	    			 ndlSlice = recoverSlice(sm, sid, sName, dn, users);
    	    			 if (ndlSlice != null)
    	    				 recoveredSlices.add(sid);
    	    			 dn = null; sName = null;
    	    			 continue;
    				 }
    			 }
    			 // the slice should be restored now, feed it the reservations that belong to it to recover
    			 // remaining properties
    			 ndlSlice = getSlice(new SliceID(a.getSliceID()));
    			 if ((ndlSlice != null)) {
    				 ndlSlice.addRecoveredReservation(a, logger);
    			 }
    		 }
    	 } catch (Exception e) {
    		 logger.error("Unable to recover XmlrpcOrcaState due to: " + e);
    		 throw new Exception("Unable to recover XmlrpcOrcaState due to: " + e);
    	 } finally {
    		 if (sm != null)
    			 returnSM(sm);
    	 }
    	 logger.info("Recovery of XmlrpcOrcaState completed successfully");
     }
     
     /** recover the slice from parsing the manifest
      * 
      * @param sm
      * @param sid
      */
     private XmlrpcControllerSlice recoverSlice(IOrcaServiceManager sm, String sid, String name, String userDN, List<Map<String, ?>> users) {
    	 logger.info("Recovering slice " + sid + "/" + name + " from " + userDN);

    	 try {

    		 SliceMng orcaSlice = sm.getSlice(new SliceID(sid));

    		 // restore slice with recovery
    		 XmlrpcControllerSlice ndlSlice = new XmlrpcControllerSlice(sm, orcaSlice, name, userDN, users, true);

    		 ndlSlice.recover(logger, controllerAssignedLabel, this.shared_IP_set);

    		 addSlice(ndlSlice);
    		 
    		 return ndlSlice;
    	 } catch (Exception e) {
    		 e.printStackTrace();
    		 logger.error("Unable to recover slice " + sid + "/" + name + " due to: " + e);
    		 return null;
    	 }
     }
     
     /**
      * Recover tags by querying the SM
      */
     public synchronized void syncTags(IOrcaServiceManager sm) {
    	 logger.info("Sync global tag for domains");
    	 try {
    		 logger.debug("Querying SM for active reservations");
    		 // get a list of all active-like reservations from the SM
    		 List<ReservationMng> actives = new ArrayList<ReservationMng>();
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateActive));
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateActiveTicketed));
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateNascent));
    		 actives.addAll(sm.getReservations(OrcaConstants.ReservationStateTicketed));

    		 // clear existing global labels
    		 // don't need to clear slice-local label set - it is only used to remove from the global
    		 // set on slice close
    		 controllerAssignedLabel.clear();
    		 
    		 // build a list of slices we need to restore
    		 logger.debug("Searching for recoverable slices among " + actives.size() + " active/ticketed reservations");
    		 for(ReservationMng a: actives) {
    			 logger.debug("Inspecting reservation " + a.getReservationID() + " of type " + a.getResourceType() + " from slice " + a.getSliceID());
    			 String domain = null, label = null; 
    			 List<UnitMng> un = sm.getUnits(new ReservationID(a.getReservationID()));
    			 if (un != null && un.size() > 0) {
    				 logger.debug("getManifest:un.size()="+un.size());	
    				 for (UnitMng u : un) {
    					 Properties p = OrcaConverter.fill(u.getProperties());
    					 if (p.getProperty(UnitProperties.UnitVlanTag) != null) {
    						 label=p.getProperty(UnitProperties.UnitVlanTag);
    					 }
    				 }
    			 }
    			 if (label == null)	//we only need net domains with assigned tag
    				 continue;

    			 XmlrpcControllerSlice s = getSlice(new SliceID(a.getSliceID()));
    			 
    			 PropertiesMng confProps = a.getConfigurationProperties();

    			 if (confProps == null)
    				 continue;
    			 
    			 // walk the properties, look for interesting ones
    			 for(PropertyMng p: confProps.getProperty()) {
    				 /**
    				  * used labels
    				  */
    				 if (UnitProperties.UnitDomain.equals(p.getName())) {
    					 domain = p.getValue().trim();
    				 }
    				 if ((label != null) && (domain != null)) {
    					 try {
    						 int iLabel = Integer.parseInt(label);
    						 logger.debug("Setting XmlrpcOrcaState.controllerAssignedLabel " + label + " for domain " + domain+";slice="+ (s != null ? s.sliceUrn : "slice not recovered"));
    						 RequestWorkflow.setBitsetLabel(controllerAssignedLabel, domain, iLabel);
    						 if (s != null)
    							 s.workflow.setControllerLabel(domain, iLabel);
    						 domain = null;
    						 label = null;
    						 continue;
    					 } catch (NumberFormatException nfe) {
    						 logger.error("Encountered non-numeric label " + label + " for domain " + domain + " unable to restore, skipping");
    						 label = null;
    						 domain = null;
    						 continue;
    					 }
    				 }
    			 }
    		 }
    	 } catch (Exception e) {
    		 logger.error("Unable to sync XmlrpcOrcaState due to: " + e);
    		 e.printStackTrace();
    		 return;
    	 } 
    	 logger.info("Sync of XmlrpcOrcaState completed successfully");
     }
}
