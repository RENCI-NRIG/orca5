package orca.handlers.network.router;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.util.Properties;


import orca.handlers.network.core.CommandException;



public class Ciena8700Device extends RouterSSHPromptDevice implements IMappingRouterDevice, RouterConstants {

    /* The prompt consists of a host-like name and an optional symbol, followed
     * by a greater-than sign and some whitespace.
     */
    protected static final String promptPatternString = "^[a-zA-Z0-9][a-zA-Z0-9_.-]*.?>\\s*";

    protected static final String PropertySubPort = "subPort"; //8700
    protected static final String PropertyQoSSubPort = "qosSubPort"; //8700
    protected static final String PropertyVirtualSwitch = "virtualSwitch"; //8700
    protected static final String CommandAddAccessPort = "AddAccessPortRequest"; //8700
    protected static final String CommandAddQoSAccessPort = "AddAccessQoSPortRequest"; //8700
    protected static final String CommandAddTrunkPort = "AddTrunkPortRequest"; //8700
    protected static final String CommandAddQoSTrunkPort = "AddTrunkQoSPortRequest"; //8700
    protected static final String CommandLogon = "Log_on"; //8700
    protected static final String CommandLogoff = "Log_off"; //8700
    //expectedPat_1 1/2 or [1-2]/2 or 1/[1-2] or [1-2]/[11-2]
    protected static final String rangePortPat = "^\\s*(\\[\\d+-\\d+\\]|\\d+)/(\\[\\d+-\\d+\\]|\\d+)\\s*$";
    protected static final String singlePortPat = "^\\s*(\\d+)/(\\d+)\\s*$";
    protected static final String rangesPat = "^\\[(\\d+)-(\\d+)\\]$";


    protected static final Pattern rangePortComp = Pattern.compile(rangePortPat);
    protected static final Pattern rangesComp = Pattern.compile(rangesPat);


    protected String virtualSwitch;

    public Ciena8700Device(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
        basepath = "/orca/handlers/network/router/ciena/8700";
    }

    @Override
    protected String getPromptPattern() {
        return promptPatternString;
    }

    protected static String genSubPortName(String virtualSwitch, String parentPort, boolean tagged){
        assert parentPort != null;
        assert virtualSwitch != null;

        String delim = "-";
        String prefix = "SP";
        if (tagged)
            return virtualSwitch + delim + prefix + delim + parentPort + "t";
        else
            return virtualSwitch + delim + prefix + delim + parentPort + "u";

    }

    protected static String genQoSSubPortName(String virtualSwitch, String parentPort){
        assert parentPort != null;
        assert virtualSwitch != null;

        String delim = "-";
        String prefix = "QSP";
        return virtualSwitch + delim + prefix + delim + parentPort;

    }

    protected static String genVirtualSwitchName(String vlanTag){
        String prefix = "xo";
        String delim = "-";

        if (vlanTag==null)
            return prefix;
        return prefix + delim + vlanTag;
    }

    private static List<String> expandRange(String s) throws CommandException {
        List<String> ret = new ArrayList<String>();

        // expand available groups
        Matcher matcher = rangePortComp.matcher(s);
        matcher.find();

        int[] start = new int[2];
        int[] stop = new int[2];
        if (matcher.groupCount() != 2)
            throw new CommandException("Unexpected number of groups " + matcher.groupCount() + " in a pattern!");
        final int firstGroup = 1;
        for (int i = firstGroup; i <= matcher.groupCount(); i++) {
            // see if this is a range
            if (Pattern.matches(rangesPat, matcher.group(i))) {
                Matcher tmpMat = rangesComp.matcher(matcher.group(i));
                tmpMat.find();
                start[i-firstGroup] = Integer.parseInt(tmpMat.group(1));
                stop[i-firstGroup] = Integer.parseInt(tmpMat.group(2));
                // swap the two if needed
                if (stop[i-firstGroup] < start[i-firstGroup]) {
                    int tmp = stop[i-firstGroup];
                    stop[i-firstGroup] = start[i-firstGroup];
                    start[i-firstGroup] = tmp;
                }
            } else {
                //not a range
                start[i-firstGroup] = Integer.parseInt(matcher.group(i));
                stop[i-firstGroup] = start[i-firstGroup];
            }

        }

        // set ranges for two for loops
        for(int pic = start[0]; pic <= stop[0]; pic++) {
            for (int port = start[1]; port <= stop[1]; port++) {
                System.out.println("" + pic + "/" + port);
            }
        }

        return ret;
    }


    /**
     * The parameter e is of the form [a-b]/[c-d], ... where a, b, c, d are non-negative integers
     *
     * @param e
     * @return a list of string to which this pseudo-regular expression expands
     */
    protected static List<String> parseInterfaceList(String e) throws CommandException {

        if ((e == null) || (e.length() == 0))
            return new ArrayList<String>();

        List<String> ret = new ArrayList<String>();

        // split along commas
        // then in a loop generate new array elements
        String[] intGroups = e.split(",");

        for(String s:intGroups) {
            // see if it matches the expected pattern for interfaces
            if (Pattern.matches(singlePortPat, s))
                ret.add(s.trim());
            else if (Pattern.matches(rangePortPat, s))
                ret.addAll(expandRange(s));
            else
                throw new CommandException("Interface name " + s + " does not match any available patterns for SAOS");
        }

        return ret;
    }

    public void createVLAN(String vlanTag, String qosRate, String qosBurstSize) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyVirtualSwitch, genVirtualSwitchName(vlanTag));
        if ((qosRate != null) && (qosRate.length() > 0) && (Integer.parseInt(qosRate) > 0)) {
            logger.debug("Ciena 8700 does not support vlan QoS => creating non-QoS vlan");
        }
        executeScript(CommandCreateVLAN, p);
        
        disconnect();
    }

    public void deleteVLAN(String vlanTag, boolean withQoS) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyVirtualSwitch, genVirtualSwitchName(vlanTag));
        if (withQoS) {
            logger.debug("Ciena 8700 does not support vlan QoS => deleting non-QoS vlan");
        }
        executeScript(CommandDeleteVLAN, p);
        
        disconnect();
    }

    public void addTrunkPortsToVLAN(String vlanTag, String ports) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyVLANTagNm, vlanTag);
        String virtualSwitch = genVirtualSwitchName(vlanTag);
        p.setProperty(PropertyVirtualSwitch, virtualSwitch);
        executeScript(CommandLogon, p);
        for (String s:parseInterfaceList(ports)) {
            p.setProperty(PropertySubPort, genSubPortName(virtualSwitch, s, true));
            p.setProperty(PropertyTrunkPorts, s);
            executeScript(CommandAddTrunkPort, p);
        }
        executeScript(CommandLogoff, p);
        
        disconnect();
    }

    public void addAccessPortsToVLAN(String vlanTag, String ports) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyVLANTagNm, vlanTag);
        String virtualSwitch = genVirtualSwitchName(vlanTag);
        p.setProperty(PropertyVirtualSwitch, virtualSwitch);
        executeScript(CommandLogon, p);
        for (String s:parseInterfaceList(ports)) {
            p.setProperty(PropertySubPort, genSubPortName(virtualSwitch, s, false));
            p.setProperty(PropertyAccessPorts, s);
            executeScript(CommandAddAccessPort, p);
        }
        executeScript(CommandLogoff, p);
        
        disconnect();
    }

    public void removeTrunkPortsFromVLAN(String vlanTag, String ports) throws CommandException {
        Properties p = getProperties();
        String virtualSwitch = genVirtualSwitchName(vlanTag);
        p.setProperty(PropertyVLANTagNm, vlanTag);
        p.setProperty(PropertyVirtualSwitch, virtualSwitch);
        for (String s:parseInterfaceList(ports)) {
            p.setProperty(PropertyTrunkPorts, s);
            p.setProperty(PropertySubPort, genSubPortName(virtualSwitch, s, true));
            executeScript(CommandRemoveTrunkPorts, p);
        }
        
        disconnect();
    }

    public void removeAccessPortsFromVLAN(String vlanTag, String ports) throws CommandException {
        Properties p = getProperties();
        String virtualSwitch = genVirtualSwitchName(vlanTag);
        p.setProperty(PropertyVLANTagNm, vlanTag);
        p.setProperty(PropertyAccessPorts, ports);
        p.setProperty(PropertyVirtualSwitch, virtualSwitch);
        for (String s:parseInterfaceList(ports)) {
            p.setProperty(PropertyAccessPorts, s);
            p.setProperty(PropertySubPort, genSubPortName(virtualSwitch, s, false));
            executeScript(CommandRemoveAccessPorts, p);
        }
        
        disconnect();
    }

    public void mapVLANs(String sourceTag, String destinationTag, String port) throws CommandException {
        Properties p = getProperties();
        String virtualSwitch = genVirtualSwitchName(destinationTag);
        p.setProperty(PropertySrcVLAN, sourceTag);
        p.setProperty(PropertyDstVLAN, destinationTag);
        p.setProperty(PropertySubPort, genSubPortName(virtualSwitch, port, true));
        executeScript(CommandMapVLANS, p);
        
        disconnect();
    }

    public void unmapVLANs(String sourceTag, String destinationTag, String port) throws CommandException {
        Properties p = getProperties();
        String virtualSwitch = genVirtualSwitchName(destinationTag);
        p.setProperty(PropertySrcVLAN, sourceTag);
        p.setProperty(PropertyDstVLAN, destinationTag);
        p.setProperty(PropertySubPort, genSubPortName(virtualSwitch, port, true));
        p.setProperty(PropertyPort, port);
        executeScript(CommandUnmapVLANS, p);
        
        disconnect();
    }

}
