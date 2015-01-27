package orca.controllers.xmlrpc;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.CredentialException;
import javax.xml.bind.DatatypeConverter;

import orca.ndl.Device;
import orca.network.InterCloudHandler;
import orca.network.ReservationConverter;
import orca.security.AbacUtil;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.RPCException;
import orca.shirako.common.ResourceType;
import orca.shirako.common.SliceID;
import orca.shirako.container.ConfigurationException;
import orca.shirako.container.Globals;
import orca.shirako.core.BrokerPolicy;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.meta.QueryProperties;
import orca.shirako.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.meta.ResourcePoolDescriptor;
import orca.shirako.meta.ResourcePoolsDescriptor;
import orca.shirako.meta.ResourceProperties;
import orca.shirako.plugins.substrate.ISubstrateDatabase;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.util.ID;

import org.apache.log4j.Logger;

public class OrcaXmlrpcHandler extends XmlrpcHandlerHelper implements IOrcaXmlrpcInterface {

	private static final String NOOP_HANDLER_STRING = "/handlers/common/noop.xml";

	protected final IServiceManager sm;
	protected final XmlrpcController controller;
	protected final Logger logger;
	
	protected ResourcePoolsDescriptor pools;
	
	protected final String root;
	protected final String noopConfigFile;
	
	public static final String PropertyRequestNdl = "request.ndl";
	public static final String PropertyDefaultBrokerName = "xmlrpc.controller.defaultBroker";
	public static final String PropertyOrcaCredentialVerification = "orca.credential.verification.required";

	/**
	 * Maps a domain to the resources available from that domain.
	 */
	protected HashMap<String, DomainResourceTypes> typesMap = new HashMap<String, DomainResourceTypes>();
	protected List<String> abstractModels = new ArrayList<String>();

	protected HashMap<ID, ResourceRequest> requests = new HashMap<ID, ResourceRequest>();

	protected final XmlrpcOrcaState instance;
	protected boolean discoveredTypes = false;
	protected boolean verifyCredentials = true;

	public OrcaXmlrpcHandler() {

		//discoverTypes(); // to populate abstractModels and typesMap
		/* discoverTypes() will be called before xmlrpc method invocation (looks like only createSliver needs it);
		 * this is to account for new resource pools added to authority actors after controller has started up
		 * as of 02/26/11
		 */

		instance = XmlrpcOrcaState.getInstance();
		
		logger = instance.getSM().getLogger();
		
		sm = instance.getSM();
		controller = instance.getController();
		
		noopConfigFile = Globals.LocalRootDirectory + NOOP_HANDLER_STRING;
		root = Globals.getContainer().getPackageRootFolder(XmlrpcControllerConstants.PackageId);
		
		if (XmlrpcController.getProperty(PropertyOrcaCredentialVerification) != null)
			verifyCredentials = new Boolean(XmlrpcController.getProperty(PropertyOrcaCredentialVerification));
		else 
			verifyCredentials = true;
	}

	/**
	 * Returns the geni AM api version 
	 * @return
	 */
	public Map<String, Object> getVersion() {
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("orca_api", 1);
		result.put("implementation", "ORCA");
		return (result);
	}

	/**
	 * Returns resource representations for all resources across all AMs
	 * @return
	 */

	public String listResources(Object[] credentials, Map<?,?> options) {
		try {
			logger.info("ORCA API listResources() invoked");
			
			validateOrcaCredential(null, credentials, null, verifyCredentials, logger);
			
			// Call discoverTypes to populate abstractModels
			try {
				discoverTypes(true);
			} catch (Exception ex) {
				logger.error("listResources(): discoverTypes() failed to populate abstractModels: " + ex);
				return "ERROR:  discoverTypes() failed to populate abstractModels";
			}

			String rspecResult = " ";
			int siteIndex = 0;

			for (String str : abstractModels) {
				// Convert each abstract ndl to rspec
				// rspecResult += convertAbstractNdlToRspec(str);
				//logger.debug("Current abstract model: \n " + str);
				rspecResult += str;
				rspecResult += "********************************************************* [" + siteIndex + "] \n";
				siteIndex++;
			}

			rspecResult += "There are " + siteIndex + " available resource domains \n" ;

			return rspecResult;
		} catch (CredentialException ce) {
			logger.error("listResources(): Credential Exception: " + ce.getMessage());
			return "ERROR: CredentialException encountered: " + ce.getMessage();
		} catch (Exception oe) {
			logger.error("listResources(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return "ERROR: Exception encountered: " + oe;
		} 

	}

	private List<String> getUserKeys(Map<String, ?> user) {
		Object keyObject = user.get("keys");
		if (keyObject instanceof String) {
			String sshkey = (String) keyObject;
			return java.util.Collections.singletonList(sshkey);
		} else if (keyObject instanceof Object[]) {
			Object[] keyArray = (Object[]) keyObject;
			List<String> result = new ArrayList<String>(keyArray.length);
			for (Object sshkey : keyArray) {
				result.add((String) sshkey);
			}
			return result;
		} else {
			// Unknown type, return an empty list
			return java.util.Collections.emptyList();
		}
	}

	/***
	 * create a slice.
	 * @param slice_urn - user-specified slice name
	 * @param credentials (ignored)
	 * @param resReq (NDL request)
	 * @param users (user ids and ssh key strings)
	 * @return
	 */
	public String createSlice(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users) {

		try {
			logger.info("ORCA API createSlice() invoked");
			
			validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);
			
			// check slice name - don't allow duplicates
			if (XmlrpcOrcaState.getInstance().getSliceID(slice_urn) != null)
				return "ERROR: duplicate slice urn " + slice_urn;

			// First get the sm, controller and slice objects

			// old way - get the SM slice
			//slice = instance.getSlice(); 
			// new way generate new slice
			ISlice slice = (ISlice) SliceFactory.getInstance().create(slice_urn);
			try {
				sm.registerSlice(slice);
			} catch (Exception e) {
				logger.error("createSlice(): Unable to register slice " + slice_urn + ": " + e);
				return "ERROR: unable to register slice " + slice_urn;
			}


			//populate typesMap and abstractModels
			try {
				discoverTypes(true);
			} catch (Exception ex) {
				logger.error("createSlice(): discoverTypes() failed to populate typesMap and abstractModels: " + ex);
				return "ERROR: discoverTypes() failed to populate typesMap and abstractModels";
			}

			String sshKey = null;

			if (users.size() > 0) {
				// if users array not empty, take a user key of the first element
				// expected is an array of structs with fields named 'urn' and 'keys'
				// where urn is a string and keys is also a string containint public key
				for (Map<String,?> user : users) {
					// String urn = (String) user.get("urn");
					List<String> userKeys = getUserKeys(user);
					if (userKeys.isEmpty()) {
						sshKey = ReservationConverter.NO_SSH_KEY_SPECIFIED_STRING;
					} else {
						sshKey = userKeys.get(0);
					}
				}
			}

			//logger.debug("Inside createSliver: Got resource request as " + resReq);

			// protoGENI adapter converts RSpec to NDL, here we assume NDL already
			String ndlRequest = resReq;

			ResourceRequest r = new ResourceRequest();
			r.requestId = new ID();

			InterCloudHandler h = makeHandler();
			h.getDomainResourcePools(pools);
			
			Hashtable <String,LinkedList <Device> > con = h.handleCloudRequest(new ByteArrayInputStream(ndlRequest.getBytes()));

			//instance.setHandler(h);
			instance.mapHandler(slice_urn,h);
			//Set up the term from the request NDL
			String term_start=h.getMapper().startTime;
			String term_end=h.getMapper().endTime;
			long termDuration = h.getMapper().termDuration;
			java.util.Date cal_start_date=null,cal_end_date=null;

			Term term=null;
			ActorClock clock = sm.getActorClock();
			if (term_start == null){
				long cycle = sm.getCurrentCycle();
				cal_start_date = clock.date(cycle);
			}
			else {
				try {
					cal_start_date = DatatypeConverter.parseDateTime(term_start).getTime();
					cal_end_date = DatatypeConverter.parseDateTime(term_end).getTime();
				} catch (IllegalArgumentException ie) {
					logger.error("Unable to parse request term start or end date: " + ie);
					return "ERROR: request term start or end date is invalid: " + ie;
				}
				if (cal_end_date.before(cal_start_date)) {
					return "ERROR: request end date before start date";
				}
			}
			if (termDuration == 0){
				termDuration = clock.getMillis(60 * 60 * 12);
			}
			else {
				termDuration = clock.getMillis(termDuration);
			}
			if (term_end != null){
				term = new Term(cal_start_date,cal_end_date);
			}
			else{
				term = new Term(cal_start_date, termDuration);
			}

			// obtain the list of reservations.
			// this method also sets the dependencies between reservations
			//r.listInterDomainReservations = getReservations(h, term);

			ReservationConverter converter = new ReservationConverter(logger);
			if (converter == null) {
				logger.error("createSlice(): Failed to create ReservationConverter");
				return ("ERROR: Failed to create ReservationConverter");
			}

			// HACK ALERT:
			//  The converted code requires a map of (domain->resourcetype). However, we no longer store
			//  the resource mapping in this form. We cannot pass the actual map, since it contains objects
			//  that are defined in this project and that would make the network project dependent on this
			//  project. The right solution (probably) is to pull the converter from the network project into this 
			//  project. 
			//  For now, the mapping is recreated before every call.

			HashMap<String, ResourceType> hackMap = new HashMap<String, ResourceType>();
			for (DomainResourceTypes drs : typesMap.values()) {
				hackMap.put(drs.getDomain(), drs.getDefaultResource().getResourceType());
			}

			r.listInterDomainReservations = converter.getReservations(h, hackMap, sshKey, term, sm, slice);

			if(r.listInterDomainReservations == null) {
				logger.error("createSlice(): No reservations created for this request");
				return("ERROR: No reservations created for the request");
			}
			else
				logger.debug("This request created " + r.listInterDomainReservations.size() + " reservations");

			Iterator<IServiceManagerReservation> it = r.listInterDomainReservations.iterator();
			while (it.hasNext()) {
				try {
					IServiceManagerReservation currRes = (IServiceManagerReservation) it.next();
					logger.debug("Issuing demand for reservation: " + currRes.getReservationID().toString());
					if(AbacUtil.verifyCredentials)
						setAbacAttributes(currRes, logger);
					sm.demand(currRes);
				} catch (Exception e) {
					logger.error("createSlice(): Exception, failed to demand reservation" + e);
					return "ERROR: Exception, failed to demand reservation" + e;
				}
			}

			requests.put(r.requestId, r);            

			// What do we return in the manifest ? reservation Id, type, units ? slice ?
			String result = "Here are the leases: \n";

			it = r.listInterDomainReservations.iterator();
			result = "Request id: " + r.requestId + "\n";
			while (it.hasNext()) {
				IServiceManagerReservation currRes = (IServiceManagerReservation) it.next();
				result += "[ "
					+ "  Slice UID: " + currRes.getSliceID().toString()
					+ " | Reservation UID: " + currRes.getReservationID().toString()
					+ " | Resource Type: " + currRes.getApprovedType().toString()
					+ " | Resource Units: " + currRes.getApprovedUnits()
					+ " ] \n"
					;
				// Map slice_urn onto ORCA slice ID (it is the same across multiple reservations)
				XmlrpcOrcaState.getInstance().mapUrn(slice_urn, currRes.getSliceID());
			}

			logger.debug(result);

			return result;
		} catch (CredentialException ce) {
			logger.error("createSlice(): Credential Exception: " + ce.getMessage());
			return "ERROR: CredentialException encountered: " + ce.getMessage();
		} catch (Exception oe) {
			logger.error("createSlice(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return "ERROR: Exception encountered: " + oe;
		} 
	}

	/**
	 * Returns the status of the reservations in the input slice
	 * @param slice_urn
	 * @return
	 */
	public String sliceStatus(String slice_urn, Object[] credentials) {
		try {
			IReservation[] allRes = null;
			String result;
			logger.info("ORCA API sliceStatus() invoked");
			
			validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "info"}, verifyCredentials, logger);
			
			try {
				allRes = getAllSliceReservations(instance, slice_urn, credentials);
			} catch (Exception e) {
				logger.error("sliceStatus(): Exception encountered for " + slice_urn + ": " + e);
				return "ERROR: unable to get slice status for " + slice_urn;
			}

			InterCloudHandler h=instance.getHandler(slice_urn);

			if(allRes == null){
				result = "ERROR: Invalid slice " + slice_urn + ", slice status can't be determined";
				logger.error("sliceStatus(): Invalid slice " + slice_urn  + ", slice status can't be determined");
				return result;
			}
			else{
				logger.debug("There are " + allRes.length + " reservations in the slice with sliceId = " + slice_urn + ":"+ sm.getShirakoPlugin().getDatabase().hashCode());
				if(allRes.length <= 0){
					result = "ERROR: There are no reservations in the slice with sliceId = " + slice_urn;
					return (result);
				}
				boolean ready = true;
				result = " ";
				for (int j=0; j<allRes.length; j++ ){
					result += "************************************************************* \n"
						+"[ "
						+ " Reservation UID: " + allRes[j].getReservationID().toString()
						+ " | Resource Type: " + allRes[j].getApprovedType().toString()
						+ " | Units: " + allRes[j].getApprovedUnits()
						+ " | Status: " + allRes[j].getReservationState().getStateName()
						+ " ] \n";

					if(!(allRes[j].getReservationState().getStateName().equalsIgnoreCase("Active"))){
						ready = false;
					}
				}

				if(!ready) {
					result += "Overall Sliver Status: pending \n";
				}
				else{
					result += "Overall Sliver Status: ready \n";
				}

				//get manifest NDL representation

				ReservationConverter converter = new ReservationConverter(logger);

				if (converter == null) {
					logger.error("sliceStatus(): failed to create ReservationConverter");
					return "ERROR: Failed to create ReservationConverter";
				}
				else{
					logger.debug("ReservationConverter is initiated!");
				}
				try{
					result += converter.getManifest(h,allRes,(ISubstrateDatabase) sm.getShirakoPlugin().getDatabase());
				}catch(Exception e){
					logger.error("sliceStatus(): converter unable to get manifest: " + e);
					return "ERROR: Failed due to exception: " + e;
				}
			}
			return(result);
		} catch (CredentialException ce) {
			logger.error("sliceStatus(): Credential Exception: " + ce.getMessage());
			return "ERROR: CredentialException encountered: " + ce.getMessage();
		} catch (Exception oe) {
			logger.error("sliceStatus(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return "ERROR: Exception encountered: " + oe;
		} 
	}


	/**
	 * Deletes the slices in the slice with input sliceId; Issue close on all underlying reservations
	 * @param sliceId
	 * @return
	 */
	public boolean deleteSlice(String slice_urn, Object[] credentials) {
		try {
			boolean result = false;
			logger.info("ORCA API deleteSlice() invoked");
			
			validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);
			
			SliceID sliceIdReal = XmlrpcOrcaState.getInstance().getSliceID(slice_urn);
			if (sliceIdReal == null)
				return false;

			XmlrpcOrcaState.getInstance().unMapUrn(slice_urn);

			String sliceId = sliceIdReal.toString();

			logger.debug("Got sliceId as " + sliceId.trim());

			ISlice[] slices = (ISlice[]) sm.getSlices();

			if(slices == null){
				logger.error("deleteSlice(): No slices for service manager");
				result = false;
			}
			else{
				for(int i=0; i<slices.length; i++){
					String currSliceId = slices[i].getSliceID().toString();
					String inputSliceId = sliceId.trim();
					if(currSliceId.equalsIgnoreCase(inputSliceId)){
						IReservation[] allRes = (IReservation[]) sm.getReservations(slices[i].getSliceID());
						if(allRes == null){
							result = false;
							logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + sliceId);
						}
						else{
							logger.debug("There are " + allRes.length + " reservations in the slice with urn " + slice_urn + " sliceId = " + sliceId);
							for (int j=0; j<allRes.length; j++ ){
								try {
									logger.debug("Closing reservation with reservation GUID: " + allRes[j].getReservationID().toString());
									if(AbacUtil.verifyCredentials)
										setAbacAttributes(allRes[j], logger);
									sm.close(allRes[j]);
								} catch (Exception ex) {
									result = false;
									throw new RuntimeException("Failed to close reservation", ex);
								}
							}
							result = true;
						}
					}
				}
			}

			try {
				sm.removeSlice(sliceIdReal);
			} catch (Exception e) {
				logger.error("deleteSlice(): Unable to unregister slice " + sliceIdReal.toString() + " for urn " + slice_urn);
				result = false;
			}

			return(result);
		} catch (CredentialException ce) {
			logger.error("deleteSlice(): Credential Exception: " + ce.getMessage());
			return false;
		} catch (Exception oe) {
			logger.error("deleteSlice(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return false;
		} 
	}

	/**
	 * 
	 */
	public boolean renewSlice(String slice_urn, Object[] credentials, String newTermEnd) {
		try { 
			boolean result = false;
			logger.info("ORCA API renewSlice() invoked");
			
			validateOrcaCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"},  verifyCredentials, logger);
			
			SliceID sliceIdReal = XmlrpcOrcaState.getInstance().getSliceID(slice_urn);
			if (sliceIdReal == null)
				return false;

			String sliceId = sliceIdReal.toString();

			logger.debug("Got sliceId as " + sliceId.trim());

			ISlice[] slices = (ISlice[]) sm.getSlices();

			if(slices == null){
				logger.error("renewSlice(): No slices for service manager");
				result = false;
			}
			else{
				for(int i=0; i<slices.length; i++){
					String currSliceId = slices[i].getSliceID().toString();
					String inputSliceId = sliceId.trim();
					if(currSliceId.equalsIgnoreCase(inputSliceId)){
						IReservation[] allRes = (IReservation[]) sm.getReservations(slices[i].getSliceID());
						if(allRes == null){
							result = false;
							logger.debug("No reservations in slice with urn " + slice_urn + " sliceId  " + sliceId);
						}
						else{
							logger.debug("There are " + allRes.length + " reservations in the slice with sliceId = " + sliceId);
							if(allRes.length <= 0){
								result = true; // if there are no reservations in the slice, don't do anything and return success
								return (result);
							}
							for (int j=0; j<allRes.length; j++ ){
								try {
									logger.debug("Extending reservation with reservation GUID: " + allRes[j].getReservationID().toString());
									ActorClock clock = sm.getActorClock();
									long cycle = sm.getCurrentCycle();
									// TODO: Check for validity of newTermEnd
									logger.debug("oldterm.startTime (app) = " + allRes[j].getApprovedTerm().getStartTime() + " | oldterm.endTime (app) = " + allRes[j].getApprovedTerm().getEndTime());
									logger.debug("oldterm.startTime = " + allRes[j].getTerm().getStartTime() + " | oldterm.endTime = " + allRes[j].getTerm().getEndTime());
									logger.debug("oldterm.startTime (req) = " + allRes[j].getRequestedTerm().getStartTime() + " | oldterm.endTime (req) = " + allRes[j].getRequestedTerm().getEndTime());

									Date termEndDate = parseRFC3339Date(newTermEnd.trim());
									Term t = new Term(allRes[j].getTerm().getStartTime(), termEndDate, new Date(allRes[j].getTerm().getEndTime().getTime()+1));
									logger.debug("New start date = " + allRes[j].getTerm().getStartTime() + " | New end date = " + termEndDate);
									IReservation currRes = sm.getReservation(allRes[j].getReservationID());
									if(AbacUtil.verifyCredentials)
										setAbacAttributes(currRes, logger);
									sm.extend(currRes, currRes.getResources(), t);
								} catch (Exception ex) {
									result = false;
									throw new RuntimeException("Failed to extend reservation", ex);
								}
							}
							result = true;
						}
					}
				}
			}

			return result;
		} catch (CredentialException ce) {
			logger.error("renewSlice(): Credential Exception: " + ce.getMessage());
			return false;
		} catch (Exception oe) {
			logger.error("renewSlice(): Exception encountered: " + oe.getMessage());	
			oe.printStackTrace();
			return false;
		} 

	}

	protected void discoverTypes(boolean synchronous) {

		IBrokerProxy brokerProxy = null;
		//IBrokerProxy brokerProxy = sm.getDefaultBroker();
		String brokerName = XmlrpcController.getProperty(PropertyDefaultBrokerName);
		if(brokerName != null) {
			brokerProxy = sm.getBroker(brokerName);
			logger.debug("Selecting my broker as: " + brokerName);
		}
		else {
			brokerProxy = sm.getDefaultBroker();
			logger.debug("Selecting my broker as: " + sm.getDefaultBroker().getName());
		}

		if (brokerProxy == null) 
			throw new RuntimeException("Unable to determine broker proxy for this controller. Please check SM container configuration and logs.");
		
		Properties request = new Properties();
		request.setProperty(QueryProperties.QueryAction, QueryProperties.QueryActionDisctoverPools);
		
		IQueryResponseHandler handler = new IQueryResponseHandler() {
			public void handle(RPCException t, Properties response) {
			    try {
    				if (t != null) {
    					logger.error("discoverTypes(): Could not discover types", t);
    				} else {
    					try {
    						pools = BrokerPolicy.getResourcePools(response);
    						for (ResourcePoolDescriptor rpd : pools) {
    							ResourceType type = rpd.getResourceType();
    							ResourcePoolAttributeDescriptor a = rpd.getAttribute(ResourceProperties.ResourceDomain);
    							if (a == null) {
    								throw new RuntimeException("Missing domain information for resource pool:  " + type);
    							}
    							String domain = a.getValue();
    							// obtain the resource types record for this domain
    							DomainResourceTypes domainResources = typesMap.get(domain);
    							if (domainResources == null) {
    							    domainResources = new DomainResourceTypes(domain);
    							    typesMap.put(domain, domainResources);
    							}
    							// make the resource type record for this resource type
    							DomainResourceType drt = new DomainResourceType(type);
    							// try to obtain the available units
                                a = rpd.getAttribute(ResourceProperties.ResourceAvailableUnits);
                                if (a != null) {
                                    drt.setAvailableUnits(a.getIntValue());
                                }
                                
                                logger.debug("Domain: " + domain + " rt=" + drt.getResourceType() + " available units=" + drt.getAvailableUnits());
                                
                                domainResources.addResource(drt);
                                
    							a = rpd.getAttribute(ResourceProperties.ResourceNdlAbstractDomain);
    							if (a != null) {
    								logger.debug("Found abstract model for resource pool: " + type);
    								//System.out.println("\n"+a.getValue());
    								abstractModels.add(a.getValue());
    							}
    						}
    					} catch (ConfigurationException e) {
    						logger.error("discoverTypes(): Could not process discover types response", e);
    					}
    				}
			    } finally {
                    synchronized(this) {
                        discoveredTypes = true;
                        this.notifyAll();
                    }
			    }
			}
		};

		discoveredTypes = false;
		sm.query(brokerProxy, request, handler);
		if (synchronous) {
		    synchronized(handler) {
		        while (!discoveredTypes) {
		        	try {
		        		handler.wait();
		        	} catch (InterruptedException e) {
		        		;
		        	}
		        }
		    }
		}
	}

	/**
	 * Creates the network handler with the abstract rdf models
	 * @return
	 *
	 */    
	protected InterCloudHandler makeHandler() {
		try {
			InterCloudHandler handler = new InterCloudHandler();
			for (String str : abstractModels) {
				handler.addAbstractDomainString(str);
			}
			handler.abstractModel();
			return handler;
		} catch (IOException e) {
			throw new RuntimeException("Could not create intercloud handler", e);
		}
	}
}
