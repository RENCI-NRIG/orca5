package orca.embed.cloudembed;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import orca.embed.cloudembed.controller.InterDomainHandler;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.RequestParserListener;
import orca.ndl.LayerConstant;
import orca.ndl.LayerConstant.Layer;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlRequestParser;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchingAction;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.PersistenceUtils;
import orca.util.persistence.Persistent;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDB;

public class NetworkHandler extends MappingHandler {

    // has only transient state
    @NotPersistent
    protected ConnectionManager mapper;

    // NOTE: can be gotten rid of
    @NotPersistent
    protected Properties pdomain_properties;

    protected static class NHState implements Persistable {
        @Persistent
        HashMap<String, BitSet> localAssignedLabel = new HashMap<String, BitSet>();

        /**
         * Merge label maps
         * 
         * @param t t
         */
        void merge(NHState t) {
            for (String key : t.localAssignedLabel.keySet()) {
                if (localAssignedLabel.containsKey(key)) {
                    localAssignedLabel.get(key).or(t.localAssignedLabel.get(key));
                } else
                    localAssignedLabel.put(key, t.localAssignedLabel.get(key));
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("LocalAssignedLabel:\n");
            for (Entry<String, BitSet> e : localAssignedLabel.entrySet()) {
                sb.append(e.getKey() + ": " + e.getValue() + "\n");
            }
            return sb.toString();
        }
    }

    // NOTE: this contains labels selected by this actor. There can be multiple labels per layer generated for each
    // reservation. Can be restored through revisit.
    @NotPersistent
    protected NHState nhState = new NHState();

    /**
     * Create handler with in-memory model
     * 
     * @param substrateFile substrateFile
     * @throws IOException in case of error
     * @throws NdlException in case of error
     */
    public NetworkHandler(String substrateFile) throws IOException, NdlException {
        super(substrateFile);
    }

    /**
     * Create handler with TDB-backed model in a directory with specified path prefix
     * 
     * @param substrateFile substrateFile
     * @param tdbPrefix tdbPrefix
     * @throws IOException in case of error
     * @throws NdlException in case of error
     */
    public NetworkHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
        super(substrateFile, tdbPrefix);
    }

    public NetworkHandler(OntModel idm_model) {
        super(idm_model);
    }

    public SystemNativeError handleRequest(RequestReservation rr) throws NdlException {
        String domainName = rr.getReservationDomain();
        SystemNativeError error = runEmbedding(domainName, rr);

        return error;
    }

    public RequestReservation getRequestReservation(String request) throws NdlException {
        RequestParserListener parserListener = new RequestParserListener();
        // run the parser (to create Java objects)
        NdlRequestParser nrp = new NdlRequestParser(request, parserListener);
        nrp.doLessStrictChecking();
        nrp.processRequest();
        RequestReservation requestReservation = parserListener.getRequest();
        return requestReservation;
    }

    @SuppressWarnings("unchecked")
    public SystemNativeError runEmbedding(String domainName, RequestReservation rr) {
        logger.debug("NetworkHandler.runEmbedding() for " + domainName);
        RequestReservation request = rr;
        OntModel requestModel = rr.getModel();
        Collection<NetworkElement> elements = request.getElements();
        LinkedList<NetworkElement> deviceList = new LinkedList<NetworkElement>();

        try {
            mapper = new ConnectionManager(requestModel, idm, false);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (Entry<String, BitSet> entry : nhState.localAssignedLabel.entrySet()) {
            logger.debug(
                    "NetworkHandler.runEmbedding(): set used layer label=" + entry.getKey() + "---" + entry.getValue());
            mapper.setUsedLabelSetPerLayer(entry.getKey(), entry.getValue());
        }
        mapper.setPdomain_properties(this.getPdomain_properties());
        SystemNativeError error = null;
        // if the request is from the edge interfaces, adding the two devices/interfaces connecting to these 2 edge
        // interfaces to the substrate model
        edgeRequestModel(requestModel);
        // mapper.getOntModel().write(System.out);
        // (1) find the the device list for current request;
        // (2) Form the (physical) interface list for the switching action for each device
        Iterator<NetworkElement> it = elements.iterator();
        while (it.hasNext()) {
            NetworkElement ne = it.next();
            logger.info("NetworkHandler:Request Connection:" + ne.getName() + ":" + ne.getURI());
            NetworkConnection nc = null;
            if (ne instanceof NetworkConnection) {
                nc = (NetworkConnection) ne;
            } else {
                continue;
            }
            // (0) path finding
            // (1) define the adapted client interface and find the right label according to the switching capability
            // (2) Convert to uni ports for Polatis
            // (3) update the action interface list and the ontModel
            // (4) fire the switching action to form the crossconnects by property "switchedTo"
            NetworkConnection connection = null;
            try {
                error = mapper.createConnection(nc, false, false, null);
                if (error == null) {
                    connection = mapper.getDeviceConnection();
                    error = mapper.processDeviceConnection(connection);
                }
            } catch (Exception e) {
                error = new SystemNativeError();
                error.setErrno(1);
                error.setMessage("Error in proecessDeviceconnection! NetworkConnection=" + request.getReservation());
                logger.error("Error in proecessDeviceconnection!");
                e.printStackTrace();
            }

            if (error != null) {
                finished(requestModel);
                return error;
            }
            // Create the virtual connection between device peers at the same layer
            try {
                mapper.createVirtualConnection(request.getReservation(), connection);
            } catch (Exception e) {
                error = new SystemNativeError();
                error.setErrno(1);
                error.setMessage("Error in createVirtualConnection! NetworkConnection=" + request.getReservation());
                logger.error("Error in createVirtualConnection!");
                e.printStackTrace();
            }

            if (error != null) {
                finished(requestModel);
                return error;
            }

            // logger.info("------Device list in the connection");
            // connection.print();
            if (connection != null)
                requestMap.put(request.getReservation(), connection);

            deviceList = (LinkedList<NetworkElement>) mapper.deviceConnection.getConnection();
        }

        setUsedLabel(deviceList, true);

        finished(requestModel);

        TDB.sync(requestModel);
        TDB.sync(idm);

        return error;
    }

    public void finished(OntModel requestModel) {
        mapper.getOntModel().remove(requestModel);
    }

    public void setUsedLabel(LinkedList<NetworkElement> deviceList, boolean swapping) {
        int numDevice = deviceList.size(), count = 0;
        BitSet layerBitSet = null;
        String layer = null;
        // first deal with the locally allocated tags
        for (NetworkElement nd : deviceList) {
            Device d = (Device) nd;
            logger.debug("setUsedLabel:" + d.getName());
            // if(d.isAllocatable()==false)
            // continue;
            LinkedList<SwitchingAction> actions = ((Device) d).getActionList();
            if (actions == null)
                continue;
            // int anum = 0;
            SwitchingAction a = null;
            for (int i = 0; i < actions.size(); i++) {
                a = actions.get(i);
                logger.debug("Action=" + a.getDefaultAction() + ";" + a.getAtLayer());
                if (Objects.equals(a.getDefaultAction(), LayerConstant.Action.Temporary.toString())) {
                    continue;
                }
                layer = a.getAtLayer();
                layerBitSet = nhState.localAssignedLabel.get(layer);
                if (layerBitSet == null) {
                    if (layer.equals(Layer.EthernetNetworkElement.toString())) {
                        layerBitSet = new BitSet(InterDomainHandler.max_vlan_tag);
                        nhState.localAssignedLabel.put(layer, layerBitSet);
                    }
                }
                if (layerBitSet != null) {
                    logger.debug("set used tag = " + (int) a.getLabel_ID());
                    layerBitSet.set((int) a.getLabel_ID());
                }
            }
            // Then deal with the tags passed in from the parents, if the domain does tag translation
            int static_label = 0, p_tag = 0;
            if (swapping && (d.getURI().contains("up") || d.getURI().contains("down"))) { // the first and last devices
                                                                                          // are the servers that may
                                                                                          // carry labels
                a = d.getDefaultSwitchingAction();
                if (a != null) {
                    layer = a.getAtLayer();
                    layerBitSet = nhState.localAssignedLabel.get(layer);
                    Interface intf = a.getDefaultClientInterface();
                    if (intf != null) {
                        Resource p_intf_rs = null;
                        logger.info("intf=" + intf.getResource().getURI());
                        if (intf.getResource().hasProperty(NdlCommons.linkTo)) {
                            p_intf_rs = intf.getResource().getProperty(NdlCommons.linkTo).getResource();
                            if (p_intf_rs != null) {
                                logger.info(";p_intf=" + p_intf_rs.getURI());
                                p_tag = this.mapper.get_pdomain_tag(p_intf_rs.getURI(), static_label,
                                        this.pdomain_properties);
                                if ((p_tag != 0) && (layerBitSet != null)) {
                                    logger.debug("set used tag from the parent p_intf=" + p_intf_rs.getURI()
                                            + "; p_tag = " + p_tag);
                                    layerBitSet.set(p_tag);
                                    a.setLabel_ID(p_tag);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Finally set all the used tags
        for (Entry<String, BitSet> entry : nhState.localAssignedLabel.entrySet()) {
            logger.debug("Set used layer labe=" + entry.getKey() + "---" + entry.getValue());
            mapper.setUsedLabelSetPerLayer(entry.getKey(), entry.getValue());
        }
    }

    public void edgeRequestModel(OntModel requestModel) {
        /*
         * try { FileOutputStream ben_os = new FileOutputStream("ben-request.rdf"); requestModel.write(ben_os);
         * System.exit(0); } catch (FileNotFoundException e) { // TODO Auto-generated catch block e.printStackTrace(); }
         */
        OntModel substrateModel = mapper.getOntModel();
        substrateModel.add(requestModel);

        /*
         * ResultSet results = mapper.connectedDevicePair(requestModel);
         * 
         * String var0=(String) results.getResultVars().get(0); String var1=(String) results.getResultVars().get(1);
         * QuerySolution solution=null; Resource rs1,rs2; OntResource ont_rs1,ont_rs2;
         * 
         * mapper.outputQueryResult(results);
         * 
         * results = mapper.connectedDevicePair(requestModel);
         * 
         * while (results.hasNext()){ solution=results.nextSolution(); rs1=solution.getResource(var0);
         * rs2=solution.getResource(var1);
         * 
         * ont_rs1=substrateModel.getOntResource(rs1); ont_rs2=substrateModel.getOntResource(rs2);
         * 
         * ont_rs1.removeProperty(NdlCommons.connectedTo, rs2); ont_rs2.removeProperty(NdlCommons.connectedTo, rs1); }
         */
    }

    synchronized public NetworkConnection releaseReservation(String reservation) throws Exception {
        logger.debug("NetworkHandler.releaseReservation(): " + reservation);
        // NetworkConnection connection=requestMap.get(reservation);

        // NetworkConnection releaseConnection=mapper.releaseConnection(connection,reservation);
        NetworkConnection releaseConnection = mapper.getReleaseNetworkConnection();
        if (releaseConnection == null) {
            logger.error("Release connection is NULL!");
            return null;
        }
        releaseConnection.print();

        mapper.releaseInModel(releaseConnection, reservation);

        return releaseConnection;
    }

    synchronized public NetworkConnection getConnectionTeardownActions(String reservation) {
        NetworkConnection conn = getConnection(reservation);
        if (conn == null) {
            throw new RuntimeException("Missing connection");
        }
        // in the recovery case, if there is no new creation before closing,
        // the mapper is null because it is not persistent. so we create a new one.
        if (mapper == null) {
            try {
                mapper = new ConnectionManager(null, idm, false);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return mapper.releaseConnection(conn, reservation);
    }

    public NetworkConnection getDeviceConnection() {
        return mapper.deviceConnection;
    }

    public ConnectionManager getMapper() {
        return mapper;
    }

    public void setMapper(ConnectionManager mapper) {
        this.mapper = mapper;
    }

    public Properties getPdomain_properties() {
        return pdomain_properties;
    }

    public void setPdomain_properties(Properties pdomain_properties) {
        this.pdomain_properties = pdomain_properties;
    }

    /**
     * Return propertly list encoding the current state of assigned labels
     * 
     * @return Properties
     * @throws Exception in case of error
     */
    public Properties saveLocalLabels() throws Exception {
        return PersistenceUtils.save(nhState);
    }

    /**
     * OR the incoming map's bitsets with what we have already
     * 
     * @param hStateProps hStateProps
     * @throws PersistenceException in case of error
     */
    public void restoreAssignedLabels(Properties hStateProps) throws PersistenceException {
        NHState tmpState = PersistenceUtils.restore(hStateProps);
        nhState.merge(tmpState);
    }

    public void recoverConnection(String uri, NetworkConnection nc) {
        if (nc == null)
            return;
        recoverConnectionModel(nc);
        requestMap.put(uri, nc);
    }

    private void recoverConnectionModel(NetworkConnection nc) {
        if (nc == null)
            return;
        NetworkElement ne = nc.getNe1();
        if (ne != null)
            ne.setModel(this.idm);
        ne = nc.getNe2();
        if (ne != null)
            ne.setModel(this.idm);
        for (NetworkElement e : nc.getConnection())
            e.setModel(this.idm);

        for (NetworkConnection clientConnection : nc.getClientConnections())
            recoverConnectionModel(clientConnection);
    }

    public String toString() {
        return "NetworkHandler ConnectionManager: " + super.toString() + "\nmapper: " + mapper + "\n" + nhState;
    }
}
