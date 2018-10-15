package net.exogeni.orca.handlers.nlr.tasks;

import org.apache.tools.ant.BuildException;

/* try to always re-reserve the unprovisioned vlan
 */
public class SmartRemoveVlan extends RemoveVlan {

    @Override
    public void execute() throws BuildException {
        initializeApi();
        try {
            lockSherpa();

            // remove vlan
            realExecute();

            // try to re-reserve it
            if (!sapi.add_reservation(vlanToRemove, SherpaDescKeyword)) {
                logger.error("Unable to reserve vlan tag " + vlanToRemove);
                setResult(-1);
                throw new BuildException("Unable to re-reserve vlan tag after release" + vlanToRemove);
            }
        } catch (Exception e) {
            logger.error("Error in smart removing vlan " + vlanToRemove + ": " + e);
            throw new BuildException("Error in smart removing vlan " + vlanToRemove + ": " + e);
        } finally {
            unlockSherpa();
        }
    }

}
