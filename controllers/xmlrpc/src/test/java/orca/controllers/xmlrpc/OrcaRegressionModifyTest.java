package orca.controllers.xmlrpc;

import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static orca.controllers.xmlrpc.OrcaXmlrpcAssertions.*;

@RunWith(Parameterized.class)
public class OrcaRegressionModifyTest {

	private static final Logger logger = Globals.getLogger(OrcaXmlrpcHandlerTest.class.getSimpleName());
	
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
    	return Arrays.asList(new Object[][] {
    		// Bootscripts need Velocity templating on Modify
    		{ "src/test/resources/112_velocityRequest.rdf", "src/test/resources/112_velocityModifyRequest.rdf", 5 },
    		// NodeGroup modify
    		{ "src/test/resources/122_request.rdf", "src/test/resources/122_nodegroups_increase_modify_request.rdf",
    			23 }, // we don't even have 23, but apparently the controller doesn't check
    		// NodeGroup modify
    		{ "src/test/resources/122_with_autoip_request.rdf",
    				"src/test/resources/122_nodegroups_increase_modify_request.rdf",
    				23 }, // we don't even have 23
    		{ "src/test/resources/122_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 5 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_one_noip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 3 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_two_noip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 4 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 5 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_one_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 3 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_two_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 4 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf", 5 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_one_noip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 4 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_two_noip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 5 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 6 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_one_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 4 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_two_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 5 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf", 6 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
    				"src/test/resources/137_nodegroups_delete_one_modify_request.rdf", 3 },
    		// NodeGroup modify
    		{ "src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
    				"src/test/resources/137_nodegroups_delete_one_modify_request.rdf", 3 },
    		// add storage modify
    		{ "../../embed/src/test/resources/orca/embed/TS1/TS1-2.rdf",
    				"src/test/resources/146_modify_add_storage_request.rdf", 2 },
    		// Interdomain modify
    		{ "../../embed/src/test/resources/orca/embed/161_interdomain_A1_B1_request.rdf",
    				"../../embed/src/test/resources/orca/embed/161_interdomain_simplified_A1_B1_B2_modify_request.rdf",
    				13 },
    		// Interdomain modify
    		{ "../../embed/src/test/resources/orca/embed/161_interdomain_A1_B1_request.rdf",
    				"../../embed/src/test/resources/orca/embed/161_interdomain_A1_B1_B2_C1_modify_request.rdf",
    				17 },
    		// Interdomain modify
    		{ "../../embed/src/test/resources/orca/embed/161_interdomain_A1_B1_request.rdf",
    				"../../embed/src/test/resources/orca/embed/161_interdomain_A1_B1_to_B2_modify_request.rdf",
    				9 },
			// Multiple modify to add interface
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_add_vlan0_with_no_ip.rdf",
					2 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_add_vlan1_with_no_ip.rdf",
					3 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_add_vlan2_with_no_ip.rdf",
					4 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_add_vlan3_with_no_ip.rdf",
					5 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_del_vlan2_with_no_ip.rdf",
					4 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_readd_vlan2_with_no_ip.rdf",
					5 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_add_vlan4_with_no_ip.rdf",
					6 },
			{ "src/test/resources/208_create_slice_request.rdf",
					"src/test/resources/208_modify_add_vlan5_with_no_ip.rdf",
					7 }
		});
    }

    // First Parameter -- file name with Request
    private String requestFilename;

    // Second Parameter -- whether test should pass
    private String modifyFilename;

    // Third Parameter -- number of Devices / Network Elements requested
    private int numDevicesInRequest;

    // JUnit automatically passes in Parameters to constructor
    public OrcaRegressionModifyTest(String requestFilename, String modifyFilename, int numDevicesInRequest) {
        this.requestFilename = requestFilename;
        this.modifyFilename = modifyFilename;
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
                + modifyFilename.substring(modifyFilename.lastIndexOf('/') + 1, modifyFilename.lastIndexOf('.'));

        logger.info("Starting Orca Regression Modify Test " + testName);

        // modify request
        LinkedHashMap<String, Integer> modifyRequests = new LinkedHashMap<>();
        modifyRequests.put(modifyFilename, numDevicesInRequest);

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
        }
    }
    
    @BeforeClass
    public static void setupTests() {
    	logger.info("Initializing NDL");
    	NdlCommons.init();
    }

}
