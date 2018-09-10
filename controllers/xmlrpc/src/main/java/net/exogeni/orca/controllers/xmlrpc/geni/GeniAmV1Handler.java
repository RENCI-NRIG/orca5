package net.exogeni.orca.controllers.xmlrpc.geni;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.CredentialException;

import net.jwhoisserver.utils.InetNetworkException;
import net.exogeni.orca.controllers.OrcaController;
import net.exogeni.orca.controllers.xmlrpc.OrcaXmlrpcHandler;
import net.exogeni.orca.controllers.xmlrpc.ReservationConverter;
import net.exogeni.orca.controllers.xmlrpc.XmlRpcController;
import net.exogeni.orca.controllers.xmlrpc.XmlrpcControllerSlice;
import net.exogeni.orca.controllers.xmlrpc.XmlrpcHandlerHelper;
import net.exogeni.orca.controllers.xmlrpc.XmlrpcOrcaState;
import net.exogeni.orca.embed.workflow.RequestWorkflow;
import net.exogeni.orca.manage.IOrcaServiceManager;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.CompressEncode;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Implementation of the GENI AM API v1 handler
 * 
 * @author ibaldin
 *
 */
public class GeniAmV1Handler extends XmlrpcHandlerHelper implements IGeniAmV1Interface {
    protected final boolean verifyCredentials;

    protected final XmlRpcController controller;
    protected final OrcaXmlrpcHandler orcaHandler;
    protected final XmlrpcOrcaState instance;

    public static final String XMLRPC_SUFFIX = "geniV1";

    public GeniAmV1Handler() throws Exception {

        if (XmlRpcController.getProperty(PropertyGeniCredentialVerification) != null)
            verifyCredentials = new Boolean(XmlRpcController.getProperty(PropertyGeniCredentialVerification));
        else
            verifyCredentials = true;

        if (XmlRpcController.getProperty(PropertyNdlConverterUrlList) != null)
            NdlConverterUrlList = XmlRpcController.getProperty(PropertyNdlConverterUrlList);
        else
            NdlConverterUrlList = DEFAULT_NDL_CONVERTER_URL_LIST;

        orcaHandler = new OrcaXmlrpcHandler();
        instance = XmlrpcOrcaState.getInstance();

        logger = OrcaController.Log;
        logger.debug("GENI AMv1 XmlrpcHandler constructor called");

        controller = instance.getController();

        logger.info("GENI credential verification is turned " + (verifyCredentials ? "ON" : "OFF"));
        logger.info("Using NDL-RSpec converters at " + NdlConverterUrlList);
    }

    /**
     * ProtoGENI compresses and base-64 encodes the output, so lets shall
     * 
     * @param credentials credentials
     * @param options options
     * @return compressed output
     */
    public String ListResources(Object[] credentials, Map<?, ?> options) {

        try {
            logger.info("GENI AM v1 ListResources() invoked");

            validateGeniCredential(null, credentials, null, null, verifyCredentials, logger);

            Map<String, Object> rr = orcaHandler.listResources(credentials, options);
            String res;
            if ((Boolean) rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD))
                res = (String) rr.get(OrcaXmlrpcHandler.MSG_RET_FIELD);
            else
                res = (String) rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);

            // TODO: run via converter

            if (XmlrpcOrcaState.getInstance().getCompression()) {
                return CompressEncode.compressEncode(res);
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
     * 
     * @param slice_urn slice_urn
     * @param credentials credentials
     * @param resReq resReq
     * @param users users
     * @return ndl request
     */
    @SuppressWarnings("unchecked")
    public String CreateSliver(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users) {

        try {
            logger.info("GENI AM v1 CreateSliver() invoked for " + slice_urn);
            validateGeniCredential(slice_urn, credentials, new String[] { "*", "pi", "instantiate", "control" }, null,
                    verifyCredentials, logger);

            if ((resReq == null) || (resReq.length() == 0))
                return "ERROR: RSpec length 0";

            // convert RSpec to NDL using converter
            String ndlReq;
            Map<String, Object> res = callConverter(RSPEC2_TO_NDL, new Object[] { resReq, DEFAULT_OUTPUT_FORMAT });

            if ((Boolean) res.get("err")) {
                logger.error("GENI CreateSliver: Error encountered converting RSpec to NDL via converter service: "
                        + (String) res.get("msg"));
                return "Error encountered converting RSpec to NDL via converter service: " + (String) res.get("msg");
            } else
                ndlReq = (String) res.get("ret");

            // submit the request
            Map<String, Object> rr = orcaHandler.createSlice(slice_urn, credentials, ndlReq, users);
            String ret;

            if ((Boolean) rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD))
                ret = (String) rr.get(OrcaXmlrpcHandler.MSG_RET_FIELD);
            else
                ret = (String) rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);

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

    public String SliverStatus(String slice_urn, Object[] credentials) {
        IOrcaServiceManager sm = null;

        try {
            logger.info("GENI AM v1 SliverStatus() invoked for " + slice_urn);
            validateGeniCredential(slice_urn, credentials, new String[] { "*", "pi", "info" }, null, verifyCredentials,
                    logger);

            List<ReservationMng> allRes = null;
            String result = "";

            sm = controller.orca.getServiceManager();

            XmlrpcControllerSlice ndlSlice = instance.getSlice(slice_urn);
            if (ndlSlice == null) {
                result = "ERROR: Invalid slice " + slice_urn + ", slice status can't be determined";
                logger.error("GENI SliverStatus: Invalid slice " + slice_urn + ", slice status can't be determined");
                return result;
            }
            allRes = ndlSlice.getAllReservations(sm);

            RequestWorkflow workflow = ndlSlice.getWorkflow();

            if (allRes == null) {
                result = "ERROR: Invalid slice " + slice_urn + ", slice status can't be determined";
                logger.error("GENI SliverStatus: Invalid slice " + slice_urn + ", slice status can't be determined");
                return result;
            } else {
                // get manifest NDL representation
                logger.debug("GENI SliverStatus: ReservationConverter is being initiated!");

                String ndlMan = null;
                try {
                    ReservationConverter orc = new ReservationConverter();
                    ndlMan = orc.getManifest(workflow.getManifestModel(), workflow.getDomainInConnectionList(),
                            workflow.getBoundElements(), allRes);
                } catch (Exception e) {
                    logger.error("sliceStatus(): converter unable to get manifest: " + e);
                    return "ERROR: Failed due to exception: " + e;
                }
                // call ndl converter to get RSpec
                // convert RSpec to NDL using converter

                Map<String, Object> res = callConverter(MANIFEST_TO_RSPEC, new Object[] { ndlMan, slice_urn });
                if ((Boolean) res.get("err")) {
                    logger.error("GENI SliverStatus: Error encountered converting manifest to RSpec: "
                            + (String) res.get("msg"));
                    return "Error encountered converting manifest to RSpec: " + (String) res.get("msg");
                } else {
                    result = (String) res.get("ret");
                }

            }
            return result;
        } catch (CredentialException ce) {
            logger.error("GENI SliverStatus: Credential Exception: " + ce);
            return "Credendial Exception: " + ce;
        } catch (Exception e) {
            logger.error("GENI SliverStatus: Other Exception: " + e);
            return "Other Exception: " + e;
        } finally {
            if (sm != null) {
                controller.orca.returnServiceManager(sm);
            }
        }
    }

    public boolean DeleteSliver(String slice_urn, Object[] credentials) {

        try {
            logger.info("GENI AM v1 DeleteSliver() invoked for " + slice_urn);
            validateGeniCredential(slice_urn, credentials, new String[] { "*", "pi", "instantiate", "control" }, null,
                    verifyCredentials, logger);

            // submit the request
            Map<String, Object> rr = orcaHandler.deleteSlice(slice_urn, credentials);
            boolean ret;
            if ((Boolean) rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD))
                ret = false;
            else
                ret = (Boolean) rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);

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

    // public boolean Shutdown(String slice_urn, Object[] credentials) throws CredentialException{
    //
    // this.validateCredential(slice_urn, credentials, new String[]{"*", "pi", "instantiate", "control"});
    //
    // // submit the request
    // boolean ret = super.Shutdown(slice_urn, credentials);
    //
    // // return the response
    // return ret;
    // }

    public boolean RenewSliver(String slice_urn, Object[] credentials, String newTermEnd) {

        try {
            logger.info("GENI AM v1 RenewSliver() invoked for " + slice_urn);
            validateGeniCredential(slice_urn, credentials, new String[] { "*", "pi", "instantiate", "control" }, null,
                    verifyCredentials, logger);

            // submit the request
            Map<String, Object> rr = orcaHandler.renewSlice(slice_urn, credentials, newTermEnd);
            boolean ret;
            if ((Boolean) rr.get(OrcaXmlrpcHandler.ERR_RET_FIELD))
                ret = false;
            else
                ret = (Boolean) rr.get(OrcaXmlrpcHandler.RET_RET_FIELD);

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
     * Right now this is same as deleteSliver It is unclear from the documentation as to how Shutdown differs from
     * deleteSliver
     * 
     * @param slice_urn slice_urn
     * @param credentials credentials
     * @return true or false
     */
    public boolean Shutdown(String slice_urn, Object[] credentials) {
        IOrcaServiceManager sm = null;
        try {
            logger.info("GENI AM v1 Shutdown() invoked for " + slice_urn);
            validateGeniCredential(slice_urn, credentials, new String[] { "*", "pi", "instantiate", "control" }, null,
                    verifyCredentials, logger);

            boolean result = false;

            String sliceIdReal = XmlrpcControllerSlice.getSliceIDForUrn(slice_urn);
            if (sliceIdReal == null)
                return false;

            String sliceId = sliceIdReal.toString();

            logger.debug("GENI Shutdown: Got sliceId as " + sliceId.trim());

            sm = controller.orca.getServiceManager();

            List<ReservationMng> allRes = sm.getReservations(new SliceID(sliceIdReal));
            if (allRes == null) {
                logger.error("GENI Shutdown: Could not obtain reservations in slice with urn " + slice_urn
                        + " sliceId  " + sliceId + " : " + sm.getLastError());
                return false;
            }

            // FIXME: add closeReservations(SliceID) that tries to close all reservations in a slice.
            logger.debug("There are " + allRes.size() + " reservations in the slice with sliceId = " + sliceId);
            for (ReservationMng r : allRes)
                try {
                    logger.debug("Closing reservation with reservation GUID: " + r.getReservationID());
                    // FIXME: is this necessary?
                    // if(AbacUtil.verifyCredentials){
                    // setAbacAttributes(r, logger);
                    // }
                    sm.closeReservation(new ReservationID(r.getReservationID()));
                } catch (Exception ex) {
                    result = false;
                    throw new RuntimeException("Failed to close reservation", ex);
                }
            return true;
        } catch (CredentialException ce) {
            logger.error("GENI Shutdown: Credential Exception: " + ce);
            return false;
        } catch (Exception e) {
            logger.error("GENI Shutdown: Other Exception: " + e);
            return false;
        } finally {
            if (sm != null) {
                controller.orca.returnServiceManager(sm);
            }
        }

    }

    public Map<String, Object> GetVersion() throws XmlRpcException, Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("geni_api", 1);
        result.put("implementation", "ORCA");
        return result;
    }

    public static void main(String[] argv) {
        String result;
        String ConverterUrls = "http://localhost:13080/ndl-conversion/, http://localhost:11080/ndl-conversion/, http://localhost:11080/ndl-conversion/";
        String[] allUrls = ConverterUrls.split(",");
        String reqFileName = "/Users/ibaldin/workspace/orca-ndl-conversion/core/scripts/sample-rspecs/v2/unbound.xml";

        StringBuilder sb = null;
        try {
            BufferedReader bin = null;
            File f = new File(reqFileName);
            FileInputStream is = new FileInputStream(f);
            bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            sb = new StringBuilder();
            String line = null;
            while ((line = bin.readLine()) != null) {
                sb.append(line);
                // re-add line separator
                sb.append(System.getProperty("line.separator"));
            }

            bin.close();
        } catch (Exception e) {
            System.err.println("Error " + e + " encountered while readling file ");
            // System.exit(1);
        } finally {
            ;
        }
        GeniAmV1Handler h = null;
        try {
            h = new GeniAmV1Handler();
            // h.logger = Logger.getLogger("mylogger");
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            // System.exit(-1);
        }

        Map<String, Object> res;
        res = h.callConverter(RSPEC2_TO_NDL, new Object[] { sb.toString(), "RDF-XML" });
        if ((Boolean) res.get("err")) {
            System.out.println(
                    "GENI SliverStatus: Error encountered converting manifest to RSpec: " + (String) res.get("msg"));
            // System.exit(-1);
        } else {
            result = (String) res.get("ret");
            System.out.println("Converter result is " + result);
            // System.exit(0);
        }
    }
}
