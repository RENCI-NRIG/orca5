package orca.embed;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.cloudembed.MultiPointNetworkHandler;
import orca.embed.cloudembed.NetworkHandler;
import orca.embed.cloudembed.PortHandler;
import orca.embed.cloudembed.controller.InterDomainHandler;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestMappingException;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.RequestParserListener;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlRequestParser;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchingAction;
import orca.util.PropList;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class InterDomainHandlerTest extends MappingHandlerTest {

	String requestFileRenci,requestFileDuke,requestFileRenciUNC,requestFile2,requestFileDukeRenci,requestFile;
	String requestFile3,requestFile4,requestFile5,requestFile33,requestFile20Duke,requestFileVMSDuke,requestFileUNC;
	InterDomainHandler handler;
	
	String substrateFileName,nlrsubstrateFileName,rencivmsubstrateFileName,dukevmsubstrateFileName,uncvmsubstrateFileName;
	NetworkHandler benhandler,mphandler;
	PortHandler portHandler1, portHandler2;
	
	protected void setUp() throws Exception {
		super.setUp();
		requestFile20Duke = "orca/ndl/request/mp-request-20-duke.rdf";  //Renci/Euca - Umass
		requestFile5 = "src/main/resources/orca/ndl/request/idRequest-rencivm-umass.rdf";  //Renci/Euca - Umass
		requestFile4 = "src/main/resources/orca/ndl/request/idRequest4.rdf";  //UNC/Euca - Umass
		requestFile3 = "orca/ndl/request/idRequest3.rdf";  //Duke/Port/6509/GE-Renci/Port/6509/GE, 2
		requestFile2 = "src/main/resources/orca/ndl/request/idRequest2.rdf";  //Duke/Euca - Umass
		requestFileRenciNersc="src/main/resources/orca/ndl/request/idRequest-renciEuca-nerscEuca.rdf";  //Nersc/Euca - Renci/Euca
		requestFileDukeRenci = "src/main/resources/orca/ndl/request/idRequest-dukeEuca-renciEuca.rdf";  //Duke/Euca - Renci/Euca
		
		requestFileRenciUNC = "src/test/resources/orca/embed/TS3/TS3-3.rdf";  //UNC/Euca - Renci/Euca
		//requestFileRenciUNC = "src/main/resources/orca/ndl/request/paul-unc-renci-request.rdf";  //UNC/Euca - Renci/Euca
		
		handler=new InterDomainHandler();
		
		substrateFileName = "orca/ndl/substrate/ben-6509.rdf";
		nlrsubstrateFileName = "orca/ndl/substrate/nlr.rdf";
		
        benhandler = new NetworkHandler(substrateFileName);
        
        mphandler = new MultiPointNetworkHandler(nlrsubstrateFileName);
        
        rencivmsubstrateFileName = "orca/ndl/substrate/rencivmsite.rdf";
        dukevmsubstrateFileName = "orca/ndl/substrate/dukevmsite.rdf";
        uncvmsubstrateFileName = "orca/ndl/substrate/uncvmsite.rdf";
        
        portHandler1 = new PortHandler(rencivmsubstrateFileName);
        portHandler2 = new PortHandler(dukevmsubstrateFileName);
	}
	
	public void testRequestHandle() throws IOException, RequestMappingException, InetNetworkException, NdlException{
		String reqStr = NdlCommons.readFile(requestFileRenciUNC);
		abstractModels=getAbstractModels();
		handler.addSubstrateModel(abstractModels);
		
		DomainResourcePools drp = new DomainResourcePools(); 
		drp.getDomainResourcePools(pools);
		
		RequestParserListener parserListener = new RequestParserListener();		
		// run the parser (to create Java objects)
		NdlRequestParser nrp = new NdlRequestParser(reqStr, parserListener);
		nrp.processRequest();		
		RequestReservation request = parserListener.getRequest();
		boolean bound = request.generateGraph(request.getElements());
		HashMap<String,BitSet> controllerAssignedLabel = new HashMap<String,BitSet>(); //local set
		handler.setControllerAssignedLabel(controllerAssignedLabel);
		handler.runEmbedding(request.getDomainRequestReservation(request.Interdomain_Domain), drp);
		
		String reservation=request.getReservation();
		
		LinkedList<NetworkElement> connection = handler.getDeviceList();

		print(connection);
		convertToOrca(connection);

		handler.getMapper().removeInConnectionProperty("ndl:inConnection",handler.getMapper().inConnection);
	}
	
	public void print(LinkedList<NetworkElement> connection) throws IOException, RequestMappingException, NdlException {
			OntModel request=null;

	        for (NetworkElement domain : connection) {
	        	DomainElement d = (DomainElement) domain;

	        	OutputStream out = null;
	            //request NDL model to the SA
	        	if(d.getURI().equals("http://geni-orca.renci.org/owl/nlr.rdf#nlr/Domain/vlan")
	        			|| d.getURI().equals("http://geni-orca.renci.org/owl/ben.rdf#ben/Domain/vlan")){
	        		System.out.println("NLR subRequest!!");
	        		request=d.domainRequest();
	        		
	        		out = new  ByteArrayOutputStream();
	        		request.write(out);
	        	}
	            //request.write(System.out);  

	            
	            if(d.getURI().equals("http://geni-orca.renci.org/owl/ben.rdf#ben/Domain/vlan"))
					try {
						//request NDL model to the SA
			            request=d.domainRequest();
			            String fileName = d.getDomainName() + "-subrequest.rdf";
			            OutputStream fsw = new FileOutputStream(fileName);
			            request.write(fsw);
			            //request.write(System.out);  
						BenControl(out.toString());
					} catch (InetNetworkException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            if(d.getURI().equals("http://geni-orca.renci.org/owl/nlr.rdf#nlr/Domain/vlan"))
					try {
						//request NDL model to the SA
			            request=d.domainRequest();
			            String fileName = d.getDomainName() + "-subrequest.rdf";
			            OutputStream fsw = new FileOutputStream(fileName);
			            request.write(fsw);
			            //request.write(System.out);  
						MPControl(out.toString());
					} catch (InetNetworkException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            //if(d.getURI().equals("http://geni-orca.renci.org/owl/rencivmsite.rdf#rencivmsite/Domain/GEPort")) d.setDownNeighbourPortsList(portNDLControl(out.toString(),1));
	            //if(d.getURI().equals("http://geni-orca.renci.org/owl/dukevmsite.rdf#dukevmsite/Domain/GEPort")) d.setDownNeighbourPortsList(portNDLControl(out.toString(),2));
	            
	            System.out.println("===> Device name=" + d.getName() + "(" + d.getURI() + ")" + " action count=" + d.getActionCount());
	            System.out.println("isAllocatable="+d.isAllocatable()+":"+d.getSwappingCapability()+":"+d.isLabelProducer()+":"+d.getDegree()+":"+d.isDepend()+"\n");
	            //System.out.println("Reservation Term:"+d.getStartingTime()+":"+d.getEndingTime()+"\n");
	            if( d.getResourceType()!=null)
	            	System.out.println("Reservation unit:" + d.getResourceType().getResourceType()+":"+d.getResourceType().getCount());
	            Statement intf_st=null;
	            String intf_name=null;
	            if(d.getPrecededBy()!=null) {
	            	for (Entry<DomainElement, OntResource> parent : d.getPrecededBySet()){
	            		if(parent.getValue()!=null){
	            		intf_st=parent.getValue().getProperty(NdlCommons.RDFS_Label);
	            		intf_name=intf_st==null?parent.getValue().getURI():intf_st.getString();
	            		/*if(parent.getKey().getDownNeighbourPortsList()!=null){
	            			intf_name=parent.getKey().getDownNeighbourPortsList();
	            			parent.getValue().addProperty(handler.getMapper().RDFS_Label,intf_name);
	            		}*/	
	            		}
	            		System.out.println("Precedded By:"+parent.getKey().getURI()+":"+parent.getValue());
	            		System.out.println(intf_name+":"+parent.getValue()+":"+parent.getValue()+"\n");
	            	}
	            }
	            if(d.getFollowedBy()!=null) {
	            	for (Entry<DomainElement, OntResource> follower : d.getFollowedBySet()){
	            		intf_st=follower.getValue().getProperty(NdlCommons.RDFS_Label);
	            		intf_name=intf_st==null?follower.getValue().getURI():intf_st.getString();
	            		/*if(d.getDownNeighbourPortsList()!=null){
	            			intf_name=d.getDownNeighbourPortsList();
	            			follower.getValue().addProperty(handler.getMapper().RDFS_Label,intf_name);
	            		}*/	
	            		System.out.println("Followed By:"+follower.getKey().getURI()+":"+follower.getValue().getProperty(NdlCommons.RDFS_Label));
	            		System.out.println(intf_name+":"+follower.getValue()+":"+follower.getValue().getProperty(NdlCommons.layerLabelIdProperty)+"\n");
	                    
	            	}
	            }
	      
	            LinkedList<SwitchingAction> actions = d.getActionList();
	            if(actions==null) continue;
	            float label_id = d.getStaticLabel();
	            for (SwitchingAction a : actions) {
	            	if(label_id==0)
	            		label_id = a.getLabel_ID();
	                System.out.print("Action=" + a.getDefaultAction() + " Label=" + label_id+ " BandWidth = " +a.getBw()+"\n");
	    
	                LinkedList<Interface> ifs = a.getClientInterface();
	                for (Interface iff : ifs) {
	                	Resource rs=iff.getResource();
	                	
	                    System.out.print(" Interface="+iff.getName()+":" +iff.getType()+":"+ rs.getURI()+"\n");
	                    		//+rs.getProperty(handler.getOntProcessor().linkTo).getResource().getProperty(handler.getOntProcessor().interfaceOf).getResource().getURI()+"\n");
	                }
	    
	                System.out.println();
	            }
	            
	        }
	}
	
	public void BenControl(String benRequest) throws IOException, InetNetworkException, NdlException{
		RequestParserListener parserListener = new RequestParserListener();		
		// run the parser (to create Java objects)
		NdlRequestParser nrp = new NdlRequestParser(benRequest, parserListener);
		nrp.processRequest();		
		RequestReservation request = parserListener.getRequest();
		String domainName=request.getReservationDomain();
		benhandler.setDebugOn();
        benhandler.runEmbedding(domainName,request); 
      }
	
	public void MPControl(String mpRequest) throws IOException, InetNetworkException, NdlException{
		if(mphandler!=null && mphandler.getIdm()!=null){
			OntModel i_m = mphandler.getIdm();
			mphandler = new MultiPointNetworkHandler(i_m);
		}
		RequestParserListener parserListener = new RequestParserListener();		
		// run the parser (to create Java objects)
		NdlRequestParser nrp = new NdlRequestParser(mpRequest, parserListener);
		nrp.processRequest();		
		RequestReservation request = parserListener.getRequest();
		String domainName=request.getReservationDomain();
		mphandler.setDebugOn();
		SystemNativeError error=mphandler.runEmbedding(domainName,request); 
        
		if(error!=null)
			return;
        
		String request1 = request.getReservation();
        /*
        mphandler.getConnectionTeardownActions(request1);
        try {
			mphandler.releaseReservation(request1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
      }
	
/*	
	public String portNDLControl(String portRequest,int end) throws IOException, RequestMappingException{
		//System.out.println(portRequest);
		
		ByteArrayInputStream is = new ByteArrayInputStream(portRequest.getBytes());
		String portList = null;
		if(end==1){
			
			portHandler1.handleMapping(is);
			
			String request=portHandler1.getCurrentRequestURI();
		
			portList = portHandler1.getPortListToString(portHandler1.getCurrentRequestURI());
			
			portHandler1.releaseReservation(request);
		}
		else if(end==2){
			
			portHandler2.handleMapping(is);
			
			String request=portHandler2.getCurrentRequestURI();
			
			portList = portHandler2.getPortListToString(portHandler2.getCurrentRequestURI());
			
			portHandler2.releaseReservation(request);
		}
		System.out.println("Port List:"+portList);
		
		
		return portList;
	}
	*/
    @SuppressWarnings("unchecked")
	public void print(NetworkConnection con)
    {
        LinkedList<Device> list = (LinkedList<Device>)con.getConnection();
        LinkedList<Device> unc = new LinkedList<Device>();
        LinkedList<Device> renci = new LinkedList<Device>();
        
        /*for (Object o : list) {
            if (o instanceof Device) {
                Device d = (Device)o;
                String uri = d.getUri();
                if (uri.indexOf("#Renci") != -1) {
                    // this is a renci device
                    renci.add(d);
                } else if (uri.indexOf("#UNC") != -1) {
                    // this a UNC device
                    unc.add(d);
                } else {
                    throw new RuntimeException("Device is from an unknown site: " + uri);
                }
            }
        }
        */
        Properties p = new Properties();
        /*print2("unc", unc, p);
        print2("renci", renci, p);
        */
        print2("BEN",list,p);
        System.out.println(p.toString());
        
    }
    
    public void print2(String site, LinkedList<Device> list, Properties p) {
        for (Device d : list) {
            String name = d.getType().toLowerCase().trim();
            if (!name.equals("server")) {            
                LinkedList<SwitchingAction> actions = d.getActionList();
                int actionCount=actions==null ? 0:actions.size();
                
                int anum = 0;
                
                for (int i = 0; i < actionCount; i++) {
                    SwitchingAction a = actions.get(i);
                    if (a.getDefaultAction() ==  LayerConstant.Action.Temporary.toString()) {
                        continue;
                    }
                    if (a.getDefaultAction() == "VLANtag") {
                        PropList.setProperty(p, "vlan.tag", (int)a.getLabel_ID());
                        PropList.setProperty(p, "bandwidth", a.getBw());
                    } // ignore label id otherwise
                    
                    LinkedList<Interface> ifs = a.getClientInterface();
                    if (ifs.size() != 2) {
                        throw new RuntimeException("Can only handle two interfaces");
                    }
                    
                    // unc.polatis.action.1.sport=
                    // unc.polatis.action.1.dport=
                    
                    PropList.setProperty(p, site + "." + name + ".action." + (anum+1) + ".sport", ifs.get(0).getName());
                    PropList.setProperty(p, site + "." + name + ".action." + (anum+1) + ".dport", ifs.get(1).getName()); 
                    
                    System.out.println(site + "." + name + ".action." + (anum+1) + ".sport="+ ifs.get(0).getName());
                    System.out.println(site + "." + name + ".action." + (anum+1) + ".dport="+ ifs.get(1).getName());
                    anum++;
                }        
                
                // unc.polatis.actions=2
                PropList.setProperty(p, site + "." + name + ".actions", anum);
                System.out.println(site + "." + name + ".actions="+ anum);
                
                String alist = "";
                 for (int i = 0; i < anum; i++) {
                    alist += Integer.toString(i+1) + " ";
                 }
                 alist = alist.trim();
                    
                 PropList.setProperty(p, site + "." + name + ".actionslist", alist);
                 System.out.println(site + "." + name + ".actionslist="+ alist);
            }
        }
    }
	

	public static final String UriSeparator = "#";
	public static final String UriSuffix = "/Domain";

	public String getResourceType(String domain) {
	    HashMap<String, String> map = new HashMap<String, String>();
	    map.put("dukevmsite", "duke.vm");
	    map.put("starlight", "starlight.vlan");
	    map.put("nlr", "nlr.vlan");
	    map.put("ben", "ben.vlan");
	    map.put("dukenet", "duke.vlan");
	    map.put("umass", "vice.testbed");
	    map.put("rencivmsite", "renci.vm");
	    
	    return map.get(domain);
	}
	
	public class DummyReservation {
	    public String domain;
	    public String start;
	    public String end;
	    public int units;
	    public String type;
	    public int bw;
	    public LinkedList<ParentReservation> deps = new LinkedList<ParentReservation>();
	}
	
	public class ParentReservation {
	    public DummyReservation r;
	    public Properties filter = new Properties();
	}
	
	public static String getDomainName(Device d) {
	    String temp = d.getURI();
	    int index = temp.indexOf(UriSeparator);
	    if (index >= 0) {
	        int index2 = temp.indexOf(UriSuffix, index);
	        if (index2 >= 0) {
	            return temp.substring(index+1, index2).toLowerCase();
	        }
	        else{
	        	return temp.substring(index+1, temp.length()).toLowerCase();
	        }
	    }
	    return null;
	}
	        
	public void convertToOrca(LinkedList<NetworkElement> connection) {
	    HashMap<Device, DummyReservation> map = new HashMap<Device, DummyReservation>();

	    // first pass: make the reservation objects
	    for (NetworkElement device : connection) {
	    	DomainElement d= (DomainElement) device;
	        String domain = getDomainName(d);
	        //System.out.println("DomainName:"+domain);
	        DummyReservation r = new DummyReservation();
	        r.domain = domain;
	        //r.start = d.getStartingTime() + "";
	        //r.end = d.getEndingTime() + "";
	        if(d.getResourceType()!=null){
	        	r.units = d.getResourceType().getCount(); // FIXME: get it from the device
	        	//r.type = getResourceType(domain);
	        	r.type=d.getResourceType().getResourceType();
	        }
	        //map.put(domain, r);
	        map.put(d, r);
	    }
	    
	    // second pass: set the dependencies
	    System.out.println("Starting the second pass!");
	    for (NetworkElement device: connection) {
	    	DomainElement d= (DomainElement) device;
	        String domain = getDomainName(d);
	        //DummyReservation r = map.get(domain);
	        DummyReservation r = map.get(d);
	        HashMap<DomainElement, OntResource> preds = d.getPrecededBy();
	        if (preds == null) {
	            continue;
	        }
	        int i=0;
	        String parent_tag_name=null; 
	        String parent_ip_addr=null;
	        for (Entry<DomainElement, OntResource> parent : d.getPrecededBySet()) {
	        	parent_tag_name="unit.eth"; 
	        	parent_ip_addr="unit.eth";
	            String pdomain = getDomainName(parent.getKey());
	            //DummyReservation pr = map.get(pdomain);
	            DummyReservation pr = map.get(parent.getKey());
	            ParentReservation dep = new ParentReservation();
	            dep.r = pr;

	            Statement intf_st=parent.getValue().getProperty(handler.getMapper().RDFS_Label);

	            String intf_name=null;
	            if(intf_st!=null)
	            	intf_name=intf_st.getString();
	            else if(parent.getKey().getDownNeighbour(parent.getKey().getModel())!=null){
	            	intf_name=parent.getKey().getDownNeighbour(parent.getKey().getModel()).getURI();
	            }
	            if(intf_name==null) {
	            	intf_name=parent.getValue().getURI();
	            }
	            
	            DatatypeProperty hostInterfaceName = handler.getIdm().createDatatypeProperty("http://geni-orca.renci.org/owl/" + "topology.owl#hostInterfaceName");	
	            String site_host_interface=null;
	            if(parent.getValue()!=null){
            		if(parent.getValue().getProperty(hostInterfaceName)!=null)
            			site_host_interface=parent.getValue().getProperty(hostInterfaceName).getString();
            	}
	            if(site_host_interface==null){
	            	System.out.println("Host Interface Definition not here: IP address is used as the parent value or its neighbors are network domains!!");
	            	System.out.println(parent.getKey()+":"+parent.getKey().getDownNeighbour(parent.getKey().getModel())+":"+parent.getKey().getUpNeighbour(parent.getKey().getModel())+"\n");
	            	if(parent.getKey().getDownNeighbour(parent.getKey().getModel())!=null){
	            		if(parent.getKey().getDownNeighbour(parent.getKey().getModel()).getProperty(hostInterfaceName)!=null){
	            			site_host_interface=parent.getKey().getDownNeighbour(parent.getKey().getModel()).getProperty(hostInterfaceName).getString();
	            		}
	            		else{
	            			if(parent.getKey().getUpNeighbour(parent.getKey().getModel())!=null){
		            			if(parent.getKey().getUpNeighbour(parent.getKey().getModel()).getProperty(hostInterfaceName)!=null)
		            				site_host_interface=parent.getKey().getUpNeighbour(parent.getKey().getModel()).getProperty(hostInterfaceName).getString();
		            		}
	            		}
	            	}
	            	else{
	            		if(parent.getKey().getUpNeighbour(parent.getKey().getModel())!=null){
	            			if(parent.getKey().getUpNeighbour(parent.getKey().getModel()).getProperty(hostInterfaceName)!=null)
	            				site_host_interface=parent.getKey().getUpNeighbour(parent.getKey().getModel()).getProperty(hostInterfaceName).getString();
	            		}
	            	}
	            }
	            if(site_host_interface==null){
	            	System.out.println("Host Interface Definition not here: neither up neighbor or down neighbor!!");
	            	site_host_interface="eth0";
	            }
	            
				System.out.println("Site host interface:"+site_host_interface);	
	            if(intf_name!=null) {
	            	dep.filter.setProperty("edge.interface",intf_name);
	            	int index=intf_name.indexOf("@");
	            	String ip_addr,host_interface;
    				
	            	if(index>0){
	            		ip_addr=intf_name.substring(0,index);
	            		host_interface = intf_name.substring(index+1);
	            		parent_ip_addr=parent_ip_addr.concat(host_interface).concat(".ip");
	            		if(d.getPrecededBySet().size()>1){
	            			parent_tag_name=parent_tag_name.concat(host_interface).concat(".vlan.tag");
	    	            }
	            		else{
	    	            	parent_tag_name= pdomain + ".vlan.tag";
	    	            }
	            	}
	            	else{
	            		ip_addr=intf_name;
	            		parent_ip_addr=parent_ip_addr.concat(String.valueOf(i)).concat(".ip");
	            		if(d.getPrecededBySet().size()>1){
	            			parent_tag_name=parent_tag_name.concat(String.valueOf(i)).concat(".vlan.tag");
	            		}
	            		else{
	    	            	parent_tag_name= pdomain + ".vlan.tag";
	    	            }
	            	}
	            	dep.filter.setProperty("unit.vlan.tag",parent_tag_name);
	            	dep.filter.setProperty(parent_ip_addr,ip_addr);
	            }
	            else{
	            	System.out.println("Edge interface name is unknown!");
	        	}
	            //System.out.print(parent_ip_addr+":"+dep.filter.getProperty(parent_ip_addr)+":"+dep.filter.getProperty("unit.vlan.tag")+":"+parent.getValue().getURI()+"\n");
	            r.deps.add(dep);
	            i++;
	        }
	        LinkedList<SwitchingAction> actions = d.getActionList();
	        if(actions!=null){
	        	for (SwitchingAction a : actions) {
               		System.out.print("Domain: " + domain + " Action=" + a.getDefaultAction() + " Label=" + a.getLabel_ID()+" Bandwidth="+a.getBw()+"\n");
    
                	LinkedList<Interface> ifs = a.getClientInterface();
                	for (Interface iff : ifs) {
                    	System.out.println("    Interface: " + iff.getName()+":"+iff.getURI());
                	}
    
                	System.out.println();
            	}
	        }
	    }

	    
	    // print what we did:
	    
	    for (DummyReservation r : map.values()) {
	        System.out.println("from " + r.domain + " units=" + r.units + " type=" + r.type);
	        int i =0;
	        for (ParentReservation pr : r.deps) {
	            System.out.println("   depends on: " + pr.r.domain);
	            System.out.println(" property: " + pr.filter.getProperty("unit.vlan.tag"));
	            System.out.println(": "+pr.filter.getProperty("edge.interface"));
	            System.out.println(": "+pr.filter.getProperty("unit.eth"+String.valueOf(i)+".ip"));
	            System.out.println(pr.filter.toString());
	            i++;
	        }
	    }
	}
}
