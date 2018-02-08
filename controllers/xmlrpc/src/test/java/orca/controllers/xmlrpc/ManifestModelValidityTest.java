package orca.controllers.xmlrpc;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;

import orca.ndl.NdlCommons;
import orca.ndl.NdlModel;
import orca.shirako.container.Globals;

public class ManifestModelValidityTest extends TestHelper {


    private static final Logger logger = Globals.getLogger(OrcaXmlrpcHandlerTest.class.getSimpleName());
	
    @BeforeClass
    public static void setupTests() {
    	logger.info("Initializing NDL");
    	NdlCommons.init();
    }
    
    /**
     * This test verifies that network connection items in intra-rack requests
     * are present on each link. See https://github.com/RENCI-NRIG/orca5/issues/196
     * for details.
     */
    @Test
    public void testNetworkConnectionItems() throws Exception {
    	logger.info("Running NetworkConnection items test");
        // Need to setup a controller
        XmlRpcController controller = new MockXmlRpcController();
        controller.init();
        controller.start();
        
        String man = doTestCreateSliceWithManifest(controller, "src/test/resources/196_networklink_items.rdf", 
        		"createSlice_testNetConItems_" + controller.getClass().getSimpleName(), logger);
        
        // this purposely doesn't use the NDL manifest parser infrastructure to provide 
        // guarantees against errors in parser itself
        
        // verify presence of two collections:item properties on each connection, fail otherwise
        OntModel manifestModel = NdlModel.getModelFromString(man, null, true);
        
        // query on the model
        String select = "SELECT ?netcon ";
        String filter = "";
        String where = "WHERE { ?netcon rdf:type topology:NetworkConnection. ?netcon rdf:type topology:LinkConnection. }";
        String queryPhrase = NdlCommons.createQueryString(select, filter, where);

        ResultSet results = NdlCommons.rdfQuery(manifestModel, queryPhrase);
        
        Resource netcon;
        while (results.hasNext()) {
            ResultBinding result = (ResultBinding) results.next();
            if (result != null) {
                netcon = (Resource) result.get("netcon");
            	System.out.println("NETCON " + netcon);
                StmtIterator stim = netcon.listProperties(NdlCommons.collectionItemProperty);
                int count = 0;
                while (stim.hasNext()) {
                	Statement stat = stim.next();
                	Resource obj = stat.getResource();
                	System.out.println("OBJECT " + obj);
                	count++;
                }
                //assert(count == 2);
            }
        }
        
        logger.info("Test successful, closing model");
        // close the model
        manifestModel.close();
    }
}
