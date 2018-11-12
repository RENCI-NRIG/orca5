package orca.embed.cloudembed.controller;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.cloudembed.IConnectionManager;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.ModifyElement;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.ModifyReservations;
import orca.embed.workflow.RequestParserListener;
import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.ndl.DomainResourceType;
import orca.ndl.INdlModifyModelListener;
import orca.ndl.INdlModifyModelListener.ModifyType;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlRequestParser;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.IPAddress;
import orca.ndl.elements.IPAddressRange;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.RequestSlice;
import orca.shirako.common.meta.UnitProperties;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ModifyHandler extends UnboundRequestHandler {
    ModifyReservations modifies = new ModifyReservations();
    LinkedList<Device> addedDevices = new LinkedList<Device>();
    LinkedList<Device> modifiedDevices = new LinkedList<Device>();

    SystemNativeError err = null;

    public ModifyHandler() throws NdlException {
        super();
    }

    public ModifyHandler(IConnectionManager icm) throws NdlException {
        super(icm);
    }

    /**
     * Create handler with in-memory model
     * 
     * @param substrateFile substrateFile
     * @throws IOException in case of error
     * @throws NdlException in case of error
     */
    public ModifyHandler(String substrateFile) throws IOException, NdlException {
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
    public ModifyHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
        super(substrateFile, tdbPrefix);
    }

    /**
     * Create a handler with TDB-backed blank model or try to recover existing TDB model
     * 
     * @param tdbPrefix tdbPrefix
     * @param recover recover
     * @throws IOException in case of error
     * @throws NdlException in case of error
     */
    public ModifyHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
        super(tdbPrefix, recover);
    }

    public SystemNativeError modifySlice(DomainResourcePools domainResourcePools,
            Collection<ModifyElement> modifyElements, OntModel manifestOnt, String sliceId,
            HashMap<String, Collection<DomainElement>> nodeGroupMap, HashMap<String, DomainElement> firstGroupElement,
            OntModel requestModel, OntModel modifyRequestModel) throws UnknownHostException, InetNetworkException {
        this.modifyVersion++;
        this.isModify = true;
        this.err = null;

        if ((modifyElements == null) || (modifyElements.size() == 0)) {
            err = new SystemNativeError();
            err.setErrno(8);
            err.setMessage("modifyElements is empty!");
            logger.error(err.toString());
            return err;
        }

        logger.debug("ModifyHandler::modifySlice() starts....");
        RequestReservation addedRequest = null;
        LinkedList<ModifyElement> addList = new LinkedList<ModifyElement>();
        try {
            Iterator<ModifyElement> mei = modifyElements.iterator();
            while (mei.hasNext()) {
                ModifyElement me = mei.next();
                logger.debug("ModifyHandler::modifySlice():" + me.getModType());
                if (me.getModType().equals(INdlModifyModelListener.ModifyType.REMOVE)) {
                    err = removeElement(me, manifestOnt, nodeGroupMap, firstGroupElement, deviceList);
                }

                if (me.getModType().equals(INdlModifyModelListener.ModifyType.ADD)
                        || me.getModType().equals(INdlModifyModelListener.ModifyType.MODIFY)) {
                    addList.add(me);
                }

                if (me.getModType().equals(INdlModifyModelListener.ModifyType.INCREASE)) {
                    err = addNodeGroupElements(me, manifestOnt, nodeGroupMap, firstGroupElement, requestModel,
                            deviceList);
                }
                if (err != null) {
                    logger.error(err.getMessage());
                    break; // TODO: should return error? otherwise addElement will still get called below?
                }
            }
            addedRequest = addElement(domainResourcePools, addList, manifestOnt, sliceId, modifyRequestModel);
        } catch (Exception e) {
            // e.printStackTrace();
            err = new SystemNativeError();
            err.setMessage("Exception generated: " + e.getClass().getSimpleName() + ", " + e.getMessage());
            logger.error(e.getMessage(), e);
        }

        if (err != null)
            return err;

        logger.debug(debug_str());

        // modify the manifest
        OntResource manifest = NdlCommons.getOntOfType(manifestOnt, "request:Manifest");
        createManifest(addedRequest, manifestOnt, manifest); //

        LinkedList<Device> nodeGroupAddedDevices = new LinkedList<Device>();
        for (Device device : addedDevices) {
            DomainElement de = (DomainElement) device;
            if ((de.getType().endsWith("vm")) || (de.getType().endsWith("baremetalce"))
                    || (de.getType().endsWith("lun")))
                if (de.getCe() != null && !de.isModify() && !NdlCommons.isStitchingNodeInManifest(de.getResource()))
                    if (de.getCe().getDependencies().size() == 0 || de.getCe().getGroup() != null) // not part of a
                                                                                                   // interdomain link
                        nodeGroupAddedDevices.add(device);
        }
        logger.debug("ModifyHandler:modifySlice():nodeGroupAddedDevices:" + nodeGroupAddedDevices.size());
        createManifest(manifestOnt, manifest, nodeGroupAddedDevices); // out of increasing nodeGroudp

        return err;
    }

    public List<ReservationMng> modifyStorage(HashMap<String, List<ReservationMng>> m_map) {
        List<ReservationMng> a_r = m_map.get(ModifyType.ADD.toString());
        List<ReservationMng> m_r = m_map.get(ModifyType.MODIFY.toString());
        if (a_r == null || m_r == null) {
            logger.debug("ModifyHandler::modifyStorage()No added or modified r");
            return null;
        }
        logger.debug("ModifyHandler:Start modifying storage:a_r.size=" + a_r.size() + ";m_r size=" + m_r.size());
        List<ReservationMng> extra_ar = new ArrayList<ReservationMng>();
        Properties config_ar = null, config_mr = null, local_ar = null, local_mr = null;
        for (ReservationMng ar : a_r) {
            config_ar = OrcaConverter.fill(ar.getConfigurationProperties());
            local_ar = OrcaConverter.fill(ar.getLocalProperties());
            String url_ar = config_ar.getProperty(UnitProperties.UnitHostNameUrl);

            if (url_ar == null) {
                logger.error("ModifyHandler::modifyStorage():ar reservation no url property:" + ar.getReservationID());
                continue;
            }
            for (ReservationMng mr : m_r) {
                config_mr = OrcaConverter.fill(mr.getConfigurationProperties());
                local_mr = OrcaConverter.fill(mr.getLocalProperties());
                String url_mr = config_mr.getProperty(UnitProperties.UnitHostNameUrl);

                if (url_mr == null) {
                    logger.error("ModifyHandler::modifyStorage():mr reservation no url property:" + mr.getReservationID());
                    continue;
                }

                if (url_ar.equals(url_mr)) {
                    logger.debug("ModifyHandler::modifyStorage():Modifying via adding storage url_al=" + url_ar + ";" + ";url_ml=" + url_mr);
                    logger.debug("ModifyHandler::modifyStorage():config:" + config_ar.toString());
                    logger.debug("ModifyHandler::modifyStorage():local:" + local_ar.toString());
                    mr.setConfigurationProperties(OrcaConverter.merge(config_ar, mr.getConfigurationProperties()));
                    mr.setLocalProperties(OrcaConverter.merge(local_ar, mr.getLocalProperties()));
                    extra_ar.add(ar);
                }
            }
        }

        a_r.removeAll(extra_ar);

        logger.debug("ModifyHandler::modifyStorage():After modifying storage:a_r.size=" + a_r.size() + ";m_r size=" + m_r.size());
        return extra_ar;
    }

    public void modifyComplete() {
        this.modifies.clear();
        this.addedDevices.clear();

        // devices are now updated in place, not added/deleted

        // resume all the isModify flag to flase
        for (NetworkElement d_s : deviceList)
            d_s.setModify(false);

        for (LinkedList<Device> list : this.domainConnectionList.values())
            logger.debug("ModifyHandler::modifyComplete():Modify complete, list size=" + list.size() + ";deviceList size=" + deviceList.size());

        this.modifiedDevices.clear();
        this.isModify = false;
    }

    public RequestReservation addElement(DomainResourcePools domainResourcePools, LinkedList<ModifyElement> meList,
            OntModel manifestOntModel, String sliceId, OntModel modifyRequestModel) throws NdlException, IOException {

        // generating new reservation in the format of a request RDF model
        if (meList.isEmpty()){
            return null;
        }

        Resource me = meList.element().getObj();
        String ns_str = me.getNameSpace();
        Resource reservation = modifyRequestModel.createIndividual(ns_str, NdlCommons.reservationOntClass);

        for (ModifyElement mee : meList) {
            me = mee.getObj();
            OntResource connection_rs = modifyRequestModel.createIndividual(me.getURI(),
                                                                            NdlCommons.getResourceType(me));
            modifyRequestModel.add(reservation, NdlCommons.collectionElementProperty, connection_rs);
            logger.debug("ModifyHandler::addElement():me=" + mee.getObj().getURI() + ";isModify=" + NdlCommons.isModify(me));
            copyProperty(modifyRequestModel, me);

            // add inDomain property from the manifest model
            if (!me.hasProperty(NdlCommons.inDomainProperty)) {
                OntResource me_manifest_ont = manifestOntModel.getOntResource(me.getURI());
                if (me_manifest_ont != null) {
                    Resource inDomain_rs = me_manifest_ont.getPropertyResourceValue(NdlCommons.inDomainProperty);
                    Resource rType_rs = me_manifest_ont
                            .getPropertyResourceValue(NdlCommons.domainHasResourceTypeProperty);
                    if (inDomain_rs != null && rType_rs != null) {
                        String rType = rType_rs.getLocalName();
                        String inDomain_str = inDomain_rs.getURI();
                        rType = rType.toLowerCase();
                        logger.debug("ModifyHandler::addElement():modify domain:" + inDomain_str + 
                                     ";" + inDomain_rs.getLocalName() + ";rType" + rType);
                        if (inDomain_rs.getLocalName().equalsIgnoreCase(rType)) {
                            inDomain_str = inDomain_str.split("/" + rType)[0];
                            inDomain_rs = inDomain_rs.getModel().createResource(inDomain_str);
                        }
                        logger.debug("ModifyHandler::addElement():modify domain:" + inDomain_rs.getURI());
                        connection_rs.addProperty(NdlCommons.inDomainProperty, inDomain_rs);
                        connection_rs.addProperty(NdlCommons.domainHasResourceTypeProperty, rType_rs);
                    }
                }
            }

            // double check isModify
            OntResource me_manifest_ont = manifestOntModel.getOntResource(me.getURI());
            if (me_manifest_ont == null) {
                connection_rs.removeAll(NdlCommons.isModifyProperty);
            }
            else if (!me.hasProperty(NdlCommons.isModifyProperty)) {
                connection_rs.addProperty(NdlCommons.isModifyProperty, "true");
            }
            else {
                connection_rs.getProperty(NdlCommons.isModifyProperty).changeLiteralObject(true);
            }
        }

        // parsing request
        RequestParserListener parserListener = new RequestParserListener();

        // NdlRequestParser nrp = new NdlRequestParser(out.toString(), parserListener);
        // run the parser (to create Java objects)
        NdlRequestParser nrp = new NdlRequestParser(modifyRequestModel, parserListener);
        nrp.processRequest();
        RequestReservation request = parserListener.getRequest();
        RequestSlice slice = request.getSlice();

        Collection<NetworkElement> requestElements = request.getElements();
        err = request.getError();
        if (err != null) {
            logger.error("ModifyHandler::addElement():Ndl request parser unable to parse request:" + err.toString());
            return null;
        }
        // TODO: check if the request is already fully bound in one domain, preparing for topology splitting
        boolean bound = request.generateGraph(requestElements);
        String reservationDomain = request.getReservationDomain();

        if (reservationDomain == null) {// invoke the embedding code
            err = runEmbedding(bound, request, domainResourcePools);
        } else { // intra-domain embedding
            err = runEmbedding(reservationDomain, request, domainResourcePools);
        }

        if (err != null) {
            logger.error("ModifyHandler::addElement():Modify failed:" + err.toString());
            return null;
        }

        for (NetworkElement device : this.getDeviceList()) {
            DomainElement dd = (DomainElement) device;
            logger.debug("ModifyHandler::addElement():dd=" + dd.getName() + ";isModify=" + dd.isModify() + ";dd.modifyversion="
                    + dd.getModifyVersion() + ";this.modifyversion=" + this.modifyVersion);

            if (dd.getModifyVersion() == this.modifyVersion) {
                if (dd.isModify()) {
                    if (!modifiedDevices.contains(dd)) {
                        modifies.addModifedElement(device.getResource());
                        modifiedDevices.add(dd);
                        // if it is adding storage modifying
                        if (addedDevices.contains(dd))
                            continue;
                        if (dd.getPrecededBy() != null) {
                            for (Entry<DomainElement, OntResource> parent : dd.getPrecededBySet()) {
                                DomainElement p_de = parent.getKey();
                                if (p_de.getResourceType().getResourceType().endsWith("lun")) {
                                    logger.debug("ModifyHandler::addElement(): parent storage=");
                                    modifies.addAddedElement(device.getResource());
                                    // added by the add operation
                                    addedDevices.add(dd);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    modifies.addAddedElement(device.getResource());
                    // added by the add operation
                    addedDevices.add(dd);
                }
            }
        }

        return request;
    }

    private String debug_str() {
        return "ModifyHandler:devicelist=" + this.getDeviceList().size() + ";addedDevice="
                + (addedDevices == null ? null : addedDevices.size()) + ";modifies.addedElement="
                + (modifies.getAddedElements() == null ? null : modifies.getAddedElements().size()) + ";modifiedDevice="
                + (modifiedDevices == null ? null : modifiedDevices.size()) + ";modifies.modifiedElement="
                + (modifies.getModifiedElements() == null ? null : modifies.getModifiedElements().size())
                + ";modifies.removedElement="
                + (modifies.getRemovedElements() == null ? null : modifies.getRemovedElements().size())
                + ";modifies.modifiedRemoveElement="
                + (modifies.getModifiedRemoveElements() == null ? null : modifies.getModifiedRemoveElements().size());
    }

    private void copyProperty(OntModel domainRequestModel, Resource me_rs) {
        if (me_rs.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.interfaceOntClass)) {
            Model model = me_rs.getModel();
            ResIterator it_ad = model.listResourcesWithProperty(NdlCommons.taggedEthernetProperty, me_rs);
            while (it_ad.hasNext()) {
                Resource sub = it_ad.next();
                domainRequestModel.add(sub, NdlCommons.taggedEthernetProperty, me_rs);
            }
        }
        StmtIterator it = me_rs.listProperties();
        if (it == null)
            return;
        Statement st = null;
        Property pr = null;
        Resource sb_rs = null, ob_rs = null;
        while (it.hasNext()) {
            st = it.next();
            pr = st.getPredicate();

            sb_rs = domainRequestModel.createResource(st.getSubject().getURI());
            try {
                ob_rs = domainRequestModel.createResource(st.getResource().getURI());
                domainRequestModel.add(sb_rs, pr, ob_rs);
                copyProperty(domainRequestModel, st.getResource());
            } catch (ResourceRequiredException e) {
                domainRequestModel.add(sb_rs, pr, st.getLiteral());
            }
        }
    }

    protected SystemNativeError addNodeGroupElements(ModifyElement me, OntModel manifestOntModel,
            HashMap<String, Collection<DomainElement>> nodeGroupMap, HashMap<String, DomainElement> firstGroupElement,
            OntModel requestModel, LinkedList<NetworkElement> deviceList)
            throws InetNetworkException, UnknownHostException, Exception {
        SystemNativeError error = null;

        int units = me.getModifyUnits();
        String n = me.getSub().getURI();
        int i = n.lastIndexOf("#");
        String group = i >= 0 ? n.substring(i + 1) : n;
        logger.info("ModifyHandler::addNodeGroupElements(): starts, units=" + units);
        logger.debug("ModifyHandler::addNodeGroupElements():nodeGroupMap size=" + nodeGroupMap.size());
        Collection<DomainElement> cde = nodeGroupMap.get(group);
        if ((cde == null) || (cde.isEmpty())) {
            error = new SystemNativeError();
            error.setErrno(8);
            error.setMessage("Adding element: group doesn't exist: group: " + group + ";cde=" + cde + ";nodeGroup size="
                    + nodeGroupMap.size());
            logger.error(error.toString());
            return error;
        }

        DomainElement firstElement = firstGroupElement.get(group);
        if (firstElement == null) {
            logger.error("ModifyHandler::addNodeGroupElements():Original element in the gorup is missed! group=" + group);
            Iterator<DomainElement> cde_it = cde.iterator();
            while (cde_it.hasNext()) {
                firstElement = cde_it.next();
                break;
            }
        } else {
            logger.info("ModifyHandler::addNodeGroupElements():First element:" + firstElement.getURI() + ";cde size=" + cde.size());
        }

        HashMap<String, IPAddressRange> group_base_ip = null;
        try {
            group_base_ip = getIPRange(firstElement, cde); // <base IP network address,IP range BitSet>
            if (group_base_ip == null) {
                // We may still create a node (and interface), even without an IP
                // don't throw an exception, just log a message
                logger.warn("ModifyHandler::addNodeGroupElements():group_base_ip is null!");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        int hole = -1;
        HashMap<DomainElement, Integer> parentMap = null;
        String domainName = firstElement.getResourceType().getDomainURL();

        for (i = 0; i < units; i++) {
            logger.info("ModifyHandler::addNodeGroupElements():create new node from firstElement:i=" + i);

            DomainElement link_device = null;
            if (firstElement.getPrecededBy() != null && !firstElement.getPrecededBy().isEmpty()) {
                parentMap = new HashMap<DomainElement, Integer>();
                for (Entry<DomainElement, OntResource> parent : firstElement.getPrecededBySet()) {
                    link_device = parent.getKey();
                    logger.debug("ModifyHandler::addNodeGroupElements():parent link_device=" + link_device.getName());
                    if (link_device.getCe() != null) // node dependency
                        continue;
                    String ip_addr = null;
                    if (parent.getValue().getProperty(NdlCommons.layerLabelIdProperty) != null) {
                        ip_addr = parent.getValue().getProperty(NdlCommons.layerLabelIdProperty).getString();
                        hole = findIPRangeHole(group_base_ip, ip_addr);
                    } else {
                        // IP Addresses not assigned, but still need to create Node and Interface
                        logger.info("ModifyHandler::addNodeGroupElements():No IP Address assigned, Node and Interface will still be created.");
                    }
                    parentMap.put(link_device, hole);

                }
                DomainElement edge_device = null;
                for (Entry<DomainElement, Integer> entry : parentMap.entrySet()) {
                    link_device = entry.getKey();
                    hole = entry.getValue();
                    if (edge_device == null) {
                        edge_device = createNewNodeGroupNode(firstElement, hole, link_device, domainName,
                                manifestOntModel, requestModel, deviceList);
                    }
                    createInterface(firstElement.getCe(), edge_device, hole, link_device);
                }
            } else {
                logger.debug("ModifyHandler::addNodeGroupElements():firstElement has no parent! No interface will be created.");
                createNewNodeGroupNode(firstElement, -1, null, domainName, manifestOntModel, requestModel, deviceList);
            }
        }

        for (i = 0; i < addedDevices.size(); i++) {
            NetworkElement ne = addedDevices.get(i);
            // ne.setModifyVersion(this.modifyVersion);
            cde.add((DomainElement) ne);
            logger.debug("ModifyHandler::addNodeGroupElements():added:" + i + ":" + ne.getURI() + ":" + ne.getName());
        }

        return error;
    }

    protected DomainElement createNewNodeGroupNode(DomainElement element, int hole, DomainElement link_device,
            String domainName, OntModel manifestModel, OntModel requestModel, LinkedList<NetworkElement> deviceList)
            throws UnknownHostException, InetNetworkException {

        ComputeElement ce = null, element_ce = null;

        logger.info("ModifyHandler::createNewNodeGroupNode():Creating new node:hole=" + hole + "domainName=" + domainName);
        DomainResourceType dType = new DomainResourceType(element.getResourceType().getResourceType(),
                element.getResourceType().getCount());
        dType.setDomainURL(domainName);
        dType.setCount(1);
        int index = element.getName().lastIndexOf("/");
        String name = index >= 0 ? element.getName().substring(0, index) : element.getName();
        if (hole >= 0)
            name = name + ("/") + String.valueOf(hole) + "/" + Math.abs(UUID.randomUUID().toString().hashCode());
        else
            name = name + "/NoIP/" + UUID.randomUUID().toString();
        element_ce = element.getCe();
        // String url=element.getURI();
        String url = name;
        ce = element_ce.partialCopy(manifestModel, requestModel, url, name);
        
        OntResource ce_ont = ce.getModel().createOntResource(name);
        if (element.getResource() != null && element.getResource().hasProperty(NdlCommons.specificCEProperty))
            ce_ont.addProperty(NdlCommons.specificCEProperty,
                    element.getResource().getProperty(NdlCommons.specificCEProperty).getResource());
        ce.setResourceType(dType);

        DomainElement edge_device = new DomainElement(manifestModel, url, name);
        edge_device.setCe(ce);
        edge_device.setResourceType(dType);
        edge_device.setModifyVersion(edge_device.getModifyVersion() + 1);

        deviceList.add(edge_device);
        modifies.addAddedElement(edge_device.getResource());
        addedDevices.add(edge_device);

        if (logger.isTraceEnabled()) {
            logger.trace("ModifyHandler::createNewNodeGroupNode():Created edge_device " + edge_device);
        }

        return edge_device;
    }

    protected SystemNativeError removeElement(ModifyElement me, OntModel manifestOntModel,
            HashMap<String, Collection<DomainElement>> nodeGroupMap, HashMap<String, DomainElement> firstGroupElement,
            LinkedList<NetworkElement> deviceList) {
        SystemNativeError error = null;
        Iterator<NetworkElement> bei = deviceList.iterator();
        NetworkElement device = null;
        boolean remove = false;
        while (bei.hasNext()) {
            device = (NetworkElement) bei.next();
            if (device.getURI().equals(me.getObj().getURI()) || device.getName().equals(me.getObj().getURI())) {
                logger.info("ModifyHandler::removeElement():Remove Device name=" + device.getName() + ";url=" + device.getURI() + ";obj url="
                        + me.getObj().getURI());
                remove = true;
                break;
            }
        }

        if (remove != true) {
            // error = new SystemNativeError();
            // error.setErrno(7);
            // error.setMessage("Removed element doesn't exist: name: "+device.getName()+";url=" + device.getURI());
            logger.error("ModifyHandler::removeElement():Removed element doesn't exist: name: " + device.getName() + ";url=" + device.getURI());
            return error;
        }

        DomainElement de = (DomainElement) device;
        String name = null;
        OntResource device_ont = null;
        // clear dependency if any
        HashMap<DomainElement, OntResource> preds = de.getPrecededBy();
        DomainElement pe = null;
        LinkedList<DomainElement> pes = new LinkedList<DomainElement>();

        if (preds != null) {
            for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
                pe = parent.getKey();
                pes.add(pe);
            }
            Iterator<DomainElement> pes_it = pes.iterator();
            while (pes_it.hasNext()) {
                pe = pes_it.next();
                pe.removeFollowedByElement(de);
                de.removePrecededByElement(pe);
            }
            pes.clear();
        }
        preds = de.getFollowedBy();
        if (preds != null) {
            for (Entry<DomainElement, OntResource> parent : de.getFollowedBySet()) {
                pe = parent.getKey();
                pes.add(pe);
            }
            Iterator<DomainElement> pes_it = pes.iterator();
            while (pes_it.hasNext()) {
                pe = pes_it.next();
                if (pe.getCe() == null) { // if follower not a node, remove dependency
                    pe.removePrecededByElement(de);
                    de.removeFollowedByElement(pe);
                } else {
                    name = pe.getName();
                    device_ont = manifestOntModel.getOntResource(name);
                    modifies.addModifedRemoveElement(device_ont);
                }
            }
            pes.clear();
        }

        if (de.getCe() != null) { // remove a node from its group
            String n = me.getSub().getURI();
            int i = n.lastIndexOf("#");
            String group = i >= 0 ? n.substring(i + 1) : n;
            Collection<DomainElement> cde = nodeGroupMap.get(group);
            if (cde != null)
                cde.remove(device);
        }

        // remove from domainInConnectionList and deviceList

        name = device.getName();
        device_ont = manifestOntModel.getOntResource(name);
        boolean inDomainList = false;
        if (device_ont != null) {
            logger.debug("ModifyHandler::removeElement():Modify remove: device_ont:" + device_ont.getURI() + ";device domain="
                    + device_ont.getProperty(NdlCommons.hasURLProperty));
            inDomainList = this.domainInConnectionList.remove(device_ont);
        } else
            logger.error("ModifyHandler::removeElement():Modify remove:Not in the domainInConnectionList:" + name);
        boolean inDeviceList = deviceList.remove(device);
        if (!inDeviceList) {
            // error = new SystemNativeError();
            // error.setErrno(7);
            // error.setMessage("Removed element doesn't exist: name: "+device_s.getName()+";url=" + device_s.getURI());
            logger.error("ModifyHandler::removeElement():Removed device doesn't exist: name: " + device.getName() + ";url=" + device.getURI());
        } else {
            logger.debug("ModifyHandler::removeElement():to be removed ont:" + device_ont.getURI());
            modifies.addRemovedElement(device_ont);
        }
        // remove from
        String domainName = device.getInDomain();
        LinkedList<Device> domainList = this.domainConnectionList.get(domainName);
        boolean inDomainConnectionList = false;
        if (domainList != null) // CloudHandler
            inDomainConnectionList = domainList.remove(device);
        else {
            for (LinkedList<Device> list : this.domainConnectionList.values()) {
                inDomainConnectionList = list.remove(device);
            }
        }
        logger.debug("ModifyHandler::removeElement():Modify remove:" + domainName + ":inDomainConnectionList=" + inDomainConnectionList);
        if (!inDomainConnectionList) {
            // error = new SystemNativeError();
            // error.setErrno(7);
            // error.setMessage("Removed device doesn't exist in domainConnectionList: name:
            // "+device_s.getName()+";url=" + device_s.getURI());
            logger.error("ModifyHandler::removeElement():Removed device doesn't exist in domainConnectionList: name: " + device.getName() + ";url="
                    + device.getURI());
        }
        // close reservation and modify manifest will be done in ReservationConverter in the controller.

        // we need to check if we are removing the firstGroupElement, and if so, reassign it
        final String uri = device.getURI();
        final int i = uri.lastIndexOf("#");
        final String group = i >= 0 ? uri.substring(i + 1) : uri;
        if (null != firstGroupElement && device.equals(firstGroupElement.get(group))) {
            // find a replacement
            final Collection<DomainElement> domainElements = nodeGroupMap.get(group);
            final DomainElement next = domainElements.iterator().next();
            firstGroupElement.put(group, next);
        }

        return error;
    }

    public OntModel createManifest(Collection<NetworkElement> boundElements, RequestReservation request, String userDN,
            String controller_url, String sliceId) {

        /*
         * OntModelSpec s = NdlModel.getOntModelSpec(OntModelSpec.OWL_MEM, true); //OntModel manifestModel =
         * ModelFactory.createOntologyModel(s); OntModel manifestModel = null; try { manifestModel =
         * NdlModel.createModel(s, true, NdlModel.ModelType.TdbPersistent, Globals.TdbPersistentDirectory +
         * Globals.PathSep + "controller" + Globals.PathSep + "manifest-" + sliceId); } catch (NdlException e1) {
         * logger.error("ModifyHandler:ModifyHandler.createManifest(): Unable to create a persistent model of manifest"); }
         * 
         * manifestModel.add(request.getModel().getBaseModel());
         */
        // top level manifest resource
        Resource reservation_rs = request.getReservation_rs();

        Individual manifest = manifestModel.createIndividual(reservation_rs.getNameSpace() + "manifest",
                NdlCommons.manifestOntClass);
        if (controller_url != null) {
            // needed to randomize the name of the controller individual not to collide with nodes or links named
            // 'controller' /ib 10/18/16
            Individual controller = manifestModel.createIndividual(
                    reservation_rs.getNameSpace() + UUID.randomUUID().toString() + "-orca-controller",
                    NdlCommons.domainControllerClass);
            controller.addProperty(NdlCommons.hasURLProperty, controller_url);
            manifest.addProperty(NdlCommons.domainHasController, controller);
        }
        // add user DN
        if (userDN != null)
            manifest.addProperty(NdlCommons.hasDNProperty, userDN, XSDDatatype.XSDstring);

        manifestModel = createManifest(request, manifestModel, manifest);
        return manifestModel;
    }

    public OntModel createManifest(RequestReservation request, OntModel manifestModel, OntResource manifest) {
        if (request == null){
            return null;
        }
        String domain, connectionName;
        RequestReservation rr;
        HashMap<String, RequestReservation> dRR = request.getDomainRequestReservation();
        Collection<NetworkElement> elements;
        NetworkElement element;
        if (dRR == null) {
            logger.error("ModifyHandler::createManifest(r):No RequestReservation:");
            return null;
        }
        LinkedList<Device> domainList = null;
        for (Entry<String, RequestReservation> entry : dRR.entrySet()) {
            domain = entry.getKey();
            rr = entry.getValue();
            elements = rr.getElements();
            Iterator<Entry<String, LinkedList<Device>>> domainConnectionListIt = domainConnectionList.entrySet()
                    .iterator();
            while (domainConnectionListIt.hasNext()) {
                Entry<String, LinkedList<Device>> domainEntry = domainConnectionListIt.next();
                connectionName = domainEntry.getKey();
                domainList = domainEntry.getValue();
                logger.info("ModifyHandler::createManifest(r):connectionName=" + connectionName + " ;num hops=" + domainList.size()
                        + ";request Domain:" + domain);

                if (domain.equals(RequestReservation.Interdomain_Domain)
                        || domain.equals(RequestReservation.MultiPoint_Domain)) {
                    Iterator<NetworkElement> elementIt = elements.iterator();
                    while (elementIt.hasNext()) {
                        element = elementIt.next();

                        // #162 make sure the element has not been Closed before trying to add it to the manifest
                        if (!element.getModel().isClosed() && (element.getName().equals(connectionName)
                                || element.getURI().equals(connectionName))) {
                            logger.info("ModifyHandler::createManifest(r):InterDomain:element url =" + element.getURI()
                                    + ";isModify=" + element.isModify());
                            createManifest(element, manifestModel, manifest, domainList, request.getElements());
                        }
                    }

                } else if (domain.equals(RequestReservation.Unbound_Domain)) {
                    if (rr.getReservationDomain() != null) {
                        if (rr.getReservationDomain().contains(connectionName)) {
                            logger.info("ModifyHandler::createManifest(r):unbound connection name =" + connectionName);
                            createManifest(manifestModel, manifest, domainList);
                        } else {
                            Iterator<NetworkElement> elementIt = elements.iterator();
                            while (elementIt.hasNext()) {
                                element = elementIt.next();

                                if (element.getName().equals(connectionName)) {
                                    logger.debug("ModifyHandler::createManifest(r):unbound: element name=" + element.getName()
                                            + ":connectionName=" + connectionName);
                                    createManifest(element, manifestModel, manifest, domainList, request.getElements());
                                }
                            }
                        }
                    }

                } else { // @cloud
                    if (domain.equals(connectionName)) {
                        logger.info("ModifyHandler::createManifest(r):connection name =" + connectionName);
                        createManifest(manifestModel, manifest, domainList);
                    }
                }

            }
        }

        // TDB.sync(manifestModel);
        return manifestModel;
    }

    public ModifyReservations getModifies() {
        return modifies;
    }

    public LinkedList<Device> getAddedDevices() {
        return addedDevices;
    }

    public LinkedList<Device> getModifiedDevices() {
        return modifiedDevices;
    }
}
