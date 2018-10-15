package net.exogeni.orca.ndl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import net.exogeni.orca.ndl.elements.LabelSet;

import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

public class NdlAbstractDelegationParserTest implements INdlAbstractDelegationModelListener {

    public void ndlNetworkDomain(Resource dom, OntModel m, List<Resource> netService, List<Resource> interfaces,
            List<LabelSet> labelSets, Map<Resource, List<LabelSet>> netLabelSets) {
        System.out.println("See a network domain " + dom);
        System.out.println("  with network services " + netService);
        System.out.println("  with labelSets " + labelSets);
        System.out.println("  with interfaces " + interfaces);
        System.out.println("  with vlan label sets " + netLabelSets);

    }

    public void ndlInterface(Resource l, OntModel om, Resource conn, Resource node, String ip, String mask) {
        System.out.println("See interface " + l + " of " + node);

    }

    public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth, long latency, List<Resource> interfaces) {
        System.out.println("See network connection " + l);

    }

    public void ndlNode(Resource ce, OntModel om, Resource ceClass, List<Resource> interfaces) {
        System.out.println("See node " + ce);

    }

    public void ndlParseComplete() {
        System.out.println("Done parsing");
    }

    private void run_(String reqFile) throws NdlException {
        InputStream is = this.getClass().getResourceAsStream(reqFile);
        assert (is != null);
        String r = new Scanner(is).useDelimiter("\\A").next();

        NdlAbstractDelegationParserTest nrpt = new NdlAbstractDelegationParserTest();

        NdlAbstractDelegationParser nrp = new NdlAbstractDelegationParser(r, nrpt);
        nrp.processDelegationModel();
        nrp.freeModel();
    }

    private static String[] ads = { "/rci-ad.rdf" };

    @Test
    public void run() throws NdlException, IOException {
        for (String r : ads) {
            System.out.println("++++++++++");
            System.out.println("Running ad " + r);
            run_(r);
        }
    }
}
