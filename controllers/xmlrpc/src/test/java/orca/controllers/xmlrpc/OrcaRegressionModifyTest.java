package orca.controllers.xmlrpc;

import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import org.junit.Test;

import java.util.List;

import static orca.controllers.xmlrpc.OrcaXmlrpcAssertions.assertBootscriptVelocityTemplating;
import static orca.controllers.xmlrpc.OrcaXmlrpcAssertions.assertManifestWillProcess;
import static orca.controllers.xmlrpc.OrcaXmlrpcAssertions.assertReservationsHaveNetworkInterface;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandlerTest.EXPECTED_RESERVATION_COUNT_FOR_MODIFY;

public class OrcaRegressionModifyTest {

    /**
     * Bootscripts need Velocity templating on Modify
     *
     * @throws Exception
     */
    @Test
    public void testVelocityOnModify() throws Exception {
        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/112_velocityModifyRequest.rdf");

        XmlrpcControllerSlice slice = OrcaXmlrpcHandlerTest.doTestModifySlice(
                "modifySlice_testVelocity",
                "src/test/resources/112_velocityRequest.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        //additional checks
        assertBootscriptVelocityTemplating(computedReservations);
    }

    /**
     * NodeGroup modify was failing on Increase request, when Auto-IP is not used.
     *
     * @throws Exception
     */
    @Test
    public void testNodeGroupModifyIncrease() throws Exception {
        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/137_modify_request.rdf");

        XmlrpcControllerSlice slice = OrcaXmlrpcHandlerTest.doTestModifySlice(
                "modifySlice_testNodeGroup",
                "src/test/resources/122_request.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        List<TicketReservationMng> computedReservations = slice.getComputedReservations();

        assertManifestWillProcess(slice);

        // Nodes added in NodeGroup Increase need to have a Network interface
        assertReservationsHaveNetworkInterface(computedReservations);

    }

}
