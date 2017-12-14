package orca.handlers.network.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public abstract class ConsoleDevice implements INetworkDevice {
    protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
    protected String name = "ConsoleDevice";
    protected String basepath;
    protected XMLReader reader;
    protected boolean isEmulation = false;
    protected String deviceAddress;
    protected String uid;
    protected String password;

    public ConsoleDevice(String deviceAddress, String uid, String password) {
        this.deviceAddress = deviceAddress;
        this.uid = uid;
        this.password = password;
    }

    /**
     * Executes the specified command
     * 
     * @param cmd
     *            command
     * @param response
     *            expected response
     * @param timeout
     *            timeout
     * @throws CommandException
     */
    public abstract void executeCommand(String cmd, String response, String timeout) throws CommandException;

    protected Properties getProperties() {
        Properties p = new Properties();
        return p;
    }

    /**
     * Load script as an input source
     * 
     * @param cmd
     * @return
     * @throws CommandException
     */
    protected InputSource loadScript(String cmd) throws CommandException {
        if (basepath == null) {
            throw new CommandException("basepath is missing");
        }
        String path = basepath + "/" + cmd + ".xml";
        logger.debug("Loading script source: " + path);
        URL url = this.getClass().getResource(path);
        if (url == null) {
            throw new CommandException("Could not find script: " + path);
        }
        return new InputSource(url.toString());
    }

    /**
     * Load script as a string
     * 
     * @param name
     * @return
     * @throws CommandException
     */
    protected String loadScriptString(String name) throws CommandException {
        if (basepath == null) {
            throw new CommandException("basepath is missing");
        }
        String path = basepath + "/" + name;
        logger.debug("Loading script source: " + path);
        URL url = this.getClass().getResource(path);
        if (url == null) {
            throw new CommandException("Could not find script: " + path);
        }
        try {
            String ret = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String frag;
            while ((frag = br.readLine()) != null) {
                ret += frag + "\n";
            }
            return ret;
        } catch (IOException e) {
            throw new CommandException("Unable to load script " + name);
        }
    }

    protected RequestScriptHandler getRequestHandler() {
        return new RequestScriptHandler(this, basepath, null);
    }

    /**
     * Executes the specified script
     * 
     * @param script
     *            script to execute
     * @param in
     * @throws CommandException
     */
    public void executeScript(String script, Properties properties) throws CommandException {
        logger.debug("Executing script: " + script + " properties=" + properties);

        // load the script
        InputSource input = loadScript(script);
        // connect to the device, if needed
        if (!isConnected()) {
            connect();
        }
        // parse the script and execute it
        try {
            reader = XMLReaderFactory.createXMLReader();
            RequestScriptHandler handler = new RequestScriptHandler(this, basepath, properties);
            reader.setContentHandler(handler);
            reader.parse(input);
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (isConnected()) {
            disconnect();
        }
        super.finalize();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void enableEmulation() {
        isEmulation = true;
    }

    public void disableEmulation() {
        isEmulation = false;
    }

    public boolean isEmulationEnabled() {
        return isEmulation;
    }

}
