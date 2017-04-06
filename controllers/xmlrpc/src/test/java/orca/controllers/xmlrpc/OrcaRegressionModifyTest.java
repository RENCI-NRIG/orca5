package orca.controllers.xmlrpc;

import orca.manage.beans.PropertyMng;
import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlCommons;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.*;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandlerTest.CHAR_TO_MATCH_RESERVATION_COUNT;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandlerTest.EXPECTED_RESERVATION_COUNT_FOR_MODIFY;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandlerTest.VALID_RESERVATION_SUMMARY_REGEX;
import static org.junit.Assert.*;

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

        List<TicketReservationMng> computedReservations = OrcaXmlrpcHandlerTest.doTestModifySlice(
                "modifySlice_testVelocity",
                "src/test/resources/112_velocityRequest.rdf",
                modReq, EXPECTED_RESERVATION_COUNT_FOR_MODIFY);

        //additional checks
        assertBootscriptVelocityTemplating(computedReservations);
    }

    /**
     * Check all VM reservations for Bootscripts that have been properly templated by Velocity
     *
     * @param computedReservations
     */
    protected void assertBootscriptVelocityTemplating(List<TicketReservationMng> computedReservations){
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " + localProperties.size());

            // only check VMs for Bootscript
            if (!reservation.getResourceType().endsWith("vm")){
                continue;
            }

            for (PropertyMng property : localProperties) {
                //System.out.println(property.getName() + ": " + property.getValue());
                if (property.getName().equals("unit.instance.config")) {
                    String bootscript = property.getValue();

                    assertFalse("Bootscript was not properly templated by Velocity: " + bootscript, bootscript.contains("$self"));

                    // don't need to check any other properties for this reservation
                    break;
                }
            }
        }
    }
}
