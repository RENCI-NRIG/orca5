package orca.network;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.log4j.Logger;

import orca.policy.core.util.PropertiesManager;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ResourceType;
import orca.shirako.container.Globals;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.meta.ConfigurationProperties;
import orca.shirako.meta.RequestProperties;
import orca.shirako.meta.UnitProperties;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.plugins.substrate.ISubstrateDatabase;
import orca.shirako.time.Term;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import java.util.Map;
import orca.ndl.*;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchingAction;

public class ReservationConverter implements LayerConstant {

    public static final String NO_SSH_KEY_SPECIFIED_STRING = "NO-SSH-KEY-SPECIFIED";
    public static final String PropertyRequestNdl = "request.ndl";
    public static final String PropertyConfigDuration = "config.duration";
    public static final String PropertyConfigUnitTag = "config.unit.tag";
    protected static String noopConfigFile = Globals.LocalRootDirectory + "/handlers/common/noop.xml";
    public static final String PropertyDefaultBrokerName = "controller.defaultBroker";
    static String masterHostName = "";
    public Logger logger;

    public ReservationConverter(Logger logger) {
        super();
        this.logger = logger;
    }

    public ReservationConverter() {
        super();
        logger = Logger.getLogger(this.getClass());
    }

    public class ReservationRequest {
            

        public String domain;
        public IServiceManagerReservation reservation;
        public boolean isNetwork;
        public boolean isVM;
        public int networkDependencies;
    }

    public List<IServiceManagerReservation> getReservations(InterDomainHandler handler, HashMap<String, ResourceType> typesMap, String sshKey, Term term, IServiceManager sm, ISlice slice) {
        LinkedList<Device> list = handler.getMapper().getDeviceConnection().getConnection();
        HashMap<String, ReservationRequest> map = new HashMap<String, ReservationRequest>();
        //IBrokerProxy brokerProxy = sm.getDefaultBroker();   
        IBrokerProxy brokerProxy = null;
        String brokerName = Globals.getConfiguration().getProperty(PropertyDefaultBrokerName);
        if (brokerName != null) {
            brokerProxy = sm.getBroker(brokerName);
        } else {
            brokerProxy = sm.getDefaultBroker();
        }

        Resource reservation = handler.getMapper().reservation;
        String vmImageURL = handler.getMapper().getVMImageURL(reservation);
        String vmImageGUID = handler.getMapper().getVMImageGUID(reservation);
        long termDuration = handler.getMapper().termDuration;
        
        //vars for ScriptConstructor
        Map<String,ArrayList<String>> scriptMap = new HashMap<String,ArrayList<String>>(); //maps group name to a list of ips
        //String master_ip = null;
        ArrayList<String> ipList;  //= new ArrayList<String>();

        try{
        
        // first pass: make the reservation objects
        int i=0;
        for (Device d : list) {
        	i++;
            String group = null;
            String ipaddr = null;
            
            group = d.getGroup();
            //group = d.getName().substring(d.getName().indexOf("#")+1);
            
            if(d.getIPAddress() != null){
		if(d.getIPAddress().indexOf("/")>0)
                	ipaddr = d.getIPAddress().substring(0,d.getIPAddress().indexOf("/"));
		else
			ipaddr = d.getIPAddress();
            } 
            logger.debug("group: " + group + ", ipaddr: " + ipaddr);
            
            
            
            String domain = getDomainName(d);
            if (domain == null) {
                throw new RuntimeException("getReservations: Invalid domain name");
            }

            
            // get the NDL request for that domain
            OntModel model = handler.domainRequest(d);
            d.setIdmRequest(model);
            OutputStream out = new ByteArrayOutputStream();
            
            
            model.write(out);
           
            
            
            String ndlRequest = out.toString();
            
            
            
            if ((ndlRequest == null)
                    || (ndlRequest.length() == 0)) {
                throw new RuntimeException("getReservations: ndlRequest is null or zero-length");
            }
          

            // what type can we get from that domain
            ResourceType type = typesMap.get(domain);
            logger.debug("getReservations(): d.url:domain: " + d.getURI() + ":" + domain + " | type: " + type);
            if (type == null) {
                throw new RuntimeException("getReservations: Invalid resource type for domain: " + domain);
            }

            logger.debug("getReservations(): d.getResourceType() : " + d.getResourceType().getResourceType() + " count " + d.getResourceType().getCount());
            ResourceSet rset = new ResourceSet(d.getResourceType().getCount(), type);

            if (rset == null) {
                System.out.println("Unable to create resource set");
            }

            // create the reservation
            IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance().create(rset, term, slice, brokerProxy);

            if (r == null) {
                System.out.println("Unable to create ServiceManagerReservation");
            }

            // no SM-side handler
            r.setLocalProperty(AntConfig.PropertyXmlFile, noopConfigFile);
            // pass the ssh key to the authority
            if (sshKey != null) {
                r.setConfigurationProperty(ConfigurationProperties.ConfigSSHKey, sshKey);
            } else {
                r.setConfigurationProperty(ConfigurationProperties.ConfigSSHKey, NO_SSH_KEY_SPECIFIED_STRING);
            }
            // pass the NDL request to the authority
            // NOTE: for now, our broker is not aware of NDL, only some of the
            // sites, e.g., BEN know how to handle NDL.
            r.setConfigurationProperty(PropertyRequestNdl, ndlRequest);
            r.setConfigurationProperty(PropertyConfigDuration,String.valueOf(termDuration));
            int staticLabel = (int) d.getStaticLabel();
            if(staticLabel>0){
            	r.setConfigurationProperty(PropertyConfigUnitTag,String.valueOf(staticLabel));
            	logger.debug("Static Tag:"+staticLabel + "---------- for domain:" +domain+"\n");
	    }
            r.setRenewable(false);
            PropertiesManager.setElasticTime(rset, false);
            ReservationRequest request = new ReservationRequest();
            request.reservation = r;
            request.domain = domain;
            // is this a reservation for a network resource?
            request.isNetwork = type.toString().endsWith(".vlan");
            request.isVM = type.toString().endsWith(".vm");
            //map.put(domain, request);

            String newName=d.getName()+"/"+UUID.randomUUID().toString();
            d.setName(newName);
            map.put(d.getName(), request);

            if (request.isVM) {
                r.setConfigurationProperty("unit.hostname.url", d.getName());
                if (d.getVMImageURL() != null) {
                    vmImageURL = d.getVMImageURL();
                }
                if (d.getVMImageGUID() != null) {
                    vmImageGUID = d.getVMImageGUID();
                }
                if (vmImageURL != null) {
                    r.setConfigurationProperty(ConfigurationProperties.ConfigImageUrl, vmImageURL);
                }
                if (vmImageGUID != null) {
                    r.setConfigurationProperty(ConfigurationProperties.ConfigImageGuid, vmImageGUID);
                }

                //add ip to scriptMap 
                if(!scriptMap.containsKey(group)){
                    //create new list and add to map
                    scriptMap.put(group, new ArrayList<String>());
                }     
                //add ip to existing list
                scriptMap.get(group).add(ipaddr);
               
            }

            if (request.isNetwork) {
                // to broker: set the bandwidth, start/end interface
                // to site: set the config names for the interfaces that need to be configured                
                r.setConfigurationProperty("unit.vlan.url", d.getName());
		LinkedList<SwitchingAction> actions = d.getActionList();
                if (actions != null) {
                    String from = null;
                    String to = null;
                    String bw = null;
                    if (actions.size() > 1) {
                        throw new RuntimeException("More than one switching action for domain: " + domain);
                    }

                    SwitchingAction a = actions.getFirst();

                    if (a.getDefaultAction() == null) {
                        logger.info("Default action is null!");
                        continue;
                    }
                    if (!a.getDefaultAction().equals(LayerConstant.Action.VLANtag.toString())) {
                        continue;
                    }

                    LinkedList<Interface> ifs = a.getSwitchingInterface();
                    /*
                    int count = 0;
                    for (Interface iff : ifs) {
                    if (iff.getName() != null) {
                    r.setConfigurationProperty("config.interface." + (count+1), iff.getName());
                    System.out.println("domain=" + domain + " setting property config.interface." + (count+1) + "=" + iff.getName());
                    count++;
                    }
                    }*/

                    int count = 0;
                    String ports = null;
                    if (ifs == null) {
                        logger.info("Interface list is empty for this action!");
                    } else {
                        for (Interface iff : ifs) {
                            if (iff == null) {
                                logger.info("Interface is null for this interface list!");
                            }

                            if (iff.getName() != null) {
                                if (ports == null) {
                                    ports = iff.getName();
                                } else {
                                    ports = ports.concat("," + iff.getName());
                                }
                                r.setConfigurationProperty("config.interface." + (count + 1), iff.getName());
                                logger.debug("domain=" + domain + " setting property config.interface." + (count + 1) + "=" + iff.getName());
                                count++;
                            }
                        }
                    }
                    r.setConfigurationProperty("config.interface.ports", ports);

                    bw = String.valueOf(a.getBw());
                    from = ifs.get(0).getURI();
                    if (count > 1) {
                        to = ifs.get(1).getURI();
                    }
		
		    if(r.getResources()!=null)
			if(r.getResources().getConfigurationProperties()!=null)
				logger.debug("config.interface.ports = " +r.getResources().getConfigurationProperties().getProperty("config.interface.ports")+ "\n");	
                    logger.debug("From:" + from + " To: " + to + " BW:" + bw + ":" + ports + "\n");
                    if (bw != null) {
                        if (from == null && to == null) {
                            throw new RuntimeException("Bandwidth requested, but no interface specified for domain: " + domain);
                        }

                        // set the bandwidth
                        r.setRequestProperty(RequestProperties.RequestBandwidth, bw);
                        logger.debug("Request Properties:" + RequestProperties.RequestBandwidth + ":" + bw);
                        // start interface
                        if (from != null) {
                            r.setRequestProperty(RequestProperties.RequestStartIface, from);
                        }

                        // end interface
                        if (to != null) {
                            r.setRequestProperty(RequestProperties.RequestEndIface, to);
                        }
                    }
                }
            }
            
           
               
        }

        
          } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        
        //Hack for testing
        //criptMap.put("CondorMaster", new ArrayList<String>());
        //scriptMap.get("CondorMaster").add("172.16.42.100");
        
       	logger.debug("XXXXXXXXXXXXXXXXXXXXXXXX scriptMap: " + scriptMap);
        
        // second pass: set the dependencies
       
        logger.info("Second Pass");

        for (Device d : list) {
            String group = null;
            String ipaddr = null;
            int ipaddr_index = -1;
            
            String netmask = d.getIPNetmask();
            
            group = d.getGroup();
            if(d.getIPAddress() != null){
		if(d.getIPAddress().indexOf("/")>0)
                	ipaddr = d.getIPAddress().substring(0,d.getIPAddress().indexOf("/"));
                else
			ipaddr = d.getIPAddress();
		ipaddr_index = scriptMap.get(group).indexOf(ipaddr);
            } 
            logger.debug("group: " + group + ", ipaddr: " + ipaddr + ", ipaddr_index: " + ipaddr_index);
            
            String domain = getDomainName(d);
            //ReservationRequest r = map.get(domain);
            ReservationRequest r = map.get(d.getName());
            if (r == null) {
                throw new RuntimeException("Missing reservation from domain " + domain);
            }
            logger.debug("from " + r.domain + " units=" + r.reservation.getUnits() + " type=" + r.reservation.getType());
            Hashtable<Device, Resource> preds = d.getPrecededBy();
            if (preds == null) {
                continue;
            }
            
           
            //handle postBootScript
            if(r.isVM){
                 
                 String bootScript = d.getPostBootScript();
                        
                 try {
                    if (bootScript != null) {
                        //index = bootScript.indexOf("/");
                        logger.debug("scriptMap: " + scriptMap + ", ipaddr_index: " + ipaddr_index);
                        bootScript = ScriptConstructor.constructScript(d.getPostBootScript(), scriptMap, ipaddr_index);
                        if (bootScript != null) {
                            logger.debug("unit.instance.config=\n" + bootScript);
                            r.reservation.setConfigurationProperty("unit.instance.config", bootScript);
			    d.setPostBootScript(bootScript);
                        }
                    }
                 } catch (Exception e) {
                    System.out.println(e.getStackTrace());
                 }
            }
            
            String parent_tag_name = null;
            String parent_ip_addr = null;
            for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
                parent_tag_name = "unit.eth";
                parent_ip_addr = "unit.eth";
                String pdomain = getDomainName(parent.getKey());
                //ReservationRequest pr = map.get(pdomain);
                ReservationRequest pr = map.get(parent.getKey().getName());

                pdomain = pdomain.split("\\/")[0];
                if (pr == null) {
                    throw new RuntimeException("Could not find reservation from domain " + domain);
                }

                Properties filter = new Properties();
                Properties interfaces = new Properties();

                String mappedVlanProperty = UnitProperties.UnitVlanTag;
                String mappedPortListProperty = UnitProperties.UnitPortList;
                // if both reservations are for network resources we must
                // map the vlan tag from pr into a prefixed vlan tag
                // so that it does not collide with the vlan tags of other
                // dependent reservations
                if (r.isNetwork) {
                    mappedVlanProperty = pdomain + "." + mappedVlanProperty;
                    mappedPortListProperty = pdomain + "." + mappedPortListProperty;
                    logger.debug("Mapped Parent Vlan Property:" + mappedVlanProperty);
                } else {
                    //if (r.networkDependencies > 1) {
                    //    throw new RuntimeException("The current version of the controller only supports one dependency between non network and network resource");
                    //}
                }


                filter.setProperty(UnitProperties.UnitVlanTag, mappedVlanProperty);
                filter.setProperty(UnitProperties.UnitPortList, mappedPortListProperty);

                // if r is network we also need to specify the interface
                // that corresponds to the
                // vlan tag. NOTE: for now the names of the devices are not
                // included in the interface.
                // Our handlers have hardcoded notions about which device
                // name to use. We would want to change this
                // in the future.
                String intf_name = null;
                if (r.isNetwork) {
                    Statement intf_st = parent.getValue().getProperty(handler.getMapper().RDFS_Label);
                    intf_name = intf_st == null ? " " : intf_st.getString();

                    interfaces.setProperty(pdomain + ".edge.interface", intf_name);

                    r.reservation.setConfigurationProperty(pdomain + ".edge.interface", intf_name);

                    Resource remote_interface = parent.getKey().getUpNeighbour();
                    String remote_interface_label = "";
                    for (Entry<Device, Resource> child : parent.getKey().getFollowedBySet()) {
                        if (d == child.getKey()) {
                            remote_interface = child.getValue();
                            Statement child_intf_st = child.getValue().getProperty(handler.getMapper().RDFS_Label);
                            remote_interface_label = child_intf_st == null ? " " : child_intf_st.getString();
                            break;
                        }
                    }

                    logger.debug("Interface:" + intf_name + "; remote_interface_label:" + remote_interface_label);
                    r.reservation.setConfigurationProperty(pdomain + ".edge.interface.remote", remote_interface_label);
                    //r.reservation.setConfigurationProperty("config.interface.ports", intf_name);
                } else {
                    intf_name = parent.getValue().getURI();

                    if (intf_name != null) {
                        //System.out.println("parent_ip_addr = " + parent_ip_addr + " intf_name = " + intf_name + ":" + d.getPostBootScript());

                        
   
                        //
                        // NOTE: handle network dependencies 
                        int index;
                        logger.debug(pr.domain + " isNetwork?:" + pr.isNetwork);
                        if (pr.isNetwork) {
                            r.networkDependencies++;

                            
                            index = intf_name.indexOf("@");
                            String ip_addr = null, host_interface = null, site_host_interface = null;
                            DatatypeProperty hostInterfaceName = handler.getIdm().createDatatypeProperty("http://geni-orca.renci.org/owl/" + "topology.owl#hostInterfaceName");

                            if (parent.getValue() != null) {
                                if (parent.getValue().getProperty(hostInterfaceName) != null) {
                                    site_host_interface = parent.getValue().getProperty(hostInterfaceName).getString();
                                }
                            }
                            if (site_host_interface == null) {
                                logger.debug("Host Interface Definition not here: IP address is used as the parent value or its neighbors are network domains!!");
                                if (parent.getKey().getDownNeighbour() != null) {
                                    if (parent.getKey().getDownNeighbour().getProperty(hostInterfaceName) != null) {
                                        site_host_interface = parent.getKey().getDownNeighbour().getProperty(hostInterfaceName).getString();
                                    } else {
                                        if (parent.getKey().getUpNeighbour() != null) {
                                            if (parent.getKey().getUpNeighbour().getProperty(hostInterfaceName) != null) {
                                                site_host_interface = parent.getKey().getUpNeighbour().getProperty(hostInterfaceName).getString();
                                            }
                                        }
                                    }
                                } else {
                                    if (parent.getKey().getUpNeighbour() != null) {
                                        if (parent.getKey().getUpNeighbour().getProperty(hostInterfaceName) != null) {
                                            site_host_interface = parent.getKey().getUpNeighbour().getProperty(hostInterfaceName).getString();
                                        }
                                    }
                                }
                            }

                            if (site_host_interface == null) {
                                logger.debug("Host Interface Definition not here: neither up neighbor or down neighbor!!");
                                site_host_interface = "eth0";
                            }

                            logger.info("Site host interface:" + site_host_interface);

                            logger.debug("$$$$$$$$$$$$$$$$$ intf_name:" + intf_name + ", index: " + index);
                            if (index > 0) {
                                       
                                ip_addr = intf_name.substring(0, index);
                                host_interface = String.valueOf(Integer.valueOf(intf_name.substring(index + 1)).intValue() + 1);
                                parent_ip_addr = parent_ip_addr.concat(host_interface).concat(".ip");
                                r.reservation.setConfigurationProperty("unit.eth" + host_interface + ".hosteth", site_host_interface);
                                if (d.getPrecededBySet().size() >= 1) {
                                    parent_tag_name = parent_tag_name.concat(host_interface).concat(".vlan.tag");
                                }
                            } else {
                                ip_addr = intf_name;
                                parent_ip_addr = parent_ip_addr.concat(String.valueOf(r.networkDependencies)).concat(".ip");
                                if (d.getPrecededBySet().size() >= 1) {
                                    r.reservation.setConfigurationProperty("unit.eth" + String.valueOf(r.networkDependencies) + ".hosteth", site_host_interface);
                                    parent_tag_name = parent_tag_name.concat(String.valueOf(r.networkDependencies)).concat(".vlan.tag");
                                }

                            }
                            filter.setProperty("unit.vlan.tag", parent_tag_name);
                            try {
                            	String addr1_tmp;	
                            	if(d.getIPAddress()!=null){
                            		if(d.getIPAddress().indexOf("/")>0)
                                		addr1_tmp = ip_addr.substring(0,ip_addr.indexOf("/"));
                                	else
                                		addr1_tmp = ip_addr;
                                	//r.reservation.setConfigurationProperty(parent_ip_addr, ipaddr + "/" + netmask);
                                	InetAddress addr1 = InetAddress.getByName(ip_addr.split("/")[0]);  //this is only for throwing an exception for a mal-formated IP address.
                                	r.reservation.setConfigurationProperty(parent_ip_addr, ip_addr);
                            	}
                            } catch (UnknownHostException e) {
                                System.out.println("Not a Valid IP address:" + parent_ip_addr + ":" + ip_addr);
                            }
                        }
                    } else {
                        System.out.println("Edge interface name is unknown!");
                    }
                }
                //}
                logger.debug("   depends on: " + pr.domain + " filter: " + filter.toString() + " interfaces: " + interfaces.toString());

                // set the relationship
                r.reservation.addRedeemPredecessor(pr.reservation, filter);
            }
        }

        ArrayList<IServiceManagerReservation> result = new ArrayList<IServiceManagerReservation>(map.size());
        for (ReservationRequest r : map.values()) {
            logger.info("Reservation:" + r.toString());
            result.add(r.reservation);
        }

        return result;
    }

    public String getManifest(InterCloudHandler handler, IReservation[] allRes, ISubstrateDatabase db) {
        logger.info("Starting getting manifest...");
        boolean ready = true;
        String connectionName = " ", link_url = " ";
        LinkedList<Device> domainList = null;
        OntModel manifestModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	manifestModel.add(handler.mapper.requestModel.getBaseModel());
        //manifestModel.write(System.out);
	Individual networkConnection, link_ont = null, reservation;
        reservation=manifestModel.getIndividual(handler.mapper.reservation.getURI());
        Individual manifest = manifestModel.createIndividual(reservation.getNameSpace()+"manifest", handler.mapper.manifestOntClass);
        
        Resource intf_start, intf_next, intf_next_next;
        ExtendedIterator<Individual> connectionIterator = manifestModel.listIndividuals(handler.mapper.networkConnectionOntClass);
        if (connectionIterator == null) {
            logger.error("There is no networkConnection from the request");
            return "ERROR:There is no networkConnection from the requestr";
        }
        LinkedList<Individual> networkConnectionList = new LinkedList<Individual>();
        while (connectionIterator.hasNext()) {
            networkConnection = connectionIterator.next();
            networkConnectionList.add(networkConnection);
        }

        Hashtable<String, LinkedList<Device>> domainConnectionList = handler.getDomainConnectionList();
        if (domainConnectionList == null) {
            logger.error("There is no domain list from the handler");
            return "ERROR:There is no domain list from the handler";
        } else {
            logger.debug("Number of domain list from the handler:" + handler.getDomainConnectionList().size());
        }
        Iterator<Entry<String, LinkedList<Device>>> domainConnectionListIt = domainConnectionList.entrySet().iterator();
        boolean split = false;
	if(handler.cloudRequest != true){
	        while (domainConnectionListIt.hasNext()) {
	            Entry<String, LinkedList<Device>> entry = domainConnectionListIt.next();
	            connectionName = entry.getKey();
	            domainList = entry.getValue();
	            logger.debug("Connection name from handler domain list:" + connectionName + " ;num hops=" + domainList.size());
	            if(networkConnectionList.size()==0){  //likely slitted request case 
	            	networkConnection = manifestModel.createIndividual(connectionName, handler.mapper.networkConnectionOntClass);
	            	networkConnectionList.add(networkConnection);
	            } 
	            for (int j = 0; j < networkConnectionList.size(); j++) {
	                networkConnection = networkConnectionList.get(j);
	                logger.debug("Connection name from request rdf:" + networkConnection.getURI()+"; resource="+networkConnection);
	                if (connectionName.equals(networkConnection.getURI())) {
	                    logger.debug("Forming link connections...");
	                    // add the facts to the graph
	                    Device start, next_Hop, next_next_Hop = null;
	                    start = domainList.get(0);
	                    String connectionType=getConnectionType(networkConnection,handler.requestMap);
	                    if((split==true) && (connectionType==null) ) connectionType="InterDomainRequest";	
			    if(connectionType.equals("InterDomainRequest")){
	                    for (int i = 1; i < domainList.size(); i++) {
	                        next_Hop = domainList.get(i);
	                        logger.debug("link connections..." + i + ":" + domainList.size() + ":" + next_Hop.getURI());
	                        //source domain
	                        if (i == 1) {
	                            link_url = start.getURI();
	                            link_ont = manifestModel.createIndividual(link_url, handler.mapper.computeElementOntClass);
	                            link_ont.addProperty(handler.mapper.hasInterface, next_Hop.getUpNeighbour());
	                            networkConnection.addProperty(handler.mapper.item, link_ont);
	                            logger.debug("First domain:" + link_ont.getURI() + "|" + next_Hop.getUpNeighbour());
	                        }
	
	                        //Link connection
	                        intf_start = start.getDownNeighbour();
	                        intf_next = next_Hop.getUpNeighbour();
	                        link_url = intf_start.getURI() + "-" + intf_next.getURI().split("\\#")[1];
	                        link_ont = manifestModel.createIndividual(link_url, handler.mapper.linkConnectionOntClass);
	                        link_ont.addProperty(handler.mapper.hasInterface, intf_start);
	                        link_ont.addProperty(handler.mapper.hasInterface, intf_next);
	                        intf_start.addProperty(handler.mapper.RDF_TYPE, handler.mapper.interfaceOntClass);
	                        intf_next.addProperty(handler.mapper.RDF_TYPE, handler.mapper.interfaceOntClass);
	                        networkConnection.addProperty(handler.mapper.item, link_ont);
	                        logger.debug("Link hop:" + link_ont.getURI() + "|");
	
	                        //crossconnect connection
	                        if (i < domainList.size() - 1) {
	                            next_next_Hop = domainList.get(i + 1);
	                            intf_next_next = next_next_Hop.getUpNeighbour();
	                            link_url = next_Hop.getURI()+"/"+UUID.randomUUID().toString()+"/vlan";
	                            link_ont = manifestModel.createIndividual(link_url, handler.mapper.crossConnectOntClass);
	                            link_ont.addProperty(handler.mapper.hasURL,next_Hop.getName());
	                            link_ont.addProperty(handler.mapper.hasInterface, intf_start);
	                            link_ont.addProperty(handler.mapper.hasInterface, intf_next_next);		                            
	                            networkConnection.addProperty(handler.mapper.item, link_ont);
	                            logger.debug("Link Crossconnect hop:" + link_ont.getURI() + "|");
	
	                        }
	
	                        //destination domain
	                        if (i == domainList.size() - 1) {
	                            link_url = next_Hop.getURI();
	                            link_ont = manifestModel.createIndividual(link_url, handler.mapper.computeElementOntClass);
	                            link_ont.addProperty(handler.mapper.hasInterface, start.getDownNeighbour());
	                            start.getDownNeighbour().addProperty(handler.mapper.RDF_TYPE, handler.mapper.interfaceOntClass);
	                            
	                            networkConnection.addProperty(handler.mapper.item, link_ont);
	                            logger.debug("Last domain:" + link_ont.getURI() + "|" + start.getDownNeighbour());
	                        }
	
	                        start = next_Hop;
	                    }
	                    logger.debug("End of forming link connections...");
	                    manifest.addProperty(handler.mapper.element, networkConnection);
	                  }
	                  if(connectionType.equals("CloudRequest")){
	      	            next_Hop=null;
	    	            for (int i = 0; i < domainList.size(); i++) {
	                        next_Hop = domainList.get(i);
	                        link_url=next_Hop.getURI();
	                        if(next_Hop.getURI().endsWith("vm")){
	                        	link_ont=manifestModel.createIndividual(link_url,handler.mapper.computeElementOntClass);
	                        	networkConnection.addProperty(handler.mapper.item, link_ont);
	                        	logger.debug("0. link_ont:"+link_ont.getURI());
	                        }
	                        if(next_Hop.getURI().endsWith("vlan")){
	                        	link_ont=manifestModel.createIndividual(link_url,handler.mapper.crossConnectOntClass);
	                        	networkConnection.addProperty(handler.mapper.item, link_ont);
	                        	logger.debug("1. link_ont:"+link_ont.getURI());
	                        }
	                        //reservation.addProperty(handler.mapper.item, link_ont);
	                        //manifest.addProperty(handler.mapper.element, link_ont);
	                        logger.debug("create individual url:"+next_Hop.getURI()+" :Name="+next_Hop.getName() +" :Individual="+link_ont);
	    	            } 
	                  }
	                  
	                }
	                logger.debug("End of manifest connection...");
	            }
	            logger.debug("End of handler connections...");
	        }
	        
	        connectionIterator = manifestModel.listIndividuals(handler.mapper.crossConnectOntClass);
	        if (connectionIterator == null) {
	            logger.error("There is no crossConnect in the manefest.");
	            return "ERROR:There is no crossConnect in the manefest.";
	        }
	        
	        networkConnectionList = new LinkedList<Individual>();
	        while (connectionIterator.hasNext()) {
	            networkConnection = connectionIterator.next();
	            networkConnectionList.add(networkConnection);
	        }
	        connectionIterator = manifestModel.listIndividuals(handler.mapper.computeElementOntClass);
	        if (connectionIterator == null) {
	            logger.error("There is no copmuteElement in the manefest.");
	            return "ERROR:There is no computeElement in the manefest.";
	        }
	        while (connectionIterator.hasNext()) {
	            networkConnection = connectionIterator.next();
	            networkConnectionList.add(networkConnection);
	        }
        }else{
        	networkConnectionList = new LinkedList<Individual>();
        	while (domainConnectionListIt.hasNext()) {
	            Entry<String, LinkedList<Device>> entry = domainConnectionListIt.next();
	            connectionName = entry.getKey();
	            domainList = entry.getValue();
	            logger.debug("Connection name from handler domain list:" + connectionName + " ;num hops=" + domainList.size());
	            Device next_Hop=null;
	            for (int i = 0; i < domainList.size(); i++) {
                    next_Hop = domainList.get(i);
                    link_url=next_Hop.getURI();
                    if(next_Hop.getURI().endsWith("vm")){
                    	link_ont=manifestModel.createIndividual(link_url,handler.mapper.computeElementOntClass);
                    	logger.debug("0. link_ont:"+link_ont.getURI());
                    	manifest.addProperty(handler.mapper.element, link_ont);
                    	networkConnectionList.add(link_ont);
                    }
                    if(next_Hop.getURI().endsWith("vlan")){
                    	link_ont=manifestModel.createIndividual(link_url,handler.mapper.linkConnectionOntClass);
                    	logger.debug("1. link_ont:"+link_ont.getURI());
                    	networkConnectionList.add(link_ont);
                    }
                    //reservation.addProperty(handler.mapper.item, link_ont);
                    //manifest.addProperty(handler.mapper.element, link_ont);
                    logger.debug("create individual url:"+next_Hop.getURI()+" :Name="+next_Hop.getName() +" :Individual="+link_ont);
	            }   
        	}
        }
        	      
        String domain,aDomain, type, rDomain, rType;
        int units,index1,index2;
        Hashtable <String, Individual> vmReservationList = new Hashtable <String, Individual>();
        Hashtable <String, Individual> vlanReservationList = new Hashtable <String, Individual>();
        logger.debug("start generating domain manifest...");
        
        for (int i = 0; i < networkConnectionList.size(); i++) {
            networkConnection = networkConnectionList.get(i);
            domain = networkConnection.getURI();
            logger.debug("Domain from domain list out of handelr:" + domain);
            for (int j = 0; j < allRes.length; j++) {
                type = allRes[j].getApprovedType().toString();
                units = allRes[j].getApprovedUnits();
                if (!(allRes[j].getReservationState().getStateName().equalsIgnoreCase("Active"))) {
                    ready = false;
                }
                rDomain = type.split("\\.")[0];
                rType = type.split("\\.")[1];
                aDomain=domain;
                index2=domain.indexOf(UriSeparator);
                if(index2>0){
                	aDomain=domain.substring(index2+1,aDomain.length());
                	index1=aDomain.indexOf(UriSuffix);
                	if(index1>0){
                		aDomain = aDomain.substring(0,index1);
                	}
                }
                logger.debug("getManifest: domain=" + domain + " ;aDomian = "+aDomain+" :rDomain=" + rDomain +" rType="+rType);
                
                if ((aDomain.equalsIgnoreCase(rDomain)) && (domain.endsWith(rType))) {
                    try {
                        if (db == null) {
                            logger.debug("db is null");
                        }
                        String notice = allRes[j].getNotices();
                        String state=allRes[j].getReservationState().getStateName();
                        Resource reservationState = manifestModel.createIndividual(handler.mapper.ORCA_NS+"request.owl#"+state, handler.mapper.reservationStateOntClass);
                        
                        Vector<Properties> v = db.getUnits(allRes[j].getReservationID());
                        if (v != null) {
                            if (v.size() > 0) {
                                for (Properties p : v) {
                                    if (rType.equalsIgnoreCase("vlan")) {
                                    	String vlan_url = p.getProperty("unit.vlan.url");
                                        logger.debug("getManifest: unit.vlan.url="+vlan_url);
                                        if (vlan_url == null) {
                                            logger.error("unit.vlan.url is null");
                                            vlan_url = domain + "/" + p.getProperty("unit.rid");
                                        }

					Individual vlan_ont = networkConnection;
                                    	if( (handler.interDomainRequest==false) && (handler.cloudRequest == true) ){
                                    		vlan_ont = manifestModel.createIndividual(vlan_url, handler.mapper.linkConnectionOntClass);
						//networkConnection.addProperty(handler.mapper.element, vlan_ont);
                                    		manifest.addProperty(handler.mapper.element, vlan_ont);
						vlan_ont.addProperty(handler.mapper.hasURL,vlan_url);
						vlanReservationList.put(vlan_url, vlan_ont);
					}
					if(vlan_ont.hasProperty(handler.mapper.hasURL)){
						if(vlan_ont.getProperty(handler.mapper.hasURL).getString().equals(vlan_url) ){
                                    			if (p.getProperty("unit.vlan.tag") != null) {
                                            			vlan_ont.addProperty(handler.mapper.RDFS_Label, p.getProperty("unit.vlan.tag"));
                                        		} else {
                                            			logger.error("unit.vlan.tag is null");
                                        		}
                                        		if (p.getProperty("unit.vlan.qos.rate") != null) {
                                            			vlan_ont.addProperty(handler.mapper.bandwidth, p.getProperty("unit.vlan.qos.rate"));
                                        		} else {
                                            			logger.error("unit.vlan.qos.rate is null");
                                        		}
							vlan_ont.addProperty(handler.mapper.message, notice);
		                                        vlan_ont.addProperty(handler.mapper.hasReservationState,reservationState);
							vlanReservationList.put(vlan_url, vlan_ont);
						}
					}
                                    }
                                    if (rType.equalsIgnoreCase("vm")) {
                                        String vm_url = p.getProperty("unit.hostname.url");
                                        if (vm_url == null) {
                                            logger.error("unit.hostname.url is null");
                                            vm_url = domain + "/" + p.getProperty("unit.rid");
                                        }
                                        Individual vm_ont = manifestModel.createIndividual(vm_url, handler.mapper.vmOntClass);
					/* 
					if( (handler.interDomainRequest==false) && (handler.cloudRequest == true) ){
                                                manifest.addProperty(handler.mapper.element, vm_ont);
					}*/	
					vmReservationList.put(vm_url, vm_ont);
                                        
                                        logger.info("VM config properties: 1="+p.getProperty("unit.eth1.hosteth") + " 2="+p.getProperty("unit.eth2.hosteth")+ " 2="+p.getProperty("unit.eth3.hosteth"));
                                        
                                        Individual service_ont = manifestModel.createIndividual(vm_url + "/Service", handler.mapper.serviceOntClass);
                                        service_ont.addProperty(handler.mapper.hasAccessMethod,handler.mapper.SSH);
                                        vm_ont.addProperty(handler.mapper.hasService,service_ont);                                        
                                        Individual vm_IP = manifestModel.createIndividual(vm_url + "/IP", handler.mapper.IPAddressOntClass);
					String unitManagementIP = p.getProperty("unit.manage.ip");
                                        String unitManagementPort = p.getProperty("unit.manage.port");
                                        if (unitManagementIP != null) {
                                        	vm_IP.addProperty(handler.mapper.label_ID, unitManagementIP);
                                        	service_ont.addProperty(handler.mapper.ip_p, unitManagementIP);
                                    		if(unitManagementPort!=null){
                                    			service_ont.addProperty(handler.mapper.port_p, unitManagementPort);
                                    		} else {
                                                logger.error("unit.manage.port is null");
                                            }
                                        	if( (unitManagementPort==null) || (unitManagementPort.equals("22")) ){  //No DNAT
                                        		vm_ont.addProperty(handler.mapper.ip4LocalIPAddressProperty, vm_IP);
                                        	}
                                        } else {
                                            logger.error("unit.manage.ip is null");
                                        }
                               
                                        if (p.getProperty("unit.instance.config") != null) {
                                            vm_ont.addProperty(handler.mapper.postBootScript, p.getProperty("unit.instance.config"));
                                        } else {
                                            logger.error("unit.instance.config is null");
                                        }
                                        
                                        vm_ont.addProperty(handler.mapper.message, notice);
                                        vm_ont.addProperty(handler.mapper.hasReservationState,reservationState);
                                        
                                        networkConnection.addProperty(handler.mapper.element, vm_ont);
                                        logger.debug("Element:" + vm_ont);
                                    }
                                    if (logger.isDebugEnabled()) {
				        for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
					    String key = (String) e.nextElement();
					    logger.debug("{Property key: " + key + " | Property value: " + p.getProperty(key) + "}");
				        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex);
                    }
                }
            }
        }
        domainList=handler.getMapper().getDeviceConnection().getConnection();
        Literal oldScript=null;
        try{
        for(Device d:domainList){
        	logger.debug("d in deviceList:"+d.getName()+" url="+d.getURI() + " resourceType="+d.getResourceType().getResourceType().toString());
        	//if(d.getResourceType().getResourceType().equalsIgnoreCase("VM")){
        	if(d.getResourceType().getResourceType().lastIndexOf("VM") >= 0){
        		for(Entry <String, Individual> entry:vmReservationList.entrySet()){
        			logger.debug("vm in vmReservationList:"+entry.getKey()+" Resource:"+entry.getValue());
        			if(d.getName().equalsIgnoreCase(entry.getKey())){
        				logger.debug("d.getName(): " + d.getName() + "entry.getKey(): " + entry.getKey());
        				if(d.getPostBootScript()!=null){
        					if(entry.getValue().hasProperty(handler.mapper.postBootScript)){
        						entry.getValue().setPropertyValue(handler.mapper.postBootScript, manifestModel.createLiteral(d.getPostBootScript()));
        					}else{
        						/*try {
									NdlGenerator.addTypedProperty(entry.getValue(),"request-schema","postBootScript", d.getPostBootScript(),XSDDatatype.XSDstring);
								} catch (NdlException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}*/
        						entry.getValue().addProperty(handler.mapper.postBootScript, d.getPostBootScript());
        					}
        					logger.debug("d.getPostBootScript(): " + d.getPostBootScript());
        				}
        				else{
        					logger.info("d.getPostBootScript() returned null");
        				}
        				/*	
        				if(d.getIPAddress()!=null){
        					Individual vm_IP = manifestModel.createIndividual(d.getName() + "/IP", handler.mapper.IPAddressOntClass);
        					vm_IP.addProperty(handler.mapper.label_ID, d.getIPAddress());
        					if(d.getIPNetmask()!=null){
        						vm_IP.addProperty(NdlCommons.ip4NetmaskProperty, d.getIPNetmask());
        					}
        					if(entry.getValue().hasProperty(NdlCommons.ip4LocalIPAddressProperty)){
        						entry.getValue().setPropertyValue(NdlCommons.ip4LocalIPAddressProperty,vm_IP);
        					}else{
        						entry.getValue().addProperty(NdlCommons.ip4LocalIPAddressProperty,vm_IP);
        					}
        					
        				}*/
        			
        				//associate VM reservations to its vlan parent
					if(d.getPrecededBySet()!=null){
						int i=0;
        					for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
        						i++;
							Device parentDevice = parent.getKey();
        						for(Entry <String, Individual> vlanentry:vlanReservationList.entrySet()){
        							String vlanDeviceName=vlanentry.getKey();
        							logger.debug("Parent:"+parentDevice.getName()+" ;value="+parent.getValue()+"----vlanentry: key="+vlanDeviceName+" ;value="+vlanentry.getValue());
        							if(vlanDeviceName.equals(parentDevice.getName())){
									Individual vm_IP = manifestModel.createIndividual(d.getName() + "/IP/"+String.valueOf(i), handler.mapper.IPAddressOntClass);
									vm_IP.addProperty(handler.mapper.label_ID, parent.getValue().getURI().split("/")[0]);
        								if( (handler.interDomainRequest==false) && (handler.cloudRequest == true) ){
										vlanentry.getValue().addProperty(handler.mapper.hasInterface, vm_IP);
									}
        								entry.getValue().addProperty(handler.mapper.hasInterface, vm_IP);
								}
        						}
        					}
					}
				}
        		}
        	}
        }}catch(Exception e){
        	e.printStackTrace();
        }
        OutputStream out = new ByteArrayOutputStream();
        manifestModel.write(out);
        return out.toString();
    }
    
    String getConnectionType(Resource networkConnection,Hashtable <String,NetworkConnection> requestMap){
    	String type="";
    	for(Entry <String,NetworkConnection > entry:requestMap.entrySet()){
			String connectionName=entry.getKey();
			NetworkConnection requestConnection=entry.getValue();
			logger.debug("getManifest: requestConnection Type:"+requestConnection.getURI());
			if(networkConnection.getURI().equals(requestConnection.getURI())){
				type=requestConnection.getConnectionType();
			}
    	}
    	
    	return type;
    }
    
    public static final String ActionVlanTag = "VLANtag";
    public static final String Server = "server";
    public static final String UriSeparator = "#";
    public static final String UriSuffix = "/";
    public static final String FilenameSeparator = "\\.";
    /**
     * Returns a normalized domain name.
     * @param d
     * @return
     */
    public static String getDomainName(Device d) {
        System.out.println("getDomainName:"+d.getName()+";uri="+d.getUri()+"\n");
	String temp = d.getUri();
        int index = temp.indexOf(UriSeparator);
        if (index >= 0) {
            int index2 = temp.indexOf(UriSuffix, index);
            if (index2 >= 0) {
                String rType = null;
                if ((d.getResourceType() == null) || (d.getResourceType().getResourceType() == null)) {
                    rType = null;
                } else {
                    String[] type = d.getResourceType().getResourceType().split("\\#");
                    if (type.length == 1) {
                        rType = null;
                    } else {
                        rType = type[1];
                    }
                }

                if (rType == null) {
                    return temp.substring(index + 1, index2);
                } else if (rType.equalsIgnoreCase("VM") || rType.equalsIgnoreCase("Testbed")) {
                    return temp.substring(index + 1, index2);
                } else {
                    return temp.substring(index + 1, index2).concat("/").concat(rType.toLowerCase());
                }
            }

        }
        return null;
    }

    //Form script for Hardoop master or slave    
    public static String getHadoopScript(String ip, String hostsScript, String topologyScript, String type) {

        String line = "#!/bin/bash \n";
        String line0 = "\t echo " + '"' + "Starting daemons" + '"' + " > /home/hadoop/log \n";
        String line1 = hostsScript;
        String line2 = "\t /etc/init.d/network restart \n";
        String line3 = "\t hostname >> /home/hadoop/log \n";
        String line4 = topologyScript;
        String line5 = "\t /home/hadoop/hadoop-euca-init.sh " + ip.split("/")[0] + " " + type + "\n";
        String line6 = "\t echo " + '"' + "Done starting daemons" + '"' + ">>" + "/home/hadoop/log \n";

        String script = line.concat(line0).concat(line1).concat(line2).concat(line3).concat(line4).concat(line5).concat(line6);
        return script;
    }

    public static String getHadoopTopologyScript(String topology, int topologyFlag) {
        String script = "\t echo \"#!/usr/bin/env python \" > /home/hadoop/topology.py \n";
        String line1 = "\t echo \"import sys \" >> /home/hadoop/topology.py \n";
        String line2 = "\t echo \"from string import join \" >> /home/hadoop/topology.py \n";
        String line3 = "\t echo \"DEFAULT_RACK = '/default/rack0'; \" >> /home/hadoop/topology.py \n";
        String line4 = "\t echo \"RACK_MAP = { \" >> /home/hadoop/topology.py \n";
        if (topologyFlag < 0) {
            line4 = line4.concat(topology);
        }
        String line5 = "\t echo \"} \" >> /home/hadoop/topology.py \n";
        String line6 = "\t echo \"if len(sys.argv)==1: \" >> /home/hadoop/topology.py \n";
        String line7 = "\t echo \"        print DEFAULT_RACK \" >> /home/hadoop/topology.py \n";
        String line8 = "\t echo \"else: \" >> /home/hadoop/topology.py \n";
        String line9 = "\t echo \"        print join([RACK_MAP.get(i, DEFAULT_RACK) for i in sys.argv[1:]]," + '"' + " " + '"' + ") \" >> /home/hadoop/topology.py \n";

        script = script.concat(line1).concat(line2).concat(line3).concat(line4).concat(line5).concat(line6).concat(line7).concat(line8).concat(line9);

        return script;
    }

    public static String getVirtualRackScript(Device d) {
        String script = "\t     echo \" '" + d.getIPAddress().split("/")[0] + "'" + " : '/datacenter";
        String rack = getVirtualRack(d);
        script = script.concat(rack).concat("/rack").concat("0").concat("',  \" >> /home/hadoop/topology.py \n");
        return script;
    }
    public static Hashtable<String, String> virtualRack = new Hashtable<String, String>();  //<site,rackNumber> pair
    public static int rackNumber = 0;

    public static String getVirtualRack(Device d) {
        String rack = null;
        String site = d.getURI();
        if (!virtualRack.containsKey(site)) {
            rack = String.valueOf(rackNumber);
            virtualRack.put(site, rack);
            rackNumber++;
        } else {
            rack = virtualRack.get(site);
        }

        return rack;
    }

    //Form script for Condor hostname script    
    public static String getClusterHostname(Device d) {
        String line = "\t echo '";
        String ip = d.getIPAddress().split("/")[0];
        String hostname = getHostname(d);

        return line.concat(ip).concat(" ").concat(hostname);
    }

    public static String getHostname(Device d) {
        String ip = d.getIPAddress().split("/")[0];
        ip = ip.replace('.', '-');     // ************* pruth *************
        String hostname = d.getPostBootScript().split("/")[0];
        if (d.getFollowedBy() == null) {
            hostname = hostname.concat("-slave-").concat(ip);
        } else {
            if (d.getFollowedBy().size() > 0) {
                hostname = hostname.concat("-").concat("master");
                masterHostName = hostname;
            } else {
                hostname = hostname.concat("-slave-").concat(ip);
            }
        }
        return hostname;
    }

    public static String getCondorScript(Device d, String hostsScript, String type) {
        String line1 = "#!/bin/bash \n";
        String line2 = "\t echo " + '"' + "hello from neuca script" + '"' + "\n";
        String line3 = "\t mkdir -p /opt/apps \n";     // ************* pruth *************
        String line4 = "\t echo 'condor-master:/var/nfs /opt/apps nfs vers=3,proto=tcp,hard,intr,timeo=600,retrans=2,wsize=32768,rsize=32768 0 0' >> /etc/fstab \n";  // ************* pruth *************

        String script = line1.concat(line2).concat(line3).concat(line4).concat(hostsScript);;

        String line4_1 = "";
        String line4_2 = "";
        //if master
        if (d.getFollowedBy() != null) {
            line4_1 = line4_1.concat("\t echo '/var/nfs *(rw,no_root_squash)' >> /etc/exports \n");	// ************* pruth *************
            line4_2 = line4_2.concat("\t /etc/init.d/nfs-kernel-server restart \n");	                // ************* pruth *************
        }

        String line5 = "\t echo '";
        String hostname = getHostname(d);
        line5 = line5.concat(hostname).concat("' > /etc/hostname \n");
        String line6 = "\t /bin/hostname -F /etc/hostname \n";
        String line7 = "\t mount -a \n";
        String line8 = "\t /etc/init.d/condor start";
        return script.concat(line4_1).concat(line4_2).concat(line5).concat(line6).concat(line7).concat(line8);
    }
}
