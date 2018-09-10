package orca.network;

import java.net.UnknownHostException;

import junit.framework.TestCase;
import net.jwhoisserver.utils.InetNetworkException;
import orca.ndl.elements.NetworkConnection;

public class RequestMappingTest extends TestCase {

    String requestFileName, substrateFileName;
    RequestMapping mapping;

    public RequestMappingTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        requestFileName = "orca/network/request-6509-2.rdf";
        substrateFileName = "orca/network/ben-dell.rdf";
        mapping = new RequestMapping(requestFileName, substrateFileName);

    }

    public void testConnection() throws UnknownHostException, InetNetworkException {
        try {
            // (1) find the the device list for current request;
            // (2) Form the (physical) interface list for the switching action for each device
            NetworkConnection connection = mapping.deviceMapping();

            // (1) define the adapted client interface and find the right label according to the switching capability
            // (2) Convert to uni ports for Polatis
            // (3) update the action interface list and the ontModel
            // (4) fire the switching action to form the crossconnects by property "switchedTo"
            mapping.processDeviceConnection(connection);

            mapping.createVirtualConnection(connection);

            // mapping.getOntModel().write(System.out);
            assertTrue(true);
        } catch (RequestMappingException e) {
            assertTrue(false);
        }

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
