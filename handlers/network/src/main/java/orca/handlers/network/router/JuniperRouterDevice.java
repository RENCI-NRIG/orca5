package orca.handlers.network.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import orca.handlers.network.core.CommandException;

public class JuniperRouterDevice extends JunosSSLDevice implements IRouterDevice, RouterConstants {
    protected String commandBuffer;
    protected String basepath;

    protected static final String CreateVLANScript = "CreateVLAN.txt";
    protected static final String CreateQoSVLANScript = "CreateQoSVLAN.txt";
    protected static final String DeleteVLANScript = "DeleteVLAN.txt";
    protected static final String DeleteQoSVLANScript = "DeleteQoSVLAN.txt";
    protected static final String AddAccessPortStub = "AddAccessPort.txt";
    protected static final String AddTrunkPortStub = "AddTrunkPort.txt";
    protected static final String RemoveAccessPortStub = "RemoveAccessPort.txt";
    protected static final String RemoveTrunkPortStub = "RemoveTrunkPort.txt";
    protected static final String MapVlansStub = "MapVLANs.txt";
    protected static final String UnmapVlansStub = "UnmapVLANs.txt";

    // interface name patterns
    protected static final String expectedPat_1 = "^\\s*([gx]e-)(\\[\\d+-\\d+\\]|\\d+)/(\\[\\d+-\\d+\\]|\\d+)/(\\[\\d+-\\d+\\]|\\d+)\\s*$";
    protected static final String expectedPat_2 = "^\\s*(ae)(\\[\\d+-\\d+\\]|\\d+)\\s*$";
    protected static final String rangesPat = "^\\[(\\d+)-(\\d+)\\]$";

    protected static final Pattern expectedComp_1 = Pattern.compile(expectedPat_1);
    protected static final Pattern expectedComp_2 = Pattern.compile(expectedPat_2);
    protected static final Pattern rangesComp = Pattern.compile(rangesPat);

    protected static final String DeleteWord = "delete: ";

    public JuniperRouterDevice(String deviceAddress, String uid, String password, String basepath) {
        super(deviceAddress, uid, password);
        this.basepath = basepath;
    }

    protected Properties getProperties() {
        return new Properties();
    }

    @Override
    protected String getConfigurationText() {
        return commandBuffer;
    }

    protected void genericUpdateCommand(String commandName) throws CommandException {
        // remove CRs from the commandBuffer
        // commandBuffer = commandBuffer.replaceAll("\\s*\\n+\\s*", " ");
        // commandBuffer = commandBuffer.replaceAll("\\s+", " ");
        if (!isEmulationEnabled()) {
            // connect to device and update configuration
            connect();
            execute();
            disconnect();
        } else {
            System.out.println(commandName
                    + " command buffer: <rpc> <load-configuration action=\"merge\" format=\"text\"> <configuration-text> "
                    + commandBuffer + " </configuration-text></load-configuration></rpc>");
        }
    }

    protected void genericDeleteCommand(String commandName) throws CommandException {
        // commandBuffer = DeleteWord + commandBuffer;
        genericUpdateCommand(commandName);
    }

    private static List<String> expandInterface_1(String s) throws CommandException {
        List<String> ret = new ArrayList<String>();

        // expand available groups
        Matcher matcher = expectedComp_1.matcher(s);
        matcher.find();

        int[] start = new int[3];
        int[] stop = new int[3];
        if (matcher.groupCount() != 4)
            throw new CommandException("Unexpected number of groups " + matcher.groupCount() + " in a pattern!");
        final int firstGroup = 2;
        for (int i = firstGroup; i <= matcher.groupCount(); i++) {
            // System.out.println("Group: " + matcher.group(i));
            // see if this is a range
            if (Pattern.matches(rangesPat, matcher.group(i))) {
                Matcher tmpMat = rangesComp.matcher(matcher.group(i));
                tmpMat.find();
                start[i - firstGroup] = Integer.parseInt(tmpMat.group(1));
                stop[i - firstGroup] = Integer.parseInt(tmpMat.group(2));
                // swap the two if needed
                if (stop[i - firstGroup] < start[i - firstGroup]) {
                    int tmp = stop[i - firstGroup];
                    stop[i - firstGroup] = start[i - firstGroup];
                    start[i - firstGroup] = tmp;
                }
                // System.out.println("Range: " + start[i-firstGroup] + " " + stop[i-firstGroup]);
            } else {
                // not a range
                start[i - firstGroup] = Integer.parseInt(matcher.group(i));
                stop[i - firstGroup] = start[i - firstGroup];
                // System.out.println("Not a range: " + stop[i-firstGroup]);
            }

        }

        // set ranges for three for loops
        for (int ch = start[0]; ch <= stop[0]; ch++)
            for (int pic = start[1]; pic <= stop[1]; pic++)
                for (int port = start[2]; port <= stop[2]; port++)
                    // System.out.println(matcher.group(1) + ch + "/" + pic + "/" + port);
                    ret.add(matcher.group(1) + ch + "/" + pic + "/" + port);

        return ret;
    }

    private static List<String> expandInterface_2(String s) throws CommandException {
        List<String> ret = new ArrayList<String>();

        // expand available groups
        Matcher matcher = expectedComp_2.matcher(s);
        matcher.find();

        int start;
        int stop;
        if (matcher.groupCount() != 2)
            throw new CommandException("Unexpected number of groups " + matcher.groupCount() + " in a pattern!");
        final int i = 2;

        if (Pattern.matches(rangesPat, matcher.group(i))) {
            Matcher tmpMat = rangesComp.matcher(matcher.group(i));
            tmpMat.find();
            start = Integer.parseInt(tmpMat.group(1));
            stop = Integer.parseInt(tmpMat.group(2));
            // swap the two if needed
            if (stop < start) {
                int tmp = stop;
                stop = start;
                start = tmp;
            }
        } else {
            // not a range
            start = Integer.parseInt(matcher.group(i));
            stop = start;
        }

        // set ranges for three for loops
        for (int ch = start; ch <= stop; ch++)
            ret.add(matcher.group(1) + ch);
        return ret;
    }

    /**
     * The parameter e is of the form ge-[a-b]/[c-d]/[e-f], ... where a, b, c, d ,e ,f are non-negative integers
     * 
     * @param e
     * @return a list of string to which this pseudo-regular expression expands
     */
    protected static List<String> expandInterfacePattern(String e) throws CommandException {

        if ((e == null) || (e.length() == 0))
            return new ArrayList<String>();

        List<String> ret = new ArrayList<String>();

        // split along commas
        // then in a loop generate new array elements
        String[] intGroups = e.split(",");

        for (String s : intGroups) {
            // see if it matches the expected pattern for interfaces
            if (Pattern.matches(expectedPat_1, s))
                ret.addAll(expandInterface_1(s));
            else if (Pattern.matches(expectedPat_2, s))
                ret.addAll(expandInterface_2(s));
            else
                throw new CommandException("Interface name " + s + " does not match any available patterns for JunOS");
        }

        return ret;
    }

    /**
     * @param vlanTag
     * @param ports
     *            - of type xe-[0-9]/[0-9]/[0-9], ge-[0-9]/[0-9]/[0-9]
     */
    public void addAccessPortsToVLAN(String vlanTag, String ports) throws CommandException {
        commandBuffer = "";
        String cmd = loadScript(AddAccessPortStub);
        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyAccessPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("JUNOS Command buffer content: " + commandBuffer);
        genericUpdateCommand("addAccessPortsToVLAN");
    }

    public void addTrunkPortsToVLAN(String vlanTag, String ports) throws CommandException {
        commandBuffer = "";
        String cmd = loadScript(AddTrunkPortStub);
        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyTrunkPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("JUNOS Command buffer content: " + commandBuffer);
        genericUpdateCommand("addTrunkPortsToVLAN");
    }

    public void createVLAN(String vlanTag, String qosRate, String qosBurstSize) throws CommandException {
        // form the command buffer
        commandBuffer = "";

        Properties props = getProperties();
        String cmd;
        try {
            if ((qosRate != null) && (qosRate.length() != 0) && (Long.parseLong(qosRate) > 0)) {
                if ((qosBurstSize == null) || (qosBurstSize.length() == 0) || (Long.parseLong(qosBurstSize) <= 0))
                    throw new CommandException(
                            "Burst size " + qosBurstSize + " is null, negative or empty in createVLAN");
                props.setProperty(PropertyQoSRateNm, qosRate);
                props.setProperty(PropertyQoSBurstSizeNm, qosBurstSize);
                props.setProperty(PropertyQoSPolicyNm, PropertyPolicyNamePrefix + vlanTag);
                cmd = loadScript(CreateQoSVLANScript);
            } else
                cmd = loadScript(CreateVLANScript);
        } catch (NumberFormatException e) {
            throw new CommandException("Bitrate and burst size must be numeric: " + qosRate + "/" + qosBurstSize);
        }

        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        commandBuffer = replaceVars(cmd, props);

        logger.debug("JUNOS Command buffer content: " + commandBuffer);

        genericUpdateCommand("createVLAN");
    }

    public void deleteVLAN(String vlanTag, boolean withQoS) throws CommandException {
        // form the command buffer
        commandBuffer = "";
        String cmd;
        if (withQoS)
            cmd = loadScript(DeleteQoSVLANScript);
        else
            cmd = loadScript(DeleteVLANScript);

        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);
        props.setProperty(PropertyQoSPolicyNm, PropertyPolicyNamePrefix + vlanTag);

        // perform parameter substitution
        commandBuffer = replaceVars(cmd, props);

        logger.debug("JUNOS Command buffer content: " + commandBuffer);

        genericDeleteCommand("deleteVLAN");
    }

    public void removeAccessPortsFromVLAN(String vlanTag, String ports) throws CommandException {
        commandBuffer = "";
        String cmd = loadScript(RemoveAccessPortStub);

        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyAccessPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("JUNOS Command buffer content: " + commandBuffer);

        genericDeleteCommand("removeAccessPortsFromVLAN");
    }

    public void removeTrunkPortsFromVLAN(String vlanTag, String ports) throws CommandException {
        commandBuffer = "";
        String cmd = loadScript(RemoveTrunkPortStub);

        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyTrunkPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("JUNOS Command buffer content: " + commandBuffer);

        genericDeleteCommand("removeTrunkPortsFromVLAN");
    }

    // load a text script
    protected String loadScript(String name) throws CommandException {
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
                ret += frag;
            }
            return ret;
        } catch (IOException e) {
            throw new CommandException("Unable to load script " + name);
        }
    }

    // perform substitutions in a script
    protected String replaceVars(String data, Properties props) {
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
            logger.error("Substitution pattern malformed: " + subPattern, e);
            throw new RuntimeException(e);
        }

        return cmd;
    }

    public static void main(String[] argv) {
        String pat = "ge-[0-2]/2/[5-7], ae0, ae[5-18], xe-1/1/[1-7], ae2";
        // String pat = "ae0";

        try {
            List<String> ret = expandInterfacePattern(pat);

            for (String r : ret)
                System.out.println(" " + r);
        } catch (Exception e) {
            System.err.println("Exception " + e);
        }

    }
}
