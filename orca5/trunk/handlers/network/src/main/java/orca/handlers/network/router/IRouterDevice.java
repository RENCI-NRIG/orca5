package orca.handlers.network.router;

import orca.handlers.network.core.CommandException;
import orca.handlers.network.core.INetworkDevice;

/**
 * 
 * @author ibaldin
 *
 * simple layer 2 device capable of dealing with VLANs
 */
public interface IRouterDevice extends INetworkDevice {
	
	// create VLAN with QoS
    public void createVLAN(String vlanTag, String qosRate, String qosBurstSize) throws CommandException;

    // delete VLAN
    public void deleteVLAN(String vlanTag, boolean withQoS) throws CommandException;
    
    // add trunk ports to a VLAN
    public void addTrunkPortsToVLAN(String vlanTag, String ports) throws CommandException;
    
    // add access ports to a VLAN
    public void addAccessPortsToVLAN(String vlanTag, String ports) throws CommandException;

    // remove trunk ports from a VLAN
    public void removeTrunkPortsFromVLAN(String vlanTag, String ports) throws CommandException;
    
    // remove access ports from a VLAN
    public void removeAccessPortsFromVLAN(String vlanTag, String ports) throws CommandException;
}
