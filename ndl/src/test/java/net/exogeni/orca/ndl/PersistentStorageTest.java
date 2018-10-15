package net.exogeni.orca.ndl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This helps test persistent storage options in Jena (TDB etc)
 * 
 * @author ibaldin
 *
 */
public class PersistentStorageTest implements INdlManifestModelListener, INdlRequestModelListener {
    private boolean requestPhase = false;

    public void loadRequest(String f, NdlModel.ModelType t, String folderName) {
        System.out.println("Loading request " + f);

        BufferedReader bin = null;
        try {
            FileInputStream is = new FileInputStream(f);
            bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = bin.readLine()) != null) {
                sb.append(line);
                // re-add line separator
                sb.append(System.getProperty("line.separator"));
            }

            bin.close();

            NdlRequestParser nrp = new NdlRequestParser(sb.toString(), this, t, folderName);
            nrp.processRequest();

            nrp.freeModel();

        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }

    }

    public void loadManifest(String f, NdlModel.ModelType t, String folderName) {
        System.out.println("Loading manifest " + f);
        BufferedReader bin = null;
        try {
            FileInputStream is = new FileInputStream(f);
            bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = bin.readLine()) != null) {
                sb.append(line);
                // re-add line separator
                sb.append(System.getProperty("line.separator"));
            }

            bin.close();

            // parse as request
            NdlRequestParser nrp = new NdlRequestParser(sb.toString(), this, t, folderName);

            nrp.doLessStrictChecking();
            nrp.processRequest();
            // nrp.freeModel();

            // parse as manifest
            requestPhase = false;
            NdlManifestParser nmp = new NdlManifestParser(sb.toString(), this, t, folderName);
            nmp.processManifest();
            // nmp.freeModel();

        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }

    public static void main(String[] argv) {

        if (argv.length == 0) {
            System.err.println("request or manifest?");
            System.exit(1);
        }

        if (argv.length == 1) {
            System.err.println("file name? ");
            System.exit(1);
        }

        PersistentStorageTest pst = new PersistentStorageTest();

        long startTime = System.currentTimeMillis();

        if ("request".equalsIgnoreCase(argv[0])) {
            pst.loadRequest(argv[1], NdlModel.ModelType.TdbEphemeral, "/tmp");
        } else {
            pst.loadManifest(argv[1], NdlModel.ModelType.TdbEphemeral, "/tmp");
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Total time: " + elapsedTime + "ms");

        // Get the Java runtime
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();
        // Calculate the used memory
        long memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used memory is bytes: " + memory);
        System.out.println("Used memory is megabytes: " + memory / (1024.0 * 1024.0));
    }

    public void ndlCrossConnect(Resource c, OntModel m, long bw, String label, List<Resource> interfaces,
            Resource parent) {
        // TODO Auto-generated method stub

    }

    public void ndlLinkConnection(Resource l, OntModel m, List<Resource> interfaces, Resource parent) {
        // TODO Auto-generated method stub

    }

    public void ndlManifest(Resource i, OntModel m) {
        // TODO Auto-generated method stub

    }

    public void ndlNetworkConnectionPath(Resource c, OntModel m, List<List<Resource>> path, List<Resource> roots) {
        // TODO Auto-generated method stub

    }

    public void ndlInterface(Resource l, OntModel om, Resource conn, Resource node, String ip, String mask) {
        // TODO Auto-generated method stub

    }

    public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth, long latency, List<Resource> interfaces) {
        // TODO Auto-generated method stub

    }

    public void ndlNode(Resource ce, OntModel om, Resource ceClass, List<Resource> interfaces) {
        // TODO Auto-generated method stub

    }

    public void ndlParseComplete() {
        // TODO Auto-generated method stub

    }

    public void ndlBroadcastConnection(Resource bl, OntModel om, long bandwidth, List<Resource> interfaces) {
        // TODO Auto-generated method stub

    }

    public void ndlNodeDependencies(Resource ni, OntModel m, Set<Resource> dependencies) {
        // TODO Auto-generated method stub

    }

    public void ndlReservation(Resource i, OntModel m) {
        // TODO Auto-generated method stub

    }

    public void ndlReservationEnd(Literal e, OntModel m, Date end) {
        // TODO Auto-generated method stub

    }

    public void ndlReservationResources(List<Resource> r, OntModel m) {
        // TODO Auto-generated method stub

    }

    public void ndlReservationStart(Literal s, OntModel m, Date start) {
        // TODO Auto-generated method stub

    }

    public void ndlReservationTermDuration(Resource d, OntModel m, int years, int months, int days, int hours,
            int minutes, int seconds) {
        // TODO Auto-generated method stub

    }

    public void ndlSlice(Resource sl, OntModel m) {
        // TODO Auto-generated method stub

    }
}
