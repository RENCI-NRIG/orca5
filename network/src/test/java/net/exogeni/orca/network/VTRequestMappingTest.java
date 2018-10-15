package orca.network;

import java.io.IOException;

import junit.framework.TestCase;

public class VTRequestMappingTest extends TestCase {

    String requestFileName, substrateFileName, currentsubstrateFileName;
    VTRequestMapping mapping;

    public VTRequestMappingTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        requestFileName = "orca/network/request-vt-1.rdf";
        // substrateFileName="orca/network/mp-request-20-duke.rdf";
        // currentsubstrateFileName="src/main/resources/orca/network/duke-20-current.dot";
        substrateFileName = "orca/network/nsf.rdf";
        currentsubstrateFileName = "src/main/resources/orca/network/nsf-current.dot";
        mapping = new VTRequestMapping(requestFileName, substrateFileName, currentsubstrateFileName);
    }

    public void testDirectVTMapping() {
        mapping.directVTMapping();
    }

    public void testVTMapping() throws IOException {
        mapping.vtMapping();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
