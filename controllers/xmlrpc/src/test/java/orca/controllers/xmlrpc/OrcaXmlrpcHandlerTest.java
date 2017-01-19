package orca.controllers.xmlrpc;

import orca.controllers.OrcaController;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
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
    protected static final int EXPECTED_RESERVATION_COUNT_FOR_CREATE = 3;

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

        doTestCreateSlice(controller);

    }

    /**
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

        Map<String, Object> result = doTestCreateSlice(controller);

        assertEquals("Number or result reservations (based on " + CHAR_TO_MATCH_RESERVATION_COUNT +
                        ") did not match expected value", EXPECTED_RESERVATION_COUNT_FOR_CREATE,
                countMatches((String) result.get(RET_RET_FIELD), CHAR_TO_MATCH_RESERVATION_COUNT));
    }

    /**
     *
     * @param controller
     */
    protected Map<String, Object> doTestCreateSlice(XmlRpcController controller){
        OrcaXmlrpcHandler orcaXmlrpcHandler = new OrcaXmlrpcHandler();
        assertNotNull(orcaXmlrpcHandler);
        orcaXmlrpcHandler.verifyCredentials = false;
        //assertNull(OrcaXmlrpcServlet.getSslSessionId());
        //assertNotNull(OrcaXmlrpcServlet.getSslSessionId());

        // setController to use either 'Live' or 'Mock' SM
        orcaXmlrpcHandler.instance.setController(controller);


        // setup parameters for createSlice()
        String slice_urn = "createSlice_test_" + controller.getClass().getSimpleName(); //java.lang.AssertionError: createSlice() returned error: ERROR: duplicate slice urn createSlice_test
        Object [] credentials = new Object[0];
        String resReq = NdlCommons.readFile("../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf");
        List<Map<String, ?>> users = new ArrayList<>();
        Map<String, Object> userEntry = new HashMap<>();
        List<String> keys = new ArrayList<String>();
        keys.add("ssh-rsa this is not a key");
        userEntry.put("keys", keys);
        userEntry.put("login", "root");
        userEntry.put("sudo", false);
        users.add(userEntry);


        Map<String, Object> result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);

        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));

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

        // setup reservationMap in SM
        //reservationMap.

        // setup parameters for modifySlice()
        String slice_urn = "modifySlice_test_" + controller.getClass().getSimpleName(); //java.lang.AssertionError: createSlice() returned error: ERROR: duplicate slice urn createSlice_test
        Object [] credentials = new Object[0];


        // need to create a slice first
        String resReq = NdlCommons.readFile("../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf");
        List<Map<String, ?>> users = new ArrayList<>();
        Map<String, Object> userEntry = new HashMap<>();
        List<String> keys = new ArrayList<String>();
        keys.add("ssh-rsa this is not a key");
        userEntry.put("keys", keys);
        userEntry.put("login", "root");
        userEntry.put("sudo", false);
        users.add(userEntry);

        // create the slice, before we can modify it
        result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);
        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));


        //ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn);

        //addReservationListToMap(reservationsFromRequest, reservationMap);

        // modify request
        String modReq = NdlCommons.readFile("src/test/resources/88_modReq.rdf");

        result = orcaXmlrpcHandler.modifySlice(slice_urn, credentials, modReq);

        // verify results of modifySlice()
        assertNotNull(result);
        assertFalse("modifySlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));
    }

    /**
     *
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

        // setup reservationMap in SM
        //reservationMap.

        // setup parameters for modifySlice()
        String slice_urn = "modifySlice_test_" + controller.getClass().getSimpleName(); //java.lang.AssertionError: createSlice() returned error: ERROR: duplicate slice urn createSlice_test
        Object [] credentials = new Object[0];

        /*
        // need to create a slice first
        String resReq = NdlCommons.readFile("../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf");
        List<Map<String, ?>> users = new ArrayList<>();
        Map<String, Object> userEntry = new HashMap<>();
        List<String> keys = new ArrayList<String>();
        keys.add("ssh-rsa this is not a key");
        userEntry.put("keys", keys);
        userEntry.put("login", "root");
        userEntry.put("sudo", false);
        users.add(userEntry);

        // create the slice, before we can modify it
        result = orcaXmlrpcHandler.createSlice(slice_urn, credentials, resReq, users);
        // verify results of createSlice()
        assertNotNull(result);
        assertFalse("createSlice() returned error: " + result.get(MSG_RET_FIELD), (boolean) result.get(ERR_RET_FIELD));
        */

        ArrayList<TicketReservationMng> reservationsFromRequest = getReservationsFromRequest(orcaXmlrpcHandler, slice_urn);

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
    }

    private int countMatches(String string, char toMatch){
        int occurrences = 0;
        for(char c : string.toCharArray()){
            if(c == toMatch){
                occurrences++;
            }
        }
        return occurrences;
    }

    private void addReservationListToMap(ArrayList<TicketReservationMng> reservationsFromRequest, Map<ReservationID, TicketReservationMng> reservationMap) {
        for (TicketReservationMng reservation : reservationsFromRequest){
            reservationMap.put(new ReservationID(reservation.getReservationID()), reservation);
        }
    }

    protected ArrayList<TicketReservationMng> getReservationsFromRequest(OrcaXmlrpcHandler orcaXmlrpcHandler, String slice_urn) throws Exception {
        String resReq = NdlCommons.readFile("../../embed/src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf");
        List<Map<String, ?>> users = new ArrayList<>();
        Map<String, Object> userEntry = new HashMap<>();
        List<String> keys = new ArrayList<String>();
        keys.add("ssh-rsa this is not a key");
        userEntry.put("keys", keys);
        userEntry.put("login", "root");
        userEntry.put("sudo", false);
        users.add(userEntry);

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

        //this also update the typesMap
        ndlSlice.setComputedReservations(reservations);

        ndlSlice.unlock();

        return reservations;

    }
}
