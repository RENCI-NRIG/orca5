package orca.controllers.xmlrpc.geni;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.CredentialException;

import net.jwhoisserver.utils.InetNetworkException;
import orca.controllers.xmlrpc.OrcaXmlrpcHandler;
import orca.controllers.xmlrpc.XmlrpcController;
import orca.controllers.xmlrpc.XmlrpcHandlerHelper;
import orca.controllers.xmlrpc.XmlrpcOrcaState;
import orca.network.InterCloudHandler;
import orca.network.ReservationConverter;
import orca.security.AbacUtil;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.plugins.substrate.ISubstrateDatabase;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * Implementation of the GENI AM API v1 handler
 * @author ibaldin
 *
 */
public class GeniAmV1Handler extends XmlrpcHandlerHelper implements IGeniAmV1Interface {
	private static final String RSPEC2_TO_NDL = "ndlConverter.requestFromRSpec2";
	private static final String MANIFEST_TO_RSPEC = "ndlConverter.manifestToRSpec2";
	private static final String DEFAULT_NDL_CONVERTER_URL = "http://geni.renci.org:12080/ndl-conversion/";
	private static final String DEFAULT_OUTPUT_FORMAT = "RDF-XML";
	
	public static final String PropertyNdlConverterUrl="ndl.converter.url";
	public static final String PropertyGeniCredentialVerification = "geni.credential.verification.required";
	
	protected final boolean verifyCredentials;
	protected final String NdlConverterUrl;
	
	protected final IServiceManager sm;
	protected final XmlrpcController controller;
	protected final OrcaXmlrpcHandler orcaHandler;
	protected final XmlrpcOrcaState instance;
	protected final Logger logger;
	
	public GeniAmV1Handler() throws Exception {

		if (XmlrpcController.getProperty(PropertyGeniCredentialVerification) != null)
			verifyCredentials = new Boolean(XmlrpcController.getProperty(PropertyGeniCredentialVerification));
		else 
			verifyCredentials = true;
		
		if (XmlrpcController.getProperty(PropertyNdlConverterUrl) != null)
			NdlConverterUrl = XmlrpcController.getProperty(PropertyNdlConverterUrl);
		else
			NdlConverterUrl = DEFAULT_NDL_CONVERTER_URL;

		orcaHandler = new OrcaXmlrpcHandler();
		instance = XmlrpcOrcaState.getInstance();
		
		logger = instance.getSM().getLogger();
		logger.debug("GENI AMv1 XmlrpcHandler constructor called");
		
		sm = instance.getSM();
		controller = instance.getController();

		logger.info("GENI credential verification is turned " + (verifyCredentials ? "ON" : "OFF"));
		logger.info("Using NDL-RSpec converter at " + NdlConverterUrl);
	}

	/**
	 * ProtoGENI compresses and base-64 encodes the output, so lets shall
	 * @param credentials
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public String ListResources(Object[] credentials, Map<?,?> options) {
		
		try {
			logger.info("GENI AM v1 ListResources() invoked");
			
			validateGeniCredential(null, credentials, null, verifyCredentials, logger);

			String res = orcaHandler.listResources(credentials, options);

			// TODO: run via converter

			if (XmlrpcOrcaState.getInstance().getCompression()) {
				return compressEncode(res);
			}

			return res;
		} catch (CredentialException ce) {
			logger.error("GENI List Resources: Credential Exception: " + ce);
			return "Credendial Exception: " + ce;
		} catch (Exception e) {
			logger.error("GENI List Resources: Other Exception: " + e);
			return "Other Exception: " + e;
		}
	}
	
	/**
	 * Convert request from XML to NDL and submit
	 * @param slice_urn
	 * @param credentials
	 * @param resReq
	 * @param users
	 * @return
	 * @throws IOException
	 * @throws InetNetworkException
	 */
	public String CreateSliver(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users) {
		
		try {
			logger.info("GENI AM v1 CreateSliver() invoked");
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

			if ((resReq == null) || (resReq.length() == 0))
				return "ERROR: RSpec length 0";

			// convert RSpec to NDL using converter
			String ndlReq;
			try {
				XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
				config.setServerURL(new URL(NdlConverterUrl));
				XmlRpcClient client = new XmlRpcClient();
				client.setConfig(config);

				logger.debug("GENI CreateSliver: Invoking NDL converter at " + NdlConverterUrl);
				ndlReq = (String)client.execute(RSPEC2_TO_NDL, new Object[]{resReq, DEFAULT_OUTPUT_FORMAT});
			} catch (XmlRpcException e) {
				logger.error("GENI CreateSliver: Error encountered converting RSpec to NDL via converter service " + NdlConverterUrl + ": " + e.toString());
				return "Error encountered converting RSpec to NDL via converter service " + NdlConverterUrl + ": " + e.toString();
			}

			// submit the request
			String ret = orcaHandler.createSlice(slice_urn, credentials, ndlReq, users);
			// return the response

			return ret;
		} catch (CredentialException ce) {
			logger.error("GENI CreateSliver: Credential Exception: " + ce);
			return "Credential Exception: " + ce;
		} catch (Exception e) {
			logger.error("GENI CreateSliver: Other Exception: " + e);
			return "Other Exception: " + e;
		}
	}
	
	/**
	 * 
	 */
	public String SliverStatus(String slice_urn, Object[] credentials) {
		
		try {
			logger.info("GENI AM v1 SliverStatus() invoked");
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "info"}, verifyCredentials, logger);

			IReservation[] allRes = null;
			String result = "";

			try {
				allRes = getAllSliceReservations(instance, slice_urn, credentials);
			} catch (Exception e) {
				return e.getMessage();
			}

			InterCloudHandler h=instance.getHandler(slice_urn);

			if(allRes == null){
				result = "ERROR: Invalid slice " + slice_urn + ", slice status can't be determined";
				logger.error("GENI SliverStatus: Invalid slice " + slice_urn + ", slice status can't be determined");
				return result;
			}
			else{
				//get manifest NDL representation
				logger.debug("GENI SliverStatus: ReservationConverter is being initiated!");
				ReservationConverter converter = new ReservationConverter(logger);

				if (converter == null) {
					logger.error("GENI SliverStatus: Failed to create ReservationConverter");
					return "ERROR: Failed to create ReservationConverter";
				}
				else{
					logger.debug("GENI SliverStatus: ReservationConverter is initiated!");
				}

				String ndlMan = converter.getManifest(h, allRes,(ISubstrateDatabase) sm.getShirakoPlugin().getDatabase());

				// call ndl converter to get RSpec
				// convert RSpec to NDL using converter

				try {
					XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
					config.setServerURL(new URL(NdlConverterUrl));
					XmlRpcClient client = new XmlRpcClient();
					client.setConfig(config);

					logger.debug("GENI SliverStatus: Invoking NDL converter at " + NdlConverterUrl);
					result = (String)client.execute(MANIFEST_TO_RSPEC, new Object[]{ ndlMan, slice_urn});
				} catch (XmlRpcException e) {
					logger.error("GENI SliverStatus: Error encountered converting manifest to RSpec: " + e.toString());
					return "Error encountered converting manifest to RSpec: " + e.toString();
				} catch (MalformedURLException e) {
					logger.error("GENI SliverStatus: Error encountered converting manifest to RSpec: " + e.toString());
					return "Error encountered converting manifest to RSpec: " + e.toString();
				}
			}
			return result;	
		} catch (CredentialException ce) {
			logger.error("GENI SliverStatus: Credential Exception: " + ce);
			return "Credendial Exception: " + ce;
		} catch (Exception e) {
			logger.error("GENI SliverStatus: Other Exception: " + e);
			return "Other Exception: " + e;
		}
	}
	
	public boolean DeleteSliver(String slice_urn, Object[] credentials) {
		
		try {
			logger.info("GENI AM v1 DeleteSliver() invoked");
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

			// submit the request
			boolean ret = orcaHandler.deleteSlice(slice_urn, credentials);

			// return the response
			return ret;
		} catch (CredentialException ce) {
			logger.error("GENI DeleteSliver: Credential Exception: " + ce);
			return false;
		} catch (Exception e) {
			logger.error("GENI DeleteSliver: Other Exception: " + e);
			return false;
		}
	}
	
//	public boolean Shutdown(String slice_urn, Object[] credentials) throws CredentialException{
//		
//		this.validateCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"});
//        
//		// submit the request
//		boolean ret = super.Shutdown(slice_urn, credentials);
//		
//		// return the response
//		return ret;
//	}
	
	public boolean RenewSliver(String slice_urn, Object[] credentials, String newTermEnd) {
		
		try {
			logger.info("GENI AM v1 RenewSliver() invoked");
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

			// submit the request
			boolean ret = orcaHandler.renewSlice(slice_urn, credentials, newTermEnd);

			// return the response
			return ret;
		} catch (CredentialException ce) {
			logger.error("GENI RenewSliver: Credential Exception: " + ce);
			return false;
		} catch (Exception e) {
			logger.error("GENI RenewSliver: Other Exception: " + e);
			return false;
		}
	}
	

	
	/**
	 * Right now this is same as deleteSliver
	 * It is unclear from the documentation as to how Shutdown differs from deleteSliver
	 * @param sliceId
	 * @return
	 */
	public boolean Shutdown(String slice_urn, Object[] credentials) {

		try {
			logger.info("GENI AM v1 Shutdown() invoked");
			validateGeniCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"}, verifyCredentials, logger);

			boolean result = false;

			SliceID sliceIdReal = XmlrpcOrcaState.getInstance().getSliceID(slice_urn);
			if (sliceIdReal == null)
				return false;

			String sliceId = sliceIdReal.toString();

			logger.debug("GENI Shutdown: Got sliceId as " + sliceId.trim());

			ISlice[] slices = (ISlice[]) sm.getSlices();

			if(slices == null){
				logger.error("GENI Shutdown: ERROR: No slices for service manager");
				result = false;
			}
			else{
				for(int i=0; i<slices.length; i++){
					String currSliceId = slices[i].getSliceID().toString();
					String inputSliceId = sliceId.trim();
					if(currSliceId.equalsIgnoreCase(inputSliceId)) {
						IReservation[] allRes = (IReservation[]) sm.getReservations(slices[i].getSliceID());
						if(allRes == null){
							result = false;
							logger.debug("GENI Shutdown: No reservations in slice with urn " + slice_urn + " sliceId  " + sliceId);
						}
						else{
							logger.debug("There are " + allRes.length + " reservations in the slice with sliceId = " + sliceId);
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
			return(result);
		} catch (CredentialException ce) {
			logger.error("GENI Shutdown: Credential Exception: " + ce);
			return false;
		} catch (Exception e) {
			logger.error("GENI Shutdown: Other Exception: " + e);
			return false;
		}
	}

	public Map<String, Object> GetVersion() throws XmlRpcException, Exception {
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("geni_api", 1);
		result.put("implementation", "ORCA");
		return result;
	}

}
