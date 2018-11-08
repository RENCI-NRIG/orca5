package orca.embed.cloudembed;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchingAction;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDB;

public class MultiPointNetworkHandler extends NetworkHandler {

    public MultiPointNetworkHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
        super(substrateFile, tdbPrefix);
    }

    public MultiPointNetworkHandler(String substrateFile) throws IOException, NdlException {
        super(substrateFile);
    }

    public MultiPointNetworkHandler(OntModel idm_model) throws IOException {
        super(idm_model);
    }

    // Override
    public SystemNativeError handleRequest(String request) throws NdlException {
        RequestReservation requestReservation = getRequestReservation(request);
        String domainName = requestReservation.getReservationDomain();
        SystemNativeError error = runEmbedding(domainName, requestReservation);

        return error;
    }

    @SuppressWarnings("unchecked")
    public SystemNativeError runEmbedding(String domainName, RequestReservation rr) {
        logger.debug("MultiPointNetworkHandler.runEmbedding() for domain " + domainName);
        LinkedList<NetworkElement> deviceList = new LinkedList<NetworkElement>();
        SystemNativeError error = null;
        RequestReservation request = rr;
        OntModel requestModel = rr.getModel();
        domainName = NdlCommons.getOrcaDomainName(domainName);

        if (debugOn) {
            try {
                String fileName = domainName + "-subrequest.rdf";
                OutputStream fsw = new FileOutputStream(fileName);
                requestModel.write(fsw);
            } catch (FileNotFoundException e1) {
                logger.error(
                        "MultiPointNetworkHandler.runEmbedding(): unable to save RDF file due to " + e1.getMessage());
            } catch (Exception ee) {
                logger.error(
                        "MultiPointNetworkHandler.runEmbedding(): unable to save RDF file due to " + ee.getMessage());
            }
        }

        Collection<NetworkElement> elements = request.getElements();

        try {
            mapper = new ConnectionManager(requestModel, idm, false);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error("Unable to create new ConnectionManager");
            e.printStackTrace();
        }

        for (Entry<String, BitSet> entry : nhState.localAssignedLabel.entrySet()) {
            logger.debug("Set used layer labe=" + entry.getKey() + "---" + entry.getValue());
            mapper.setUsedLabelSetPerLayer(entry.getKey(), entry.getValue());
        }
        mapper.setPdomain_properties(getPdomain_properties());
        edgeRequestModel(requestModel);
        // mapper.getOntModel().write(System.out);
        // (1) find the the device list for current request;
        // (2) Form the (physical) interface list for the switching action for each device
        Iterator<NetworkElement> it = elements.iterator();
        while (it.hasNext()) {
            NetworkElement ne = it.next();
            logger.debug("NetworkHandler:Request Connection:" + ne.getName() + ":" + ne.getURI());
            NetworkConnection nc = null;
            if (ne instanceof NetworkConnection) {
                nc = (NetworkConnection) ne;
            } else {
                continue;
            }

            if ((nc.getCastType() != null) && (nc.getCastType().equalsIgnoreCase(NdlCommons.multicast)))// Yes: MP
                                                                                                        // connection
                error = handleMP(nc, requestModel, deviceList);
            else {
                error = mapper.createConnection(nc, false, true, null);
                if (error != null) {
                    if (error.getErrno() >= 1000) {
                        logger.info(error.toString());
                        error = handleMP(nc, requestModel, deviceList);
                    } else {
                        logger.info("embedding finished 1, error=" + error.toString());
                        finished(requestModel);
                        return error;
                    }
                } else {
                    deviceList = (LinkedList<NetworkElement>) mapper.deviceConnection.getConnection();
                    error = mapper.processDeviceConnection(mapper.deviceConnection);
                }
            }
        }

        if (error != null && error.getErrno() < 1000) {
            logger.info("embedding finished 2, error=" + error.toString());

            String uri = request.getReservation();
            try {
                NetworkConnection teardown = this.getConnectionTeardownActions(uri);
                NetworkConnection releaseConnection = this.getMapper().getReleaseNetworkConnection();
                releaseConnection = this.releaseReservation(uri);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            finished(requestModel);
            return error;
        }

        NetworkConnection connection = mapper.getDeviceConnection();
        if (connection != null)
            requestMap.put(request.getReservation(), connection);

        setUsedLabel(deviceList, false);

        finished(requestModel);

        TDB.sync(requestModel);
        TDB.sync(idm);

        return error;
    }

    public void finished(OntModel requestModel) {
        // mapper.removeInConnectionProperty("domain:isAllocatable",NdlCommons.domainIsAllocatable);
        mapper.removeInConnectionProperty("topology:portOccupied", NdlCommons.portOccupied);
        mapper.removeInConnectionProperty("ndl:visited", NdlCommons.visited);
        mapper.getOntModel().remove(requestModel);
    }

    public void finished() {
        mapper.removeInConnectionProperty("topology:portOccupied", NdlCommons.portOccupied);
        mapper.removeInConnectionProperty("ndl:visited", NdlCommons.visited);
    }

    @SuppressWarnings("unchecked")
    protected SystemNativeError handleMP(NetworkConnection nc, OntModel requestModel,
            LinkedList<NetworkElement> deviceList) {
        SystemNativeError error = null;
        Resource mp_device_rs = NdlCommons.getDomainHasCastType(NdlCommons.multicast, "topology:hasSwitchMatrix", idm);
        OntResource mp_device_ont = requestModel.createIndividual(mp_device_rs.getURI(), NdlCommons.deviceOntClass);
        NetworkElement mp_device = new NetworkElement(requestModel, mp_device_ont);
        DomainResourceType dType = nc.getResourceType();
        if (dType.getResourceType() == null)
            dType.setResourceType(dType.VLAN_RESOURCE_TYPE);
        mp_device.setResourceType(dType);

        LinkedList<NetworkConnection> leaves = new LinkedList<NetworkConnection>();
        NetworkConnection branch_nc = null;
        if ((nc.getCastType() != null) && (nc.getCastType().equalsIgnoreCase(NdlCommons.multicast))) {
            for (Object leaf : nc.getConnection()) {
                branch_nc = formBranchNC(mp_device, (NetworkElement) leaf, nc);
                leaves.add(branch_nc);
            }
        } else {
            if (nc.getNe1() != null) {
                branch_nc = formBranchNC(mp_device, nc.getNe1(), nc);
                leaves.add(branch_nc);
            }
            if (nc.getNe2() != null) {
                branch_nc = formBranchNC(mp_device, nc.getNe2(), nc);
                leaves.add(branch_nc);
            }
        }
        LinkedList<Device> d_list = null;
        for (NetworkConnection b_nc : leaves) {
            error = mapper.createConnection(b_nc, false, false, null);
            if (error != null)
                break;
            d_list = (LinkedList<Device>) mapper.deviceConnection.getConnection();
            error = mapper.processDeviceConnection(mapper.deviceConnection);
            if (error != null)
                break;
            boolean isAllocatable = true;
            for (Device d : d_list) {
                // if(d.getCastType()==null || (!d.getCastType().equalsIgnoreCase(NdlCommons.multicast)))
                // continue;
                isAllocatable = d.isAllocatable();

                if (!isAllocatable) {
                    continue;
                } else {
                    LinkedList<SwitchingAction> actions = ((Device) d).getActionList();
                    if (actions != null) {
                        for (int i = 0; i < actions.size(); i++) {
                            SwitchingAction a = actions.get(i);
                            LinkedList<Interface> ifs = a.getClientInterface();
                            if (ifs == null)
                                continue;
                            for (Interface intf : ifs) {
                                OntResource intf_ont = idm.getOntResource(intf.getResource());
                                intf_ont.addProperty(NdlCommons.portOccupied, "true", XSDDatatype.XSDboolean);
                                if (intf_ont.hasProperty(NdlCommons.adaptationPropertyOf)) {
                                    intf_ont.getProperty(NdlCommons.adaptationPropertyOf).getResource()
                                            .addProperty(NdlCommons.portOccupied, "true", XSDDatatype.XSDboolean);
                                }
                            }
                        }
                    }
                }

                Device existing_d = mapper.getDevice(d, deviceList);
                if (existing_d == null)
                    deviceList.add(d);
                else {
                    existing_d.addSwitchingAction(d.getDefaultSwitchingAction());
                }
            }

        }
        if (error != null)
            return error;
        mapper.deviceConnection.setConnection(deviceList);
        return error;
    }

    public NetworkConnection formBranchNC(NetworkElement mp_device, NetworkElement leaf, NetworkConnection nc) {
        String nc_name = mp_device.getName() + "-" + leaf.getName();
        NetworkConnection b_nc = nc.copy(nc_name);
        b_nc.setResourceType(mp_device.getResourceType());
        b_nc.setNe1(mp_device);
        b_nc.setNe2(leaf);
        logger.info("Formed leaf:" + leaf.getName());
        return b_nc;
    }

}
