package orca.controllers.xmlrpc;

import orca.controllers.OrcaController;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.NetworkElement;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.meta.UnitProperties;
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
        Map<String, List<PropertyMng>> reservationProperties = new HashMap<>();
        prepareExpectedPropertyValuesForModifySliceWithNetmaskOnAdd(reservationProperties, slice.sliceUrn);
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
        reservationPropertyCountMap.put("Link2", 5);
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
        Map<String, List<PropertyMng>> reservationProperties = new HashMap<>();
        prepareExpectedPropertyValuesForModifySliceWithNetmaskOnAdd(reservationProperties, slice.sliceUrn);
        removeExpectedPropertiesInterdomain(reservationProperties);
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
        reservationPropertyCountMap.put("Link2", 5);
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
        Map<String, List<PropertyMng>> reservationProperties = new HashMap<>();
        prepareExpectedPropertyValuesForModifySliceWithNetmaskOnAddMixedDomain(reservationProperties, slice.sliceUrn);
        removeExpectedPropertiesInterdomain(reservationProperties);
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
        linkIPsMap.put("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Link2", "172.16.0.1/30");
        linkIPsMap.put("http://geni-orca.renci.org/owl/6fec4f59-8b7a-4ab0-a922-0e8f161724d5#Link304", "172.16.0.10/30");
        Map<String, Map<String, String>> nodeLinkIPsMap = new HashMap<>();
        nodeLinkIPsMap.put("Node0", linkIPsMap);
        assertLinkMatchesIPProperty(computedReservations, nodeLinkIPsMap);
    }

    /**
     * Some properties are not very easy to verify in Interdomain requests,
     * they will be removed here.
     *
     * @param reservationProperties
     */
    private void removeExpectedPropertiesInterdomain(Map<String, List<PropertyMng>> reservationProperties) {
        // in interdomain, cannot verify parent.url
        for (String node : reservationProperties.keySet()) {
            final List<PropertyMng> propertyMngs = reservationProperties.get(node);
            propertyMngs.removeIf(propertyMng -> propertyMng.getName().endsWith(UnitProperties.UnitEthParentUrl));
        }
    }

    private void prepareExpectedPropertyValuesForModifySliceWithNetmaskOnAdd(Map<String, List<PropertyMng>> reservationProperties, String sliceUrn) {
        List<PropertyMng> nodeProperties;
        PropertyMng property;

        nodeProperties = new ArrayList<>();

        property = new PropertyMng();
        property.setName("element.GUID");
        property.setValue("dcaa5e8d-ed44-4729-aab8-4361f108ca22");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("local.isVM");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("modify.version");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("num.parent.exist");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("num.parent.new");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.interface");
        property.setValue("2");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.storage");
        property.setValue("0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.slice.name");
        property.setValue(sliceUrn);
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.url");
        property.setValue("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Node0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.hosteth");
        property.setValue("vlan-data");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.ip");
        property.setValue("172.16.0.1/30");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.netmask");
        property.setValue("255.255.255.252");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.parent.url");
        property.setValue("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Link2");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth2.hosteth");
        property.setValue("vlan-data");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth2.ip");
        property.setValue("172.16.0.6/30");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth2.netmask");
        property.setValue("255.255.255.252");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth2.parent.url");
        property.setValue("http://geni-orca.renci.org/owl/110aa696-555b-4726-8502-8e961d3072ce#Link13");
        nodeProperties.add(property);


        reservationProperties.put("Node0", nodeProperties);
    }

    private void prepareExpectedPropertyValuesForModifySliceWithNetmaskOnAddMixedDomain(Map<String, List<PropertyMng>> reservationProperties, String sliceUrn) {
        List<PropertyMng> nodeProperties;
        PropertyMng property;

        nodeProperties = new ArrayList<>();

        property = new PropertyMng();
        property.setName("element.GUID");
        property.setValue("dcaa5e8d-ed44-4729-aab8-4361f108ca22");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("local.isVM");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("modify.version");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("num.parent.exist");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("num.parent.new");
        property.setValue("2");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.interface");
        property.setValue("3");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.storage");
        property.setValue("0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.slice.name");
        property.setValue(sliceUrn);
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.url");
        property.setValue("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Node0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.hosteth");
        property.setValue("vlan-data");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.ip");
        property.setValue("172.16.0.1/30");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.netmask");
        property.setValue("255.255.255.252");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth1.parent.url");
        property.setValue("http://geni-orca.renci.org/owl/029294b2-a517-48d6-b6c5-f9f77a95457c#Link2");
        nodeProperties.add(property);

        reservationProperties.put("Node0", nodeProperties);
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
        Map<String, List<PropertyMng>> reservationProperties = new HashMap<>();
        prepareExpectedPropertyValuesForNodeWithTwoInterfacesDeleteAdd(reservationProperties, slice.sliceUrn);
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

    private void prepareExpectedPropertyValuesForNodeWithTwoInterfacesDeleteAdd(Map<String, List<PropertyMng>> reservationProperties, String sliceUrn) {
        List<PropertyMng> nodeProperties;
        PropertyMng property;

        nodeProperties = new ArrayList<>();

        property = new PropertyMng();
        property.setName("num.parent.exist");
        property.setValue("2");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("num.parent.new");
        property.setValue("1");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.interface");
        property.setValue("3");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.storage");
        property.setValue("0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.slice.name");
        property.setValue(sliceUrn);
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.url");
        property.setValue("http://geni-orca.renci.org/owl/2e33b59b-0e3e-4b40-a64d-d9f3c055326f#Node0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth3.hosteth");
        property.setValue("vlan-data");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth3.ip");
        property.setValue("172.16.0.10/30");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth3.netmask");
        property.setValue("255.255.255.252");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.eth3.parent.url");
        property.setValue("http://geni-orca.renci.org/owl/2e33b59b-0e3e-4b40-a64d-d9f3c055326f#Link2");
        nodeProperties.add(property);


        reservationProperties.put("Node0", nodeProperties);
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
        Map<String, List<PropertyMng>> reservationProperties = new HashMap<>();
        prepareExpectedPropertyValuesForNodeWithTwoInterfacesDeleteAdd(reservationProperties, slice.sliceUrn);
        removeExpectedPropertiesInterdomain(reservationProperties);
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
        Map<String, List<PropertyMng>> reservationProperties = new HashMap<>();
        prepareExpectedPropertyValuesForNodeWithTwoInterfacesDeleteAddMixedDomain(reservationProperties, slice.sliceUrn);
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

    private void prepareExpectedPropertyValuesForNodeWithTwoInterfacesDeleteAddMixedDomain(Map<String, List<PropertyMng>> reservationProperties, String sliceUrn) {
        List<PropertyMng> nodeProperties;
        PropertyMng property;

        nodeProperties = new ArrayList<>();

        property = new PropertyMng();
        property.setName("num.parent.exist");
        property.setValue("4");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("num.parent.new");
        property.setValue("2");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.interface");
        property.setValue("6");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.number.storage");
        property.setValue("0");
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.slice.name");
        property.setValue(sliceUrn);
        nodeProperties.add(property);

        property = new PropertyMng();
        property.setName("unit.url");
        property.setValue("http://geni-orca.renci.org/owl/2e33b59b-0e3e-4b40-a64d-d9f3c055326f#Node0");
        nodeProperties.add(property);

        reservationProperties.put("Node0", nodeProperties);
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
            if (modifyEntry.getKey().startsWith("src/test/resources/146_remove_node_interdomain_modify_request.rdf")
                    && slice_urn.startsWith("testNodeWithTwoInterfacesInterdomainDeleteAdd"))
            {
                List<String> elementsToModify = getLinkElementsToModify(slice_urn, orcaXmlrpcHandler, "Node1");
                modReq = getModifiedRequestFor146InterdomainDelete(modReq, elementsToModify);
            } else if (modifyEntry.getKey().startsWith("src/test/resources/146_remove_node_mixed_domain_modify_request.rdf")
                    && slice_urn.startsWith("testNodeWithTwoInterfacesMixedDomainDeleteAdd"))
            {
                List<String> elementsToModify = getLinkElementsToModify(slice_urn, orcaXmlrpcHandler, "Node1");
                modReq = getModifiedRequestFor146MixedDomainDelete(modReq, elementsToModify);
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

    private static String getModifiedRequestFor146InterdomainDelete(String modReq, List<String> elementsToModify) {
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

    private static String getModifiedRequestFor146MixedDomainDelete(String modReq, List<String> elementsToModify) {
        // Modify the Modify Request with the created (random) URLs for the links
        for (String element : elementsToModify) {
            if (element.contains("uhNet.rdf")){
                System.out.println("Found uhNet: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/uhNet.rdf#uhNet/Domain/vlan/41a6bf0d-5063-47b3-9f4c-96aa44e504f0/vlan",
                        element);
            } else if (element.contains("ion.rdf")){
                System.out.println("Found ion: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/ion.rdf#ion/Domain/vlan/0239e47d-bce8-4b2d-bdbb-5cfb1b6b4118/vlan",
                        element);
            } else if (element.contains("nlr.rdf")){
                System.out.println("Found nlr: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/nlr.rdf#nlr/Domain/vlan/fb596fc5-99bc-41ff-86a5-c672f016550f/vlan",
                        element);
            } else if (element.contains("ben.rdf")){
                System.out.println("Found ben: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/ben.rdf#ben/Domain/vlan/8bacf107-074d-4206-9ee3-5361a4857bbb/vlan",
                        element);
            } else if (element.contains("rciNet.rdf")){
                System.out.println("Found rciNet: " + element);
                modReq = modReq.replaceAll("http://geni-orca.renci.org/owl/rciNet.rdf#rciNet/Domain/vlan/e0991977-e12b-4b4b-b5d1-53bb485b520e/vlan",
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
     * @param node
     * @return
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
