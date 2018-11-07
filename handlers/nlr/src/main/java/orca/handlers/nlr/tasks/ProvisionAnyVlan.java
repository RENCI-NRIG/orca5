package orca.handlers.nlr.tasks;

import java.util.Iterator;
import java.util.List;

import orca.handlers.nlr.SherpaAPIResponse.EntityDefinition;
import orca.handlers.nlr.SherpaAPIResponse.SPDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanIdDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanStatusDefinition;

import org.apache.tools.ant.BuildException;

public class ProvisionAnyVlan extends GenericSherpaTask {
    protected String nodeA, intA, nodeZ, intZ;
    protected long bw;
    protected String vlanStatusProperty;

    protected String reservedVlanProperty; // to hold the vlan tag
    protected String reservationIdProperty; // to hold the reservation id

    public void setReservedVlanProperty(String name) {
        this.reservedVlanProperty = name;
    }

    public String getReservedVlanProperty() {
        return this.reservedVlanProperty;
    }

    public void setReservationIdProperty(String name) {
        this.reservationIdProperty = name;
    }

    public String getReservationIdProperty() {
        return this.reservationIdProperty;
    }

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {
            Long bb = new Long(this.bw);
            // get available vlan
            logger.info("Provisioning vlan");

            VlanIdDefinition vlan_id = sapi.get_available_vlan_id();

            // reserve it
            if (!sapi.add_reservation(vlan_id.vlan_id, SherpaDescKeyword)) {
                logger.error("Unable to reserve vlan tag " + vlan_id.vlan_id);
                setResult(-1);
                throw new BuildException("Unable to reserve vlan tag " + vlan_id.vlan_id);
            }
            // save the results
            String vlanAsString = Integer.toString(vlan_id.vlan_id);
            getProject().setProperty(getReservedVlanProperty(), vlanAsString);

            getProject().setProperty(getReservationIdProperty(), vlanAsString + "|" + nodeA.trim() + ":" + intA.trim()
                    + "|" + vlanAsString + "|" + nodeZ.trim() + ":" + intZ.trim() + "|" + vlanAsString);

            // get shortest path and entities
            List<SPDefinition> sp = sapi.get_shortest_path(nodeA, nodeZ, bw);
            List<EntityDefinition> entities = sapi.get_entities();

            if (!sapi.provision_vlan(nodeA, intA, nodeZ, intZ, sp, vlan_id.vlan_id, bw, SherpaDescKeyword,
                    SherpaRequestId, entities.get(0).entity_id)) {
                logger.error("Unable to provision vlan tag " + vlan_id);
                throw new BuildException("Unable to provision vlan tag " + vlan_id);
            }

            // get status of the vlan
            List<VlanStatusDefinition> statusList = sapi.get_status(vlan_id.vlan_id, SherpaRequestId);

            Iterator<VlanStatusDefinition> it = statusList.listIterator();
            String statusString = "";
            while (it.hasNext()) {
                VlanStatusDefinition vsd = it.next();
                statusString += "[" + vsd.date + " " + vsd.state + "]";
            }
            getProject().setProperty(vlanStatusProperty, statusString);
            setResult(0);
        } catch (Exception e) {
            logger.error("Error in provisioning any vlan: " + e, e);
            throw new BuildException("Error in provisioning any vlan: " + e);
        }
    }

    /**
     * set source node:interface
     * 
     * @param na
     */
    public void setEndpointA(String na) {
        if (na.split(":").length != 2)
            throw new BuildException("Error in endpoint A specification");
        this.nodeA = na.split(":")[0];
        this.intA = na.split(":")[1];
    }

    /**
     * set destination node:interface
     * 
     * @param nz
     */
    public void setEndpointZ(String nz) {
        if (nz.split(":").length != 2)
            throw new BuildException("Error in endpoint A specification");
        this.nodeZ = nz.split(":")[0];
        this.intZ = nz.split(":")[1];
    }

    /**
     * set vlan bandwidth
     * 
     * @param b
     */
    public void setBandwidth(String b) {
        this.bw = Long.valueOf(b);
    }

    /**
     * set vlan status property
     * 
     * @param s
     */
    public void setVlanStatusProperty(String s) {
        this.vlanStatusProperty = s;
    }

    public String getVlanStatusProperty() {
        return this.vlanStatusProperty;
    }
}
