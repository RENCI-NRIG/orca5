package orca.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import orca.manage.IOrcaServiceManager;
import orca.manage.beans.ProxyMng;
import orca.security.AbacUtil;
import orca.shirako.common.ConfigurationException;
import orca.shirako.container.Globals;
import orca.util.ID;
import orca.util.PathGuesser;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class OrcaController {
    public static final String HomeDirectory = PathGuesser.getOrcaControllerHome();
    public static final String ConfigDirectory = HomeDirectory + System.getProperty("file.separator") + "config"
            + System.getProperty("file.separator");
    protected static Properties controllerProperties;
    public static final String ControllerConfigurationFile = ConfigDirectory + "controller.properties";
    public static final Logger Log = makeLogger();

    public static final String RootLoggerName = "controller";
    public static final String OrcaURL = "orca.manage.url";
    public static final String OrcaUser = "orca.manage.user";
    public static final String OrcaLogin = "orca.manage.password";
    public static final String ControllerServiceManager = "controller.sm.guid";
    public static final String CometPubKeysEnabled = "orca.comet.pubkeys.enabled";
    public static final String CometHostNamesEnabled = "orca.comet.hosts.enabled";

    public OrcaConnectionFactory orca;
    protected boolean fresh = true;

    static {
        System.setProperty(AbacUtil.ABAC_ROOT, HomeDirectory);
        try {
            loadProperties();
        } catch (Exception e) {
            Log.error(e);
            System.exit(1);
        }
    }

    protected void init() throws Exception {
        initConnectionFactory();
        determineBootMode();
        if (fresh == true)
            createLock();
    }

    protected void recover() throws Exception {
        if (fresh)
            Log.info("Starting controller fresh");
        else {
            Log.info("Starting " + this.getClass().getSimpleName() + " recovery");
            _recover();
        }
    }

    protected void _recover() throws Exception {

    }

    public ID getBroker(IOrcaServiceManager sm) {
        List<ProxyMng> brokers = sm.getBrokers();
        if (brokers != null) {
            return new ID(brokers.get(0).getGuid());
        }
        return null;
    }

    private void initConnectionFactory() throws Exception {
        String smGuid = controllerProperties.getProperty(ControllerServiceManager);
        if (smGuid == null) {
            throw new ConfigurationException("Please specify a service manager to connect to");
        }

        String url = controllerProperties.getProperty(OrcaURL);
        String user = controllerProperties.getProperty(OrcaUser);
        String password = controllerProperties.getProperty(OrcaLogin);

        orca = new OrcaConnectionFactory(url, user, password, new ID(smGuid));
    }

    public static String getProperty(String p) {
        return controllerProperties.getProperty(p);
    }

    private static void loadProperties() throws ConfigurationException {
        controllerProperties = new Properties();
        File f = new File(ControllerConfigurationFile);
        Log.info("Attempting to load controller configuration from: " + f.getAbsolutePath());
        if (!f.exists()) {
            throw new ConfigurationException(
                    "Could not locate controller configuration file at: " + f.getAbsolutePath());
        } else {
            try {
                controllerProperties.load(new FileInputStream(f));
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    private static Logger makeLogger() {
        String logFile = null;
        try {
            Properties p = new Properties();
            File f = new File(ControllerConfigurationFile);
            p.load(new FileInputStream(f));
            p.setProperty("log4j.appender.file.File",
                    HomeDirectory + "logs" + System.getProperty("file.separator") + "controller.log");
            p.setProperty("log4j.appender.ndl.appender.File",
                    HomeDirectory + "logs" + System.getProperty("file.separator") + "ndl.log");
            PropertyConfigurator.configure(p);
        } catch (Exception e) {
            System.err.println("Could not initialize log4j: " + logFile + "\n");
            e.printStackTrace();
        }
        // return the "controller" logger
        return Logger.getLogger(RootLoggerName);
    }

    public static Logger getLogger(String name) {
        String temp = name;
        if (!temp.startsWith(RootLoggerName)) {
            temp = RootLoggerName + "." + name;
        }
        return Logger.getLogger(temp);
    }

    private void determineBootMode() {
        String fileName = Globals.ControllerLockLocation;
        File file = new File(fileName);
        Globals.Log.debug("Checking if this controller is recovering. Looking for: " + fileName);
        if (file.exists()) {
            Globals.Log.debug("Found lock file. This controller is recovering");
            fresh = false;
        } else {
            Globals.Log.debug("Recovery lock file does not exist. This is a fresh controller");
            fresh = true;
        }
    }

    private void createLock() throws Exception {
        Globals.Log.debug("Creating controller lock file");
        File f = new File(Globals.ControllerLockLocation);
        PrintWriter w = new PrintWriter(new FileOutputStream(f));
        try {
            w.println(
                    "This file tells the Orca Controller to maintain its state on recovery. \nHence, removing this file will make the controller discard and reset its state.");
            Globals.Log.debug("Lock file created successfully");
        } finally {
            w.close();
        }
    }

}
