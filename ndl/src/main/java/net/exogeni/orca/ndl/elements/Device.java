package net.exogeni.orca.ndl.elements;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;

//import net.exogeni.orca.network.LayerConstant.Action;
import net.exogeni.orca.ndl.DomainResourceType;
import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Device extends NetworkElement {
    @Persistent
    protected LinkedList<SwitchMatrix> switchingMatrix;
    @Persistent
    protected LinkedList<SwitchingAction> actionList;
    @Persistent
    protected Integer actionCount = 0;
    @Persistent
    protected String direction;

    @Persistent
    protected Integer degree = 0;

    // for abstract domain in interdomain provisioning: dependency tree
    @Persistent
    protected Boolean isLabelProducer;
    @Persistent
    protected Boolean layerLabelPrimary;
    @Persistent
    protected String swappingCapability, tunnelingCapability;

    @Persistent
    protected Boolean depend;

    @Persistent
    protected HashMap<String, LinkedList<LabelSet>> labelSets;

    @Persistent
    protected Float staticLabel;

    @Persistent
    protected String availableLabelSet;

    @Persistent
    protected Boolean isAllocatable;

    @Persistent
    protected String NetUUID;

    // interfaces in neighboring domain; or neighboring devices in the virtual connection.
    // protected OntResource upNeighbour,upLocal;
    // protected OntResource downNeighbour,downLocal;
    @Persistent
    protected String upNeighbourUri, upLocalUri, downNeighbourUri, downLocalUri;

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());
        sb.append("Capabilities: " + isLabelProducer + "/" + swappingCapability + "/" + tunnelingCapability + "\n");
        sb.append("SwitchMatrix: \n");
        if (switchingMatrix != null) {
            for (SwitchMatrix sm : switchingMatrix) {
                sb.append(sm);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public Device(OntModel m, String u, String n) {
        super(m, u, n);
        init();
    }

    public Device(OntModel m, OntResource rs) {
        super(m, rs);
        init();
        setParameters(m, rs);
    }

    public Device(OntModel m, Resource rs) {
        super(m, rs);
        init();
        setParameters(m, getResource());
    }

    public Device() {
        super();
        init();
    }

    private void init() {
        degree = 1;
        switchingMatrix = null;
        actionList = null;
        swappingCapability = null;
        isLabelProducer = false;
        depend = false;
        staticLabel = 0.0f;
        isAllocatable = true;
        layerLabelPrimary = false;
    }

    public void setParameters(OntModel ontModel, OntResource rs_ont) {
        Statement st = null;
        st = rs_ont.getProperty(NdlCommons.topologyManagementIP);
        if (st != null)
            setManagementIP(st.getString());
        st = rs_ont.getProperty(NdlCommons.hostName_p);
        if (st != null)
            setHostName(st.getString());
        st = rs_ont.getProperty(NdlCommons.isLabelProducer);
        if (st != null)
            setLabelProducer(st.getBoolean());
        st = rs_ont.getProperty(NdlCommons.layerLabelIsPrimary);
        if (st != null)
            setLayerLabelPrimary(st.getBoolean());

        st = rs_ont.getProperty(NdlCommons.domainHasServiceProperty);
        if (st != null) {
            Resource networkService = st.getResource();
            st = networkService.getProperty(NdlCommons.hasCastType);
            if (st != null)
                this.castType = st.getResource().getLocalName();
        }

        String atLayer = null;
        SwitchMatrix sm = null;
        String cap = null;
        Resource rs0;
        StmtIterator stit = rs_ont.listProperties(NdlCommons.hasSwitchMatrix);
        while (stit != null && stit.hasNext()) {
            rs0 = stit.nextStatement().getResource();

            sm = new SwitchMatrix(ontModel, rs_ont);
            st = rs0.getProperty(NdlCommons.switchingCapability);
            if (st != null) {
                cap = st.getResource().getLocalName();
                sm.setCapability(cap);
                sm.setAtLayer(cap);
            }
            st = rs0.getProperty(NdlCommons.swappingCapability);
            if (st != null) {
                sm.setSwappingcapability(st.getResource().getLocalName());
                swappingCapability = st.getResource().getLocalName();
            }

            st = rs0.getProperty(NdlCommons.tunnelingCapability);
            if (st != null) {
                sm.setTunnelingcapability(st.getResource().getLocalName());
                tunnelingCapability = st.getResource().getLocalName();
            }

            st = rs0.getProperty(NdlCommons.connectionDirection);
            if (st != null)
                sm.setDirection(st.getResource().getLocalName());

            st = rs0.getProperty(NdlCommons.hasCastType);
            if (st != null) {
                sm.setCastType(st.getResource().getLocalName());
                this.setCastType(st.getResource().getLocalName());
            }

            st = rs0.getProperty(NdlCommons.domainIsAllocatable);
            if (st != null)
                this.setAllocatable(st.getBoolean());

            setSwitchMatrix(sm);
            setDirection(sm.getDirection());
            if (atLayer == null)
                atLayer = cap;
            if ((atLayer != null) && (cap != null)) {
                if (Layer.valueOf(atLayer).rank() > Layer.valueOf(cap).rank())
                    atLayer = cap;
            }
        }
        if (atLayer != null) {
            if (this.resourceType == null) {
                resourceType = new DomainResourceType(Layer.valueOf(atLayer).getLabelP().toString(), 1);
            }
            this.setAtLayer(atLayer);
            setRank(Layer.valueOf(atLayer).rank());
        }
    }

    public int compareTo(Object o) {
        int compare = 0;

        if (o == null)
            return 1;

        Device d = (Device) o;

        DomainResourceType rType = d.getResourceType();
        if ((resourceType == null) || (rType == null))
            compare = 0;

        if (resourceType.getCount() < rType.getCount())
            compare = 1;
        else if (resourceType.getCount() > rType.getCount())
            compare = -1;

        return compare;
    }

    // If uni-interfaces, change the original switching action (psuedo bi-interface) to two separate actions
    public void processUNIInterface(String layer) {
        int actionCount = getActionCount();
        SwitchingAction action = null;
        LinkedList<SwitchingAction> uniList = null;
        for (int j = 0; j < actionCount; j++) {
            action = (SwitchingAction) actionList.get(j);
            if (action.getAtLayer().equals(layer)) {
                uniList = findUNIInterface(action);

                if (uniList != null) {
                    // LinkedList <Interface> intfList=action.getClientInterface();
                    // Iterator <Interface> it=intfList.iterator();
                    // while(it.hasNext()){
                    // Interface intf=it.next();
                    // //intf.getResource().removeAll(inConnection);
                    // }
                    // actionList.remove(j);
                    actionList.add(j, uniList.get(0));
                    actionList.add(j + 1, uniList.get(1));
                    j++;
                }
            }
        }
    }

    // find the 2 corresponding uni interface switching for a BI interface switching action
    public LinkedList<SwitchingAction> findUNIInterface(SwitchingAction action) {
        LinkedList<Interface> interfaceList = action.getClientInterface();
        Interface intf;
        Interface intf1 = null;
        Interface intf2 = null;

        String uriBase = action.getURI();
        uriBase.replaceAll("d$", "");

        SwitchingAction action1 = new SwitchingAction(action.getAtLayer(), action.getStartTime(), action.getEndTime());
        action1.setURI(uriBase + "1");
        SwitchingAction action2 = new SwitchingAction(action.getAtLayer(), action.getStartTime(), action.getEndTime());
        action2.setURI(uriBase + "2");
        boolean valid = true;
        int i = 0;
        for (i = 0; i < interfaceList.size(); i++) {
            intf = (Interface) interfaceList.get(i);
            intf1 = intf.getInput();
            if (intf1 == null)
                valid = false;
            intf2 = intf.getOutput();
            if (i == 0) {
                action1.addClientInterface(intf1);
                action2.addClientInterface(intf2);

            } else {
                action2.addClientInterface(intf1);
                action1.addClientInterface(intf2);
            }
            action1.setDefaultAction(action.getDefaultAction());
            action2.setDefaultAction(action.getDefaultAction());
        }

        if (i > 0)
            action.setDefaultAction(Action.Temporary.toString());
        LinkedList<SwitchingAction> subList = null;
        if (valid) {
            subList = new LinkedList<SwitchingAction>();
            subList.add(action1);
            subList.add(action2);
        }

        return subList;
    }

    public void print(Logger logger) {
        super.print(logger);

        int size = 0;
        int i = 0;
        logger.info("\n" + isLabelProducer + ":" + swappingCapability + ":" + degree);
        SwitchMatrix matrix = null;
        if (switchingMatrix != null) {
            size = switchingMatrix.size();
            logger.debug("Switching Matrix:" + size);
            for (i = 0; i < size; i++) {
                matrix = (SwitchMatrix) switchingMatrix.get(i);
                if (matrix == null)
                    logger.error("No Switching Matrix");
                else
                    matrix.print(logger);
            }
        }

        SwitchingAction action = null;
        if (actionList != null) {
            logger.info("Switching actions:");
            size = actionList.size();
            for (i = 0; i < size; i++) {
                action = (SwitchingAction) actionList.get(i);
                if (action == null)
                    logger.info("No Action");
                else
                    action.print(logger);
            }
        } else {
            logger.error("actionList is not initialized");
        }
    }

    public SwitchingAction getDefaultSwitchingAction() {
        return actionList.getFirst();
    }

    public void addSwitchingAction(SwitchingAction action) {
        if (actionList == null)
            actionList = new LinkedList<SwitchingAction>();
        action.setURI(this.getURI() + "/" + "action" + actionCount);
        actionList.add(action);
        // action.print();
        actionCount++;
    }

    public void removeSwitchingAction(int key) {
        actionList.remove(key);
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree() {
        this.degree++;
    }

    public boolean isLabelProducer() {
        return isLabelProducer;
    }

    public void setLabelProducer(boolean isLabelProducer) {
        this.isLabelProducer = isLabelProducer;
    }

    public Boolean getLayerLabelPrimary() {
        return layerLabelPrimary;
    }

    public void setLayerLabelPrimary(Boolean layerLabelPrimary) {
        this.layerLabelPrimary = layerLabelPrimary;
    }

    public String getSwappingCapability() {
        return swappingCapability;
    }

    public void setSwappingCapability(String swappingCapability) {
        this.swappingCapability = swappingCapability;
    }

    public String getTunnelingCapability() {
        return tunnelingCapability;
    }

    public void setTunnelingCapability(String tunnelingCapability) {
        this.tunnelingCapability = tunnelingCapability;
    }

    public boolean isDepend() {
        return depend;
    }

    public void setDepend(boolean depend) {
        this.depend = depend;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    public OntResource getDownNeighbour(OntModel m) {
        if (m != null)
            return m.getOntResource(downNeighbourUri);
        return null;
    }

    public void setDownNeighbour(OntResource downNeighbour) {
        downNeighbourUri = downNeighbour.getURI();
    }

    public OntResource getUpNeighbour(OntModel m) {
        if (m != null)
            return m.getOntResource(upNeighbourUri);
        return null;
    }

    public void setUpNeighbour(OntResource upNeighbour) {
        upNeighbourUri = upNeighbour.getURI();
    }

    public String getUpNeighbourUri() {
        return upNeighbourUri;
    }

    public void setUpNeighbourUri(String upNeighbourUri) {
        this.upNeighbourUri = upNeighbourUri;
    }

    public String getUpLocalUri() {
        return upLocalUri;
    }

    public void setUpLocalUri(String upLocalUri) {
        this.upLocalUri = upLocalUri;
    }

    public String getDownNeighbourUri() {
        return downNeighbourUri;
    }

    public void setDownNeighbourUri(String downNeighbourUri) {
        this.downNeighbourUri = downNeighbourUri;
    }

    public String getDownLocalUri() {
        return downLocalUri;
    }

    public void setDownLocalUri(String downLocalUri) {
        this.downLocalUri = downLocalUri;
    }

    public OntResource getUpLocal(OntModel m) {
        if (m != null)
            return m.getOntResource(upLocalUri);
        return null;
    }

    public void setUpLocal(OntResource upLocal) {
        upLocalUri = upLocal.getURI();
    }

    public OntResource getDownLocal(OntModel m) {
        if (m != null)
            return m.getOntResource(downLocalUri);
        return null;
    }

    public void setDownLocal(OntResource downLocal) {
        downLocalUri = downLocal.getURI();
    }

    public void setSwitchMatrix(SwitchMatrix matrix) {
        if (switchingMatrix == null)
            switchingMatrix = new LinkedList<SwitchMatrix>();
        switchingMatrix.add(matrix);
    }

    public SwitchMatrix getSwitchingMatrix(int i) {
        SwitchMatrix matrix = (SwitchMatrix) switchingMatrix.get(i);
        return matrix;
    }

    public LinkedList<SwitchMatrix> getSwitchingMatrix() {
        return switchingMatrix;
    }

    public LinkedList<SwitchingAction> getActionList() {
        return actionList;
    }

    public int getActionCount() {
        return actionCount;
    }

    public void setActionCount(int actionCount) {
        this.actionCount = actionCount;
    }

    public void setActionList(LinkedList<SwitchingAction> actionList) {
        this.actionList = actionList;
    }

    public void setSwitchingMatrix(LinkedList<SwitchMatrix> switchingMatrix) {
        this.switchingMatrix = switchingMatrix;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public void setResource(OntResource resource) {
        super.setResource(resource);
        if (resource != null) {
            name = resource.getURI();
        }
    }

    public HashMap<String, LinkedList<LabelSet>> getLabelSets() {
        return labelSets;
    }

    public void setLabelSets(HashMap<String, LinkedList<LabelSet>> labelSets) {
        this.labelSets = labelSets;
    }

    public LinkedList<LabelSet> getLabelSet(String rType) {
        return labelSets.get(rType);
    }

    public float getStaticLabel() {
        return staticLabel;
    }

    public void setStaticLabel(float staticLabel) {
        this.staticLabel = staticLabel;
    }

    public String getAvailableLabelSet() {
        return availableLabelSet;
    }

    public void setAvailableLabelSet(BitSet bitSet) {
        if (bitSet != null)
            this.availableLabelSet = bitSet.toString().replace("{", "").replace("}", "");
    }

    public boolean isAllocatable() {
        return isAllocatable;
    }

    public void setAllocatable(boolean isAllocatable) {
        this.isAllocatable = isAllocatable;
    }

    public String getNetUUID() {
        return NetUUID;
    }

    public void setNetUUID(String netUUID) {
        NetUUID = netUUID;
    }

}
