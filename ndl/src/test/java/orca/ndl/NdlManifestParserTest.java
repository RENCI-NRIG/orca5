package orca.ndl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

public class NdlManifestParserTest implements INdlManifestModelListener {

    public void ndlNode(Resource ce, OntModel om, Resource ceClass, List<Resource> interfaces) {
        // TODO Auto-generated method stub

    }

    public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth, long latency, List<Resource> interfaces) {
        // TODO Auto-generated method stub

    }

    public void ndlInterface(Resource l, OntModel om, Resource conn, Resource node, String ip, String mask) {
        // TODO Auto-generated method stub

    }

    public void ndlParseComplete() {
        // TODO Auto-generated method stub

    }

    public void ndlManifest(Resource i, OntModel m) {
        // TODO Auto-generated method stub

    }

    public void ndlLinkConnection(Resource l, OntModel m, List<Resource> interfaces, Resource parent) {
        // TODO Auto-generated method stub

    }

    public void ndlCrossConnect(Resource c, OntModel m, long bw, String label, List<Resource> interfaces,
            Resource parent) {
        // TODO Auto-generated method stub

    }

    public void ndlNetworkConnectionPath(Resource c, OntModel m, List<List<Resource>> paths, List<Resource> roots) {
        System.out.println("Network Connection Path: " + c);
        if (roots != null) {
            System.out.println("Printing roots");
            for (Resource rr : roots) {
                System.out.println(rr);
            }
        }
        if (paths != null) {
            System.out.println("Printing paths");
            for (List<Resource> p : paths) {
                StringBuilder sb = new StringBuilder();
                sb.append("   Path: ");
                for (Resource r : p) {
                    sb.append(r + " ");
                }
                System.out.println(sb.toString());
            }

        } else
            System.out.println("   None");

    }

    private void run_(String reqFile) throws NdlException, FileNotFoundException {
        InputStream is = this.getClass().getResourceAsStream(reqFile);
        if (is == null) {
            is = new FileInputStream(reqFile);
        }
        assert (is != null);
        String r = new Scanner(is).useDelimiter("\\A").next();

        NdlManifestParserTest pt = new NdlManifestParserTest();
        NdlManifestParser mp = new NdlManifestParser(r, pt);
        mp.processManifest();
        mp.freeModel();
    }

    private static String[] manifests = { "/test-color-extension-manifest.rdf", "/manifest-node-sharedvlan.rdf",
            "/manifest-node-intra.rdf" };

    @Test
    public void run() throws NdlException, IOException {
        for (String r : manifests) {
            System.out.println("++++++++++");
            System.out.println("Running manifest " + r);
            run_(r);
        }
    }

    public static void main(String[] argv) {
        NdlManifestParserTest t = new NdlManifestParserTest();

        try {
            t.run_("/Users/ibaldin/Desktop/broken-paths.rdf");
        } catch (Exception e) {
            System.out.println("UNABLE: " + e);
            e.printStackTrace();
        }
    }

}
