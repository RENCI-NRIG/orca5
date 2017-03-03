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
                /*{ "src/test/resources/orca/embed/CloudHandlerTest/XOXlargeRequest_tooLarge.rdf", false, 5 }*/ // this test does not expose the error correctly
                { "../controllers/xmlrpc/src/test/resources/20_create_with_netmask.rdf", true, 3}
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
            domain = new Domain("orca/ndl/substrate/uvanlvmsite.rdf");

            HashMap<String, Integer> resource = new HashMap<>();
            resource.put("site.vm", 8); // this must be smaller than the test we expect to fail (4 XO Xlarge)
            resource.put("site.vlan", 2);
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
    public void runEmbedding_withXOXlargeRequests_tooLarge() throws Exception {
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
        err = cloudHandler.runEmbedding("http://geni-orca.renci.org/owl/uvanlvmsite.rdf#uvanlvmsite/Domain", request, domainResourcePools);
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