package orca.controllers;

import java.util.ArrayList;

import org.springframework.ws.client.WebServiceIOException;

import orca.manage.IOrcaContainer;
import orca.manage.IOrcaServiceManager;
import orca.manage.Orca;
import orca.manage.OrcaError;
import orca.util.ID;

public class OrcaConnectionFactory {
    public static final int MAX_OBJECTS = 10;

    private final String url;
    private final String user;
    private final String password;
    private final ID smGuid;

    private IOrcaContainer container;
    private ArrayList<IOrcaServiceManager> connections = new ArrayList<IOrcaServiceManager>();

    public OrcaConnectionFactory(String url, String user, String password, ID smGuid) {
        if (smGuid == null) {
            throw new IllegalArgumentException("smGuid cannot be null");
        }

        if (url == null && (user == null || password == null)) {
            throw new IllegalArgumentException(
                    "Both user name and password must be specified when using non-local JVM communication");
        }

        this.url = url;
        this.user = user;
        this.password = password;
        this.smGuid = smGuid;
    }

    private void connect() throws Exception {
        // WRITEME: quiet period between reconnect attempts?

        OrcaController.Log.info("Attempting to establish connection with Orca");

        if (url == null) {
            OrcaController.Log.info("Connecting to the Orca instance in the same JVM");
            container = Orca.connect();
        } else {
            container = Orca.connect(url, user, password);
        }
        OrcaController.Log.info("Successfully connected to the Orca server");
    }

    public synchronized IOrcaServiceManager getServiceManager() throws Exception {
        IOrcaServiceManager sm = null;
        if (container == null) {
            connect();
        }

        if (connections.isEmpty()) {
            sm = container.getServiceManager(smGuid);
            if (sm == null) {
                OrcaError error = container.getLastError();
                if (isIOError(error)) {
                    container = null;
                }
                throw new Exception("Could not connect to the Service Manager: " + error);
            }
        } else {
            sm = connections.remove(0);
        }
        return sm;
    }

    private boolean isIOError(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof WebServiceIOException) {
            return true;
        }
        return isIOError(t.getCause());
    }

    private boolean isIOError(OrcaError error) {
        if (error != null) {
            return isIOError(error.getException());
        }
        return false;
    }

    public synchronized void returnServiceManager(IOrcaServiceManager sm) {
        if (sm == null) {
            throw new IllegalArgumentException("sm cannot be null");
        }

        if (container == null || connections.size() > MAX_OBJECTS) {
            return;
        }

        // WRITEME: also check for expired login/session

        if (isIOError(sm.getLastError())) {
            OrcaController.Log.debug("Detected a proxy with I/O errors. Disconnecting");
            connections.clear();
            container = null;
        } else {
            connections.add(sm);
        }
    }
}