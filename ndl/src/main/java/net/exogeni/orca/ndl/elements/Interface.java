package net.exogeni.orca.ndl.elements;

import java.net.UnknownHostException;
import java.util.LinkedList;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import net.jwhoisserver.utils.InetNetworkException;
import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.Persistent;

public class Interface extends NetworkElement {

    @Persistent
    private String upLayer;
    @Persistent
    private String lowLayer;

    @Persistent
    private String usedLabelSetUri = null;
    @Persistent
    private Label label;

    // Leaving NotPersistent because NetworkElement.clientInterfaces and this point to each other.
    // Also it is not actually used for anything as of 05/14 /ib
    @NotPersistent
    private LinkedList<Interface> serverInterface;
    @Persistent
    private Interface input, output;
    @Persistent
    private Integer mark; // -1--not used; 0--input; 1--output

    public Interface(OntModel m, String u, String n) {
        super(m, u, n);
        Individual new_intf_ont = null;
        if (this.getResource() == null)
            new_intf_ont = model.createIndividual(u, NdlCommons.interfaceOntClass);
    }

    public Interface() {
    }

    public Interface(OntModel m, OntResource rs) {
        super(m, rs);
    }

    public void setLabel(String ip, String mask) throws UnknownHostException, InetNetworkException {
        label = new IPAddress(ip, mask);
        label.setResource(getResource());
        // NOTE: here I'm assuming that model is the right place to look for the label resource based on its URI /ib
        if ((((IPAddress) label).getCIDR() != null)
                && (label.getResource(model).getProperty(NdlCommons.layerLabelIdProperty) == null))
            label.getResource(model).addProperty(NdlCommons.layerLabelIdProperty, ((IPAddress) label).getCIDR());
        logger.debug(
                "Interface:" + label.getResource(model).getProperty(NdlCommons.layerLabelIdProperty) + "-----ip=" + ip);
    }

    public Interface(OntModel m, OntResource rs, boolean client) {

        super(m, rs.getURI(), rs.getLabel(null));

        setResource(rs);
        String layer = NdlCommons.findLayer(m, rs);
        setAtLayer(layer);

        if (client)
            findClientInterface(this); // recursively

        findUNIInterface(rs, this);
    }

    // find the client interface in the adaptation for a server interface

    public void findClientInterface(Interface parent) {

        ResultSet results = NdlCommons.getLayerAdapatation(this.model, parent.getURI());

        // outputQueryResult(results);
        Resource parent_rs = parent.getResource();
        Interface intf = null;
        Resource rs = null;
        OntResource rs_ont;

        String varName = (String) results.getResultVars().get(0);
        // changed from while to if 03/10/2010 /ib
        if (results.hasNext()) {
            rs = results.nextSolution().getResource(varName);
            rs_ont = model.getOntResource(rs);
            intf = new Interface(this.model, rs_ont, true); // recursion
            parent.addClientInterface(intf);
            intf.addServerInterface(parent);
            rs_ont.addProperty(NdlCommons.adaptationPropertyOf, parent_rs);
            parent_rs.addProperty(NdlCommons.adaptationProperty, rs);
        }
    }

    // get its 2 uni interfaces for a bi interface
    public void findUNIInterface(Resource rs, Interface intf) {
        Statement st = rs.getProperty(NdlCommons.hasInputInterface);
        if (st != null)
            intf.setInput(new Interface(this.getModel(), model.getOntResource(st.getResource()), true));
        // installed a 100ms delay to rate-limit Jena queries 03/10/2010 /ib
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
        st = rs.getProperty(NdlCommons.hasOutputInterface);
        if (st != null)
            intf.setOutput(new Interface(this.model, model.getOntResource(st.getResource()), true));
        // intf.print();
    }

    public void addClientInterface(OntModel ontModel, Interface client, String adaptation) {
        addClientInterface(client);

        Property adaptationP = ontModel.createProperty(NdlCommons.ORCA_NS + "layer.owl#" + adaptation);

        Resource rs_client = client.getResource();
        getResource().addProperty(NdlCommons.adaptationProperty, rs_client);
        getResource().addProperty(adaptationP, rs_client);
        // System.out.println(rs_client.getURI()+":"+parent.getURI());
        rs_client.addProperty(NdlCommons.topologyInterfaceOfProperty,
                getResource().getProperty(NdlCommons.topologyInterfaceOfProperty).getResource());
        rs_client.addProperty(NdlCommons.adaptationPropertyOf, getResource());
    }

    public void addServerInterface(Interface intf) {
        if (serverInterface == null)
            serverInterface = new LinkedList<Interface>();
        serverInterface.add(intf);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());

        sb.append("Label:");
        if (label != null)
            sb.append(label);
        int size = 0;
        int i = 0;

        Interface intf = null;
        if (clientInterface != null) {
            size = clientInterface.size();
            sb.append("Client Interface:" + size);
            for (i = 0; i < size; i++) {
                intf = (Interface) clientInterface.get(i);
                sb.append(intf.toString());
            }
        }
        return sb.toString();
    }

    public String getLowLayer() {
        return lowLayer;
    }

    public void setLowLayer(String lowLayer) {
        this.lowLayer = lowLayer;
    }

    public String getUpLayer() {
        return upLayer;
    }

    public void setUpLayer(String upLayer) {
        this.upLayer = upLayer;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public LinkedList<Interface> getServerInterface() {
        return serverInterface;
    }

    public void setServerInterface(LinkedList<Interface> serverInterface) {
        this.serverInterface = serverInterface;
    }

    public Interface getInput() {
        return input;
    }

    public void setInput(Interface input) {
        this.input = input;
    }

    public Interface getOutput() {
        return output;
    }

    public void setOutput(Interface output) {
        this.output = output;
    }

    public Resource getUsedLabelSet(Model m) {
        if (m != null)
            return m.getResource(usedLabelSetUri);
        return null;
    }

    public void setUsedLabelSet(Resource usedLabelSet) {
        usedLabelSetUri = usedLabelSet.getURI();
    }

}
