package orca.controllers.xmlrpc;

import com.hp.hpl.jena.ontology.Individual;
import orca.controllers.OrcaController;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.NetworkElement;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    public static final String IMAGE_URL = "http://geni-images.renci.org/images/standard/centos/centos6.7-v1.1.0/centos6.7-v1.1.0.xml";
    public static final String IMAGE_HASH = "0c22c525b8a4f0f480f17587557b57a7a111d198";
    public static final String IMAGE_NAME = "Centos 6.7 v1.1.0";

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
    public static XmlrpcControllerSlice doTestCreateSlice(XmlRpcController controller, String ndlFile, String slice_urn, int expectedReservationCount){
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
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/88_modReq.rdf", EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Node0", 13);
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node3", 13);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice("modifySlice_test",
                "../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf",
                modifyRequests);

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
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/48_modify_request.rdf", EXPECTED_RESERVATION_COUNT_FOR_MODIFY_WITH_NETMASK);

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Node0", 15);
        reservationPropertyCountMap.put("Node1", 15);
        reservationPropertyCountMap.put("Link1", 6);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "modifySlice_testWithNetmaskExisting",
                "src/test/resources/48_initial_request.rdf",
                modifyRequests);

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
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/48_modifyadd_modify.rdf", EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link2", 5);
        reservationPropertyCountMap.put("Node0", 24);
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node2", 14);
        reservationPropertyCountMap.put("Link13", 6);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "modifySlice_testWithNetmaskNew",
                "src/test/resources/20_create_with_netmask.rdf",
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // check individual property values
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/48_modifyadd_modify.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);

        // two of the VMs should have one network interface, one of them should have two interfaces
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 2);
        reservationInterfaceCountMap.put("Node1", 1);
        reservationInterfaceCountMap.put("Node2", 1);

        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

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
    public void testModifySliceWithNetmaskOnAddInterdomain() throws Exception {
        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/48_modifyadd_modify_interdomain.rdf", EXPECTED_RESERVATION_COUNT_FOR_MODIFY+4);

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link1", 5);
        reservationPropertyCountMap.put("Node0", 24);
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node2", 13); // 14); // only missing "element.GUID"

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "modifySlice_testWithNetmaskNew_Interdomain",
                "src/test/resources/20_create_with_netmask_bound_rci.rdf",
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // check individual property values
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/48_modifyadd_modify_interdomain.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);

        // two of the VMs should have one network interface, one of them should have two interfaces
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 2);
        reservationInterfaceCountMap.put("Node1", 1);
        reservationInterfaceCountMap.put("Node2", 1);

        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

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
    public void testModifySliceWithNetmaskOnAddMixedDomain() throws Exception {
        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/48_modifyadd_modify_mixed_domain.rdf", 7+4);

        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link1", 5);
        reservationPropertyCountMap.put("Node0", 30);
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node2", 13); // 14); // only missing "element.GUID"
        reservationPropertyCountMap.put("Node3", 14);
        reservationPropertyCountMap.put("Link304", 6);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "modifySlice_testWithNetmaskNew_MixedDomain",
                "src/test/resources/20_create_with_netmask_bound_rci.rdf",
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // check individual property values
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/48_modifyadd_modify_mixed_domain.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);

        // two of the VMs should have one network interface, one of them should have two interfaces
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 3);
        reservationInterfaceCountMap.put("Node1", 1);
        reservationInterfaceCountMap.put("Node2", 1);
        reservationInterfaceCountMap.put("Node3", 1);

        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

        assertNetmaskPropertyPresent(computedReservations);

        // check Link Parent and IP address matches for Intra-Domain links
        Map<String, String> linkIPsMap = new HashMap<>();
        linkIPsMap.put("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Link1", "172.16.0.1/30");
        linkIPsMap.put("http://geni-orca.renci.org/owl/6fec4f59-8b7a-4ab0-a922-0e8f161724d5#Link304", "172.16.0.10/30");
        Map<String, Map<String, String>> nodeLinkIPsMap = new HashMap<>();
        nodeLinkIPsMap.put("Node0", linkIPsMap);
        assertLinkMatchesIPProperty(computedReservations, nodeLinkIPsMap);
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
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/48_modifyremove_modify.rdf", EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        // specify the number of properties expected based on VM reservation ID
        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link11", 5);
        reservationPropertyCountMap.put("Node0", 19);
        reservationPropertyCountMap.put("Node1", 19);
        reservationPropertyCountMap.put("Node2", 19);
        reservationPropertyCountMap.put("Link9", 5);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "modifySlice_testModifyRemove",
                "src/test/resources/48_modifyremove_request.rdf",
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();


        // These are not exactly the expected values, since we are removing the interface connection
        // between two nodes.
        // However, ORCA does not currently remove interface information on Modify
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 2);
        reservationInterfaceCountMap.put("Node1", 2);
        reservationInterfaceCountMap.put("Node2", 2);

        // additional checks
        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);
        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);
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
        assertSliceHasNoDuplicateInterfaces(slice);
    }

    /**
     * Start with three nodes, with two nodes each connected to the central node (Node0).
     * Modify the slice by deleting one of the outer nodes.
     * Modify the slice by adding a new node, connected to the central node (Node0).
     * 
     * @throws Exception
     */
    @Test
    public void testNodeWithTwoInterfacesDeleteAdd() throws Exception {
        // Create Request
        String createRequestFile = "src/test/resources/146_create_node_with_two_interfaces_request.rdf";

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/146_remove_node_modify_request.rdf", 3);
        modifyRequests.put("src/test/resources/146_add_node_modify_request.rdf", 5);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "testNodeWithTwoInterfacesDeleteAdd",
                createRequestFile,
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        // specify the number of properties expected based on VM reservation ID
        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        reservationPropertyCountMap.put("Link1", 5);
        reservationPropertyCountMap.put("Node0", 31);
        reservationPropertyCountMap.put("Node2", 13);
        reservationPropertyCountMap.put("Node3", 14);
        reservationPropertyCountMap.put("Link2", 6);

        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // These are not exactly the expected values.
        // However, ORCA does not currently remove interface information on Modify
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 3);
        reservationInterfaceCountMap.put("Node2", 1);
        reservationInterfaceCountMap.put("Node3", 1);
        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

        //
        assertSliceHasNoDuplicateInterfaces(slice);

        // check individual property values
        // Note: since eth1 and eth2 are created at the same time, we cannot ensure which one is created first
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/146_two_interfaces_delete_add.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);

        // check Link Parent and IP address matches
        Map<String, String> linkIPsMap = new HashMap<>();
        linkIPsMap.put("http://geni-orca.renci.org/owl/2e33b59b-0e3e-4b40-a64d-d9f3c055326f#Link0", "172.16.0.2/30");
        linkIPsMap.put("http://geni-orca.renci.org/owl/2e33b59b-0e3e-4b40-a64d-d9f3c055326f#Link1", "172.16.0.5/30");
        linkIPsMap.put("http://geni-orca.renci.org/owl/2e33b59b-0e3e-4b40-a64d-d9f3c055326f#Link2", "172.16.0.10/30");
        Map<String, Map<String, String>> nodeLinkIPsMap = new HashMap<>();
        nodeLinkIPsMap.put("Node0", linkIPsMap);
        assertLinkMatchesIPProperty(computedReservations, nodeLinkIPsMap);
    }

    /**
     * Start with three nodes, with two nodes each connected to the central node (Node0).
     * Modify the slice by deleting one of the outer nodes.
     * Modify the slice by adding a new node, connected to the central node (Node0).
     *
     * @throws Exception
     */
    @Test
    public void testNodeWithTwoInterfacesInterdomainDeleteAdd() throws Exception {
        // Create Request
        String createRequestFile = "src/test/resources/146_create_node_with_two_interfaces_interdomain_request.rdf";

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/146_remove_node_interdomain_modify_request.rdf", 3+4);
        modifyRequests.put("src/test/resources/146_add_node_interdomain_modify_request.rdf", 5+8);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "testNodeWithTwoInterfacesInterdomainDeleteAdd",
                createRequestFile,
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        // specify the number of properties expected based on VM reservation ID
        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        // these seem to be only missing "element.GUID"
        reservationPropertyCountMap.put("Node0", 30); // 31
        reservationPropertyCountMap.put("Node2", 12); // 13
        reservationPropertyCountMap.put("Node3", 13); // 14);

        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // These are not exactly the expected values.
        // However, ORCA does not currently remove interface information on Modify
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 3);
        reservationInterfaceCountMap.put("Node2", 1);
        reservationInterfaceCountMap.put("Node3", 1);
        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

        //
        assertSliceHasNoDuplicateInterfaces(slice);

        // check individual property values
        // Note: since eth1 and eth2 are created at the same time, we cannot ensure which one is created first
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/146_two_interfaces_interdomain_delete_add.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);

        // can't check that Link Parent and IP address matches very easily in Interdomain
    }

    /**
     * Start with five nodes, with four nodes each connected to the central node (Node0).
     * Of these starting nodes, three nodes are all bound to RCI (including Node0).
     * The remaining two nodes are bound at UH, for Inter-Domain connection.
     * Modify the slice by deleting one of the outer nodes from each RCI and UH. (Mixed-Domain modify).
     * Modify the slice by adding a new node, connected to the central node (Node0),
     * from each of the RCI and UH domains (Mixed-Domain modify).
     *
     * @throws Exception
     */
    @Test
    public void testNodeWithTwoInterfacesMixedDomainDeleteAdd() throws Exception {
        // Create Request
        String createRequestFile = "src/test/resources/146_create_node_with_two_interfaces_mixed_domain_request.rdf";

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/146_remove_node_mixed_domain_modify_request.rdf", 5+4);
        modifyRequests.put("src/test/resources/146_add_node_mixed_domain_modify_request.rdf", 9+8);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "testNodeWithTwoInterfacesMixedDomainDeleteAdd",
                createRequestFile,
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        // specify the number of properties expected based on VM reservation ID
        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        // these seem to be only missing "element.GUID"
        reservationPropertyCountMap.put("Node0", 51); // ?
        reservationPropertyCountMap.put("Node2", 12); // 13
        reservationPropertyCountMap.put("Node3", 13); // 14);
        reservationPropertyCountMap.put("Node6", 13);
        reservationPropertyCountMap.put("Node7", 14);

        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // These are not exactly the expected values.
        // However, ORCA does not currently remove interface information on Modify
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 6);
        reservationInterfaceCountMap.put("Node2", 1);
        reservationInterfaceCountMap.put("Node3", 1);
        reservationInterfaceCountMap.put("Node6", 1);
        reservationInterfaceCountMap.put("Node7", 1);
        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

        //
        assertSliceHasNoDuplicateInterfaces(slice);

        // check individual property values
        // Note: since eth1 and eth2 are created at the same time, we cannot ensure which one is created first
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/146_two_interfaces_mixeddomain_delete_add.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);

        // can't check that Link Parent and IP address matches very easily in Interdomain
        // check Link Parent and IP address matches for Intra-Domain links
        Map<String, String> linkIPsMap = new HashMap<>();
        linkIPsMap.put("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Link331", "172.16.1.14/30");
        linkIPsMap.put("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Link332", "172.16.1.10/30");
        linkIPsMap.put("http://geni-orca.renci.org/owl/6fec4f59-8b7a-4ab0-a922-0e8f161724d5#Link370", "172.16.1.24/30");
        Map<String, Map<String, String>> nodeLinkIPsMap = new HashMap<>();
        nodeLinkIPsMap.put("Node0", linkIPsMap);
        assertLinkMatchesIPProperty(computedReservations, nodeLinkIPsMap);
    }

    /**
     * #162
     * Start a new slice with three nodes, in different domains, with no links between them. Create this slice.
     * Modify the slice by adding a broadcast link between two nodes (A and B). Submit this modify.
     * Modify the slice by adding a broadcast link between two nodes (B and C). Submit this modify.
     *
     * @throws Exception
     */
    @Test
    public void test162ModifyAddInterDomainBroadcastLinks() throws Exception {
        // Create Request
        String createRequestFile = "src/test/resources/162_interdomain_link_add_request.rdf";

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put("src/test/resources/162_interdomain_link_add_broadcast_A-B_modify_request.rdf", 3+5);
        modifyRequests.put("src/test/resources/162_interdomain_link_add_broadcast_B-A_modify_request.rdf", 3+5+3);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "test162ModifyAddInterDomainBroadcastLinks",
                createRequestFile,
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        //
        assertReservationsHaveNetworkInterface(computedReservations);
        assertSliceHasNoDuplicateInterfaces(slice);
    }

    /**
     * Adding and removing Nodes one at a time, means that we should always be able to predict
     * the interface numbering.
     *
     * @throws Exception
     */
    @Test
    public void testMixedDomainMultiStepModify() throws Exception {
        // Create Request
        String createRequestFile = "src/test/resources/20_create_with_netmask_bound_rci.rdf";

        String modifyXML;
        final String nsGuid = "029294b2-a517-48d6-b6c5-f9f77a95457c";  // matches GUID from createRequestFile

        // generate some modify requests.  a little awkward, since they need to be written to files.
        modifyXML = generateAddNodeModifyRequest(
                nsGuid,
                10,
                "rcivmsite.rdf#rcivmsite",
                "http://geni-orca.renci.org/owl/" + nsGuid+ "#Node0",
                "dcaa5e8d-ed44-4729-aab8-4361f108ca22");

        Path addNode10File = Files.createTempFile("addNode10-", ".rdf");
        Files.write(addNode10File, modifyXML.getBytes());

        modifyXML = generateRemoveNodeModifyRequest(
                "http://geni-orca.renci.org/owl/" + nsGuid+ "#",
                10);
        Path removeNode10File = Files.createTempFile("removeNode10-", ".rdf");
        Files.write(removeNode10File, modifyXML.getBytes());

        modifyXML = generateAddNodeModifyRequest(
                nsGuid,
                20,
                "uhvmsite.rdf#uhvmsite",
                "http://geni-orca.renci.org/owl/" + nsGuid+ "#Node0",
                "dcaa5e8d-ed44-4729-aab8-4361f108ca22");

        Path addNode20File = Files.createTempFile("addNode20-", ".rdf");
        Files.write(addNode20File, modifyXML.getBytes());

        modifyXML = generateRemoveInterDomainNodeModifyRequest(
                "http://geni-orca.renci.org/owl/" + nsGuid+ "#",
                20);
        Path removeNode20File = Files.createTempFile("removeNode20-", ".rdf");
        Files.write(removeNode20File, modifyXML.getBytes());

        modifyXML = generateAddNodeModifyRequest(
                nsGuid,
                11,
                "rcivmsite.rdf#rcivmsite",
                "http://geni-orca.renci.org/owl/" + nsGuid+ "#Node0",
                "dcaa5e8d-ed44-4729-aab8-4361f108ca22");

        Path addNode11File = Files.createTempFile("addNode10-", ".rdf");
        Files.write(addNode11File, modifyXML.getBytes());

        modifyXML = generateAddNodeModifyRequest(
                nsGuid,
                21,
                "uhvmsite.rdf#uhvmsite",
                "http://geni-orca.renci.org/owl/" + nsGuid+ "#Node0",
                "dcaa5e8d-ed44-4729-aab8-4361f108ca22");

        Path addNode21File = Files.createTempFile("addNode20-", ".rdf");
        Files.write(addNode21File, modifyXML.getBytes());

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put(addNode10File.toString(), 5);
        modifyRequests.put(removeNode10File.toString(), 3);
        modifyRequests.put(addNode20File.toString(), 5+4);
        modifyRequests.put(removeNode20File.toString(), 3);
        modifyRequests.put(addNode11File.toString(), 5);
        modifyRequests.put(addNode21File.toString(), 7+4);

        XmlrpcControllerSlice slice = doTestMultipleModifySlice(
                "testMixedDomainMultiStepModify",
                createRequestFile,
                modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        // specify the number of properties expected based on VM reservation ID
        Map<String, Integer> reservationPropertyCountMap = new HashMap<>();
        // these seem to be only missing "element.GUID"
        reservationPropertyCountMap.put("Node0", 39); // ?
        reservationPropertyCountMap.put("Node1", 13);
        reservationPropertyCountMap.put("Node11", 14);
        reservationPropertyCountMap.put("Node21", 13);

        assertExpectedPropertyCounts(computedReservations, reservationPropertyCountMap);

        // These are not exactly the expected values.
        // However, ORCA does not currently remove interface information on Modify
        Map<String, Integer> reservationInterfaceCountMap = new HashMap<>();
        reservationInterfaceCountMap.put("Node0", 5);
        reservationInterfaceCountMap.put("Node1", 1);
        reservationInterfaceCountMap.put("Node11", 1);
        reservationInterfaceCountMap.put("Node21", 1);
        assertReservationsHaveNetworkInterface(computedReservations, reservationInterfaceCountMap);

        //
        assertSliceHasNoDuplicateInterfaces(slice);

        // check individual property values
        // All interfaces are created sequentially; they should always have the same interface property number.
        Map<String, PropertiesMng> reservationProperties = new HashMap<>();
        InputStream input = new FileInputStream("src/test/resources/146_mixeddomain_multi_step.properties");
        reservationProperties.put("Node0", OrcaConverter.load(input));
        input.close();
        assertExpectedPropertyValues(computedReservations, reservationProperties);
    }

    /**
     * Remove an existing Node, and the inter-domain links.
     * This is not a clean process, because the inter-domain links are not known ahead of time.
     *
     * @param baseURL
     * @param existingNodeNumber
     * @return
     * @throws NdlException
     */
    private String generateRemoveInterDomainNodeModifyRequest(String baseURL, int existingNodeNumber) throws NdlException {
        String nsGuid = UUID.randomUUID().toString();
        NdlGenerator ngen = new NdlGenerator(nsGuid, logger, true);
        final String nm = nsGuid + "/modify";
        final Individual reservation = ngen.declareModifyReservation(nm);

        // add Remove statements for both Node and Link
        ngen.declareModifyElementRemoveNode(reservation, baseURL + "Node" + existingNodeNumber, UUID.randomUUID().toString());

        // maybe parameters for the connecting links??

        // GUIDs are arbitrary, will be corrected by getModifiedRequestFor146Delete()
        ngen.declareModifyElementRemoveLink(
                reservation,
                "http://geni-orca.renci.org/owl/uhNet.rdf#uhNet/Domain/vlan/d001b872-652e-45af-bd1e-474105d81363/vlan",
                UUID.randomUUID().toString());
        ngen.declareModifyElementRemoveLink(
                reservation,
                "http://geni-orca.renci.org/owl/ion.rdf#ion/Domain/vlan/5a168b1d-9eaa-4051-ac5d-feae36a2f6ab/vlan",
                UUID.randomUUID().toString());
        ngen.declareModifyElementRemoveLink(
                reservation,
                "http://geni-orca.renci.org/owl/nlr.rdf#nlr/Domain/vlan/098e3edb-12df-48fd-8748-04c6c4088a26/vlan",
                UUID.randomUUID().toString());
        ngen.declareModifyElementRemoveLink(
                reservation,
                "http://geni-orca.renci.org/owl/ben.rdf#ben/Domain/vlan/9d8a08eb-eaa7-4af1-bd2a-f309daa0a4bd/vlan",
                UUID.randomUUID().toString());
        ngen.declareModifyElementRemoveLink(
                reservation,
                "http://geni-orca.renci.org/owl/rciNet.rdf#rciNet/Domain/vlan/d55b28c3-f686-4393-b0f1-4e3fe5122061/vlan",
                UUID.randomUUID().toString());

        // get RDF XML of modify request
        return ngen.toXMLString();
    }

    /**
     * Remove an existing Node and it's corresponding Link
     *
     * @param baseURL
     * @param existingNodeNumber
     * @return
     * @throws NdlException
     */
    private String generateRemoveNodeModifyRequest(String baseURL, int existingNodeNumber) throws NdlException {
        String nsGuid = UUID.randomUUID().toString();
        NdlGenerator ngen = new NdlGenerator(nsGuid, logger, true);
        final String nm = nsGuid + "/modify";
        final Individual reservation = ngen.declareModifyReservation(nm);

        // add Remove statements for both Node and Link
        ngen.declareModifyElementRemoveNode(reservation, baseURL + "Node" + existingNodeNumber, UUID.randomUUID().toString());
        ngen.declareModifyElementRemoveLink(reservation, baseURL + "Link" + existingNodeNumber, UUID.randomUUID().toString());

        // get RDF XML of modify request
        return ngen.toXMLString();
    }

    /**
     * Create a new node linked to an existing Node.
     *
     * @param nsGuid
     * @param newNodeNumber
     * @param newNodeDomain
     * @param existingNodeUrl
     * @param existingNodeGuid
     * @return
     * @throws NdlException
     */
    private String generateAddNodeModifyRequest(String nsGuid, int newNodeNumber, String newNodeDomain, String existingNodeUrl, String existingNodeGuid) throws NdlException {
        if (newNodeNumber <= 0 || newNodeNumber >= 255) {
            return null;
        }

        // generate a modify request
        NdlGenerator ngen = new NdlGenerator(nsGuid, logger, true);
        final String nm = nsGuid + "/modify";
        final Individual reservation = ngen.declareModifyReservation(nm);

        // generate a new Node
        final Individual nodeI = ngen.declareComputeElement("Node" + newNodeNumber);

        // set Node Type on Instance
        ngen.addVMDomainProperty(nodeI);
        //ngen.addNodeTypeToCE() //?

        // add Image to Node
        final Individual imI = ngen.declareDiskImage(IMAGE_URL, IMAGE_HASH, IMAGE_NAME);
        ngen.addDiskImageToIndividual(imI, nodeI);

        // add Domain to Node
        final Individual domI = ngen.declareDomain(newNodeDomain);
        ngen.addNodeToDomain(domI, nodeI);

        // add new Node to reservation
        ngen.declareModifyElementAddElement(reservation, nodeI);

        // generate Link between new Node and existing Node
        final Individual edgeI = ngen.declareNetworkConnection("Link" + newNodeNumber);
        ngen.addGuid(edgeI, UUID.randomUUID().toString());
        ngen.addResourceToReservation(reservation, edgeI);

        String linkName;
        Individual intI;
        Individual ipInd;

        // add interface to new Node
        linkName = "Link" + newNodeNumber + "-" + "Node" + newNodeNumber;
        intI = ngen.declareInterface(linkName);
        ngen.addInterfaceToIndividual(intI, edgeI);
        ngen.addInterfaceToIndividual(intI, nodeI);
        ipInd = ngen.addUniqueIPToIndividual("172.16." + newNodeNumber + ".1", linkName, intI);
        ngen.addNetmaskToIP(ipInd, "255.255.255.252");

        // Need to modify existing Node to add interface
        Individual modCE = ngen.declareModifiedComputeElement(existingNodeUrl, existingNodeGuid);
        ngen.declareModifyElementModifyNode(reservation, modCE);

        final int index = existingNodeUrl.lastIndexOf("#");
        final String existingNode = index >= 0 ? existingNodeUrl.substring(index + 1) : existingNodeUrl;

        // add interface to existing node
        linkName = "Link" + newNodeNumber + "-" + existingNode;
        intI = ngen.declareInterface(linkName);
        ngen.addInterfaceToIndividual(intI, edgeI);
        ngen.addInterfaceToIndividual(intI, modCE);
        ipInd = ngen.addUniqueIPToIndividual("172.16." + newNodeNumber + ".2", linkName, intI);
        ngen.addNetmaskToIP(ipInd, "255.255.255.252");

        ngen.declareModifyElementAddElement(reservation, edgeI);

        // get RDF XML of modify request
        return ngen.toXMLString();
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

            // make any necessary modifications to the modify request
            if (
                    (modifyEntry.getKey().contains("146_remove_node_interdomain_modify_request.rdf")
                    && slice_urn.startsWith("testNodeWithTwoInterfacesInterdomainDeleteAdd"))
                    ||
                    (modifyEntry.getKey().contains("146_remove_node_mixed_domain_modify_request.rdf")
                    && slice_urn.startsWith("testNodeWithTwoInterfacesMixedDomainDeleteAdd"))
                    )
            {
                List<String> elementsToModify = getLinkElementsToModify(slice_urn, orcaXmlrpcHandler, "Node1");
                modReq = getModifiedRequestFor146Delete(modReq, elementsToModify);
            } else if (modifyEntry.getKey().contains("removeNode20")
                    && slice_urn.startsWith("testMixedDomainMultiStepModify"))
            {
                List<String> elementsToModify = getLinkElementsToModify(slice_urn, orcaXmlrpcHandler, "Node20");
                modReq = getModifiedRequestFor146Delete(modReq, elementsToModify);
            }

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
     * The modify request file used has static GUIDs for the Links between Nodes,
     * but those GUIDs will be randomly generated by ORCA on slice creation.
     * This method replaces the URLs (with GUID) for the Links that should be deleted,
     * using the URLs discovered in the already created slice.
     *
     * @param modReq The existing modify request from the static file.
     * @param elementsToModify A list of Link URLs discovered in the slice, as created by ORCA.
     * @return the modify request as a String, modified with the Link URLs as present in the 'live' slice.
     */
    private static String getModifiedRequestFor146Delete(String modReq, List<String> elementsToModify) {
        // Modify the Modify Request with the created (random) URLs for the links
        for (String element : elementsToModify) {
            if (element.contains("uhNet.rdf")){
                System.out.println("Found uhNet: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/uhNet.rdf#uhNet/Domain/vlan/d001b872-652e-45af-bd1e-474105d81363/vlan",
                        element);
            } else if (element.contains("ion.rdf")){
                System.out.println("Found ion: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/ion.rdf#ion/Domain/vlan/5a168b1d-9eaa-4051-ac5d-feae36a2f6ab/vlan",
                        element);
            } else if (element.contains("nlr.rdf")){
                System.out.println("Found nlr: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/nlr.rdf#nlr/Domain/vlan/098e3edb-12df-48fd-8748-04c6c4088a26/vlan",
                        element);
            } else if (element.contains("ben.rdf")){
                System.out.println("Found ben: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/ben.rdf#ben/Domain/vlan/9d8a08eb-eaa7-4af1-bd2a-f309daa0a4bd/vlan",
                        element);
            } else if (element.contains("rciNet.rdf")){
                System.out.println("Found rciNet: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/rciNet.rdf#rciNet/Domain/vlan/d55b28c3-f686-4393-b0f1-4e3fe5122061/vlan",
                        element);
            } else {
                fail("Reservation contains an unknown domain: " + element);
            }
        }
        return modReq;
    }

    /**
     *
     * @param slice_urn
     * @param orcaXmlrpcHandler
     * @param node The Node being deleted, which will be our starting point in determining all of the Links between this node and it's neighbor node.
     * @return a list of all of the Link URLs which will need to be modified (deleted).
     */
    private static List<String> getLinkElementsToModify(String slice_urn, OrcaXmlrpcHandler orcaXmlrpcHandler, String node) {
        List<String> elementsToModify = new ArrayList<>();

        final Collection<NetworkElement> boundElements;
        boundElements = orcaXmlrpcHandler.instance.getSlice(slice_urn).workflow.getBoundElements();

        // Start with the Node being deleted, and find the link path up to the connected Node
        for (NetworkElement element : boundElements) {
            if (element.getName().endsWith(node)){
                DomainElement domainElement = (DomainElement) element;

                if (null != domainElement.getPrecededBy()) {
                    for (DomainElement entry : domainElement.getPrecededBy().keySet()) {
                        findLinkElementsToModify(elementsToModify, entry);
                    }
                }

                if (null != domainElement.getFollowedBy()) {
                    for (DomainElement entry : domainElement.getFollowedBy().keySet()) {
                        findLinkElementsToModify(elementsToModify, entry);
                    }
                }
            }
        }
        System.out.println(Arrays.toString(elementsToModify.toArray()));
        return elementsToModify;
    }

    /**
     * Follow all PrecededBy and FollowedBy paths to find links between two VM nodes
     * @param elementsToModify
     * @param domainElement
     */
    private static void findLinkElementsToModify(List<String> elementsToModify, DomainElement domainElement) {
        if (domainElement.getResourceType().getResourceType().equals("vm")){
            return;
        }
        if (elementsToModify.contains(domainElement.getName())){
            return;
        }
        elementsToModify.add(domainElement.getName());

        if (null != domainElement.getPrecededBy()) {
            for (DomainElement entry : domainElement.getPrecededBy().keySet()) {
                findLinkElementsToModify(elementsToModify, entry);
            }
        }

        if (null != domainElement.getFollowedBy()) {
            for (DomainElement entry : domainElement.getFollowedBy().keySet()) {
                findLinkElementsToModify(elementsToModify, entry);
            }
        }
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

        final Map<String, Object> result = doTestRenewSlice(newTermEnd);

        assertEquals("renewSlice() resulted in non-matching term end", newTermEnd, result.get(TERM_END_FIELD));
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

        final Map<String, Object> result = doTestRenewSlice(newTermEnd);

        assertFalse("renewSlice() Over Max should not be resultant end term.", newTermEnd.equals(result.get(TERM_END_FIELD)));
    }

    /**
     * Call renewSlice with a given newTermEnd
     *
     * @param newTermEnd
     * @throws Exception
     */
    protected Map<String, Object> doTestRenewSlice(String newTermEnd) throws Exception {
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

        assertNotNull(result.get(TERM_END_FIELD));

        return result;
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
