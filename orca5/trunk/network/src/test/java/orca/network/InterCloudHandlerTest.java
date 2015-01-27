package orca.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import junit.framework.TestCase;
import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.cloudembed.NetworkHandler;
import orca.embed.cloudembed.PortHandler;
import orca.embed.policyhelpers.RequestMappingException;
import orca.embed.workflow.Domain;
import orca.ndl.DomainResourceType;
import orca.ndl.DomainResources;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchingAction;
import orca.shirako.common.ResourceType;
import orca.shirako.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.meta.ResourcePoolAttributeType;
import orca.shirako.meta.ResourcePoolDescriptor;
import orca.shirako.meta.ResourcePoolsDescriptor;
import orca.shirako.meta.ResourceProperties;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class InterCloudHandlerTest extends TestCase {

	String [] inputFileName={"orca/network/gpovmsite.rdf","orca/network/dukevmsite.rdf","orca/network/rencivmsite.rdf",
			"orca/network/ricevmsite.rdf","orca/network/uhoustonvmsite.rdf",
			"orca/network/uncvmsite.rdf","orca/network/nersc.rdf",
			"orca/network/ben-6509.rdf",
			"orca/network/nlr.rdf","orca/network/starlight.rdf","orca/network/ion.rdf","orca/network/nerscNet.rdf",
			"orca/network/renciNet.rdf","orca/network/uncNet.rdf","orca/network/dukeNet.rdf",
			"orca/network/riceNet.rdf","orca/network/uhoustonNet.rdf","orca/network/learnNet.rdf","orca/network/learn.rdf"};
	
	String requestFile,requestFileDuke,requestFile33,requestFileVMSDuke,requestFileUNC,requestFileRenci,requestFileDukeUnc;
	String requestFile2,requestFileNSF,requestFileDukeRenci,requestFile0,requestFileUncRenci,requestFileConverter;
	String requestFileDuke26Node,requestFileGPO,requestFileGush,requestFileRenciNersc;
	String requestFileCondor,requestFileHadoop,requestFileDukeVMSVlan,requestFileRenciVMSVlan;
	String substrateFileName,rencivmsubstrateFileName,dukevmsubstrateFileName,uncvmsubstrateFileName;
	String requestFileDukeUHouston,requestFileDukeRice;
	String requestFileFluke;
	InterCloudHandler handler;
	NetworkHandler benhandler;
	PortHandler portHandler1, portHandler2;
	
	public InterCloudHandlerTest(String name) {
		super(name);
		
	}

	protected void setUp() throws Exception {
		super.setUp();
		requestFileFluke="orca/network/fluke-request-pegasus.rdf"; //fluke
		requestFileRenciNersc="orca/network/idRequest-renciEuca-nerscEuca.rdf";  //Nersc/Euca - Renci/Euca
		requestFileCondor = "orca/network/condor-request-1.rdf";  //a condor cluster 1/ 1 master and 5 slaves
		requestFileHadoop = "orca/network/hadoop-request-1.rdf";  //a Hadoop cluster 1/ 1 master and 30 slaves
		requestFile0 = "orca/network/idRequest-uncEuca-renciEuca.rdf";  //UNC/Euca - Renci/Euca
		requestFileNSF = "orca/network/nsf.rdf"; //unbounded NSF
		requestFileDuke = "orca/network/triangle-mp-dukevmsite-request.rdf"; //a Triangle VT in dukevmsite	
		requestFileRenci = "orca/network/id-mp-rencivmsite-request.rdf"; //a Triangle VT in rencivmsite
		requestFileUNC = "orca/network/id-mp-uncvmsite-request.rdf"; //a Triangle VT in uncvmsite
		requestFile33 = "orca/network/id-mp-rencivmsite-request.rdf";//a VM cluster in a VLAN in rencivmsite
		requestFileVMSDuke = "orca/network/vms-duke.rdf";//5 VMS in dukevmsite
		requestFile = "orca/network/id-mp-Request2.rdf"; //a Triangle VT inter-cloud 
		requestFileDukeRenci = "orca/network/idRequest-dukeEuca-renciEuca.rdf";  //Duke/Euca - Renci/Euca
		requestFile2 = "orca/network/idRequest2.rdf";  //Duke/Euca - Umass
		
		requestFileUncRenci = "orca/network/idRequest-uncEuca-renciEuca.rdf";  //UNC/Euca - Renci/Euca
		requestFileDukeUnc = "orca/network/idRequest-dukeEuca-uncEuca.rdf";  //UNC/Euca - Duke/Euca
		
		requestFileConverter = "orca/network/converter-request-link-term.rdf";  //UNC/Euca - Duke/Euca
		
		requestFileDuke26Node = "orca/network/dukevmsite-26node-request.rdf";  // 26 node tree topology in duke vmsite
		
		requestFileDukeVMSVlan= "orca/network/duke-vms-vlan-request.rdf";  // a vlan connected 6-node cluster in duke vmsite
		requestFileRenciVMSVlan= "orca/network/renci-vms-vlan-request.rdf";  // a vlan connected 3-node cluster in renci vmsite
		
		requestFileGPO = "orca/network/request-converter-image.rdf"; 
		
		requestFileGush = "orca/network/gush-request-2-vm-link.rdf"; 
		
		requestFileDukeUHouston = "orca/network/idRequest-dukeEuca-uhoustonEuca.rdf";
		requestFileDukeRice = "orca/network/idRequest-dukeEuca-riceEuca.rdf";
		
		handler=new InterCloudHandler();
		
		handler.deltaVM = 10;
		handler.maxVMDomain = "http://geni-orca.renci.org/owl/dukevmsite.rdf#DukeVMSite/Domain";
		
		substrateFileName = "orca/network/ben-6509.rdf";
	       
        benhandler = new NetworkHandler(substrateFileName);
        
        rencivmsubstrateFileName = "orca/network/rencivmsite.rdf";
        dukevmsubstrateFileName = "orca/network/dukevmsite.rdf";
        uncvmsubstrateFileName = "orca/network/uncvmsite.rdf";
        
        portHandler1 = new PortHandler(rencivmsubstrateFileName);
        portHandler2 = new PortHandler(dukevmsubstrateFileName);
	}

	public void testRequestHandle() throws IOException, InetNetworkException, RequestMappingException {	
		int numDomain=inputFileName.length;
		String [] type = {"site.vm","site.GEPort","site.TenGEPort","site.vlan"};
		DomainResources domainResources=null;
		ResourcePoolsDescriptor pools=new ResourcePoolsDescriptor();
		ResourcePoolDescriptor pool=null;

		for(int i=0;i<numDomain;i++){	
			if(i==0){
				Domain d = new Domain(inputFileName[i]);
				String abstractModel = d.delegateDomainModelToString("site.vm");
				handler.addAbstractDomainString(abstractModel);
				domainResources=d.getDomainResources(abstractModel);
				pool=getResourcePoolDescriptor(domainResources);
				//System.out.println(pool.getResourceType().toString()+":"+pool.getDescription());
				pools.add(pool);
			}
			else if(i<6){
				for (String j : type){
					Domain d = new Domain(inputFileName[i]);
					String abstractModel = d.delegateDomainModelToString(j);
					handler.addAbstractDomainString(abstractModel);
					domainResources=d.getDomainResources(abstractModel);
					pool=getResourcePoolDescriptor(domainResources);
					pools.add(pool);
				}
			}
			else if(i==6){
				Domain d = new Domain(inputFileName[i]);
				String abstractModel = d.delegateDomainModelToString("nersc.vm");
				handler.addAbstractDomainString(abstractModel);
				domainResources=d.getDomainResources(abstractModel);
				pool=getResourcePoolDescriptor(domainResources);
				pools.add(pool);
				d = new Domain(inputFileName[i]);
				abstractModel = d.delegateDomainModelToString("nersc.GEPort");
				handler.addAbstractDomainString(abstractModel);
				domainResources=d.getDomainResources(abstractModel);
				pool=getResourcePoolDescriptor(domainResources);
				pools.add(pool);
			}
			else {
				Domain d = new Domain(inputFileName[i]);
				String abstractModel = d.delegateDomainModelToString("site.vlan");
				handler.addAbstractDomainString(abstractModel);
				domainResources=d.getDomainResources(abstractModel);
				pool=getResourcePoolDescriptor(domainResources);
				pools.add(pool);
				//if(i==9){
				//	System.out.println(abstractModel);
				//}
			}
		}
		
		handler.getDomainResourcePools(pools);
		
		OntModel model=handler.abstractModel();
		
		Individual link_ont=model.createIndividual("http://geni-orca.renci.org/owl/compute.owl#testServer",handler.mapper.serverCloudOntClass);
		
		handler.handleCloudRequest(requestFileDukeVMSVlan);

		System.out.println("Term:"+handler.mapper.startTime+":"+handler.mapper.endTime+":"+handler.mapper.reservation+"\n");
		
		Calendar cal_start = null,cal_end = null;
		if(handler.mapper.startTime!=null)
			cal_start=DatatypeConverter.parseDateTime(handler.mapper.startTime);
		else
			cal_start=new GregorianCalendar();
		
		if(handler.mapper.endTime!=null)
			cal_end=DatatypeConverter.parseDateTime(handler.mapper.endTime);
		else{
			cal_end=new GregorianCalendar();
			cal_end.add(Calendar.DAY_OF_MONTH,1);
		}
		Date date_start=cal_start.getTime();
		Date date_end=cal_end.getTime();
		
		//Term term = new Term(date_start,date_end);
		int termDuration =(int) handler.mapper.termDuration;
		System.out.println("Term in ORCA:"+date_start+":"+date_end+":"+termDuration);
		
		if(handler.getMapper().getDeviceConnection()!=null){
			print(handler.getMapper().getDeviceConnection().getConnection());
			assert(true);
		}
		else{
			assert(false);
		}
	}
	
	public ResourcePoolDescriptor getResourcePoolDescriptor(DomainResources domainResources){
		ResourcePoolDescriptor pool =new ResourcePoolDescriptor();

		String rdf = null;
		DomainResourceType dType= domainResources.getResourceType().get(0);
		rdf = dType.getDomainURL().split("\\#")[0];
		//if(domainResources.getResources().size()!=0)
		//	rdf=domainResources.getResources().get(0).iface.split("\\#")[0]; 
		String type = dType.getResourceType().toLowerCase();
		
		String value=dType.generateDomainName(rdf, type);
		
		ResourceType rType=new ResourceType(value+"."+type);
		
		//System.out.println("ResourceDescriptor:"+rdf+":"+type+":"+value+":"+rType.toString());
		
		pool.setResourceType(rType);
		pool.setUnits(domainResources.getResourceType().get(0).getCount());
		
		ResourcePoolAttributeDescriptor att = new ResourcePoolAttributeDescriptor();
		att.setKey(ResourceProperties.ResourceDomain);
		att.setValue(value);
		pool.addAttribute(att);
		
		att = new ResourcePoolAttributeDescriptor();
		att.setKey(ResourceProperties.ResourceAvailableUnits);
		att.setType(ResourcePoolAttributeType.INTEGER);
		att.setValue(String.valueOf(domainResources.getResourceType().get(0).getCount()));
		pool.addAttribute(att);
		
		return pool;
	}
	
	public void print(LinkedList<Device> list) throws IOException, RequestMappingException {
		ReservationConverter converter = new ReservationConverter(); 
		
		OntModel request=null;
		String dType=null,type;
		Device device;
		String hostsScript=" ";
		String topologyScript = " ";
		String masterIP=null;
        for (Device d : list) {
        	System.out.println("===> Device name=" + d.getName() + "(" + d.getUri() + ")" + " action count=" + d.getActionCount());
            //request NDL model to the SA
            request=handler.domainRequest(d);
            d.setIdmRequest(request);
            OutputStream out = new  ByteArrayOutputStream();
            request.write(out);
            //request.write(System.out);  
            
            if(d.getURI().equals("http://geni-orca.renci.org/owl/ben.rdf#ben/Domain/vlan"))
				try {
					BenControl(out.toString());
				} catch (InetNetworkException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            if(d.getURI().equals("http://geni-orca.renci.org/owl/rencivmsite.rdf#RenciVMSite/Domain/GEPort")) d.setDownNeighbourPortsList(portNDLControl(out.toString(),1));
            if(d.getURI().equals("http://geni-orca.renci.org/owl/dukevmsite.rdf#DukeVMSite/Domain/GEPort")) d.setDownNeighbourPortsList(portNDLControl(out.toString(),2));
            
            
            //System.out.println(d.getSwappingCapability()+":"+d.isLabelProducer()+":"+d.getDegree()+":"+d.isDepend()+"IP Adress:"+d.getIPAddress()+"\n");
            //System.out.println("VMIMAGEURL:"+d.getVMImageURL()+"\n");
            //System.out.println("VMIMAGEGUID:"+d.getVMImageGUID()+"\n");
            //System.out.println("Reservation Term:"+d.getStartingTime()+":"+d.getEndingTime()+"\n");
            if( d.getResourceType()!=null){
            	System.out.println("Reservation unit:" + d.getResourceType().getResourceType()+":"+d.getResourceType().getCount());
            	dType=d.getResourceType().getResourceType();
            }
            Statement intf_st=null;
            String intf_name=null;
            if(d.getPrecededBy()!=null) {
            	for (Entry<Device,Resource> parent : d.getPrecededBySet()){
            		intf_st=parent.getValue().getProperty(handler.getMapper().RDFS_Label);
            		intf_name=intf_st==null?parent.getValue().getURI():intf_st.getString();
            		device=parent.getKey();
            		if(device.getDownNeighbourPortsList()!=null){
            			intf_name=device.getDownNeighbourPortsList();
            			parent.getValue().addProperty(handler.getMapper().RDFS_Label,intf_name);
            		}	
            		type=device.getResourceType().getResourceType();
            		System.out.println("Precedded By:"+device.getName()+":"+device.getURI()+":"+type+":"+parent.getValue().getProperty(handler.getMapper().RDFS_Label));
            		System.out.println(intf_name+"\n");
            		//System.out.println("Static Label: parent:"+parent.getKey().getStaticLabel()+";d:"+d.getStaticLabel());
            		//System.out.println("Type, postBootScript:"+type+":"+d.getPostBootScript()+":"+parent.getKey().getIPAddress());
            		if(type.endsWith("VM")){
            			if(d.getPostBootScript()!=null){
                  		  //if(d.getPostBootScript().equals("Condor")){
                  			 String hostScript = converter.getClusterHostname(d);
                  			 hostsScript=hostsScript.concat(hostScript).concat("' >> /etc/hosts \n");
                  			 String topology = converter.getVirtualRackScript(d);
                  			 topologyScript=topologyScript.concat(topology);
                  		  //}
                  		  //else{
                  		//	  System.out.println(ReservationConverter.getHadoopScript(intf_name,hostsScript, ""));
                  		 // }
                  	  	}
            			masterIP=intf_name;
            		}
            		
            		 String ip_addr=null,host_interface=null,site_host_interface=null;
                     DatatypeProperty hostInterfaceName = handler.getIdm().createDatatypeProperty("http://geni-orca.renci.org/owl/" + "topology.owl#hostInterfaceName");	
     				
                     //System.out.println("Parent interface:"+parent.getValue()+";"+parent.getKey().getDownNeighbour()+":"+parent.getKey().getUpNeighbour());
                     if(parent.getValue()!=null){
                     	if(parent.getValue().getProperty(hostInterfaceName)!=null)
                 			site_host_interface=parent.getValue().getProperty(hostInterfaceName).getString();
                     }
                     if(site_host_interface==null){
                     	//System.out.println("Host Interface Definition not here: IP address is used as the parent value or its neighbors are network domains!!");
                     	if(parent.getKey().getDownNeighbour()!=null){
                     		if(parent.getKey().getDownNeighbour().getProperty(hostInterfaceName)!=null){
                     			site_host_interface=parent.getKey().getDownNeighbour().getProperty(hostInterfaceName).getString();
                     		}
                     		else{
                     			if(parent.getKey().getUpNeighbour()!=null){
                     				if(parent.getKey().getUpNeighbour().getProperty(hostInterfaceName)!=null)
                     					site_host_interface=parent.getKey().getUpNeighbour().getProperty(hostInterfaceName).getString();
                     			}
                     		}
                     	}
                     	else{
                     		if(parent.getKey().getUpNeighbour()!=null){
                     			if(parent.getKey().getUpNeighbour().getProperty(hostInterfaceName)!=null)
                     				site_host_interface=parent.getKey().getUpNeighbour().getProperty(hostInterfaceName).getString();
                     		}
                     	}
                     }
     	        
                     if(site_host_interface==null){
                     	//System.out.println("Host Interface Definition not here: neither up neighbor or down neighbor!!");
                     	site_host_interface="eth0";
                     }
     	            
                     //System.out.println("Site host interface:"+site_host_interface);	
            		
            	}
            }
            if(d.getFollowedBy()!=null) {
            	for (Entry<Device,Resource> follower : d.getFollowedBySet()){
            		intf_st=follower.getValue().getProperty(handler.getMapper().RDFS_Label);
            		intf_name=intf_st==null?follower.getValue().getURI():intf_st.getString();
            		device=follower.getKey();
            		type=device.getResourceType().getResourceType();
            		if(d.getDownNeighbourPortsList()!=null){
            			intf_name=d.getDownNeighbourPortsList();
            			follower.getValue().addProperty(handler.getMapper().RDFS_Label,intf_name);
            		}	
            		System.out.println("Followed By:"+device.getName()+":"+device.getURI()+":"+type+":"+follower.getValue().getProperty(handler.getMapper().RDFS_Label));
            		System.out.println(intf_name+"\n");
            	}
            	if(dType.endsWith("VM")){
            		if(d.getPostBootScript()!=null){
                		  //if(d.getPostBootScript().equals("Condor")){
                			 String hostScript = converter.getClusterHostname(d);
                			 hostsScript=hostsScript.concat(hostScript).concat("' >> /etc/hosts \n");
                			 String topology = converter.getVirtualRackScript(d);
                			 topologyScript=topologyScript.concat(topology);
                			//}               			 
                		  //else{
                		//	  System.out.println(ReservationConverter.getHadoopScript(d.getIPAddress(), " -master"));
                		 // }
                	  }
        			
        		}
            	
            }
            
            LinkedList<SwitchingAction> actions = d.getActionList();
            if(actions==null) continue;
            for (SwitchingAction a : actions) {
                //System.out.print("Action=" + a.getDefaultAction() + " Label=" + a.getLabel_ID()+ " BandWidth = " +a.getBw()+"\n");
    
                LinkedList<Interface> ifs = a.getSwitchingInterface();
                for (Interface iff : ifs) {
                	Resource rs=iff.getResource();
                	if(rs!=null){
                		//System.out.print(" Interface="+iff.getName()+":" +iff.getType()+":"+ rs.getURI()+"\n");
                	}
                	else{
                		//System.out.println(" Interface="+iff.getName()+":" +iff.getType()+":"+"None");
                	}
                    		//+rs.getProperty(handler.getOntProcessor().linkTo).getResource().getProperty(handler.getOntProcessor().interfaceOf).getResource().getURI()+"\n");
                }
    
                //System.out.println();
            }
            
        }
        
        for (Device d : list) {
        	String bootScript=d.getPostBootScript();
        	if(bootScript==null) continue;
        	String script=null;

        	String hadoopTopologyScript="";
      		int index = bootScript.indexOf("/");
      		
      		hadoopTopologyScript=converter.getHadoopTopologyScript(topologyScript,index);
      		
  			if(d.getFollowedBy()==null){	//slave node
  				if(bootScript.equals("condor")){
      				script=converter.getCondorScript(d,hostsScript,null);
  				}
  				else{
  					script=converter.getHadoopScript(converter.masterHostName,hostsScript,hadoopTopologyScript, "");
  				}
      			//System.out.println(script);
  			}
      		if(d.getFollowedBy()!=null) { //master node
      			if(bootScript.equals("condor")){
          			script=converter.getCondorScript(d,hostsScript,"master");
      			}
  				else{
  					script=converter.getHadoopScript(converter.masterHostName,hostsScript,hadoopTopologyScript, " -master");
  				}
          		//System.out.println(script);
      		}
        }
}
	
	public void BenControl(String benRequest) throws IOException, InetNetworkException{
		System.out.println("Starting BEN provisioning!\n");
		
		ByteArrayInputStream is = new ByteArrayInputStream(benRequest.getBytes());
        
        benhandler.handleMapping(is, true);

        String request=benhandler.getCurrentRequestURI();
        
        NetworkConnection con = benhandler.getLastConnection();

        if(con==null){
        	System.out.println("No path found in BEN!\n");
        }
        else {
        	con.print();
        }
        
        //benhandler.releaseReservation(request);
       }
	
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
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
