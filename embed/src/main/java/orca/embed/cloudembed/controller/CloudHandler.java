package orca.embed.cloudembed.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.cloudembed.MappingHandler;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.Domain;
import orca.ndl.DomainResource;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.IPAddress;
import orca.ndl.elements.IPAddressRange;
import orca.ndl.elements.Interface;
import orca.ndl.elements.LabelSet;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchingAction;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDB;

import orca.shirako.container.Globals;

public class CloudHandler extends MappingHandler {

    LinkedList<NetworkElement> deviceList = new LinkedList<NetworkElement>();
    protected int numNetworkConnection = 0;
    protected DomainElement commonLinkDevice = null;

    public static final int max_vlan_tag = 4095;

    // these were migrated from MappingHandler in preparation for recovery /ib 04/10/14
    protected LinkedList<OntResource> domainInConnectionList = new LinkedList<OntResource>();

    protected HashMap<String, BitSet> controllerAssignedLabel;

    protected HashMap<String, BitSet> globalControllerAssignedLabel; // from/to XmlrpcOrcaState

    protected HashMap<String, LinkedList<String>> shared_IP_set; // non-persistent

    protected HashMap<String, OntModel> domainModel;

    protected HashMap<String, LinkedList<Device>> domainConnectionList = new HashMap<String, LinkedList<Device>>();

    public HashMap<String, OntModel> getDomainModel() {
        return domainModel;
    }

    public void setDomainModel(HashMap<String, OntModel> domainModel) {
        this.domainModel = domainModel;
    }

    public LinkedList<OntResource> getDomainInConnectionList() {
        return domainInConnectionList;
    }

    public void setDomainInConnectionList(LinkedList<OntResource> domainInConnectionList) {
        this.domainInConnectionList = domainInConnectionList;
    }

    public HashMap<String, BitSet> getControllerAssignedLabel() {
        return controllerAssignedLabel;
    }

    public void setControllerAssignedLabel(HashMap<String, BitSet> controllerAssignedLabel) {
        this.controllerAssignedLabel = controllerAssignedLabel;
    }

    public HashMap<String, BitSet> getGlobalControllerAssignedLabel() {
        return globalControllerAssignedLabel;
    }

    public void setGlobalControllerAssignedLabel(HashMap<String, BitSet> globalControllerAssignedLabel) {
        this.globalControllerAssignedLabel = globalControllerAssignedLabel;
    }

    public HashMap<String, LinkedList<String>> getShared_IP_set() {
        return shared_IP_set;
    }

    public void setShared_IP_set(HashMap<String, LinkedList<String>> shared_IP_set) {
        this.shared_IP_set = shared_IP_set;
    }

    public CloudHandler() throws NdlException {
        super();
    }

    /**
     * Create handler with in-memory model
     * 
     * @param substrateFile substrateFile
     * @throws IOException in case of error
     * @throws NdlException in case of error
     */
    public CloudHandler(String substrateFile) throws IOException, NdlException {
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
    public CloudHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
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
    public CloudHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
        super(tdbPrefix, recover);
    }

    public LinkedList<NetworkElement> getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(Collection<NetworkElement> boundElements) {
        deviceList = (LinkedList<NetworkElement>) boundElements;
    }

    // dType = domainname.type
    public SystemNativeError runEmbedding(String domainName, RequestReservation request,
            DomainResourcePools domainResourcePools) {
        logger.debug("CloudHandler::runEmbedding() IN");
        Collection<NetworkElement> elements = request.getElements();
        OntModel requestModel = request.getModel();
        SystemNativeError error = null;

        // Check if enough domain resource
        HashMap<String, Integer> typeTotalUnits = request.getTypeTotalUnits();
        String pureType, domain;
        int count, resourceCount = 0, typeCount = 0;
        for (Entry<String, Integer> entry : typeTotalUnits.entrySet()) {
            typeCount++;
            pureType = entry.getKey();
            count = entry.getValue();
            if (!domainName.endsWith(pureType)) {
                domain = domainName + "/" + pureType;
            } else {
                domain = domainName;
                String tmpType = "/" + pureType;
                domainName = domainName.split(tmpType)[0];
            }
            if (domainResourcePools.getDomainResourceType(domain) == null) {
                error = new SystemNativeError();
                error.setErrno(1);
                error.setMessage("No available resources in domain: " + domain + ":" + pureType + "; Requested: "
                        + pureType + ":" + count);
                logger.error("CloudHandler:runEmbedding():" + error.toString());
                return error;
            }
            resourceCount = domainResourcePools.getDomainResourceType(domain).getCount();
            logger.info("CloudHandler:runEmbedding():" + pureType + ":" + count + ":" + domain + ":" + resourceCount);
            if (resourceCount < count) {
                error = new SystemNativeError();
                error.setErrno(2);
                error.setMessage("Not enough resources in domain: " + domain + ":" + pureType + "; Requested:" + count
                        + ";resource pool=" + resourceCount);
                logger.error("CloudHandler:runEmbedding():" +error.toString());
                return error;
            }
        }
        logger.info("CloudHandler:runEmbedding():There are " + typeCount + " types of resource in the request!!!");
        if (typeCount == 0) { // possible modifying
            if ((domainName.endsWith("vm")) || (!domainName.endsWith("baremetalce")) || (!domainName.endsWith("lun"))) {
                domain = domainName;
                int last_index = domainName.lastIndexOf("/");
                pureType = domainName.substring(last_index, domainName.length());
                domainName = domainName.substring(0, last_index);
                logger.info("CloudHandler:runEmbedding():But possible modifying element:domainName=" + domainName + ";type=" + pureType);
            }
        }
        // go form the reservation domainElement
        DomainElement link_device = null;
        for (NetworkElement element : elements) {
            logger.debug("CloudHandler:runEmbedding():element:" + element.getName() + ";isModify=" + element.isModify() + 
                         ";modify version=" + element.getModifyVersion() + ";this.modify version=" + this.modifyVersion);
            if (element instanceof NetworkConnection) {
                NetworkConnection nc = (NetworkConnection) element;
                if (!element.isModify()) {
                    link_device = createLinkDevice(nc, domainName, deviceList, domainResourcePools);
                }
                else {
                    for (NetworkElement existing_ce : deviceList) {
                        if (existing_ce.getName().equalsIgnoreCase(element.getName())) {
                            link_device = (DomainElement) existing_ce;
                            link_device.setModify(true);
                            setModifyFlag(link_device);
                            break;
                        }
                    }
                }
                if (link_device == null) {
                    error = new SystemNativeError();
                    error.setErrno(1);
                    error.setMessage(
                            "Cannot create the network reservation, likely no available resource, the user specified label or hybrid OF is not valid in this site:"
                                    + domainName);
                    return error;
                }
                try {
                    siteIPAddress(link_device);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (nc.getNe1() != null) {
                    logger.debug("CloudHandler:runEmbedding():NE1 not null, create edge device");
                    error = createEdgeDevice(nc.getNe1(), link_device, domainName, request.getNumNetworkConnection(),
                            requestModel, deviceList, domainResourcePools);
                    if (error != null)
                        return error;
                }
                if (nc.getNe2() != null) {
                    logger.debug("CloudHandler:runEmbedding():NE2 not null, create edge device");
                    error = createEdgeDevice(nc.getNe2(), link_device, domainName, request.getNumNetworkConnection(),
                            requestModel, deviceList, domainResourcePools);
                    if (error != null)
                        return error;
                }
                Iterator<? extends NetworkElement> it = nc.getConnection().iterator();
                while (it.hasNext()) {
                    error = createEdgeDevice(it.next(), link_device, domainName, request.getNumNetworkConnection(),
                            requestModel, deviceList, domainResourcePools);
                    if (error != null)
                        return error;
                }
                setControllerIPLabelSet(link_device);
            } else if (element instanceof ComputeElement) {
                // 1. an edge resource request from the InterDomainHandler, where the original domain name is
                // "InterDomain"
                if (request.getOri_reservationDomain() != null) {
                    ComputeElement ce = (ComputeElement) element;
                    for (Object de : ce.getDependencies().toArray()) {// From InterDomain request
                        if (de instanceof NetworkConnection) {
                            logger.debug("CloudHandler:runEmbedding():creating edge device for compute dependencies");
                            NetworkConnection ne = (NetworkConnection) de;
                            link_device = (DomainElement) ne.getFirstConnectionElement();
                            error = createEdgeDevice(element, link_device, domainName,
                                    request.getNumNetworkConnection(), requestModel, deviceList, domainResourcePools);
                            if (error != null)
                                return error;
                            break;
                        }
                    }
                }
                // 2. not edge of a request connection in the intra-site case, or
                if (((ComputeElement) element).getInterfaces() == null) {
                    logger.debug("CloudHandler:runEmbedding():creating edge device for compute intra-site case");
                    error = createEdgeDevice(element, null, domainName, request.getNumNetworkConnection(), requestModel,
                            deviceList, domainResourcePools);
                    if (error != null)
                        return error;
                }
            }
        }
        if (domainConnectionList.containsKey(domainName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("CloudHandler:runEmbedding():domainName=" + domainName + ":newDomainList.size="
                        + domainConnectionList.get(domainName).size());
            }
            logger.debug("CloudHandler:runEmbedding():node grp dependency");
            nodeGroupDependency(domainConnectionList.get(domainName));
        }

        logger.debug("CloudHandler::runEmbedding() OUT");
        return error;
    }

    private void setControllerIPLabelSet(DomainElement link_device) {
        logger.debug("CloudHandler::setControllerIPLabelSet() IN");
        // only set this controller's bitSet
        String d_name = link_device.getInDomain();
        IPAddressRange ip_range = link_device.getIp_range();
        if (ip_range == null)
            return;
        BitSet i_bSet = ip_range.getbSet();
        BitSet c_bSet = this.controllerAssignedLabel.get(d_name);
        BitSet g_bSet = null;
        if (this.globalControllerAssignedLabel != null)
            g_bSet = this.globalControllerAssignedLabel.get(d_name);

        if (i_bSet != null) {
            BitSet f_bSet = new BitSet(i_bSet.size());
            f_bSet.or(i_bSet);
            if (g_bSet != null)
                f_bSet.andNot(g_bSet);
            if (c_bSet == null) {
                this.controllerAssignedLabel.put(d_name, f_bSet);
            } else {
                this.controllerAssignedLabel.remove(d_name);
                this.controllerAssignedLabel.put(d_name, f_bSet);
            }
        }
        logger.debug("CloudHandler::setControllerIPLabelSet() OUT");
    }

    private IPAddressRange siteIPAddress(DomainElement link_device) throws UnknownHostException, InetNetworkException {
        logger.debug("CloudHandler::siteIPAddress() IN");
        if (link_device == null)
            return null;

        SwitchingAction action = link_device.getDefaultSwitchingAction();
        if (action == null)
            return null;

        LinkedList<Interface> intf_list = action.getClientInterface();
        OntResource action_intf_ont = null;
        Resource label_rs = null, ip_addr_rs = null;
        String ip_addr = null, ip_netmask = null;

        String domainName = link_device.getInDomain();
        logger.info("CloudHandler:siteIPAddress(): domain name=" + domainName);
        // BitSet bSet = this.controllerAssignedLabel.get(domainName);
        BitSet bSet = getAvailableBitSet(domainName);
        if (debugOn)
            System.out.println("CloudHandler domain="+domainName+";bSet="+bSet);
        IPAddressRange ip_range = null;
        for (Interface action_intf : intf_list) {
            action_intf_ont = action_intf.getResource();
            if (action_intf_ont.hasProperty(NdlCommons.layerLabel)) {
                label_rs = action_intf_ont.getProperty(NdlCommons.layerLabel).getResource();
                LinkedList<OntResource> ip_addr_rs_list = new LinkedList<OntResource>();
                for (StmtIterator k = label_rs.listProperties(NdlCommons.ip4LocalIPAddressProperty); k.hasNext();) {
                    ip_addr_rs = k.next().getResource();
                    OntResource ip_addr_ont = idm.getOntResource(ip_addr_rs);
                    if (ip_addr_ont.hasProperty(NdlCommons.layerLabelIsPrimary)
                            && (ip_addr_ont.getProperty(NdlCommons.layerLabelIsPrimary).getBoolean() == true)) {
                        ip_addr_rs_list.addFirst(ip_addr_ont);
                    } else {
                        ip_addr_rs_list.add(ip_addr_ont);
                    }
                }
                int list_size = ip_addr_rs_list.size();
                for (int i = 0; i < list_size; i++) {
                    OntResource ip_addr_ont = ip_addr_rs_list.get(i);
                    if (ip_addr_ont.hasProperty(NdlCommons.layerLabelIdProperty))
                        ip_addr = ip_addr_ont.getProperty(NdlCommons.layerLabelIdProperty).getString();
                    if (ip_addr_ont.hasProperty(NdlCommons.ip4NetmaskProperty))
                        ip_netmask = ip_addr_ont.getProperty(NdlCommons.ip4NetmaskProperty).getString();
                    if (ip_addr != null) {
                        if (ip_range == null) {
                            ip_range = new IPAddressRange(ip_addr, ip_netmask, ip_addr_ont);
                            ip_range.setbSet(bSet);
                            if (debugOn)
                                System.out.println("2. CloudHandler controller label bSet="+this.controllerAssignedLabel.get(domainName));
                        } else {
                            ip_range.modify(ip_addr, ip_netmask, ip_addr_ont);
                            if (debugOn) 
                                System.out.println("3. CloudHandler controller label bSet="+this.controllerAssignedLabel.get(domainName));
                        }
                    }
                }
            }
        }
        if (ip_range != null) {
            // possible recovered IP address;
            logger.debug("CloudHandler:siteIPAddress():Recovered shared IP, domain=" + domainName);
            if (shared_IP_set != null) {
                for (Entry<String, LinkedList<String>> entry : shared_IP_set.entrySet()) {
                    logger.debug("CloudHandler:siteIPAddress():recover:domain=" + entry.getKey());
                    for (String shared_ip_str : entry.getValue()) {
                        logger.debug("CloudHandler:siteIPAddress():recover:ip=" + shared_ip_str);
                    }
                }
            }

            if (this.shared_IP_set != null) {
                LinkedList<String> str_list = this.shared_IP_set.get(domainName);
                if (str_list != null) {
                    for (String shared_ip_str : str_list) {
                        logger.debug("CloudHandler:siteIPAddress():ip=" + shared_ip_str + ";netmask=" + ip_netmask);
                        int index = shared_ip_str.indexOf("/");
                        String ip_str = index > 0 ? shared_ip_str.split("\\/")[0] : shared_ip_str;
                        ip_range.modify(ip_str, ip_netmask, null);
                    }
                    this.shared_IP_set.remove(domainName);
                }
            }
            // create the ontResource for the base_ip_addr
            IPAddress base_ip_addr = ip_range.getBase_ip_addr();
            Individual base_ip_addr_rs = link_device.getModel().createIndividual(base_ip_addr.getURI(),
                    NdlCommons.IPAddressOntClass);
            base_ip_addr_rs.addProperty(NdlCommons.layerLabelIdProperty, base_ip_addr.getCIDR());
            base_ip_addr_rs.addProperty(NdlCommons.ip4NetmaskProperty, base_ip_addr.netmask);
            link_device.setIp_range(ip_range);
        }
        logger.debug("CloudHandler::siteIPAddress() OUT");
        return ip_range;
    }

    private BitSet getAvailableBitSet(String domainName) {
        logger.debug("CloudHandler::getAvailableBitSet() IN");
        BitSet startBitSet = null, controllerStartBitSet = null;
        if (this.globalControllerAssignedLabel != null) {
            startBitSet = this.globalControllerAssignedLabel.get(domainName);
        }
        if (this.controllerAssignedLabel != null) {
            controllerStartBitSet = this.controllerAssignedLabel.get(domainName);
        }
        BitSet sBitSet = new BitSet(255);
        if (startBitSet != null)
            sBitSet.or(startBitSet);
        if (controllerStartBitSet != null)
            sBitSet.or(controllerStartBitSet);

        logger.debug("CloudHandler::getAvailableBitSet() OUT");
        return sBitSet;
    }

    private SystemNativeError createEdgeDevice(NetworkElement element, DomainElement link_device, String domainName,
            int maxNumNetworkConnection, OntModel requestModel, LinkedList<NetworkElement> deviceList,
            DomainResourcePools domainResourcePools) {
        logger.debug("CloudHandler::createEdgeDevice() IN");
        SystemNativeError error = null;
        int num = element.getNumUnits();

        ComputeElement ce_element = (ComputeElement) element;
        ComputeElement cg_element = null;
        if (ce_element.getCeGroup() == null) {
            error = createEdgeDeviceCG(ce_element, link_device, domainName, requestModel, domainResourcePools, num);
        } else {
            for (NetworkElement ne_cg : ce_element.getCeGroup()) {
                cg_element = (ComputeElement) ne_cg;
                cg_element.setDependencies(ce_element.getDependencies());
                num = cg_element.getNumUnits();
                error = createEdgeDeviceCG(cg_element, link_device, domainName, requestModel, domainResourcePools, num);
            }
        }
        logger.debug("CloudHandler::createEdgeDevice() OUT");
        return error;
    }

    private SystemNativeError createEdgeDeviceCG(NetworkElement element, DomainElement link_device, String domainName,
            OntModel requestModel, DomainResourcePools domainResourcePools, int num) {
        logger.debug("CloudHandler:createEdgeDeviceCG(): IN");
        SystemNativeError error = null;
        DomainElement device = null;
        boolean mpDevice = false;
        DomainResourceType dType = null;
        String domain_name = domainName + "/" + element.getResourceType().getResourceType();

        for (int i = 0; i < num; i++) {
            mpDevice = false;
            device = createNewNode(element, i, link_device, domain_name, requestModel, deviceList, num);

            if (device.getCastType() != null && device.getCastType().equalsIgnoreCase(NdlCommons.multicast)) {
                mpDevice = true;
            }
            logger.debug("CloudHandler:createEdgeDeviceCG(): device:" + device.getName() + ";numInterface=" + device.getNumInterface()
                    + ";isModify=" + device.isModify() + ";modify version=" + element.getModifyVersion() + ";this.modify version=" + this.modifyVersion);
            setModifyFlag(device);

            // this should only happen if not a modify ?
            if (!deviceList.contains(device)) {
                deviceList.add(device);
                if (!mpDevice && !device.isModify()) {
                    dType = domainResourcePools.getDomainResourceType(domain_name);
                    int resourceCount = resourceCount(device, dType);
                    if (resourceCount < 0) {
                        error = new SystemNativeError();
                        error.setErrno(1);
                        error.setMessage(
                                "No available resources in domain: " + domainName + ":resorceCount=" + resourceCount);
                        logger.error("CloudHandler:createEdgeDeviceCG():" +error.toString());
                        return error;
                    }
                }
            }

            // new links need to be added to list
            LinkedList<Device> domainDevices = domainConnectionList.computeIfAbsent(domainName,
                    k -> new LinkedList<>());
            if (!mpDevice && !domainDevices.contains(device)) {
                domainDevices.add(device);
            }
        }
        logger.debug("CloudHandler:createEdgeDeviceCG(): OUT");
        return error;
    }

    /**
     * creates a new node, or if Node is being Modified, will get/update it from the deviceList
     */
    private DomainElement createNewNode(NetworkElement element, int i, DomainElement link_device, String domainName,
            OntModel requestModel, LinkedList<NetworkElement> deviceList, int num) {
         
        logger.info("CloudHandler:createNewNode(): IN");
        logger.info("CloudHandler:createNewNode():Creating new node:hole=" + i + ";domainName=" + domainName);
        if (link_device != null)
            logger.debug(";link_device=" + link_device.getName());

        DomainResourceType dType = new DomainResourceType(element.getResourceType().getResourceType(),
                element.getResourceType().getCount());
        dType.setCount(1);
        dType.setDomainURL(domainName);
        // String url=dType.getDomainURL()+"/"+dType.getResourceType();
        String name = element.getName();
        String url = name;
        ComputeElement ce_element = (ComputeElement) element;
        String dTypeStr = element.getResourceType().getResourceType();
        if ((dTypeStr.endsWith("vm")) || (dTypeStr.endsWith("baremetalce")) || (dTypeStr.endsWith("lun"))) {
            if (ce_element.getGroup() != null && !ce_element.isModify())
                name = name.concat("/") + String.valueOf(i);
        }

        String device_guid = element.getGUID();
        if (device_guid == null) {
            device_guid = UUID.randomUUID().toString();
            element.getResource().addProperty(NdlCommons.hasGUIDProperty, device_guid);
        }
        OntModel device_model = element.getModel();
        if (this.manifestModel != null){
            device_model = this.manifestModel;
        }
        if (device_model.getOntResource(element.getName()) == null) {
            device_model.createIndividual(element.getName(), element.getResource().getRDFType(true));
            device_model.getOntResource(element.getName()).addProperty(NdlCommons.hasGUIDProperty, device_guid);
        }
        ComputeElement ce = ce_element.partialCopy(device_model, requestModel, url, name);
        ce.setResourceType(dType);
        ce.setNodeGroupName(ce_element.getNodeGroupName());
        ce.setPostBootScript(ce_element.getPostBootScript());
        HashMap<String, DomainResource> map = ce_element.getResourcesMap();
        if (map == null)
            logger.debug("CloudHandler:createNewNode(): no constraint 1!");
        ce.setResourcesMap(map);

        // create a new DomainElement, or get one from the deviceList
        DomainElement edge_device = new DomainElement(device_model, url, name);
        int listIndex = deviceList.indexOf(edge_device);
        if (-1 != listIndex) {
            edge_device = (DomainElement) deviceList.get(listIndex);
            if (logger.isDebugEnabled()) {
                logger.debug("CloudHandler:createNewNode():Using existing device: " + listIndex  + " " + edge_device.getName());
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("CloudHandler:createNewNode():Device is new: " + edge_device.getName());
            }
            edge_device.setResourceType(dType);
            edge_device.setGUID(device_guid);
        }

        // update or set the CE
        if (null != edge_device.getCe()) {
            ce.addDependency((HashSet<NetworkElement>) edge_device.getCe().getDependencies());
            // this is the fix for #196
            ce.setInterfaces(edge_device.getCe().getInterfaces());
            ce.setClientInterface(edge_device.getCe().getClientInterface());
        }
        edge_device.setCe(ce);

        // update or set the Resources Map
        if (null == edge_device.getResourcesMap()) {
            edge_device.setResourcesMap(map);
        }
        if (null == edge_device.getCe().getResourcesMap()) {
            logger.info("CloudHandler:createNewNode():updating edge_device: " + edge_device.getName() + "with new CE.");
            edge_device.setCe(ce);
        }

        edge_device.setModify(element.isModify());

        try {
            if (logger.isDebugEnabled()) {
                if (ce.getGroup() == null) {
                    logger.debug("CloudHandler:createNewNode():Adding a single node");
                } else {
                    logger.debug("CloudHandler:createNewNode():Adding node to group");
                }
            }

            createInterface(ce_element, edge_device, i, link_device);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InetNetworkException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("CloudHandler:createNewNode():Created edge_device " + edge_device);
        }
        logger.info("CloudHandler:createNewNode(): OUT");
        return edge_device;
    }

    public OntResource getCEOnt(OntResource rs_ont) {
        return getCEInterfaceOnt(manifestModel, rs_ont);
    }

    /**
     * This method is called from external classes. TODO: Should probably be moved to a utility class?
     *
     * @param manifestModel manifestModel
     * @param rs_ont rs_ont
     * @return Individual
     */
    public static Individual getCEInterfaceOnt(OntModel manifestModel, OntResource rs_ont) {
        if (Globals.Log.isTraceEnabled()) {
            Globals.Log.trace("Inside getCEInterfaceOnt", new Throwable());
        }

        Individual rs1_ind = null;
        if (rs_ont != null) {
            rs1_ind = manifestModel.createIndividual(rs_ont.getURI(), NdlCommons.interfaceOntClass);
            if (rs_ont.hasProperty(NdlCommons.ip4LocalIPAddressProperty)) {
                Resource ip_rs = rs_ont.getProperty(NdlCommons.ip4LocalIPAddressProperty).getResource();
                Individual ip_ind = manifestModel.createIndividual(ip_rs.getURI(), NdlCommons.IPAddressOntClass);
                rs1_ind.addProperty(NdlCommons.ip4LocalIPAddressProperty, ip_ind);
                if (ip_rs.hasProperty(NdlCommons.layerLabelIdProperty))
                    ip_ind.addProperty(NdlCommons.layerLabelIdProperty,
                            ip_rs.getProperty(NdlCommons.layerLabelIdProperty).getString());
                if (ip_rs.hasProperty(NdlCommons.ip4NetmaskProperty)) {
                    ip_ind.addProperty(NdlCommons.ip4NetmaskProperty,
                            ip_rs.getProperty(NdlCommons.ip4NetmaskProperty).getString());
                } else {
                    if (Globals.Log.isTraceEnabled()) {
                        Globals.Log.trace("Individual did not contain netmask property: " + ip_rs);
                    }
                }
            }

            if (rs_ont.hasProperty(NdlCommons.hasGUIDProperty)){
                rs1_ind.addProperty(NdlCommons.hasGUIDProperty,
                        rs_ont.getProperty(NdlCommons.hasGUIDProperty).getString());
            }
        }
        return rs1_ind;
    }

    public boolean isInterdomain(ComputeElement ce_element, ComputeElement ce, DomainElement link_device) {
        logger.debug("CloudHandler::isInterdomain() IN");
        boolean isInter = false;
        if (link_device == null)
            return isInter;
        LinkedList<Interface> interfaces = ce_element.getClientInterface();
        NetworkConnection ncByInterface = null;
        if (interfaces == null)
            return isInter;
        Interface ce_intf = null;
        for (Interface intf : interfaces) {
            ncByInterface = ce_element.getConnectionByInterfaceName(intf);
            if (ncByInterface == null) {
                logger.warn("CloudHandler:isInterdomain():No connection associated with this interface" + intf.getName());
                continue;
            }
            logger.debug("CloudHandler:isInterdomain()::ncByInterface=" + ncByInterface.getName() + "intf=" + intf.getName()
                    + "ce_intf=" + ce.getInterfaceByName(ncByInterface.getName()));
            if (link_device != null)
                logger.debug(";link_device=" + link_device.getName());
            if (!ncByInterface.getName().equals(link_device.getName())) { // inter-site topology request: link !=
                                                                          // connection
                ce_intf = ce.getInterfaceByName(ncByInterface.getName());
                if (ce_intf != null)
                    logger.debug("CloudHandler:isInterdomain():ce_intf=" + ce_intf.getName());
                logger.debug("CloudHandler:isInterdomain():intf=" + intf.getName());
                if (ce_intf != null && ce_intf == intf) {
                    isInter = true;
                    break;
                }
            }
        }
        logger.debug("CloudHandler::isInterdomain() OUT");
        return isInter;
    }

    public void createInterface(ComputeElement element, DomainElement edge_device, int hole, DomainElement link_device)
            throws InetNetworkException, UnknownHostException {
        logger.debug("CloudHandler::createInterface(): IN");
        if (link_device == null)
            return;

        ComputeElement ce = edge_device.getCe();
        if (ce == null) {
            logger.info("CloudHandler:createInterface():ce is null!");
            return;
        }

        LinkedList<Interface> interfaces = element.getClientInterface();
        NetworkConnection ncByInterface = null;
        Interface current_intf = null;
        if (interfaces != null) {
            for (Interface intf : interfaces) {
                ncByInterface = element.getConnectionByInterfaceName(intf);
                if (ncByInterface == null) {
                    logger.warn("CloudHandler:createInterface():No connection associated with this interface" + intf.getName());
                    continue;
                }
                if (ncByInterface.getName().equals(link_device.getName())) { // inter-site topology request: link !=
                                                                             // connection
                    current_intf = intf;
                    break;
                }
                boolean outer_break = false;
                for (Object de : ce.getDependencies().toArray()) {
                    if (de instanceof NetworkConnection) {
                        NetworkConnection ne = (NetworkConnection) de;
                        logger.info("CloudHandler:createInterface():ncByInterface=" + ncByInterface.getName() + ";ne=" + ne.getName());
                        if ((ncByInterface.getName().equals(ne.getName())) && ne.hasConnection(link_device)) {
                            current_intf = intf;
                            outer_break = true;
                            break;
                        }
                    }
                }
                if (outer_break)
                    break;
            }
        }

        if (current_intf == null)
            return;

        // #178 -- NodeGroup Increase needs to refer to the FirstElements Parent interface (sameAs)
        Resource intf_ont = current_intf.getResource();
        if (null != NdlCommons.getSameAsResource(intf_ont)){
            intf_ont = NdlCommons.getSameAsResource(intf_ont);
        }

        OntModel device_model = element.getModel();
        if (this.manifestModel != null){
            device_model = this.manifestModel;
        }

        IPAddressRange ip_range = link_device.getIp_range();

        String network_str = null, netmask = null;
        IPAddress new_ip = null, base_ip = null;
        if (ip_range != null) {
            if (ip_range.getBase_IP() == null) {
                logger.error("CloudHandler:createInterface():: No base IP address with site defined IP range!");
                return;
            } else {
                network_str = ip_range.getBase_IP().getNetwork();
                hole = findIPRangeHole(ip_range);
                base_ip = ip_range.getBase_ip_addr();
            }
        } else if (current_intf != null) {
            base_ip = (IPAddress) current_intf.getLabel();
            if (base_ip == null) {
                logger.error("CloudHandler:createInterface():: No base IP address in theinterface:" + current_intf.getName());
                return;
            }
            InetNetwork ip_str_IP = new InetNetwork(base_ip.address, base_ip.netmask);
            if (base_ip.address != null)
                network_str = ip_str_IP.getNetwork();
        }
        netmask = base_ip.netmask;
        String url = null;
        try {
            // #178 use the Ontology Resource URI for the name, since it might have been updated to the `sameAs` interface
            new_ip = base_ip.getNewIpAddress(device_model, network_str, netmask, intf_ont.getURI(), hole);

            // #137, when IPs are not assigned, duplicate interfaces can be created
            if (-1 == hole) {
                int index = ce.getURI().lastIndexOf("/");
                String nodeUUID = index > 0 ? ce.getURI().substring(index + 1) : UUID.randomUUID().toString();
                url = new_ip.getURI() + nodeUUID + "/intf";
            } else {
                url = new_ip.getURI() + "/intf";
            }

            if (logger.isTraceEnabled()) {
                logger.trace("CloudHandler:createInterface():new_ip " + new_ip + " using netmask " + netmask);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Interface new_intf = new Interface(device_model, url, url);
        OntResource new_intfResource = new_intf.getResource();

        new_intfResource.addProperty(NdlCommons.ip4LocalIPAddressProperty, new_ip.getResource(device_model));

        if (intf_ont.getProperty(NdlCommons.hostInterfaceName) != null) {

            String site_host_interface = intf_ont.getProperty(NdlCommons.hostInterfaceName).getString();
            new_intf.getResource().addProperty(NdlCommons.hostInterfaceName, site_host_interface);
        }
        if (new_ip.cidr != null){
            new_intfResource.addProperty(NdlCommons.layerLabelIdProperty, new_ip.cidr);
        }
        new_intfResource.addProperty(NdlCommons.ip4NetmaskProperty, netmask);
        new_intf.setLabel(new_ip);

        ce.addClientInterface(new_intf);
        edge_device.addClientInterface(new_intf);

        new_intfResource.addProperty(NdlCommons.OWL_sameAs, intf_ont);

        String element_guid = UUID.randomUUID().toString();
        new_intfResource.addProperty(NdlCommons.hasGUIDProperty, element_guid);

        logger.debug("CloudHandler:createInterface():new_ip=" + new_ip.getURI() + ";url=" + url + 
        ";new_intf=" + new_intf.getURI() + " new_guid=" + element_guid);

        ncByInterface = element.getConnectionByInterfaceName(current_intf);

        ce.setInterfaceName(ncByInterface, new_intf);

        if (debugOn) {
            System.out.println("SETTING NEIGHBORHOOD" );
            System.out.println("  Edge Device " + edge_device.getName());
            System.out.println("  Link Device " + link_device.getName());
            System.out.println("  New Intf " + new_intf.getName());
            System.out.println("  ncByInterface " + ncByInterface.getName());
        }
        setEdgeNeighbourhood(edge_device, link_device, new_intf, ncByInterface);
        logger.debug("CloudHandler::createInterface(): OUT");
    }

    protected void setEdgeNeighbourhood(DomainElement edge_device, DomainElement link_device, Interface new_intf,
            NetworkConnection ncByInterface) {
        logger.debug("CloudHandler::setEdgeNeighbourhood() IN");
        ComputeElement ce = edge_device.getCe();
        if (ce == null) {
            logger.info("CloudHandler:setEdgeNeighbourhood():ce is null!");
            return;
        }
        ce.setInterfaceName(ncByInterface, new_intf);
        if (link_device != null) {
            if (ncByInterface != null) {
                if (ncByInterface.getName().equals(link_device.getName())) { // intra-site topology request: link = connection
                    if (new_intf.getResource() == null) {
                        logger.error("CloudHandler:setEdgeNeighbourhood():new_intf.getResource() is null - this will cause errors");
                    }
                    link_device.setFollowedBy(edge_device, new_intf.getResource());
                    edge_device.setPrecededBy(link_device, new_intf.getResource());
                } else { // Inter-site topology request: link=the neighboring domain; connection = requestconnection
                    for (Object de : ce.getDependencies().toArray()) {
                        if (de instanceof NetworkConnection) {
                            NetworkConnection ne = (NetworkConnection) de;
                            // this IF caused problems firing sometimes for #178 due to random order of traversal of array above /ib 01/24/18
                            if (ne.getName().equals(ncByInterface.getName())) {
                                if (ne.getFirstConnectionElement() != null)
                                    link_device = (DomainElement) ne.getFirstConnectionElement();
                                link_device.setFollowedBy(edge_device, new_intf.getResource());
                                edge_device.setPrecededBy(link_device, new_intf.getResource());
                                // Ultimately the cause of #178 was the break statement below living outside the enclosing
                                // IF statement. Now it is in the right place, however we must note that if somehow more
                                // than one ne matches the enclosing if (due to a bug), the results of this function become
                                // random. /ib 01/24/18
                                break;
                            }
                        }
                    }
                }
            } else {
                link_device.setFollowedBy(edge_device, new_intf.getResource());
                edge_device.setPrecededBy(link_device, new_intf.getResource());
            }
            logger.info("CloudHandler:setEdgeNeighbourhood():dependency: " + (link_device != null ? link_device.getName() : "null") + "; ncByInterface="
                    + (ncByInterface != null ? ncByInterface.getName() : "null") + "; interface="
                    + ((new_intf != null) && (new_intf.getResource() != null) ? new_intf.getResource().getURI()
                            : "null")
                    + ";id=" + ((new_intf != null) && (new_intf.getResource() != null)
                            ? new_intf.getResource().getProperty(NdlCommons.layerLabelIdProperty) : "null"));
        }
        logger.debug("CloudHandler::setEdgeNeighbourhood() OUT");
    }

    private DomainElement createLinkDevice(NetworkConnection element, String domainName,
            LinkedList<NetworkElement> deviceList, DomainResourcePools domainResourcePools) {
        logger.debug("CloudHandler::createLinkDevice() IN");
        DomainResourceType dType = element.getResourceType();
        String domain_name = domainName + "/" + dType.getResourceType();

        dType.setDomainURL(domain_name);
        OntModel device_model = element.getModel();
        if (this.manifestModel != null)
            device_model = this.manifestModel;
        DomainElement link_device = new DomainElement(device_model, element.getName(), element.getName());
        if (device_model.getOntResource(element.getName()) == null){
            device_model.createIndividual(element.getName(), element.getResource().getRDFType(true));
        }
        link_device.setResourceType(dType);
        logger.info("CloudHandler:createLinkDevice():" + domainName + " link_device: url=" + link_device.getURI() + ":name="
                + link_device.getName() + " BW=" + element.getBandwidth());

        LinkedList<String> edge_type = new LinkedList<String>();
        if (element.getNe1() != null) {
            String eType = element.getNe1().getType();
            logger.debug("CloudHandler:createLinkDevice():ne1 type=" + eType);
            edge_type.add(eType);
            if (element.getNe2() != null) {
                eType = element.getNe2().getType();
                edge_type.add(eType);
                logger.debug("CloudHandler:createLinkDevice():ne2 type=" + eType);
            }
        } else { // broadcast link
            Iterator<? extends NetworkElement> it = element.getConnection().iterator();
            while (it.hasNext()) {
                edge_type.add(it.next().getType());
            }
        }
        float label_id = element.getLabel_ID();
        float site_label_id = 0;

        LinkedList<Interface> intf_list = new LinkedList<Interface>();
        Interface intf = null;
        LinkedList<OntResource> edge_intf_list = getEdgeInterface(domainName, edge_type, element);
        if (edge_intf_list == null) {
            logger.error("CloudHandler:createLinkDevice():No edge interface found in domain:" + domainName + ":" + edge_type);
            return null;
        }
        for (OntResource edge_intf : edge_intf_list) {
            intf = new Interface();
            intf.setURI(edge_intf.getURI());
            if (edge_intf.getProperty(NdlCommons.RDFS_Label) != null)
                intf.setName(edge_intf.getProperty(NdlCommons.RDFS_Label).getString());
            intf.setResource(edge_intf);
            intf_list.add(intf);
            if (edge_intf.hasProperty(NdlCommons.layerLabel)) {
                Resource site_label_rs = edge_intf.getProperty(NdlCommons.layerLabel).getResource();
                if (site_label_rs.hasProperty(NdlCommons.layerLabelIdProperty))
                    site_label_id = site_label_rs.getProperty(NdlCommons.layerLabelIdProperty).getFloat();
                if (site_label_id != 0) {
                    logger.info("CloudHandler:createLinkDevice():This is a tagged link in the site:" + site_label_id);
                    link_device.setUpNeighbour(edge_intf);
                    if (label_id != 0) {
                        if (label_id != site_label_id) {
                            logger.error("CloudHandler:createLinkDevice():Requested user specified tag doesn't match the tagged interface!");
                            return null; // requested user specified tag doesn't match the tagged interface
                        }
                    } else
                        label_id = site_label_id;
                }
            }

        }

        SwitchingAction action = null;
        String action_layer = "EthernetElement";
        if (action_layer != null) {
            if ((label_id != 0) && (site_label_id == 0)) {
                if (validLabelID(link_device.getInDomain(), label_id) == false) {
                    logger.error("CloudHandler:createLinkDevice():User specified label id is not valid in this substrate!" + label_id);
                    return null;
                }
            }
            action = new SwitchingAction(device_model);
            action.setAtLayer(action_layer);
            action.setDefaultAction("VLANtag");
            action.setBw(element.getBandwidth());
            for (Interface action_intf : intf_list)
                action.addClientInterface(action_intf);

            if (label_id != 0) {
                action.setLabel_ID(label_id);
                link_device.setAllocatable(false);
                link_device.setStaticLabel(label_id); // user defined label
            }
            link_device.addSwitchingAction(action);
        }

        if (edge_intf_list.isEmpty())
            return null;

        if (site_label_id == 0)
            link_device.setUpNeighbour(edge_intf_list.element()); // the first edge

        dType = domainResourcePools.getDomainResourceType(domain_name);
        int resourceCount = resourceCount(link_device, dType);
        if (resourceCount < 0)
            return null;

        setModifyFlag(link_device);
        deviceList.add(link_device);

        LinkedList<Device> domainDevices = domainConnectionList.computeIfAbsent(domainName, k -> new LinkedList<>());
        if (!domainDevices.contains(link_device)) {
            domainDevices.add(link_device);
        }

        logger.debug("CloudHandler::createLinkDevice() OUT");
        return link_device;
    }

    public void setModifyFlag(DomainElement device) {
        if (this.isModify())
            device.setModifyVersion(this.modifyVersion);
    }

    public int resourceCount(DomainElement de, DomainResourceType dType) {
        logger.debug("CloudHandler::resourceCount() IN");
        if (dType == null) {
            logger.error("CloudHandler:resourceCount():No available resource:dType=" + de.toString());
            return -1;
        }
        int resourceCount = dType.getCount();
        String domainName = de.getInDomain();
        int requestResourceCount = de.getResourceType().getCount();
        logger.debug("CloudHandler:resourceCount():domainName=" + domainName + ";resourceCount=" + resourceCount + ";requestResourceCount="
                + requestResourceCount);

        if (resourceCount - requestResourceCount < 0)
            return -1;

        dType.setCount(resourceCount - requestResourceCount);

        logger.debug("CloudHandler::resourceCount() OUT");
        return dType.getCount();
    }

    protected boolean validLabelID(String domain_url, float id) {
        logger.debug("CloudHandler::validLabelID() IN");
        logger.info("CloudHandler:validLabelID():Validating the user specified label. domain_url=" + domain_url + ";id=" + id);

        OntModel domainOnt = domainModel.get(domain_url);
        if (domainOnt == null) {
            logger.error("CloudHandler:validLabelID():No this site model:" + domain_url);
            return false;
        }
        Resource domain = null;
        for (ResIterator j = domainOnt.listResourcesWithProperty(NdlCommons.RDF_TYPE, NdlCommons.deviceOntClass); j
                .hasNext();) {
            domain = j.next();
        }
        Resource networkService = domain.getProperty(NdlCommons.domainHasServiceProperty).getResource();

        boolean valid = false;
        Resource set = null, type_rs = null, label_set = null, label = null;
        Statement setStm, elementStm, labelStm, lbStm, ubStm;
        float lb_id = 0, ub_id = 0;
        for (StmtIterator j = networkService.listProperties(NdlCommons.availableLabelSet); j.hasNext();) {
            set = j.next().getResource();
            valid = false;
            if (set.hasProperty(NdlCommons.domainIsAllocatable)) {
                if (set.getProperty(NdlCommons.domainIsAllocatable).getBoolean() == false) {
                    // labelSet properties to be carried on
                    for (StmtIterator element = set.listProperties(NdlCommons.collectionElementProperty); element
                            .hasNext();) {
                        elementStm = element.next();

                        label_set = elementStm.getResource();
                        lbStm = label_set.getProperty(NdlCommons.lowerBound);
                        ubStm = label_set.getProperty(NdlCommons.upperBound);
                        if (lbStm != null) {
                            label = lbStm.getResource();
                            labelStm = label.getProperty(NdlCommons.layerLabelIdProperty);
                            if (labelStm != null)
                                lb_id = labelStm.getFloat();
                        }
                        if (ubStm != null) {
                            label = ubStm.getResource();
                            labelStm = label.getProperty(NdlCommons.layerLabelIdProperty);
                            if (labelStm != null)
                                ub_id = labelStm.getFloat();
                        }
                        logger.info("CloudHandler:validLabelID():lb=" + lb_id + ":ub=" + ub_id);
                        if ((id >= lb_id) && (id <= ub_id))
                            valid = true;
                        // single label
                        if ((lbStm == null) && (ubStm == null)) {
                            setStm = label_set.getProperty(NdlCommons.layerLabelIdProperty);
                            if (setStm != null) {
                                logger.info("CloudHandler:validLabelID():element=" + setStm.getFloat());
                                if (id == setStm.getFloat())
                                    valid = true;
                            }
                        }
                        if (valid)
                            break;
                    }
                }
            }
            if (valid)
                break;
        }
        logger.debug("CloudHandler::validLabelID() OUT");
        return valid;
    }

    // Dependencies between compute elements
    // If there are multiple elements in the parent nodegroup, the children will only follow the first master node.
    private void nodeGroupDependency(LinkedList<Device> deviceList) {
        logger.debug("CloudHandler::nodeGroupDependency() IN");
        DomainElement de, parent_de;
        ComputeElement child, parent;
        Set<NetworkElement> nodeDependency = null;
        String parent_name = null;
        for (Device device : deviceList) {
            de = (DomainElement) device;
            child = de.getCe();
            if (child == null)
                continue;
            nodeDependency = child.getDependencies();
            if (nodeDependency != null) {
                for (NetworkElement parent_ne : nodeDependency) {
                    for (Device parent_device : deviceList) {
                        parent_de = (DomainElement) parent_device;
                        parent = parent_de.getCe();
                        if (parent == null)
                            continue;
                        if (parent.getNodeGroupName() != null)
                            parent_name = parent.getNodeGroupName();
                        else
                            parent_name = parent.getName();

                        if (parent_name.equals(parent_ne.getName())) {
                            logger.info("CloudHandler:nodeGroupDependency():Added master-slave!" + child.getResource() + ":" + parent_name + ":"
                                    + parent_ne.getName());
                            if (child.getDefaultClientInterfaceIPAddressRS() != null)
                                parent_de.setFollowedBy(de, child.getDefaultClientInterfaceIPAddressRS());
                            else {
                                if (child.getDefaultClientInterface() != null)
                                    parent_de.setFollowedBy(de, child.getDefaultClientInterface().getResource());
                            }

                            if (parent.getDefaultClientInterfaceIPAddressRS() != null)
                                de.setPrecededBy(parent_de, parent.getDefaultClientInterfaceIPAddressRS());
                            else {
                                if (parent.getDefaultClientInterface() != null)
                                    de.setPrecededBy(parent_de, parent.getDefaultClientInterface().getResource());
                            }
                        }
                    }
                }
            }
        }
        logger.debug("CloudHandler::nodeGroupDependency() OUT");
    }

    public LinkedList<OntResource> getEdgeInterface(String domainName, LinkedList<String> edge_type,
            NetworkConnection nc) {
        logger.debug("CloudHandler::getEdgeInterface() IN");
        LinkedList<OntResource> edge_intf_ont_list = new LinkedList<OntResource>();
        String link_device_edge = null;
        for (String type : edge_type) {
            link_device_edge = domainName + "/" + type;
            OntResource edge_intf_ont = getEdgeInterface(link_device_edge, nc);
            if (edge_intf_ont != null)
                edge_intf_ont_list.add(edge_intf_ont);
        }
        logger.debug("CloudHandler::getEdgeInterface() OUT");
        return edge_intf_ont_list;
    }

    public OntResource getEdgeInterface(String domain_url, NetworkConnection nc) {
        logger.debug("CloudHandler::getEdgeInterface() IN");
        OntResource domain_rs = this.idm.getOntResource(domain_url);
        OntResource edge_intf_rs = null;
        if (domain_rs == null) {
            logger.error("CloudHandler:getEdgeInterface():Domain doesn't exist:" + domain_url + "\n");
            return null;
        }
        if (domain_rs.getProperty(NdlCommons.topologyHasInterfaceProperty) == null) {
            logger.error("CloudHandler:getEdgeInterface():Domain doesn't have interface:" + domain_rs + "\n");
            return null;
        } else {
            Resource intf_rs = null;
            String intf_of = null;
            String nc_of = null;
            if (nc != null)
                nc_of = nc.getOpenflowCapable();
            for (StmtIterator j = domain_rs.listProperties(NdlCommons.topologyHasInterfaceProperty); j.hasNext();) {
                intf_rs = j.next().getResource();
                intf_of = NdlCommons.getOpenFlowVersion(intf_rs);
                if (nc_of != null) {
                    if (intf_of != null) {
                        if (intf_of.equalsIgnoreCase(nc_of)) {
                            edge_intf_rs = idm.getOntResource(intf_rs);
                            break;
                        }
                    }
                } else {
                    if (intf_of == null) {
                        edge_intf_rs = idm.getOntResource(intf_rs);
                        break;
                    }
                }
            }
            if (edge_intf_rs == null)
                logger.error("CloudHandler:getEdgeInterface():No edge interface found!! Domain=" + domain_url);
            else
                logger.info("CloudHandler:getEdgeInterface():Domain:" + domain_url + "border interface:" + edge_intf_rs.getURI());
            logger.info("OF:nc_of=" + nc_of + ";intf_of" + intf_of);
            logger.debug("CloudHandler::getEdgeInterface() OUT");
            return edge_intf_rs;
        }
    }

    public OntModel createManifest(OntModel manifestModel, OntResource manifest, LinkedList<Device> domainList) {
        logger.debug("CloudHandler::createManifest() IN");
        Device next_Hop = null;
        String link_url, link_name, domain_name;
        OntResource link_ont = null;
        Resource domain_rs = null;
        for (int i = 0; i < domainList.size(); i++) {
            next_Hop = domainList.get(i);
            link_url = next_Hop.getURI();
            link_name = next_Hop.getName();

            logger.debug("CloudHandler:createManifest():" + i + "." + link_name + ":" + next_Hop.getType());

            DomainElement d = (DomainElement) next_Hop;
            ComputeElement ce = d.getCe();

            if (ce != null && ce.isModify())
                continue;

            if (next_Hop.getType().endsWith("lun"))
                link_ont = manifestModel.createIndividual(link_name, NdlCommons.networkStorageClass);
            else if ((next_Hop.getType().endsWith("vm")) || (next_Hop.getType().endsWith("baremetalce"))) {
                link_ont = manifestModel.createIndividual(link_name, NdlCommons.computeElementClass);
            } else if (next_Hop.getType().endsWith("vlan")) {
                link_ont = manifestModel.createIndividual(link_name, NdlCommons.topologyLinkConnectionClass);
            } else
                continue;

            if (!link_ont.hasProperty(NdlCommons.inDomainProperty)) {
                domain_name = next_Hop.getResourceType().getDomainURL();
            } else {
                domain_rs = link_ont.getProperty(NdlCommons.inDomainProperty).getResource();
                domain_name = domain_rs.getURI();
                link_ont.removeProperty(NdlCommons.inDomainProperty, domain_rs);
            }
            if (!domain_name.endsWith(next_Hop.getType()))
                domain_name = domain_name + "/" + next_Hop.getType();
            domain_rs = manifestModel.createResource(domain_name);
            link_ont.addProperty(NdlCommons.inDomainProperty, domain_rs);
            link_ont.addProperty(NdlCommons.topologyHasURL, domain_name);
            addDomainProperty(domain_rs, manifestModel);

            // nodeGroup
            if (ce != null) {
                if (ce.getNodeGroupName() != null)
                    link_ont.addProperty(NdlCommons.hasRequestGroupURL, ce.getNodeGroupName());
                if (ce.getImage() != null) {
                    Individual image = manifestModel.createIndividual(ce.getImage(), NdlCommons.diskImageClass);
                    link_ont.addProperty(NdlCommons.diskImageProperty, image);
                    if (ce.getVMImageURL() != null)
                        image.addProperty(NdlCommons.hasURLProperty, ce.getVMImageURL());
                    if (ce.getVMImageHash() != null)
                        image.addProperty(NdlCommons.hasGUIDProperty, ce.getVMImageHash());
                }
                if (ce.getSpecificCETypeurl() != null) {
                    Resource ceType_rs = manifestModel.createResource(ce.getSpecificCETypeurl());
                    link_ont.addProperty(NdlCommons.specificCEProperty, ceType_rs);
                }

                if (ce.getPostBootScript() != null)
                    link_ont.addProperty(NdlCommons.requestPostBootScriptProperty, ce.getPostBootScript());
            }
            // vlan
            if (next_Hop.getStaticLabel() != 0) {
                link_ont.addProperty(NdlCommons.RDFS_Label, String.valueOf(next_Hop.getStaticLabel()));
                link_ont.addLiteral(NdlCommons.domainIsAllocatable, next_Hop.isAllocatable());
            }
            // add to collection
            if (next_Hop.getResourceType().getTypeResource() != null)
                link_ont.addProperty(NdlCommons.domainHasResourceTypeProperty,
                        next_Hop.getResourceType().getTypeResource());
            manifest.addProperty(NdlCommons.collectionElementProperty, link_ont);
            logger.debug("CloudHandler:createManifest():Add to domainInConnectionList:" + domainInConnectionList.size() + "link_ont:"
                    + link_ont.getURI() + "in?" + domainInConnectionList.contains(link_ont));
            if (!domainInConnectionList.contains(link_ont))
                domainInConnectionList.add(link_ont);

            logger.debug("CloudHandler:createManifest(): " + i + ":created individual url:" + next_Hop.getURI() + " :Name=" + next_Hop.getName()
                    + " :Individual=" + link_ont);
        }
        logger.debug("CloudHandler::createManifest() OUT");
        return null;
    }

    public void addDomainProperty(Resource d_ont, OntModel manifest) {
        logger.debug("CloudHandler::addDomainProperty() IN");
        if (idm.isClosed()) // closed when modifying
            return;
        OntResource domain_ont = idm.getOntResource(d_ont.getURI());
        Resource pop_rs = null;
        if (domain_ont != null) {
            if (domain_ont.hasProperty(NdlCommons.collectionElementProperty)) {
                for (StmtIterator i = domain_ont.listProperties(NdlCommons.collectionElementProperty); i.hasNext();) {
                    pop_rs = i.next().getResource();
                    if (pop_rs.hasProperty(NdlCommons.locationLocatedAtProperty)) {
                        Resource location_rs = pop_rs.getProperty(NdlCommons.locationLocatedAtProperty).getResource();
                        for (StmtIterator l = location_rs.listProperties(); l.hasNext();) {
                            manifest.add(l.next());
                        }
                        manifest.add(d_ont, NdlCommons.locationLocatedAtProperty, location_rs);
                    }
                }
            }
        }
        logger.debug("CloudHandler::addDomainProperty() OUT");
    }

    public OntModel addSubstrateModel(List<String> modelStr) {
        logger.debug("CloudHandler::addSubstrateModel() IN");
        assert (modelStr != null);

        domainModel = new HashMap<String, OntModel>();
        for (String str : modelStr) {
            ByteArrayInputStream modelStream = new ByteArrayInputStream(str.getBytes());
            Domain domain = null;
            try {
                domain = new Domain(modelStream);
            } catch (IOException e) {
                logger.error("CloudHandler:addSubstrateModel():IO Exception while adding substrate model: " + e);
                e.printStackTrace();
            } catch (NdlException e) {
                logger.error("CloudHandler:addSubstrateModel():NDL Exception while adding substrate model: " + e);
                e.printStackTrace();
            }
            DomainElement domainElement = domain.getDomainElement();
            domainModel.put(domainElement.getURI(), domainElement.getModel());
            logger.info("CloudHandler:addSubstrateModel():Added Domain:" + domainElement.getURI());
            idm.add(domainElement.getModel());
        }
        TDB.sync(idm);
        logger.debug("CloudHandler::addSubstrateModel() OUT");
        return idm;
    }

    public int findCommonLabel(DomainElement start, DomainElement next) {
        logger.debug("CloudHandler::findCommonLabel() IN");
        BitSet startBitSet = null, controllerStartBitSet = null;
        BitSet nextBitSet = null, controllerNextBitSet = null;
        LinkedList<LabelSet> sSetList = null, nSetList = null;
        if (this.globalControllerAssignedLabel != null) {
            startBitSet = this.globalControllerAssignedLabel.get(start.getURI());
            if (next != null)
                nextBitSet = this.globalControllerAssignedLabel.get(next.getURI());
        }
        if (this.controllerAssignedLabel != null) {
            controllerStartBitSet = this.controllerAssignedLabel.get(start.getURI());
            if (next != null)
                controllerNextBitSet = this.controllerAssignedLabel.get(next.getURI());
        }
        DomainResourceType sRType = start.getResourceType(), nRType = next.getResourceType();
        sSetList = start.getLabelSet(sRType.getResourceType());
        if (next != null)
            nSetList = next.getLabelSet(nRType.getResourceType());
        int min = 0, max = 0;
        BitSet sBitSet = new BitSet(max_vlan_tag);
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
            logger.debug("CloudHandler:findCommonLabel():min-max-used:" + min + ":" + max + ":" + sSet.getUsedBitSet());
        }
        logger.debug("CloudHandler:findCommonLabel():----initial Start labelSet:" + sBitSet);
        if (startBitSet != null)
            sBitSet.andNot(startBitSet);
        logger.debug("CloudHandler:findCommonLabel():After globalAssignedLabel + Start labelSet:" + sBitSet);
        if (controllerStartBitSet != null)
            sBitSet.andNot(controllerStartBitSet);
        logger.debug("CloudHandler:findCommonLabel():Final Start labelSet:" + sBitSet);
        BitSet nBitSet = new BitSet(max_vlan_tag);
        if (nSetList != null) {
            for (LabelSet nSet : nSetList) {
                min = (int) nSet.getMinLabel_ID();
                max = (int) nSet.getMaxLabe_ID();

                if ((min == max) || (max == 0)) {
                    nBitSet.set(min);
                } else {
                    nBitSet.set(min, max + 1);
                }
                if ((min == 0) && (max == 0)) {
                    nBitSet.set(0, max_vlan_tag);
                }
                nBitSet.andNot(nSet.getUsedBitSet());
                logger.debug("CloudHandler:findCommonLabel():min-max-used:" + min + ":" + max + ":" + nSet.getUsedBitSet());
            }
            logger.debug("CloudHandler:findCommonLabel():initial next labelSet:" + nBitSet);
            if (nextBitSet != null)
                nBitSet.andNot(nextBitSet);
            logger.debug("CloudHandler:findCommonLabel():After globalAssignedLabel + next labelSet:" + nBitSet);
            if (controllerNextBitSet != null)
                nBitSet.andNot(controllerNextBitSet);
        }
        logger.debug("CloudHandler:findCommonLabel():final next labelSet:" + nBitSet);
        sBitSet.and(nBitSet);

        int commonLabel = -1;
        // in case static label carried over
        int start_static_label = (int) start.getStaticLabel();
        if (start_static_label > 0 && sBitSet.get(start_static_label))
            commonLabel = start_static_label;
        else // otherwise, use the common label
            commonLabel = sBitSet.nextSetBit(0);
        // int commonLabel = randomSetBit(sBitSet);
        if (commonLabel > 0) {
            for (LabelSet sSet : sSetList) {
                sSet.setUsedBitSet(commonLabel);
            }
            if (nSetList != null) {
                for (LabelSet nSet : nSetList) {
                    nSet.setUsedBitSet(commonLabel);
                }
            }
        }
        start.setAvailableLabelSet(sBitSet);
        if (next != null)
            next.setAvailableLabelSet(sBitSet);
        logger.debug("CloudHandler::findCommonLabel() OUT");
        return commonLabel;
    }

}
