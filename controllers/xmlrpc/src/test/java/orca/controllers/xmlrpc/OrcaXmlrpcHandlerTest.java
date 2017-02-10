package orca.controllers.xmlrpc;

import orca.controllers.OrcaController;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.*;
import orca.ndl.NdlCommons;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.*;
import static org.junit.Assert.*;

public class OrcaXmlrpcHandlerTest {

    private static final Logger logger = Globals.getLogger(OrcaXmlrpcHandlerTest.class.getSimpleName());

    protected static final char CHAR_TO_MATCH_RESERVATION_COUNT = '[';
    protected static final int EXPECTED_RESERVATION_COUNT_FOR_MODIFY = 5;
    protected static final int EXPECTED_RESERVATION_COUNT_FOR_MODIFY_WITH_NETMASK = 3;
    protected static final int EXPECTED_RESERVATION_COUNT_FOR_CREATE = 3;
    protected static final int EXPECTED_RESERVATION_COUNT_FOR_CREATE_FAILURE = 5;
    protected static final String VALID_RESERVATION_SUMMARY_REGEX =
            "\\A[\\w\\s]+\\p{Punct}\\s*\\n" + // Here are the leases:
            "[\\w\\s]+\\p{Punct}[\\w\\s-]+\\n" + //Request id: 66c2001b-5c86-4747-b451-f072dd17b588
            "(?:\\p{Punct}(?:[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*)+\\n)+" + // [   Slice UID: 66c2001b-5c86-4747-b451-f072dd17b588 | Reservation UID: 0c77a77d-300d-4e68-ab71-5287aa67894e | Resource Type: ncsuvmsite.vm | Resource Units: 1 ]
            "(?:[\\w\\s]+)*\\z"; //No errors reported

    /**
     * testCreateSliceWithLiveSM() requires a connection to a running (non-test)
     * SM and AM+Broker.
     *
     * This test is slow (taking about 30 seconds), and should be disabled when
     * a test using a Mock SM is available.
     *
     * @throws Exception
     */
    //@Test
    public void testCreateSliceWithLiveSM() throws Exception {
        // Need to setup a controller
        // Currently this works if an SM is running locally.  Need to setup a Mock one.
        XmlRpcController controller = new XmlRpcController();
        controller.init();
        controller.start();

        Map<String, Object> result = doTestCreateSlice(controller,
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf",
                "createSlice_test_" + controller.getClass().getSimpleName());

        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

    }

    /**
     * Test that a slice can be created, using MockXmlRpcController
     *
     * Uses a MockXmlRpcController to fake a lot of things, avoiding the need
     * to talk to 'Live' SM or AM+Broker.
     *
     * @throws Exception
     */
    @Test
    public void testCreateSliceWithMockSM() throws Exception {
        // Need to setup a controller
        // Currently this works if an SM is running locally.  Need to setup a Mock one.
        XmlRpcController controller = new MockXmlRpcController();
        controller.init();
        controller.start();

        Map<String, Object> result = doTestCreateSlice(controller,
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf",
                "createSlice_test_" + controller.getClass().getSimpleName());

        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", EXPECTED_RESERVATION_COUNT_FOR_CREATE,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

        assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

    }

    /**
     * Test that a slice can be created, using MockXmlRpcController
     *
     * Uses a MockXmlRpcController to fake a lot of things, avoiding the need
     * to talk to 'Live' SM or AM+Broker.
     *
     * @throws Exception
     */
    @Test
    public void testCreateSliceWithNetmask() throws Exception {
        // Need to setup a controller
        // Currently this works if an SM is running locally.  Need to setup a Mock one.
        XmlRpcController controller = new MockXmlRpcController();
        controller.init();
        controller.start();

        // presence of netmask is examine inside doTestCreateSlice()
        Map<String, Object> result = doTestCreateSlice(controller,
                "src/test/resources/20_create_with_netmask.rdf",
                "createSlice_testWithNetmask_" + controller.getClass().getSimpleName());

        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", EXPECTED_RESERVATION_COUNT_FOR_CREATE,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

        assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

    }

    /**
     * Unfortunately this type of error cannot be detected naturally in the controller.
     *
     * Uses a MockXmlRpcController to fake a lot of things, avoiding the need
     * to talk to 'Live' SM or AM+Broker.
     *
     * @throws Exception
     */
    @Test
    public void testCreateSliceReservationFailureWithMockSM() throws Exception {
        // empty reservationMap is unchanged in this test
        Map<ReservationID, TicketReservationMng> reservationMap = new HashMap<>();

        MockXmlRpcController controller = new MockXmlRpcController();
        controller.init(reservationMap, true);
        controller.start();

        Map<String, Object> result = doTestCreateSlice(controller,
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_tooLarge.rdf",
                "createSlice_testFailure_" + controller.getClass().getSimpleName());

        // verify results of createSlice()
        assertNotNull(result);

        //unfortunately this type of error cannot be detected naturally in the controller
        //assertTrue("createSlice() should have returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

    }

    /**
     * The Live SM doesn't show reservations as being failed, only `Invalid`
     *
     * @throws Exception
     */
    //@Test
    public void testCreateSliceReservationFailureWithLiveSM() throws Exception {
        XmlRpcController controller = new XmlRpcController();
        controller.init();
        controller.start();

        Map<String, Object> result = doTestCreateSlice(controller,
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_tooLarge.rdf",
                "createSlice_testFailure_" + controller.getClass().getSimpleName());

        // verify results of createSlice()
        assertNotNull(result);
        //assertTrue("createSlice() should have returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

    }

    /**
     *
     * Used by createSlice() tests
     *
     * @param controller either a 'Live' or Mock XmlRpcController
     * @param ndlFile filename of createSlice() request RDF
     * @param slice_urn slice name
     */
    protected Map<String, Object> doTestCreateSlice(XmlRpcController controller, String ndlFile, String slice_urn){
        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        // setController to use either 'Live' or 'Mock' SM
        orcaXmlrpcHandler.instance.setController(controller);


        // setup parameters for createSlice()
        Object [] credentials = new Object[0];
        String resReq = NdlCommons.readFile(ndlFile);
        List<Map<String, ?>> users = getUsersMap();


        Map<String, Object> result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);

        // check for netmask in modified reservation
        if (slice_urn.startsWith("createSlice_testWithNetmask_")) {
            boolean foundNetmask = false;

            XmlrpcControllerSlice slice = orcaXmlrpcHandler.instance.getSlice(slice_urn);
            for (TicketReservationMng reservation : slice.getComputedReservations()) {
                for (PropertyMng property : reservation.getLocalProperties().getProperty()) {
                    if (property.getName().equals("unit.eth1.netmask")) {
                        foundNetmask = true;
                        break;
                    }
                }
            }

            assertTrue("Could not find netmask value in computed reservations.", foundNetmask);
        }

        return result;
    }

    /**
     * If this test runs too slowly, it will fail in the modifySlice() saying that it cannot modify DEAD slices.
     * The AM+Broker gives this message about the slice:
     * "<updateData><failed>true</failed><message>Closed while allocating ticket</message></updateData>"
     * Not sure why.
     *
     * @throws Exception
     */
    //@Test
    public void testModifySliceWithLiveSm() throws Exception {
        XmlRpcController controller = new XmlRpcController();
        controller.init();
        controller.start();

        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        orcaXmlrpcHandler.instance.setController(controller);

        Map<String, Object> result;

                // setup parameters for modifySlice()
        String slice_urn = "modifySlice_test_" + controller.getClass().getSimpleName(); //java.lang.AssertionError: createSlice() returned error: ERROR: duplicate slice urn createSlice_test
        Object [] credentials = new Object[0];


        // need to create a slice first
        String resReq = NdlCommons.readFile("../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf");
        List<Map<String, ?>> users = getUsersMap();

        // create the slice, before we can modify it
        result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);
        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));


        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/88_modReq.rdf");

        result = orcaXmlrpcHandler.modifySlice(slice_urn, credentials, modReq);

        // verify results of modifySlice()
        assertNotNull(result);
        assertFalse("modifySlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));
    }

    /**
     * Test that a simple slice modify, using MockXmlRpcController
     *
     * @throws Exception
     */
    @Test
    public void testModifySliceWithMockSm() throws Exception {
        Map<ReservationID, TicketReservationMng> reservationMap = new HashMap<>();

        MockXmlRpcController controller = new MockXmlRpcController();
        controller.init(reservationMap);
        controller.start();

        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        orcaXmlrpcHandler.instance.setController(controller);

        Map<String, Object> result;

        // setup parameters for modifySlice()
        String slice_urn = "modifySlice_test_" + controller.getClass().getSimpleName(); //java.lang.AssertionError: createSlice() returned error: ERROR: duplicate slice urn createSlice_test
        Object [] credentials = new Object[0];

        // get the reservations that would have been created by a previous call to createSlice()
        ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn, "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf");

        // add them to the reservationMap in our Mock SM
        addReservationListToMap(reservationsFromRequest, reservationMap);

        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/88_modReq.rdf");

        result = orcaXmlrpcHandler.modifySlice(slice_urn, credentials, modReq);

        // verify results of modifySlice()
        assertNotNull(result);
        assertFalse("modifySlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", EXPECTED_RESERVATION_COUNT_FOR_MODIFY,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

        assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

    }

    /**
     * Test that a simple slice modify, using MockXmlRpcController
     *
     * @throws Exception
     */
    @Test
    public void testModifySliceWithNetmask() throws Exception {
        Map<ReservationID, TicketReservationMng> reservationMap = new HashMap<>();

        MockXmlRpcController controller = new MockXmlRpcController();
        controller.init(reservationMap);
        controller.start();

        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        orcaXmlrpcHandler.instance.setController(controller);

        Map<String, Object> result;

        // setup parameters for modifySlice()
        String slice_urn = "modifySlice_testWithNetmask_" + controller.getClass().getSimpleName(); //java.lang.AssertionError: createSlice() returned error: ERROR: duplicate slice urn createSlice_test
        Object [] credentials = new Object[0];

        // get the reservations that would have been created by a previous call to createSlice()
        ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn, "src/test/resources/48_initial_request.rdf");

        // add them to the reservationMap in our Mock SM
        addReservationListToMap(reservationsFromRequest, reservationMap);

        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/48_modify_request.rdf");

        // modify the modify request to match UUIDs created by create slice
        modReq = modReq.replaceAll("64dced03-270a-48a2-a33d-e73494aab1b5", "4dd3f9c5-4555-436f-848a-3b578a5b2083");

        result = orcaXmlrpcHandler.modifySlice(slice_urn, credentials, modReq);

        // verify results of modifySlice()
        assertNotNull(result);
        assertFalse("modifySlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        // TODO: the expected reservations should be 3, not 5. But it is our test framework in error, not the code.
        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", EXPECTED_RESERVATION_COUNT_FOR_MODIFY_WITH_NETMASK,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

        assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

        // check for netmask in modified reservation
        boolean foundNetmask = false;

        XmlrpcControllerSlice slice = orcaXmlrpcHandler.instance.getSlice(slice_urn);
        for (TicketReservationMng reservation : slice.getComputedReservations()) {
            for (PropertyMng property : reservation.getLocalProperties().getProperty()) {
                if (property.getName().equals("unit.eth1.netmask")) {
                    foundNetmask = true;
                    break;
                }
            }
        }

        assertTrue("Could not find netmask value in computed reservations.", foundNetmask);
    }

    /**
     * Craft a userMap required by createSlice() and modifySlice().
     *
     * @return a UserMap with junk values
     */
    private List<Map<String, ?>> getUsersMap() {
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
     * @param string the string to test
     * @param toMatch the character to look for
     * @return the number of times toMatch is present in string
     */
    private int countMatches(String string, char toMatch){
        int occurrences = 0;
        for(char c : string.toCharArray()){
            if(c == toMatch){
                occurrences++;
            }
        }
        return occurrences;
    }

    /**
     * Modify the passed in Map to include the reservations from the passed in List, using ReservationID as key
     *
     * @param reservationsFromRequest a list of reservations to be added to reservationMap
     * @param reservationMap is modified by adding all reservations from reservationsFromRequest
     */
    private void addReservationListToMap(ArrayList<TicketReservationMng> reservationsFromRequest, Map<ReservationID, TicketReservationMng> reservationMap) {
        for (TicketReservationMng reservation : reservationsFromRequest){
            reservationMap.put(new ReservationID(reservation.getReservationID()), reservation);
        }
    }

    /**
     * Uses much of the same code as createSlice(), but stops after getting the List of reservations.
     *
     * @param orcaXmlrpcHandler
     * @param slice_urn the slice name
     * @param requestFile
     * @return a list of reservations created.
     * @throws Exception
     */
    protected ArrayList<TicketReservationMng> getReservationsFromRequest(OrcaXmlrpcHandler orcaXmlrpcHandler, String slice_urn, String requestFile) throws Exception {
        String resReq = NdlCommons.readFile(requestFile);
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
        workflow.run(drp, orcaXmlrpcHandler.abstractModels, resReq, userDN, controller_url, ndlSlice.getSliceID());

        ArrayList<TicketReservationMng> reservations = orc.getReservations(sm, workflow.getBoundElements(), orcaXmlrpcHandler.typesMap, workflow.getTerm(), workflow.getslice());

        // pretend the reservations are all active
        for (TicketReservationMng reservation : reservations){
            reservation.setState(OrcaConstants.ReservationStateActive);
        }

        //this also update the typesMap
        ndlSlice.setComputedReservations(reservations);

        ndlSlice.unlock();

        return reservations;

    }

}
