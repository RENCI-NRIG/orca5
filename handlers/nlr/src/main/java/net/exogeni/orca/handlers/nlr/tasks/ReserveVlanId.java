/**
 * 
 */
package net.exogeni.orca.handlers.nlr.tasks;

import org.apache.tools.ant.BuildException;

/**
 * Reserve a vlan id for future use
 * 
 * @author ibaldin
 *
 */
public class ReserveVlanId extends GenericSherpaTask {
    protected int vlan_id;

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {
            if (!sapi.add_reservation(vlan_id, SherpaDescKeyword)) {
                logger.error("Unable to reserve vlan tag " + vlan_id);
                setResult(-1);
                throw new BuildException("Unable to reserve vlan tag " + vlan_id);
            }
            setResult(0);
        } catch (Exception e) {
            logger.error("Error in reserving vlan tag " + vlan_id + ": " + e);
            throw new BuildException("Error in reserving vlan tag " + vlan_id + ": " + e);
        }
    }

    /**
     * set vlan id to reserve
     * 
     * @param vlan
     */
    public void setVlanId(String vlan) {
        this.vlan_id = Integer.valueOf(vlan);
    }
}
