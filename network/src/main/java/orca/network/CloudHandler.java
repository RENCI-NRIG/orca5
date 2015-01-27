package orca.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import java.util.Hashtable;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import orca.ndl.*;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchingAction;

public class CloudHandler extends MappingHandler {
	
	public CloudHandler(){
		super();		
	}
	
	public CloudHandler(String subFile) throws IOException{
		super(subFile);
		
		NetworkConnection connection = new NetworkConnection();
		this.mapper.setDeviceConnection(connection);
	}
	
	public CloudHandler(OntModel model) throws IOException{
		super(model);
		
		NetworkConnection connection = new NetworkConnection();
		this.mapper.setDeviceConnection(connection);
	}
	
	public void addRequest(OntModel requestModel){
	    //if(requestModel!=null)
    	//requestModel.write(System.out);
		if(this.mapper==null){
			logger.error("Cloud controller mapper is NULL!");
		}
		this.mapper.addRequest(requestModel);
		this.requestModel=requestModel;
	}
	
	//it should have gotten the requestMap 
	public LinkedList <Device> handleMapping(OntModel requestModel){
	    addRequest(requestModel);
	    
	    return handleMapping(true);
	}
	
	public LinkedList <Device> handleMapping(boolean isConnection){
	    logger.debug("**************Handle a new VT request in domain:"+mapper.domain);
	    
	    OntResource rs1 = null,rs2 = null,rs1_ip=null,rs2_ip=null, rs1_device=null,rs2_device=null;
		String rs1_str = null,rs2_str = null,rs1_ip_str=null,rs2_ip_str=null,resourceType;
		long bw = 0;
		String connectionName;
		
		NetworkElement link_device=null,device=null;
    	boolean success=true;	
    	NetworkConnection requestConnection=null;
    	int numLink=0;
    	DomainResourceType type = null;
    	LinkedList <NetworkElement> deviceList=mapper.deviceConnection.getConnection();
    	Resource edge_intf;
 
    	//this means the request is either a cluster of VMs or just a number of VMs
    	//if(requestMap.size()==0){
    	//	this.handleRequest();
    	//}
    	String realDomain = null;
    	//this is for VT within the domain
	    for(Entry <String,NetworkConnection > entry:requestMap.entrySet()){
			connectionName=entry.getKey();
			requestConnection=entry.getValue();
			rs1_str=requestConnection.getEndPoint1();
			rs2_str=requestConnection.getEndPoint2();
			rs1_ip_str=requestConnection.getEndPoint1_ip();
			rs2_ip_str=requestConnection.getEndPoint2_ip();
			bw=requestConnection.getBw();
			resourceType= requestConnection.getType();
			rs1=requestModel.getOntResource(rs1_str);
			rs2=requestModel.getOntResource(rs2_str);
			
			if(rs1_ip_str!=null)
				rs1_ip=requestModel.createOntResource(rs1_ip_str);
			
			if(rs2_ip_str!=null)
				rs2_ip=requestModel.createOntResource(rs2_ip_str);
			
			realDomain = requestConnection.getEndPoint1_domain();
			if(realDomain!=null){
				this.mapper.domain=realDomain;
			}
			edge_intf=getEdgeInterfaceStr();
			//Use the label to represent the link, metric to hold the two end points, 
			String connectionType= requestConnection.getType();
			if(connectionType==null)
					connectionType="None";
			if(connectionName.startsWith("None")){
				connectionName=this.mapper.domain+"/"+connectionName;
			}			
			logger.info("Request Connection:"+connectionName+":"+connectionType+":"+rs1_str+":"+rs1_ip_str+"-"+rs2_str+":"+rs2_ip_str+" BW="+requestConnection.getBw());
			if((connectionName!="None")&& (isConnection==true)&&(!connectionType.equals("None"))){
				if(!(connectionName.split("\\/")[0].equalsIgnoreCase("None")) || ((connectionName.split("\\/")[0].equalsIgnoreCase("None")) && (numLink==0))){ //Nodegroup w/IP request
					link_device = new Device(mapper.ontModel,this.mapper.domain+"/vlan",connectionName);
                                	type=mapper.getResourceType("http://geni-orca.renci.org/owl/domain.owl#VLAN");
					logger.debug("CloudHandler: link_device: url:name " +link_device.getUri()+":"+link_device.getName()+" BW="+requestConnection.getBw());
					type.setCount(1);
					SwitchingAction action=new SwitchingAction();
					String action_layer="EthernetElement";
					if (action_layer != null) {
						action.setAtLayer(action_layer);
						action.setDefaultAction("VLANtag");
						action.setBw(requestConnection.getBw());
					}
					Interface intf=new Interface();
					if(edge_intf!=null){
						intf.setURI(edge_intf.getURI());	
                                                if(edge_intf.getProperty(mapper.RDFS_Label) != null)
            						intf.setName(edge_intf.getProperty(mapper.RDFS_Label).getString());
						intf.setResource(edge_intf);
					}
					action.addInterface(intf);
					link_device.addSwitchingAction(action);
					link_device.setResourceType(type);
					deviceList.add(link_device);
				}
			}
			if(rs1_str!=null){
				device=requestConnection.getDevice1();
				
				if(device!=null){
						if(mapper.getDevice(device.getResource(), deviceList)==null){
						deviceList.add(device);
					}

					type=device.getResourceType();
					if(type==null){
						type=new DomainResourceType();
					}
				}else{
					type=new DomainResourceType();
					type.setResourceType(requestConnection.getEndPoint1_type());
				}
				
				if(requestConnection.getResourceCount1()==0){
					type.setCount(1);
				}else{
					type.setCount(requestConnection.getResourceCount1());
				}
				if((rs1!=null) && rs1.hasRDFType(mapper.interfaceOntClass)){
					rs1_device=getDeviceOfInterface(rs1_str);
				}
				else{
					rs1_device=requestModel.createOntResource(rs1_str);
				}
				
				if(device==null){
					device=mapper.getDevice(rs1_device, deviceList);
				}
				if(device==null){
					device=new Device(mapper.ontModel,this.mapper.domain+"/vm",rs1_device.getURI());
					deviceList.add(device);                 
				}else{
					device.setUri(this.mapper.domain+"/vm");
				}
				device.setResourceType(type);
				device.setIPAddress(rs1_ip_str);
				if(rs1!=null){
					if(rs1.getProperty(this.mapper.dependOn)!=null){
						device.setNodeDependency(rs1.getProperty(this.mapper.dependOn).getResource());
						device.setUpNeighbour(rs1.getProperty(this.mapper.dependOn).getResource());
					}
				}
				if(device.getVMImageURL()==null){
					device.setVMImageURL(this.mapper.getVMImageURL(rs1));
					device.setVMImageGUID(this.mapper.getVMImageGUID(rs1));
				}
				if(device.getPostBootScript()==null){
					if(rs1!=null)
						device.setPostBootScript(this.mapper.getPostBootScript(rs1));
				}
	    		logger.debug("Device "+rs1+" imageURL=" + device.getVMImageURL()+" imageGUID="+device.getVMImageGUID()+" postBootScript="+device.getPostBootScript());
				
				if(link_device!=null){
					if(rs1_ip!=null){
						device.setPrecededBy(link_device, rs1_ip);
						link_device.setFollowedBy(device, rs1_ip);
					}
					else{
						device.setPrecededBy(link_device,edge_intf);
						link_device.setFollowedBy(device, edge_intf);
					}
					if(device.getUpNeighbour()==null){
						device.setUpNeighbour(edge_intf);
						link_device.setDownNeighbour(edge_intf);
					}else{
						if(link_device.getDownNeighbour()==null){
							link_device.setDownNeighbour(edge_intf);
						}
					}
				}
				logger.info("CloudHandler: vm_device 1: url:name " +device.getUri()+":"+device.getName()+":"+device.getResourceType().toString());
				if(link_device!=null)
					logger.info("link_device:"+link_device.getURI()+":"+link_device.getName());
			}
	
			if(rs2_str!=null){
				type=new DomainResourceType();
				type.setResourceType(mapper.getResourceType("http://geni-orca.renci.org/owl/compute.owl#VM").getResourceType());
				
				if(requestConnection.getResourceCount2()==0){
					type.setCount(1);
				}else{
					type.setCount(requestConnection.getResourceCount2());
				}
				if((rs2!=null) && rs2.hasRDFType(mapper.interfaceOntClass)){
					rs2_device=getDeviceOfInterface(rs2_str);
				}
				else{
					rs2_device=requestModel.createOntResource(rs2_str);
				}
				
				device=mapper.getDevice(rs2_device, deviceList);
				
				if(device==null){
					device=new Device(mapper.ontModel,this.mapper.domain+"/vm",rs2_device.getURI());
  					device.setResourceType(type);
					device.setIPAddress(rs2_ip_str);
					deviceList.add(device);
				}
				if(rs2!=null){
					if(rs2.getProperty(this.mapper.dependOn)!=null){
						device.setNodeDependency(rs2.getProperty(this.mapper.dependOn).getResource());
						device.setUpNeighbour(rs2.getProperty(this.mapper.dependOn).getResource());
					}
				}
				if(device.getVMImageURL()==null){
					device.setVMImageURL(this.mapper.getVMImageURL(rs2));
					device.setVMImageGUID(this.mapper.getVMImageGUID(rs2));
				}
				if(device.getPostBootScript()==null){
					if(rs2!=null)	
						device.setPostBootScript(this.mapper.getPostBootScript(rs2));
				}
	    		logger.debug("Device="+rs2+" imageURL=" + device.getVMImageURL()+" imageGUID="+device.getVMImageGUID()+" postBootScript="+device.getPostBootScript());
				
				if(link_device!=null){
					if(rs2_ip!=null){
						device.setPrecededBy(link_device, rs2_ip);
						link_device.setFollowedBy(device, rs2_ip);
					}
					else{
						device.setPrecededBy(link_device,edge_intf);
						link_device.setFollowedBy(device, edge_intf);
					}
					if(device.getUpNeighbour()==null){
						device.setUpNeighbour(edge_intf);
						link_device.setDownNeighbour(edge_intf);
					}else{
						if(link_device.getDownNeighbour()==null){
							link_device.setDownNeighbour(edge_intf);
						}
					}
				}
				logger.info("CloudHandler: vm_device 2: url:name " +device.getUri()+":"+device.getName()+":"+device.getResourceType().toString());
				if(link_device!=null)
					logger.info("link_device:"+link_device.getURI()+":"+link_device.getName());
			}
			numLink++;
	    }

	    //nodeGroup dependency scan
	    nodeGroupDependency(deviceList);
	    
	    return deviceList;
	}
	
	public void nodeGroupDependency(LinkedList <Device> deviceList){
		for(Device child:deviceList){
			Resource rs_parent = child.getNodeDependency();
			logger.info("Master-Slave:"+child.getResource()+":"+child.getName()+":"+rs_parent);
			if(rs_parent!=null){
				for(Device parent:deviceList){	
					logger.info("Master:"+parent.getResource()+":"+parent.getName()+":"+rs_parent);
					if((rs_parent!=null) && (parent.getResource()!=null)){
						if(parent.getResource().getURI()==null) continue;
						if((parent.getResource().getURI().contains(rs_parent.getURI())) || (parent.getName().contains(rs_parent.getURI()))){
							if(child.getName()==null) continue; 
							if(!child.getName().equals(parent.getName())){
								logger.info("Master-Slave to be:"+child.getResource()+":"+rs_parent+":"+parent.getResource());
								parent.setFollowedBy(child,requestModel.createOntResource(child.getIPAddress()));
								child.setPrecededBy(parent,requestModel.createOntResource(parent.getIPAddress()));
							}
						}
					}
				}
			}
		}
	}
	
	//virtual cluster
	public void  handleRequest(){
		DomainResourceType rType=null;
		//this.requestModel.write(System.out);
		String queryPhrase=mapper.createQueryStringSite();
        ResultSet results = mapper.rdfQuery(this.requestModel,queryPhrase);
        //mapper.outputQueryResult(results);
        //results = mapper.rdfQuery(this.requestModel,queryPhrase);
        
		String var0=(String) results.getResultVars().get(0);//connection
		String var1=(String) results.getResultVars().get(1);//node
		String var2=(String) results.getResultVars().get(2);//resource type
		String var3=(String) results.getResultVars().get(3);//units
		String var4=(String) results.getResultVars().get(4);//bw
		
		QuerySolution solution=null;
		Resource rs0 = null,rs1 = null,rs2=null;
		String rs3_str=null,rs4_str=null;
		long bw=0;
		int units=0;
		NetworkConnection requestConnection=null;
		String connectionName=null;
		while(results.hasNext()){
			solution=results.nextSolution();
			rs0=solution.getResource(var0);
			rs1=solution.getResource(var1);
			rs2=solution.getResource(var2);
			if(solution.getLiteral(var3)!=null){
				rs3_str=solution.getLiteral(var3).getValue().toString();
				units=Integer.valueOf(rs3_str).intValue();
			}
			if(solution.getLiteral(var4)!=null){
				rs4_str=solution.getLiteral(var4).getValue().toString();
				bw=Long.valueOf(rs4_str).longValue();
			}
			
			
			requestConnection=new NetworkConnection();
			if(rs0!=null){			
				requestConnection.setResource(rs0);
				if(rs0==null) connectionName=rs1.getURI();
				else connectionName=rs0.getURI();
				rType=mapper.getResourceType("http://geni-orca.renci.org/owl/domain.owl#VLAN");
				requestConnection.setType(rType.getResourceType());
				requestConnection.setBw(bw);
			}
			else{
				connectionName="None";
			}
			requestConnection.setName(connectionName);
			requestConnection.setEndPoint1(rs1.getURI());
			
			if(units==0){
				rType = mapper.requestResourceType(rs1);
				units=rType.getCount();
			}
			//System.out.println(rs0+":"+rs1+":"+rType+":"+connectionName);
			requestConnection.setResourceCount1(units);
			
			if(requestConnection!=null) 
				requestMap.put(connectionName, requestConnection);	
		}	
	}
	
	public OntResource getDeviceOfInterface(String rs){
		Resource resource = null;
		String queryPhrase = mapper.createQueryStringInterfaceOfDevice(rs);
		ResultSet results = mapper.rdfQuery(this.requestModel, queryPhrase);
		String var0=(String) results.getResultVars().get(0);
		QuerySolution solution=null;
		while (results.hasNext()){
			solution=results.nextSolution();
			resource=solution.getResource(var0);
		}
		//System.out.println("Device of Interface:"+resource);
		OntResource or = null;
		if(resource!=null) 
			or=requestModel.getOntResource(resource);
		return or;
	}
	
	public Resource getEdgeInterfaceStr(){
		
		Resource domain_rs=this.idm.getResource(this.mapper.domain+"/vm");
		
		//System.out.println(domain_rs);
		//this.idm.write(System.out);
		if(domain_rs.getProperty(mapper.hasInterface)==null){
			logger.error("Domain doesn't have interface:"+domain_rs+"\n");
			return null;
		}else{
			Resource intf_rs = domain_rs.getRequiredProperty(mapper.hasInterface).getResource();
		
			Statement intf_st=intf_rs.getProperty(this.mapper.RDFS_Label);
			String intf_name=intf_st==null?"":intf_st.getString();
		
			logger.debug(domain_rs+":"+intf_rs.getURI()+":"+intf_name);
		
			return intf_rs;
		}
	}
}
