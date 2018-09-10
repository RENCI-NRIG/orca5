package net.exogeni.orca.handlers.network.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.NetconfDevice;

public class IBMRouterDevice extends NetconfDevice implements IRouterDevice, RouterConstants {
    protected static final String CreateVLANScript = "CreateVLAN.txt";
    protected static final String CreateQoSVLANScript = "CreateQoSVLAN.txt";
    protected static final String DeleteVLANScript = "DeleteVLAN.txt";
    protected static final String DeleteQoSVLANScript = "DeleteQoSVLAN.txt";
    protected static final String AddAccessPortStub = "AddAccessPort.txt";
    protected static final String AddTrunkPortStub = "AddTrunkPort.txt";
    protected static final String RemoveAccessPortStub = "RemoveAccessPort.txt";
    protected static final String RemoveTrunkPortStub = "RemoveTrunkPort.txt";

    protected static final Pattern RangePat = Pattern.compile("^\\s*(\\d+)-(\\d+)\\s*$");
    protected static final Pattern VmapPat = Pattern.compile("^access-control\\s+vmap\\s+(\\d+).*$");
    protected static final Pattern EndVlanPat = Pattern.compile("^[a-zA-Z]+\\s+.*$");
    protected static final String VlanTemplate = "^vlan\\s+%\\s*$";
    protected static final Pattern VmapInVlanPat = Pattern.compile("^\\s+vmap\\s+(\\d+).*$");

    public IBMRouterDevice(String deviceAddr, String uid, String pass, String base) throws CommandException {
        super(deviceAddr, uid, pass);
        basepath = base;
    }

    @Override
    public void createVLAN(String vlanTag, String qosRate, String qosBurstSize) throws CommandException {

        Properties props = getProperties();
        String cmd;
        try {
            if ((qosRate != null) && (qosRate.length() != 0) && (Long.parseLong(qosRate) > 0)) {
                if ((qosBurstSize == null) || (qosBurstSize.length() == 0) || (Long.parseLong(qosBurstSize) <= 0))
                    throw new CommandException(
                            "Burst size " + qosBurstSize + " is null, negative or empty in createVLAN");

                connect();

                // find available vmap index
                Integer vmap = findFreeVmap();
                props.setProperty(PropertyVMAP, "" + vmap);

                props.setProperty(PropertyQoSRateNm, qosRate);
                props.setProperty(PropertyQoSBurstSizeNm, qosBurstSize);
                props.setProperty(PropertyQoSPolicyNm, PropertyPolicyNamePrefix + vlanTag);
                cmd = loadScriptString(CreateQoSVLANScript);
            } else
                cmd = loadScriptString(CreateVLANScript);

            props.setProperty(PropertyVLANTagNm, vlanTag);
            props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

            String commandBuffer = replaceVars(cmd, props);

            logger.debug("IBM Command buffer content: " + commandBuffer);

            if (!isConnected()) {
                connect();
            }

            genericUpdateCommandNoConnect("createVLAN", commandBuffer);
        } catch (NumberFormatException e) {
            throw new CommandException("Bitrate and burst size must be numeric: " + qosRate + "/" + qosBurstSize);
        } finally {
            if (isConnected()) {
                disconnect();
            }
        }

    }

    @Override
    public void deleteVLAN(String vlanTag, boolean withQoS) throws CommandException {

        String cmd;
        Properties props = getProperties();
        if (withQoS) {
            connect();
            Integer vmap = findVlanVmap(vlanTag);
            if (vmap == null)
                // could not find vmap, at least delete the vlan
                cmd = loadScriptString(DeleteVLANScript);
            else {
                cmd = loadScriptString(DeleteQoSVLANScript);
                props.setProperty(PropertyVMAP, "" + vmap);
            }
        } else
            cmd = loadScriptString(DeleteVLANScript);

        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);
        props.setProperty(PropertyQoSPolicyNm, PropertyPolicyNamePrefix + vlanTag);

        // perform parameter substitution
        String commandBuffer = replaceVars(cmd, props);

        logger.debug("IBM Command buffer content: " + commandBuffer);

        if (!isConnected())
            connect();
        genericUpdateCommandNoConnect("deleteVLAN", commandBuffer);
        disconnect();
    }

    @Override
    public void addTrunkPortsToVLAN(String vlanTag, String ports) throws CommandException {
        String commandBuffer = "";
        String cmd = loadScriptString(AddTrunkPortStub);
        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyTrunkPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("IBM Command buffer content: " + commandBuffer);
        genericUpdateCommand("addTrunkPortsToVLAN", commandBuffer);
    }

    @Override
    public void addAccessPortsToVLAN(String vlanTag, String ports) throws CommandException {
        String commandBuffer = "";
        String cmd = loadScriptString(AddAccessPortStub);
        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyAccessPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("IBM Command buffer content: " + commandBuffer);
        genericUpdateCommand("addAccessPortsToVLAN", commandBuffer);
    }

    @Override
    public void removeTrunkPortsFromVLAN(String vlanTag, String ports) throws CommandException {
        String commandBuffer = "";
        String cmd = loadScriptString(RemoveTrunkPortStub);

        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyTrunkPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("IBM Command buffer content: " + commandBuffer);

        genericUpdateCommand("removeTrunkPortsFromVLAN", commandBuffer);
    }

    @Override
    public void removeAccessPortsFromVLAN(String vlanTag, String ports) throws CommandException {
        String commandBuffer = "";
        String cmd = loadScriptString(RemoveAccessPortStub);

        Properties props = getProperties();
        props.setProperty(PropertyVLANTagNm, vlanTag);
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);

        List<String> interfaces = expandInterfacePattern(ports);
        for (String iface : interfaces) {
            props.setProperty(PropertyAccessPorts, iface);
            commandBuffer += replaceVars(cmd, props) + " ";
        }
        logger.debug("IBM Command buffer content: " + commandBuffer);

        genericUpdateCommand("removeAccessPortsFromVLAN", commandBuffer);
    }

    /**
     * Find an available VMAP index in the device (assuming 1-128)
     * 
     * @return Integer in case of error
     * @throws CommandException in case of error
     */
    protected Integer findFreeVmap() throws CommandException {

        if (isEmulationEnabled())
            return 1;

        BufferedReader br = getDeviceConfigurationReader();

        String line = null;
        BitSet seenVmaps = new BitSet();
        try {
            while ((line = br.readLine()) != null) {
                Matcher m = VmapPat.matcher(line);
                if (m.matches()) {
                    seenVmaps.set(Integer.parseInt(m.group(1)));
                }
            }
            Integer ret = seenVmaps.nextClearBit(1);
            if (ret > 128)
                throw new CommandException("No available VMAP indices on device " + device.gethostName());
            return ret;
        } catch (IOException ioe) {
            throw new CommandException(
                    "Unable to find free VMAP in device " + device.gethostName() + " due to IO error: " + ioe);
        }
    }

    /**
     * Find the vmap for this vlan tag or return null if nothing found
     * 
     * @param vlan vlan
     * @return integer
     * @throws CommandException in case of error
     */
    protected Integer findVlanVmap(String vlan) throws CommandException {
        if (isEmulationEnabled())
            return 1;

        BufferedReader br = getDeviceConfigurationReader();

        String line = null;
        int level = 0;
        try {
            while ((line = br.readLine()) != null) {
                switch (level) {
                case 0: {
                    Pattern filledPattern = Pattern.compile(VlanTemplate.replaceAll("%", vlan));
                    Matcher m1 = filledPattern.matcher(line);
                    if (m1.matches()) {
                        level = 1;
                    }
                }
                    break;
                case 1: {
                    Matcher m1 = VmapInVlanPat.matcher(line);
                    Matcher m2 = EndVlanPat.matcher(line);
                    if (m1.matches()) {
                        return Integer.parseInt(m1.group(1));
                    }
                    if (m2.matches()) {
                        level = 0;
                        continue;
                    }
                }
                    break;
                }
            }
        } catch (IOException ioe) {
            throw new CommandException("Unable to find VMAP associated with vlan " + vlan + " due to error: " + ioe);
        } catch (Exception e) {
            throw new CommandException("Unable to find VMAP associated with vlan " + vlan + " due to error: " + e);
        }
        return null;
    }

    protected void genericUpdateCommand(String commandName, String commandBuffer) throws CommandException {
        // remove CRs from the commandBuffer
        // commandBuffer = commandBuffer.replaceAll("\\s*\\n+\\s*", " ");
        // commandBuffer = commandBuffer.replaceAll("\\s+", " ");
        if (!isEmulationEnabled()) {
            // connect to device and update configuration
            connect();
            executeCommand(commandBuffer);
            disconnect();
        } else {
            System.out.println(commandName
                    + " command buffer: <rpc> <load-configuration action=\"merge\" format=\"text\"> <configuration-text> "
                    + commandBuffer + " </configuration-text></load-configuration></rpc>");
        }
    }

    /**
     * FOr cases when we want to do connect/disconnect outside the function
     * 
     * @param commandName commandName
     * @param commandBuffer commandBuffer
     * @throws CommandException in case of error
     */
    protected void genericUpdateCommandNoConnect(String commandName, String commandBuffer) throws CommandException {
        if (!isEmulationEnabled()) {
            executeCommand(commandBuffer);
        } else {
            System.out.println(commandName
                    + " command buffer: <rpc> <load-configuration action=\"merge\" format=\"text\"> <configuration-text> "
                    + commandBuffer + " </configuration-text></load-configuration></rpc>");
        }
    }

    /**
     * Expand pattern x-z into a list [x, y, z]
     * 
     * @param s
     * @return
     * @throws CommandException
     */
    private static List<String> expandInterface(String s) throws CommandException {
        Matcher m1 = RangePat.matcher(s);
        m1.matches();

        String lower = m1.group(1), higher = m1.group(2);
        List<String> ret = new ArrayList<String>();
        try {
            Integer lowerInt = Integer.parseInt(lower);
            Integer higherInt = Integer.parseInt(higher);
            if (lowerInt > higherInt) {
                Integer tmp = higherInt;
                higherInt = lowerInt;
                lowerInt = tmp;
            }
            for (int i = lowerInt; i <= higherInt; i++) {
                ret.add("" + i);
            }
        } catch (NumberFormatException nfe) {
            throw new CommandException("Interface " + s + " does not match any available patterns for IBM");
        }
        return ret;
    }

    /**
     * Expand a list of ports in the form of a-b,c,d,e-f into a list of individual names
     * 
     * @param e e 
     * @return list of ports
     * @throws CommandException in case of error
     */
    protected static List<String> expandInterfacePattern(String e) throws CommandException {

        if ((e == null) || (e.length() == 0))
            return new ArrayList<String>();

        List<String> ret = new ArrayList<String>();

        // split along commas
        // then in a loop generate new array elements
        String[] intGroups = e.split(",");

        for (String s : intGroups) {
            s = s.trim();
            // see if it matches the expected pattern for interfaces
            Matcher m1 = RangePat.matcher(s);
            if (m1.matches())
                ret.addAll(expandInterface(s));
            else {
                try {
                    Integer.parseInt(s);
                    ret.add(s);
                } catch (NumberFormatException nfe) {
                    throw new CommandException("Interface " + s + " does not match any available patterns for IBM");
                }
            }
        }
        return ret;
    }

    public static void main(String[] argv) {
        String v = "1, 4-6, 10-8, 14, 20";

        try {
            List<String> ret = expandInterfacePattern(v);
            for (String r : ret) {
                System.out.println(r);
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }

    }

}
