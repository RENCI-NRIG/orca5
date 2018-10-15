/**
 * 
 */
package orca.network;

import junit.framework.TestCase;
import orca.ndl.OntProcessor;
import orca.ndl.LayerConstant.Layer;
import orca.ndl.elements.Interface;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * @author yxin
 *
 */

public class getAvailableLabelTest extends TestCase {

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */

    String inputFileName;
    OntProcessor mapper;

    protected void setUp() throws Exception {
        super.setUp();
        inputFileName = "orca/network/ben-6509.rdf";
        mapper = new OntProcessor(inputFileName);
        // mapper.getOntModel().write(System.out);
    }

    @SuppressWarnings("static-access")
    public void testGetLayer() {
        System.out.println("--------GetLayer-------\n");
        String uri = "http://geni-orca.renci.org/owl/ben-dtn.rdf#Duke/Infinera/DTN/fB/1/ocgB/1";
        System.out.println(mapper.getOntModel().getResource(uri).getLocalName());
        ResultSet results = mapper.getLayer(mapper.getOntModel(), uri);
        String layerName = null;
        String varName = (String) results.getResultVars().get(0);
        while (results.hasNext()) {
            layerName = results.nextSolution().getResource(varName).getLocalName();
            System.out.println(layerName);

        }

        assertTrue(layerName != null);
    }

    @SuppressWarnings("static-access")
    public void testConnectedTo() {
        System.out.println("--------ConnectedTo-------\n");
        String url1 = "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/2/fiber";
        String selectStr = "SELECT ?intf ";
        String fromStr = "";
        String whereStr = "WHERE {" + "<" + url1 + ">" + " ndl:linkTo ?intf. " + "      }";

        OntModel ont1 = mapper.getOntModel();

        String queryPhrase = mapper.createQueryString(selectStr, fromStr, whereStr);

        System.out.println(queryPhrase);

        mapper.outputQueryResult(mapper.rdfQuery(ont1, queryPhrase));
    }

    public void testGetAvailableLabelSet() {

        System.out.println("----------------GetAvailableLabelSet Test");

        boolean valid = false;
        String uri = "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/2/ethernet";
        // String uri="http://geni-orca.renci.org/owl/ben-dtn.rdf#Renci/Infinera/DTN/fB/1/ocgB/1";
        OntModel ontModel = mapper.getOntModel();
        OntResource intf_rs = ontModel.getOntResource(uri);
        System.out.println(intf_rs);
        Interface intf = new Interface(mapper, intf_rs, false);
        String rsURI = intf.getResource().getURI();
        String layer = intf.getAtLayer();
        String availableLabelSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getASet();
        String usedLabelSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getUSet();
        String usedSet = intf.getUri() + "/" + Layer.valueOf(layer).getUSet();

        ResultSet results = mapper.getAvailableLabelSet(rsURI, availableLabelSet);
        // mapper.outputQueryResult(results);

        int lower = 0, upper = 0;

        String lowerBound = (String) results.getResultVars().get(0);
        String upperBound = (String) results.getResultVars().get(1);
        String l = (String) results.getResultVars().get(2);
        String u = (String) results.getResultVars().get(3);
        String element = (String) results.getResultVars().get(4);
        String setElement = (String) results.getResultVars().get(4);
        String availableSet = (String) results.getResultVars().get(5);

        Resource lowerLabel = null;
        Resource upperLabel = null;
        Resource labelRange_rs = null;
        Resource availableSet_rs = null;
        QuerySolution solution;
        if (results.hasNext()) {
            solution = results.nextSolution();
            availableSet_rs = solution.getResource(availableSet);
            labelRange_rs = solution.getResource(setElement);

            if (solution.getLiteral(lowerBound) != null) {
                lower = solution.getLiteral(lowerBound).getInt();
                upper = solution.getLiteral(upperBound).getInt();
                lowerLabel = solution.getResource(l);
                upperLabel = solution.getResource(u);
                System.out.println(lower + ":" + upper);
            } else {
                lowerLabel = labelRange_rs;
                lower = lowerLabel.getProperty(mapper.label_ID).getInt();
            }
            valid = true;
            System.out.println("Label Range:" + lower + ":" + upper + ":" + lowerLabel + ":" + upperLabel);
        }

        mapper.ontLabelUpdate(availableSet_rs, labelRange_rs, lower, upper, lowerLabel, upperLabel, 150);

        results = mapper.getAvailableLabelSet(rsURI, availableLabelSet);
        mapper.outputQueryResult(results);

        assertTrue(valid);
    }

    public void testRemoveAvailableLabelSet() {
        System.out.println("----------------RemoveAvailableLabelSet Test");
        ResultSet results;
        OntModel model = mapper.getOntModel();
        Property lowerBound = model.getProperty("http://geni-orca.renci.org/owl/layer.owl#lowerBound");
        Property hasInterface = model.getProperty("http://geni-orca.renci.org/owl/topology.owl#hasInterface");
        Property element = model.getProperty("http://geni-orca.renci.org/owl/collections.owl#element");
        String availSet = "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/availableVLANSet";
        String url250 = "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/VLANLabel/250";
        String url = "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/availableVLANSet/1";
        String url_dv = "http://geni-orca.renci.org/owl/ben.rdf#Renci/Cisco/6509";
        Resource rs_availSet = model.getResource(availSet);
        Resource rs_url250 = model.getResource(url250);
        Resource rs_label = model.getResource(url);
        Resource rs_dv = model.getResource(url_dv);
        Resource rs = model.getResource(
                "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/1/fiber");
        rs_label.removeAll(lowerBound);

        StmtIterator it = model.listStatements(rs_availSet, element, rs_url250);
        Statement st = null;
        while (it.hasNext()) {
            st = it.nextStatement();
            System.out.println(st + "\n");
        }

        rs_availSet.removeAll(element);

        rs_dv = model.getResource(rs_dv.getURI());
        System.out.println(":" + hasInterface + ":" + rs_dv.getProperty(hasInterface));
        String s = "SELECT ?p ?b ?s ?e ?pp ?d ";
        String f = "";
        String w = "WHERE {" + "<" + url_dv + "> " + "?p ?b." + "?b layer:availableLabelSet ?s."
                + "?s collections:element ?e. " +
                // "?e ?pp ?d "+

                "}";

        String queryPhrase = mapper.createQueryString(s, f, w);

        results = mapper.rdfQuery(model, queryPhrase);

        mapper.outputQueryResult(results);

    }
}
