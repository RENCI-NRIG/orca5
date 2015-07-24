package orca.controllers.ben.interdomain;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetNetworkException;
import orca.controllers.ben.BenConstants;
import orca.controllers.ben.control.BenNdlControl;
import orca.controllers.ben.control.BenNdlPropertiesConverter;
import orca.ndl.Device;
import orca.ndl.Interface;
import orca.ndl.LayerConstant;
import orca.ndl.NetworkConnection;
import orca.ndl.SwitchingAction;
import orca.network.CloudHandler;
import orca.network.InterDomainHandler;
import orca.policy.core.util.PropertiesManager;
import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IController;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.RPCException;
import orca.shirako.common.ResourceType;
import orca.shirako.container.ConfigurationException;
import orca.shirako.container.Globals;
import orca.shirako.core.BrokerPolicy;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.meta.ConfigurationProperties;
import orca.shirako.meta.QueryProperties;
import orca.shirako.meta.RequestProperties;
import orca.shirako.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.meta.ResourcePoolDescriptor;
import orca.shirako.meta.ResourcePoolsDescriptor;
import orca.shirako.meta.ResourceProperties;
import orca.shirako.meta.UnitProperties;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.util.ID;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * author anirban
 * @author aydan
 */
public class InterDomainController implements IController, BenConstants {
    /**
     * Maps domain to resource type provided by that domain (only one type per
     * domain for now).
     */
    protected HashMap<String, ResourceType> typesMap = new HashMap<String, ResourceType>();
    protected HashMap<ID, InterDomainRequest> requests;
    protected IServiceManager sm;
    protected ISlice slice;
    protected String root;
    protected String benRoot;
    protected ActorClock clock;
    protected Logger logger = null;
    protected String noopConfigFile = null;
    protected IBrokerProxy brokerProxy = null;
    protected String sshKey;
    protected List<String> abstractModels;

    private boolean initialized = false;
    private boolean discoveredTypes = false;
    
    private InterDomainHandler h;
    
    public InterDomainController() {
        requests = new HashMap<ID, InterDomainRequest>();
        abstractModels = new ArrayList<String>();
    }

    protected void getBrokers() {
        brokerProxy = sm.getBroker("ndl-broker");
        if (brokerProxy == null) {
            throw new RuntimeException("missing broker proxy");
        }
    }

    private static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        FileInputStream f = new FileInputStream(filePath);
        f.read(buffer);
        return new String(buffer);
    }

    public void initialize() throws Exception {
        if (!initialized) {
            if (sm == null) {
                throw new Exception("Missing actor");
            }

            if (slice == null) {
                throw new Exception("Missing slice");
            }

            clock = sm.getActorClock();
            logger = sm.getLogger();
            root = Globals.getContainer().getPackageRootFolder(MyPackageId);
            sshKey = readFileAsString(root + "/keys/ben.pub");
            getBrokers();

            noopConfigFile = Globals.LocalRootDirectory + "/handlers/common/noop.xml";
            discoverTypes();
            initialized = true;
        }
    }

    protected void discoverTypes() throws Exception {
        Properties request = new Properties();
        request.setProperty(QueryProperties.QueryAction, QueryProperties.QueryActionDisctoverPools);
        IQueryResponseHandler handler = new IQueryResponseHandler() {
            public void handle(RPCException t, Properties response) {
                if (t != null) {
                    logger.error("Could not discover types", t);
                }else {
                    try {
                        ResourcePoolsDescriptor pools = BrokerPolicy.getResourcePools(response);
                        for (ResourcePoolDescriptor rpd : pools) {
                            ResourceType type = rpd.getResourceType();
                            ResourcePoolAttributeDescriptor a = rpd.getAttribute(ResourceProperties.ResourceDomain);
                            if (a == null) {
                                throw new RuntimeException("Missing domain information for resource pool:  " + type);
                            }
                            typesMap.put(a.getValue(), type);
                            System.out.println("Mapping: " + a.getValue() + " to " + type);
    
                            a = rpd.getAttribute(ResourceProperties.ResourceNdlAbstractDomain);
                            if (a != null) {
                                System.out.println("Found abstract model for resource pool: " + type);
                                //System.out.println("\n"+a.getValue());
                                abstractModels.add(a.getValue());
                            }
                        }
                        discoveredTypes = true;
                    } catch (ConfigurationException e) {
                        logger.error("Could not process discover types response", e);
                    }
                }                    
            }
        };
        
        sm.query(brokerProxy, request, handler);
    }

    public void tick(long cycle) {
    }

    protected InterDomainHandler makeHandler() {
        try {
            InterDomainHandler handler = new InterDomainHandler();
            for (String str : abstractModels) {
                handler.addAbstractDomainString(str);
            }
            handler.abstractModel();
            return handler;
        } catch (IOException e) {
            throw new RuntimeException("Could not create interdomain handler", e);
        }
    }

    public ID addRequest(String ndlRequest, Term term) throws IOException, InetNetworkException{  
        if (!discoveredTypes) {
            throw new RuntimeException("Types have not been discovered yet");
        }
        
        InterDomainRequest r = new InterDomainRequest();
        r.requestId = new ID();

	// Uncomment for port provisioning testing without portal GUI
	//ndlRequest = readFileAsString("/opt/orca/ndl/idRequest3.rdf");
	
        h = makeHandler();
        Hashtable <String,LinkedList <Device> > con = h.handleRequest(new ByteArrayInputStream(ndlRequest.getBytes()));
        try {
        	
            if (con == null) {
            	if(h.getMapper().domain!=null){
            		System.out.println("VT mapping within one domain:"+h.getMapper().domain);
        			CloudHandler cloudHandler = new CloudHandler(h.getIdm());
            		cloudHandler.setRequestMap(h.getRequestMap());
            		LinkedList <Device>  deviceList=cloudHandler.handleMapping(h.getRequestModel());
            		NetworkConnection connection = new NetworkConnection ();
                    connection.setName(cloudHandler.getCurrentRequestURI());
                    connection.setConnection(deviceList);
            		h.getMapper().setDeviceConnection(connection);
            		logger.info("VT mapping within a domain:"+h.getMapper().domain);
            	}
            	else{
            		logger.error("Invalid request");
            		return null;
            	}
            }
        } catch (Exception e) {
            logger.error("An error occurred while processing request", e);
            return null;
        }

        // obtain the list of reservations.
        // this method also sets the right relationships between reservations
        r.listInterDomainReservations = getReservations(h, term);

        Iterator<IServiceManagerReservation> it = r.listInterDomainReservations.iterator();
        while (it.hasNext()) {
            try {
                IServiceManagerReservation currRes = (IServiceManagerReservation) it.next();
                sm.demand(currRes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to demand reservation", e);
            }
        }

        requests.put(r.requestId, r);
        return r.requestId;
    }

    protected void close(IReservation r) {
        try {
            if (r != null) {
                sm.close(r);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to close reservation", e);
        }
    }

    public void close(ID id) {
        InterDomainRequest request = getRequest(id);

        if (request == null) {
            return;
        }

        request.closed = true;

        Iterator<IServiceManagerReservation> it = request.listInterDomainReservations.iterator();
        while (it.hasNext()) {
            IServiceManagerReservation currRes = (IServiceManagerReservation) it.next();
            close(currRes);
        }

    }

    public InterDomainRequest getRequest(ID id) {
        return requests.get(id);
    }

    public InterDomainRequest[] getRequests() {
        InterDomainRequest[] result = new InterDomainRequest[requests.size()];
        requests.values().toArray(result);
        return result;
    }

    public void setActor(IActor sm) {
        this.sm = (IServiceManager) sm;
    }

    public void setSlice(ISlice slice) {
        this.slice = slice;
    }

    public IActor getActor() {
        return sm;
    }

    public ISlice getSlice() {
        return slice;
    }

    public Logger getLogger() {
        return logger;
    }

    public IBrokerProxy getBroker() {
        return brokerProxy;
    }

    public void reset(Properties p) throws Exception {
    }

    public Properties save() throws Exception {
        Properties p = new Properties();
        save(p);

        return p;
    }

    public void save(Properties p) throws Exception {
    }

    protected class ReservationRequest {
        public String domain;
        public IServiceManagerReservation reservation;
        public boolean isNetwork;
        public int networkDependencies;
    }

    public String getNdl(String file) {
        try {
            InputStream s = getClass().getClassLoader().getResource(file).openStream();
            StringBuffer sb = new StringBuffer();

            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            String line = null;
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts the ontology device list into Orca reservations.
     * @param list
     * @return
     */
    protected List<IServiceManagerReservation> getReservations(InterDomainHandler handler, Term term) {
       	
    	List<Device> list = handler.getMapper().getDeviceConnection().getConnection();
        HashMap<String, ReservationRequest> map = new HashMap<String, ReservationRequest>();

        // first pass: make the reservation objects
        for (Device d : list) {
            String domain = BenNdlPropertiesConverter.getDomainName(d);
            if (domain == null) {
                throw new RuntimeException("Invalid domain name");
            }

            // get the NDL request for that domain
            OntModel model = handler.domainRequest(d);
            d.setIdmRequest(model);
            OutputStream out = new ByteArrayOutputStream();
            model.write(out);
            String ndlRequest = out.toString();

            // what type can we get from that domain
            ResourceType type = typesMap.get(domain);
 
            if (type == null) {
                throw new RuntimeException("Invalid resource type for domain: " + domain);
            }

            ResourceSet rset = new ResourceSet(d.getResourceType().getCount(), type);

            // create the reservation
            IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance().create(rset, term, slice, brokerProxy);
            // no SM-side handler
            r.setLocalProperty(AntConfig.PropertyXmlFile, noopConfigFile);
            // pass the ssh key to the authority
            r.setConfigurationProperty(ConfigurationProperties.ConfigSSHKey, sshKey);
            // pass the NDL request to the authority
            // NOTE: for now, our broker is not aware of NDL, only some of the
            // sites, e.g., BEN know how to handle NDL.
            r.setConfigurationProperty(BenNdlControl.PropertyRequestNdl, ndlRequest);

            r.setRenewable(true);
            PropertiesManager.setElasticTime(rset, true);
            ReservationRequest request = new ReservationRequest();
            request.reservation = r;
            request.domain = domain;
            // is this a reservation for a network resource?
            request.isNetwork = type.toString().endsWith(".vlan");
            //map.put(domain, request);

            map.put(d.getName(), request);
            
            if (request.isNetwork) {
                // to broker: set the bandwidth, start/end interface
                // to site: set the config names for the interfaces that need to be configured                
                LinkedList<SwitchingAction> actions = d.getActionList();
                if (actions != null) {
                    String from = null;
                    String to = null;
                    String bw = null;
                    if (actions.size() > 1) {
                        throw new RuntimeException("More than one switching action for domain: " + domain);
                    }
		
                    SwitchingAction a = actions.getFirst();
		
                    if(a.getDefaultAction()==null){
                    	System.out.println("Default action is null!");
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
                    String ports=null;
                    if(ifs==null){
                    	System.out.println("Interface list is empty for this action!");
                    }
                    else{
                     for (Interface iff : ifs) {
                    	 if(iff==null){
                            System.out.println("Interface is null for this interface list!");
                    	 }

                        if (iff.getName() != null) {
                        	if(ports==null){
                        		ports=iff.getName();
                        	}
                        	else{
                        		ports=ports.concat(","+iff.getName());
                        	}
                            r.setConfigurationProperty("config.interface." + (count+1), iff.getName());
                            System.out.println("domain=" + domain + " setting property config.interface." + (count+1) + "=" + iff.getName());
                            count++;
                        }
                     }
                    }
                    r.setConfigurationProperty("config.interface.ports",ports);
                    
                    bw = String.valueOf(a.getBw());
                    from = ifs.get(0).getURI();
                    if (count > 1) {
                        to = ifs.get(1).getURI();
                    }
     
                    System.out.println("From:" + from + " To: " + to + " BW:" + bw + ":"+ports+"\n");
                    if (bw != null) {
                        if (from == null && to == null) {
                            throw new RuntimeException("Bandwidth requested, but no interface specified for domain: " + domain);
                        }    
		   
                        // set the bandwidth
                        r.setRequestProperty(RequestProperties.RequestBandwidth, bw);
                        System.out.println("Request Properties:"+RequestProperties.RequestBandwidth+":"+bw);
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

        // second pass: set the dependencies

        for (Device d : list) {
            String domain = BenNdlPropertiesConverter.getDomainName(d);
            //ReservationRequest r = map.get(domain);
            ReservationRequest r = map.get(d.getName());
            if (r == null) {
                throw new RuntimeException("Missing reservation from domain " + domain);
            }
            System.out.println("from " + r.domain + " units=" + r.reservation.getUnits() + " type=" + r.reservation.getType());
            Hashtable<Device, Resource> preds = d.getPrecededBy();
            if (preds == null) {
                continue;
            }
            String parent_tag_name=null;
            String parent_ip_addr=null;
            for (Entry<Device, Resource> parent : d.getPrecededBySet()) {
            	parent_tag_name="unit.eth";
            	parent_ip_addr="unit.eth";
                String pdomain =BenNdlPropertiesConverter.getDomainName(parent.getKey());
                //ReservationRequest pr = map.get(pdomain);
                ReservationRequest pr = map.get(parent.getKey().getName());
                
                pdomain=pdomain.split("\\/")[0];
                if (pr == null) {
                    throw new RuntimeException("Could not find reservation from domain " + domain);
                }
                Properties filter = new Properties();
                Properties interfaces = new Properties();
                // NOTE: we can only handle network dependencies for now
                System.out.println(pr.domain+" isNetwork?:"+ pr.isNetwork);
                //if (pr.isNetwork) {
                    r.networkDependencies++;
                    String mappedVlanProperty = UnitProperties.UnitVlanTag;
                    String mappedPortListProperty = UnitProperties.UnitPortList;	
                    // if both reservations are for network resources we must
                    // map the vlan tag from pr into a prefixed vlan tag
                    // so that it does not collide with the vlan tags of other
                    // dependent reservations
                    //if (r.isNetwork) {
                    //    mappedVlanProperty = pdomain + "." + mappedVlanProperty;
                    //    mappedPortListProperty=pdomain + "." +mappedPortListProperty;
                    //    filter.setProperty(UnitProperties.UnitVlanTag, mappedVlanProperty);
                    //} else {
                        //if (r.networkDependencies > 1) {
                        //    throw new RuntimeException("The current version of the controller only supports one dependency between non network and network resource");
                        //}
                    //}

                    //if(d.getPrecededBySet().size()>1){
    	            	//parent_tag_name=parent_tag_name.concat(String.valueOf(r.networkDependencies)).concat(".vlan.tag");
    	            	//filter.setProperty(UnitProperties.UnitVlanTag,parent_tag_name);
    	            	//System.out.println("unit.eth"+String.valueOf(r.networkDependencies)+".hosteth"+":"+"eth0");
    	            	
    	            	//parent_ip_addr=parent_ip_addr.concat(String.valueOf(r.networkDependencies)).concat(".ip");
    	            //}
    	            //else{
    	            //	filter.setProperty(UnitProperties.UnitVlanTag, mappedVlanProperty);
    	            //}
                    //filter.setProperty(UnitProperties.UnitVlanTag, mappedVlanProperty);
                    filter.setProperty(UnitProperties.UnitPortList, mappedPortListProperty);
                    // if r is network we also need to specify the interface
                    // that corresponds to the
                    // vlan tag. NOTE: for now the names of the devices are not
                    // included in the interface.
                    // Our handlers have hardcoded notions about which device
                    // name to use. We would want to change this
                    // in the future.
                    String intf_name=null;
                    if (r.isNetwork) {
                    	 mappedVlanProperty = pdomain + "." + mappedVlanProperty;
                         mappedPortListProperty=pdomain + "." +mappedPortListProperty;
                         filter.setProperty(UnitProperties.UnitVlanTag, mappedVlanProperty);
                         
                        Statement intf_st = parent.getValue().getProperty(handler.getMapper().RDFS_Label);
                        intf_name = intf_st == null ? " " : intf_st.getString();

                        interfaces.setProperty(pdomain + ".edge.interface", intf_name);
                        r.reservation.setConfigurationProperty(pdomain + ".edge.interface", intf_name);
           
                        r.reservation.setConfigurationProperty("config.interface.ports",intf_name);
                    }
                    else{
                    	intf_name=parent.getValue().getURI();
                    	if(intf_name!=null) {
                    		//filter.setProperty(parent_ip_addr,intf_name);
				System.out.println("parent_ip_addr = " + parent_ip_addr + " intf_name = " + intf_name);
            				int index=intf_name.indexOf("@");
            				String ip_addr,host_interface,site_host_interface=null;
            				DatatypeProperty hostInterfaceName = h.getIdm().createDatatypeProperty("http://geni-orca.renci.org/owl/" + "topology.owl#hostInterfaceName");	
            				
            	            if(parent.getValue()!=null){
                        		if(parent.getValue().getProperty(hostInterfaceName)!=null)
                        			site_host_interface=parent.getValue().getProperty(hostInterfaceName).getString();
                        	}
            	            if(site_host_interface==null){
            	            	System.out.println("Host Interface Definition not here: IP address is used as the parent value !!");
            	            	if(parent.getKey().getDownNeighbour()!=null){
            	            		if(parent.getKey().getDownNeighbour().getProperty(hostInterfaceName)!=null)
            	            			site_host_interface=parent.getKey().getDownNeighbour().getProperty(hostInterfaceName).getString();
            	            	}
            	            	else{
            	            		if(parent.getKey().getUpNeighbour()!=null){
            	            			if(parent.getKey().getUpNeighbour().getProperty(hostInterfaceName)!=null)
            	            				site_host_interface=parent.getKey().getUpNeighbour().getProperty(hostInterfaceName).getString();
            	            		}
            	            	}
            	            }
            	            if(site_host_interface==null){
            	            	System.out.println("Host Interface Definition not here: neither up neighbor or down neighbor!!");
            	            	site_host_interface="eth0";
            	            }
            	            
            				System.out.println("Site host interface:"+site_host_interface);	
            				
            				if(index>0){
            					ip_addr=intf_name.substring(0,index);
            					host_interface = String.valueOf(Integer.valueOf(intf_name.substring(index+1)).intValue()+1);
            					parent_ip_addr=parent_ip_addr.concat(host_interface).concat(".ip");
    	            			r.reservation.setConfigurationProperty("unit.eth"+host_interface+".hosteth",site_host_interface);
            					if(d.getPrecededBySet().size()>1){
            						parent_tag_name=parent_tag_name.concat(host_interface).concat(".vlan.tag");
            					}
          
            				}
            				//FIXME: (1) if hosteth is not given; (2) if IP is given in multiple domain request.
            				else{
            					ip_addr=intf_name;
            					parent_ip_addr=parent_ip_addr.concat(String.valueOf(r.networkDependencies)).concat(".ip");
            					if(d.getPrecededBySet().size()>=1){
            						r.reservation.setConfigurationProperty("unit.eth"+String.valueOf(r.networkDependencies)+".hosteth",site_host_interface);
            						parent_tag_name=parent_tag_name.concat(String.valueOf(r.networkDependencies)).concat(".vlan.tag");
            					}
            					
            				}
            				filter.setProperty("unit.vlan.tag",parent_tag_name);
            				try {
								InetAddress addr1 = InetAddress.getByName(ip_addr.split("/")[0]);
								
								r.reservation.setConfigurationProperty(parent_ip_addr, ip_addr);
								
							} catch (UnknownHostException e) {
								logger.debug("It is not a valid IP address:"+parent_ip_addr+":"+ip_addr);
							}
                    	}
                    	else{
        	            	System.out.println("Edge interface name is unknown!");
        	        	}
                    }
                //}
                System.out.println("   depends on: " + pr.domain + " filter: " + filter.toString() + " interfaces: " + interfaces.toString());

                // set the relationship
                r.reservation.addRedeemPredecessor(pr.reservation, filter);
            }
        }

        ArrayList<IServiceManagerReservation> result = new ArrayList<IServiceManagerReservation>(map.size());
        for (ReservationRequest r : map.values()) {
            result.add(r.reservation);
        }

        return result;
    }
}
