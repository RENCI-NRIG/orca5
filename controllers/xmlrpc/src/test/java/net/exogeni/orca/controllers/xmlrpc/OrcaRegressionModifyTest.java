package net.exogeni.orca.controllers.xmlrpc;

import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.shirako.container.Globals;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static net.exogeni.orca.controllers.xmlrpc.OrcaXmlrpcAssertions.*;

@RunWith(Parameterized.class)
public class OrcaRegressionModifyTest {

    private static final Logger logger = Globals.getLogger(OrcaXmlrpcHandlerTest.class.getSimpleName());
    
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // Bootscripts need Velocity templating on Modify
            { "src/test/resources/112_velocityRequest.rdf", Arrays.asList("src/test/resources/112_velocityModifyRequest.rdf"), Arrays.asList(5) },
            // NodeGroup modify
            { "src/test/resources/122_request.rdf", Arrays.asList("src/test/resources/122_nodegroups_increase_modify_request.rdf"),
                Arrays.asList(23) }, // we don't even have 23, but apparently the controller doesn't check
            // NodeGroup modify
            { "src/test/resources/122_with_autoip_request.rdf",
                    Arrays.asList("src/test/resources/122_nodegroups_increase_modify_request.rdf"),
                    Arrays.asList(23) }, // we don't even have 23
            { "src/test/resources/122_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(5) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_one_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf"), Arrays.asList(3) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_two_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf"), Arrays.asList(4) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf"), Arrays.asList(5) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_one_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf"), Arrays.asList(3) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_two_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf"), Arrays.asList(4) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf"), Arrays.asList(5) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_one_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(4) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_two_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(5) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(6) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_one_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(4) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_two_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(5) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf"), Arrays.asList(6) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_delete_one_modify_request.rdf"), Arrays.asList(3) },
            // NodeGroup modify
            { "src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
                    Arrays.asList("src/test/resources/137_nodegroups_delete_one_modify_request.rdf"), Arrays.asList(3) },
            // add storage modify
            { "../../embed/src/test/resources/net/exogeni/orca/embed/TS1/TS1-2.rdf",
                    Arrays.asList("src/test/resources/146_modify_add_storage_request.rdf"), Arrays.asList(2) },
            // Interdomain modify
            { "../../embed/src/test/resources/net/exogeni/orca/embed/161_interdomain_A1_B1_request.rdf",
                    Arrays.asList("../../embed/src/test/resources/net/exogeni/orca/embed/161_interdomain_simplified_A1_B1_B2_modify_request.rdf"),
                    Arrays.asList(13) },
            // Interdomain modify
            { "../../embed/src/test/resources/net/exogeni/orca/embed/161_interdomain_A1_B1_request.rdf",
                    Arrays.asList("../../embed/src/test/resources/net/exogeni/orca/embed/161_interdomain_A1_B1_B2_C1_modify_request.rdf"),
                    Arrays.asList(17) },
            // Interdomain modify
            { "../../embed/src/test/resources/net/exogeni/orca/embed/161_interdomain_A1_B1_request.rdf",
                    Arrays.asList("../../embed/src/test/resources/net/exogeni/orca/embed/161_interdomain_A1_B1_to_B2_modify_request.rdf"),
                    Arrays.asList(9) },
            // Multiple modify to add interface
            { "src/test/resources/208_create_slice_request.rdf",
                    Arrays.asList( "src/test/resources/208_modify_add_vlan0_with_no_ip.rdf",
                    "src/test/resources/208_modify_add_vlan1_with_no_ip.rdf",
                    "src/test/resources/208_modify_add_vlan2_with_no_ip.rdf",
                    "src/test/resources/208_modify_add_vlan3_with_no_ip.rdf",
                    "src/test/resources/208_modify_del_vlan2_with_no_ip.rdf",
                    "src/test/resources/208_modify_readd_vlan2_with_no_ip.rdf",
                    "src/test/resources/208_modify_add_vlan4_with_no_ip.rdf",
                    "src/test/resources/208_modify_add_vlan5_with_no_ip.rdf" ),
                    Arrays.asList(3, 4, 5, 6, 6, 7, 8, 9) },
            // Multiple modify to add interface
            { "src/test/resources/208_create_slice_request_with_vlan.rdf",
                    Arrays.asList( "src/test/resources/208_modify_add_vlan1_with_no_ip_2.rdf",
                            "src/test/resources/208_modify_add_vlan2_with_no_ip_2.rdf",
                            "src/test/resources/208_modify_add_vlan3_with_no_ip_2.rdf"),
                    Arrays.asList(4, 5, 6) }
        });
    }

    // First Parameter -- file name with Request
    private String requestFilename;

    // Second Parameter -- whether test should pass
    private List<String> modifyFilenames;

    // Third Parameter -- number of Devices / Network Elements requested
    private List<Integer> numDevicesInRequest;

    // JUnit automatically passes in Parameters to constructor
    public OrcaRegressionModifyTest(String requestFilename, List<String> modifyFilenames, List<Integer> numDevicesInRequest) {
        this.requestFilename = requestFilename;
        this.modifyFilenames = modifyFilenames;
        this.numDevicesInRequest = numDevicesInRequest;
    }

    /**
     * run the regression test suite
     *
     * @throws Exception
     */
    @Test
    public void testModifyRegressions() throws Exception {
        String testName = requestFilename.substring(requestFilename.lastIndexOf('/') + 1,
                requestFilename.lastIndexOf('.'));
        testName += "_"
                + modifyFilenames.get(0).substring(modifyFilenames.get(0).lastIndexOf('/') + 1, modifyFilenames.get(0).lastIndexOf('.'));

        System.out.println("Starting Orca Regression Modify Test " + testName);

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        Iterator<String> fileIter = modifyFilenames.iterator();
        Iterator<Integer> deviceIter = numDevicesInRequest.iterator();
        while(fileIter.hasNext() && deviceIter.hasNext()) {
                modifyRequests.put(fileIter.next(), deviceIter.next());
        }

        XmlrpcControllerSlice slice = OrcaXmlrpcHandlerTest.doTestMultipleModifySlice("modifySlice_test_" + testName,
                requestFilename, modifyRequests);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        assertManifestWillProcess(slice);

        // additional checks
        if (requestFilename.contains("112_")) {
            System.out.println("Checking Velocity Templating");
            assertBootscriptVelocityTemplating(computedReservations);
        } else if (requestFilename.contains("122_")) {
            // #122
            System.out.println("Checking for requested Resource Constraints");
            assertReservationsHaveResourceConstraints(computedReservations);
            assertNodeGroupReservationsHaveCorrectInterfaceNames(slice);
        } else if (requestFilename.contains("137_")) {
            System.out.println("Checking for Network Interfaces");
            // Nodes added in NodeGroup Increase need to have a Network interface
            assertReservationsHaveNetworkInterface(computedReservations);
            assertSliceHasNoDuplicateInterfaces(slice);
        } else if (requestFilename.contains("208_create_slice_request.rdf")) {
            System.out.println("Checking for Network Interfaces");
            assertSliceHasExpectedVlans(slice, computedReservations, 7);
        } else if (requestFilename.contains("208_create_slice_request_with_vlan.rdf")) {
            System.out.println("Checking for Network Interfaces");
            assertSliceHasExpectedVlans(slice, computedReservations, 4);
        }
    }
    
    @BeforeClass
    public static void setupTests() {
        logger.info("Initializing NDL");
        NdlCommons.init();
    }

}
