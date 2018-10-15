package net.exogeni.orca.server;

/**
 * Utility class to start and stop an Orca instance.
 */
public class OrcaTestServer {
    public static OrcaServer server;

    /**
     * Starts Orca discarding all prior state.
     * 
     * @throws Exception
     */
    public static void startServer() throws Exception {
        if (server != null) {
            stopServer();
        }
        if (System.getProperty("ORCA_HOME") == null) {
            System.setProperty("ORCA_HOME", "orca");
        }
        server = new OrcaServer(true);
        server.start();
    }

    /**
     * Starts Orca, potentially recovering from prior state.
     * 
     * @throws Exception
     */
    public static void startOrRecoverServer() throws Exception {
        if (server != null) {
            stopServer();
        }
        if (System.getProperty("ORCA_HOME") == null) {
            System.setProperty("ORCA_HOME", "orca");
        }
        server = new OrcaServer();
        server.start();
    }

    /**
     * Stops Orca.
     * 
     * @throws Exception
     */
    public static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}