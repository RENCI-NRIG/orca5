/*
 * License
 */

package orca.handlers.nlr.tasks;

import org.apache.tools.ant.BuildException;

/**
 * A task that uses a vlan id to un-reserve previous reservation.
 * 
 * @author ibaldin
 *
 */
public class UnreserveVlanId extends GenericSherpaTask {
    protected int vlanToUnReserve;

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {
            if (!sapi.remove_reservation(vlanToUnReserve)) {
                logger.error("Unable to un-reserve vlan tag " + vlanToUnReserve);
                setResult(-1);
                throw new BuildException("Unable to un-reserve vlan tag " + vlanToUnReserve);
            }
        } catch (Exception e) {
            logger.error("Error in un-reserving vlan tag " + vlanToUnReserve + ": " + e);
            throw new BuildException("Error in un-reserving vlan tag " + vlanToUnReserve + ": " + e);
        }
    }

    /**
     * Vlan to remove a reservation on
     * 
     * @param vlan
     */
    public void setVlanId(String vlan) {
        this.vlanToUnReserve = Integer.valueOf(vlan.split("[|]")[0]);
    }
}
