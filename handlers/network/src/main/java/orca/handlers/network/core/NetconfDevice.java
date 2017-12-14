package orca.handlers.network.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.juniper.netconf.Device;
import net.juniper.netconf.NetconfException;
import net.juniper.netconf.XML;

import org.xml.sax.SAXException;

/**
 * Implements a Netconf Device using Juniper's netconf java library
 * 
 * @author ibaldin
 *
 */
public class NetconfDevice extends ConsoleDevice {
    protected String name = "NetconfDevice";
    protected Device device = null;
    private boolean connected = false;

    public NetconfDevice(String deviceAddr, String uid, String pass) throws CommandException {
        super(deviceAddr, uid, pass);
        try {
            device = new Device(deviceAddr, uid, pass, null);
        } catch (Exception nee) {
            device = null;
            throw new CommandException("Unable to initialize netconf device " + deviceAddress + ": " + nee);
        }
    }

    public void connect() throws CommandException {
        try {
            if (!isEmulationEnabled()) {
                device.connect();
            }
            connected = true;
        } catch (NetconfException nee) {
            throw new CommandException("Unable to connect to netconf device " + deviceAddress + " due to: " + nee);
        }
    }

    public void disconnect() {
        if ((device != null) && connected)
            device.close();
    }

    /**
     * Get a text of current device configuration as text (multi-line)
     * 
     * @return
     * @throws CommandException
     */
    public String getDeviceConfiguration() throws CommandException {
        try {
            XML ret = device.getRunningConfig();
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/rpc-reply/data/configuration-text");
            return (String) expr.evaluate(ret.getOwnerDocument(), XPathConstants.STRING);
        } catch (IOException ioe) {
            throw new CommandException("Unable to retrieve netconf configuration from " + deviceAddress + ": " + ioe);
        } catch (SAXException se) {
            throw new CommandException("Unable to retrieve netconf configuration from " + deviceAddress + ": " + se);
        } catch (XPathExpressionException xe) {
            return null;
        }
    }

    /**
     * Get a buffered reader of the configuration (convenient for reading line-by-line)
     * 
     * @return
     * @throws CommandException
     */
    public BufferedReader getDeviceConfigurationReader() throws CommandException {
        return new BufferedReader(new StringReader(getDeviceConfiguration()));
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Execute a configuration change command (in this case response and timeout are ignored)
     */
    public void executeCommand(String cmd, String response, String timeout) throws CommandException {
        executeCommand(cmd);
    }

    /**
     * Execute a configuration change command modifying runtime configuration
     * 
     * @param cmd
     * @throws CommandException
     */
    public void executeCommand(String cmd) throws CommandException {
        if (device == null)
            throw new CommandException("Netconf device " + deviceAddress + " not connected");

        try {
            device.loadRunningTextConfiguration(cmd, "merge");
        } catch (Exception e) {
            throw new CommandException(
                    "Unable to update configuration of netconf device " + deviceAddress + " due to: " + e);
        }
    }

    /**
     * Perform variable substitutions in the script. Variable names and values are in the properties. Variables in the
     * script should appear as {$VariableName}
     * 
     * @param data
     * @param props
     * @return
     * @throws CommandException
     */
    public static String replaceVars(String data, Properties props) throws CommandException {
        String cmd = data;
        String tagPattern = "\\{\\$([^\\{\\}]+)\\}";
        String subPattern = null;
        String val = null;

        Pattern tagPatternComp = Pattern.compile(tagPattern);
        Matcher match = tagPatternComp.matcher(cmd);

        try {
            while (match.find()) {
                String tag = match.group(1);
                val = props.getProperty(tag);
                if (val == null)
                    throw new RuntimeException("Unable to find property " + tag + " expected by the script!");

                val = val.replace("/", "\\/");
                val = val.replace("$", "\\$");

                subPattern = "\\{\\$" + tag + "\\}";
                cmd = cmd.replaceAll(subPattern, val);
            }
        } catch (PatternSyntaxException e) {
            throw new CommandException("Unable to perform variable substitution for IBM router: " + e);
        }

        return cmd;
    }

}
