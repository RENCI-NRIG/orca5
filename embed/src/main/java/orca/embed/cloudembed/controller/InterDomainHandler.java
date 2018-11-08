
package orca.embed.cloudembed.controller;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import orca.embed.cloudembed.IConnectionManager;
import orca.embed.cloudembed.RequestMapping;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.Interface;
import orca.ndl.elements.LabelSet;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.ndl.elements.SwitchingAction;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class InterDomainHandler extends CloudHandler implements LayerConstant {
    protected RequestMapping mapper;
    public boolean cloudRequest = false, interDomainRequest = true, multipointRequest = false;
    HashMap<String, LinkedList<Device>> connectionList = new HashMap<String, LinkedList<Device>>();
    boolean stitching = false;

    public InterDomainHandler(IConnectionManager icm) throws NdlException {
        super();
        mapper = (RequestMapping) icm;
    }

    public InterDomainHandler() throws NdlException {
        super();
    }

    /**
     * Create handler with in-memory model
     * 
     * @param substrateFile substrateFile
     * @throws IOException in case of error
     * @throws NdlException in case of error
     */
    public InterDomainHandler(String substrateFile) throws IOException, NdlException {
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
    public InterDomainHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
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
    public InterDomainHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
        super(tdbPrefix, recover);
    }

    // Interdomain path computation
    @SuppressWarnings("unchecked")
    public SystemNativeError runEmbedding(RequestReservation rr, DomainResourcePools domainResourcePools)
            throws IOException {

        OntModel requestModel = rr.getModel();

        mapper = new RequestMapping(requestModel, this.idm, true);
        LinkedList<Device> domainList = null;
        SystemNativeError error = null;

        // String fileName = "/home/geni-orca/workspace-orca5/orca5/stitch" + "-subrequest.rdf";
        // OutputStream fsw = new FileOutputStream(fileName);
        // requestModel.write(fsw);

        // check all elements, modifying the request for any that are stitching
        stitching = checkStitching(rr.getElements(), requestModel);
        logger.debug("InterDomainHandler::runEmbedding:is stitching=" + stitching + ";isModify=" + isModify);

        // add the requestModel only once
        if (stitching) {
            this.idm.add(requestModel);
        }
        /*
         * String homeDir = PathGuesser.getOrcaControllerHome(); String fileName = "/logs/idm.rdf"; OutputStream fsw =
         * new FileOutputStream(homeDir + fileName); idm.write(fsw); fsw.close();
         */

        for (NetworkElement element : rr.getElements()) {
            if (!(element instanceof NetworkConnection)) {
                continue;
            }

            NetworkConnection networkConnection = (NetworkConnection) element;

            logger.debug("InterDomainHandler::runEmbedding: connection:" + networkConnection.getName() + ";"
                    + networkConnection.getNe1().getName() + ":" + networkConnection.getNe1().getInDomain() + ":"
                    + networkConnection.getNe2().getName() + ":" + networkConnection.getNe2().getInDomain());
            if ((networkConnection.getNe1() == null) || (networkConnection.getNe2() == null)) {
                logger.error("InterDomainHandler::runEmbedding: This request connection misses the end point(s):nc=" 
                        + networkConnection.getName()
                        + ":ne1=" + networkConnection.getNe1() + ";ne2=" + networkConnection.getNe2());
                continue;
            }
            if ((networkConnection.getNe1().getInDomain() == null)
                    || (networkConnection.getNe2().getInDomain() == null)) {
                logger.error("InterDomainHandler::runEmbedding: This request connection misses the end point(s):nc=" 
                        + networkConnection.getName()
                        + ":ne1=" + networkConnection.getNe1().getInDomain() + ";ne2="
                        + networkConnection.getNe2().getInDomain());
                continue;
            }
            if (networkConnection.getNe1().getInDomain().equals(networkConnection.getNe2().getInDomain())) {
                RequestReservation intra_request = generateConnectionRequest(requestModel, networkConnection,
                        rr.getTerm(), rr.getReservationDomain(), rr.getReservation(), rr.getReservation_rs());
                runEmbedding(networkConnection.getNe1().getInDomain(), intra_request, domainResourcePools);
                continue;
            }

            error = mapper.createConnection(networkConnection, interDomainRequest, false,
                    networkConnection.getOpenflowCapable());
            if (error != null)
                break;
            NetworkConnection deviceConnection = mapper.getDeviceConnection();
            domainList = (LinkedList<Device>) deviceConnection.getConnection();
            DomainElement de = null;
            String domainName = null;
            DomainResourceType dType = null;
            int resourceCount = 0;
            boolean isStitchingDomain = false, isMulticast = false;
            for (int i = 0; i < domainList.size(); i++) {
                resourceCount = 0;
                de = (DomainElement) domainList.get(i);
                domainName = de.getInDomain();
                dType = domainResourcePools.getDomainResourceType(domainName);
                isStitchingDomain = NdlCommons.isStitchingNodeInManifest(de.getResource());

                if (isStitchingDomain){
                    resourceCount = 1;
                }
                else if (de.getCe() == null) { // edge resources counted in cloudHandler
                    resourceCount = resourceCount(de, dType);
                }
                if (resourceCount < 0) {
                    error = new SystemNativeError();
                    error.setErrno(1);
                    error.setMessage(
                            "Insufficient resources or Unknown domain: " + domainName + ":" + resourceCount + "!");
                    return error;
                }
            }
            error = domainDepend(domainList, deviceList, networkConnection, rr); // 1. dependency computation; 2.
                                                                                 // forming the deviceList
            if (error != null)
                break;

            if (domainList.size() > 1) {
                DomainElement source = (DomainElement) domainList.get(0);
                DomainElement source_link = (DomainElement) domainList.get(1);

                error = processEdges(source, source_link, rr, networkConnection, requestModel, domainResourcePools);
                if (error != null) {
                    break;
                }

                DomainElement dest = (DomainElement) domainList.get(domainList.size() - 1);
                DomainElement dest_link = (DomainElement) domainList.get(domainList.size() - 2);

                error = processEdges(dest, dest_link, rr, networkConnection, requestModel, domainResourcePools);
                if (error != null) {
                    break;
                }
            }
            domainConnectionList.put(networkConnection.getName(), domainList);
        }
        return error;
    }

    /**
     * processes the special cases of different edge nodes in the request: stitching, MP, ng, etc..
     *
     * @param domainElement domainElement
     * @param domainElementLink domainElementLink
     * @param requestReservation requestReservation
     * @param networkConnection networkConnection
     * @param requestModel requestModel
     * @param domainResourcePools domainResourcePools
     * @return SystemNativeError
     */
    protected SystemNativeError processEdges(DomainElement domainElement, DomainElement domainElementLink,
            RequestReservation requestReservation, NetworkConnection networkConnection, OntModel requestModel,
            DomainResourcePools domainResourcePools) {
        boolean isStitchingDomain = NdlCommons.isStitchingNodeInManifest(domainElement.getResource());
        boolean isMulticast;
        SystemNativeError error = null;

        if (isStitchingDomain) {
            domainElement.setAllocatable(false);
        } else {
            isMulticast = domainElement.getCastType() != null
                    && domainElement.getCastType().equalsIgnoreCase(NdlCommons.multicast);

            if ((!isMulticast) && (domainElement.getNumUnits() > 0 || domainElement.getCe().getCeGroup() != null)) {
                RequestReservation edgeRequest = generateEdgeRequest(domainElement.getInDomain(), domainElement,
                        domainElementLink, networkConnection, requestReservation.getTerm(), requestModel);
                edgeRequest.setOri_reservationDomain(requestReservation.getReservationDomain());
                error = runEmbedding(domainElement.getInDomain(), edgeRequest, domainResourcePools);
                if (error != null)
                    return error;
                if (domainElement.getCe().getGroup() != null || (domainElement.getCe().getCeGroup() != null
                        && domainElement.getCe().getCeGroup().size() > 1)) {
                    // if(dest.getCe().getGroup()!=null){
                    if (domainElementLink.getFollowedBy() != null)
                        domainElementLink.getFollowedBy().remove(domainElement);
                    deviceList.remove(domainElement);
                }
            }
        }

        return null;
    }

    /**
     *
     * @param elements elements
     * @param requestModel requestModel
     * @return true or false
     */
    protected boolean checkStitching(Collection<NetworkElement> elements, OntModel requestModel) {
        boolean isStitching = false;

        for (NetworkElement element : elements) {
            if (element instanceof NetworkConnection) {
                if (checkStitching((NetworkConnection) element, requestModel)) {
                    isStitching = true;
                }
            }
        }

        return isStitching;
    }

    protected boolean checkStitching(NetworkConnection element, OntModel requestModel) {
        Resource layer_rs = null;
        if (element.getResource().hasProperty(NdlCommons.atLayer))
            layer_rs = element.getResource().getProperty(NdlCommons.atLayer).getResource();
        NetworkElement stitchingNode = null;
        boolean stitching = false;
        if (NdlCommons.isStitchingNode(element.getNe1().getResource())) {
            stitchingNode = element.getNe1();
            stitching = modifyRequestModel(stitchingNode, layer_rs, requestModel);
        }

        if (NdlCommons.isStitchingNode(element.getNe2().getResource())) {
            stitchingNode = element.getNe2();
            stitching = modifyRequestModel(stitchingNode, layer_rs, requestModel);
        }
        return stitching;
    }

    protected boolean modifyRequestModel(NetworkElement node, Resource layer, OntModel requestModel) {
        OntResource stitchingNode = node.getResource();
        if (!stitchingNode.hasProperty(NdlCommons.topologyHasInterfaceProperty))
            return false;
        Resource domain_rs = stitchingNode.getProperty(NdlCommons.inDomainProperty).getResource();
        if (domain_rs.equals(NdlCommons.stitchingDomain)) {
            String domain_url = NdlCommons.ORCA_NS + "orca.rdf#" + UUID.randomUUID().toString() + "/"
                    + NdlCommons.stitching_domain_str;
            domain_rs = requestModel.createResource(domain_url, NdlCommons.deviceOntClass);
        }
        stitchingNode.removeAll(NdlCommons.inDomainProperty);
        stitchingNode.addProperty(NdlCommons.inDomainProperty, domain_rs);
        if (stitchingNode.hasProperty(NdlCommons.inDomainProperty)) {
            logger.info("InterDomainHandler::modifyRequestModel:stitching node:" 
                        + stitchingNode.getProperty(NdlCommons.inDomainProperty));
        }
        domain_rs.addProperty(NdlCommons.inDomainProperty, domain_rs);
        node.setInDomain(domain_rs.getURI());

        Resource intf_rs = stitchingNode.getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
        Resource shadow_intf_rs = requestModel.createIndividual(domain_rs.getURI() + "/intf",
                NdlCommons.interfaceOntClass);
        if (layer == null)
            layer = NdlCommons.ethernetNetworkElementClass;
        shadow_intf_rs.addProperty(NdlCommons.atLayer, layer);
        // @kthar10 10/31/2018; commenting this  statement as it is needed later so that interfaces can be copied to
        // StitchPort object in the manifest construction in case of modify
        // Needed for a fix for Issue# 223
        //requestModel.remove(stitchingNode.getProperty(NdlCommons.topologyHasInterfaceProperty));
        Statement statement = null;
        HashSet<Statement> p_set = (HashSet<Statement>) intf_rs.listProperties().toSet();
        for (Iterator<Statement> j = p_set.iterator(); j.hasNext();) {
            statement = (Statement) j.next();
            shadow_intf_rs.addProperty(statement.getPredicate(), statement.getObject());
        }

        ResultSet results = NdlCommons.getLayerAdapatationOf(requestModel, intf_rs.getURI());
        String varName = (String) results.getResultVars().get(0);
        Resource intf_rs_base = null, device_rs = null;
        if (results.hasNext()) {
            intf_rs_base = results.nextSolution().getResource(varName);
        }
        Resource idm_intf_rs = null;
        if (intf_rs_base == null) {
            String intf_str = intf_rs.getURI();
            int index = intf_str.lastIndexOf('/');
            String intf_base_str = intf_str.substring(0, index);
            if (intf_base_str != null)
                idm_intf_rs = idm.getResource(intf_base_str);
        } else
            idm_intf_rs = idm.getResource(intf_rs_base.getURI());
        String stitching_intf_label = null;
        if (idm_intf_rs != null) {
            if (idm_intf_rs.getProperty(NdlCommons.topologyHasName) != null) {
                String stitching_domain_name = idm_intf_rs.getProperty(NdlCommons.topologyHasName).getString();
                domain_rs.addProperty(NdlCommons.topologyHasName, stitching_domain_name);
            } else {
                logger.error("InterDomainHandler::modifyRequestModel:Stitching interface doesn't specify the " + 
                             "neighboring domain name, may fail the tag mapping!"
                                + idm_intf_rs.getURI());
            }
            if (idm_intf_rs.getProperty(NdlCommons.RDFS_Label) != null) {
                stitching_intf_label = idm_intf_rs.getProperty(NdlCommons.RDFS_Label).getString();
            } else {
                logger.error("InterDomainHandler::modifyRequestModel:Stitching interface doesn't specify the label!" 
                            + idm_intf_rs.getURI());
            }
        }
        domain_rs.addProperty(NdlCommons.topologyHasInterfaceProperty, shadow_intf_rs);
        domain_rs.addProperty(NdlCommons.RDF_TYPE, NdlCommons.deviceOntClass);
        shadow_intf_rs.addProperty(NdlCommons.topologyInterfaceOfProperty, domain_rs);
        shadow_intf_rs.addProperty(NdlCommons.linkTo, intf_rs);
        intf_rs.addProperty(NdlCommons.linkTo, shadow_intf_rs);
        if (intf_rs_base != null) {
            p_set = (HashSet<Statement>) idm_intf_rs.listProperties(NdlCommons.topologyInterfaceOfProperty).toSet();
            for (Iterator<Statement> j = p_set.iterator(); j.hasNext();) {
                statement = (Statement) j.next();
                device_rs = statement.getResource();
                intf_rs.addProperty(statement.getPredicate(), device_rs);
                requestModel.add(device_rs, NdlCommons.topologyHasInterfaceProperty, intf_rs);
            }
            if (stitching_intf_label != null)
                intf_rs.addProperty(NdlCommons.RDFS_Label, stitching_intf_label);
        }

        ExtendedIterator<Individual> res_it = requestModel.listIndividuals(NdlCommons.reservationOntClass);
        Individual res_ind = null;
        while (res_it.hasNext()) {
            res_ind = res_it.next();
            break;
        }

        res_ind.removeProperty(NdlCommons.collectionElementProperty, stitchingNode);
        res_ind.addProperty(NdlCommons.collectionElementProperty, domain_rs);
        /*
         * try { FileOutputStream ben_os = new FileOutputStream("stitch-shadow-request.rdf");
         * requestModel.write(ben_os); //System.exit(0); } catch (FileNotFoundException e) { // TODO Auto-generated
         * catch block e.printStackTrace(); }
         */
        return true;
    }

    public RequestReservation generateConnectionRequest(OntModel m, NetworkElement e, OrcaReservationTerm t,
            String reservationD, String r, Resource r_rs) {
        RequestReservation request = new RequestReservation();
        request.setRequest(m, e, t, reservationD, r, r_rs);
        return request;
    }

    protected RequestReservation generateEdgeRequest(String domainName, DomainElement de, DomainElement de_link,
            NetworkConnection rc, OrcaReservationTerm term, OntModel requestModel) {
        RequestReservation request = new RequestReservation();
        ComputeElement ce = de.getCe();
        NetworkConnection ce_link = new NetworkConnection(requestModel, rc.getURI(), rc.getName());
        ce_link.addConnection(de_link);
        ce_link.setClientConnection(rc);
        HashSet<NetworkElement> parents = (HashSet<NetworkElement>) ce.getDependencies();
        Collection<NetworkConnection> pl = new LinkedList<NetworkConnection>();
        for (Object pde : parents.toArray()) {// From InterDomain request
            if (pde instanceof NetworkConnection) {
                pl.add((NetworkConnection) pde);
            }
        }
        parents.removeAll(pl);

        ce.addDependency(ce_link);
        request.setRequest(requestModel, ce, term, domainName, request.getReservation(), request.getReservation_rs());

        return request;
    }

    public SystemNativeError domainDepend(LinkedList<Device> domainList, LinkedList<NetworkElement> dependList,
            NetworkConnection requestElement, RequestReservation rr) {
        SystemNativeError error = null;
        DomainElement start, next_Hop, root = null;
        start = (DomainElement) domainList.get(0);
        domainNoDepend(start);
        if (!start.isDepend()) {
            if (mapper.getDevice(start, dependList) == null) {
                dependList.add(start);
            } else {
                int degree = start.getDegree();
                start.setDegree(degree + 1);
            }
        }
        int path_len = domainList.size();
        logger.info("InterDomainHandler::domainDepend(dl):Beginning of Dependency:" + path_len);
        for (int i = 1; i < domainList.size(); i++) {
            next_Hop = (DomainElement) domainList.get(i);
            domainNoDepend(next_Hop);
            if ((i == 1) && (!start.isDepend())) {
                setModifyFlag(next_Hop);
                dependList.add(next_Hop);
            }
            if (start.isDepend() & next_Hop.isDepend()) {
                ComputeElement ce1 = (ComputeElement) requestElement.getNe1();
                ComputeElement ce2 = (ComputeElement) requestElement.getNe2();

                OntResource rs1_ont = null, rs2_ont = null, rs_ont = null;
                if (ce1.getInterfaceName(requestElement) != null) {
                    // NOTE: I'm assuming I'm getting hold of the right model in the getResource call /ib
                    rs_ont = ce1.getInterfaceName(requestElement).getLabel() == null ? null
                            : ce1.getInterfaceName(requestElement).getLabel()
                                    .getResource(ce1.getInterfaceName(requestElement).getModel());

                    rs1_ont = getCEOnt(rs_ont);
                }

                if (ce2.getInterfaceName(requestElement) != null) {
                    // NOTE: I'm assuming I'm getting hold of the right model in the getResource call /ib
                    rs_ont = ce2.getInterfaceName(requestElement).getLabel() == null ? null
                            : ce2.getInterfaceName(requestElement).getLabel()
                                    .getResource(ce2.getInterfaceName(requestElement).getModel());

                    rs2_ont = getCEOnt(rs_ont);
                }

                root = domainDepend(start, next_Hop, dependList, i, path_len, rs1_ont, rs2_ont);
                if (root == null) {
                    error = new SystemNativeError();
                    error.setErrno(1);
                    error.setMessage(
                            "Error in building the dependency tree, probably not available vlan path OR trying to reuse a stitching tag:"
                                    + rr.getReservation());
                }
            } else {
                setModifyFlag(next_Hop);
                dependList.add(next_Hop);
            }
            start = next_Hop;
        }
        // last domain
        if (!start.isDepend()) {
            if (mapper.getDevice(start, dependList) == null) {
                setModifyFlag(start);
                dependList.add(start);
            } else {
                int degree = start.getDegree();
                start.setDegree(degree + 1);
            }
        }
        logger.info("InterDomainHandler::domainDepend(dl):End of Dependency Computation! dependList size:" + 
                    dependList.size() + " ;domainList size:"
                    + domainList.size());
        return error;
    }

    // not covered all combinations yet: (1)label producer;(2) node degree.
    public DomainElement domainDepend(DomainElement start, DomainElement next, LinkedList<NetworkElement> dependList,
            int hop, int path_len, OntResource rs1_ont, OntResource rs2_ont) {
        int sSetSize = 0, nSetSize = 0;
        boolean flag = false, primaryDomain = false;
        DomainElement root = null;
        DomainResourceType sRType = start.getResourceType(), nRType = next.getResourceType();
        int sRank = 0, nRank = 0;
        if (sRType != null)
            sRank = sRType.getRank();
        if (nRType != null)
            nRank = nRType.getRank();
        logger.info("InterDomainHandler::domainDepend(de): " + start.getURI() + ":" + start.getLayerLabelPrimary() 
                + ":" + start.isLabelProducer() + ":" + sRType
                + ":" + sRank + ":" + start.getSwappingCapability());
        logger.info("InterDomainHandler::domainDepend(de): " + next.getURI() + ":" + next.getLayerLabelPrimary() 
                + ":" + next.isLabelProducer() + ":" + nRType
                + ":" + nRank + ":" + next.getSwappingCapability() + "\n");
        if (start.getLayerLabelPrimary() || next.getLayerLabelPrimary()) {
            primaryDomain = true;
            start.setLayerLabelPrimary(primaryDomain);
            next.setLayerLabelPrimary(primaryDomain);
        }
        if (start.isLabelProducer()) {
            if (next.isLabelProducer()) {
                if (sRank == nRank) {
                    if ((start.getTunnelingCapability() != null) || (next.getTunnelingCapability() != null)) {

                        if (start.getTunnelingCapability() != null) {
                            flag = true;
                        } else {
                            flag = false;
                            logger.info("InterDomainHandler::domainDepend(de): TunnelingCapability:" 
                                    + start.getTunnelingCapability() + ":"
                                    + next.getTunnelingCapability() + ":" + hop + ":" + path_len);
                            // find the common label for start and next+1
                            if (hop <= path_len - 3) {
                                DomainElement next_next = (DomainElement) this.mapper.getDeviceConnection()
                                        .getConnection().get(hop + 1);
                                int commonLabel = findCommonLabel(start, next_next);
                                if (commonLabel < 0) {
                                    logger.error("InterDomainHandler::domainDepend(de): No common label between domains:" 
                                                 + start.getURI() + ":" + next.getURI());
                                    return null;
                                }
                                start.setStaticLabel(commonLabel);
                                next_next.setStaticLabel(commonLabel);
                                logger.info("InterDomainHandler::domainDepend(de): " + start.getURI() + ":" 
                                        + next.getURI() + ":" + next_next.getURI()
                                        + "----Next Next Common Label:" + commonLabel);
                            }
                        }

                    } else {
                        if (start.getSwappingCapability() != null) {
                            if (next.getSwappingCapability() != null) {
                                if (start.getDegree() <= next.getDegree())
                                    flag = true;
                            } else {
                                String castType = next.getCastType();
                                if ((this.multipointRequest == true) && castType != null
                                        && castType.equalsIgnoreCase(NdlCommons.multicast)) {
                                    int commonLabel = findCommonLabel(start, next);
                                    if (commonLabel < 0) {
                                        logger.error("InterDomainHandler::domainDepend(de): No common label between domains:" 
                                                    + start.getURI() + ":"
                                                    + next.getURI());
                                        return null;
                                    }
                                    logger.info("InterDomainHandler::domainDepend(de): Found common label:" 
                                            + commonLabel + " between domains:"
                                            + start.getURI() + ":" + next.getURI());
                                    start.setStaticLabel(commonLabel);
                                    next.setStaticLabel(commonLabel);
                                    start.setNeedFollowerInterface(true);
                                    flag = false;
                                } else
                                    flag = true;
                            }
                        } else if (next.getSwappingCapability() != null) {
                            String castType = start.getCastType();
                            if ((this.multipointRequest == true) && castType != null
                                    && castType.equalsIgnoreCase(NdlCommons.multicast)) {
                                int commonLabel = findCommonLabel(start, next);
                                if (commonLabel < 0) {
                                    logger.error(
                                            "InterDomainHandler::domainDepend(de): No common label between domains:" + start.getURI() + ":" + next.getURI());
                                    return null;
                                }
                                logger.info("InterDomainHandler::domainDepend(de): Found common label:" + commonLabel + " between domains:" + start.getURI()
                                        + ":" + next.getURI());
                                next.setStaticLabel(commonLabel);
                                start.setStaticLabel(commonLabel);
                                next.setNeedFollowerInterface(true);
                                flag = true;
                            } else
                                flag = false;
                        } else { // both have no swappingCapability, check who has a specific available label range
                            LinkedList<LabelSet> sSetList = start.getLabelSet(sRType.getResourceType());
                            LinkedList<LabelSet> nSetList = next.getLabelSet(nRType.getResourceType());
                            for (LabelSet sSet : sSetList) {
                                sSetSize = sSetSize + sSet.getLabelRangeSize();
                            }
                            for (LabelSet nSet : nSetList) {
                                nSetSize = nSetSize + nSet.getLabelRangeSize();
                            }

                            logger.info("InterDomainHandler::domainDepend(de): LabelSet Size:" + start.getName() + ":" + sSetSize + "****" + next.getName()
                                    + ":" + nSetSize);
                            if (sSetSize == 0) {
                                if (nSetSize > 0) {
                                    start.setStaticLabel(-1);
                                    flag = true;
                                } else { // both == 0, needs to use the common label
                                    flag = true;
                                    start.setStaticLabel(-1);
                                    next.setStaticLabel(-1);
                                }
                            } else if (nSetSize == 0) {
                                flag = false;
                            } else { // both >0, needs to find the common label to use
                                int commonLabel = findCommonLabel(start, next);
                                if (commonLabel < 0) {
                                    logger.error(
                                            "InterDomainHandler::domainDepend(de): No common label between domains:" + start.getURI() + ":" + next.getURI());
                                    return null;
                                }
                                logger.info("InterDomainHandler::domainDepend(de): Found common label:" + commonLabel);
                                start.setStaticLabel(commonLabel);
                                next.setStaticLabel(commonLabel);
                                if (sSetSize > nSetSize)
                                    flag = true;
                                else
                                    flag = false;
                                if ((start.getCastType() != null)
                                        && (start.getCastType().equalsIgnoreCase(NdlCommons.multicast)))
                                    flag = true;
                                if ((next.getCastType() != null)
                                        && (next.getCastType().equalsIgnoreCase(NdlCommons.multicast)))
                                    flag = false;
                            }
                        }
                    }
                } else if (sRank < nRank) {
                    flag = false;
                } else
                    flag = true;
            } else
                flag = false;
        } else if (next.isLabelProducer()) {
            flag = true;
        }

        boolean isStitchingDomain = false;
        float stitching_tag = 0;
        isStitchingDomain = NdlCommons.isStitchingNodeInManifest(start.getResource());
        if (isStitchingDomain)
            flag = false;
        else {
            isStitchingDomain = NdlCommons.isStitchingNodeInManifest(next.getResource());

            if (isStitchingDomain)
                flag = true;
        }
        if (isStitchingDomain) {
            stitching_tag = stitchingTag(start, next, flag);
            logger.info("InterDomainHandler::domainDepend(de): Stitching tag=" + stitching_tag);
            if (stitching_tag <= 0)
                return null;
            else {
                if (flag == false) {
                    start.setStaticLabel(stitching_tag);
                    if (next.getSwappingCapability() == null)
                        next.setStaticLabel(stitching_tag);
                } else {
                    next.setStaticLabel(stitching_tag);
                    if (start.getSwappingCapability() == null)
                        start.setStaticLabel(stitching_tag);
                }
            }
        }

        Resource nextUpNeighbour = next.getUpNeighbour(next.getModel());
        Resource nextUpLocal = next.getUpLocal(next.getModel());
        Resource startDownNeighbour = start.getDownNeighbour(next.getModel());
        Resource startDownLocal = start.getDownLocal(next.getModel());

        DomainElement device = (DomainElement) mapper.getDevice(start, dependList);
        if ((device != null) && (!start.isModify()) && mapper.isCe(device)) {
            start = device;
        } else {
            if (hop == 1) {
                if ((device != null) && mapper.isCe(device)) {// start is modify, copy start to device
                    device.setURI(start.getURI());
                    device.copyDependency(start);
                    device.setModify(start.isModify());
                    device.setCe(start.getCe());
                    setModifyFlag(device);
                }
                setModifyFlag(start);
                dependList.add(start);

                logger.debug("InterDomainHandler::domainDepend(de): modified device, new start:" + start.getName());
            }
        }

        device = (DomainElement) mapper.getDevice(next, dependList);

        // if((device!=null) && (device.getResourceType().getResourceType().toLowerCase().endsWith("vm"))){
        if ((device != null) && (!next.isModify())) {
            String castType = device.getCastType();
            if (castType == null)
                castType = "";
            if ((mapper.isCe(device)) || (NdlCommons.isStitchingNodeInManifest(next.getResource()))
                    || (castType.equalsIgnoreCase(NdlCommons.multicast) && (hop == path_len - 1))) {
                device.setStaticLabel(next.getStaticLabel());
                next = device;
            } else {
                setModifyFlag(next);
                dependList.add(next);
            }
        } else {
            setModifyFlag(next);
            dependList.add(next);
            logger.debug("InterDomainHandler::domainDepend(de): modified device, new next:" + next.getName());
        }
        if (flag) {// start depends on next
            if ((hop == 1) && (rs1_ont != null)) { // First device should be the VM and check if IP is given from the
                                                   // request
                start.setPrecededBy(next, rs1_ont);
                next.setFollowedBy(start, rs1_ont);
            } else {
                start.setPrecededBy(next, nextUpNeighbour);
                next.setFollowedBy(start, startDownNeighbour);
                if (next.isNeedFollowerInterface() == true) {
                    logger.info("InterDomainHandler::domainDepend(de): Next's follower interface:" + nextUpNeighbour.getURI() + ":" + nextUpLocal.getURI()
                            + ":" + startDownNeighbour.getURI() + ":" + startDownLocal.getURI());

                    next.setFollowerInterface(nextUpLocal);
                }
            }
            root = next;
        } else {// next depends on start
            if ((hop == path_len - 1) && (rs2_ont != null)) { // Last device should be the VM and check if IP is given
                                                              // from the request
                next.setPrecededBy(start, rs2_ont);
                start.setFollowedBy(next, rs2_ont);
            } else {
                next.setPrecededBy(start, startDownNeighbour);
                start.setFollowedBy(next, nextUpNeighbour);
                if (start.isNeedFollowerInterface() == true) {
                    logger.info("InterDomainHandler::domainDepend(de): start's follower interface:" + nextUpNeighbour.getURI() + ":" + nextUpLocal.getURI()
                            + ":" + startDownNeighbour.getURI() + ":" + startDownLocal.getURI());
                    start.setFollowerInterface(startDownLocal);
                }
            }
            root = start;
        }
        if (primaryDomain == false) {
            setAssignedLabel(start);
            setAssignedLabel(next);
        }

        logger.info("InterDomainHandler::domainDepend(de): start:" + start.getURI() + ":" + start.getStaticLabel() + ";" + start.getSwappingCapability() + ";"
                + start.getName() + ":" + rs1_ont + ":flag=" + flag + "\n");
        logger.info("InterDomainHandler::domainDepend(de): next:" + next.getURI() + ":" + ":" + start.getStaticLabel() + ";" + next.getSwappingCapability()
                + ";" + next.getName() + ":" + rs2_ont + "\n");

        // System.out.println("flag:"+flag+"\n");
        return root;
    }

    public float stitchingTag(DomainElement start, DomainElement next, boolean flag) {
        DomainElement parent_de = flag ? next : start;
        DomainElement child_de = flag ? start : next;
        Resource intf = parent_de.getResource().getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
        String label_str = NdlCommons.getLayerLabelLiteral(intf);
        // boolean stitching=false;
        float label_id = 0;
        if (label_str != null) {
            label_id = 0;
            try {
                label_id = Float.valueOf(label_str);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("Unable to parse stitching tag " + label_str + " into a float for interface "
                        + intf + " of parent " + parent_de);
            }
            // parent_de.setStaticLabel(label_id);
            if (child_de.getSwappingCapability() == null) { // need to take the stitching tag
                if (isTagAvailable(child_de, label_id)) {
                    child_de.setStaticLabel(label_id);
                    stitching = true;
                } else {
                    logger.error("InterDomainHandler::stitchingTag: Stitching tag " + label_id + " is not available in domain:" + child_de.getURI());
                    label_id = 0;
                }
            } else { // need to check if the upstream stitching tag has been used in this stitching port
                if (!isTagAvailable(parent_de, label_id)) {
                    logger.error(
                            "InterDomainHandler::stitchingTag: Stitching tag " + label_id + " is not available anymore in domain:" + child_de.getURI());
                    label_id = 0;
                }

            }
        }
        return label_id;
    }

    // set controller assigned label
    public void setAssignedLabel(DomainElement de) {
        int label = (int) de.getStaticLabel();
        String domain = de.getURI();
        BitSet labelSet = null;
        if (controllerAssignedLabel.containsKey(domain)) {
            labelSet = controllerAssignedLabel.get(domain);
            if (label > 0)
                labelSet.set(label);
            logger.debug("InterDomainHandler::setAssignedLabel:existing domain=" + domain + ";label=" + label + ";labelSet=" + labelSet);
        } else {
            if (label > 0) {
                labelSet = new BitSet(max_vlan_tag);
                labelSet.set(label);
                // for stitching domain, use the stitching port as the index key
                boolean isStitchingDomain = NdlCommons.isStitchingNodeInManifest(de.getResource());
                if (isStitchingDomain) {
                    domain = getStitchingPort(de);
                }
                controllerAssignedLabel.put(domain, labelSet);
            }
            logger.debug("InterDomainHandler::setAssignedLabel:empty domain=" + domain + ";labelSet=" + labelSet);
        }
    }

    // get the stitching port
    String getStitchingPort(DomainElement de) {
        Resource shadow_intf_rs = de.getResource().getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
        Resource intf_rs = shadow_intf_rs.getProperty(NdlCommons.linkTo).getResource();
        String intf_rs_str = null;
        if (intf_rs != null)
            intf_rs_str = intf_rs.getURI();
        return intf_rs_str;
    }

    public BitSet getAssignedLabel(DomainElement de) {
        return controllerAssignedLabel.get(de.getURI());
    }

    public boolean isTagAvailable(DomainElement de, float tag) {
        BitSet set = getAvailableBitSet(de);
        // System.out.println("tag="+tag+";aSet="+set);
        if (set.get((int) tag))
            return true;
        else {
            if (de.getStaticLabel() == tag)
                return true;
            else
                return false;
        }
    }

    public BitSet getAvailableBitSet(DomainElement de) {
        BitSet startBitSet = null, controllerStartBitSet = null;
        String domain_str = de.getURI();
        boolean isStitchingDomain = NdlCommons.isStitchingNodeInManifest(de.getResource());
        if (isStitchingDomain) {
            domain_str = getStitchingPort(de);
        }

        if (this.globalControllerAssignedLabel != null) {
            startBitSet = this.globalControllerAssignedLabel.get(domain_str);
        }
        logger.debug("InterDomainHandler::getAvailableBitSet(): globalAssignedLabel(" + domain_str + ")=" + startBitSet);
        if (this.controllerAssignedLabel != null) {
            controllerStartBitSet = this.controllerAssignedLabel.get(domain_str);
        }
        logger.debug("InterDomainHandler::getAvailableBitSet():controllerAssignedLabel=" + controllerStartBitSet);

        BitSet sBitSet = new BitSet(max_vlan_tag);
        if (!isStitchingDomain) {
            DomainResourceType sRType = de.getResourceType();
            LinkedList<LabelSet> sSetList = de.getLabelSet(sRType.getResourceType());
            int min = 0, max = 0;
            for (LabelSet sSet : sSetList) {
                min = (int) sSet.getMinLabel_ID();
                max = (int) sSet.getMaxLabe_ID();
                if ((min == max) || (max == 0)) {
                    sBitSet.set(min);
                } else {
                    sBitSet.set(min, max + 1);
                }
                if ((min == 0) && (max == 0)) {
                    sBitSet.set(0, max_vlan_tag);
                }
                sBitSet.andNot(sSet.getUsedBitSet());
                logger.debug("InterDomainHandler::getAvailableBitSet():min-max-used:" + min + ":" + max + ":" + sSet.getUsedBitSet());
            }
        } else {
            sBitSet.set(0, max_vlan_tag);
        }
        logger.debug("InterDomainHandler::getAvailableBitSet():findLabelSet----domain=" + domain_str + ";initial Start labelSet:" + sBitSet);
        if (startBitSet != null)
            sBitSet.andNot(startBitSet);
        if (controllerStartBitSet != null)
            sBitSet.andNot(controllerStartBitSet);
        logger.debug("InterDomainHandler::getAvailableBitSet():Final labelSet:" + sBitSet);
        return sBitSet;
    }

    public int randomSetBit(BitSet bs) {
        int sb = 0, num = 0;
        int[] sb_array = new int[bs.size()];
        int first_sb = bs.nextSetBit(0);
        for (int i = first_sb; i >= 0; i = bs.nextSetBit(i + 1)) {
            sb_array[num] = i;
            num++;
        }

        sb = (int) (Math.random() * num);
        sb = sb_array[sb];

        return sb;
    }

    // If the VLAN is fixed in the interface, then no need for dependency
    public void domainNoDepend(Device domain) {
        String elementDomain = domain.getInDomain();
        boolean isStitchingDomain = elementDomain.contains(NdlCommons.stitching_domain_str) ? true : false;
        if (isStitchingDomain) {
            domain.setDepend(true);
            return;
        }

        int action_size = 0;
        SwitchingAction action = null;
        if (domain.getActionList() != null) { // another request??
            action_size = domain.getActionList().size();
            action = (SwitchingAction) domain.getActionList().get(action_size - 1);
        }
        Resource intf_rs = null;
        Resource vlan_rs = null;
        boolean depend = false;
        if (action != null) {
            for (Interface intf : action.getClientInterface()) {
                vlan_rs = null;
                intf_rs = intf.getResource();
                if (intf_rs.getProperty(NdlCommons.vlan) != null)
                    vlan_rs = intf_rs.getProperty(NdlCommons.vlan).getResource();
                // System.out.println(intf_rs+":"+vlan_rs+"\n");
                if (vlan_rs == null) {
                    depend = true;
                    break;
                }
            }
        }
        domain.setDepend(depend);
    }

    // add to the manifestModel
    public void createManifest(NetworkElement requestElement, OntModel manifestModel, OntResource manifest,
            LinkedList<Device> domainList, Collection<NetworkElement> elements) {
        logger.debug("InterDomainHandler::createManifest() IN");
        OntResource rc_ont = requestElement.getResource();

        if (requestElement.isModify()) {
            if (rc_ont == null){
                rc_ont = manifestModel.getOntResource(requestElement.getURI());
            }
            else {
                rc_ont = manifestModel.getOntResource(rc_ont.getURI());
            }
            if (requestElement.getCastType().equalsIgnoreCase("multicast")) { // go one level above
                for (StmtIterator j = manifest.listProperties(NdlCommons.collectionElementProperty); j.hasNext();) {
                    Resource nc = j.next().getResource();
                    for (StmtIterator i = nc.listProperties(NdlCommons.collectionItemProperty); i.hasNext();) {
                        Resource md = i.next().getResource();
                        if (md.getURI().equalsIgnoreCase(rc_ont.getURI())) {
                            rc_ont = manifestModel.getOntResource(nc);
                            logger.debug("InterDomainHandler::createManifest(): modify; fetch3 multicast " + rc_ont);
                            break;
                        }

                    }
                }
            }
            logger.debug("InterDomainHandler::createManifest(): modifying Resource=" + rc_ont);
        } else {

            logger.debug("InterDomainHandler::createManifest(): added Resource=" + rc_ont);
            rc_ont = manifestModel.createIndividual(rc_ont.getURI(), NdlCommons.topologyNetworkConnectionClass);

            // This happens only for modify request
            // Fix for Issue#223
            logger.debug("InterDomainHandler::createManifest(): checking if Resource was copied from request Resource=" + requestElement.getURI());
            if(rc_ont.getProperty(NdlCommons.hasGUIDProperty) == null) {
                logger.debug("InterfaceDomainHandler::createManifest(): copying resource from request"); 
                OntResource r = requestElement.getResource();
                for (StmtIterator itr = r.listProperties(NdlCommons.topologyHasInterfaceProperty); itr.hasNext();) {
                    Statement s = itr.next();
                    logger.debug("InterfaceDomainHandler::createManifest(): hasInterface " + s.getObject());
                    rc_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, s.getObject());
                }
                for (StmtIterator itr = r.listProperties(NdlCommons.atLayer); itr.hasNext();) {
                    Statement s = itr.next();
                    logger.debug("InterfaceDomainHandler::createManifest(): atLayer " + s.getObject());
                    rc_ont.addProperty(NdlCommons.atLayer, s.getObject());
                }
                for (StmtIterator itr = r.listProperties(NdlCommons.layerBandwidthProperty); itr.hasNext();) {
                    Statement s = itr.next();
                    logger.debug("InterfaceDomainHandler::createManifest(): bandwidth " + s.getObject());
                    rc_ont.addProperty(NdlCommons.layerBandwidthProperty, s.getObject());
                }
                for (StmtIterator itr = r.listProperties(NdlCommons.hasGUIDProperty); itr.hasNext();) {
                    Statement s = itr.next();
                    logger.debug("InterfaceDomainHandler::createManifest(): hasGUID " + s.getObject());
                    rc_ont.addProperty(NdlCommons.hasGUIDProperty, s.getObject());
                }
            }

            manifest.addProperty(NdlCommons.collectionElementProperty, rc_ont);
        }
        Device start = domainList.get(0), next_Hop, next_next_Hop = null;
        String link_url, domain_name;
        OntResource link_ont, intf_start, intf_next, intf_next_next;
        Resource domain_rs = null;

        for (int i = 1; i < domainList.size(); i++) {
            next_Hop = domainList.get(i);
            intf_start = start.getDownNeighbour(start.getModel());
            intf_next = next_Hop.getUpNeighbour(next_Hop.getModel());
            logger.info("InterDomainHandler::createManifest():link connections..." + i + ":" + domainList.size() + ":start url:" + start.getURI()
                    + ";next_hop:" + next_Hop.getURI());

            // source domain
            if (i == 1) {
                addEdgeDeviceToTopology(manifestModel, rc_ont, start, intf_next, elements);
            }

            // Link connection
            link_url = intf_start.getURI() + "-" + intf_next.getURI().split("\\#")[1] + "/"
                    + UUID.randomUUID().toString();
            link_ont = manifestModel.createIndividual(link_url, NdlCommons.topologyLinkConnectionClass);
            link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, intf_start);
            link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, intf_next);
            intf_start.addProperty(NdlCommons.RDF_TYPE, NdlCommons.interfaceOntClass);
            intf_next.addProperty(NdlCommons.RDF_TYPE, NdlCommons.interfaceOntClass);
            rc_ont.addProperty(NdlCommons.collectionItemProperty, link_ont);
            logger.debug("InterDomainHandler::createManifest():Link hop:" + link_ont.getURI() + "|");

            // crossconnect connection
            if (i < domainList.size() - 1) {
                next_next_Hop = domainList.get(i + 1);
                intf_next_next = next_next_Hop.getUpNeighbour(next_next_Hop.getModel());
                link_url = next_Hop.getURI() + "/" + UUID.randomUUID().toString() + "/vlan";
                link_ont = manifestModel.createIndividual(link_url, NdlCommons.topologyCrossConnectClass);
                next_Hop.setName(link_url);
                link_ont.addProperty(NdlCommons.hasURLProperty, next_Hop.getName());
                link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, intf_start);
                link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, intf_next_next);
                if (!link_ont.hasProperty(NdlCommons.inDomainProperty)) {
                    domain_rs = manifestModel.createResource(next_Hop.getResourceType().getDomainURL());
                    link_ont.addProperty(NdlCommons.inDomainProperty, domain_rs);
                    addDomainProperty(domain_rs, manifestModel);
                }
                if (next_Hop.getResourceType().getTypeResource() != null)
                    link_ont.addProperty(NdlCommons.domainHasResourceTypeProperty,
                            next_Hop.getResourceType().getTypeResource());
                link_ont.addProperty(NdlCommons.inConnection, "true", XSDDatatype.XSDboolean);
                link_ont.addProperty(NdlCommons.inRequestNetworkConnection, rc_ont);
                rc_ont.addProperty(NdlCommons.collectionItemProperty, link_ont);

                domainInConnectionList.add(link_ont);

                logger.info("InterDomainHandler::createManifest():Link Crossconnect url=:" + link_ont.getURI() + ";d.name= " + next_Hop.getName()
                        + ";d.inDomain=" + link_ont.getProperty(NdlCommons.inDomainProperty));
            }

            // destination domain
            if (i == domainList.size() - 1) {
                addEdgeDeviceToTopology(manifestModel, rc_ont, next_Hop, intf_start, elements);
            }

            start = next_Hop;
        }
        logger.debug("InterDomainHandler::createManifest() OUT");
        // if(stitching)
        // this.idm.remove(requestModel);
    }

    /**
     *
     * @param manifestModel manifestModel
     * @param topologyOnt topologyOnt
     * @param edgeDevice edgeDevice
     * @param edgeInterface edgeInterface
     */
    protected void addEdgeDeviceToTopology(OntModel manifestModel, OntResource topologyOnt, Device edgeDevice,
            OntResource edgeInterface, Collection<NetworkElement> elements) {
        logger.debug("InterDomainHandler::addEdgeDeviceToTopology IN");

        String link_url;
        OntResource link_ont;
        String domain_name;
        Resource domain_rs;
        boolean isStitchingDomain = NdlCommons.isStitchingNodeInManifest(edgeDevice.getResource());

        if (isStitchingDomain) {
            logger.debug("InterDomainHandler::addEdgeDeviceToTopology:adding stitchport to manifest: " + edgeDevice.getName());
            link_url = edgeDevice.getName();
            OntResource t = manifestModel.getOntResource(link_url);
            
            // Modify scenario
            boolean copyStitchPortPropertiesFromRequest = false;
            if(t == null) {
                logger.debug("InterDomainHandler::addEdgeDeviceToTopology:stitchport added via modify: " + edgeDevice.getName());
                for(NetworkElement e: elements) {
                    logger.debug("InterDomainHandler::addEdgeDeviceToTopology: e=" +e.getName() + " d=" + edgeDevice.getName());
                    if(e.getName() == edgeDevice.getName()) {
                        t = e.getResource();
                        copyStitchPortPropertiesFromRequest = true;
                        break;
                    }
                }
            }
            link_ont = manifestModel.createIndividual(link_url, NdlCommons.deviceOntClass);
            OntResource this_intf = idm.getOntResource(edgeInterface);
            for (StmtIterator i_s = this_intf.listProperties(NdlCommons.linkTo); i_s.hasNext();) {
                OntResource other_intf = idm.getOntResource(i_s.next().getResource());
                for (StmtIterator j_s = other_intf.listProperties(NdlCommons.linkTo); j_s.hasNext();) {
                    Resource stitch_intf = j_s.next().getResource();
                    if (stitch_intf.hasProperty(NdlCommons.hasURNProperty)) {
                        Literal stitch_urn = stitch_intf.getProperty(NdlCommons.hasURNProperty).getLiteral();
                        manifestModel.add(edgeInterface, NdlCommons.hasURNProperty, stitch_urn);
                    }
                }
            }
            if(t!=null && copyStitchPortPropertiesFromRequest){
                logger.debug("InterDomainHandler::addEdgeDeviceToTopology():copy stitchport properties from request r=" + t);
                for (StmtIterator itr = t.listProperties(NdlCommons.topologyHasInterfaceProperty); itr.hasNext();) {
                    Statement s = itr.next();
                    logger.debug("InterfaceDomainHandler::addEdgeDeviceToTopology(): hasInterface " + s.getObject());
                    link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, s.getObject());
                }
                for (StmtIterator itr = t.listProperties(NdlCommons.hasGUIDProperty); itr.hasNext();) {
                    Statement s = itr.next();
                    logger.debug("InterfaceDomainHandler::addEdgeDeviceToTopology(): hasGuid " + s.getObject());
                    link_ont.addProperty(NdlCommons.hasGUIDProperty, s.getObject());
                }
            }
        } else if (edgeDevice.getCastType() != null
                && edgeDevice.getCastType().equalsIgnoreCase(NdlCommons.multicast)) {
            link_url = edgeDevice.getName();
            link_ont = manifestModel.createIndividual(link_url, NdlCommons.deviceOntClass);
            link_ont.addProperty(NdlCommons.hasCastType, NdlCommons.multicastOntClass);
            topologyOnt.addProperty(NdlCommons.collectionItemProperty, link_ont);
            link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, edgeInterface);
        } else {
            link_url = edgeDevice.getURI();
            link_ont = manifestModel.createIndividual(link_url, NdlCommons.computeElementClass);
            link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, edgeInterface);
        }

        if (!link_ont.hasProperty(NdlCommons.inDomainProperty)) {
            domain_name = edgeDevice.getResourceType().getDomainURL();
        } else {
            domain_rs = link_ont.getProperty(NdlCommons.inDomainProperty).getResource();
            domain_name = domain_rs.getURI();
            link_ont.removeProperty(NdlCommons.inDomainProperty, domain_rs);
        }
        if (isStitchingDomain){
            link_ont.addProperty(NdlCommons.inDomainProperty, edgeDevice.getResource());
        }
        else {
            domain_rs = manifestModel.createResource(domain_name);
            link_ont.addProperty(NdlCommons.inDomainProperty, domain_rs);
            addDomainProperty(domain_rs, manifestModel);
        }

        link_ont.addProperty(NdlCommons.hasURLProperty, domain_name);
        link_ont.addProperty(NdlCommons.inConnection, "true", XSDDatatype.XSDboolean);
        link_ont.addProperty(NdlCommons.inRequestNetworkConnection, topologyOnt);
        edgeInterface.addProperty(NdlCommons.RDF_TYPE, NdlCommons.interfaceOntClass);

        if (isStitchingDomain){
            topologyOnt.addProperty(NdlCommons.collectionItemProperty, link_ont);
        }
        if (!domainInConnectionList.contains(link_ont))
            domainInConnectionList.add(link_ont);

        logger.debug("InterDomainHandler::addEdgeDeviceToTopology:Added domain:url=" + link_ont.getURI() + ";d.name= " + edgeDevice.getName() + ";domainName="
                + domain_name + ";rc_ont=" + topologyOnt.getURI());
        logger.debug("InterDomainHandler::addEdgeDeviceToTopology OUT");
    }

    public RequestMapping getMapper() {
        return mapper;
    }

}
