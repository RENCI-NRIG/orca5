package orca.controllers.xmlrpc.geni;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.CredentialException;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.OrcaXmlrpcHandler;
import orca.controllers.xmlrpc.ReservationConverter;
import orca.controllers.xmlrpc.SliceStateMachine.SliceCommand;
import orca.controllers.xmlrpc.SliceStateMachine.SliceState;
import orca.controllers.xmlrpc.XMLRPCDateTransport;
import orca.controllers.xmlrpc.XmlRpcController;
import orca.controllers.xmlrpc.XmlrpcControllerSlice;
import orca.controllers.xmlrpc.XmlrpcHandlerHelper;
import orca.controllers.xmlrpc.XmlrpcOrcaState;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.ndl.NdlToRSpecHelper;
import orca.ndl.elements.OrcaReservationTerm;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.CompressEncode;
import orca.util.VersionUtils;

import org.apache.xmlrpc.XmlRpcException;

import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;


/**
 * GENI AM API v2 implementation 
 * WARNING: Any method you declare public (non-static) becomes a remote method!
 * @author ibaldin
 *
 */
public class GeniAmV2Handler extends XmlrpcHandlerHelper implements IGeniAmV2Interface {
	private static final String GENI_URN_FIELD = "urn";
	private static final String GENI_KEYS_FIELD = "keys";
	private static final String DEFAULT_EXOSM_URL = "https://geni.renci.org:11443/orca/xmlrpc";	
	public static final String ORCA_XMLRPC_SUFFIX = "xmlrpc";
	public static final String XMLRPC_SUFFIX="geni";

	public static final String PropertyXmlrpcControllerUrl = "xmlrpc.controller.base.url";
	public static final String PropertyUseGeniRootLogin = "use.geni.root.login";
	
	protected final boolean verifyCredentials;
	
	protected final XmlRpcController controller;
	protected final OrcaXmlrpcHandler orcaHandler;
	protected final XmlrpcOrcaState instance;

	protected final String baseUrl;
	protected boolean useGENIRootLogin = true;
	
	public GeniAmV2Handler() {
		if (XmlRpcController.getProperty(PropertyGeniCredentialVerification) != null)
			verifyCredentials = new Boolean(XmlRpcController.getProperty(PropertyGeniCredentialVerification));
		else 
			verifyCredentials = true;
		
		if (XmlRpcController.getProperty(PropertyNdlConverterUrlList) != null)
			NdlConverterUrlList = XmlRpcController.getProperty(PropertyNdlConverterUrlList);
		else
			NdlConverterUrlList = DEFAULT_NDL_CONVERTER_URL_LIST;

		if (XmlRpcController.getProperty(PropertyXmlrpcControllerUrl) != null)
			baseUrl = XmlRpcController.getProperty(PropertyXmlrpcControllerUrl);
		else
			baseUrl = DEFAULT_EXOSM_URL;
		
		// its enough to define it
		if (XmlRpcController.getProperty(PropertyUseGeniRootLogin) != null)
			useGENIRootLogin = false;
		
		orcaHandler = new OrcaXmlrpcHandler();
		instance = XmlrpcOrcaState.getInstance();
		
		logger = OrcaController.Log;
		logger.debug("GENI AM2 XmlrpcHandler constructor called");
		
		controller = instance.getController();

		logger.info("GENI credential verification is turned " + (verifyCredentials ? "ON" : "OFF"));
		logger.info("Using NDL-RSpec converters at " + NdlConverterUrlList);
	}
	
	/**
	 * Create a standard return map
	 * @param code
	 * @param value
	 * @param output
	 * @return
	 */
	private Map<String, Object> getStandardApiReturn(int code, Object value, Object output) {
		Map<String, Object> ret = new HashMap<String, Object>();
		
		Map<String, Object> codes = new HashMap<String, Object>();
		codes.put(ApiReturnFields.CODE_GENI_CODE.name, code);
		ret.put(ApiReturnFields.CODE.name, codes);
		if (value != null)
			ret.put(ApiReturnFields.VALUE.name, value);
		if (output != null)
			ret.put(ApiReturnFields.OUTPUT.name, output);
		return ret;
	}
	
	
//	public Map<String, Object> CreateSliver(String slice_urn,
//			Object[] credentials, String resReq, List<Map<String, ?>> users,
//			Map<String, Object> options) {
//		// FIXME: ignore options
//		return CreateSliver(slice_urn, credentials, resReq, users);
//	}
	
	private Map<String, Object> getSliceManifest(IOrcaServiceManager sm, String slice_urn, Object[] credentials, boolean compressed) {
		// now get the manifest (what's there of it)
		List<ReservationMng> allRes = null;
		String result = "";
		XmlrpcControllerSlice ndlSlice = null;
		
		try {
			ndlSlice = instance.getSlice(slice_urn);
			if (ndlSlice == null) {
				logger.error("getSliceManifest(): Slice " + slice_urn + " does not exist.");
				return getStandardApiReturn(ApiReturnCodes.SEARCHFAILED.code, null, "Invalid slice urn " + slice_urn);
			}
			// lock slice
			ndlSlice.lock();
			ndlSlice.getStateMachine().transitionSlice(SliceCommand.REEVALUATE);
			logger.debug("Slice " + slice_urn + " transitioned to state " + ndlSlice.getStateMachine().getState());

			try {
				allRes = ndlSlice.getAllReservations(sm);
			} catch (Exception e) {
				logger.error("getSliceManifest(): Exception encountered for " +
						slice_urn + ": " + e);
				return getStandardApiReturn(ApiReturnCodes.ERROR.code, null,
						"ERROR: unable to get slice manifest for " + slice_urn);
			}

			if(allRes == null){
				result = "ERROR: Invalid slice " + slice_urn + ", slice status can't be determined";
				logger.error("GENI SliverStatus: Invalid slice " + slice_urn + ", slice status can't be determined");
			}
			else{
				// get manifest NDL representation
				logger.debug("GENI SliverStatus: Collecting slice information");

				if(allRes.size() <= 0) {
					return getStandardApiReturn(ApiReturnCodes.ERROR.code, null,
							"ERROR: slice " + slice_urn + " contains no reservations");
				}
				
				String ndlMan = null;
				GeniStates geniStates = GeniAmV2Handler.getSliceGeniState(instance, slice_urn);
				try {
					ReservationConverter orc = ndlSlice.getOrc();
					orc.updateGeniStates(ndlSlice.getWorkflow().getManifestModel(),
							 geniStates);
					ndlMan = orc.getManifest(ndlSlice.getWorkflow().getManifestModel(),
							ndlSlice.getWorkflow().getDomainInConnectionList(),
							ndlSlice.getWorkflow().getBoundElements(),
							allRes);
				} catch(Exception e) {
					logger.error("getSliceManifest(): converter unable to get manifest: " + e);
					e.printStackTrace();
					return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "ERROR: Failed due to exception: " + e);
				}
				// call ndl converter to get RSpec
				// convert RSpec to NDL using converter

				Map<String, Object> convRes = callConverter(MANIFEST_TO_RSPEC, new Object[]{ndlMan, slice_urn});
				if ((Boolean)convRes.get("err")) {
					logger.error("GENI SliverStatus: Error encountered converting manifest to RSpec: " + (String)convRes.get("msg"));
					return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, 
							"Error encountered converting manifest to RSpec: " + (String)convRes.get("msg"));
				} else {
					result = (String)convRes.get("ret");
				}
				if (compressed)
					result = CompressEncode.compressEncode(result);
			}
			return getStandardApiReturn(ApiReturnCodes.SUCCESS.code, result, null);
		} catch (Exception e) {
			logger.error("getSliceManifest(): Exception " + e);
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null,
					"ERROR: unable to get slice manifest for " + slice_urn);
		} finally {
			if (ndlSlice != null) {
				ndlSlice.getWorkflow().syncManifestModel();
				ndlSlice.getWorkflow().syncRequestModel();
				ndlSlice.unlock();
			}
		}
	}
	
	/**
	 * Turn urns into logins
	 * @param users
	 * @param addRoot - add a root login in addition to other users
	 * @param rootUrn - if addRoot is set, use this urn's ssh key for root. If null, use the first key on the list.
	 * @return
	 */
	private static List<Map<String, ?>> stripLogins(List<Map<String, ?>> users, boolean addRoot, String rootUrn) {
		List<Map<String, ?>> ret = new ArrayList<Map<String, ?>>();
		
		for(Map<String, ?> e: users) {
			String urn = null;
			try {
				urn = (String)e.get(GENI_URN_FIELD);
			} catch (ClassCastException cce) {
				continue;
			}
			if (urn == null)
				continue;
			String pid = PublicId.decodeURN(urn);
			String[] splits = pid.split(" ");
			String login = null;
			try {
				login = splits[splits.length - 1];
			} catch (ArrayIndexOutOfBoundsException ee) {
				continue;
			}
			Map<String, Object> tmp = new HashMap<String, Object>();
			if (e.get(GENI_KEYS_FIELD) == null)
				continue;
			tmp.put(ReservationConverter.LOGIN_FIELD, login);
			tmp.put(ReservationConverter.KEYS_FIELD, e.get(GENI_KEYS_FIELD));
			// GENI expectation is sudo and urn
			tmp.put(ReservationConverter.SUDO_FIELD, "yes");
			tmp.put(ReservationConverter.URN_FIELD, urn);
			ret.add(tmp);
		}
		
		// add main user's SSH key to 'root' login
		if (addRoot && (users.size() > 0)) {
			Map<String, Object> tmp = new HashMap<String, Object>();
			tmp.put(ReservationConverter.LOGIN_FIELD, "root");
			tmp.put(ReservationConverter.SUDO_FIELD, "no");
			
			// find the right SSH key on the list
			int index = 0;
			if (rootUrn != null) {
				// find the urn on the list
				int i = 0;
				for(Map<String, ?> e: users) {
					if (rootUrn.contains(((String)e.get(GENI_URN_FIELD)))) {
						index = i;
						break;
					}
					i++;
				}
			}
			
			Map<String, ?> e = users.get(index);
			if (e.get(GENI_KEYS_FIELD) == null)
				return ret;
			tmp.put(ReservationConverter.KEYS_FIELD, e.get(GENI_KEYS_FIELD));
			ret.add(tmp);
		}
		
		return ret;
	}
	
	public Map<String, Object> CreateSliver(String slice_urn,
			Object[] credentials, String resReq, List<Map<String, ?>> users, Map<String, Object> options) {
		IOrcaServiceManager sm = null;
		try {
			logger.info("GENI AM v2 CreateSliver() invoked for " + slice_urn);
			Date saExpDate = validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, options,
					verifyCredentials, logger);
			
			// compare slice end date to system default and pick earliest
			Calendar systemDefaultEndCal = Calendar.getInstance();
			systemDefaultEndCal.add(Calendar.MILLISECOND, (int)OrcaXmlrpcHandler.MaxReservationDuration);
			Date sliceEndDate = saExpDate;
			if (saExpDate != null) {
				Calendar saExpDateCal = Calendar.getInstance();
				saExpDateCal.setTime(saExpDate);
				if (systemDefaultEndCal.before(saExpDateCal))
					sliceEndDate = systemDefaultEndCal.getTime();
			}
			
			if ((resReq == null) || (resReq.length() == 0))
				return getStandardApiReturn(ApiReturnCodes.BADARGS.code, null, "ERROR: RSpec length 0");

			// convert RSpec to NDL using converter
			String ndlReq;
			Map<String, Object> res;
			if (sliceEndDate == null)
				res = callConverter(RSPEC3_TO_NDL, new Object[]{resReq, DEFAULT_OUTPUT_FORMAT});
			else {
				String sDate = XMLRPCDateTransport.dateToString(new Date());
				String eDate = XMLRPCDateTransport.dateToString(sliceEndDate);
				res = callConverter(RSPEC3_TO_NDL, new Object[]{resReq, sDate, eDate, DEFAULT_OUTPUT_FORMAT});
			}
			
			if ((Boolean)res.get("err")) {
				logger.error("GENI CreateSliver: Error encountered converting RSpec to NDL via converter service: " + 
						(String)res.get("msg"));
				return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, 
						"Error encountered converting RSpec to NDL via converter service: " + (String)res.get("msg"));
			} else
				ndlReq = (String)res.get("ret");

			// submit the request
			Map<String, Object> rr = orcaHandler.createSlice(slice_urn, credentials, ndlReq, stripLogins(users, useGENIRootLogin, XmlrpcHandlerHelper.getCredentialDN(logger)));
			String ret;
			if ((Boolean)rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD))
				ret = (String)rr.get(OrcaXmlrpcHandler.MSG_RET_FIELD);
			else
				ret = (String)rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);
			
			if (ret.contains("ERROR") && ret.contains("busy"))
				return getStandardApiReturn(ApiReturnCodes.BUSY.code, null, ret);
			
			if (ret.contains("ERROR")) {
				return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, ret);
			}

			// now get the manifest (what's there of it)
			sm = instance.getSM();
			return getSliceManifest(sm, slice_urn, credentials, false);
		} catch (CredentialException ce) {
			logger.error("GENI CreateSliver: Credential Exception: " + ce);
			return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null, "Credendial Exception: " + ce);
		} catch (Exception e) {
			logger.error("GENI CreateSliver: Other Exception: " + e);
			e.printStackTrace();
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
		} finally {
			if (sm != null){
				controller.orca.returnServiceManager(sm);
			}
		}
	}

	public Map<String, Object> DeleteSliver(String slice_urn,
			Object[] credentials, Map<String, Object> options) {
		
		try {
			logger.info("GENI AM v2 DeleteSliver() invoked for " + slice_urn);
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, options, verifyCredentials, logger);

			// submit the request
			Map<String, Object> rr = orcaHandler.deleteSlice(slice_urn, credentials);
			boolean ret;
			Object msg = null;
			int code = ApiReturnCodes.SUCCESS.code;
	
			if ((Boolean)rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD)) {
				ret = false;
				String tmsg = (String)rr.get(OrcaXmlrpcHandler.MSG_RET_FIELD);
				if (tmsg.contains("unable to find"))
					code = ApiReturnCodes.SEARCHFAILED.code;
				else
					code = ApiReturnCodes.ERROR.code;
				msg = tmsg;
			}
			else {
				ret = true;
				msg = rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);
			}

			// return the response
			return getStandardApiReturn(code, ret, msg);
		} catch (CredentialException ce) {
			logger.error("GENI DeleteSliver: Credential Exception: " + ce);
			return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null, "Credendial Exception: " + ce);
		} catch (Exception e) {
			logger.error("GENI DeleteSliver: Other Exception: " + e);
			e.printStackTrace();
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
		}
	}

	public Map<String, Object> GetVersion(Map<String, Object> options)
			throws XmlRpcException, Exception {
		return GetVersion();
	}
	
	public Map<String, Object> GetVersion()
			throws XmlRpcException, Exception {
		
		Map<String, Object> value = new HashMap<String, Object>();
	
		value.put(ApiReturnFields.GENI_API.name, 2);
		value.put(ApiReturnFields.ORCA_VERSION.name, VersionUtils.buildVersion);
		value.put(ApiReturnFields.GENI_AM_TYPE.name, "orca");
		
		Map<String, Object> apiVersions = new HashMap<String, Object>();
		// remove /xmlrpc and replace with /geni
		apiVersions.put("2", baseUrl);
		//apiVersions.put("1", baseUrl + "/" + GeniAmV1Handler.XMLRPC_SUFFIX);		
		value.put(ApiReturnFields.VALUE_API_VERSIONS.name, apiVersions);
		
		Object[] reqVersions = new Object[1];
		
		Map<String, Object> geniV3 = new HashMap<String, Object>();
		geniV3.put(ApiReturnFields.VALUE_RSPEC_TYPE.name, "GENI");
		geniV3.put(ApiReturnFields.VALUE_RSPEC_VERSION.name, "3");
		geniV3.put(ApiReturnFields.VALUE_RSPEC_SCHEMA.name, "http://www.geni.net/resources/rspec/3/request.xsd");
		geniV3.put(ApiReturnFields.VALUE_RSPEC_NAMESPACE.name, "http://www.geni.net/resources/rspec/3");
		String[] extensions = new String[3];
		extensions[0] = "http://www.geni.net/resources/rspec/ext/shared-vlan/1";
		extensions[1] = "http://www.geni.net/resources/rspec/ext/postBootScript/1";
		extensions[2] = "http://hpn.east.isi.edu/rspec/ext/stitch/0.1/";
		geniV3.put(ApiReturnFields.VALUE_RSPEC_EXTENSIONS.name, extensions);
		reqVersions[0] = geniV3;
		
//		Map<String, Object> ndl = new HashMap<String, Object>();
//		pgV2.put(ApiReturnFields.VALUE_RSPEC_TYPE.name, "NDL-OWL");
//		pgV2.put(ApiReturnFields.VALUE_RSPEC_SCHEMA.name, "http://geni-orca.renci.org/owl/request.owl");
//		reqVersions[1] = ndl;
		
		value.put(ApiReturnFields.VALUE_REQ_RSPEC_VERSIONS.name, reqVersions);
		
		Object[] adVersions = new Object[1];
		geniV3 = new HashMap<String, Object>();
		geniV3.put(ApiReturnFields.VALUE_RSPEC_TYPE.name, "GENI");
		geniV3.put(ApiReturnFields.VALUE_RSPEC_VERSION.name, "3");
		geniV3.put(ApiReturnFields.VALUE_RSPEC_SCHEMA.name, "http://www.geni.net/resources/rspec/3/ad.xsd");
		geniV3.put(ApiReturnFields.VALUE_RSPEC_NAMESPACE.name, "http://www.geni.net/resources/rspec/3");
		extensions = new String[2];
		extensions[0] = "http://hpn.east.isi.edu/rspec/ext/stitch/0.1/stitch-schema.xsd";
		extensions[1] = "http://www.protogeni.net/resources/rspec/ext/emulab/1/ptop_extension.xsd";
		geniV3.put(ApiReturnFields.VALUE_RSPEC_EXTENSIONS.name, extensions);
		adVersions[0] = geniV3;
		
//		ndl = new HashMap<String, Object>();
//		pgV2.put(ApiReturnFields.VALUE_RSPEC_TYPE.name, "NDL-OWL");
//		pgV2.put(ApiReturnFields.VALUE_RSPEC_SCHEMA.name, "http://geni-orca.renci.org/owl/manifest.owl");
//		adVersions[1] = ndl;
		
		value.put(ApiReturnFields.VALUE_AD_RSPEC_VERSIONS.name, adVersions);
		
		Map<String, Object> topLevel = getStandardApiReturn(ApiReturnCodes.SUCCESS.code, value, null);
		topLevel.put(ApiReturnFields.GENI_API.name, 2);
		
		return topLevel;
	}

	/**
	 * List resources can return resources of the AM or the slice
	 * depending on the options
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> ListResources(Object[] credentials,
			Map<?, ?> options) {
		logger.info("GENI AM v2 ListResources() invoked");
		IOrcaServiceManager sm = null;
		try {			
			// If the option is geni_slice_urn, then list the
			// resources of the slice, otherwise list all
			// pay attention to 'compressed'
			Map<String, Object> op = (Map<String, Object>)options;
			
			String slice_urn = (String)op.get(ApiOptionFields.GENI_SLICE_URN.name);

			// will result be compressed?
			boolean compressed = true;
			if ((op.get(ApiOptionFields.GENI_COMPRESSED.name) != null) &&
					(!(Boolean)op.get(ApiOptionFields.GENI_COMPRESSED.name)))
				compressed = false;

			// will it be for slice or all?
			if (slice_urn != null) {
				validateGeniCredential(slice_urn, credentials, 
						new String[]{"*", "pi", "instantiate", "control"}, op, verifyCredentials, logger);
				// just the slice
				sm = controller.orca.getServiceManager();
				return getSliceManifest(sm, slice_urn, credentials, compressed);
			} else {
				validateGeniCredential(null, credentials, 
						new String[]{"*", "pi", "instantiate", "control", "info", "resolve"}, op, verifyCredentials, logger);
				// all resources
				XmlrpcOrcaState.getInstance().setCompression(compressed);
				return ListResources(credentials);
			}
		} catch (CredentialException ce) {
			logger.error("GENI List Resources: Credential Exception: " + ce);
			return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null, "Credendial Exception: " + ce);
		} catch (Exception e) {
			logger.error("GENI List Resources: Other Exception: " + e);
			e.printStackTrace();
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
		} finally {
			if (sm != null){
				controller.orca.returnServiceManager(sm);
			}
		}
	}
	
	// FIXME: do I need to be public?
        // *** TODO: Do we need this version of ListResources
	public Map<String, Object> ListResources(Object[] credentials) {
		try {
			validateGeniCredential(null, credentials, 
					new String[]{"*", "pi", "instantiate", "control", "info", "resolve"}, null, verifyCredentials, logger);

			Map<String, Object> rr = orcaHandler.listResources(credentials, null);
			
			if ((Boolean)rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD))
				return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, 
						"Error listing resources: " + rr.get(OrcaXmlrpcHandler.MSG_RET_FIELD));
			
			String resAd = (String)rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);  
 

			// submit to converter
			Map<String, Object>res = callConverter(ADS_TO_RSPEC, new Object[]{resAd});
			
			if ((Boolean)res.get("err")) {
				logger.error("GENI ListResources: Error encountered converting NDL ads to RSpec via converter service: " + (String)res.get("msg"));
				return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, 
						"Error encountered converting NDL ads to RSpec via converter service: " + (String)res.get("msg"));
			} 

			// compress if needed
			if (XmlrpcOrcaState.getInstance().getCompression()) {
				return getStandardApiReturn(ApiReturnCodes.SUCCESS.code, CompressEncode.compressEncode((String)res.get("ret")), null);
			} else
				return getStandardApiReturn(ApiReturnCodes.SUCCESS.code, (String)res.get("ret"), null);
			
		} catch (CredentialException ce) {
			logger.error("GENI List Resources: Credential Exception: " + ce);
			return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null, "Credendial Exception: " + ce);
		} catch (Exception e) {
			logger.error("GENI List Resources: Other Exception: " + e);
			e.printStackTrace();
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
		}
	}

        public Map<String, Object> RenewSliver(String slice_urn,
                        Object[] credentials, String newTermEnd, Map<String, Object> options) {

                try {
                        logger.info("GENI AM v2 RenewSliver() invoked for " + slice_urn);
                        Date saExpDate = validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, options, verifyCredentials, logger);

                        // compare slice end date to system default and pick earliest
                        Calendar systemDefaultEndCal = Calendar.getInstance();
                        systemDefaultEndCal.add(Calendar.MILLISECOND, (int)OrcaXmlrpcHandler.MaxReservationDuration);
                        Date sliceEndDate = saExpDate;

                        if (saExpDate != null) {
                                Calendar saExpDateCal = Calendar.getInstance();
                                saExpDateCal.setTime(saExpDate);
                                if (systemDefaultEndCal.before(saExpDateCal))
                                        sliceEndDate = systemDefaultEndCal.getTime();
                        }

                        // check if "geni_extend_alap" option is present
                        boolean extend_alap = false;
                        if(options != null){
                            Boolean is_alap = (Boolean)options.get(ApiOptionFields.GENI_ALAP.name);
                            if(is_alap != null){
                                if(is_alap == true){
                                    extend_alap = true;
                                }
                                else{
                                    extend_alap = false;
                                }
                                logger.info("GENI RenewSliver: geni_extend_alap option found, and set to " + extend_alap);
                            }
                        }

                        
                        // test that new date is not beyond expiration time of the slice or system default
                        Date termEndDate = parseRFC3339Date(newTermEnd.trim());

                        Calendar cal = Calendar.getInstance();
                        if (sliceEndDate != null) {
                                cal.setTime(sliceEndDate);
                                Calendar termEndDateCal = Calendar.getInstance();
                                termEndDateCal.setTime(termEndDate);
                                if (cal.before(termEndDateCal)) { // sliceEndDate is before new term's end date
                                        if(!extend_alap){ // return error if alap option is false
                                            return getStandardApiReturn(ApiReturnCodes.BADARGS.code, null, "Requested new end date is after slice expiration or exceeds system default");
                                        }
                                        else{ // extend as late as possible
                                            logger.info("GENI RenewSliver: geni_extend_alap present; requested newTermEnd = " + newTermEnd);
                                            newTermEnd = getRFC3339String(cal); // cal is the calendar with sliceEndDate
                                            logger.info("GENI RenewSliver: geni_extend_alap present; using newTermEnd = " + newTermEnd);
                                        }
                                }
                        }

                        // At this point, newTermEnd <= sliceEndDate

                        // submit the request
                        Map<String, Object> rr = orcaHandler.renewSlice(slice_urn, credentials, newTermEnd);
                        if ((Boolean)rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD)) {
                                // error has occurred
                                return getStandardApiReturn(ApiReturnCodes.BADARGS.code, null, rr.get(OrcaXmlrpcHandler.MSG_RET_FIELD));
                        }

                        // return the response
                        // return the newTermEnd also in the output
                        return getStandardApiReturn(ApiReturnCodes.SUCCESS.code, true, newTermEnd);
                } catch (CredentialException ce) {
                        logger.error("GENI RenewSliver: Credential Exception: " + ce);
                        return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null, "Credendial Exception: " + ce);
                } catch (Exception e) {
                        logger.error("GENI RenewSliver: Other Exception: " + e);
                        e.printStackTrace();
                        return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
                }
        }                                

	
	public Map<String, Object> Shutdown(String slice_urn, Object[] credentials,
			Map<String, Object> options) {
		
		IOrcaServiceManager sm = null;
		try {
			logger.info("GENI AM v2 Shutdown() invoked for " + slice_urn);
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, options, verifyCredentials, logger);

			boolean result = false;

			String sliceIdReal = XmlrpcControllerSlice.getSliceIDForUrn(slice_urn);
			if (sliceIdReal == null)
				return getStandardApiReturn(ApiReturnCodes.SEARCHFAILED.code, null, 
						"Unable to find a slice with this URN: " + slice_urn);

			String sliceId = sliceIdReal.toString();

			logger.debug("GENI Shutdown: Got sliceId as " + sliceId.trim());
			
			sm = controller.orca.getServiceManager();
			List<ReservationMng> allRes = sm.getReservations(new SliceID(sliceIdReal));
			if(allRes == null){
				throw new Exception("Could not obtain the reservations in  slice with urn " + slice_urn + " sliceId  " + sliceId + " : " + sm.getLastError());
			}

			// FIXME: close these by closeReservations(realSliceId);
			logger.debug("There are " + allRes.size() + " reservations in the slice with sliceId = " + sliceId);
			for (ReservationMng r : allRes){
				try {
					logger.debug("Closing reservation with reservation GUID: " + r.getReservationID());
					// FIXME: is this really necessary?
//					if(AbacUtil.verifyCredentials){
//						setAbacAttributes(r, logger);
//					}
					sm.closeReservation(new ReservationID(r.getReservationID()));
				} catch (Exception ex) {
					result = false;
					throw new RuntimeException("Failed to close reservation", ex);
				}
			}
			result = true;
			return getStandardApiReturn(ApiReturnCodes.SUCCESS.code, result, null);
		} catch (CredentialException ce) {
			logger.error("GENI Shutdown: Credential Exception: " + ce);
			return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null, "Credendial Exception: " + ce);
		} catch (Exception e) {
			logger.error("GENI Shutdown: Other Exception: " + e);
			e.printStackTrace();
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
		} finally {
			if (sm != null){
				controller.orca.returnServiceManager(sm);
			}
		}
	}

	/**
	 * 	public static final int ReservationStateUnknown = 0;
    public static final int ReservationStateNascent = 1;
    public static final int ReservationStateTicketed = 2;
    public static final int ReservationStateActive = 3;
    public static final int ReservationStateActiveTicketed = 4;
    public static final int ReservationStateClosed = 5;
    public static final int ReservationStateCloseWait = 6;
    public static final int ReservationStateFailed = 7;
	 * @param r
	 * @return
	 */
	public static GeniStates getSliverGeniState(ReservationMng r) {
		switch(r.getState()) {
		case OrcaConstants.ReservationStateUnknown:
			return GeniStates.UNKNOWN;
		case OrcaConstants.ReservationStateNascent:
			return GeniStates.CONFIGURING;
		case OrcaConstants.ReservationStateTicketed:
			return GeniStates.CONFIGURING;
		case OrcaConstants.ReservationStateActive:
			return GeniStates.READY;
		case OrcaConstants.ReservationStateActiveTicketed:
			return GeniStates.CONFIGURING;
		case OrcaConstants.ReservationStateClosed:
			return GeniStates.UNKNOWN;
		case OrcaConstants.ReservationStateCloseWait:
			return GeniStates.UNKNOWN;
		case OrcaConstants.ReservationStateFailed:
			return GeniStates.FAILED;
		default:
			return GeniStates.UNKNOWN;
		}
	}
	
	public static GeniStates getSliceGeniState(XmlrpcOrcaState instance, String slice_urn) {
		
        XmlrpcControllerSlice ndlSlice = instance.getSlice(slice_urn);
        if (ndlSlice == null) 
        	return GeniStates.UNKNOWN;
        
        SliceState orcaSliceState = ndlSlice.getStateMachine().getState();
        
        if (orcaSliceState == null)
        	return GeniStates.UNKNOWN;
        
        switch(orcaSliceState) {
        case NULL:
        	return GeniStates.UNKNOWN;
        case CONFIGURING:
        	return GeniStates.CONFIGURING;
        case STABLE_OK:
        	return GeniStates.READY;
        case STABLE_ERROR:
        	return GeniStates.FAILED;
        case CLOSING:
        	return GeniStates.UNKNOWN;
        case DEAD:
        	return GeniStates.UNKNOWN;
        default:
        	return GeniStates.UNKNOWN;
        }

	}

	public Map<String, Object> SliverStatus(String slice_urn,
			Object[] credentials, Map<String, Object> options) {
		
		IOrcaServiceManager sm = null;
		XmlrpcControllerSlice ndlSlice = null;
		try {
			logger.info("GENI AM v2 SliverStatus() invoked for " + slice_urn);
			validateGeniCredential(slice_urn, credentials, new String[]{"*",
					"pi", "instantiate", "control"}, options, verifyCredentials, logger);
			List<ReservationMng> allRes = null;

            ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
            	logger.error("SliverStatus(): invalid slice URN " + slice_urn);
            	return getStandardApiReturn(ApiReturnCodes.SEARCHFAILED.code, null,
            			"ERROR: unable to get slice status for " + slice_urn);
            }
            // lock the slice
            ndlSlice.lock();
            ndlSlice.getStateMachine().transitionSlice(SliceCommand.REEVALUATE);
            logger.debug("Slice " + slice_urn + " transitioned to state " + ndlSlice.getStateMachine().getState());
            
            RequestWorkflow workflow = ndlSlice.getWorkflow();
            OrcaReservationTerm term = workflow.getTerm();
            Date slice_end = term.getEnd(); 
            
			sm = instance.getSM();
			
            try {
            	allRes = ndlSlice.getAllReservations(sm);
            } catch (Exception e) {
            	logger.error("sliverStatus(): Exception encountered for " +
            			slice_urn + ": " + e);
            	return getStandardApiReturn(ApiReturnCodes.ERROR.code, null,
            			"ERROR: unable to get slice status for " + slice_urn);
            }

            if (allRes == null) {
            	logger.error("sliceStatus(): Invalid slice " + slice_urn  + ", slice status can't be determined");
            	return getStandardApiReturn(ApiReturnCodes.SEARCHFAILED.code, null,
            			"ERROR: Invalid slice " + slice_urn + ", slice status can't be determined");
            }
            else {
            	if (allRes.size() <= 0){
            		return getStandardApiReturn(ApiReturnCodes.ERROR.code, null,
            				"ERROR: There are no reservations in the slice with sliceId = "
            				+ slice_urn);
				}

				Map<String, Object> ss = new HashMap<String, Object>();
				ss.put(ApiReturnFields.GENI_URN.name, slice_urn);
				//ss.put(ApiReturnFields.GENI_RESOURCES.name, "");
				// list reservations and their errors
				List<Map<String, Object>> resourceList = new ArrayList<Map<String, Object>>();
				for (ReservationMng r: allRes) {
					Map<String, Object> en = new HashMap<String, Object>();
					//String[] tmpSplit = r.getResourceType().split("\\.");
					//String dom = null;
					//if (tmpSplit.length > 1)
					//	dom = tmpSplit[0];
					//String resType = r.getApprovedType().getType().split(".")[1];
					String resUrl = OrcaConverter.getLocalProperty(r, ReservationConverter.UNIT_URL_RES);
					//String resUrn = NdlToRSpecHelper.sliverUrnFromURL(resUrl, dom);
					// replaced with the following line on 09/03/13 to match NDL converter output
					//String resUrn = NdlToRSpecHelper.cidUrnFromUrl(dom, UrnType.Sliver,  NdlToRSpecHelper.getTrueName(resUrl));
					String resUrn = NdlToRSpecHelper.sliverUrnFromRack(NdlToRSpecHelper.getTrueName(resUrl), NdlToRSpecHelper.getControllerForUrl(baseUrl));
					en.put(ApiReturnFields.GENI_URN.name, resUrn);
					en.put(ApiReturnFields.GENI_STATUS.name, getSliverGeniState(r).name);
					if (r.getState() == OrcaConstants.ReservationStateFailed){ 
						en.put(ApiReturnFields.GENI_ERROR.name, (r.getNotices() != null ? r.getNotices() : "ERROR: no detailed error message available"));
					}else{
						en.put(ApiReturnFields.GENI_ERROR.name, "");
					}
					//en.put(ApiReturnFields.ORCA_EXPIRES.name, (new Date(r.getEnd())).toString()); //ORCA reservation extended end time ??
					
					// if we are extending the ticket, report slice end date, otherwise report core end date
					Date res_end = new Date(r.getEnd());
					if ((r.getState() == OrcaConstants.ReservationStateActive) && (r.getPendingState() == OrcaConstants.ReservationPendingStateExtendingTicket)) {
						res_end = slice_end;
					}
					en.put(ApiReturnFields.ORCA_EXPIRES.name, res_end.toString());
					resourceList.add(en);
				}
				ss.put(ApiReturnFields.GENI_RESOURCES.name, resourceList);
				ss.put(ApiReturnFields.GENI_STATUS.name, getSliceGeniState(instance, slice_urn).name);
				
				return getStandardApiReturn(ApiReturnCodes.SUCCESS.code, ss, null);
			}
		} catch (CredentialException ce) {
			logger.error("GENI SliverStatus: Credential Exception: " + ce);
			return getStandardApiReturn(ApiReturnCodes.FORBIDDEN.code, null,
					"Credendial Exception: " + ce);
		} catch (Exception e) {
			logger.error("GENI SliverStatus: Other Exception: " + e);
			e.printStackTrace();
			return getStandardApiReturn(ApiReturnCodes.ERROR.code, null, "Other Exception: " + e);
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
}
