package orca.ndl.elements;

import java.util.Iterator;
import java.util.LinkedList;

import orca.ndl.DomainResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public final class NetworkConnection extends NetworkElement {

    @Persistent // depends on network element
    private NetworkElement ne1, ne2; // two end point

    @Persistent // depends on network element
    private LinkedList<NetworkElement> connection = new LinkedList<NetworkElement>(); // link/devices in the path

    @Persistent // recursive
    private LinkedList<NetworkConnection> clientConnections = new LinkedList<NetworkConnection>(); // client connections
                                                                                                   // adapted

    @Persistent
    private String connectionType; // crossconnect; link; subnetwork; network;

    @Persistent
    private String openflowCapable;

    @Persistent
    protected Long bandwidth = 0L;

    @Persistent
    protected Long latency = 0L;

    @Persistent
    private Float label_ID = 0.0F;

    public NetworkConnection() {
        super();
    }

    public NetworkConnection(OntModel m, OntResource rs) {
        super(m, rs);
    }

    public NetworkConnection(OntModel m, Resource rs) {
        super(m, rs);
    }

    public NetworkConnection(OntModel model, String url, String name) {
        super(model, url, name);
    }

    public NetworkConnection copy(String name) {
        NetworkConnection b_nc = new NetworkConnection();
        b_nc.setName(name);
        b_nc.setBandwidth(bandwidth);
        b_nc.setOpenflowCapable(openflowCapable);
        b_nc.setAtLayer(atLayer);
        b_nc.setLabel_ID(label_ID);

        if (this.resourceType == null)
            this.resourceType = new DomainResourceType(DomainResourceType.VLAN_RESOURCE_TYPE, 1);
        b_nc.setResourceType(this.resourceType);

        return b_nc;
    }

    public String getOpenflowCapable() {
        return openflowCapable;
    }

    public void setOpenflowCapable(String openflowCapable) {
        this.openflowCapable = openflowCapable;
    }

    public float getLabel_ID() {
        return label_ID;
    }

    public void setLabel_ID(float label_ID) {
        this.label_ID = label_ID;
    }

    public NetworkElement getNe1() {
        return ne1;
    }

    public void setNe1(NetworkElement ne1) {
        this.ne1 = ne1;
    }

    public NetworkElement getNe2() {
        return ne2;
    }

    public void setNe2(NetworkElement ne2) {
        this.ne2 = ne2;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public int compareTo(Object o) {
        int compare = 0;
        int rc1 = 0, rc2 = 0;

        if (o == null)
            return 1;

        NetworkConnection ne = (NetworkConnection) o;

        if (this.bandwidth < ne.getBandwidth())
            compare = 1;
        else if (this.bandwidth > ne.getBandwidth())
            compare = -1;
        else if (this.bandwidth == ne.getBandwidth()) {
            rc1 = this.getNe1().getNumUnits() + this.getNe1().getNumUnits();
            rc2 = this.getNe2().getNumUnits() + this.getNe2().getNumUnits();

            if (rc1 <= rc2)
                compare = 1;
            else
                compare = -1;
        }
        return compare;
    }

    @Override
    public void print() {
        NetworkElement ne;
        System.out.println(name + ":" + uri + ":" + uri);
        if (ne1 != null)
            ne1.print();
        if (ne2 != null)
            ne2.print();
        if (connection != null) {
            Iterator<NetworkElement> it = connection.iterator();
            while (it.hasNext()) {
                ne = (NetworkElement) it.next();
                ne.print();
            }
        }
    }

    @Override
    public void print(Logger logger) {
        NetworkElement ne;
        logger.info(connectionType + ":" + ne1.getName() + ":" + ne1.getNumUnits() + "-" + ne2.getName() + ":"
                + ne2.getNumUnits() + ":" + bandwidth);
        Iterator<NetworkElement> it = connection.iterator();
        while (it.hasNext()) {
            ne = (NetworkElement) it.next();
            ne.print(logger);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());

        // sb.append(connectionType+":"+ne1.getName()+":"+ne1.getNumUnits()+"-"+ne2.getName()+":"+ne2.getNumUnits()+":"+bandwidth
        // + "\n");
        sb.append("NE1: \n" + ne1 + "\n");
        sb.append("NE2: \n" + ne2 + "\n");
        sb.append("Attributes: " + connectionType + "/" + bandwidth + "\n");
        sb.append("NetworkElements:\n");
        if (connection != null) {
            int j = 0;
            Iterator<NetworkElement> it = connection.iterator();
            while (it.hasNext()) {
                sb.append("NetworkElement " + j++ + ":\n");
                NetworkElement ne = (NetworkElement) it.next();
                sb.append(ne);
            }
        }

        sb.append("Client connections:\n");
        if (clientConnections != null) {
            Iterator<NetworkConnection> cit = clientConnections.iterator();
            int i = 0;
            while (cit.hasNext()) {
                NetworkConnection nc = (NetworkConnection) cit.next();
                sb.append("Connection " + i++ + ":\n");
                sb.append(nc);
            }
        }
        return sb.toString();
    }

    public LinkedList<NetworkConnection> getClientConnections() {
        return clientConnections;
    }

    public NetworkElement getFirstClientConnectionsElement() {
        return clientConnections.getFirst();
    }

    public void setClientConnections(LinkedList<NetworkConnection> clientConnections) {
        this.clientConnections = clientConnections;
    }

    public void setClientConnection(NetworkConnection clientConnection) {
        this.clientConnections.add(clientConnection);
    }

    public LinkedList<? extends NetworkElement> getConnection() {
        return connection;
    }

    public void setConnection(LinkedList<NetworkElement> connection) {
        this.connection = connection;
    }

    public NetworkElement getFirstConnectionElement() {
        if (connection.isEmpty())
            return null;
        return connection.getFirst();
    }

    public void addConnection(NetworkElement element) {
        this.connection.add(element);
    }

    public boolean hasConnection(NetworkElement element) {
        for (NetworkElement ne : connection) {
            if (ne == element)
                return true;
        }

        return false;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

}
