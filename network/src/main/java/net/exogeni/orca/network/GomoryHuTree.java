package orca.network;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Random;
import orca.ndl.*;
import orca.ndl.elements.NetworkConnection;

import org.apache.log4j.Logger;

import orca.network.policyhelpers.JniLoader;
import orca.network.policyhelpers.SystemNativeError;

public class GomoryHuTree {
    private static final Logger log = Logger.getLogger(GomoryHuTree.class);

    public int[][] Edges = null;
    public int[][] Adj = null;
    public int numEdges, numVertices;
    public float[] capacities = null;

    public GomoryHuTree() {
        JniLoader loader = new JniLoader("gomory");

        if (loader != null) {
            SystemNativeError error = loader.loadJni();
            String message = "Error loading JNI: " + error.getMessage() + " (" + error.getErrno() + ")";

            if (error.getAdditional() != null && !error.getAdditional().isEmpty())
                message += ": " + error.getAdditional();
            if (error.getErrno() != 0)
                log.error(message);
        }
    }

    public void createGraph(int numNode, Hashtable<String, NetworkConnection> requestMap) {
        numVertices = numNode;
        numEdges = requestMap.size();

        Edges = new int[2][numEdges];
        capacities = new float[numEdges];
        Adj = new int[numVertices][numEdges];

        int s1, s2, i = 0;
        float bw;
        NetworkConnection requestConnection = null;
        Random random = new Random();
        for (Entry<String, NetworkConnection> entry : requestMap.entrySet()) {
            requestConnection = entry.getValue();
            s1 = requestConnection.getSn1();
            s2 = requestConnection.getSn2();
            Edges[0][i] = s1 - 1;
            Edges[1][i] = s2 - 1;
            Adj[s1 - 1][i] = 1;
            Adj[s2 - 1][i] = -1;
            capacities[i] = requestConnection.getBw();
            if (capacities[i] == 0.0) {
                capacities[i] = random.nextInt(100) * 10 + 100;
            }
            // System.out.println("Java Graph:"+Edges[0][i]+";"+Edges[1][i]+":"+capacities[i]+"\n");
            i++;
        }
    }

    public static native void gomory(int numVertices, int numEdges, int[][] Edges, float[] capacities, int[][] Adj);
}
