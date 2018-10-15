package orca.network;

import java.io.IOException;

import net.jwhoisserver.utils.InetNetworkException;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.XSD;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.ResultSet;

public class NetworkHandlerOriginalTest extends TestCase {

    public static final String rootURL = "http://geni-orca.renci.org/owl/";
    String requestFileName, requestFileName2, substrateFileName;
    String request1, request2;
    NetworkHandler handler;

    public NetworkHandlerOriginalTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();

        requestFileName = "orca/network/request-6509.rdf";
        requestFileName2 = "orca/network/request-6509-2.rdf";
        substrateFileName = "orca/network/ben-dell.rdf";
    }

    public void testHandleMapping() throws IOException, InetNetworkException {
        handler = new NetworkHandler(substrateFileName);

        handler.handleMapping(requestFileName);

        RequestMapping mapper = handler.getMapper();

        request1 = mapper.getRequestURI();

        OntModel model = mapper.getOntModel();

        // model.write(System.out);

        // handler=new NetworkHandler();

        // handler.getMapper().setOntModel(model);
        // handler.getMapper().createProperty();

        handler.handleMapping(requestFileName2);

        request2 = mapper.getRequestURI();

        System.out.println(request2);

        handler.releaseReservation(request1);

        handler.releaseReservation(request2);

        // model.write(System.out);
        assertTrue(true);
    }

}
