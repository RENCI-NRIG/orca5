package orca.handlers.network.router;

import java.util.Properties;

import orca.handlers.network.core.CommandException;

public class Cisco6509Device extends CiscoRouterDevice implements IMappingRouterDevice {

    public Cisco6509Device(String deviceAddress, String uid, String password, String adminPassword, 
    		String defaultPrompt) {
        super(deviceAddress, uid, password, adminPassword, defaultPrompt);
        basepath = "/orca/handlers/network/router/cisco/6509";
    }
    
    public void mapVLANs(String sourceTag, String destinationTag, String port) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertySrcVLAN, sourceTag);
        p.setProperty(PropertyDstVLAN, destinationTag);
        p.setProperty(PropertyPort, port);
        executeScript(CommandMapVLANS, p);
    }

    public void unmapVLANs(String sourceTag, String destinationTag, String port) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertySrcVLAN, sourceTag);
        p.setProperty(PropertyDstVLAN, destinationTag);
        p.setProperty(PropertyPort, port);
        executeScript(CommandUnmapVLANS, p);
    }

}
