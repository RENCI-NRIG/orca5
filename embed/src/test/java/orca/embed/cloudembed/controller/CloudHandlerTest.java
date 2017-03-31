package orca.embed.cloudembed.controller;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import orca.embed.EmbedTestHelper;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.Domain;
import orca.embed.workflow.RequestParserListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlRequestParser;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkElement;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class CloudHandlerTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_ok.rdf", true, 3 },
                { "../controllers/xmlrpc/src/test/resources/20_create_with_netmask.rdf", true, 3},
                // run the regression test suite
                { "src/test/resources/orca/embed/TS1/TS1-1.rdf", true, 1},
                { "src/test/resources/orca/embed/TS1/TS1-2.rdf", true, 1},
                { "src/test/resources/orca/embed/TS1/TS1-3.rdf", true, 1},
                { "src/test/resources/orca/embed/TS1/TS1-4.rdf", true, 4},
                { "src/test/resources/orca/embed/TS1/TS1-5.rdf", true, 4},
                { "src/test/resources/orca/embed/TS1/TS1-6.rdf", true, 4},
                { "src/test/resources/orca/embed/TS1/TS1-7.rdf", true, 8},
                { "src/test/resources/orca/embed/TS1/TS1-8.rdf", true, 8},
                { "src/test/resources/orca/embed/TS1/TS1-9.rdf", true, 4},
                // not sure if TS2+ can be tested correctly here. should it be run by a different handler?
                { "src/test/resources/orca/embed/TS2/TS2-1.rdf", true, 13},
                { "src/test/resources/orca/embed/TS2/TS2-2.rdf", true, 13},
                { "src/test/resources/orca/embed/TS2/TS2-3.rdf", true, 5},
                { "src/test/resources/orca/embed/TS2/TS2-4.rdf", true, 13},
                { "src/test/resources/orca/embed/TS2/TS2-6.rdf", true, 11},
                { "src/test/resources/orca/embed/TS2/TS2-7.rdf", true, 12},
                { "src/test/resources/orca/embed/TS2/TS2-8.rdf", true, 4},
                { "src/test/resources/orca/embed/TS2/TS2-9.rdf", true, 3},
                { "src/test/resources/orca/embed/TS2/TS2-10.rdf", true, 4},
                { "src/test/resources/orca/embed/TS2/TS2-11.rdf", true, 3},
                { "src/test/resources/orca/embed/TS2/TS2-12.rdf", true, 45},
                { "src/test/resources/orca/embed/TS3/TS3-1.rdf", true, 4},
                { "src/test/resources/orca/embed/TS3/TS3-2.rdf", true, 4},
                { "src/test/resources/orca/embed/TS3/TS3-3.rdf", true, 13},
                { "src/test/resources/orca/embed/TS3/TS3-4.rdf", true, 13},
                { "src/test/resources/orca/embed/TS3/TS3-5.rdf", true, 9},
                { "src/test/resources/orca/embed/TS3/TS3-6.rdf", true, 13},
                { "src/test/resources/orca/embed/TS3/TS3-7.rdf", true, 8},
                { "src/test/resources/orca/embed/TS3/TS3-8.rdf", true, 5},
                { "src/test/resources/orca/embed/TS3/TS3-9.rdf", true, 20},
                { "src/test/resources/orca/embed/TS3/TS3-10.rdf", true, 56},
                { "src/test/resources/orca/embed/TS3/TS3-11.rdf", true, 42},
                { "src/test/resources/orca/embed/TS3/TS3-12.rdf", true, 10},
                { "src/test/resources/orca/embed/TS3/TS3-13.rdf", true, 10},
                { "src/test/resources/orca/embed/TS3/TS3-14.rdf", true, 99+3},
                { "src/test/resources/orca/embed/TS4/TS4-1.rdf", true, 5},
                { "src/test/resources/orca/embed/TS4/TS4-2.rdf", true, 5},
                { "src/test/resources/orca/embed/TS4/TS4-3.rdf", true, 10},
                { "src/test/resources/orca/embed/TS5/TS5-1.rdf", true, 6},
                { "src/test/resources/orca/embed/TS7/TS7-1.rdf", true, 14}
                // TS8 really only tests Post-boot Scripts. Not useful in Unit tests
                /*
                { "src/test/resources/orca/embed/TS8/TS8-1.rdf", true, 12},
                { "src/test/resources/orca/embed/TS8/TS8-2.rdf", true, 1},
                { "src/test/resources/orca/embed/TS8/TS8-3.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-4.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-5.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-6.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-7.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-8.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-9.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-10.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-11.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-12.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-13.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-14.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-15.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-16.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-17.rdf", true, 6},
                { "src/test/resources/orca/embed/TS8/TS8-18.rdf", true, 6}
                */
        });
    }

    // First Parameter -- file name with Request
    private String requestFilename;

    // Second Parameter -- whether test should pass
    private boolean expected;

    // Third Parameter -- number of Devices / Network Elements requested
    private int numDevicesInRequest;

    protected static Map<Domain, Map<String, Integer>> resourceMap;
    static {
        resourceMap = new HashMap<>();
        Domain domain;

        try {
            //domain = new Domain("orca/ndl/substrate/mass.rdf");
            domain = new Domain("orca/ndl/substrate/rcivmsite.rdf");

            HashMap<String, Integer> resource = new HashMap<>();
            resource.put("site.vm", 132);
            resource.put("site.vlan", 9);
            //resource.put("site.lun", 2);

            resourceMap.put(domain, resource);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NdlException e) {
            e.printStackTrace();
        }
    }

    // JUnit automatically passes in Parameters to constructor
    public CloudHandlerTest(String requestFilename, boolean expected, int numDevicesInRequest){
        this.requestFilename = requestFilename;
        this.expected = expected;
        this.numDevicesInRequest = numDevicesInRequest;
    }

    @Test
    public void testRunEmbedding() throws Exception {
        String testName = requestFilename.substring(requestFilename.lastIndexOf('/') + 1);
        System.out.println("Starting Orca Regression Test " + testName);
        
        CloudHandler cloudHandler = new CloudHandler();
        List<String> abstractModels = new ArrayList<>();


        // setup resource pools
        DomainResourcePools domainResourcePools = new DomainResourcePools();
        ResourcePoolsDescriptor pools = new ResourcePoolsDescriptor();

        EmbedTestHelper.populateModelsAndPools(abstractModels, pools, resourceMap);

        domainResourcePools.getDomainResourcePools(pools);
        cloudHandler.addSubstrateModel(abstractModels);
        //System.out.println(abstractModels);

        // setup request
        String reqStr = NdlCommons.readFile(requestFilename);
        //String reqStr = NdlCommons.readFile("src/test/resources/orca/embed/TS1/TS1-9.rdf");
        RequestParserListener parserListener = new RequestParserListener();
        // run the parser (to create Java objects)
        NdlRequestParser nrp = new NdlRequestParser(reqStr, parserListener);
        nrp.processRequest();
        RequestReservation request = parserListener.getRequest();

        System.out.println("request.getReservationDomain(): " + request.getReservationDomain());
        //System.out.println("request.getElements(): " + Arrays.toString(request.getElements().toArray()));

        System.out.println("TypeTotalUnits before: " + request.getTypeTotalUnits().size());
        boolean bound = request.generateGraph(request.getElements());
        System.out.println("TypeTotalUnits after: " + request.getTypeTotalUnits().size());
        System.out.println("Request is bound? " + bound);


        // check for errors, right before we runEmbedding()
        SystemNativeError err = request.getError();
        if (err != null) {
            // can't use assertNull if we want to use err.toString() as part of message.
            fail("Ndl request parser unable to parse request: " + err.toString());
        }

        //cloudHandler.runEmbedding("http://geni-orca.renci.org/owl/mass.rdf", request, domainResourcePools);
        //err = cloudHandler.runEmbedding("http://geni-orca.renci.org/owl/mass.rdf#mass/Domain", request, domainResourcePools);
        err = cloudHandler.runEmbedding("http://geni-orca.renci.org/owl/rcivmsite.rdf#rcivmsite/Domain", request, domainResourcePools);
        if ((err != null) == expected) {
            System.out.println("TestCase: " + requestFilename + " should have passed? " + expected);
            if (expected) {
                fail("cloudHandler.runEmbedding() failed: " + err.toString());
            } else {
                fail("embedding should have failed for request: " + requestFilename);
            }
        }

        //System.out.println("deviceList: " + Arrays.toString(cloudHandler.deviceList.toArray()));
        //System.out.println("domainConnectionList: " + Arrays.toString(cloudHandler.domainConnectionList.entrySet().toArray()));
        assertEquals(numDevicesInRequest, cloudHandler.deviceList.size());

        // check the manifest IP Address for presence of CIDR notation (Issue #110)
        LinkedList<NetworkElement> deviceList = cloudHandler.getDeviceList();
        for (NetworkElement element : deviceList){
            Interface defaultClientInterface = element.getDefaultClientInterface();
            if (defaultClientInterface != null){
                OntResource resource = defaultClientInterface.getResource();
                if (resource != null) {
                    RDFNode labelProperty = resource.getPropertyValue(NdlCommons.layerLabelIdProperty);
                    if (labelProperty != null) {
                        assertTrue("label_ID property should contain CIDR", labelProperty.toString().contains("/"));
                    }


                    OntResource addressProperty = (OntResource) resource.getPropertyValue(NdlCommons.ip4LocalIPAddressProperty);
                    RDFNode addressLabelProperty = addressProperty.getPropertyValue(NdlCommons.layerLabelIdProperty);
                    if (addressLabelProperty != null) {
                        assertFalse("IPAddress property should not contain CIDR", addressLabelProperty.toString().contains("/"));
                    }
                }
            }
        }
    }

}