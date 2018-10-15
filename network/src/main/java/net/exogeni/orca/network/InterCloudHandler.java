package orca.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetIP2UBI;
import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;
import orca.ndl.DomainResourceType;
import orca.ndl.elements.Device;
import orca.ndl.elements.NetworkConnection;
import orca.shirako.common.ResourceType;
import orca.shirako.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.meta.ResourcePoolDescriptor;
import orca.shirako.meta.ResourcePoolsDescriptor;
import orca.shirako.meta.ResourceProperties;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.util.FileManager;

public class InterCloudHandler extends InterDomainHandler {

    private int numVMSite;
    private int numNetworkService;
    private ArrayList<DomainResourceType> setOfCloudSite;
    private ArrayList<DomainResourceType> setOfTransitSite;

    public boolean cloudRequest = false, interDomainRequest = false;

    public long Default_BW = 100000000;

    public boolean requestBounded = true;

    Hashtable<String, DomainResourceType> domainResourcePools; // List of <domain, rType<type,unit>>
    Hashtable<String, Integer> typeTable; // accumulated units of each resourceType over all sites.
    private LinkedList<Device> cloudPartition;
    private Device masterDevice;
    public int deltaVM, maxVM, secondMaxVM;
    public String maxVMDomain, secondMaxVMDomain;

    OntModel cloudModel; // ontmodel consists of edge cloud domains meshed by shortest path over the inter-domain path.

    public static String gomory_Input_File = "input3.in";
    public static int alpha = 1, beta = 5;
    public static Random randomGenerator = new Random();

    public InterCloudHandler() {
        super();
        // TODO Auto-generated constructor stub
    }

    // Handle the request in RDF
    public Hashtable<String, LinkedList<Device>> handleCloudRequest(String requestFile)
            throws IOException, InetNetworkException {
        mapper.setOntModel(idm);
        // this.requestModel = mapper.ontCreate(requestFile);
        this.requestModel = mapper.addRequest(requestFile);
        return handleCloudRequest(this.requestModel);
    }

    public Hashtable<String, LinkedList<Device>> handleCloudRequest(InputStream requestStream)
            throws IOException, InetNetworkException {
        logger.info("Start the InterCloudHandler..........\n");
        mapper.setOntModel(idm);
        // this.requestModel = mapper.ontCreate(requestStream);
        this.requestModel = mapper.addRequest(requestStream);
        return handleCloudRequest(this.requestModel);
    }

    public Hashtable<String, LinkedList<Device>> handleCloudRequest(OntModel requestModel)
            throws IOException, UnknownHostException, InetNetworkException {
        Hashtable<String, LinkedList<Device>> connectionList = null;
        if (this.mapper == null) {
            logger.error("Inter Cloud controller mapper is NULL!");
            return null;
        }
        interDomainRequest = false;
        cloudRequest = false;
        this.mapper.addRequest(requestModel); // inDomain property is queried here
        this.requestModel = requestModel;
        if (this.mapper.defaultBandwidth != 0)
            Default_BW = this.mapper.defaultBandwidth;
        requestMap = mapper.parseRequest(requestModel);
        cloudRequest = false;
        Hashtable<Integer, String> nodeMap = null;
        // Case 1: Virtual topology
        if (requestMap.size() != 0) {
            nodeMap = generateGraph(requestMap);
            logger.info("This is a topology request, number of VT nodes:" + numVMSite + ":Delta VM:" + deltaVM + "\n");
            if (requestBounded) { // Domain is specified for each node of the requestConnection
                logger.info("Bounded inter-domain VT mapping: number of connections:" + requestMap.size() + "\n");
                connectionList = handleRequest(requestMap);
                interDomainRequest = true;
                NetworkConnection connection = mapper.deviceConnection;
                logger.debug(connection.getEndPoint1_ip() + ":" + connection.getResourceCount1() + ":"
                        + connection.getEndPoint2_ip() + ":" + connection.getResourceCount2());
                if (((connection.getEndPoint1_ip() != null) && (connection.getResourceCount1() >= 1))
                        || ((connection.getEndPoint2_ip() != null) && (connection.getResourceCount2() >= 1))) {
                    // generate reservation for each individual VM in the request
                    generateIDVCConnection(connection, connectionList, true);
                }
            } else {
                if (mapper.domain == null) { //
                    if (numVMSite <= deltaVM) { // but the number of nodes in VT is too small, it is assigned to the
                                                // maxDomain
                        mapper.domain = maxVMDomain; // assign the request to the domain with most available VMs
                        // mapper.domain=secondMaxVMDomain;
                        logger.info("Unbounded request to be assigned to the cloud:" + mapper.domain + "\n");
                        cloudRequest = true;
                    }
                } else {// VT request within one domain
                    cloudRequest = true;
                }
                if (!cloudRequest) {
                    logger.info("We have to partition the unbounded VT and assign to multiple cloud sites.\n");
                    OntModel cloudModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM,
                            buildCloudOntModel());
                    // this.mapper.parseRequest(cloudModel);

                    VTRequestMapping vtMapper = new VTRequestMapping();
                    vtMapper.setOntModel(cloudModel);
                    vtMapper.setSetOfCloudSite(setOfCloudSite);

                    connectionList = vtMapper.handleMapping(nodeMap.size(), requestMap, domainResourcePools);

                    this.setMapper(vtMapper);
                }
            }
        } else {// Case 2: virtual cluster
            logger.info("This is a virtual cluster request:" + "\n");
            requestMap = mapper.parseRequestSite(requestModel);
            if (requestMap.isEmpty()) {
                logger.error("No feasible resource request, quit!");
                return null;
            }
            if (mapper.domain != null) {
                cloudRequest = true;
            } else { // unbounded VC request
                String connectionName, IPAddress;
                NetworkConnection requestConnection = null, newRequestConnection = null;
                DomainResourceType rType = null;
                Device cluster;
                int numVM = 0;
                Hashtable<String, NetworkConnection> newRequestMap = new Hashtable<String, NetworkConnection>();
                // Hashtable <Device,Resource> dependMap=new Hashtable <Device,Resource> ();
                for (Entry<String, NetworkConnection> entry : requestMap.entrySet()) {
                    connectionName = entry.getKey();
                    requestConnection = entry.getValue();
                    cluster = requestConnection.getDevice1();
                    rType = cluster.getResourceType();
                    numVM = rType.getCount();
                    numVMSite += numVM;
                    IPAddress = requestConnection.getEndPoint1_ip(); // cidr format
                    if (IPAddress != null)
                        IPAddress = IPAddress.substring(0, IPAddress.indexOf("/"));
                    // generate a separate connection request for each individual VM, so each of them can be an
                    // individual reservation
                    if (IPAddress != null) {
                        for (int i = 0; i < numVM; i++) {
                            // String group =
                            // requestConnection.getEndPoint1().substring(requestConnection.getEndPoint1().indexOf("#")+1);
                            String group = cluster.getGroup();
                            logger.debug("############## group: " + group + ", IPAddress: " + IPAddress);
                            newRequestConnection = generateRequestConnection(requestConnection, cluster.getVMImageURL(),
                                    cluster.getVMImageGUID(), IPAddress, cluster.getIPNetmask(), i, 0, false, group,
                                    null, cluster.getName());
                            newRequestMap.put(newRequestConnection.getName(), newRequestConnection);
                        }
                    } else {
                        newRequestMap.put(connectionName, requestConnection);
                    }

                }
                String requestEndType = rType.getResourceType().split("#")[1].toLowerCase();
                if (!typeTable.containsKey(requestEndType)) {
                    logger.error("No Available Resource found for the requested edge resource:" + requestEndType + ":"
                            + rType.toString());
                }

                if (numVMSite <= deltaVM) { // but the number of nodes in VT is too small,
                    mapper.domain = maxVMDomain; // assign the request to the domain with most available VMs
                    // mapper.domain=secondMaxVMDomain;
                    logger.info("Unbounded request to be assigned to the cloud:" + mapper.domain + ":" + numVM + ":"
                            + deltaVM + "\n");
                    // IP address defined?, if not, just one VM reservation, otherwise, one reservation for each VM

                    cloudRequest = true;
                } else {// need partition
                    if (numVMSite > typeTable.get(requestEndType)) {
                        logger.error(
                                "Number of requested VM is greater than the total amount of available VM resource in the system!\n");
                        return null;
                    } else {
                        requestMap = newRequestMap;

                        logger.info("Partitioned master-slave request:" + maxVMDomain + ":" + secondMaxVMDomain);
                        Hashtable<String, NetworkConnection> interVCRequestMap = new Hashtable<String, NetworkConnection>();
                        NetworkConnection connection = generateVCConnection(rType);
                        // partition into 2 clouds, so 1 inter-domain request connection
                        interVCRequestMap.put(connection.getName(), connection);
                        connectionList = handleRequest(interVCRequestMap); // get the inter-domain path/dependency tree.
                        generateVMDependency(connectionList);
                    }
                }
            }
        }

        // if the request (vc or VT) specifies the "inDomain"
        if (cloudRequest) {
            if (mapper.domain != null) { // embedding within one domain

                NetworkConnection connection = VCHandler(mapper.domain, this.requestMap, true);

                logger.info("Embedding a VC in the domain:" + this.domain + ":" + connection.getName() + ":"
                        + connection.getConnection().size() + ":" + connection.getDevice1() + "-"
                        + connection.getDevice2() + " BW=" + connection.getBw());

                connectionList = new Hashtable<String, LinkedList<Device>>();
                connectionList.put(connection.getName(), connection.getConnection());

                domainConnectionList = new Hashtable<String, LinkedList<Device>>();
                copyDomainList(domainConnectionList, connectionList); // for Manifest purpose;

                generateIDVCConnection(connection, connectionList, false);

                LinkedList<Device> newDList = connection.getConnection();

                this.getMapper().setDeviceConnection(connection);
            }
        }
        return connectionList;
    }

    public void copyDomainList(Hashtable<String, LinkedList<Device>> domainConnectionList,
            Hashtable<String, LinkedList<Device>> connectionList) {
        LinkedList<Device> newDomainList = new LinkedList<Device>();
        for (Entry<String, LinkedList<Device>> entry : connectionList.entrySet()) {
            newDomainList.addAll(entry.getValue());
            domainConnectionList.put(entry.getKey(), newDomainList);
            for (Device d : entry.getValue()) {
                logger.debug("Copied device:" + d.getName() + " :url=" + d.getURI());
            }
        }
    }

    public void generateIDVCConnection(NetworkConnection connection,
            Hashtable<String, LinkedList<Device>> connectionList, boolean isVT)
            throws InetNetworkException, IOException {
        LinkedList<Device> dList = null;
        LinkedList<Device> newDList = null;
        cloudPartition = new LinkedList<Device>();
        LinkedList<Device> tempDList = new LinkedList<Device>();
        Hashtable<String, NetworkConnection> newRequestMap = null;
        NetworkConnection newRequestConnection = null;
        String ip = null, netmask = null;
        Device commonLinkDevice = null, currentLinkDevice = null;
        Resource currentParentInterface = null;
        boolean vcRequest = false;
        int vmPosition = 0;

        // find the master node device first if nodeDependency
        for (Entry<String, LinkedList<Device>> entry : connectionList.entrySet()) {
            dList = entry.getValue();
            for (Device d : dList) {
                if (d.getNodeDependency() != null) {
                    logger.info("d.nodeDependency=" + d.getNodeDependency() + " ;ip=" + d.getIPAddress());
                    for (Device dMaster : dList) {
                        if (dMaster.getName().equals(d.getNodeDependency().getURI())) {
                            if (d.getIPAddress() != null) {
                                Resource ip_rs = requestModel.createResource(d.getIPAddress());
                                d.setPrecededBy(dMaster, ip_rs);
                                dMaster.setFollowedBy(d, ip_rs);
                                logger.info("Added InteDdomainHandler node dependency: d=" + d.getName() + " ;dMaster="
                                        + dMaster.getName());
                            }
                        }
                    }
                }
            }
        }

        // generate individual requests and adjust the dependency
        for (Entry<String, LinkedList<Device>> entry : connectionList.entrySet()) {
            logger.info("------Current generateIDVCConnection:" + entry.getKey());
            dList = entry.getValue();
            int path_position = 0;
            for (Device d : dList) {
                DomainResourceType dType = d.getResourceType();
                if (dType.getResourceType().endsWith("VM")) { // two end domains
                    if (d.getIPAddress() != null) { // generate new request with IP address for multi unit request at
                                                    // the end
                        logger.info("generateIDVCConnection:" + d.getName() + ":" + d.getResourceType().toString() + ":"
                                + d.getIPAddress() + ":" + d.getIPNetmask() + ":" + d.getUpNeighbour());
                        newRequestMap = new Hashtable<String, NetworkConnection>();
                        ip = d.getIPAddress();
                        netmask = d.getIPNetmask();
                        if (d.getIPAddress().indexOf("/") >= 0) {
                            ip = d.getIPAddress().split("/")[0];
                            netmask = d.getIPAddress().split("/")[1].split("@")[0];
                        }

                        // node group dependency, implies all the VMs reservation will share the same vlan, which
                        // implies it's a VC request within a domain
                        if (d.getNodeDependency() != null) {
                            logger.info("Implies a VC request with node group dependency!:"
                                    + d.getNodeDependency().getURI());
                            vcRequest = true;
                        }
                        if (d.getResourceType().getCount() > 1) {
                            logger.info("Implies a multiple VM requests!:" + d.getResourceType().getCount());
                            vcRequest = true;
                        }
                        // first serverCloud node
                        if (vmPosition == 0) {
                            logger.info("First device:" + d.getName());
                            if (d.getPrecededBySet() != null) {
                                for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
                                    commonLinkDevice = parent.getKey();
                                    if (commonLinkDevice.getResourceType().getResourceType().endsWith("VLAN")) {
                                        logger.info("First Device's common Link:" + commonLinkDevice.getName());
                                        break;
                                    }
                                }
                            }
                        } else {
                            if (vcRequest == true) {
                                // replace the link parent with the commonLinkDevice
                                for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
                                    currentLinkDevice = parent.getKey();
                                    if (currentLinkDevice.getResourceType().getResourceType().endsWith("VLAN")) {
                                        currentParentInterface = parent.getValue();
                                        break;
                                    }
                                }
                                logger.info("Parent Common Link Device:" + commonLinkDevice.getName() + ":"
                                        + currentLinkDevice.getName() + ":" + currentParentInterface);

                                if ((!currentLinkDevice.getName().equals(commonLinkDevice.getName()))
                                        && (currentLinkDevice.getResourceType().getResourceType().endsWith("VM"))) {
                                    cloudPartition.add(currentLinkDevice);
                                    d.setPrecededBy(commonLinkDevice, currentParentInterface);
                                    commonLinkDevice.setFollowedBy(d, currentParentInterface);
                                    commonLinkDevice.getFollowedBy().remove(d);
                                    d.getPrecededBy().remove(currentLinkDevice);
                                    // d.setUpNeighbour(currentParentInterface);
                                }
                            }
                        }
                        logger.info("Resource Type:" + d.getResourceType().toString());
                        if (d.getResourceType().getCount() > 1) {
                            if (d.getIPAddress() != null) {
                                for (int i = 0; i < d.getResourceType().getCount(); i++) {
                                    String group = d.getGroup();// getName().substring(d.getName().indexOf("#")+1);
                                    newRequestConnection = generateRequestConnection(connection, d.getVMImageURL(),
                                            d.getVMImageGUID(), ip, netmask, i, path_position, isVT, group,
                                            d.getPostBootScript(), d.getName());
                                    newRequestMap.put(newRequestConnection.getName(), newRequestConnection);
                                }
                            }
                            NetworkConnection vcConnection = VCHandler(d.getUri(), newRequestMap, false);
                            newDList = vcConnection.getConnection();

                            adjustDependency(newDList, d, 0);
                            tempDList.addAll(newDList);
                            // get cloudDeviceList of x1 and x2
                            cloudPartition.add(d);
                        }
                        for (Device dd : cloudPartition) {
                            logger.info("cloudPartition:" + dd.getName() + ":" + dd.getResourceType().toString());
                        }
                    }
                    vmPosition++;
                }
                path_position++;
            }
            for (Device d : dList) {
                logger.debug("New DList:" + d.getName() + ":" + d.getResourceType().toString());
            }

            // remove the original 2 cloud sites reservation from the dependency DAG
            for (Device old : cloudPartition) {
                dList.remove(old);
            }
            dList.addAll(tempDList);
        }
        connection.setConnection(dList);
    }

    // generate the reservation dependency due to the master-slave relationship, after the inter-cloud connection
    // computation
    // replace the VC reservation in each cloud with the group of VM reservation
    public void generateVMDependency(Hashtable<String, LinkedList<Device>> connectionList) throws IOException {
        // split the vm reservation to multiple
        cloudPartition = new LinkedList<Device>();
        LinkedList<Device> dList = null;
        for (Entry<String, LinkedList<Device>> entry : connectionList.entrySet()) {
            dList = entry.getValue();
            for (Device d : dList) {
                DomainResourceType dType = d.getResourceType();

                if (dType.getResourceType().endsWith("VM")) { // two end domains
                    logger.debug("Cloud partition:" + d.getUri() + ":" + dType.toString());

                    // get cloudDeviceList of x1 and x2
                    cloudPartition.add(d);
                }
            }
        }
        Collections.sort(cloudPartition); // ascending order according to the rType.count.
        // dList+cloudDeviceList
        int partition = 1, position = 0;
        for (Device d : cloudPartition) {
            Hashtable<String, NetworkConnection> vcRequestMap = getVCRequestMap(d, requestMap, partition,
                    cloudPartition.size(), position);
            logger.debug("Cloud partition provisioning:" + d.getUri() + ":" + d.getName() + ":"
                    + d.getResourceType().toString() + ";subRequest size:" + partition + ":" + vcRequestMap.size() + ":"
                    + position + ":" + requestMap.entrySet().size());
            NetworkConnection vcConnection = VCHandler(d.getUri(), vcRequestMap, false);
            LinkedList<Device> newDList = vcConnection.getConnection();

            adjustDependency(newDList, d, partition);

            dList.addAll(newDList);
            partition++;
            position = +d.getResourceType().getCount();
        }
        // remove the original 2 cloud sites reservation from the dependency DAG
        for (Device old : cloudPartition) {
            dList.remove(old);
        }
    }

    //
    public void adjustDependency(LinkedList<Device> newDList, Device d, int partition) {
        // add original dependency parent from inter-domain path.
        String newIP = null;
        for (Device child : newDList) {
            // System.out.println("New
            // dList:"+child.getName()+":"+child.getUri()+":"+child.getIPAddress()+":dIPAddress:"+d.IPAddress);
            newIP = child.getIPAddress();
            newIP = newIP;// .concat("/").concat(child.getIPNetmask());
            if (d.getIPAddress() != null) {
                if (d.getIPAddress().indexOf("@") >= 0) {
                    newIP = newIP.concat("@").concat(d.getIPAddress().split("@")[1]);
                }
            }
            Resource nIP = requestModel.createResource(newIP);
            if (d.getPrecededBy() != null) {
                for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
                    logger.info("Inherite the parent:" + parent.getKey().getName() + ";rType:"
                            + parent.getKey().getResourceType().toString());
                    if (parent.getKey().getResourceType().getResourceType().endsWith("VM")) {
                        child.setPrecededBy(parent.getKey(), parent.getValue());
                    }
                    if (parent.getKey().getResourceType().getResourceType().endsWith("VLAN")) {
                        child.setPrecededBy(parent.getKey(), nIP);
                    }
                    parent.getKey().setFollowedBy(child, nIP);
                }
            }

            // add the master-slave dependency to partions other than the master partition,
            // this is only for the inter-cloud case. "masterDevice" is set by this:getVCRequestMap
            if ((partition > 0) && (masterDevice != null)) {
                logger.info("Add master node dependency:" + masterDevice.getName());
                if (!child.getName().equals(masterDevice.getName())) {
                    child.setPrecededBy(masterDevice, requestModel.createResource(masterDevice.getIPAddress()));
                    masterDevice.setFollowedBy(child, requestModel.createResource(child.getIPAddress()));
                }
            }
        }

        // remove the original children from original parents
        if (d.getPrecededBy() != null) {
            LinkedList<Device> childList = new LinkedList<Device>();
            for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
                // System.out.println("Precedded By:"+parent.getKey()+":"+parent.getValue());
                for (Device c : parent.getKey().getFollowedBy().keySet()) {
                    if (d == c) {
                        childList.add(d);
                    }
                }
                for (Device newChild : childList) {
                    parent.getKey().getFollowedBy().remove(newChild);
                }

            }
        }
    }

    // generate intra-cloud provisioning request connection for CloudHandler
    public Hashtable<String, NetworkConnection> getVCRequestMap(Device d,
            Hashtable<String, NetworkConnection> requestMap, int partition, int numPartition, int position) {
        Hashtable<String, NetworkConnection> vcRequestMap = new Hashtable<String, NetworkConnection>();
        Device cluster;
        int starting = 0;
        logger.debug("getVCRequestMap:" + partition + ":" + numPartition + ":" + position + ":" + starting + ":"
                + requestMap.entrySet().size() + "---d:" + d.getResourceType().toString());
        for (Entry<String, NetworkConnection> entry : requestMap.entrySet()) {
            starting++;
            if (requestMap.size() > 1) {
                if (starting <= position) {
                    continue;
                }
            }
            entry.getValue().setEndPoint1_domain(d.getUri());
            if (requestMap.size() == 1) {
                entry.getValue().setResourceCount1(d.getResourceType().getCount());
                entry.getValue().setDevice1(d);
            }

            cluster = entry.getValue().getDevice1();
            if (cluster.getUpNeighbour() == null) { // the master node
                masterDevice = cluster;
                logger.info("This is the master node:" + cluster.getUri() + ":" + cluster.getName() + ":"
                        + cluster.getIPAddress());
            }
            cluster.setUri(d.getUri());

            vcRequestMap.put(entry.getKey(), entry.getValue());

            if (vcRequestMap.size() == d.getResourceType().getCount()) {
                break;
            }
        }

        return vcRequestMap;
    }

    // generate a inter-cloud request connection for routing using InterdomainHandler
    public NetworkConnection generateVCConnection(DomainResourceType rType) {
        NetworkConnection connection = new NetworkConnection();
        int x1 = (maxVM - secondMaxVM + numVMSite) / 2;
        int x2 = numVMSite - x1;
        String temp_prefix = this.mapper.reservation.getNameSpace();
        String connectionName = temp_prefix + "Connection";
        Resource rs_connection = requestModel.createResource(connectionName);
        String rs0_str = temp_prefix + "Master", rs1_str = temp_prefix + "Slave";

        connection.setResource(rs_connection);
        connection.setName(connectionName);

        connection.setEndPoint1_domain(maxVMDomain);
        connection.setEndPoint2_domain(secondMaxVMDomain);

        connection.setEndPoint1(rs0_str);
        connection.setEndPoint2(rs1_str);
        Resource rs0 = requestModel.createResource(rs0_str);
        rs0.addProperty(mapper.inDomain, requestModel.getResource(maxVMDomain));
        Resource rs1 = requestModel.createResource(rs1_str);
        rs1.addProperty(mapper.inDomain, requestModel.getResource(secondMaxVMDomain));
        connection.setResourceCount1(x1);
        connection.setEndPoint1_type(rType.getResourceType());
        connection.setResourceCount2(x2);
        connection.setEndPoint2_type(rType.getResourceType());

        connection.setBw(Default_BW);

        // default connection type is VLAN
        connection.setType("http://geni-orca.renci.org/owl/domain.owl#VLAN");

        logger.info("InterCloud VC connection:" + x1 + ":" + x2 + ":" + numVMSite + ";" + connectionName + ":" + rs0_str
                + ":" + rs1_str + ":" + connection.getEndPoint1_domain() + ":" + connection.getEndPoint2_domain());

        return connection;
    }

    // generate a separate request connection for each VM because of the IP address assignment
    public NetworkConnection generateRequestConnection(NetworkConnection requestConnection, String imageURL,
            String imageGUID, String IPAddress, String netmask, int i, int path_position, boolean isVT, String group,
            String postBootScript, String dName) throws InetNetworkException, UnknownHostException {
        NetworkConnection connection = new NetworkConnection();
        connection.setName(requestConnection.getName() + "/" + String.valueOf(i));
        connection.setBw(requestConnection.getBw());
        connection.setType(requestConnection.getType());
        connection.setResourceCount1(1);

        // System.out.println("begining:"+requestConnection.getType()+":"+requestConnection.getDevice1()+":"+path_position);

        InetAddress ip = InetAddress.getByName(IPAddress.split("\\/")[0]);
        BigInteger biIP = InetIP2UBI.convertIP2UBI(ip.getAddress());
        BigInteger nbiIP = biIP.add(new BigInteger(new Integer(i).toString()));
        InetAddress nIP = InetIP2UBI.convertUBI2IP(nbiIP, 4);

        InetNetwork nnIP = new InetNetwork(nIP.toString().substring(1), netmask);
        connection.setEndPoint1_ip(nnIP.networkIdentifierCIDR());
        Device device = null;
        Device cluster = new Device();
        if (path_position != 0) {
            device = requestConnection.getDevice2();
            // connection.setEndPoint1(requestConnection.getEndPoint2()+"/"+connection.getEndPoint2_ip());
            connection.setEndPoint1(dName + "/" + connection.getEndPoint1_ip());
        }
        if ((path_position == 0) || (device == null)) {
            device = requestConnection.getDevice1();
            // connection.setEndPoint1(requestConnection.getEndPoint1()+"/"+connection.getEndPoint1_ip());
            connection.setEndPoint1(dName + "/" + connection.getEndPoint1_ip());
        }

        if (device == null) {
            logger.error("Device in the path connection edge is NULL!");
        }

        DomainResourceType type = new DomainResourceType();

        type.setResourceType(device.getResourceType().getResourceType());
        type.setCount(1);
        cluster.setResourceType(type);
        cluster.setName(connection.getEndPoint1());
        cluster.setResource(requestModel.createResource(cluster.getName()));
        cluster.setIPAddress(nIP.toString().substring(1));
        cluster.setIPNetmask(netmask);
        if (!isVT) {
            cluster.setUpNeighbour(device.getUpNeighbour());
        }
        cluster.setVMImageGUID(imageGUID);
        cluster.setVMImageURL(imageURL);

        if (postBootScript == null) {
            cluster.setPostBootScript(device.getPostBootScript());
        } else {
            cluster.setPostBootScript(postBootScript);
        }

        // Super hack to get postBootScripts working
        if (group == null) {
            logger.error("The grooup name was not being set from the request!");
            group = connection.getEndPoint1().substring(connection.getEndPoint1().indexOf("#") + 1);
            group = group.substring(0, group.indexOf("/"));
            // for single node groups
            cluster.setGroup(group);
            device.setGroup(group);
        } else {
            // for multi-node groups
            cluster.setGroup(group);
        }

        connection.setDevice1(cluster);

        // System.out.println("New
        // request:"+connection.name+":"+connection.getEndPoint1_ip()+":"+cluster.resource+":"+type);

        return connection;
    }

    // Get the domain resource pools from SM<-broker.
    public void getDomainResourcePools(ResourcePoolsDescriptor pools) {
        typeTable = new Hashtable<String, Integer>();
        String pureType = null, domainName = null;
        int count = 0;
        domainResourcePools = new Hashtable<String, DomainResourceType>();
        for (ResourcePoolDescriptor rpd : pools) {
            ResourceType type = rpd.getResourceType();
            DomainResourceType dType = new DomainResourceType();
            dType.setResourceType(type.getType());

            // logger.debug("******From Resource Pool:"+
            // rpd.getResourceType().getType()+":"+rpd.getAttribute(ResourceProperties.ResourceAvailableUnits).getIntValue());
            dType.setCount(rpd.getAttribute(ResourceProperties.ResourceAvailableUnits).getIntValue());
            // dType.setCount(rpd.getUnits());
            // logger.debug("From Resource Pool:"+ rpd.getResourceType().getType()+":"+rpd.getUnits());

            pureType = type.getType().split("\\.")[1];
            if (typeTable.containsKey(pureType)) {
                count = typeTable.get(pureType);
                count = count + dType.getCount();
                typeTable.remove(pureType);
                typeTable.put(pureType, count);
            } else {
                typeTable.put(pureType, dType.getCount());
            }
            logger.debug("Pure Type:" + pureType + ":" + typeTable.get(pureType));
            ResourcePoolAttributeDescriptor a = rpd.getAttribute(ResourceProperties.ResourceDomain);

            if (a == null) {
                throw new RuntimeException("Missing domain information for resource pool:  " + type);
            }
            domainName = DomainResourceType.generateDomainResourceName(a.getValue());
            if (pureType.equalsIgnoreCase("vm")) {
                if (maxVM <= dType.getCount()) {
                    maxVM = dType.getCount();
                    maxVMDomain = domainName;
                } else {
                    if (secondMaxVM < dType.getCount()) {
                        secondMaxVM = dType.getCount();
                        secondMaxVMDomain = domainName;
                    }
                }
                logger.debug("Find Delta:" + maxVM + ":" + secondMaxVM + ":" + dType.getCount() + "\n");
            }
            logger.debug("Domain Resource Pool: " + a.getValue() + " with " + dType.toString() + "-:" + domainName);
            domainResourcePools.put(domainName, dType);

            a = rpd.getAttribute(ResourceProperties.ResourceNdlAbstractDomain);
            if (a == null) {
                logger.debug("Found no abstract model for resource pool: " + type);
            }
        }
        // Fixme: Temporary fix when no available resource pools correctly queried from broker
        if (maxVM == 0) {
            deltaVM = 1000;
            logger.error("no available resource pools queried from broker" + maxVMDomain + "\n");
        } else {
            deltaVM = (int) (maxVM - Math.ceil((double) secondMaxVM / 3));
        }
    }

    // Build the ontmodel graph of edge cloud sites interconnected by interdomain paths
    public InfModel buildCloudOntModel() {
        setOfCloudSite = new ArrayList<DomainResourceType>();
        setOfTransitSite = new ArrayList<DomainResourceType>();

        cloudModel = ModelFactory.createOntologyModel();
        Model schemaModel = FileManager.get().loadModel("http://geni-orca.renci.org/owl/domain.owl");

        OntModel cloudModelBase = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        Reasoner cloudModelReasoner = cloudModelBase.getReasoner();
        cloudModelReasoner = cloudModelReasoner.bindSchema(schemaModel);

        InfModel cloudInf = ModelFactory.createInfModel(cloudModelReasoner, cloudModel);
        String sourceDomain = null, destinationDomain = null;
        Resource c_s_rs = null, c_d_rs;
        String domainURL = null;
        for (Entry<String, DomainResourceType> entry : domainResourcePools.entrySet()) {
            domainURL = entry.getKey();
            // System.out.println("Substrate site:"+entry.getKey()+":"+domainURL+"\n");
            if (entry.getValue().getResourceType().endsWith("VM")) {
                domainURL = domainURL + "/vm";
                entry.getValue().setDomainURL(domainURL);
                this.setOfCloudSite.add(entry.getValue());
            }
            if (entry.getValue().getResourceType().endsWith("VLAN")) {
                domainURL = domainURL + "/vm";
                entry.getValue().setDomainURL(domainURL);
                this.setOfTransitSite.add(entry.getValue());
            }
        }
        int size = this.setOfCloudSite.size();
        int i = 0, j = 0;
        long pathMinBW = 0;
        Resource source_rs, destination_rs;
        Resource source_intf_rs, destination_intf_rs;
        String rType = "http://geni-orca.renci.org/owl/domain.owl#VLAN";
        ArrayList<ArrayList<OntResource>> path = null;
        for (i = 0; i < size; i++) {
            sourceDomain = this.setOfCloudSite.get(i).getDomainURL();
            source_rs = idm.getResource(sourceDomain);
            for (j = i + 1; j < size; j++) {
                destinationDomain = this.setOfCloudSite.get(j).getDomainURL();
                destination_rs = idm.getResource(destinationDomain);

                path = mapper.findShortestPath(idm, source_rs, destination_rs, 0, rType);
                pathMinBW = mapper.minBW(path);
                int path_len = path.size();
                c_s_rs = cloudInf.createResource(path.get(0).get(0).getURI());
                source_intf_rs = cloudInf.createResource(path.get(0).get(1).getURI());
                c_s_rs.addProperty(mapper.hasInterface, source_intf_rs);
                source_intf_rs.addProperty(mapper.interfaceOf, c_s_rs);
                source_intf_rs.addLiteral(this.mapper.bandwidth, pathMinBW);
                source_intf_rs.addLiteral(this.mapper.numHop, path_len / 2);

                c_d_rs = cloudInf.createResource(path.get(path_len - 1).get(0).getURI());
                destination_intf_rs = cloudInf.createResource(path.get(path_len - 1).get(1).getURI());
                c_d_rs.addProperty(mapper.hasInterface, destination_intf_rs);
                destination_intf_rs.addProperty(mapper.interfaceOf, c_d_rs);
                destination_intf_rs.addLiteral(this.mapper.bandwidth, pathMinBW);
                destination_intf_rs.addLiteral(this.mapper.numHop, path_len / 2);

                source_intf_rs.addProperty(mapper.connectedTo, destination_intf_rs);
                destination_intf_rs.addProperty(mapper.connectedTo, source_intf_rs);
                // System.out.println("Meshed
                // link:"+path.get(0).get(0)+":"+path.get(path_len-1).get(0)+":"+path_len+"\n");
                logger.debug(
                        "Cloud:" + c_s_rs + ":" + c_d_rs + ":" + source_intf_rs.getProperty(this.mapper.numHop).getInt()
                                + ":" + destination_intf_rs.getProperty(this.mapper.bandwidth).getLong() + "\n");
            }
        }
        // cloudInf.write(System.out);
        return cloudInf;
    }

    // convert the request topology into graph with node numbered by sn.
    public Hashtable<Integer, String> generateGraph(Hashtable<String, NetworkConnection> connectionList)
            throws IOException {

        if (connectionList.isEmpty())
            return null;
        numNetworkService = connectionList.size();
        numVMSite = 0;
        Hashtable<Integer, String> nodeMap = new Hashtable<Integer, String>();

        String rs1_str = null, rs2_str = null;
        String connectionName = null;
        NetworkConnection requestConnection = null;
        // numbering the nodes using the integer stqrting from 1
        for (Entry<String, NetworkConnection> entry : connectionList.entrySet()) {
            requestConnection = entry.getValue();
            rs1_str = requestConnection.getEndPoint1();
            rs2_str = requestConnection.getEndPoint2();

            if (!nodeMap.containsValue(rs1_str)) {
                numVMSite++;
                nodeMap.put(numVMSite, rs1_str);
                if (requestConnection.getEndPoint1_domain() == null) {
                    logger.info("Unbounded:" + rs1_str + ":" + requestConnection.getEndPoint1_domain());
                    requestBounded = false;
                }
            }
            requestConnection.setSn1(getNodeMapKey(nodeMap, rs1_str));
            if (rs2_str != null) {
                if (!nodeMap.containsValue(rs2_str)) {
                    numVMSite++;
                    nodeMap.put(numVMSite, rs2_str);
                    if (requestConnection.getEndPoint2_domain() == null) {
                        logger.info("Unbounded:" + rs2_str + ":" + requestConnection.getEndPoint2_domain());
                        requestBounded = false;
                    }
                }
                requestConnection.setSn2(getNodeMapKey(nodeMap, rs2_str));
            }
            // not inter-domain request
            if ((requestConnection.getEndPoint1_domain() != null)
                    && (requestConnection.getEndPoint2_domain() != null)) {
                if (requestConnection.getEndPoint1_domain().equals(requestConnection.getEndPoint2_domain())) {
                    requestBounded = false;
                    this.mapper.domain = requestConnection.getEndPoint1_domain();
                } else {
                    requestBounded = true;
                }
            }
        }

        outputGraph(gomory_Input_File, numVMSite, connectionList);

        return nodeMap;
    }

    // mapping node url and sn
    public Integer getNodeMapKey(Hashtable<Integer, String> nodeMap, String rs) {
        Integer sn = 0;
        for (Entry<Integer, String> entry : nodeMap.entrySet()) {
            if (rs == entry.getValue()) {
                sn = entry.getKey();
                break;
            }
        }
        return sn;
    }

    // output the graph to the format file as the input to the gomory code
    public void outputGraph(String outputFile, int numNode, Hashtable<String, NetworkConnection> connectionList)
            throws IOException {
        Writer output = null;

        File file = new File(outputFile);
        output = new BufferedWriter(new FileWriter(file));
        output.write(numNode + "\n");
        output.write(connectionList.size() + "\n");

        NetworkConnection requestConnection = null;
        int rs1_sn = 0, rs2_sn = 0, i = 1;
        long bw = 0;
        for (Entry<String, NetworkConnection> entry : connectionList.entrySet()) {
            requestConnection = entry.getValue();
            rs1_sn = requestConnection.getSn1();
            rs2_sn = requestConnection.getSn2();
            bw = requestConnection.getBw();
            if (bw == 0)
                bw = randomGenerator.nextInt(150) + 1;
            output.write(rs1_sn + " " + rs2_sn + " " + bw + "\n");
            i++;
        }

        output.close();
    }

    public int getNumVMSite() {
        return numVMSite;
    }

    public void setNumVMSite(int numVMSite) {
        this.numVMSite = numVMSite;
    }

    public int getNumNetworkService() {
        return numNetworkService;
    }

    public void setNumNetworkService(int numNetworkService) {
        this.numNetworkService = numNetworkService;
    }

}
