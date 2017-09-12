package orca.controllers.xmlrpc;

import orca.controllers.OrcaController;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static orca.controllers.xmlrpc.OrcaXmlrpcAssertions.*;
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
                    "(?:\\p{Punct}[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*[\\w\\s]+\\:[\\w\\s.\\/-]+?\\p{Punct}\\s*[\\w\\s]+\\:\\s*1\\s*?\\p{Punct}\\s*\\n)+" + // [   Slice UID: 66c2001b-5c86-4747-b451-f072dd17b588 | Reservation UID: 0c77a77d-300d-4e68-ab71-5287aa67894e | Resource Type: ncsuvmsite.vm | Resource Units: 1 ]
            "(?:[\\w\\s]+)*\\z"; //No errors reported
    protected static final SimpleDateFormat rfc3339Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

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
        XmlRpcController controller = new MockXmlRpcController();
        controller.init();
        controller.start();

        doTestCreateSlice(controller,
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf",
                "createSlice_test_" + controller.getClass().getSimpleName(),
                EXPECTED_RESERVATION_COUNT_FOR_CREATE);

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
        XmlRpcController controller = new MockXmlRpcController();
        controller.init();
        controller.start();

        XmlrpcControllerSlice slice = doTestCreateSlice(controller,
                "src/test/resources/20_create_with_netmask.rdf",
                "createSlice_testWithNetmask_" + controller.getClass().getSimpleName(),
                EXPECTED_RESERVATION_COUNT_FOR_CREATE);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        assertReservationsHaveNetworkInterface(computedReservations);
        assertNetmaskPropertyPresent(computedReservations);
    }

    /**
     *
     * Used by createSlice() tests
     *  @param controller either a 'Live' or Mock XmlRpcController
     * @param ndlFile filename of createSlice() request RDF
     * @param slice_urn slice name
     */
    protected static XmlrpcControllerSlice doTestCreateSlice(XmlRpcController controller, String ndlFile, String slice_urn, int expectedReservationCount){
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

        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", expectedReservationCount,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

        assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

        // return the slice to allow test to check properties
        XmlrpcControllerSlice slice = orcaXmlrpcHandler.instance.getSlice(slice_urn);

        return slice;

    }

    /**
     * Test that a simple slice modify, using MockXmlRpcController
     * The 'create' part of this request includes netmask information.
     *
     * @throws Exception
     */
    @Test
    public void testModifySliceWithMockSm() throws Exception {

        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/88_modReq.rdf");

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Node0", 13);
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node3", 13);

        XmlrpcControllerSlice slice = doTestModifySlice("modifySlice_test",
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);
    }

    /**
     * Test that a simple slice modify, using MockXmlRpcController
     * This test modifies existing VM reservations to include netmask.
     * Start with two unconnected VMs for the Create.
     * Modify by adding a network connection between them, and "Auto-IP"
     *
     * @throws Exception
     */
    @Test
    public void testModifySliceWithNetmaskOnModify() throws Exception {
        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/48_modify_request.rdf");

        // modify the modify request to match UUIDs created by create slice
        modReq = modReq.replaceAll("64dced03-270a-48a2-a33d-e73494aab1b5", "4dd3f9c5-4555-436f-848a-3b578a5b2083");

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Node0", 15);
        reservationPropertyCountMap.put("Node1", 15);
        reservationPropertyCountMap.put("Link1", 6);

        XmlrpcControllerSlice slice = doTestModifySlice(
                "modifySlice_testWithNetmaskExisting",
                "src/test/resources/48_initial_request.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY_WITH_NETMASK);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);
        assertReservationsHaveNetworkInterface(computedReservations);
        assertNetmaskPropertyPresent(computedReservations);
    }

    /**
     * Test that a simple slice modify, using MockXmlRpcController
     * This test modifies the slice to include new VM reservations that include netmask.
     * Start with two VMs connected with an "Auto-IP" network connection.
     * Modify by adding a third VM, connected to one of the original two, and "Auto-IP".
     *
     * @throws Exception
     */
    @Test
    public void testModifySliceWithNetmaskOnAdd() throws Exception {
        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/48_modifyadd_modify.rdf");

        // modify the modify request to match UUIDs created by create slice
        modReq = modReq.replaceAll("110aa696-555b-4726-8502-8e961d3072ce", "029294b2-a517-48d6-b6c5-f9f77a95457c");

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link2", 5);
        reservationPropertyCountMap.put("Node0", 22);
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node2", 14);
        reservationPropertyCountMap.put("Link13", 6);

        XmlrpcControllerSlice slice = doTestModifySlice(
                "modifySlice_testWithNetmaskNew",
                "src/test/resources/20_create_with_netmask.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // two of the VMs should have one network interface, one of them should have two interfaces
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 2);
        reservationInterfaceCountMap.put("Node1", 1);
        reservationInterfaceCountMap.put("Node2", 1);

        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

        assertNetmaskPropertyPresent(computedReservations);


    }

    /**
     * This tests a Modify with ModifyRemove elements.
     * I'm not convinced the test harness is correctly implementing everything...
     * Start with three VMs connected with a ring network, and "Auto-IP".
     * Modify by removing the network connection between any two nodes.
     *
     * @throws Exception
     */
    @Test
    public void testModifySliceWithModifyRemove() throws Exception {
        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/48_modifyremove_modify.rdf");

        // modify the modify request to match UUIDs created by create slice
        modReq = modReq.replaceAll("bea9b263-2506-4c7b-b09b-6df7ec2b56fa", "0cacb1fe-7af4-41a4-bfcf-79d7886c8155");

        // specify the number of properties expected based on VM reservation ID
        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link11", 5);
        reservationPropertyCountMap.put("Node0", 19);
        reservationPropertyCountMap.put("Node1", 19);
        reservationPropertyCountMap.put("Node2", 19);
        reservationPropertyCountMap.put("Link9", 5);

        XmlrpcControllerSlice slice = doTestModifySlice(
                "modifySlice_testModifyRemove",
                "src/test/resources/48_modifyremove_request.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);
        assertReservationsHaveNetworkInterface(computedReservations);
        assertNetmaskPropertyPresent(computedReservations);
    }

    /**
     * Modifying a NodeGroup,
     * by first removing the "first" element (0)
     * and then increasing the NodeGroup size by one.
     * Part of Issue #137
     *
     * @throws Exception
     */
    @Test
    public void testNodeGroupModifyDeleteIncrease() throws Exception {
        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/137_nodegroups_delete_one_modify_request.rdf", 3);
        modifyRequests.put("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 4);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "modifySlice_testModifyRemove",
                "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        // Nodes added in NodeGroup Increase need to have a Network interface
        assertReservationsHaveNetworkInterface(computedReservations);
        assertNodeGroupHasNoDuplicateInterfaces(slice);
    }

    /**
     *
     * @param slice_urn
     * @param requestFile
     * @param modReq
     * @param expectedReservationCount
     * @throws Exception
     */
    protected static XmlrpcControllerSlice doTestModifySlice(String slice_urn, String requestFile, String modReq, int expectedReservationCount) throws Exception {

        Map<ReservationID, TicketReservationMng> reservationMap = new HashMap<>();

        String resReq = NdlCommons.readFile(requestFile);


        MockXmlRpcController controller = new MockXmlRpcController();
        controller.init(reservationMap);
        controller.start();

        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        orcaXmlrpcHandler.instance.setController(controller);

        Map<String, Object> result;

        // setup parameters for modifySlice()
        Object [] credentials = new Object[0];

        // get the reservations that would have been created by a previous call to createSlice()
        ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn, resReq);

        // add them to the reservationMap in our Mock SM
        addReservationListToMap(reservationsFromRequest, reservationMap);

        result = orcaXmlrpcHandler.modifySlice(slice_urn, credentials, modReq);

        // verify results of modifySlice()
        assertNotNull(result);
        assertFalse("modifySlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", expectedReservationCount,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

        assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

        assertNotNull(result.get(TICKETED_ENTITIES_FIELD));

        // check the reservation properties
        return orcaXmlrpcHandler.instance.getSlice(slice_urn);
    }

    /**
     *
     * @param slice_urn
     * @param requestFile
     * @param modifyRequests Map of Modify Request filename (key) and expected reservation count (value)
     * @throws Exception
     */
    protected static XmlrpcControllerSlice doTestMultipleModifySlice(String slice_urn, String requestFile, LinkedHashMap<String, Integer> modifyRequests) throws Exception {

        Map<ReservationID, TicketReservationMng> reservationMap = new HashMap<>();

        String resReq = NdlCommons.readFile(requestFile);

        MockXmlRpcController controller = new MockXmlRpcController();
        controller.init(reservationMap);
        controller.start();

        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        orcaXmlrpcHandler.instance.setController(controller);

        Map<String, Object> result;

        // setup parameters for modifySlice()
        Object [] credentials = new Object[0];

        // get the reservations that would have been created by a previous call to createSlice()
        ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn, resReq);

        // add them to the reservationMap in our Mock SM
        addReservationListToMap(reservationsFromRequest, reservationMap);

        // perform all of the requested Modifies
        for (Map.Entry<String, Integer> modifyEntry : modifyRequests.entrySet()) {
            String modReq = NdlCommons.readFile(modifyEntry.getKey());
            int expectedReservationCount = modifyEntry.getValue();

            result = orcaXmlrpcHandler.modifySlice(slice_urn, credentials, modReq);

            // verify results of modifySlice()
            assertNotNull(result);
            assertFalse("modifySlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

            assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                            ") did not match expected value", expectedReservationCount,
                    countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));

            assertTrue("Result does not match regex.", ((String) result.get(RET_RET_FIELD)).matches(VALID_RESERVATION_SUMMARY_REGEX));

            assertNotNull(result.get(TICKETED_ENTITIES_FIELD));
        }

        // check the reservation properties
        return orcaXmlrpcHandler.instance.getSlice(slice_urn);
    }


    /**
     * Test RenewSlice() with a valid extended duration
     */
    @Test
    public void testRenewSlice() throws Exception {
        Calendar systemDefaultEndCal = Calendar.getInstance();
        systemDefaultEndCal.add(Calendar.MILLISECOND, (int)MaxReservationDuration / 2);
        String newTermEnd = rfc3339Formatter.format(systemDefaultEndCal.getTime());
        System.out.println(newTermEnd);

        doTestRenewSlice(newTermEnd);


    }

    /**
     * Test RenewSlice() with an invalid extended duration
     */
    @Test
    public void testRenewSliceOverMax() throws Exception {
        Calendar systemDefaultEndCal = Calendar.getInstance();
        systemDefaultEndCal.add(Calendar.MILLISECOND, Integer.MAX_VALUE);
        String newTermEnd = rfc3339Formatter.format(systemDefaultEndCal.getTime());
        System.out.println(newTermEnd);

        doTestRenewSlice(newTermEnd);


    }

    /**
     * Call renewSlice with a given newTermEnd
     *
     * @param newTermEnd
     * @throws Exception
     */
    protected void doTestRenewSlice(String newTermEnd) throws Exception {
        Map<ReservationID, TicketReservationMng> reservationMap = new HashMap<>();
        String requestFile = "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf";
        String resReq = NdlCommons.readFile(requestFile);
        String slice_urn = "testRenewSlice";

        // modify the create request to have a current Start Date
        String newStartDate = rfc3339Formatter.format(new Date());
        resReq = resReq.replaceAll("2016-12-13T12:15:12\\.633-05:00", newStartDate);


        MockXmlRpcController controller = new MockXmlRpcController();
        controller.init(reservationMap);
        controller.start();

        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;

        orcaXmlrpcHandler.instance.setController(controller);

        Map<String, Object> result;

        // setup parameters for modifySlice()
        Object [] credentials = new Object[0];

        // get the reservations that would have been created by a previous call to createSlice()
        ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn, resReq);

        // add them to the reservationMap in our Mock SM
        addReservationListToMap(reservationsFromRequest, reservationMap);


        result = orcaXmlrpcHandler.renewSlice(slice_urn, credentials, newTermEnd);

        // verify results of renewSlice()
        assertNotNull(result);
        assertFalse("renewSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));
    }

    /**
     * Craft a userMap required by createSlice() and modifySlice().
     *
     * @return a UserMap with junk values
     */
    private static List<Map<String, ?>> getUsersMap() {
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
    protected static int countMatches(String string, char toMatch){
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
    private static void addReservationListToMap(ArrayList<TicketReservationMng> reservationsFromRequest, Map<ReservationID, TicketReservationMng> reservationMap) {
        for (TicketReservationMng reservation : reservationsFromRequest){
            reservationMap.put(new ReservationID(reservation.getReservationID()), reservation);
        }
    }

    /**
     * Uses much of the same code as createSlice(), but stops after getting the List of reservations.
     *
     * @param orcaXmlrpcHandler
     * @param slice_urn the slice name
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
