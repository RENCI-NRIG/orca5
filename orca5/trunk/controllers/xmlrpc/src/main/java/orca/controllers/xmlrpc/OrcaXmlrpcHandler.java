package orca.controllers.xmlrpc;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
//import com.ibm.icu.util.Calendar;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.security.auth.login.CredentialException;

import orca.controllers.OrcaController;
import orca.controllers.OrcaControllerException;
import orca.controllers.xmlrpc.SliceStateMachine.SliceCommand;
import orca.controllers.xmlrpc.geni.GeniAmV2Handler;
import orca.controllers.xmlrpc.geni.IGeniAmV2Interface.GeniStates;
import orca.controllers.xmlrpc.x509util.Credential;
import orca.embed.cloudembed.controller.InterCloudHandler;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.workflow.Domain;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.LeaseReservationMng;
import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.beans.UnitMng;
import orca.ndl.INdlModifyModelListener.ModifyType;
import orca.ndl.NdlException;
import orca.ndl.elements.Device;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.security.AbacUtil;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.shirako.common.meta.ResourceProperties;
import orca.util.CompressEncode;
import orca.util.ID;
import orca.util.ResourceType;
import orca.util.VersionUtils;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
/**
 * ORCA XMLRPC client interface
 * WARNING: Any method you declare public (non-static) becomes a remote method!
 * @author ibaldin
 *
 */
public class OrcaXmlrpcHandler extends XmlrpcHandlerHelper implements IOrcaXmlrpcInterface {
	private static final String WHITELIST_ERROR = "ERROR: this user is not on the controller's whitelist; system may be in maintenance.";
	public static final String RET_RET_FIELD = "ret";
	public static final String MSG_RET_FIELD = "msg";
	public static final String ERR_RET_FIELD = "err";
	
	protected final Logger logger = OrcaController.getLogger(this.getClass().getName());
	
	protected ResourcePoolsDescriptor pools;
	
	public static final String PropertyXmlrpcControllerUrl = "xmlrpc.controller.base.url";
	public static final String PUBSUB_PROPS_FILE_NAME = "controller.properties";
	public static final String PUBSUB_ENABLED_PROP = "ORCA.publish.manifest";
	public static final String MAX_DURATION_PROP = "controller.max.duration";

	public static final long MaxReservationDuration = ReservationConverter.getMaxDuration();
	
	/**
	 * Maps a domain to the resources available from that domain.
	 */
	protected HashMap<String, SiteResourceTypes> typesMap;
	protected List<String> abstractModels;

	protected final XmlrpcOrcaState instance;
	protected boolean verifyCredentials = true;

	// thread for deferred slices due to interdomain complexity
	protected static final SliceDeferThread sdt = new SliceDeferThread();
	protected static final Thread sdtThread = new Thread(sdt);
	
	// lock to create slice
	protected static Integer createLock = 0;
	
	// start the thread
	static {
		sdtThread.setDaemon(true);
		sdtThread.setName("SliceDeferThread");
		sdtThread.start();
	}
	
	/** manage the xmlrpc return structure
	 * 
	 * @param msg
	 * @return
	 */
	private static Map<String, Object> setError(String msg) {
		Map <String, Object> m = new HashMap<String, Object>();
		m.put(ERR_RET_FIELD, true);
		m.put(MSG_RET_FIELD, msg);
		return m;
	}
	
	/** manage the xmlrpc return structure
	 * 
	 * @param ret
	 * @return
	 */
	private static Map<String, Object> setReturn(Object ret) {
		Map <String, Object> m = new HashMap<String, Object>();
		m.put(ERR_RET_FIELD, false);
		m.put(RET_RET_FIELD, ret);
		return m;
	}
	

	public OrcaXmlrpcHandler() {
		//Some Fields in the XmlrpcorcaState are populated by the XmlrpcController, before invoke this.
		instance = XmlrpcOrcaState.getInstance();		
		
		try {
			if (OrcaController.getProperty(XmlRpcController.PropertyOrcaCredentialVerification) != null){
				verifyCredentials = new Boolean(OrcaController.getProperty(XmlRpcController.PropertyOrcaCredentialVerification));
			} else { 
				verifyCredentials = true;
			}
		} catch (Exception e) {
			verifyCredentials = true;
		}
	}

	/**
	 * Returns the geni AM api version 
	 * @return
	 */
	public Map<String, Object> getVersion() {
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("orca_api", 1);
		result.put("implementation", "ORCA");
		result.put("version", VersionUtils.buildVersion);
		return (result);
	}

	
	/**
	 * Returns resource representations for all resources across all AMs
	 * @return
	 */

	public Map<String, Object> listResources(Object[] credentials, Map<?,?> options) {
		IOrcaServiceManager sm = null;
		
		instance.closeDeadSlices();
		try {
			logger.info("ORCA API listResources() invoked");
			
			String userDN = validateOrcaCredential(null, credentials, null, verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
			
			sm = instance.getSM();
			
			// Call discoverTypes to populate abstractModels
			try {
				discoverTypes(sm);
			} catch (Exception ex) {
				logger.error("listResources(): discoverTypes() failed to populate abstractModels: ", ex);
				return setError("ERROR:  discoverTypes() failed to populate abstractModels");
			}
			
			//discoverTypes();

			StringBuilder rspecResult = new StringBuilder(" ");
			int siteIndex = 0;

			rspecResult.append(VersionUtils.buildVersion);
			rspecResult.append("\n");
			for (String str : abstractModels) {
				// Convert each abstract ndl to rspec
				// rspecResult.append(convertAbstractNdlToRspec(str));
				//logger.debug("Current abstract model: \n " + str);
				rspecResult.append(str);
				rspecResult.append("********************************************************* [");
				rspecResult.append(siteIndex);
				rspecResult.append("] \n");
				siteIndex++;
			}

			rspecResult.append("There are ");
			rspecResult.append(siteIndex);
			rspecResult.append(" available resource domains \n");

			return setReturn(rspecResult.toString());
		} catch (CredentialException ce) {
			logger.error("listResources(): Credential Exception: " + ce.getMessage());
			return setError("ERROR: CredentialException encountered: " + ce.getMessage());
		} catch (Exception oe) {
			logger.error("listResources(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("ERROR: Exception encountered: " + oe);
		} finally {
			if (sm != null){
				instance.returnSM(sm);
			}
		}

	}

	/***
	 * create a slice.
	 * @param slice_urn - user-specified slice name
	 * @param credentials (ignored)
	 * @param resReq (NDL request)
	 * @param users (user logins and lists of ssh keys for each login; field names are "login", "sudo" and "keys")
	 * @return
	 */
	public Map<String, Object> createSlice(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users) {
		IOrcaServiceManager sm = null;
		XmlrpcControllerSlice ndlSlice = null;

		//Check existing slices, remove the closed ones from XmlrpcOrcaState
		instance.closeDeadSlices();
		
		if (!checkMemory(null)) {
			return setError("ERROR: low system memory, please try later");
		}
		
		synchronized(createLock) {

			try {
				logger.info("ORCA API createSlice() invoked");

				String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

				// check the whitelist
				if (verifyCredentials && !checkWhitelist(userDN)) 
					return setError(WHITELIST_ERROR);

				sm = instance.getSM();


				// check slice name - don't allow duplicates
				if (XmlrpcOrcaState.getInstance().getSlice(slice_urn) != null){
					return setError("ERROR: duplicate slice urn " + slice_urn);
				}


				// generate and register new slice
				SliceMng slice = new SliceMng();
				slice.setName(slice_urn);
				slice.setClientSlice(true);
				SliceID sid = sm.addSlice(slice);

				if (sid == null){
					throw new Exception("Could not create slice: " + sm.getLastError());
				}

				// if uuid was not provided, use Orca GUID
				// FIXME: really should check for tuple <name, uuid> if it is already present.
				String uuid = null;
				int index = 0;
				while (index < credentials.length) {
					// see if we can get uuid from credentials
					try {
						Credential cred = new Credential((String)credentials[index]);
						uuid = cred.getObjectGid().getUuid().toString();
						break;
					} catch (ClassCastException cce) {
						// just use GUID
						break;
					} catch (NullPointerException npe) {
						// try to find it in the next one
						index++;
						continue;
					} 
				}
				if (uuid == null){
					uuid = sid.toString();
				}
				// now uuid should be set

				//populate typesMap and abstractModels
				try {
					discoverTypes(sm);
				} catch (Exception ex) {
					logger.error("createSlice(): discoverTypes() failed to populate typesMap and abstractModels: " + ex, ex);
					return setError("ERROR: createSlice(): discoverTypes() failed to populate typesMap and abstractModels");
				}
				
				// create XmlrpcSlice object and register with Orca state
				ndlSlice = new XmlrpcControllerSlice(sm, slice, slice_urn, userDN, users, false);
				// we lock the slice from any concurrent modifications
				ndlSlice.lock();
				ndlSlice.getStateMachine().transitionSlice(SliceCommand.CREATE);

				instance.addSlice(ndlSlice);

				instance.sync(sm);
				
				instance.getController();
				String controller_url = OrcaController.getProperty(PropertyXmlrpcControllerUrl);

				ReservationConverter orc = ndlSlice.getOrc(); 
				
				DomainResourcePools drp = new DomainResourcePools(); 
				drp.getDomainResourcePools(pools);


				// get the slice workflow and run it
				RequestWorkflow workflow =null;
				try{
					workflow = ndlSlice.getWorkflow();
					workflow.setGlobalControllerAssignedLabel(instance.getControllerAssignedLabel());
					workflow.setShared_IP_set(instance.getShared_IP_set());
					workflow.run(drp, abstractModels, resReq, userDN, controller_url, ndlSlice.getSliceID());
				}catch(Exception e){
					e.printStackTrace();
					instance.removeSlice(ndlSlice);
					logger.error("createSlice(): No reservations created for this request; Error:" + e);
					return setError("Error:Embedding workflow Exception! " + e);
				}

				if(workflow.getErrorMsg()!=null){
					instance.removeSlice(ndlSlice);
					logger.error("createSlice(): No reservations created for this request; Error:"+workflow.getErrorMsg());
					return setError(workflow.getErrorMsg());
				}

				workflow.setSliceName(slice_urn, XmlrpcControllerSlice.getSliceIDForUrn(slice_urn), userDN);

				//this also update the typesMap
				ndlSlice.setComputedReservations(orc.getReservations(sm,workflow.getBoundElements(), typesMap, workflow.getTerm(),workflow.getslice()));

				orc.updateTerm(workflow.getManifestModel());

				if (ndlSlice.getComputedReservations() == null) {
					logger.error("createSlice(): No reservations created for this request");
					return setError("ERROR: No reservations created for the request");
				}
				else
					logger.debug("This request created " + ndlSlice.getComputedReservations().size() + " reservations");

				Iterator<TicketReservationMng> it = ndlSlice.getComputedReservations().iterator();

				while (it.hasNext()) {
					try {
						TicketReservationMng currRes = it.next();
						if (userDN != null) {
							OrcaConverter.setLocalProperty(currRes, XmlrpcOrcaState.XMLRPC_USER_DN, userDN.trim());
						}			

						if(AbacUtil.verifyCredentials){
							setAbacAttributes(currRes, logger);
						}
					} catch (ThreadDeath td) {
						throw td;
					} catch (Throwable t) {
						// FIXME: close all reservations
						logger.error("createSlice(): Exception, failed to demand reservation" + t, t);
						return setError("ERROR: Exception, failed to demand reservation" + t);
					}
				}

				// call on slicedeferthread to either demand immediately
				// or put on deferred queue
				sdt.processSlice(ndlSlice);

				// What do we return in the manifest ? reservation Id, type, units ? slice ?
				StringBuilder result = new StringBuilder("Here are the leases: \n");

				it = ndlSlice.getComputedReservations().iterator();
				result.append("Request id: ");
				result.append(ndlSlice.getSliceID());
				result.append("\n");

				while(it.hasNext()){
					LeaseReservationMng currRes = (LeaseReservationMng) sm.getReservation(new ReservationID(it.next().getReservationID()));

					result.append("[ ");

					result.append("  Slice UID: ");
					result.append(currRes.getSliceID().toString());

					result.append(" | Reservation UID: ");
					result.append(currRes.getReservationID().toString());

					result.append(" | Resource Type: ");
					result.append(currRes.getResourceType().toString());

					result.append(" | Resource Units: ");
					result.append(currRes.getUnits());

					result.append(" ] \n");
				}

				// call publishManifest if there are reservations in the slice
				if((ndlSlice.getComputedReservations() != null) && (ndlSlice.getComputedReservations().size() > 0)) {
					ndlSlice.publishManifest(logger);
				}

				result.append(workflow.getErrorMsg());
				
				//workflow.closeModel(); //close the substrate model, but would break modifying now
				
				return setReturn(result.toString());
			} catch (CredentialException ce) {
				logger.error("createSlice(): Credential Exception: " + ce.getMessage());
				return setError("ERROR: CredentialException encountered: " + ce.getMessage());
			} catch (ReservationConverter.ReservationConverterException re) {
				logger.error("createSlice(): Reservation converter exception: " + re.getMessage());
				return setReturn("ERROR: Unable to create reservation due to: " + re.getMessage());
			} catch (Exception oe) {
				logger.error("createSlice(): Exception encountered: " + oe.getMessage());	
				oe.printStackTrace();
				return setReturn("ERROR: Exception encountered: " + oe);
			} finally {
				if (sm != null){
					instance.returnSM(sm);
				}
				if (ndlSlice != null)
					ndlSlice.unlock();
			}
		}
	}

	/**
	 * Returns the status of the reservations in the input slice
	 * @param slice_urn
	 * @return
	 */
	public Map<String, Object> sliceStatus(String slice_urn, Object[] credentials) {
		try {
			List<ReservationMng> allRes = null;
			String result;
			StringBuilder resultSB = new StringBuilder();
			logger.info("ORCA API sliceStatus() invoked");
			
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "info"}, verifyCredentials, logger);

			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
			
			allRes = getSliceReservations(instance, slice_urn, logger);
			
			if (allRes == null){
				result = "ERROR: Invalid slice " + slice_urn + ", no reservations in the slice";
				logger.error("sliceStatus(): Invalid slice " + slice_urn  + ", no reservations in the slice");
				return setError(result);
			}
			else{
				logger.debug("There are " + allRes.size() + " reservations in the slice with sliceId = " + slice_urn);
				if (allRes.size() <= 0) {
					result = "ERROR: There are no reservations in the slice with sliceId = " + slice_urn;
					return setError(result);
				}
				resultSB.append(VersionUtils.buildVersion);
				resultSB.append("\n");
				for (ReservationMng res: allRes){
					resultSB.append("************************************************************* \n");
					resultSB.append("[ ");

					resultSB.append(" Reservation UID: ");
					resultSB.append(res.getReservationID());

					resultSB.append(" | Resource Type: ");
					resultSB.append(res.getResourceType());  // FIXME: this used to use getApprovedResourceType()

					resultSB.append(" | Units: ");
					resultSB.append(((LeaseReservationMng)res).getLeasedUnits());

					resultSB.append(" | Status: ");
					resultSB.append(OrcaConstants.getReservationStateName(res.getState()));

					resultSB.append(" ] \n");
				}

				resultSB.append(getSliceManifest(instance, slice_urn, logger));
			}
			result = resultSB.toString();
			
			return setReturn(result);
		} catch (CredentialException ce) {
			logger.error("sliceStatus(): Credential Exception: " + ce.getMessage());
			return setError("ERROR: CredentialException encountered: " + ce.getMessage());
		} catch (OrcaControllerException oce) {
			logger.error("sliceStatus(): ControllerException: " + oce.getMessage());
			return setError("ERROR: ControllerException encountered: " + oce.getMessage());
		} catch (Exception oe) {
			logger.error("sliceStatus(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("ERROR: Exception encountered: " + oe);
		} 
	}

	public Map<String, Object> modifySlice(String slice_urn, Object[] credentials, String modReq) {
		XmlrpcControllerSlice ndlSlice = null;
		IOrcaServiceManager sm = null;

		try {
			String result = null;
			logger.info("ORCA API modifySlice() invoked");

			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);

			if ((modReq == null) || (modReq.length() == 0)) {
				logger.error("modifySlice(): modification request for slice " + slice_urn + " is empty");
				return setError("ERROR: modification request is empty for slice " + slice_urn);
			}

			// find this slice and lock it
			ndlSlice = instance.getSlice(slice_urn);
			if (ndlSlice == null) {
				logger.error("modifySlice(): unable to find slice " + slice_urn + " among active slices");
				return setError("ERROR: unable to find slice " + slice_urn + " among active slices");
			}
			// lock the slice
			ndlSlice.lock();
			ndlSlice.getStateMachine().transitionSlice(SliceCommand.MODIFY);

			RequestWorkflow workflow = ndlSlice.getWorkflow();

			sm = instance.getSM();
			
			//populate typesMap and abstractModels
			try {
				discoverTypes(sm);
			} catch (Exception ex) {
				logger.error("modifySlice(): discoverTypes() failed to populate typesMap and abstractModels: " + ex);
				return setError("ERROR: discoverTypes() failed to populate typesMap and abstractModels");
			}
			DomainResourcePools drp = new DomainResourcePools(); 
			drp.getDomainResourcePools(pools);
			ReservationConverter orc = ndlSlice.getOrc();
			ReservationElementCollection r_collection = orc.getElementCollection();
			if(r_collection==null){
				logger.error("ModifySlice(): No reservations created for this request; Error:no r_collection");
				return setError("Error:ModifySlice Exception! no r_coolection");
			}else if(r_collection.NodeGroupMap==null){
				logger.error("ModifySlice(): No reservations created for this request; Error:no NodeGroupMap");
				return setError("Error:ModifySlice Exception! no NodeGroupMap");
			}else if(r_collection.firstGroupElement==null){
				logger.error("ModifySlice(): No reservations created for this request; Error:no firstGroupElement");
				return setError("Error:ModifySlice Exception! no firstGroupElement");
			}
			try{
				workflow.modify(drp, modReq,r_collection.NodeGroupMap, r_collection.firstGroupElement);
			}catch(Exception e){
				e.printStackTrace();
				logger.error("ModifySlice(): No reservations created for this request; Error:");
				return setError("Error:ModifySlice Exception!");
			}

			if(workflow.getErrorMsg()!=null){
				logger.error("modifySlice(): No reservations created for this request; Error:"+workflow.getErrorMsg());
				return setError(workflow.getErrorMsg());
			}
			
			// See createSlice() and XmlrpcControllerSlice object for methods.
			// remapping between urn's and slice ids is done by using static methods on
			// XmlrpcControllerSlice object. XmlrpcOrcaState only has one map from slice IDs
			// to XmlrpcControllerSlice objects. These slice objects are locked within each call
			// using a fair semaphore. 
			List<ReservationMng> allRes = null;
			HashMap <String, List<ReservationMng>> m_map = null;
			logger.info("ORCA API modifySlice() started...");

			try {
				allRes = ndlSlice.getAllReservations(sm);
			} catch (Exception e) {
				logger.error("modifySlice(): Exception encountered for " + slice_urn + ": " + e);
				return setError("ERROR: unable to get reservations in slice status for " + slice_urn);
			} 

			if (allRes == null){
				result = "ERROR: Invalid slice " + slice_urn + ", slice status can't be determined";
				logger.error("modifySlice(): Invalid slice " + slice_urn  + ", slice status can't be determined");
				return setError(result);
			}
			else {
				//ReservationConverter orc = new ReservationConverter();
				GeniStates geniStates = GeniAmV2Handler.getSliceGeniState(instance, slice_urn);
				orc.updateGeniStates(ndlSlice.getWorkflow().getManifestModel(), geniStates);
				OntModel manifestModel=orc.getManifestModel(workflow.getManifestModel(),
						workflow.getDomainInConnectionList(),
						workflow.getBoundElements(),
						allRes);

				InterCloudHandler ih =  (InterCloudHandler) workflow.getEmbedderAlgorithm();

				LinkedList <Device> addedDevices = ih.getAddedDevices();
				LinkedList<NetworkElement> l_D = new LinkedList<NetworkElement>();
				for(int i=0;i<addedDevices.size();i++)
					l_D.add((NetworkElement)addedDevices.get(i) );
				m_map=orc.modifyReservations(manifestModel, allRes, typesMap, workflow.getslice(), ih.getModifies(),l_D);
				ih.modifyComplete(); //clear the modify data.
			}
			//remove reservations
			List <ReservationMng> a_r = m_map.get(ModifyType.REMOVE.toString());
			if (a_r == null) {
				result ="No removed reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID();
				logger.debug("No removed reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
			} else {
				logger.debug("There are " + a_r.size() + " reservations to be removed in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());
				for (ReservationMng rr: a_r){
					try{
						instance.releaseAddressAssignment(rr);
						// not really needed /ib 08/05/14
						//ndlSlice.removeComputedReservations(rr);
						sm.closeReservation(new ReservationID(rr.getReservationID()));
					} catch (Exception ex) {
						ex.printStackTrace();
						result = "Failed to close reservation"+ex;
						throw new RuntimeException("Failed to close reservation", ex);
					}
				}
			}       
			//add reservations
			a_r=m_map.get(ModifyType.ADD.toString());
			if (a_r == null) {
				result ="No added reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID();
				logger.debug("No added reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
			} else {
				logger.debug("There are " + a_r.size() + " new reservations in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());
				for (ReservationMng rr: a_r){
					try{
						instance.releaseAddressAssignment(rr);

						if (userDN != null) 
							OrcaConverter.setLocalProperty(rr, XmlrpcOrcaState.XMLRPC_USER_DN, userDN.trim());

						logger.debug("Issuing demand for reservation: " + rr.getReservationID().toString());
						if(AbacUtil.verifyCredentials)
							setAbacAttributes(rr, logger);
						
						// not really needed /ib 08/05/15
						//ndlSlice.addComputedReservations((TicketReservationMng) rr);
						sm.demand(rr);
					} catch (Exception ex) {
						result = "Failed to redeem reservation"+ex;
						throw new RuntimeException("Failed to redeem reservation", ex);
					}
				}
			}       

			// call publishManifest if there are reservations in the slice
			List<ReservationMng> sliceRes = ndlSlice.getReservationsByState(sm, OrcaConstants.ReservationStateActive, 
					OrcaConstants.ReservationStateActiveTicketed, OrcaConstants.ReservationStateTicketed);
			if ((sliceRes != null) && (sliceRes.size() > 0)){
				ndlSlice.publishManifest(logger);
			}

			if (result == null)
				result = "No result available";
			return setReturn(result);
		} catch (CredentialException ce) {
			logger.error("modifySlice(): Credential Exception: " + ce.getMessage());
			return setError("ERROR: CredentialException encountered: " + ce.getMessage());
		} catch (Exception oe) {
			logger.error("modifySlice(): Exception encountered: " + oe.getMessage());       
			oe.printStackTrace();
			return setError("ERROR: Exception encountered: " + oe);
		}
		finally {
			if (sm != null)
				instance.returnSM(sm);

			if (ndlSlice != null)
				ndlSlice.unlock();
		}


	}
	

	/**
	 * Deletes the slices in the slice with input sliceId; Issue close on all underlying reservations
	 * @param sliceId
	 * @return
	 */
	public Map<String, Object> deleteSlice(String slice_urn, Object[] credentials) {
		IOrcaServiceManager sm = null;
		XmlrpcControllerSlice ndlSlice = null;
		
		try {
			Boolean result = false;
			logger.info("ORCA API deleteSlice() invoked");
			
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
			
            // find this slice and lock it
            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
            	logger.error("deleteSlice(): unable to find slice " + slice_urn + " among active slices");
            	return setError("ERROR: unable to find slice " + slice_urn + " among active slices");
            }
            
            if (sdt.inDeferredQueue(ndlSlice)) {
            	logger.error("deleteSlice(): unable to delete slice " + slice_urn + ", it is waiting in the defer queue");
            	return setError("ERROR: unable to delete deferred slice " + slice_urn + ", please try some time later");
            }
            
            // lock the slice
            ndlSlice.lock();

			sm = instance.getSM();
			
			List<ReservationMng> allRes = ndlSlice.getAllReservations(sm);
			int failCount = 0;
			if(allRes == null){
				 result = false;
				 ndlSlice.getStateMachine().transitionSlice(SliceCommand.DELETE);
                 logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
			} else {
				logger.debug("There are " + allRes.size() + " reservations in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());
				for (ReservationMng r : allRes){
					try {
						logger.debug("Closing reservation with reservation GUID: " + r.getReservationID());
						if (userDN != null) {
							if (!userDN.equals(OrcaConverter.getLocalProperty(r, XmlrpcOrcaState.XMLRPC_USER_DN))) {
								logger.error("User " + userDN + " is trying to close reservation " + 
										r.getReservationID() + " of which it is not the owner (real owner: " + 
										OrcaConverter.getLocalProperty(r, XmlrpcOrcaState.XMLRPC_USER_DN) + ")");
								failCount++;
							} else {
								// FIXME: this should be redundant, since we just validated the user_dn and
								// setAbacAttributes is called on createSlice. Moreover, closeReservation uses only the
								// reservationId, not the whole object
								if(AbacUtil.verifyCredentials){
									setAbacAttributes(r, logger);
								}
								sm.closeReservation(new ReservationID(r.getReservationID()));
								instance.releaseAddressAssignment(r);
							}
						}
					} catch (Exception ex) {
						result = false;
						return setError("Failed to close reservation due to " + ex);
					}
				}
				result = true;
			}
	
			if (failCount == 0) {
				ndlSlice.getStateMachine().transitionSlice(SliceCommand.DELETE);
				//FixME: needs to reset typeMaps for each domain.........
				try {
					sm.removeSlice(new SliceID(ndlSlice.getSliceID()));
				} catch (Exception e) {
					// FIXME: what should we do here?
					logger.error("deleteSlice(): Unable to unregister slice " + ndlSlice.getSliceID() + " for urn " + slice_urn);
				}

				instance.removeSlice(ndlSlice);
			
				// delete this slice from publish queue;
				ndlSlice.deleteFromPublishQ(logger);
			} else {
				result = false;
			}
			
			return setReturn(result);
		} catch (CredentialException ce) {
			logger.error("deleteSlice(): Credential Exception: " + ce.getMessage());
			return setError("deleteSlice(): Credential Exception: " + ce.getMessage());
		} catch (Exception oe) {
			logger.error("deleteSlice(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("deleteSlice(): Exception encountered: " + oe.getMessage());
		} finally {
			if (sm != null){
				instance.returnSM(sm);
			}
			if (ndlSlice != null)
				ndlSlice.unlock();
		}
	}

	/**
	 * Retrieve urns/names of all slices belonging to the owner of the credentials
	 * @param credentials
	 * @return
	 */
	public Map<String, Object> listSlices(Object[] credentials) {
		List<String> ret = new ArrayList<String>();
		// NOTE: This is the first call that will properly return a struct
		// Other calls will be modified as well 08/09/12 /ib
		try {
			String userDn = validateOrcaCredential(null, credentials, null, verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDn)) 
				return setError(WHITELIST_ERROR);
			
			List<String> activeSlices = instance.getSlices(userDn);
			
			if (activeSlices != null)
				for(String s: activeSlices) {
					String urn = XmlrpcControllerSlice.getSliceUrnForId(s);
					if (urn != null) {
						ret.add(urn.trim());
					} else
						logger.error("listSlices(): encountered null urn for slice " + s + " user " + userDn + "; skipping");
			}
		} catch (CredentialException ce) {
			logger.error("listSlices(): Credential Exception: " + ce.getMessage());
			return setError("listSlices(): Credential Exception: " + ce.getMessage());
		} catch (Exception oe) {
			logger.error("listSlices(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("listSlices(): Exception encountered: " + oe.getMessage());
		} finally {

		}
		
		return setReturn(ret);
	}

	/**
	 * 
	 */
     public Map<String, Object>  renewSlice(String slice_urn, Object[] credentials, String newTermEnd) {
		IOrcaServiceManager sm = null;
		XmlrpcControllerSlice ndlSlice = null;
    	try { 
    		Boolean result = false;
			logger.info("ORCA API renewSlice() invoked");
			
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
			
            // find this slice and lock it
            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
                    logger.error("renewSlice(): unable to find slice " + slice_urn + " among active slices");
                    return setError("ERROR: unable to find slice " + slice_urn + " among active slices");
            }
            // lock the slice
            ndlSlice.lock();

            if (!ndlSlice.isStableOK() && !ndlSlice.isStableError() && !ndlSlice.isDead()) {
            	logger.info("renewSlice(): unable to extendy slice that is not yet stable, try again later");
            	return setError("ERROR: unable to extend slice that is not yet stable, try again later");
            }
            
			sm = instance.getSM();
			
			Date termEndDate = parseRFC3339Date(newTermEnd.trim());
			logger.debug("New end date = " + termEndDate);
			
			Calendar termEndDateCal = Calendar.getInstance();
			termEndDateCal.setTime(termEndDate);
			Calendar systemDefaultEndCal = Calendar.getInstance();
			systemDefaultEndCal.add(Calendar.MILLISECOND, (int)MaxReservationDuration);
			Calendar nowCal = Calendar.getInstance();
			
			if (nowCal.after(termEndDateCal)){
				logger.debug("New term end date in the past..");
				return setError("renewSlice(): renewal term end time is in the past.. Can't renew slice..");
			}
			
			// compare slice end date to system default 
			if (systemDefaultEndCal.before(termEndDateCal)) {
				logger.debug("New term end date exceeds system default, setting to system default.");
				termEndDate = systemDefaultEndCal.getTime();
			}
			
			// compare slice end to current end
			RequestWorkflow workflow = ndlSlice.getWorkflow();
			OrcaReservationTerm term = workflow.getTerm();
			Date sliceEnd = term.getEnd();
			Calendar sliceEndCal = Calendar.getInstance();
			sliceEndCal.setTime(sliceEnd);
			
			// can't extend back in time
			if (termEndDateCal.before(sliceEndCal)) {
				logger.debug("Attempted extend date is shorter than current slice end date");
				return setError("renewSlice(): renewal term shorter than original slice end is not valid.");
			}
			
			List<ReservationMng> allRes =  ndlSlice.getAllReservations(sm);
			List<ReservationMng> failedToExtend = new ArrayList<ReservationMng>();
			if (allRes == null){
				result = false;
				logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
			} else {
				logger.debug("There are " + allRes.size() + " reservations in the slice with sliceId = " + ndlSlice.getSliceID());
				Calendar extendedEnd = Calendar.getInstance();
				extendedEnd.setTime(termEndDate);
				for (ReservationMng r : allRes){
					Calendar resEnd = Calendar.getInstance();
					resEnd.setTime(new Date(r.getEnd()));
					if (extendedEnd.before(resEnd)) {
						logger.debug("Attempted extend date: " + getRFC3339String(extendedEnd) + " is shorter than original reservation " + r.getReservationID() + " end date: " + getRFC3339String(resEnd));
						return setError("renewSlice(): renewal term shorter than original reservation end is not valid.");
					}
					try {
						logger.debug("Extending reservation with reservation GUID: " + r.getReservationID());
						if (AbacUtil.verifyCredentials){
							setAbacAttributes(r, logger);
						}
						boolean extret = sm.extendReservation(new ReservationID(r.getReservationID()), termEndDate);
						if (!extret) 
							failedToExtend.add(r);
					} catch (Exception ex) {
						result = false;
						throw new RuntimeException("Failed to extend reservation", ex);
					}
				}
				
				if (failedToExtend.size() == 0) {
					workflow.modifyTerm(termEndDate);
					ReservationConverter orc = ndlSlice.getOrc();
					orc.modifyTerm(workflow.getManifestModel(), workflow.getTerm());
					result = true;
				} else {
					String extMessage;
					if (failedToExtend.size() == allRes.size()) {
						extMessage = "Failed to extend all reservations in slice " + slice_urn;
					} else {
						StringBuilder sb = new StringBuilder("Failed to extend reservations ");
						for (ReservationMng r: failedToExtend) {
							sb.append(" " + r.getReservationID());
						}
						sb.append(" in slice " + slice_urn);
						extMessage = sb.toString();
					}
					result = false;
					return setError("renewSlice(): " + extMessage);
				}
			}

			return setReturn(result);
		} catch (CredentialException ce) {
			logger.error("renewSlice(): Credential Exception: " + ce.getMessage());
			return setError("renewSlice(): Credential Exception: " + ce.getMessage());
		} catch (Exception oe) {
			logger.error("renewSlice(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("renewSlice(): Exception encountered: " + oe.getMessage());
		} finally {
			if (sm != null){
				instance.returnSM(sm);
			}
			if (ndlSlice != null)
				ndlSlice.unlock();
		}
	}
    
    /**
     * Return unit properties of a sliver in a list of maps of pName, pValue. One map per unit. 
     * @param slice_urn
     * @param sliver_guid
     * @param credentials
     * @return
     */
    public Map<String, Object> getSliverProperties(String slice_urn, String sliver_guid, Object[] credentials) {
		IOrcaServiceManager sm = null;
		XmlrpcControllerSlice ndlSlice = null;
		
    	logger.info("ORCA API getSliverProperties() invoked for " + sliver_guid);
    	
		if (sliver_guid == null) 
			return setError("ERROR: getSliverProperties() sliver_guid is null");
		
    	try {
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
    		sm = instance.getSM();
    		
            // find this slice and lock it
            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
                    logger.error("getSliverProperties(): unable to find slice " + slice_urn + " among active slices");
                    return setError("ERROR: unable to find slice " + slice_urn + " among active slices");
            }
            // lock the slice
            ndlSlice.lock();
    		
    		List<UnitMng> sliverUnits = ndlSlice.getUnits(sm, sliver_guid);
    		if (sliverUnits == null) {
    			return setError("getSliverProperties(): no units associated with reservation " + sliver_guid);
    		}
    		List<Map<String, String>> uProps = new LinkedList<Map<String, String>>();
    		for(UnitMng unit: sliverUnits) {
    			PropertiesMng up = unit.getProperties();
				if (up != null) {
					Map<String, String> mp = new HashMap<String, String>();
					for(PropertyMng pp: up.getProperty()) {
						mp.put(pp.getName(), pp.getValue());
					}
					uProps.add(mp);
				}
    		}
    		return setReturn(uProps);
    	} catch (Exception e) {
    		logger.error("getSliverProperties(): Exception encountered: " + e.getMessage());	
			e.printStackTrace();
			return setError("getSliverProperties(): Exception encountered: " + e.getMessage());
    	} finally {
			if (sm != null){
				instance.returnSM(sm);
			}
			if (ndlSlice != null)
				ndlSlice.unlock();
    	}
    	
    }
     
	protected void discoverTypes(IOrcaServiceManager sm) {
		typesMap = new HashMap<String, SiteResourceTypes>();
		abstractModels = new ArrayList<String>();
		
		ID broker = instance.getController().getBroker(sm);
		if (broker == null){
			throw new RuntimeException("Unable to determine broker proxy for this controller. Please check SM container configuration and logs.");
		}

        instance.setBroker(broker.toString());

		List<PoolInfoMng> mypools = sm.getPoolInfo(broker);
		if (mypools == null){
			throw new RuntimeException("Could not discover types: " + sm.getLastError(), sm.getLastError().getException());
		}
		
		pools = new ResourcePoolsDescriptor();
		for (PoolInfoMng pool : mypools) {
			try {
				ResourcePoolDescriptor rpd = OrcaConverter.fill(pool);
				ResourceType type = rpd.getResourceType();
	    		ResourcePoolAttributeDescriptor a = rpd.getAttribute(ResourceProperties.ResourceDomain);
	    		if (a == null) {
	    			throw new RuntimeException("Missing domain information for resource pool:  " + type);
	    		}
	    		pools.add(rpd);
	    		String domain = a.getValue();
	    		// obtain the resource types record for this domain
	    		SiteResourceTypes domainResources = typesMap.get(domain);
	    		if (domainResources == null) {
	    			domainResources = new SiteResourceTypes(domain);
	    			typesMap.put(domain, domainResources);
	    		}
				// make the resource type record for this resource type
				SiteResourceType drt = new SiteResourceType(type);
				// try to obtain the available units
	            a = rpd.getAttribute(ResourceProperties.ResourceAvailableUnits);
				int total = 0;
	            if (a != null) {
					total = a.getIntValue();
	                drt.setAvailableUnits(a.getIntValue());
	            }
	            
	          	logger.info("discoverTypes: " + domain + " rt=" + drt.getResourceType() + " available units=" + drt.getAvailableUnits());
	            domainResources.addResource(drt);
	            BitSet bSet = instance.getControllerAssignedLabel().get(domain);
				a = rpd.getAttribute(ResourceProperties.ResourceNdlAbstractDomain);
				if (a != null) {
					logger.debug("Found abstract model for resource pool: " + type);
					//System.out.println("\n"+a.getValue());
	                String model;
	                try {
	                	model = CompressEncode.decodeDecompress(a.getValue());
	                } catch (DataFormatException dfe) {
	                	// maybe it is not compressed so just keep going
	                	model = a.getValue();
	                }
	                model = updateModel(model, total,bSet);
					abstractModels.add(model);
				}
			} catch (ConfigurationException e) {
				logger.error("discoverTypes(): Could not process discover types response", e);
			}
		}
	}
	
	private String updateModel(String model_str,int total, BitSet bSet){
		try {
			Domain domain=new Domain();
			model_str=domain.updateAbstactModel(model_str,total,bSet);
		} catch (NdlException ee) {
			ee.printStackTrace();
		}
		return model_str;
	}
	
	/**
	 * Check against a list of patterns
	 * @param dn
	 * @return
	 * @throws Exception
	 */
	private boolean checkList(String dn, String fileName) throws Exception {
		
		// read the list. it is inefficient, but will not require
		// restarts if users are added/removed at runtime
		
		File f = new File(fileName);
		
		UserWhitelist uwl = new UserWhitelist(f);
	
		return uwl.onWhiteList(dn);
	}
	
	/**
	 * Check this user against an optional blacklist and a mandatory whitelist (in that order)
	 * @param dn
	 * @return
	 */
	private boolean checkWhitelist(String dn) throws Exception {
		
		if (dn == null)
			return false;
		
		// first blacklist
		String patFileName = OrcaController.getProperty(XmlRpcController.PropertyControllerBlackListFile);
		if (patFileName == null){
			logger.info("No " +  XmlRpcController.PropertyControllerBlackListFile + " property is specified in XMLRPC controller properties, skipping");
		} else {
			File bl = new File(patFileName);
			if (bl.exists() && checkList(dn, patFileName))
				// user blacklisted
				return false;
		}
		
		patFileName = OrcaController.getProperty(XmlRpcController.PropertyControllerWhiteListFile);
		
		if (patFileName == null){
			throw new Exception("No " +  XmlRpcController.PropertyControllerWhiteListFile + " property is specified in XMLRPC controller properties");
		}
		
		return checkList(dn, patFileName);
	}
	
	/**
	 * This function runs a memory check. Returns true if memory state is OK, false otherwise.
	 * @return
	 */
	private boolean checkMemory(Double mThresh) {
		try {
			File checkFile = new File(OrcaController.getProperty(XmlRpcController.PropertyControllerDisableMemoryCheckFile));
			if (checkFile.exists()) {
				logger.info("checkMemory(): Memory check is disabled, proceeding");
				return true;
			}
		} catch (Exception e) {
			logger.error("checkMemory(): unable to test for presence of disable file " + 
					OrcaController.getProperty(XmlRpcController.PropertyControllerDisableMemoryCheckFile));
		}
		
		Double thresh = mThresh;
		try {
			thresh = Double.parseDouble(OrcaController.getProperty(XmlRpcController.PropertyControllerMemoryThreshold));
		} catch (Exception e) {
			logger.error("checkMemory(): Unable to parse memory threshold value, proceeding");
			return true;
		}
		
		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); 

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		
		if (((double)heapSize)/((double)heapMaxSize) > thresh) {
			logger.info("checkMemory(): Memory usage exceeds threshold: " + heapSize + "/" + heapMaxSize + " t = " + thresh);
			return false; 
		}
		
		return true;
	}
	
}