package orca.controllers.xmlrpc;

import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.ERR_RET_FIELD;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.MSG_RET_FIELD;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.PropertyXmlrpcControllerUrl;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.RET_RET_FIELD;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.TICKETED_ENTITIES_FIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import orca.controllers.OrcaController;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import orca.shirako.common.SliceID;

public class TestHelper {

	protected static final char CHAR_TO_MATCH_RESERVATION_COUNT = '[';
	protected static final int EXPECTED_RESERVATION_COUNT_FOR_MODIFY = 5;
	protected static final int EXPECTED_RESERVATION_COUNT_FOR_MODIFY_WITH_NETMASK = 3;
	protected static final int EXPECTED_RESERVATION_COUNT_FOR_CREATE = 3;
	protected static final int EXPECTED_RESERVATION_COUNT_FOR_CREATE_FAILURE = 5;
	protected static final String VALID_RESERVATION_SUMMARY_REGEX = "\\A[\\w\\s]+\\p{Punct}\\s*\\n" + // Here are the
	                                                                                                      // leases:
	            "[\\w\\s]+\\p{Punct}[\\w\\s-]+\\n" + // Request id: 66c2001b-5c86-4747-b451-f072dd17b588
	            "(?:\\p{Punct}[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*[\\w\\s]+\\:\\s*1\\s*?\\p{Punct}\\s*\\n)+"
	            + // [ Slice UID: 66c2001b-5c86-4747-b451-f072dd17b588 | Reservation UID:
	              // 0c77a77d-300d-4e68-ab71-5287aa67894e | Resource Type: ncsuvmsite.vm | Resource Units: 1 ]
	            "(?:[\\w\\s]+)*\\z";
	protected static final SimpleDateFormat rfc3339Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	public static final String IMAGE_URL = "http://geni-images.renci.org/images/standard/centos/centos6.7-v1.1.0/centos6.7-v1.1.0.xml";
	public static final String IMAGE_HASH = "0c22c525b8a4f0f480f17587557b57a7a111d198";
	public static final String IMAGE_NAME = "Centos 6.7 v1.1.0";

	/**
	 *
	 * Used by createSlice() tests
	 * 
	 * @param controller
	 *            either a 'Live' or Mock XmlRpcController
	 * @param ndlFile
	 *            filename of createSlice() request RDF
	 * @param slice_urn
	 *            slice name
	 * @param expectedSuccess
	 */
	public static XmlrpcControllerSlice doTestCreateSlice(XmlRpcController controller, String ndlFile, String slice_urn, boolean expectedSuccess, int expectedReservationCount) {
	    OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
	    assertNotNull(orcaXmlrpcHandler);
	    orcaXmlrpcHandler.verifyCredentials = false;
	
	    // setController to use either 'Live' or 'Mock' SM
	    orcaXmlrpcHandler.instance.setController(controller);
	
	    // setup parameters for createSlice()
	    Object[] credentials = new Object[0];
	    String resReq = NdlCommons.readFile(ndlFile);
	    List<Map<String, ?>> users = getUsersMap();
	
	    Map<String, Object> result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);
	
	    // verify results of createSlice()
	    assertNotNull(result);
	    assertEquals("createSlice() returned error: " + result.get(MSG_RET_FIELD), expectedSuccess,
	            !(boolean) result.get(ERR_RET_FIELD));
	
	    // Tests that are expected to fail should return here
	    if (!expectedSuccess) {
	        return null;
	    }
	
	    assertEquals(
	            "Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT
	                    + ") did not match expected value",
	            expectedReservationCount,
	            countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));
	
	    assertTrue("Result does not match regex.",
	            ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));
	
	    assertNotNull(result.get(TICKETED_ENTITIES_FIELD));
	
	    // return the slice to allow test to check properties
	    XmlrpcControllerSlice slice = orcaXmlrpcHandler.instance.getSlice(slice_urn);
	
	    return slice;
	
	}
	
	/**
	 * 
	 * @param controller
	 * @param ndlFile
	 * @param slice_urn
	 * @return slice manifest
	 */
	public String doTestCreateSliceWithManifest(XmlRpcController controller, String ndlFile, String slice_urn, Logger logger) {
	    OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
	    assertNotNull(orcaXmlrpcHandler);
	    orcaXmlrpcHandler.verifyCredentials = false;
	
	    // setController to use either 'Live' or 'Mock' SM
	    orcaXmlrpcHandler.instance.setController(controller);
	
	    // setup parameters for createSlice()
	    Object[] credentials = new Object[0];
	    String resReq = NdlCommons.readFile(ndlFile);
	    List<Map<String, ?>> users = getUsersMap();
	
	    Map<String, Object> result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);
	
	    // verify results of createSlice()
	    assertNotNull(result);
	    assertEquals("createSlice() returned error: " + result.get(MSG_RET_FIELD), true,
	            !(boolean) result.get(ERR_RET_FIELD));
	
	    assertTrue("Result does not match regex.",
	            ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));
	
	    assertNotNull(result.get(TICKETED_ENTITIES_FIELD));
	
	    try {
	    	return XmlrpcHandlerHelper.getSliceManifest(orcaXmlrpcHandler.instance, slice_urn, logger);
	    } catch (Exception e) {
	    	return null;
	    }
	}
	

	/**
	 * Craft a userMap required by createSlice() and modifySlice().
	 *
	 * @return a UserMap with junk values
	 */
	protected static List<Map<String, ?>> getUsersMap() {
	    List<Map<String, ?>> users = new ArrayList<>();
	    Map<String, Object> userEntry = new HashMap<>();
	    List<String> keys = new ArrayList<String>();
	    keys.add("ssh-rsa this is not a key");
	    userEntry.put("keys", keys);
	    userEntry.put("login", "root");
	    userEntry.put("sudo", false);
	    users.add(userEntry);
	    return users;
	}

	/**
	 * Count the number of times a specific character is present in a string
	 *
	 * @param string
	 *            the string to test
	 * @param toMatch
	 *            the character to look for
	 * @return the number of times toMatch is present in string
	 */
	protected static int countMatches(String string, char toMatch) {
	    int occurrences = 0;
	    for (char c : string.toCharArray()) {
	        if (c == toMatch) {
	            occurrences++;
	        }
	    }
	    return occurrences;
	}

	/**
	 * Uses much of the same code as createSlice(), but stops after getting the List of reservations.
	 *
	 * @param orcaXmlrpcHandler
	 * @param slice_urn
	 *            the slice name
	 * @param resReq
	 * @return a list of reservations created.
	 * @throws Exception
	 */
	protected static ArrayList<TicketReservationMng> getReservationsFromRequest(OrcaXmlrpcHandler orcaXmlrpcHandler, String slice_urn, String resReq) throws Exception {
	
	    List<Map<String, ?>> users = getUsersMap();
	
	    String userDN = "test";
	
	    IOrcaServiceManager sm = orcaXmlrpcHandler.instance.getSM();
	
	    // generate and register new slice
	    SliceMng slice = new SliceMng();
	    slice.setName(slice_urn);
	    slice.setClientSlice(true);
	    SliceID sid = sm.addSlice(slice);
	
	    orcaXmlrpcHandler.discoverTypes(sm);
	
	    // create XmlrpcSlice object and register with Orca state
	    XmlrpcControllerSlice ndlSlice = new XmlrpcControllerSlice(sm, slice, slice_urn, userDN, users, false);
	    // we lock the slice from any concurrent modifications
	    ndlSlice.lock();
	    ndlSlice.getStateMachine().transitionSlice(SliceStateMachine.SliceCommand.CREATE);
	
	    orcaXmlrpcHandler.instance.addSlice(ndlSlice);
	
	    String controller_url = OrcaController.getProperty(PropertyXmlrpcControllerUrl);
	
	    ReservationConverter orc = ndlSlice.getOrc();
	
	    DomainResourcePools drp = new DomainResourcePools();
	    drp.getDomainResourcePools(orcaXmlrpcHandler.pools);
	
	    RequestWorkflow workflow = ndlSlice.getWorkflow();
	    workflow.setGlobalControllerAssignedLabel(orcaXmlrpcHandler.instance.getControllerAssignedLabel());
	    workflow.setShared_IP_set(orcaXmlrpcHandler.instance.getShared_IP_set());
	    final SystemNativeError runError = workflow.run(drp, orcaXmlrpcHandler.abstractModels, resReq, userDN,
	            controller_url, ndlSlice.getSliceID());
	
	    // this shouldn't happen, unless we've made an error in creating our tests
	    if (runError != null) {
	        assertFalse("Could not create slice necessary for Modify: " + runError.getMessage(), runError.isError());
	    }
	
	    ArrayList<TicketReservationMng> reservations = orc.getReservations(sm, workflow.getBoundElements(),
	            orcaXmlrpcHandler.typesMap, workflow.getTerm(), workflow.getslice());
	
	    // pretend the reservations are all active
	    for (TicketReservationMng reservation : reservations) {
	        reservation.setState(OrcaConstants.ReservationStateActive);
	    }
	
	    // this also update the typesMap
	    ndlSlice.setComputedReservations(reservations);
	
	    ndlSlice.unlock();
	    
	    return reservations;
	
	}

}
