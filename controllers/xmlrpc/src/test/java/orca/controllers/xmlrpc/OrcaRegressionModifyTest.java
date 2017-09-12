package orca.controllers.xmlrpc;

import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static orca.controllers.xmlrpc.OrcaXmlrpcAssertions.*;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandlerTest.EXPECTED_RESERVATION_COUNT_FOR_MODIFY;

@RunWith(Parameterized.class)
public class OrcaRegressionModifyTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                //Bootscripts need Velocity templating on Modify
                {"src/test/resources/112_velocityRequest.rdf",
                        "src/test/resources/112_velocityModifyRequest.rdf",
                        5},
                //NodeGroup modify
                {"src/test/resources/122_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        5},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_one_noip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf",
                        3},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_two_noip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf",
                        4},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf",
                        5},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_one_autoip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf",
                        3},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_two_autoip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf",
                        4},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_one_modify_request.rdf",
                        5},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_one_noip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        4},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_two_noip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        5},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_three_noip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        6},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_one_autoip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        4},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_two_autoip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        5},
                //NodeGroup modify
                {"src/test/resources/137_one_nodegroups_of_size_three_autoip_request.rdf",
                        "src/test/resources/137_nodegroups_increase_by_two_modify_request.rdf",
                        6}
        });
    }

    // First Parameter -- file name with Request
    private String requestFilename;

    // Second Parameter -- whether test should pass
    private String modifyFilename;

    // Third Parameter -- number of Devices / Network Elements requested
    private int numDevicesInRequest;

    // JUnit automatically passes in Parameters to constructor
    public OrcaRegressionModifyTest(String requestFilename, String modifyFilename, int numDevicesInRequest){
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
        String testName = requestFilename.substring(requestFilename.lastIndexOf('/') + 1, requestFilename.lastIndexOf('.'));
        testName += "_" + modifyFilename.substring(modifyFilename.lastIndexOf('/') + 1, modifyFilename.lastIndexOf('.'));

        System.out.println("Starting Orca Regression Modify Test " + testName);

        // modify request
        String modReq = NdlCommons.readFile(modifyFilename);

        XmlrpcControllerSlice slice = OrcaXmlrpcHandlerTest.doTestModifySlice(
                "modifySlice_test" + testName,
                requestFilename,
                modReq, numDevicesInRequest);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        assertManifestWillProcess(slice);

        //additional checks
        if (requestFilename.contains("112_")) {
            System.out.println("Checking Velocity Templating");
            assertBootscriptVelocityTemplating(computedReservations);
        } else if (requestFilename.contains("122_") || (requestFilename.contains("137_"))) {
            System.out.println("Checking for Network Interfaces");
            // Nodes added in NodeGroup Increase need to have a Network interface
            assertReservationsHaveNetworkInterface(computedReservations);
            assertNodeGroupHasNoDuplicateInterfaces(slice);
        }

    }

}
