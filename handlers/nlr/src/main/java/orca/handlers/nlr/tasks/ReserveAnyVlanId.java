package orca.handlers.nlr.tasks;

import orca.handlers.nlr.SherpaAPIResponse.VlanIdDefinition;

import org.apache.tools.ant.BuildException;

/**
 * This task is meant primarily for testing. It reserves the first
 * available vlan and prints out the id.
 * @author ibaldin
 *
 */
public class ReserveAnyVlanId extends GenericSherpaTask {
	
	protected String reservedVlanProperty;
	
	public void setReservedVlanProperty(String name) {
		this.reservedVlanProperty = name;
	}
	
	public String getReservedVlanProperty() {
		return this.reservedVlanProperty;
	}
	
	@Override
	public void execute() throws BuildException {
        super.execute();
        
        try {
        	// get available vlan
        	VlanIdDefinition vlan_id = sapi.get_available_vlan_id();
        	
        	// reserve it
        	if (!sapi.add_reservation(vlan_id.vlan_id, SherpaDescKeyword)) {
        		logger.error("Unable to reserve vlan tag " + vlan_id.vlan_id);
        		setResult(-1);
        		throw new BuildException("Unable to reserve vlan tag " + vlan_id.vlan_id);
        	}
        	getProject().setProperty(getReservedVlanProperty(), Integer.toString(vlan_id.vlan_id));
        	setResult(0);
        } catch (Exception e) {
        	logger.error("Error in reserving vlan tag: " + e);
        	getProject().setProperty(getReservedVlanProperty(), "0");
        	throw new BuildException("Error in reserving vlan tag: " + e);
        }
	}
}
