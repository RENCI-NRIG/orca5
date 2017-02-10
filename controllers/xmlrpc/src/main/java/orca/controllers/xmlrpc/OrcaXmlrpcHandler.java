package orca.controllers.xmlrpc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
//import com.ibm.icu.util.Calendar;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.DataFormatException;

import javax.security.auth.login.CredentialException;

import orca.controllers.OrcaController;
import orca.controllers.OrcaControllerException;
import orca.controllers.xmlrpc.SliceStateMachine.SliceCommand;
import orca.controllers.xmlrpc.geni.GeniAmV2Handler;
import orca.controllers.xmlrpc.geni.IGeniAmV2Interface.GeniStates;
import orca.embed.cloudembed.controller.InterCloudHandler;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.StringProcessor;
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
import orca.manage.beans.ReservationStateMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.beans.UnitMng;
import orca.ndl.INdlModifyModelListener.ModifyType;
import orca.ndl.NdlException;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.IPAddress;
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
import orca.shirako.common.meta.UnitProperties;
import orca.util.CompressEncode;
import orca.util.ID;
import orca.util.ResourceType;
import orca.util.VersionUtils;
import orca.util.password.hash.OrcaPasswordHash;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

import static orca.manage.OrcaConstants.ReservationStateFailed;


/**
 * ORCA XMLRPC client interface
 * WARNING: Any method you declare public (non-static) becomes a remote method!
 * @author ibaldin
 *
 */
public class OrcaXmlrpcHandler extends XmlrpcHandlerHelper implements IOrcaXmlrpcInterface {
	private static final String WHITELIST_ERROR = "this user is not on the controller's whitelist; system may be in maintenance.";
	public static final String RET_RET_FIELD = "ret";
	public static final String MSG_RET_FIELD = "msg";
	public static final String ERR_RET_FIELD = "err";
	public static final String TICKETED_ENTITIES_FIELD = "ticketedRequestEntities";
	public static final int BASE_RESERVATION_BUILDER_SIZE = 100;
	public static final int PER_RESERVATION_BUILDER_SIZE = 180;

	protected final Logger logger = OrcaController.getLogger(this.getClass().getSimpleName());
	
	protected ResourcePoolsDescriptor pools;
	
	public static final String PropertyXmlrpcControllerUrl = "xmlrpc.controller.base.url";
	public static final String PUBSUB_PROPS_FILE_NAME = "controller.properties";
	public static final String PUBSUB_ENABLED_PROP = "ORCA.publish.manifest";
	public static final String MAX_DURATION_PROP = "controller.max.duration";
	public static final String PropertyPublishManifest = "ORCA.publish.manifest";

	public static final long MaxReservationDuration = ReservationConverter.getMaxDuration();
	
	/**
	 * Maps a domain to the resources available from that domain.
	 */
	protected HashMap<String, SiteResourceTypes> typesMap;
	protected List<String> abstractModels;

	protected final XmlrpcOrcaState instance;
	protected boolean verifyCredentials = true;

	// lock to create slice
	protected static Integer globalStateLock = 0;
		
	/** manage the xmlrpc return structure
	 * 
	 * @param msg
	 * @return
	 */
	private static Map<String, Object> setError(String msg) {
		Map <String, Object> m = new HashMap<String, Object>();
		m.put(ERR_RET_FIELD, true);
		m.put(MSG_RET_FIELD, "ERROR: " + msg);
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

	/**
	 * Adds more details about a reservation createSlice() or modifySlice(),
	 * in a new Map object of ticketRequestEntities.
	 *
	 * @param ret
	 * @param ticketedRequestEntities
	 * @return
	 */
	private static Map<String, Object> setReturn(Object ret, Map<String, Object> ticketedRequestEntities) {
		Map <String, Object> m = setReturn(ret);
		m.put(TICKETED_ENTITIES_FIELD, ticketedRequestEntities);
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
				return setError("discoverTypes() failed to populate abstractModels");
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
			return setError("CredentialException encountered: " + ce.getMessage());
		} catch (Exception oe) {
			logger.error("listResources(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("Exception encountered: " + oe);
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

		logger.info("ORCA API createSlice() invoked");
		
		//Check existing slices, remove the closed ones from XmlrpcOrcaState
		instance.closeDeadSlices();
		
		if (!checkMemory(null)) {
			return setError("low system memory, please try later");
		}
		
		synchronized(globalStateLock) {

			try {
				
				if (!LabelSyncThread.tryLock(LabelSyncThread.getWaitTime())) {
					return setError("system is busy, please try again in a few minutes");
				}

				String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

				// check the whitelist
				if (verifyCredentials && !checkWhitelist(userDN)) 
					return setError(WHITELIST_ERROR);

				if (!verifyCredentials && null == userDN){
					logger.error("Setting userDN to test. This should only happen in Unit Testing.");
					userDN = "test";
				}

				sm = instance.getSM();


				// check slice name - don't allow duplicates
				if (XmlrpcOrcaState.getInstance().getSlice(slice_urn) != null){
					return setError("duplicate slice urn " + slice_urn);
				}


				// generate and register new slice
				SliceMng slice = new SliceMng();
				slice.setName(slice_urn);
				slice.setClientSlice(true);
				SliceID sid = sm.addSlice(slice);

				if (sid == null){
					throw new Exception("Could not create slice: " + sm.getLastError());
				}

				// create XmlrpcSlice object and register with Orca state
				ndlSlice = new XmlrpcControllerSlice(sm, slice, slice_urn, userDN, users, false);
				// we lock the slice from any concurrent modifications
				ndlSlice.lock();
				ndlSlice.getStateMachine().transitionSlice(SliceCommand.CREATE);

				instance.addSlice(ndlSlice);

				// now done on separate thread LabelSyncThread /ib 11/30/15
				//instance.syncTags(sm);
				
				String controller_url = OrcaController.getProperty(PropertyXmlrpcControllerUrl);

				ReservationConverter orc = ndlSlice.getOrc();

				//populate typesMap and abstractModels
				try {
					discoverTypes(sm);
				} catch (Exception ex) {
					logger.error("createSlice(): discoverTypes() failed to populate typesMap and abstractModels: " + ex, ex);
					return setError("discoverTypes() failed to populate typesMap and abstractModels");
				}

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
					return setError("Embedding workflow Exception! " + e);
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
					return setError("No reservations created for the request");
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
						return setError("Exception, failed to demand reservation" + t);
					}
				}

				// call on slicedeferthread to either demand immediately
				// or put on deferred queue
				XmlrpcOrcaState.getSDT().processSlice(ndlSlice);

				StringBuilder result = ndlSlice.getComputedReservationSummary();

				// call getManifest to fully form it (otherwise recovery will fail) /ib
				List<ReservationMng> allRes = ndlSlice.getAllReservations(sm);
				orc.getManifest(workflow.getManifestModel(),
						workflow.getDomainInConnectionList(),
						workflow.getBoundElements(),
						allRes);

				// get Map of requested entities to return to user
				Map<String, Object> ticketedRequestEntities = ndlSlice.getRequestedEntities();


				// call publishManifest if there are reservations in the slice
				if((ndlSlice.getComputedReservations() != null) && (ndlSlice.getComputedReservations().size() > 0)) {
					ndlSlice.publishManifest(logger);
				}

				
				//workflow.closeModel(); //close the substrate model, but would break modifying now

				logger.debug("createSlice(): returning result " + result);
				return setReturn(result.toString(), ticketedRequestEntities);
			} catch (CredentialException ce) {
				logger.error("createSlice(): Credential Exception: " + ce.getMessage());
				return setError("CredentialException encountered: " + ce.getMessage());
			} catch (ReservationConverter.ReservationConverterException re) {
				logger.error("createSlice(): Reservation converter exception: " + re.getMessage());
				return setError("Unable to create reservation due to: " + re.getMessage());
			} catch (Exception oe) {
				logger.error("createSlice(): Exception encountered: " + oe.getMessage());	
				oe.printStackTrace();
				return setError("Exception encountered: " + oe);
			} finally {
				if (sm != null){
					instance.returnSM(sm);
				}
				if (ndlSlice != null) {
					ndlSlice.getWorkflow().syncManifestModel();
					ndlSlice.getWorkflow().syncRequestModel();
					ndlSlice.unlock();
				}
				LabelSyncThread.releaseLock();
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

			allRes = getSliceReservations(instance, slice_urn, userDN, logger);

			if (allRes == null){
				result = "Invalid slice " + slice_urn + ", no reservations in the slice";
				logger.error("sliceStatus(): Invalid slice " + slice_urn  + ", no reservations in the slice");
				return setError(result);
			}
		
			logger.debug("There are " + allRes.size() + " reservations in the slice with sliceId = " + slice_urn);
			if (allRes.size() <= 0) {
				result = "There are no reservations in the slice with sliceId = " + slice_urn;
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

			result = resultSB.toString();
			
			return setReturn(result);
		} catch (CredentialException ce) {
			logger.error("sliceStatus(): Credential Exception: " + ce.getMessage());
			return setError("CredentialException encountered: " + ce.getMessage());
		} catch (OrcaControllerException oce) {
			logger.error("sliceStatus(): ControllerException: " + oce.getMessage());
			return setError("ControllerException encountered: " + oce.getMessage());
		} catch (Exception oe) {
			logger.error("sliceStatus(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return setError("Exception encountered: " + oe);
		} 
	}

	public Map<String, Object> modifySlice(String slice_urn, Object[] credentials, String modReq) {
		XmlrpcControllerSlice ndlSlice = null;
		IOrcaServiceManager sm = null;

		logger.info("ORCA API modifySlice() invoked");
		if (logger.isTraceEnabled()){
			logger.trace("modReq: " + modReq);
		}

		//Check existing slices, remove the closed ones from XmlrpcOrcaState
		instance.closeDeadSlices();
		
		if (!checkMemory(null)) {
			return setError("low system memory, please try later");
		}

		synchronized(globalStateLock) {
			
			try {
				if (!LabelSyncThread.tryLock(LabelSyncThread.getWaitTime())) {
					return setError("system is busy, please try again in a few minutes");
				}
				
				String result_str = null;
				logger.info("ORCA API modifySlice() invoked for " + slice_urn);

				String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

				// check the whitelist
				if (verifyCredentials && !checkWhitelist(userDN)) 
					return setError(WHITELIST_ERROR);

				if (!verifyCredentials && null == userDN){
					logger.error("Setting userDN to test. This should only happen in Unit Testing.");
					userDN = "test";
				}

				if ((modReq == null) || (modReq.length() == 0)) {
					logger.error("modifySlice(): modification request for slice " + slice_urn + " is empty");
					return setError("modification request is empty for slice " + slice_urn);
				}

				sm = instance.getSM();
				
				// find this slice and lock it
				ndlSlice = instance.getSlice(slice_urn);
				if (ndlSlice == null) {
					logger.error("modifySlice(): unable to find slice " + slice_urn + " among active slices");
					return setError("unable to find slice " + slice_urn + " among active slices");
				}
				// lock the slice
				ndlSlice.lock();
				ndlSlice.getStateMachine().transitionSlice(SliceCommand.MODIFY);

				if (!ndlSlice.matchUserDN(userDN)) {
					logger.error("modifySlice(): user " + userDN + " is not owner of slice " + slice_urn);
					return setError("user " + userDN + " is not owner of slice " + slice_urn);
				}
				
				RequestWorkflow workflow = ndlSlice.getWorkflow();

				//populate typesMap and abstractModels
				try {
					discoverTypes(sm);
				} catch (Exception ex) {
					logger.error("modifySlice(): discoverTypes() failed to populate typesMap and abstractModels: " + ex);
					return setError("discoverTypes() failed to populate typesMap and abstractModels");
				}
				DomainResourcePools drp = new DomainResourcePools(); 
				drp.getDomainResourcePools(pools);
				ReservationConverter orc = ndlSlice.getOrc();
				ReservationElementCollection r_collection = orc.getElementCollection();
				if(r_collection==null){
					logger.error("ModifySlice(): No reservations created for this request; Error:no r_collection");
					return setError("ModifySlice Exception! no r_collection");
				}else if(r_collection.NodeGroupMap==null){
					logger.error("ModifySlice(): No reservations created for this request; Error:no NodeGroupMap");
					return setError("ModifySlice Exception! no NodeGroupMap");
				}else if(r_collection.firstGroupElement==null){
					logger.error("ModifySlice(): No reservations created for this request; Error:no firstGroupElement");
					return setError("ModifySlice Exception! no firstGroupElement");
				}
				try{
					workflow.modify(drp, modReq,ndlSlice.getSliceID(), r_collection.NodeGroupMap, r_collection.firstGroupElement);
				}catch(Exception e){
					e.printStackTrace();
					logger.error("ModifySlice(): No reservations created for this request; Error:");
					return setError("ModifySlice Exception!" + e);
				}

				if(workflow.getErrorMsg()!=null){
					logger.error("modifySlice(): No reservations created for this request; Error:"+workflow.getErrorMsg());
					return setError(workflow.getErrorMsg());
				}

				InterCloudHandler ih =  (InterCloudHandler) workflow.getEmbedderAlgorithm();

				// See createSlice() and XmlrpcControllerSlice object for methods.
				// remapping between urn's and slice ids is done by using static methods on
				// XmlrpcControllerSlice object. XmlrpcOrcaState only has one map from slice IDs
				// to XmlrpcControllerSlice objects. These slice objects are locked within each call
				// using a fair semaphore. 
				List<ReservationMng> allRes = null;
				HashMap <String, ReservationMng> allRes_map = null;
				HashMap <String, List<ReservationMng>> m_map = null;
				logger.info("ORCA API modifySlice() started...");

				try {
					allRes = ndlSlice.getAllReservations(sm);
				} catch (Exception e) {
					logger.error("modifySlice(): Exception encountered for " + slice_urn + ": " + e);
					return setError("unable to get reservations in slice status for " + slice_urn);
				} 

				if (allRes == null){
					result_str = "Empty slice " + slice_urn + ", slice status can't be determined";
					logger.error("modifySlice(): Invalid slice " + slice_urn  + ", slice status can't be determined");
					return setError(result_str);
				}
				else {
					
					allRes_map = new HashMap <String, ReservationMng>();
					for(ReservationMng aRes:allRes)
						allRes_map.put(aRes.getReservationID(), aRes);
					if(ndlSlice.getComputedReservations()!=null)
						for(TicketReservationMng cRes:ndlSlice.getComputedReservations())
							cRes.setState(allRes_map.get(cRes.getReservationID()).getState());

					GeniStates geniStates = GeniAmV2Handler.getSliceGeniState(instance, slice_urn);
					orc.updateGeniStates(ndlSlice.getWorkflow().getManifestModel(), geniStates);
					OntModel manifestModel=orc.getManifestModel(workflow.getManifestModel(),
							workflow.getDomainInConnectionList(),
							workflow.getBoundElements(),
							allRes);

					LinkedList <Device> addedDevices = ih.getAddedDevices();
					LinkedList<NetworkElement> l_D = new LinkedList<NetworkElement>();
					for(int i=0;i<addedDevices.size();i++)
						l_D.add((NetworkElement)addedDevices.get(i) );
					LinkedList <Device> modifiedDevices = ih.getModifiedDevices(); 
					LinkedList<NetworkElement> l_M = new LinkedList<NetworkElement>();
					for(int i=0;i<modifiedDevices.size();i++)
						l_M.add((NetworkElement)modifiedDevices.get(i) );		
					m_map=orc.modifyReservations(manifestModel, allRes, typesMap, workflow.getslice(), ih.getModifies(),l_D,l_M);
					List <ReservationMng> extra_ar = ih.modifyStorage(m_map);
					if(extra_ar!=null){
						for(ReservationMng ar: extra_ar){
							ReservationID r_id=new ReservationID(ar.getReservationID());
							logger.debug("redundant reservation:"+r_id);
							sm.closeReservation(r_id);
							sm.removeReservation(r_id);
						}
					}
					ih.modifyComplete(); //clear the modify data.
				}

				logger.info("modifySlice(): processing removed reservations");
				//modifyRemove reserations (remove a interface)
				List <ReservationMng> a_r = m_map.get(ModifyType.MODIFYREMOVE.toString());	
				List <ReservationMng> p_r = m_map.get(ModifyType.REMOVE.toString());
				if (a_r == null || p_r==null) {
					result_str ="No modifyremoved reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID()+";"+a_r+";"+p_r;
					logger.debug("No modifyremoved reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID()+";"+a_r+";"+p_r);
				} else {				
					logger.debug("There are " + a_r.size() + " reservations to be modifyremoved in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());				

					LinkedList <NetworkElement> d_list = ih.getDeviceList();
					HashMap <String, NetworkElement> d_list_guid_map = new HashMap <String, NetworkElement>();
					for(NetworkElement ne:d_list){
						//if(ne.getGUID()!=null)
						//d_list_guid_map.put(ne.getGUID(), ne);
						d_list_guid_map.put(ne.getName(), ne);
					}
					HashMap <String, ReservationMng> p_r_map = new HashMap <String, ReservationMng>();
					for(ReservationMng p_r_m:p_r){
						Properties local = OrcaConverter.fill(p_r_m.getLocalProperties());
						String url=local.getProperty(ReservationConverter.UNIT_URL_RES);
						if(url==null)
							logger.error("No url:"+p_r_m);
						else
							p_r_map.put(url, p_r_m);
					}
					for (ReservationMng rr: a_r){
						try{
							Properties local = OrcaConverter.fill(rr.getLocalProperties());
							Properties config = OrcaConverter.fill(rr.getConfigurationProperties());
							Properties request = OrcaConverter.fill(rr.getRequestProperties());
							Properties resource = OrcaConverter.fill(rr.getResourceProperties());

							String rr_guid = local.getProperty(UnitProperties.UnitURL);
							if(rr_guid==null){
								logger.error("No element url found in the reservation:"+rr.getReservationID());
								continue;
							}

							DomainElement de = (DomainElement) d_list_guid_map.get(rr_guid);
							if(de==null){
								logger.error("No de, guid="+rr_guid);
								continue;
							}
							if(de.getPrecededBy().isEmpty()){
								logger.error("No parent, de="+de.getName());
								continue;
							}
							logger.debug("modifyremove:rr="+rr.getReservationID()+";name="+de.getName()
									+";guid="+rr_guid+";parent size="+de.getPrecededBy().size());
							

							String modifySubcommand = ModifyHelper.ModifySubcommand.REMOVEIFACE.getName();
							
							for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()){
								String parent_prefix = UnitProperties.UnitEthPrefix;
								Properties modifyProperties=new Properties();

								DomainElement pe = parent.getKey();
								String name = pe.getName();
								ReservationMng p_r_m = p_r_map.get(name);

								if(p_r_m==null){
									logger.error("no this parent reservation:"+name);
									continue;
								}
								logger.debug("modifyremove:found parent reservation="+name);
								Properties pr_local=null;
								String isNetwork=null,isLun=null;
								pr_local=OrcaConverter.fill(p_r_m.getLocalProperties());

								isNetwork = pr_local.getProperty(ReservationConverter.PropertyIsNetwork);
								isLun = pr_local.getProperty(ReservationConverter.PropertyIsLUN);

								pr_local=null;
								String host_interface=null;
								String unit_tag = null,unit_parent_url=null;
								if(isNetwork!=null && isNetwork.equals("1")){	//Parent is a networking reservation
									List<UnitMng> un = sm.getUnits(new ReservationID(p_r_m.getReservationID()));
									if (un != null) {
										for (UnitMng u : un) {
											pr_local = OrcaConverter.fill(u.getProperties());										
											if (pr_local.getProperty(UnitProperties.UnitVlanTag) != null)
												unit_tag = pr_local.getProperty(UnitProperties.UnitVlanTag);
											if (pr_local.getProperty(UnitProperties.UnitVlanUrl) != null)
												unit_parent_url = pr_local.getProperty(UnitProperties.UnitVlanUrl);
										}
									}

									host_interface=StringProcessor.getHostInterface(local,unit_parent_url);
									
									if(host_interface==null){	//modify case, properties only in config
										// this likely never happens because even network links added as part of modify
										// have their properties set as if they are new. Also, setting parent_prefix to "modify."
										// is incorrect, since modify properties are modify.[index].suffix, not modify.suffix
										host_interface=StringProcessor.getHostInterface(config,unit_parent_url);
										parent_prefix = UnitProperties.ModifyPrefix;
										// added by me 09/30/16 /ib because I don't think this every happens
										logger.error("Unable to find local properties to perform REMOVEIFACE modify");
										throw new Exception("Unable to find local properties to perform REMOVEIFACE modify");
									}
									
									// commented out as a result of above /ib 09/30/16
									//if(host_interface==null){
									//	logger.warn("Unable to find the parent interface index:unit_tag="+unit_tag+";parent_url="+unit_parent_url);
									//	continue;
									//}

									logger.debug("modifyRemove: host_interface=" + host_interface+";tag=" + unit_tag+";parent url=" + unit_parent_url);

									String parent_tag_name = parent_prefix+host_interface+UnitProperties.UnitEthVlanSuffix;
									modifyProperties.setProperty(UnitProperties.UnitEthVlan, unit_tag);
									/*local.remove(parent_tag_name);
								      config.remove(parent_tag_name);
								      request.remove(parent_tag_name);
								      resource.remove(parent_tag_name);
									 */
									//TODO: code duplication
									String parent_mac_addr = parent_prefix+host_interface+UnitProperties.UnitEthMacSuffix;
									String parent_ip_addr = parent_prefix+host_interface+UnitProperties.UnitEthIPSuffix;
									String parent_quantum_uuid = parent_prefix+host_interface+UnitProperties.UnitEthNetworkUUIDSuffix;
									String parent_interface_uuid = parent_prefix+host_interface + UnitProperties.UnitEthUUID;
									String site_host_interface = parent_prefix + host_interface + UnitProperties.UnitHostEthSuffix;
									String property_parent_netmask = parent_prefix + host_interface + UnitProperties.UnitEthNetmaskSuffix;

									if(config.getProperty(parent_mac_addr)!=null){
										modifyProperties.setProperty(UnitProperties.UnitEthMac, config.getProperty(parent_mac_addr));
										/*local.remove(parent_mac_addr);
									config.remove(parent_mac_addr);
									request.remove(parent_mac_addr);
									resource.remove(parent_mac_addr);*/
									}
									if(config.getProperty(parent_ip_addr)!=null){
										modifyProperties.setProperty(UnitProperties.UnitEthIP,config.getProperty(parent_ip_addr));
										/*local.remove(parent_ip_addr);
									config.remove(parent_ip_addr);
									request.remove(parent_ip_addr);
									resource.remove(parent_ip_addr);*/
									}
									if(config.getProperty(parent_quantum_uuid)!=null){
										modifyProperties.setProperty(UnitProperties.UnitEthNetworkUUID, config.getProperty(parent_quantum_uuid));
										/*local.remove(parent_quantum_uuid);
									config.remove(parent_quantum_uuid);
									request.remove(parent_quantum_uuid);
									resource.remove(parent_quantum_uuid);*/
									}
									if(config.getProperty(parent_interface_uuid)!=null){
										modifyProperties.setProperty(UnitProperties.UnitEthUUID, config.getProperty(parent_interface_uuid));
										/*local.remove(parent_interface_uuid);
									config.remove(parent_interface_uuid);
									request.remove(parent_interface_uuid);
									resource.remove(parent_interface_uuid);*/
									}
									if(config.getProperty(site_host_interface)!=null){
										modifyProperties.setProperty(UnitProperties.UnitHostEth, config.getProperty(site_host_interface));
										/*local.remove(site_host_interface);
									config.remove(site_host_interface);
									request.remove(site_host_interface);
									resource.remove(site_host_interface);*/
									}
									if (config.getProperty(property_parent_netmask) != null){
										String netmask = config.getProperty(property_parent_netmask);
										modifyProperties.setProperty(UnitProperties.UnitEthNetmask, netmask);
										if (logger.isTraceEnabled()){
											logger.trace("modifySlice: copying netmask from config to modifyProperties: " + netmask);
										}
									}
								}

								//parent is lun 
								if(isLun!=null && isLun.equals("1")){	//Parent is a storage reservation
									List<UnitMng> un = sm.getUnits(new ReservationID(p_r_m.getReservationID()));
									if (un != null) {
										for (UnitMng u : un) {
											pr_local = OrcaConverter.fill(u.getProperties());
											if (pr_local.getProperty(UnitProperties.UnitLUNTag) != null)
												unit_tag = pr_local.getProperty(UnitProperties.UnitLUNTag);
										}
									}
									
									if(unit_tag!=null){
										modifyProperties.setProperty(UnitProperties.UnitTargetLun, unit_tag);
										host_interface=StringProcessor.getHostInterface(local,p_r_m);
										if(host_interface==null){	//modify case, properties only in config
											host_interface=StringProcessor.getHostInterface(config,p_r_m);
											parent_prefix = UnitProperties.ModifyPrefix;
										}
										if(host_interface==null){
											logger.warn("Not find the parent interface index:unit_tag="+unit_tag+";parent_url="+unit_parent_url);
											continue;
										}
										logger.debug("isLun="+isLun+";parent unit lun tag:"+unit_tag
												+";parent_prefix:"+parent_prefix
												+";host_interface:"+host_interface);

										//TODO: code duplication
										String parent_tag_name = parent_prefix.concat(host_interface).concat(UnitProperties.UnitEthVlanSuffix);
										String parent_mac_addr = parent_prefix+host_interface+UnitProperties.UnitEthMacSuffix;
										String parent_ip_addr = parent_prefix+host_interface+UnitProperties.UnitEthIPSuffix;
										String site_host_interface = parent_prefix + host_interface + UnitProperties.UnitHostEthSuffix;
										String property_parent_netmask = parent_prefix + host_interface + UnitProperties.UnitEthNetmaskSuffix;

										if(config.getProperty(parent_tag_name)!=null)
											modifyProperties.setProperty(UnitProperties.UnitEthVlan, config.getProperty(parent_tag_name));
										if(config.getProperty(parent_mac_addr)!=null)
											modifyProperties.setProperty(UnitProperties.UnitEthMac, config.getProperty(parent_mac_addr));
										if(config.getProperty(parent_ip_addr)!=null)
											modifyProperties.setProperty(UnitProperties.UnitEthIP, config.getProperty(parent_ip_addr));
										if(config.getProperty(site_host_interface)!=null)
											modifyProperties.setProperty(UnitProperties.UnitHostEth, config.getProperty(site_host_interface));
										if (config.getProperty(property_parent_netmask) != null){
											String netmask = config.getProperty(property_parent_netmask);
											modifyProperties.setProperty(UnitProperties.UnitEthNetmask, netmask);
											if (logger.isTraceEnabled()){
												logger.trace("modifySlice: copying netmask from config to modifyProperties: " + netmask);
											}
										}
									}else{	//no need to go further
										logger.error("Parent did not return the unit lun tag:"+pr_local);
										continue;
									}
								}
								logger.debug("modifycommand:"+modifySubcommand+":properties:"+modifyProperties.toString());
								ModifyHelper.enqueueModify(rr.getReservationID().toString(), modifySubcommand, modifyProperties);

								//rr.setLocalProperties(OrcaConverter.unset(local, rr.getLocalProperties()));
								//rr.setConfigurationProperties(OrcaConverter.unset(config, rr.getConfigurationProperties()));
								//rr.setRequestProperties(OrcaConverter.unset(request, rr.getRequestProperties()));
								//rr.setResourceProperties(OrcaConverter.unset(resource, rr.getResourceProperties()));

							}

						} catch (Exception ex) {
							ex.printStackTrace();
							result_str = "Failed to modifyremove reservation"+ex;
						}
					}
				}
				//remove reservations
				a_r = m_map.get(ModifyType.REMOVE.toString());
				if (a_r == null) {
					result_str ="No removed reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID();
					logger.debug("No removed reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
				} else {			
					for (ReservationMng rr: a_r){
						try{
							instance.releaseAddressAssignment(rr);
							ndlSlice.removeComputedReservations(rr.getReservationID());
							sm.closeReservation(new ReservationID(rr.getReservationID()));
						} catch (Exception ex) {
							ex.printStackTrace();
							result_str = "Failed to close reservation"+ex;
							throw new Exception("Failed to close reservation", ex);
						}
					}
					// please note that computed reservations can be null after recovery /ib
					logger.debug("There are " + a_r.size() 
							+ " reservations to be removed in the slice with urn=" + slice_urn 
							+ " sliceId = " + ndlSlice.getSliceID()
							+ (ndlSlice.getComputedReservations() != null ? "reservations "+ ndlSlice.getComputedReservations().size() : ""));
				}       

				logger.info("modifySlice(): processing add reservations");
				//add reservations
				a_r=m_map.get(ModifyType.ADD.toString());
				if (a_r == null) {
					result_str ="No added reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID();
					logger.debug("No added reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
				}else{ 
					logger.debug("There are " + a_r.size() + " new reservations in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());
					for (ReservationMng rr: a_r){
						try{
							instance.releaseAddressAssignment(rr);

							if (userDN != null) 
								OrcaConverter.setLocalProperty(rr, XmlrpcOrcaState.XMLRPC_USER_DN, userDN.trim());

							logger.debug("Issuing demand for reservation: " + rr.getReservationID().toString());
							if(AbacUtil.verifyCredentials)
								setAbacAttributes(rr, logger);

							ndlSlice.addComputedReservations((TicketReservationMng) rr);
						} catch (Exception ex) {
							result_str = "Failed to redeem reservation"+ex;
							throw new Exception("Failed to redeem reservation", ex);
						}
					}
				}

				orc.updateTerm(workflow.getManifestModel());

				// call on slicedeferthread to either demand immediately
				// or put on deferred queue
				XmlrpcOrcaState.getSDT().processSlice(ndlSlice);

				logger.debug("modifySlice(): modifying existing reservations");
				//modify existing reservations
				a_r=m_map.get(ModifyType.MODIFY.toString());
				if (a_r == null) {
					result_str ="No modified reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID();
					logger.debug("No modified reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
				} else {
					logger.debug("There are " + a_r.size() + " modified reservations in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());
					for (ReservationMng rr: a_r){
						try{
							//instance.releaseAddressAssignment(rr);

							if (userDN != null) 
								OrcaConverter.setLocalProperty(rr, XmlrpcOrcaState.XMLRPC_USER_DN, userDN.trim());

							logger.debug("Issuing modify demand for reservation: " + rr.getReservationID().toString());
							if(AbacUtil.verifyCredentials)
								setAbacAttributes(rr, logger);
							String sliver_guid = rr.getReservationID();

							Properties local = OrcaConverter.fill(rr.getLocalProperties());
							//String unit_url = local.getProperty(ReservationConverter.UNIT_URL_RES);
							//String modify_ver = local.getProperty(ReservationConverter.PropertyModifyVersion);
							//String element_guid = local.getProperty(ReservationConverter.PropertyElementGUID);
							// for testing - add a status watch for this reservation
							//List<ReservationIDWithModifyIndex> actList = 
							//		Collections.<ReservationIDWithModifyIndex>singletonList(new ReservationIDWithModifyIndex(new ReservationID(sliver_guid), Integer.valueOf(modify_ver)));
							//local.list(System.out);

							ReservationDependencyStatusUpdate rr_depend = new ReservationDependencyStatusUpdate();
							rr_depend.setReservation(rr);
							List <ReservationID> rr_l = Collections.<ReservationID>singletonList(new ReservationID(sliver_guid));
							List <ReservationID> rr_d_list = new ArrayList<ReservationID>();

							//get properties to get its parent reservations

							String  p_str = local.getProperty(ReservationConverter.PropertyNumExistParentReservations);
							logger.debug("addActiveStatuWatch:numExistParent="+p_str);
							int p = 0;
							String r_id=null;
							if(p_str!=null){
								p=Integer.valueOf(p_str);
								for(int i=0;i<p;i++){
									String key=ReservationConverter.PropertyExistParent + String.valueOf(i);
									r_id=local.getProperty(key);
									if(r_id!=null)
										rr_d_list.add(new ReservationID(r_id));
								}
							}
							p_str = local.getProperty(ReservationConverter.PropertyNumNewParentReservations);
							logger.debug("addActiveStatuWatch:numNewParent="+p_str);
							if(p_str!=null){
								p=Integer.valueOf(p_str);
								for(int i=0;i<p;i++){
									String key=ReservationConverter.PropertyNewParent + String.valueOf(i);
									r_id=local.getProperty(key);
									if(r_id!=null)
										rr_d_list.add(new ReservationID(r_id));
								}
							}
							logger.debug("addActiveStatuWatch:"+rr_d_list+";;self="+rr_l);
							XmlrpcOrcaState.getSUT().addActiveStatusWatch(rr_d_list,rr_l, rr_depend);

							//String modifySubcommand = null;
							//List<Map<String, ?>> modifyProperties=null;
							//ret = modifySliver(slice_urn, sliver_guid, credentials, 
							//	modifySubcommand, modifyProperties);

						} catch (Exception ex) {
							result_str = "Failed to redeem reservation"+ex;
							throw new Exception("Failed to redeem reservation", ex);
						}
					}

				}
				StringBuilder result = ndlSlice.getComputedReservationSummary();

				// call getManifest to fully form it (otherwise recovery will fail) /ib
				allRes = ndlSlice.getAllReservations(sm);
				orc.getManifest(workflow.getManifestModel(),
						workflow.getDomainInConnectionList(),
						workflow.getBoundElements(),
						allRes);

				String errMsg = workflow.getErrorMsg();
				result.append((errMsg == null ? "No errors reported" : errMsg));

				// get Map of requested entities to return to user
				Map<String, Object> ticketedRequestEntities = ndlSlice.getRequestedEntities();


				// update published manifest
				if((ndlSlice.getComputedReservations() != null) && (ndlSlice.getComputedReservations().size() > 0)) {
					ndlSlice.updatePublishedManifest(logger);
				}


				logger.debug("modifySlice(): returning result " + result);
				return setReturn(result.toString(), ticketedRequestEntities);
			} catch (CredentialException ce) {
				logger.error("modifySlice(): Credential Exception: " + ce.getMessage());
				return setError("CredentialException encountered: " + ce.getMessage());
			} catch (Exception oe) {
				logger.error("modifySlice(): Exception encountered: " + oe.getMessage());       
				oe.printStackTrace();
				return setError("Exception encountered: " + oe);
			}
			finally {
				if (sm != null)
					instance.returnSM(sm);

				if (ndlSlice != null) {
					ndlSlice.getWorkflow().syncManifestModel();
					ndlSlice.getWorkflow().syncRequestModel();
					ndlSlice.unlock();
				}

				LabelSyncThread.releaseLock();
			}
		}

	}

	/**
	 * Permit stitching by other slices to this sliver with password. This call does not check
	 * the type of the sliver, only its existence within the given slice and 
	 * simply puts the hashed password onto properties of the reservation. 
	 * @param slice_urn
	 * @param sliver_guid
	 * @param pass
	 * @param credentials
	 * @return
	 */
	public Map<String, Object> permitSliceStitch(String slice_urn, String sliver_guid, String pass, Object[] credentials) {
    	IOrcaServiceManager sm = null;
    	XmlrpcControllerSlice ndlSlice = null;

    	logger.info("ORCA API permitSliceStitch() invoked for " + sliver_guid + " of slice " + slice_urn);

    	if (sliver_guid == null) 
    		return setError("permitSliceStitch() sliver_guid is null");
    	try {
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
    		sm = instance.getSM();
    		
            // find this slice and lock it
            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
            	logger.error("permitSliceStitch(): unable to find slice " + slice_urn + " among active slices");
            	return setError("unable to find slice " + slice_urn + " among active slices");
            }
            
            // lock the slice
            ndlSlice.lock();
           
            ReservationMng rmng = sm.getReservation(new ReservationID(sliver_guid));
            
            if (rmng == null) {
            	logger.error("permitSliceStitch(): unable to find reservation " + sliver_guid + " in slice " + slice_urn);
            	return setError("unable to find reservation " + sliver_guid + " in slice " + slice_urn);
            }
            
            
			if (!ndlSlice.matchUserDN(userDN)) {
				logger.error("permitSliceStitch(): user " + userDN + " is not owner of slice " + slice_urn);
				return setError("user " + userDN + " is not owner of slice " + slice_urn);
			}
            
            if (!validateSliverListSlice(ndlSlice.getAllReservations(sm), Collections.singletonList(sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + sliver_guid + " is not part of slice " + slice_urn);
            	return setReturn("Reservation " + sliver_guid + " is not part of slice " + slice_urn);
            }

            if (rmng.getState() != OrcaConstants.ReservationStateActive)  {
            	logger.error("permitSliceStitch(): reservation " + rmng.getReservationID() + " is not in Active state: " + 
            			rmng.getState() + ", unable to revoke stitching permission");
            	return setError("reservation " + rmng.getReservationID() + " is not in Active state: " + 
            			rmng.getState() + ", unable to revoke stitching permission");
            }
            
            // compute hash of password and set the property of the reservation
            String hash = OrcaPasswordHash.generatePasswordHash(pass);
            
            Properties lp = new Properties();
            lp.setProperty(UnitProperties.SliceStitchPass, hash);
            lp.setProperty(UnitProperties.SliceStitchAllowed, UnitProperties.YES);
            
            addLocalProperties(sm, rmng, UnitProperties.SliceStitchPrefix, lp);
            
            return setReturn(true);
    	} catch (Exception e) {
    		logger.error("permitSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));	
    		e.printStackTrace();
    		return setError("permitSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));
    	} finally {
    		if (sm != null){
    			instance.returnSM(sm);
    		}
    		if (ndlSlice != null) {
				ndlSlice.getWorkflow().syncManifestModel();
				ndlSlice.getWorkflow().syncRequestModel();
    			ndlSlice.unlock();
    		}
    	}

	}

	public Map<String, Object> revokeSliceStitch(String slice_urn, String sliver_guid, Object[] credentials) {
    	IOrcaServiceManager sm = null;
    	XmlrpcControllerSlice ndlSlice = null;

    	logger.info("ORCA API revokeSliceStitch() invoked for " + sliver_guid + " of slice " + slice_urn);

    	if (sliver_guid == null) 
    		return setError("revokeSliceStitch() sliver_guid is null");
    	try {
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
    		sm = instance.getSM();
    		
            // find this slice and lock it
            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
            	logger.error("revokeSliceStitch(): unable to find slice " + slice_urn + " among active slices");
            	return setError("revokeSliceStitch() unable to find slice " + slice_urn + " among active slices");
            }
            
            // lock the slice
            ndlSlice.lock();
           
            ReservationMng rmng = sm.getReservation(new ReservationID(sliver_guid));
            
            if (rmng == null) {
            	logger.error("revokeSliceStitch(): unable to find reservation " + sliver_guid + " in slice " + slice_urn);
            	return setError("unable to find reservation " + sliver_guid + " in slice " + slice_urn);
            }
            
			if (!ndlSlice.matchUserDN(userDN)) {
				logger.error("revokeSliceStitch(): user " + userDN + " is not owner of slice " + slice_urn);
				return setError("user " + userDN + " is not owner of slice " + slice_urn);
			}
            
            if (!validateSliverListSlice(ndlSlice.getAllReservations(sm), Collections.singletonList(sliver_guid))) {
            	logger.error("revokeSliceStitch(): reservation " + sliver_guid + " is not part of slice " + slice_urn);
            	return setReturn("Reservation " + sliver_guid + " is not part of slice " + slice_urn);
            }
            
            if (rmng.getState() != OrcaConstants.ReservationStateActive)  {
            	logger.error("revokeSliceStitch(): reservation " + rmng.getReservationID() + " is not in Active state: " + 
            			rmng.getState() + ", unable to revoke stitching permission");
            	return setError("reservation " + rmng.getReservationID() + " is not in Active state: " + 
            			rmng.getState() + ", unable to revoke stitching permission");
            }
            
            Properties lp = new Properties();
            lp.setProperty(UnitProperties.SliceStitchAllowed, UnitProperties.NO);
            
            addLocalProperties(sm, rmng, UnitProperties.SliceStitchPrefix, lp);
            
            return setReturn(true);
    	} catch (Exception e) {
    		logger.error("revokeSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));	
    		e.printStackTrace();
    		return setError("revokeSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));
    	} finally {
    		if (sm != null){
    			instance.returnSM(sm);
    		}
    		if (ndlSlice != null) {
				ndlSlice.getWorkflow().syncManifestModel();
				ndlSlice.getWorkflow().syncRequestModel();
    			ndlSlice.unlock();
    		}
    	}
	}

	/**
	 * Perform stitching from one slice to another. The caller is the owner of the 'from_slice', using 'to_pass' password to connect
	 * to sliver on 'to_slice'. 
	 * This call validates that from_sliver and to_sliver are of type node and link (or link and node) and that the 'to_sliver' has
	 * a hashed password that matches the provided 'to_pass'. It then computes the necessary properties and invoked the modify on the
	 * node to add an interface for the corresponding link.
	 * @param from_slice_urn
	 * @param from_sliver_guid
	 * @param to_slice_urn
	 * @param to_sliver_guid
	 * @param to_pass
	 * @param node_properties
	 * @param credentials
	 * @return
	 */
	public Map<String, Object> performSliceStitch(String from_slice_urn, String from_sliver_guid, String to_slice_urn, String to_sliver_guid, String to_pass, 
			Map<String, ?> node_properties, Object[] credentials) {
    	IOrcaServiceManager sm = null;
    	XmlrpcControllerSlice fromNdlSlice = null, toNdlSlice = null;

    	logger.info("ORCA API performSliceStitch() invoked for " + from_sliver_guid + " of slice " + from_slice_urn);

    	
    	if ((from_slice_urn == null) || (from_sliver_guid == null) || (to_slice_urn == null) || (to_sliver_guid == null))
    		return setError("performSliceStitch() slice identifier or sliver guid is null");

		if (from_slice_urn.equals(to_slice_urn)) {
			logger.error("performSliceStitch(): cannot stitch slice " + from_slice_urn + " to itself");
			return setError("cannot stitch slice " + from_slice_urn + " to itself");
		}
		
		if (from_sliver_guid.equals(to_sliver_guid)) {
			logger.error("performSliceStitch(): cannot stitch slice " + from_slice_urn + " to itself");
			return setError("cannot stitch slice " + from_slice_urn + " to itself");
		}
    	try {
    		// check we own the 'from' slice
			String userDN = validateOrcaCredential(from_slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
    		sm = instance.getSM();
    		
            // find the from slice 
            fromNdlSlice = instance.getSlice(from_slice_urn);
            if (fromNdlSlice == null) {
                    logger.error("performSliceStitch(): unable to find 'from' slice " + from_slice_urn + " among active slices");
                    return setError("unable to find 'from' slice " + from_slice_urn + " among active slices");
            }
            
            toNdlSlice = instance.getSlice(to_slice_urn);
            if (toNdlSlice == null) {
                    logger.error("performSliceStitch(): unable to find 'to' slice " + from_slice_urn + " among active slices");
                    return setError("unable to find 'to' slice " + from_slice_urn + " among active slices");
            }
            if (from_slice_urn.compareTo(to_slice_urn) > 0) {
                // lock both slices in order of their names to avoid race conditions (and remember to unlock)
                fromNdlSlice.lock();
                toNdlSlice.lock();
            } else {
            	toNdlSlice.lock();
            	fromNdlSlice.lock();
            }
    		
			if (!fromNdlSlice.matchUserDN(userDN)) {
				logger.error("performSliceStitch(): user " + userDN + " is not owner of slice " + from_slice_urn);
				return setError("user " + userDN + " is not owner of slice " + from_slice_urn);
			}
            
            if (!validateSliverListSlice(toNdlSlice.getAllReservations(sm), Collections.singletonList(to_sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + to_sliver_guid + " is not part of slice " + to_slice_urn);
            	return setReturn("Reservation " + to_sliver_guid + " is not part of slice " + to_slice_urn);
            }
            
            if (!validateSliverListSlice(fromNdlSlice.getAllReservations(sm), Collections.singletonList(from_sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + from_sliver_guid + " is not part of slice " + from_slice_urn);
            	return setReturn("Reservation " + from_sliver_guid + " is not part of slice " + from_slice_urn);
            }

            // determine which is the node, and which is the network, get local properties
            ReservationMng fromRes = sm.getReservation(new ReservationID(from_sliver_guid));
            ReservationMng toRes = sm.getReservation(new ReservationID(to_sliver_guid));
            ReservationMng netRes = null, nodeRes = null;
            
            Properties fromLocal = null, toLocal = null, nodeLocal = null, netLocal = null;
            
            fromLocal = OrcaConverter.fill(fromRes.getLocalProperties());
            toLocal = OrcaConverter.fill(toRes.getLocalProperties());
            
            if (UnitProperties.ONE.equals(fromLocal.getProperty(ReservationConverter.PropertyIsNetwork))) {
            	netRes = fromRes;
            	netLocal = fromLocal;
            }
            
            if (UnitProperties.ONE.equals(fromLocal.getProperty(ReservationConverter.PropertyIsVM))) {
            	nodeRes = fromRes;
            	nodeLocal = fromLocal;
            }
            
            if (UnitProperties.ONE.equals(toLocal.getProperty(ReservationConverter.PropertyIsNetwork))) {
            	netRes = toRes;
            	netLocal = toLocal;
            }
            
            if (UnitProperties.ONE.equals(toLocal.getProperty(ReservationConverter.PropertyIsVM))) {
            	nodeRes = toRes;
            	nodeLocal = toLocal;
            }
            
            if ((nodeRes == null) || (netRes == null)) {
            	logger.error("performSliceStitch(): unable to clearly identify node and network between " + from_sliver_guid + " and " + 
            			to_sliver_guid + ", unable to perform the stitch");
            	return setError("unable to clearly identify node and network between " + from_sliver_guid + " and " + 
            			to_sliver_guid + ", unable to perform the stitch");
            }
            
            if ((nodeRes.getState() != OrcaConstants.ReservationStateActive) || (netRes.getState() != OrcaConstants.ReservationStateActive)) {
            	logger.error("performSliceStitch(): one of stitching reservations " + nodeRes.getReservationID() + "/" + 
            			netRes.getReservationID() + " is not in Active state: " + nodeRes.getState() + "/" + netRes.getState() + 
            			", unable to stitch slices " + from_slice_urn + " and " + to_slice_urn);
            	return setError("one of stitching reservations " + nodeRes.getReservationID() + "/" + 
            			netRes.getReservationID() + " is not in Active state: " + nodeRes.getState() + "/" + netRes.getState() + 
            			", unable to stitch slices " + from_slice_urn + " and " + to_slice_urn);
            }
            
            // compare domains
            String netDomain = XmlrpcHandlerHelper.getShortDomain(netRes);
            String nodeDomain = XmlrpcHandlerHelper.getShortDomain(nodeRes);
            if ((netDomain == null) || (nodeDomain == null)) {
            	logger.error("performSliceStitc(): unable to determine domain of net or node reservation");
            	return setError("unable to determine domain of net or node reservation");
            }
            if ((netDomain != null) && (!netDomain.equals(nodeDomain))) {
            	logger.error("performSliceStitch(): domain mismatch  in to/from reservations");
            	return setError("domain mismatch in to/from reservations");
            }
            
            // Verify authorization to stitch on the 'to' reservation.
            boolean allowed = false;
            if (UnitProperties.YES.equals(toLocal.getProperty(UnitProperties.SliceStitchPrefix + UnitProperties.DOT + UnitProperties.SliceStitchAllowed))) {
            	String storedPass = toLocal.getProperty(UnitProperties.SliceStitchPrefix + UnitProperties.DOT + UnitProperties.SliceStitchPass);
            	if (OrcaPasswordHash.validatePassword(to_pass, storedPass)) 
            		allowed = true;
            }
            
            if (!allowed) {
            	logger.error("performSliceStitch(): stitch to " + to_sliver_guid + " was not authorized or password is invalid");
            	return setError("stitch to " + to_sliver_guid + " was not authorized or password is invalid");
            }
            
            String activeStitchGuid = findActiveStitch(nodeLocal, netRes.getReservationID());
            if (activeStitchGuid != null) {
            	logger.error("performSliceStitch(): stitch to " + to_sliver_guid + " already active with guid " + activeStitchGuid+ ", exiting");
            	return setError("stitch to " + to_sliver_guid + " already active");
            }
            
            List<UnitMng> un = sm.getUnits(new ReservationID(netRes.getReservationID()));
            String unitTag = null, unitQuantumUUID = null;
        	Properties modifyProperties = new Properties();
            if (un != null) {
            	for (UnitMng u : un) {
            		Properties uP = OrcaConverter.fill(u.getProperties());										
            		unitTag = uP.getProperty(UnitProperties.UnitVlanTag);
            		unitQuantumUUID = uP.getProperty(UnitProperties.UnitQuantumNetUUID);
            	}
            
            	// generate mac address, copy IP address from properties if available, vlan.tag from unit properties
            	// get quantum UUID (local) and hosteth (host interface) (unit, not available in emulation) from the network reservation 
            	// (not same as done for slice modify, since this code does not use NDL models 
            	
                // meta data properties. issue unique guid to this stitch operation
                String stitchGuid = UUID.randomUUID().toString();
            	
            	modifyProperties.setProperty(UnitProperties.UnitEthMac, ReservationConverter.generateNewMAC(instance));
            	
            	if (node_properties.containsKey(UnitProperties.UnitEthIP)) {
            		if (IPAddress.validateCIDR((String)node_properties.get(UnitProperties.UnitEthIP)))
            			modifyProperties.setProperty(UnitProperties.UnitEthIP, (String)node_properties.get(UnitProperties.UnitEthIP));
            		else {
            			logger.error("performSliceStitch(): ip address is not properly formatted: " + 
            					(String)node_properties.get(UnitProperties.UnitEthIP) + ", unable to perform stitch");
        				return setError("ip address is not properly formatted: " + 
            					(String)node_properties.get(UnitProperties.UnitEthIP) + ", unable to perform stitch");
            		}
            	}
            	
            	if (netLocal.getProperty(UnitProperties.UnitQuantumNetname) != null)
            		modifyProperties.setProperty(UnitProperties.UnitHostEth, netLocal.getProperty(UnitProperties.UnitQuantumNetname));
            	
            	if (unitQuantumUUID != null)
            		modifyProperties.setProperty(UnitProperties.UnitEthNetworkUUID, unitQuantumUUID);
            	
            	if (unitTag != null)
            		modifyProperties.setProperty(UnitProperties.UnitEthVlan, unitTag);
            	
            	modifyProperties.setProperty(UnitProperties.SliceStitchUUID, stitchGuid);

            	logger.debug("performSliceStitch(): saving stitching metadata on both reservations " + nodeRes.getReservationID() + ", " + netRes.getReservationID());

            	Properties sp = new Properties();
                
            	SimpleDateFormat s = new SimpleDateFormat(RFC3399_DATE_FORMAT);//spec for RFC3339
        		s.setTimeZone(TimeZone.getTimeZone("UTC"));
            	
                sp.setProperty(UnitProperties.SliceStitchPerformed, s.format(new Date()));
                
                sp.setProperty(UnitProperties.SliceStitchToReservation, nodeRes.getReservationID());
                sp.setProperty(UnitProperties.SliceStitchToSlice, nodeLocal.getProperty(UnitProperties.UnitSliceName));
                sp.setProperty(UnitProperties.SliceStitchDN, nodeLocal.getProperty(UnitProperties.UserDN));
                
                addLocalProperties(sm, netRes, UnitProperties.SliceStitchPrefix + UnitProperties.DOT + stitchGuid, sp);
                
                sp.setProperty(UnitProperties.SliceStitchToReservation, netRes.getReservationID());
                sp.setProperty(UnitProperties.SliceStitchToSlice, netLocal.getProperty(UnitProperties.UnitSliceName));
                sp.setProperty(UnitProperties.SliceStitchDN, netLocal.getProperty(UnitProperties.UserDN));
                
                addLocalProperties(sm, nodeRes, UnitProperties.SliceStitchPrefix + UnitProperties.DOT + stitchGuid, sp);
                
                logger.info("performSliceStitch(): enqueuing modify operation on node " + nodeRes.getReservationID());
                ModifyHelper.enqueueModify(nodeRes.getReservationID(), ModifyHelper.ModifySubcommand.ADDIFACE.getName(), modifyProperties);
                
                return setReturn(true);
			} else {
				// no units - this shouldn't happen
				logger.error("performSliceStitch(): no units found on the net/vlan reservation " + netRes.getReservationID() + " unable to perform stitch");
				return setError("no units found on the net/vlan reservation " + netRes.getReservationID() + " unable to perform stitch");
			}

    	} catch (Exception e) {
    		logger.error("performSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));	
    		e.printStackTrace();
    		return setError("performSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));
    	} finally {
    		if (sm != null){
    			instance.returnSM(sm);
    		}
    		if (fromNdlSlice != null) {
				fromNdlSlice.getWorkflow().syncManifestModel();
				fromNdlSlice.getWorkflow().syncRequestModel();
    			fromNdlSlice.unlock();
    		}
    		if (toNdlSlice != null) {
				toNdlSlice.getWorkflow().syncManifestModel();
				toNdlSlice.getWorkflow().syncRequestModel();
    			toNdlSlice.unlock();
    		}
    	}

	}
	
	/**
	 * Undo previously create stitch. No password required, can be initiated by either side of the stitch.
	 * @param from_slice_urn
	 * @param from_sliver_guid
	 * @param to_slice_urn
	 * @param to_sliver_guid
	 * @param credentials
	 * @return
	 */
	public Map<String, Object> undoSliceStitch(String from_slice_urn, String from_sliver_guid, String to_slice_urn, String to_sliver_guid, Object[] credentials) {
		
    	IOrcaServiceManager sm = null;
    	XmlrpcControllerSlice fromNdlSlice = null, toNdlSlice = null;

    	logger.info("ORCA API undoSliceStitch() invoked for " + from_sliver_guid + " of slice " + from_slice_urn);
    	
    	if ((from_slice_urn == null) || (from_sliver_guid == null) || (to_slice_urn == null) || (to_sliver_guid == null))
    		return setError("undoSliceStitch() slice identifier or sliver guid is null");

		if (from_slice_urn.equals(to_slice_urn)) {
			logger.error("undoSliceStitch(): cannot unstitch slice " + from_slice_urn + " from itself");
			return setError("cannot unstitch slice " + from_slice_urn + " from itself");
		}
		
		if (from_sliver_guid.equals(to_sliver_guid)) {
			logger.error("undoSliceStitch(): cannot unstitch slice " + from_slice_urn + " from itself");
			return setError("cannot unstitch slice " + from_slice_urn + " from itself");
		}
    	try {
    		// check we own the 'from' slice
			String userDN = validateOrcaCredential(from_slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
    		sm = instance.getSM();
    		
            // find the from slice 
            fromNdlSlice = instance.getSlice(from_slice_urn);
            if (fromNdlSlice == null) {
                    logger.error("undoSliceStitch(): unable to find 'from' slice " + from_slice_urn + " among active slices");
                    return setError("unable to find 'from' slice " + from_slice_urn + " among active slices");
            }
            
            toNdlSlice = instance.getSlice(to_slice_urn);
            if (toNdlSlice == null) {
                    logger.error("undoSliceStitch(): unable to find 'to' slice " + from_slice_urn + " among active slices");
                    return setError("unable to find 'to' slice " + from_slice_urn + " among active slices");
            }
            if (from_slice_urn.compareTo(to_slice_urn) > 0) {
                // lock both slices in order of their names to avoid race conditions (and remember to unlock)
                fromNdlSlice.lock();
                toNdlSlice.lock();
            } else {
            	toNdlSlice.lock();
            	fromNdlSlice.lock();
            }
    		
			if (!fromNdlSlice.matchUserDN(userDN)) {
				logger.error("undoSliceStitch(): user " + userDN + " is not owner of slice " + from_slice_urn);
				return setError("user " + userDN + " is not owner of slice " + from_slice_urn);
			}

            if (!validateSliverListSlice(toNdlSlice.getAllReservations(sm), Collections.singletonList(to_sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + to_sliver_guid + " is not part of slice " + to_slice_urn);
            	return setReturn("Reservation " + to_sliver_guid + " is not part of slice " + to_slice_urn);
            }
            
            if (!validateSliverListSlice(fromNdlSlice.getAllReservations(sm), Collections.singletonList(from_sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + from_sliver_guid + " is not part of slice " + from_slice_urn);
            	return setReturn("Reservation " + from_sliver_guid + " is not part of slice " + from_slice_urn);
            }
            
            // determine which is the node, and which is the network, get local properties
            ReservationMng fromRes = sm.getReservation(new ReservationID(from_sliver_guid));
            ReservationMng toRes = sm.getReservation(new ReservationID(to_sliver_guid));
            ReservationMng netRes = null, nodeRes = null;
            
            Properties fromLocal = null, toLocal = null, nodeLocal = null, netLocal = null, 
            		fromConfig = null, toConfig = null, nodeConfig = null, netConfig = null;
            
            fromLocal = OrcaConverter.fill(fromRes.getLocalProperties());
            toLocal = OrcaConverter.fill(toRes.getLocalProperties());
            fromConfig = OrcaConverter.fill(fromRes.getConfigurationProperties());
            toConfig = OrcaConverter.fill(toRes.getConfigurationProperties());
            
            if (UnitProperties.ONE.equals(fromLocal.getProperty(ReservationConverter.PropertyIsNetwork))) {
            	netRes = fromRes;
            	netLocal = fromLocal;
            	netConfig = fromConfig;
            }
            
            if (UnitProperties.ONE.equals(fromLocal.getProperty(ReservationConverter.PropertyIsVM))) {
            	nodeRes = fromRes;
            	nodeLocal = fromLocal;
            	nodeConfig = fromConfig;
            }
            
            if (UnitProperties.ONE.equals(toLocal.getProperty(ReservationConverter.PropertyIsNetwork))) {
            	netRes = toRes;
            	netLocal = toLocal;
            	netConfig = toConfig;
            }
            
            if (UnitProperties.ONE.equals(toLocal.getProperty(ReservationConverter.PropertyIsVM))) {
            	nodeRes = toRes;
            	nodeLocal = toLocal;
            	nodeConfig = toConfig;
            }
            
            if ((nodeRes == null) || (netRes == null)) {
            	logger.error("undoSliceStitch(): unable to clearly identify node and network between " + from_sliver_guid + " and " + 
            			to_sliver_guid + ", unable to perform the stitch");
            	return setError("unable to clearly identify node and network between " + from_sliver_guid + " and " + 
            			to_sliver_guid + ", unable to perform the stitch");
            }
            
            if (nodeRes.getState() != OrcaConstants.ReservationStateActive) {
            	logger.error("undoSliceStitch(): node stitching reservation " + nodeRes.getReservationID() + 
            			" is not in Active state: " + nodeRes.getState() + 
            			", unable to unstitch slices " + from_slice_urn + " and " + to_slice_urn);
            	return setError("node  stitching reservation " + nodeRes.getReservationID() + 
            			" is not in Active state: " + nodeRes.getState() + 
            			", unable to unstitch slices " + from_slice_urn + " and " + to_slice_urn);
            }
            
            // find the guids and modify indices of all stitch operations performed between these two reservations
            // determine if a stitch is still active
            // if yes, create modify properties to undo, update local properties to indicate unstitch was performed
            // otherwise exit
            String activeStitchGuid = findActiveStitch(nodeLocal, netRes.getReservationID());
            
            if (activeStitchGuid == null) {
            	logger.error("undoSliceStitch(): no active stitches between " + from_sliver_guid + " and " + to_sliver_guid + ", exiting");
            	return setError("no active stitches between " + from_sliver_guid + " and " + to_sliver_guid + ", exiting");
            }
    		
            // find the properties of stitch with this guid on the node, copy them on modify properties and invoke REMOVEIFACE modify
            int index = findModifyIndexForStitch(nodeConfig, activeStitchGuid);
            
            if (index < 0) {
            	logger.error("undoSliceStitch(): unable to find modify operations matching stitch guid " + activeStitchGuid + " between " + 
            			to_sliver_guid + " and " + from_sliver_guid);
            	return setError("unable to find modify operations matching stitch guid " + activeStitchGuid + " between " + 
            			to_sliver_guid + " and " + from_sliver_guid);
            }
            
            logger.debug("undoSliceStitch() getting properties from prefix " + UnitProperties.ModifyPrefix + index);
            
        	Properties modifyProperties = new Properties();
        	
        	if (nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        			UnitProperties.UnitEthMacSuffix) != null)
        		modifyProperties.setProperty(UnitProperties.UnitEthMac, nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        				UnitProperties.UnitEthMacSuffix));
        	
        	if (nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        			UnitProperties.UnitEthIPSuffix) != null)
        		modifyProperties.setProperty(UnitProperties.UnitEthIP, nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        				UnitProperties.UnitEthIPSuffix));
        	
        	if (nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        			UnitProperties.UnitHostEthSuffix) != null)
        		modifyProperties.setProperty(UnitProperties.UnitHostEth, nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        				UnitProperties.UnitHostEthSuffix));
        	
        	if (nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        			UnitProperties.UnitEthNetworkUUIDSuffix) != null) 
        	modifyProperties.setProperty(UnitProperties.UnitEthNetworkUUID, nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        			UnitProperties.UnitEthNetworkUUIDSuffix));
        	
        	if (nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        			UnitProperties.UnitEthVlanSuffix) != null)
        		modifyProperties.setProperty(UnitProperties.UnitEthVlan, nodeConfig.getProperty(UnitProperties.ModifyPrefix + index + 
        				UnitProperties.UnitEthVlanSuffix));
        	
        	modifyProperties.setProperty(UnitProperties.SliceStitchUUID, activeStitchGuid);
        	
        	logger.debug("undoSliceStitch(): saving stitching metadata on both reservations " + nodeRes.getReservationID() + 
        			", " + netRes.getReservationID());
        	
            Properties sp = new Properties();
            
        	SimpleDateFormat s = new SimpleDateFormat(RFC3399_DATE_FORMAT);//spec for RFC3339
    		s.setTimeZone(TimeZone.getTimeZone("UTC"));
    		
            sp.setProperty(UnitProperties.SliceStitchUndone, s.format(new Date()));
            
            addLocalProperties(sm, netRes, UnitProperties.SliceStitchPrefix + UnitProperties.DOT + activeStitchGuid, sp);
            addLocalProperties(sm, nodeRes, UnitProperties.SliceStitchPrefix + UnitProperties.DOT + activeStitchGuid, sp);

        	logger.info("undoSliceStitch(): enqueuing modify operation on node " + nodeRes.getReservationID());
            ModifyHelper.enqueueModify(nodeRes.getReservationID(), ModifyHelper.ModifySubcommand.REMOVEIFACE.getName(), modifyProperties);
 
            return setReturn(true);
    	} catch (Exception e) {
    		logger.error("undoSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));	
    		e.printStackTrace();
    		return setError("undoSliceStitch(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));
    	} finally {
    		if (sm != null){
    			instance.returnSM(sm);
    		}
    		if (fromNdlSlice != null) {
				fromNdlSlice.getWorkflow().syncManifestModel();
				fromNdlSlice.getWorkflow().syncRequestModel();
    			fromNdlSlice.unlock();
    		}
    		if (toNdlSlice != null) {
				toNdlSlice.getWorkflow().syncManifestModel();
				toNdlSlice.getWorkflow().syncRequestModel();
    			toNdlSlice.unlock();
    		}
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

		synchronized(globalStateLock) {
			try {
				
				if (!LabelSyncThread.tryLock(LabelSyncThread.getWaitTime())) {
					return setError("system is busy, please try again in a few minutes");
				}
				
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
					return setError("unable to find slice " + slice_urn + " among active slices");
				}

				// it is safe to close reservations even in the defer queue /ib 10/29/15
				//            if (XmlrpcOrcaState.getSDT().inDeferredQueue(ndlSlice)) {
				//            	logger.error("deleteSlice(): unable to delete slice " + slice_urn + ", it is waiting in the defer queue");
				//            	return setError("ERROR: unable to delete deferred slice " + slice_urn + ", please try some time later");
				//            }

				
				sm = instance.getSM();
				
				// lock the slice
				ndlSlice.lock();

				if (ndlSlice.isDeadOrClosing())
					return setError("slice already closed");
				
				if (!ndlSlice.matchUserDN(userDN)) {
					logger.error("deleteSlice(): user " + userDN + " is not owner of slice " + slice_urn);
					return setError("user " + userDN + " is not owner of slice " + slice_urn);
				}
				

				List<ReservationMng> allRes = ndlSlice.getAllReservations(sm);
				if(allRes == null){
					result = false;
					ndlSlice.getStateMachine().transitionSlice(SliceCommand.DELETE);
					logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
				} else {
					logger.debug("There are " + allRes.size() + " reservations in the slice with urn " + slice_urn + " sliceId = " + ndlSlice.getSliceID());
					for (ReservationMng r : allRes){
						try {
							logger.debug("Closing reservation with reservation GUID: " + r.getReservationID());
							// FIXME: this should be redundant, since we just validated the user_dn and
							// setAbacAttributes is called on createSlice. Moreover, closeReservation uses only the
							// reservationId, not the whole object
							if(AbacUtil.verifyCredentials){
								setAbacAttributes(r, logger);
							}
							sm.closeReservation(new ReservationID(r.getReservationID()));
								instance.releaseAddressAssignment(r);
						} catch (Exception ex) {
							result = false;
							return setError("Failed to close reservation due to " + ex);
						}
					}
					result = true;
				}

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
				if (ndlSlice != null) {
					ndlSlice.getWorkflow().syncManifestModel();
					ndlSlice.getWorkflow().syncRequestModel();
					ndlSlice.unlock();
				}
				
				LabelSyncThread.releaseLock();
			}
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
			logger.info("ORCA API renewSlice() invoked");
			
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
			
			sm = instance.getSM();

			// find this slice and lock it
			ndlSlice = instance.getSlice(slice_urn);
			if (ndlSlice == null) {
				logger.error("renewSlice(): unable to find slice " + slice_urn + " among active slices");
				return setError("unable to find slice " + slice_urn + " among active slices");
			}
			// lock the slice
			ndlSlice.lock();
			
			if (!ndlSlice.matchUserDN(userDN)) {
				logger.error("renewSlice(): user " + userDN + " is not owner of slice " + slice_urn);
				return setError("user " + userDN + " is not owner of slice " + slice_urn);
			}
			
			List<ReservationMng> allRes =  ndlSlice.getAllReservations(sm);
			if(allRes == null){
				ndlSlice.getStateMachine().transitionSlice(SliceCommand.DELETE);
				logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
				return setError("no reservations in slice " + slice_urn + " sliceId " + ndlSlice.getSliceID());
			} 

            if (!ndlSlice.isStableOK() && !ndlSlice.isStableError() && !ndlSlice.isDead()) {
            	logger.info("renewSlice(): unable to extendy slice that is not yet stable, try again later");
            	return setError("unable to extend slice that is not yet stable, try again later");
            }
            
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
			
			List<ReservationMng> failedToExtend = new ArrayList<ReservationMng>();

			logger.debug("There are " + allRes.size() + " reservations in the slice with sliceId = " + ndlSlice.getSliceID());
			Calendar extendedEnd = Calendar.getInstance();
			extendedEnd.setTime(termEndDate);
			for (ReservationMng r : allRes){
				if ((r.getState() == OrcaConstants.ReservationStateClosed) || (r.getState() == ReservationStateFailed))
					continue;
				
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
					throw new Exception("Failed to extend reservation", ex);
				}
			}

			workflow.modifyTerm(termEndDate);
			ReservationConverter orc = ndlSlice.getOrc();
			orc.modifyTerm(workflow.getManifestModel(), workflow.getTerm());

			if (failedToExtend.size() != 0) {
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
				return setError("renewSlice(): " + extMessage);
			}
			
			return setReturn(true);
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
			if (ndlSlice != null) {
				ndlSlice.getWorkflow().syncManifestModel();
				ndlSlice.getWorkflow().syncRequestModel();
				ndlSlice.unlock();
			}
		}
	}
    
     /**
      * Return current and pending states of a sliver in a list of maps of pName, pValue. One map per unit. If sliver_guids list is empty
      * state of all reservations in the slice is returned
      * @param slice_urn
      * @param sliver_guid - list of guids
      * @param credentials
      * @return
      */
     public Map<String, Object> getReservationStates(String slice_urn, List<String> sliver_guids, Object[] credentials) {
    	 IOrcaServiceManager sm = null;
    	 XmlrpcControllerSlice ndlSlice = null;

    	 logger.info("ORCA API getReservationStates() invoked for " + sliver_guids + " of slice " + slice_urn);

    	 if (sliver_guids == null) 
    		 sliver_guids = new ArrayList<String>();

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
    			 return setError("unable to find slice " + slice_urn + " among active slices");
    		 }
    		 // lock the slice
    		 ndlSlice.lock();
 			
 			if (!ndlSlice.matchUserDN(userDN)) {
 				logger.error("getReservationStates(): user " + userDN + " is not owner of slice " + slice_urn);
 				return setError("user " + userDN + " is not owner of slice " + slice_urn);
 			}

    		 List<ReservationMng> allRes =  ndlSlice.getAllReservations(sm);
    		 if(allRes == null){
    			 ndlSlice.getStateMachine().transitionSlice(SliceCommand.DELETE);
    			 logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
    			 return setError("no reservations in slice " + slice_urn + " sliceId " + ndlSlice.getSliceID());
    		 } 
    		 
    		 // if list was empty, populate with all reservations
    		 if (sliver_guids.size() == 0) {
    			 for(ReservationMng rmng: allRes) {
    				 sliver_guids.add(rmng.getReservationID());
    			 }
    		 }
    		 
    		 // check the reservations belong to this slice
    		 if (!validateSliverListSlice(allRes, sliver_guids)) {
    			 logger.error("getReservationStates(): some reservations on the list " + sliver_guids + " are not part of slice " + slice_urn);
    			 return setReturn("Some reservations on the list " + sliver_guids + " are not part of slice " + slice_urn);
    		 }
    		 
    		 List<ReservationStateMng> resStates = ndlSlice.getReservationStates(sm, sliver_guids);
    		 if (resStates == null) {
    			 return setError("getReservationStates(): unable to get states for " + sliver_guids);
    		 }

    		 Map<String, Map<String, String>> res = new HashMap<>();
    		 Iterator<String> resId = sliver_guids.iterator();
    		 Iterator<ReservationStateMng> resState = resStates.iterator();
    		 while(resId.hasNext() && resState.hasNext()) {
    			 Map<String, String> el = new HashMap<>();
    			 ReservationStateMng elState = resState.next();
    			 el.put("reservation.state", OrcaConstants.getReservationStateName(elState.getState()));
    			 el.put("reservation.pending", OrcaConstants.getReservationPendingStateName(elState.getPending()));
    			 res.put(resId.next(), el);
    		 }

    		 return setReturn(res);
    	 } catch (Exception e) {
    		 logger.error("getReservationStates(): Exception encountered: " + e.getMessage());	
    		 e.printStackTrace();
    		 return setError("getReservationStates(): Exception encountered: " + e.getMessage());
    	 } finally {
    		 if (sm != null){
    			 instance.returnSM(sm);
    		 }
    		 if (ndlSlice != null){
 				ndlSlice.getWorkflow().syncManifestModel();
 				ndlSlice.getWorkflow().syncRequestModel();
 				ndlSlice.unlock();
    		 }
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
		
    	logger.info("ORCA API getSliverProperties() invoked for " + sliver_guid + " of slice " + slice_urn);
    	
		if (sliver_guid == null) 
			return setError("getSliverProperties() sliver_guid is null");
		
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
                    return setError("unable to find slice " + slice_urn + " among active slices");
            }
            // lock the slice
            ndlSlice.lock();
			
			if (!ndlSlice.matchUserDN(userDN)) {
				logger.error("getSliverProperties: user " + userDN + " is not owner of slice " + slice_urn);
				return setError("user " + userDN + " is not owner of slice " + slice_urn);
			}
            
            if (!validateSliverListSlice(ndlSlice.getAllReservations(sm), Collections.singletonList(sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + sliver_guid + " is not part of slice " + slice_urn);
            	return setReturn("Reservation " + sliver_guid + " is not part of slice " + slice_urn);
            }
            
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
			if (ndlSlice != null) {
				ndlSlice.getWorkflow().syncManifestModel();
				ndlSlice.getWorkflow().syncRequestModel();
				ndlSlice.unlock();
			}
    	}	
    }
    
    /**
     * Get information about reservation stitches to other slices in the form of map of maps of maps (reservation id, stitching guid, stitching properties)
     * @param slice_urn
     * @param sliver_guids
     * @param credentials
     * @return
     */
    public Map<String, Object> getReservationSliceStitchInfo(String slice_urn, List<String> sliver_guids, Object[] credentials) {
    	IOrcaServiceManager sm = null;
    	XmlrpcControllerSlice ndlSlice = null;
    	Map<String, Object> ret = new HashMap<>();

    	logger.info("ORCA API getReservationSliceStitchInfo() invoked for " + sliver_guids + " of slice " + slice_urn);

    	if (sliver_guids == null) 
    		return setError("getReservationSliceStitchInfo() sliver_guids is null");

    	try {
    		String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);

    		// check the whitelist
    		if (verifyCredentials && !checkWhitelist(userDN)) 
    			return setError(WHITELIST_ERROR);
    		sm = instance.getSM();

    		// find this slice and lock it
    		ndlSlice = instance.getSlice(slice_urn);
    		if (ndlSlice == null) {
    			logger.error("getReservationSliceStitchInfo: unable to find slice " + slice_urn + " among active slices");
    			return setError("unable to find slice " + slice_urn + " among active slices");
    		}
    		// lock the slice
    		ndlSlice.lock();

			if (!ndlSlice.matchUserDN(userDN)) {
				logger.error("getReservationSliceStitchInfo(): user " + userDN + " is not owner of slice " + slice_urn);
				return setError("user " + userDN + " is not owner of slice " + slice_urn);
			}
    		
    		List<ReservationMng> allRes =  ndlSlice.getAllReservations(sm);
    		if(allRes == null){
    			ndlSlice.getStateMachine().transitionSlice(SliceCommand.DELETE);
    			logger.debug("getReservationSliceStitchInfo(): No reservations in slice with urn " + slice_urn + " sliceId  " + ndlSlice.getSliceID());
    			return setError("no reservations in slice " + slice_urn + " sliceId " + ndlSlice.getSliceID());
    		} 
    		
            if (!validateSliverListSlice(ndlSlice.getAllReservations(sm), sliver_guids)) {
            	logger.error("getReservationStates(): Some reservations in list " + sliver_guids + " are not part of slice " + slice_urn);
            	return setReturn("Some reservations in list " + sliver_guids + " are not part of slice " + slice_urn);
            }
            
    		// collect all stitch properties from mentioned reservations
    		for(ReservationMng rmng: allRes) {
    			if (sliver_guids.contains(rmng.getReservationID())) {
        			Map<String, Object> stitchesMap = new HashMap<>();
    				Properties local = OrcaConverter.fill(rmng.getLocalProperties());
    				
    	            // add a state section showing stitching is permitted or not
    				String allowed = local.getProperty(UnitProperties.SliceStitchPrefix + UnitProperties.DOT + UnitProperties.SliceStitchAllowed); 
    	            stitchesMap.put(UnitProperties.SliceStitchAllowed, (allowed == null ? UnitProperties.NO : allowed));

    	            // find stitch history
    				Set<String> stitches = findAllStitches(local);
    				for(String stitchGuid: stitches) {
    					Properties sp = getStitchProperties(local, stitchGuid);
    					Map<String, String> spm = ModifyHelper.fromProperties(sp, UnitProperties.SliceStitchPrefix + UnitProperties.DOT + stitchGuid + UnitProperties.DOT);
    					stitchesMap.put(stitchGuid, spm);
    				}
    				ret.put(rmng.getReservationID(), stitchesMap);
    			} 
    		}
    		
    		return setReturn(ret);
    	} catch (Exception e) {
    		logger.error("getReservationSliceStitchInfo(): Exception encountered: " + e.getMessage());	
    		e.printStackTrace();
    		return setError("getReservationSliceStitchInfo(): Exception encountered: " + e.getMessage());
    	} finally {
    		if (sm != null){
    			instance.returnSM(sm);
    		}
    		if (ndlSlice != null) {
    			ndlSlice.getWorkflow().syncManifestModel();
    			ndlSlice.getWorkflow().syncRequestModel();
    			ndlSlice.unlock();
    		}
    	}	
    }
     
    /**
     * Takes on modify properties as a list of maps. Most times the list need only have one entry of one map, however
     * this way we can have multiple maps, as e.g. for modifying SSH keys
     * @param slice_urn
     * @param sliver_guid
     * @param credentials
     * @param modifySubcommand
     * @param modifyProperties
     * @return
     */
    public Map<String, Object> modifySliver(String slice_urn, String sliver_guid, Object[] credentials, 
    		String modifySubcommand, List<Map<String, ?>> modifyProperties) {
    	IOrcaServiceManager sm = null;
    	XmlrpcControllerSlice ndlSlice = null;

    	logger.info("ORCA API modifySliver() invoked for " + sliver_guid + " of slice " + slice_urn + " subcommand " + modifySubcommand);

    	if (sliver_guid == null) 
    		return setError("modifySliver() sliver_guid is null");
    	try {
			String userDN = validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			// check the whitelist
			if (verifyCredentials && !checkWhitelist(userDN)) 
				return setError(WHITELIST_ERROR);
    		sm = instance.getSM();
    		
            // find this slice and lock it
            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
                    logger.error("modifySliver(): unable to find slice " + slice_urn + " among active slices");
                    return setError("unable to find slice " + slice_urn + " among active slices");
            }
      
            // lock the slice
            ndlSlice.lock();
    		
			if (!ndlSlice.matchUserDN(userDN)) {
				logger.error("modifySliver(): user " + userDN + " is not owner of slice " + slice_urn);
				return setError("user " + userDN + " is not owner of slice " + slice_urn);
			}
            
            if (!validateSliverListSlice(ndlSlice.getAllReservations(sm), Collections.singletonList(sliver_guid))) {
            	logger.error("getReservationStates(): reservation " + sliver_guid + " is not part of slice " + slice_urn);
            	return setReturn("Reservation " + sliver_guid + " is not part of slice " + slice_urn);
            }

            // use the queueing version to avoid collisions with modified performed by the controller itself
            logger.info("modifySliver(): enqueuing modify operation");
            ModifyHelper.enqueueModify(sliver_guid, modifySubcommand, modifyProperties);
            
            return setReturn(true);
    	} catch (Exception e) {
    		logger.error("modifySliver(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));	
    		e.printStackTrace();
    		return setError("modifySliver(): Exception encountered: " + (e.getMessage() != null ? e.getMessage() : e));
    	} finally {
    		if (sm != null){
    			instance.returnSM(sm);
    		}
    		if (ndlSlice != null) {
				ndlSlice.getWorkflow().syncManifestModel();
				ndlSlice.getWorkflow().syncRequestModel();
    			ndlSlice.unlock();
    		}
    	}

    }
		
	protected void discoverTypes(IOrcaServiceManager sm) throws Exception {
		typesMap = new HashMap<String, SiteResourceTypes>();
		abstractModels = new ArrayList<String>();
		
		ID broker = instance.getController().getBroker(sm);
		if (broker == null){
			throw new Exception("Unable to determine broker proxy for this controller. Please check SM container configuration and logs.");
		}

        instance.setBroker(broker.toString());

		List<PoolInfoMng> mypools = sm.getPoolInfo(broker);
		if (mypools == null){
			throw new Exception("Could not discover types: " + sm.getLastError(), sm.getLastError().getException());
		}
		
		pools = new ResourcePoolsDescriptor();
		for (PoolInfoMng pool : mypools) {
			try {
				ResourcePoolDescriptor rpd = OrcaConverter.fill(pool);
				ResourceType type = rpd.getResourceType();
	    		ResourcePoolAttributeDescriptor a = rpd.getAttribute(ResourceProperties.ResourceDomain);
	    		if (a == null) {
	    			throw new Exception("Missing domain information for resource pool:  " + type);
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
	            
	          	logger.debug("discoverTypes: " + domain + " rt=" + drt.getResourceType() + " available units=" + drt.getAvailableUnits());
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
