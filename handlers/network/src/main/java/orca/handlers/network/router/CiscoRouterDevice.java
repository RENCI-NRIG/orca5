package orca.handlers.network.router;

import java.util.Properties;

import orca.handlers.network.core.CommandException;

public abstract class CiscoRouterDevice extends RouterSSHDevice {
    /**
     * Administrative password.
     */
    protected String adminPassword, defaultPrompt;

    public CiscoRouterDevice(String deviceAddress, String uid, String password, String adminPassword, String defaultPrompt) {
        super(deviceAddress, uid, password);
        this.adminPassword = adminPassword;
        this.defaultPrompt = defaultPrompt;
    }

    public void createVLAN(String vlanTag, String qosRate, String qosBurstSize) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyVLANTagNm, vlanTag);
        p.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + vlanTag);
        if ((qosRate != null) && (qosRate.length() > 0) && (Integer.parseInt(qosRate) > 0)) {
        	p.setProperty(PropertyQoSPolicyNm, PropertyPolicyNamePrefix + vlanTag);
        	p.setProperty(PropertyQoSRateNm, qosRate);
            p.setProperty(PropertyQoSBurstSizeNm, qosBurstSize);
        	executeScript(CommandCreateQoSVLAN, p);
        } else 
        	executeScript(CommandCreateVLAN, p);
    }

    public void deleteVLAN(String vlanTag, boolean withQoS) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyVLANTagNm, vlanTag);
        if (withQoS) {
        	p.setProperty(PropertyQoSPolicyNm, PropertyPolicyNamePrefix + vlanTag);
        	executeScript(CommandDeleteQoSVLAN, p);
        }
        else 
        	executeScript(CommandDeleteVLAN, p);
    }

    // add trunk ports to a VLAN
    public void addTrunkPortsToVLAN(String vlanTag, String ports) throws CommandException {
    	Properties p = getProperties();
    	p.setProperty(PropertyVLANTagNm, vlanTag);
    	p.setProperty(PropertyTrunkPorts, ports);
    	executeScript(CommandAddTrunkPorts, p);
    }
    
    // add access ports to a VLAN
    public void addAccessPortsToVLAN(String vlanTag, String ports) throws CommandException {
    	Properties p = getProperties();
    	p.setProperty(PropertyVLANTagNm, vlanTag);
    	p.setProperty(PropertyAccessPorts, ports);
    	executeScript(CommandAddAccessPorts, p);
    }

    // remove trunk ports from a VLAN
    public void removeTrunkPortsFromVLAN(String vlanTag, String ports) throws CommandException {
    	Properties p = getProperties();
    	p.setProperty(PropertyVLANTagNm, vlanTag);
    	p.setProperty(PropertyTrunkPorts, ports);
    	executeScript(CommandRemoveTrunkPorts, p);
    }
    
    // remove access ports from a VLAN
    public void removeAccessPortsFromVLAN(String vlanTag, String ports) throws CommandException {
    	Properties p = getProperties();
    	p.setProperty(PropertyVLANTagNm, vlanTag);
    	p.setProperty(PropertyAccessPorts, ports);
    	executeScript(CommandRemoveAccessPorts, p);
    }
    
    @Override
    protected Properties getProperties() {
        Properties p = super.getProperties();
        p.setProperty(PropertyDeviceAdminPWD, adminPassword);
        p.setProperty(PropertyDefaultPrompt, defaultPrompt);
        return p;
    }

}
