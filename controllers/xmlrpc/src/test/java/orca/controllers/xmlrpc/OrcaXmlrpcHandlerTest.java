package orca.controllers.xmlrpc;

import orca.controllers.OrcaXmlrpcServlet;
import orca.ndl.NdlCommons;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.Test;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.ERR_RET_FIELD;
import static orca.controllers.xmlrpc.OrcaXmlrpcHandler.MSG_RET_FIELD;
import static org.junit.Assert.*;

public class OrcaXmlrpcHandlerTest {


    /**
     * testCreateSliceWithLiveSM() requires a connection to a running (non-test)
     * SM and AM+Broker.
     *
     * This test is slow (taking about 30 seconds), and should be disabled when
     * a test using a Mock SM is available.
     *
     * @throws Exception
     */
    @Test
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

        doTestCreateSlice(controller);

    }

    /**
     *
     * @param controller
     */
    protected void doTestCreateSlice(XmlRpcController controller){
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
    }

    @Test
    public void modifySlice() throws Exception {

    }

}