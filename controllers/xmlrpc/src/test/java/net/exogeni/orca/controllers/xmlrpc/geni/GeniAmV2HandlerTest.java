package net.exogeni.orca.controllers.xmlrpc.geni;

import net.exogeni.orca.controllers.xmlrpc.MockXmlRpcController;
import net.exogeni.orca.controllers.xmlrpc.*;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import static net.exogeni.orca.controllers.xmlrpc.OrcaXmlrpcHandler.MaxReservationDuration;
import static org.junit.Assert.*;

public class GeniAmV2HandlerTest {
    protected static final SimpleDateFormat rfc3339Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Test
    public void renewSliver() throws Exception {

        // setup the renew term
        Calendar systemDefaultEndCal = Calendar.getInstance();
        systemDefaultEndCal.add(Calendar.MILLISECOND, (int) MaxReservationDuration / 2);
        String newTermEnd = rfc3339Formatter.format(systemDefaultEndCal.getTime());
        System.out.println(newTermEnd);

        final Map<String, Object> geniReturn = doRenewSliver(newTermEnd);

        // check for success
        final Object output = geniReturn.get(IGeniAmV2Interface.ApiReturnFields.OUTPUT.name);
        System.out.println(output);

        assertEquals("Geni API renew end did not match expected!", newTermEnd, output);

    }

    @Test
    public void renewSliverOverMax() throws Exception {

        // setup the renew term
        Calendar systemDefaultEndCal = Calendar.getInstance();
        systemDefaultEndCal.add(Calendar.MILLISECOND, Integer.MAX_VALUE);
        String newTermEnd = rfc3339Formatter.format(systemDefaultEndCal.getTime());
        System.out.println(newTermEnd);

        final Map<String, Object> geniReturn = doRenewSliver(newTermEnd);

        // check for success
        final Object output = geniReturn.get(IGeniAmV2Interface.ApiReturnFields.OUTPUT.name);
        System.out.println(output);

        assertFalse("Geni API renew value Over Max should not be returned as successful end term",
                newTermEnd.equals(output));

    }

    protected Map<String, Object> doRenewSliver(String newTermEnd) throws Exception {
        // use our Mock controller
        final MockXmlRpcController controller = new MockXmlRpcController();
        controller.setProperty(XmlRpcController.PropertyOrcaCredentialVerification, "false");
        controller.start();

        // create a slice
        final XmlrpcControllerSlice slice = OrcaXmlrpcHandlerTest.doTestCreateSlice(controller,
                "src/test/resources/20_create_with_netmask.rdf", "geniRenewSliver", true, 3);

        // need to force the slice reservations to be active
        for (TicketReservationMng reservation : slice.getComputedReservations()) {
            reservation.setState(OrcaConstants.ReservationStateActive);
        }

        // Start the Renew via Geni API
        GeniAmV2Handler geniHandler = new GeniAmV2Handler();
        geniHandler.verifyCredentials = false;
        geniHandler.controller = controller;

        Object[] credentials = new Object[0];

        final Map<String, Object> geniReturn = geniHandler.RenewSliver(slice.getSliceUrn(), credentials, newTermEnd,
                null);

        // check for success
        final Map<String, Object> codes = (Map<String, Object>) geniReturn
                .get(IGeniAmV2Interface.ApiReturnFields.CODE.name);
        final int code = (int) codes.get(IGeniAmV2Interface.ApiReturnFields.CODE_GENI_CODE.name);
        final Object output = geniReturn.get(IGeniAmV2Interface.ApiReturnFields.OUTPUT.name);

        assertEquals("Geni API returned error! " + output, IGeniAmV2Interface.ApiReturnCodes.SUCCESS.code, code);

        return geniReturn;
    }

}