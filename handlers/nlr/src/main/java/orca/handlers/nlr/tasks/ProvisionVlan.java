package orca.handlers.nlr.tasks;

import java.util.Iterator;
import java.util.List;

import orca.handlers.nlr.SherpaAPIResponse.EntityDefinition;
import orca.handlers.nlr.SherpaAPIResponse.SPDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanStatusDefinition;

import org.apache.tools.ant.BuildException;

public class ProvisionVlan extends GenericSherpaTask {
    protected int vlanToReserve = 0;
    protected String nodeA, intA, nodeZ, intZ;
    protected long bw = 0;
    protected String vlanStatusProperty = null;
    protected String reservedVlanProperty = null; // to hold the vlan tag
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

    /**
     * provision a path for a given tag (assumes the tag already reserved)
     * 
     * @throws BuildException
     */
    protected final void realExecute() throws BuildException {
        try {
            // get shortest path and entities
            List<SPDefinition> sp = sapi.get_shortest_path(nodeA, nodeZ, bw);
            List<EntityDefinition> entities = sapi.get_entities();

            logger.debug("Sherpa shortest path:");
            for (SPDefinition spd : sp) {
                logger.debug(spd.circuit_id);
            }

            if (!sapi.provision_vlan(nodeA, intA, nodeZ, intZ, sp, vlanToReserve, bw, SherpaDescKeyword,
                    SherpaRequestId, entities.get(0).entity_id)) {
                logger.error("Unable to provision vlan tag " + vlanToReserve);
                throw new BuildException("Unable to provision vlan tag " + vlanToReserve);
            }

            // get status of the vlan
            List<VlanStatusDefinition> statusList = sapi.get_status(vlanToReserve, SherpaRequestId);

            Iterator<VlanStatusDefinition> it = statusList.listIterator();
            String statusString = "";
            while (it.hasNext()) {
                VlanStatusDefinition vsd = it.next();
                statusString += "[" + vsd.date + " " + vsd.state + "]";
            }
            getProject().setProperty(vlanStatusProperty, statusString);

            // Save tag and reservation info
            String vlanAsString = Integer.toString(vlanToReserve);
            getProject().setProperty(getReservedVlanProperty(), vlanAsString);

            getProject().setProperty(getReservationIdProperty(), vlanAsString + "|" + nodeA.trim() + ":" + intA.trim()
                    + "|" + vlanAsString + "|" + nodeZ.trim() + ":" + intZ.trim() + "|" + vlanAsString);
            setResult(0);
        } catch (Exception e) {
            logger.error("Error in provisioning vlan " + vlanToReserve + ": " + e);
            throw new BuildException("Error in provisioning vlan " + vlanToReserve + ": " + e);
        }
    }

    @Override
    public void execute() throws BuildException {
        super.execute();

        realExecute();
    }

    /**
     * set vlan id to reserve
     * 
     * @param vlan
     */
    public void setVlanId(String vlan) {
        if (vlan.length() > 0)
            this.vlanToReserve = Integer.valueOf(vlan);
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

    public void setBandwidth(String b) {
        if (b.length() > 0)
            this.bw = Long.valueOf(b);
    }

    public void setVlanStatusProperty(String s) {
        this.vlanStatusProperty = s;
    }

    public String getVlanStatusProperty() {
        return this.vlanStatusProperty;
    }
}
