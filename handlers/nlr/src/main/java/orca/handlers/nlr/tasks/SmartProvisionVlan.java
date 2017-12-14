package orca.handlers.nlr.tasks;

import java.util.Iterator;
import java.util.List;

import orca.handlers.nlr.SherpaAPIResponse.EntityDefinition;
import orca.handlers.nlr.SherpaAPIResponse.SPDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanGetReservationDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanIdDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanStatusDefinition;

import org.apache.tools.ant.BuildException;

/**
 * If vlan id is given, check available reservations, if available, then provision it, otherwise fail. If vlan id is not
 * given, check available reservations, if available pick one and provision. If not available, reserve a new one, then
 * provision.
 * 
 * @author ibaldin
 *
 */
public class SmartProvisionVlan extends ProvisionVlan {

    @Override
    public void execute() throws BuildException {
        initializeApi();
        try {

            lockSherpa();

            // get a list of reservations
            List<VlanGetReservationDefinition> reservedVlanTags = sapi.get_all_reservations();

            if (vlanToReserve == 0) {
                // if no VLAN is specified, pick one
                // from reserved list or reserve fresh
                if (reservedVlanTags.size() > 0)
                    vlanToReserve = reservedVlanTags.iterator().next().vlan_id;
                else {
                    VlanIdDefinition vlan_id = sapi.get_available_vlan_id();
                    if (!sapi.add_reservation(vlan_id.vlan_id, SherpaDescKeyword)) {
                        logger.error("Unable to reserve vlan tag " + vlan_id.vlan_id);
                        setResult(-1);
                        throw new BuildException("Unable to reserve vlan tag " + vlan_id.vlan_id);
                    }
                    vlanToReserve = vlan_id.vlan_id;
                }
            } else {
                // check against already reserved
                boolean present = false;
                for (VlanGetReservationDefinition def : reservedVlanTags) {
                    if (def.vlan_id == vlanToReserve) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    // try to reserve it
                    if (!sapi.add_reservation(vlanToReserve, SherpaDescKeyword)) {
                        logger.error("Unable to reserve vlan tag " + vlanToReserve);
                        setResult(-1);
                        throw new BuildException("Unable to reserve vlan tag " + vlanToReserve);
                    }
                }
            }
            // now vlanToReserve should be properly set and available(reserved) so we can provision
            realExecute();

        } catch (Exception e) {
            logger.error("Error in smart provisioning vlan " + vlanToReserve + ": " + e);
            throw new BuildException("Error in smart provisioning vlan " + vlanToReserve + ": " + e);
        } finally {
            unlockSherpa();
        }
    }
}
