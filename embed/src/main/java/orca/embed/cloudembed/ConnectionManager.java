package orca.embed.cloudembed;

import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import orca.embed.cloudembed.controller.InterDomainHandler;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.Label;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchMatrix;
import orca.ndl.elements.SwitchingAction;
import orca.util.persistence.NotPersistent;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ConnectionManager extends RequestMapping {

	// this is used as temporary storage between two methods
	@NotPersistent
	HashMap <OntResource,OntResource[]> nc_intf_list = new HashMap <OntResource,OntResource[]>(); //bookKeeping
	
	ConnectionManager(OntModel rModel, OntModel substrateModel,boolean inter)
			throws IOException {
		super(rModel, substrateModel,inter);
	}
	
	@SuppressWarnings("unchecked")
	public void createVirtualConnection(String reservation,NetworkConnection deviceConnection){
		Device device=null,device_peer=null,device_peer_next=null;
		Resource rs=null,rs_peer=null,rs_peer_next=null; 
		LinkedList <Device> deviceList=null;
		Iterator <Device> it;
		String type=null,type_peer=null,type_peer_next=null;
		
		OntResource connection_ont=null;
		LinkedList <OntResource> top_connection_ont=new LinkedList<OntResource>();
		
		boolean isServer=false;
		try{
		 if(deviceConnection!=null){
			deviceList=(LinkedList<Device>)deviceConnection.getConnection();
			
			int size=deviceList.size();
			int j=0;
			it=deviceList.iterator();
			while(it.hasNext()){
				device=(Device) it.next();
				rs=device.getResource();
				type=device.getType();

				if(type==null){
					j++;
					continue;
				}
				if(type.equals("vm")){
					isServer=true;
				}else{
					isServer=false;
				}
				
				for(int i=j+1;i<size;i++){
					device_peer=deviceList.get(i);
					rs_peer=device_peer.getResource();
					type_peer=device_peer.getType();
								
					logger.info("Device pair:"+device.getURI()+":"+type+";"+device_peer.getURI()+":"+type_peer+"\n");
							
					if(type_peer!=null){		
						if((!isServer) && (!type_peer.equals("vm"))){
							if(peerLayer(device,device_peer)) {
								logger.info("Device peer:"+device.getURI()+":"+type+";"+device_peer.getURI()+":"+type_peer+"\n");
								if(i<size-1){
									device_peer_next=deviceList.get(i+1);
									type_peer_next=device_peer_next.getType();
									if(type_peer_next!=null){
										if((!type_peer_next.equals("vm")) && peerLayer(device,device_peer_next)){												
											if(type_peer_next.equals("vlan")){	//if it is vlan layer, no multi-hop connection
												connection_ont=createPeerConnection(device,device_peer,deviceList);
											}else
												continue;
										}else{
											connection_ont=createPeerConnection(device,device_peer,deviceList);
										}			
										if(connection_ont!=null){ 
											top_connection_ont.add(connection_ont);
											StmtIterator stit=connection_ont.listProperties(NdlCommons.collectionItemProperty);
											Resource item_rs=null;
											OntResource item_ont=null;
											Statement st=null;
											if(stit!=null){
												while(stit.hasNext()){	    
													st=stit.nextStatement();
													item_rs=st.getResource();
													item_ont=ontModel.getOntResource(item_rs);
													logger.info("Virtual NetworkConnection item:"+item_ont);
													if(item_ont.hasRDFType(NdlCommons.topologyNetworkConnectionClass)){  //subconnection or link segment
														if(!top_connection_ont.contains(item_ont))
															top_connection_ont.add(item_ont);;
													}
												}
											}
										}
									}
								}
							}
						}
						else if ((isServer) & (type_peer.equals("vm"))){
							logger.info("----No virtual connection between Servers-----"+rs.getURI()+":"+rs_peer.getURI());
						}
					}						
					break; //no need to consider this virtual connection hop.
				}
					
				j++;
			}

			Resource random_reservation = ontModel.createResource(reservation,NdlCommons.reservationOntClass);	
			for (OntResource top:top_connection_ont) {
				//logger.debug("RequestMapping: carryReservation: " + top.getURI() + ";this.reservation: " + this.reservation);
				logger.info("RequestMapping: carryReservation: " + top.getURI() + ";random.reservation: " + random_reservation.getURI());

				top.addProperty(NdlCommons.carryReservation, random_reservation);
			}
			
		 }
		}catch (Exception e){
			e.printStackTrace();
		} 
		removeInConnectionProperty("ndl:inConnection",inConnection);	
		removeInConnectionProperty("ndl:visited",visited);
	}
	
	public OntResource createPeerConnection(Device device, Device device_peer,LinkedList <Device> deviceList){
		
		Resource intf=null,intf_peer=null,connection_rs=null;
		OntResource connection_ont=null;
		ResultSet results=null;
		
		intf=device.getDownNeighbour(device.getModel());
		OntResource device_ont = ontModel.getOntResource(intf.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource());
		Device neighbor_device=getDevice(device_ont,deviceList);
		//logger.debug("This VC end point 1:"+intf.getURI()+":"+neighbor_device.getURI()+":"+neighbor_device.getUpNeighbour().getURI());
		if(neighbor_device==null)
			return null;
		intf=neighbor_device.getUpNeighbour(neighbor_device.getModel());
		
		intf_peer=device_peer.getUpNeighbour(device_peer.getModel());
		if(intf_peer!=null){
			device_ont = ontModel.getOntResource(intf_peer.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource());
			neighbor_device=getDevice(device_ont,deviceList);
			logger.debug("This VC end point 2:"+intf_peer.getURI()+":"+neighbor_device.getURI()+":"+neighbor_device.getDownNeighbour(neighbor_device.getModel()).getURI());
			intf_peer=neighbor_device.getDownNeighbour(neighbor_device.getModel());
		}
		if((intf!=null) && (intf_peer!=null)){
			results=interfaceOfNetworkConnection(intf.getURI());
		    String var0=(String) results.getResultVars().get(0);
			while(results.hasNext()){
				connection_rs = results.nextSolution().getResource(var0);
				connection_ont=ontModel.getOntResource(connection_rs);
			}
			if(connection_ont!=null)
				return connection_ont;
		}
		
		if(intf!=null){
			if(intf.getProperty(NdlCommons.visited)!=null){
				logger.warn("Interface was used in existing VC:"+intf);
				return null;
			}
			intf.addProperty(NdlCommons.visited, "true",XSDDatatype.XSDboolean);
		}
		if(intf_peer!=null){
			if(intf_peer.getProperty(NdlCommons.visited)!=null){
				logger.warn("Interface was used in existing VC:"+intf_peer);
				return null;
			}
			intf_peer.addProperty(NdlCommons.visited, "true",XSDDatatype.XSDboolean);
		}
		
		logger.info("----creating virtual connection-----"+device.getResource()+":"+device_peer.getResource());
		
		if((intf!=null) && (intf_peer!=null)){
			if(intf.getProperty(NdlCommons.connectedTo)==null){
				if(!device.getType().equals("vm")){
					connection_ont=toNetworkConnection(intf,intf_peer,device, device_peer);
					intf.addProperty(NdlCommons.connectedTo, intf_peer);
					intf_peer.addProperty(NdlCommons.connectedTo, intf);
				}
			}
			if(!intf.getRequiredProperty(NdlCommons.connectedTo).getResource().equals(intf_peer)){
				if(!device.getType().equals("vm")){
					connection_ont=toNetworkConnection(intf,intf_peer,device, device_peer);
					intf.addProperty(NdlCommons.connectedTo, intf_peer);
					intf_peer.addProperty(NdlCommons.connectedTo, intf);
				}
			}
			else{
				results=getNetworkConnection(intf.getURI(),intf_peer.getURI());
				if(results.hasNext()){
					String var0=(String) results.getResultVars().get(0);
					connection_ont=ontModel.getOntResource(results.nextSolution().getResource(var0));
				}
			}
		}
		
		return connection_ont;
	}
	

	//create networkconnection
	public OntResource toNetworkConnection(Resource intf0_rs,Resource intf1_rs,Device device0, Device device1){
       //Form a network connection and Find the links

	   logger.info("Generating connections:"+intf0_rs+":"+intf1_rs);

       ResultSet results=getConnectionSubGraphSwitchedTo(intf0_rs.getURI(),intf1_rs.getURI());
       //this.outputQueryResult(results);
       //results=getConnectionSubGraphSwitchedTo(intf0_rs.getURI(),intf1_rs.getURI());
       if(!results.hasNext()){
    	   results=getConnectionSubGraphSwitchedToAdaptation(intf0_rs.getURI(),intf1_rs.getURI());
    	   //this.outputQueryResult(results);
    	   //results=getConnectionSubGraphSwitchedToAdaptation(intf0_rs.getURI(),intf1_rs.getURI());
       }
       String var0=(String) results.getResultVars().get(0); //?a
       String var1=(String) results.getResultVars().get(1); //?b
       String var2=(String) results.getResultVars().get(2); //?c

       Resource in0,in1,in2;

       OntResource connection_ont=null;
       OntResource link_ont=null;
       QuerySolution solution;        
       String url;
       if(results.hasNext()){
           url=intf0_rs.getURI()+"-"+intf1_rs.getURI().split("\\#")[1];

           connection_ont=ontModel.createIndividual(url,NdlCommons.topologyNetworkConnectionClass);
           connection_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, intf0_rs);
           connection_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, intf1_rs);
           logger.info("Virtual connection:"+connection_ont+":"+intf0_rs+":"+intf1_rs+"\n");
       }
   
       HashMap <String,Resource[]> linkList = new HashMap  <String,Resource[]> ();  

       while (results.hasNext()){
           solution= results.nextSolution();
           in0= solution.getResource(var0);
           in1= solution.getResource(var1);
           in2= solution.getResource(var2);

           //logger.debug("Possible hop:"+in0.getURI()+":"+in2.getURI()+":"+device0.getDownNeighbour()+":"+device1.getUpNeighbour()+"\n");
           
           if(!in2.getURI().equals(device0.getDownNeighbour(device0.getModel())) && (device1.getDownNeighbour(device1.getModel())==null))  //last hop
        	   continue;
           if(!in0.getURI().equals(device1.getUpNeighbour(device1.getModel())) && (device0.getUpNeighbour(device0.getModel())==null)) //first hop
        	   continue;
           
           url=in0.getURI()+"-"+in2.getURI().split("\\#")[1];
           if(in1.equals(NdlCommons.connectedTo)){
               Resource [] link_src = {NdlCommons.topologyLinkConnectionClass,in0,in2};
               linkList.put(url, link_src);
           }
           if(in1.equals(NdlCommons.switchedTo)){
        	   Resource [] link_src = {topologyCrossConnectClass,in0,in2};
        	   linkList.put(url, link_src);        	      
           }
       }
       // add the facts to the graph
       for (Entry <String, Resource []> link : linkList.entrySet()){
    	   if((link.getValue()[1].getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()!=intf0_rs.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()) && (link.getValue()[2].getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()!=intf0_rs.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource())){
    		   if((link.getValue()[1].getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()!=intf1_rs.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()) && (link.getValue()[2].getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()!=intf1_rs.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource()))
    		   {
    			   link_ont=ontModel.createIndividual(link.getKey(),link.getValue()[0]);
    			   link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, link.getValue()[1]);
    			   link_ont.addProperty(NdlCommons.topologyHasInterfaceProperty, link.getValue()[2]);
    			   connection_ont.addProperty(NdlCommons.collectionItemProperty,link_ont);
    			   logger.debug("Link hop:"+link.getKey()+"|"+link.getValue()[0]+"|"+link.getValue()[1]+"|"+link.getValue()[2]);
    		   }
    	   }
       }

       return connection_ont;
   }
		
	public boolean peerLayer(Device d1,Device d2){
		boolean peer=false;
		LinkedList <SwitchMatrix> sw1=d1.getSwitchingMatrix();
		LinkedList <SwitchMatrix> sw2=d2.getSwitchingMatrix();
		Iterator <SwitchMatrix> it1=sw1.iterator();
		Iterator <SwitchMatrix> it2=sw2.iterator();
		String l1,l2;
		
		while(it1.hasNext()){
			l1=it1.next().getAtLayer();
			while(it2.hasNext()){
				l2=it2.next().getAtLayer();
				//System.out.println("Layer:"+l1+":"+l2);
				if(l1.equals(l2)) {
					peer=true;
					break; 
				}
			}
			if(peer) break;
		}
		
		return peer;
	}	
	@SuppressWarnings("unchecked")
	public NetworkConnection releaseConnection(NetworkConnection connection,String requestURI){
		if(connection==null)
			return null;
		//usedlabelset not empty?
		Iterator <Device> it;
		Device device=null;
		
		releaseNetworkConnection=new NetworkConnection();
		LinkedList <Device> releaseDeviceList=(LinkedList<Device>)releaseNetworkConnection.getConnection();
		
		logger.info("---1. Finding Releasing CRS/Device/VC-----:"+connection.getName());

		BitSet releasedLayerBitSet=null;
		String connectionLayer = connection.getAtLayer();
		if(connectionLayer.equals(Layer.EthernetNetworkElement.toString()))
			releasedLayerBitSet=new BitSet(InterDomainHandler.max_vlan_tag);
		if(connectionLayer.equals(Layer.LambdaNetworkElement.toString()))
			releasedLayerBitSet=new BitSet(11);
		
		LinkedList <Device>	deviceList=(LinkedList<Device>)connection.getConnection();
		Collections.reverse(deviceList);
		it=deviceList.iterator();
		while(it.hasNext()){
			device=(Device) it.next();
			if(device.getType()==null){
				logger.warn("Device has no type:"+device.getName());
				continue;
			}

			if((device.getResource()==null) || (device.getResource().getProperty(visited)!=null)){//underneath the occupied connection
				logger.debug("Device visited in release:"+device.getName());
				continue;
			}
			
			if(device.getType().equalsIgnoreCase("vm")){
				logger.debug("Device is edge:"+device.getName());		
			}else{	
				releaseCRS(device,false,requestURI);
			}
			logger.info("releaseConnection-- device="+device.getName()+";device.atLayer="+device.getAtLayer()+":connection.atLayer="+connection.getAtLayer());
			//in the sorted device list, the first two are always the two end points of the device
			if(device.getAtLayer().equals(connection.getAtLayer())){
				if(!device.getType().equalsIgnoreCase("vm")){
					logger.info("Releasing Device Added:"+device.getURI());
					releaseDeviceList.add(device);
				}
				LinkedList<SwitchingAction> actions = device.getActionList();
				if(actions==null)
					continue;
	            int anum = 0;

	            for (int i = 0; i < actions.size(); i++) {
	                SwitchingAction a = actions.get(i);
	                logger.debug("Releasing Action=" + a.getDefaultAction()+";"+a.getAtLayer());
	                if (a.getDefaultAction() == LayerConstant.Action.Temporary.toString()) {
	                    continue;
	                }
	                if(a.getAtLayer().equalsIgnoreCase(connectionLayer)){
	                	releasedLayerBitSet.set((int) a.getLabel_ID());
	                	logger.debug("release vlan="+(int) a.getLabel_ID());
	                }
	            }
			}
		}	
		
		BitSet usedLayerBitSet = this.getUsedLabelSetPerLayer(connectionLayer);
		if(usedLayerBitSet!=null)
			usedLayerBitSet.andNot(releasedLayerBitSet);
		else
			logger.error("ERROR:No local used bitset");
		
		removeInConnectionProperty("ndl:visited",visited);
		return releaseNetworkConnection;		
	}

	// release CRS in the switchingAction
	//releaseFlag: true: release in model; false: 
	public boolean releaseCRS(Device device,boolean releaseFlag,String requestURI){
		boolean valid=false;
		SwitchingAction action=null;
		LinkedList <SwitchingAction> actionList=device.getActionList();		
		if(actionList==null) {
			logger.error("No switching action:"+device.getResource());
			return valid;
		}
		Iterator <SwitchingAction> it_action=actionList.iterator();		
		while(it_action.hasNext()){
			action=it_action.next();
			action.setDefaultAction(Action.Delete.toString());
			String current_layer=action.getAtLayer();
			
			valid=switchingActionInModel(action, current_layer, releaseFlag,requestURI);	
			logger.info("end of releasing crs in model:"+device.getResource()+":"+releaseFlag);
		}	
		return valid;
	}
	
	//get the "Delete" action
	//releaseFlag: true:
	//			   false: (1) Find the virtual connections ending with the interfaces of the action	
	public boolean switchingActionInModel(SwitchingAction action, String current_layer, boolean releaseFlag,String requestURI){	 
		LinkedList <Interface> interfaceList=action.getClientInterface();
		Interface intf=null;
		Interface intf_next=null;
		Resource rs, rs_next;
		OntResource rs_ont,rs_next_ont;
		boolean done=false;
		
		if(action.getModel()==null)
			action.setModel(ontModel);
		
		Iterator <Interface> it=interfaceList.iterator();
		int size=interfaceList.size();
		int i=0;

		while(it.hasNext()){
			intf=it.next();
			i++;
			if(intf.getModel()==null)
				intf.setModel(ontModel);
			rs=intf.getResource();
			rs_ont=ontModel.getOntResource(rs);
			for(int j=i;j<size;j++){
				//logger.debug(i+":"+j+":"+size);
				intf_next=interfaceList.get(j);
				if(intf_next.getModel()==null)
					intf_next.setModel(ontModel);
				rs_next=intf_next.getResource();
				rs_next_ont=ontModel.getOntResource(rs_next);
				if(releaseFlag) {
					removeReservation(rs_ont,rs_next_ont,requestURI);
					releaseCrossConnect(rs_ont.getURI(),rs_next_ont.getURI());
					returnLabel(action,intf,intf_next);
				}
				else{
					done=removeVirtualConnection(rs_ont,rs_next_ont,requestURI);
				}
			}
		}
		return done;
	}
	
	public boolean removeVirtualConnection(OntResource rs_ont,OntResource rs_next_ont,String requestURI){
		
		boolean done=false;		
		LinkedList <Resource> connection_rs_list = new LinkedList <Resource> ();
		Resource connection_rs=null;
		String var0=null;
		
		logger.info("Virtual Connection removal:"+rs_ont+":"+rs_next_ont);
		//find the virtual connections using this or its adapted interface.
		ResultSet results=interfaceOfNetworkConnection(rs_ont.getURI());
		if(results!=null){
			var0=(String) results.getResultVars().get(0);
			while(results.hasNext()){
				connection_rs = results.nextSolution().getResource(var0);
				connection_rs_list.add(connection_rs);
			}
		}
		//if(!results.hasNext())	
		results=interfaceOfNetworkConnection(rs_next_ont.getURI());
		if(results!=null){
			var0=(String) results.getResultVars().get(0);
			while(results.hasNext()){
				connection_rs = results.nextSolution().getResource(var0);
				connection_rs_list.add(connection_rs);
			}
		}
			//if(!results.hasNext())
		if(connection_rs_list.size()<1)
			return false;
						
		//while(results.hasNext()){
		while(!connection_rs_list.isEmpty()){
			//connection_rs = results.nextSolution().getResource(var0);
			connection_rs=connection_rs_list.remove();
			if(connection_rs.getProperty(visited)!=null)
				if(connection_rs.getProperty(visited).getBoolean()==true)
					continue;
			markDeviceList = new LinkedList <Resource>();
			releasedVCList = new LinkedList <Resource>();
			underneathVC(connection_rs);
			clearCarriedReservation(requestURI);
			if(carryOtherReservation(connection_rs,requestURI)){  //leave the connection and return
				releaseNetworkConnection(connection_rs,true,requestURI);
				done =true;
				//break;
			}
			else {
				releaseNetworkConnection(connection_rs,false,requestURI); //try to tear down the connection
				//to avoid further check
			}
			for(Resource d_rs:markDeviceList){
				d_rs.addProperty(visited, "true",XSDDatatype.XSDboolean);
				logger.debug("Visted:"+d_rs.getURI());
			}
		}
		return done;
	}
	
	LinkedList <Resource> markDeviceList = null;
	LinkedList <Resource> releasedVCList = null;	
	public void clearCarriedReservation(String requestURI){
		if(releasedVCList==null)
			return;
		for(Resource rs:releasedVCList){
			OntResource rs_ont = ontModel.getOntResource(rs);
			rs_ont.removeProperty(carryReservation, ontModel.getResource(requestURI));
		}
	}
	public void underneathVC(Resource nc_rs){
		if(nc_rs==null)
			return;
		releasedVCList.add(nc_rs);
		Statement st=null;
		StmtIterator stit=nc_rs.listProperties(NdlCommons.collectionItemProperty);
		Resource rs=null;
		OntResource rs_ont=null;
		if(stit!=null){
			while(stit.hasNext()){	    
				st=stit.nextStatement();
				rs=st.getResource();
				rs_ont=ontModel.getOntResource(rs);
				if(rs_ont.hasRDFType(NdlCommons.topologyNetworkConnectionClass)){  //subconnection or link segment
					underneathVC(rs);
				}
			}
		}	
	}
	// release a given network connection -> Device list w/ crs delete switching actions
	@SuppressWarnings("unchecked")
	public boolean releaseNetworkConnection(Resource nc_rs,boolean markFlag,String requestURI){
		boolean release=false;
		
		LinkedList <Device> releaseDeviceList=(LinkedList<Device>)releaseNetworkConnection.getConnection();

		logger.info("NetworkConnection release recursively:"+nc_rs);
		markDeviceList.add(nc_rs);
		
		Statement st=null;
		StmtIterator stit=nc_rs.listProperties(NdlCommons.collectionItemProperty);
		Resource rs=null;
		OntResource rs_ont=null;
		if(stit!=null){
			while(stit.hasNext()){	    
				st=stit.nextStatement();
				rs=st.getResource();
				rs_ont=ontModel.getOntResource(rs);
				logger.info("releaseNetworkConnection item:"+rs_ont);
				if(rs_ont.hasRDFType(NdlCommons.topologyNetworkConnectionClass)){  //subconnection or link segment
					releaseNetworkConnection(rs,carryOtherReservation(rs,requestURI),requestURI);
				}
				if(rs_ont.hasRDFType(NdlCommons.topologyCrossConnectClass)){	// crs (switch) segment
					Device device = getReleaseDevice(rs);
					if(device!=null){
						markDeviceList.add(device.getResource());
						if(!markFlag){
							logger.info("Release CrossConnect:"+rs_ont+":"+device.getName());
							releaseDeviceList.add(device);
						}
					}
				}
			}
		}

		//find the interfaces of this connection, to be deleted in the model
		getConnectionInterfaces(nc_rs);
	
		return release;
	}	
	
	//find or define the device to be released 
	@SuppressWarnings("unchecked")
	public Device getReleaseDevice(Resource crs_rs){
		Resource device_rs=null,intf_rs=null,intf_rs_next=null;
		ResultSet results=getCRSDevice(crs_rs.getURI());
        //outputQueryResult(results);
        //results=getCRSDevice(crs_rs.getURI());
        
		if(!results.hasNext()){
			logger.error("Orphon CRS:"+crs_rs.getURI());
			return null;
		}
		//assume 2 interfaces per CRS
		QuerySolution solution=null;		
		Interface intf=null,intf_next=null; 		
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);	
		if(results.hasNext()){	
			solution=results.nextSolution();
			device_rs = solution.getResource(var0);
			intf_rs = solution.getResource(var1);		
			if(results.hasNext())	
				intf_rs_next = results.nextSolution().getResource(var1);
		}	
		
		Device device=getDevice(device_rs,(LinkedList<Device>)releaseNetworkConnection.getConnection());
		if(device!=null){ //already in the release List
			device=null;
		}else{
			device=getDevice(device_rs,(LinkedList<Device>)deviceConnection.getConnection());
			if(device==null){ //if in the connection list
				device=new Device(ontModel,device_rs);				
				SwitchingAction action=new SwitchingAction(ontModel);	
				intf=new Interface(ontModel,ontModel.getOntResource(intf_rs),false);
				action.addClientInterface(intf);
				intf_next=new Interface(ontModel,ontModel.getOntResource(intf_rs_next),false);
				action.addClientInterface(intf_next);
				action.setAtLayer(intf.getAtLayer());
				action.setDefaultAction(Action.Delete.toString());
				String layer=null;
				if(intf.getResource().getProperty(NdlCommons.atLayer)!=null)
					layer=intf.getResource().getProperty(NdlCommons.atLayer).getResource().getLocalName();
				else if(intf_next.getResource().getProperty(NdlCommons.atLayer)!=null)
					layer=intf_next.getResource().getProperty(NdlCommons.atLayer).getResource().getLocalName();
				if(layer!=null){
					String labelP=NdlCommons.ORCA_NS+Layer.valueOf(layer).getPrefix()+".owl#"+Layer.valueOf(layer).getLabelP();
					ObjectProperty label_p=ontModel.getObjectProperty(labelP);
					Resource label_rs=null;
					logger.info("getReleaseDevice intf:"+intf.getResource().getURI()+"-"+intf_next.getResource()+":"+label_p);
					if(intf.getResource().getProperty(label_p)!=null){
						label_rs=intf.getResource().getProperty(label_p).getResource();
						logger.info("getReleaseDevice label_rs 1:"+label_rs.getURI());
					}
					if(intf_next.getResource().getProperty(label_p)!=null){
						label_rs=intf_next.getResource().getProperty(label_p).getResource();
						logger.info("getReleaseDevice label_rs 2:"+label_rs.getURI());
					}
					if(label_rs!=null){
						Label label=new Label();
						label.setResource(ontModel.getOntResource(label_rs));
						action.setLabel(label);
					}
				}
				device.addSwitchingAction(action);
				if(device.getDirection().equals(Direction.UNIDirectional.toString())){
					device.processUNIInterface(action.getAtLayer());
					action.setDefaultAction(Action.Temporary.toString());
				}
			}
		}
		return device;
	}
	
	public void getConnectionInterfaces(Resource vc){
		OntResource [] nc_intf = new OntResource[2]; 
		StmtIterator stit_nc = vc.listProperties(NdlCommons.topologyHasInterfaceProperty);
		OntResource rs_nc;
		int i=0;
		while(stit_nc.hasNext()){			
			rs_nc=ontModel.getOntResource(stit_nc.nextStatement().getResource());
			nc_intf[i]=rs_nc;
			i++;
		}
		rs_nc=ontModel.getOntResource(vc);
		nc_intf_list.put(rs_nc, nc_intf);
		logger.debug("Connection interfaces to be removed put in the waiting list:"+nc_intf[0]+":"+nc_intf[1]);	
	}
	
	//Real release in the RDF model
	@SuppressWarnings("unchecked")
	public NetworkConnection releaseInModel(NetworkConnection connection,String requestURI){
		LinkedList <Device> deviceList=null;
		Iterator <Device> it;
		Device device=null;
		logger.info("----2. Releasing CrossConnect in the ontology model: (Real Release)-----"+connection.getURI());
		if(connection!=null){
			deviceList=(LinkedList<Device>)releaseNetworkConnection.getConnection();
			it=deviceList.iterator();
			while(it.hasNext()){
				device=(Device) it.next();
				logger.info("Releasing device in model: "+device.getURI());
				releaseCRS(device,true,requestURI);			
			}
		 }

		for (Entry <OntResource,OntResource[]> intf_pair : nc_intf_list.entrySet()){
			if(!carryOtherReservation(intf_pair.getKey(),requestURI)){
				intf_pair.getKey().remove();
				intf_pair.getValue()[0].removeProperty(NdlCommons.connectedTo, intf_pair.getValue()[1]);
				intf_pair.getValue()[1].removeProperty(NdlCommons.connectedTo, intf_pair.getValue()[0]);
			
				logger.info("Releasing the virtual connection:"+intf_pair.getValue()[0]+":"+intf_pair.getValue()[1]+"\n");
		
			}
		}
		
		removeInConnectionProperty("ndl:portOccupied",portOccupied);
		
		logger.info("Release is Done!\n");

		 return connection;
	}	
	
	//remove the carried reservation from the vc in the model
	public void removeReservation(OntResource rs_ont,OntResource rs_next_ont,String requestURI){
		rs_ont.removeProperty(switchedTo, rs_next_ont);
		rs_next_ont.removeProperty(switchedTo, rs_ont);
		
		LinkedList <Resource> connection_rs_list = new LinkedList <Resource> ();
		Resource connection_rs=null;
		String var0=null;
		
		logger.info("Virtual Connection reservation removal:"+rs_ont+":"+rs_next_ont);
		//find the virtual connections using this or its adapted interface.
		ResultSet results=interfaceOfNetworkConnection(rs_ont.getURI());
		if(results!=null){
			var0=(String) results.getResultVars().get(0);
			while(results.hasNext()){
				connection_rs = results.nextSolution().getResource(var0);
				connection_rs_list.add(connection_rs);
			}
		}
		//if(!results.hasNext())	
		results=interfaceOfNetworkConnection(rs_next_ont.getURI());
		if(results!=null){
			var0=(String) results.getResultVars().get(0);
			while(results.hasNext()){
				connection_rs = results.nextSolution().getResource(var0);
				connection_rs_list.add(connection_rs);
			}
		}
		if(connection_rs_list.size()<1)
			return;

		logger.info("Reservation in Connection removal:"+requestURI+":"+rs_ont+":"+rs_next_ont);			

		OntResource connection_rs_ont=null;
		LinkedList <OntResource> connectionReservationList = new LinkedList <OntResource>();
		LinkedList <OntResource> clientConnectionReservationList=null;
		while(!connection_rs_list.isEmpty()){	
			connection_rs = connection_rs_list.remove();
			if(connection_rs!=null){
				clientConnectionReservationList=getClientNetworkConnections(connection_rs);
				connectionReservationList.addAll(clientConnectionReservationList);
			}
		}
		
		for(OntResource or:connectionReservationList){
			or.removeProperty(carryReservation, ontModel.getResource(requestURI));
			logger.info("Carried Reservation removed:"+ontModel.getResource(requestURI)+":"+connection_rs+"\n");
		}
	}
	//get all client connections in a virtual NetworkConnection, including itself, recursively
	public LinkedList <OntResource> getClientNetworkConnections(Resource nc_rs){
		if(nc_rs==null)
			return null;
		LinkedList <OntResource> connectionReservationList = new LinkedList <OntResource>();
		connectionReservationList.add(ontModel.getOntResource(nc_rs));
		LinkedList <OntResource> clientConnectionReservationList=null;
		Statement st=null;
		StmtIterator stit=nc_rs.listProperties(NdlCommons.collectionItemProperty);
		Resource rs=null;
		OntResource rs_ont=null;
		if(stit!=null){
			while(stit.hasNext()){	    
				st=stit.nextStatement();
				rs=st.getResource();
				rs_ont=ontModel.getOntResource(rs);
				if(rs_ont.hasRDFType(NdlCommons.topologyNetworkConnectionClass)){  //subconnection or link segment
					logger.info("clientNetworkConnection item:"+rs_ont);
					connectionReservationList.add(rs_ont);
					clientConnectionReservationList=getClientNetworkConnections(rs);
					if(clientConnectionReservationList.size()<1)
						connectionReservationList.addAll(clientConnectionReservationList);
				}
			}
		}
		return connectionReservationList;
	}
	
	
	
	public void releaseCrossConnect(String rs1,String rs2){

        String s ="SELECT ?r ";
        String f="";
        String w=
        	"WHERE {" +
        	"?r ndl:hasInterface " + "<"+rs1+">."+
        	"?r ndl:hasInterface " + "<"+rs2+">."+
        	" ?r rdf:type "+ "ndl:CrossConnect" +
        	"      }"; 
        String queryPhrase=createQueryString(s,f,w);
        
		ResultSet results=rdfQuery(ontModel,queryPhrase);
		
		String var0=null;
		if(results.hasNext()) var0=(String) results.getResultVars().get(0);
		
		Resource crs_rs=null;
		
		if(results.hasNext()){	
			crs_rs = results.nextSolution().getResource(var0);
			logger.info("Tear down CRS:"+crs_rs);
			crs_rs.removeProperties();
		}
	}
	
	public Resource returnLabel(SwitchingAction action, Interface intf1, Interface intf2){

		if(action.getLabel()==null) return null;
		
		Resource label_rs = action.getLabel().getResource(action.getModel());
		Resource label1_rs = null,label2_rs = null;
		Resource rs1_parent = null,rs2_parent = null;
		String labelP=null;
		ObjectProperty label_p=null;
		
		String aSet=null;
		ObjectProperty aSet_p=null;		
		OntResource rs1_parent_availableSet =null,rs2_parent_availableSet =null;
		
		String uSet=null;
		ObjectProperty uSet_p=null;		
		OntResource rs1_parent_usedSet = null,rs2_parent_usedSet = null;
		
		String layer=action.getAtLayer();
		String prefix=Layer.valueOf(layer).getPrefix().toString();
		
		if(intf1.getResource().getProperty(adaptationPropertyOf)!=null){
			if(intf1.getResource().getProperty(NdlCommons.atLayer)!=null)
				layer=intf1.getResource().getProperty(NdlCommons.atLayer).getResource().getLocalName();
		
			labelP = NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getLabelP();
			label_p=ontModel.getObjectProperty(labelP);
			if(intf1.getResource().getProperty(label_p)!=null){
				label1_rs = intf1.getResource().getProperty(label_p).getResource();
				intf1.getResource().removeProperty(label_p, label1_rs);
			}
			
			rs1_parent=intf1.getResource().getProperty(adaptationPropertyOf).getResource();
			if(label1_rs!=null){ //Only consider aSet and uSet for interface with assigned label 
				logger.info("This labeled interface:"+layer+":"+aSet_p+":"+rs1_parent+":"+rs1_parent_availableSet+"--"+label1_rs.getURI()+"\n");
				try{
					layer=findLayer(ontModel,rs1_parent);	
					prefix=Layer.valueOf(layer).getPrefix().toString();
				
					aSet=NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getASet();
					aSet_p=ontModel.getObjectProperty(aSet);
			
					if(rs1_parent.getProperty(aSet_p)!=null)
						rs1_parent_availableSet= ontModel.getOntResource(rs1_parent.getProperty(aSet_p).getResource());
				
					uSet=NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getUSet();
					uSet_p=ontModel.createObjectProperty(uSet);
				}catch(Exception e){
					logger.error("Exception:"+e.getLocalizedMessage()+":"
							+rs1_parent.getURI()+":"
							+layer+":"
							+label1_rs.getURI());
				}
			}
		}
		if(rs1_parent_availableSet!=null){
			logger.info("Returning used label:"+uSet_p+":"+rs1_parent+":"+rs1_parent.getProperty(uSet_p));
			if(rs1_parent.getProperty(uSet_p)!=null){
				rs1_parent_usedSet = ontModel.getOntResource(rs1_parent.getProperty(uSet_p).getResource());
				if(rs1_parent_availableSet!=null && rs1_parent_usedSet!=null && rs1_parent_usedSet.hasProperty(NdlCommons.collectionElementProperty, label1_rs) && (label1_rs!=null)){
					rs1_parent_usedSet.removeProperty(NdlCommons.collectionElementProperty, label1_rs);
					rs1_parent_availableSet.addProperty(NdlCommons.collectionElementProperty, label1_rs);
					logger.info("Returned used label:"+label1_rs+":"+rs1_parent_availableSet.hasProperty(NdlCommons.collectionElementProperty,label1_rs)+":" + 
						rs1_parent_usedSet+":"+rs1_parent_usedSet.hasProperty(NdlCommons.collectionElementProperty,label1_rs)+"\n");	
				}else
					logger.warn("No Returned used label:label1_rs="+label1_rs);
			}
		}

		if(intf2.getResource().getProperty(adaptationPropertyOf)!=null){
			if(intf2.getResource().getProperty(NdlCommons.atLayer)!=null)
				layer=intf2.getResource().getProperty(NdlCommons.atLayer).getResource().getLocalName();
		
			labelP = NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getLabelP();
			label_p=ontModel.getObjectProperty(labelP);
			if(intf2.getResource().getProperty(label_p)!=null){
				label2_rs = intf2.getResource().getProperty(label_p).getResource();	
				intf2.getResource().removeProperty(label_p, label2_rs);
			}
			rs2_parent=intf2.getResource().getProperty(adaptationPropertyOf).getResource();
			if(label2_rs!=null){
				logger.info("This labeled interface:"+layer+":"+aSet_p+":"+rs2_parent+":"+rs2_parent_availableSet+"--"+label2_rs.getURI()+"\n");
				try{
					layer=findLayer(ontModel,rs2_parent);
					prefix=Layer.valueOf(layer).getPrefix().toString();
						
					aSet=NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getASet();
					aSet_p=ontModel.getObjectProperty(aSet);
					if(rs2_parent.getProperty(aSet_p)!=null)
						rs2_parent_availableSet = ontModel.getOntResource(rs2_parent.getProperty(aSet_p).getResource());
						
					uSet=NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getUSet();
					uSet_p=ontModel.createObjectProperty(uSet);
				}catch(Exception e){
					logger.error("Exception:"+e.getLocalizedMessage()+":"
							+rs2_parent.getURI()+":"
							+layer+":"
							+label2_rs.getURI());
				}
			}
		}
		
		if( (rs1_parent_availableSet==null) && (rs2_parent_availableSet==null)) return null;
		if(rs2_parent.getProperty(uSet_p)!=null){	
			logger.info("Returning used label:"+uSet_p+":"+rs2_parent+":"+rs2_parent.getProperty(uSet_p));
			rs2_parent_usedSet = ontModel.getOntResource(rs2_parent.getProperty(uSet_p).getResource());
			if(rs2_parent_usedSet!=null && rs2_parent_usedSet!=rs1_parent_usedSet){		
				if(rs2_parent_availableSet!=null && rs2_parent_usedSet.hasProperty(NdlCommons.collectionElementProperty, label2_rs) && (label2_rs!=null)){
					rs2_parent_usedSet.removeProperty(NdlCommons.collectionElementProperty, label2_rs);
					rs2_parent_availableSet.addProperty(NdlCommons.collectionElementProperty, label2_rs);
					logger.info("Returned used label:"+label2_rs+":"+rs2_parent_availableSet.hasProperty(NdlCommons.collectionElementProperty,label2_rs) + ":" + 
					rs2_parent_usedSet+":"+rs2_parent_usedSet.hasProperty(NdlCommons.collectionElementProperty,label2_rs)+"\n");
				}else
					logger.warn("No Returned used label:label2_rs="+label2_rs);
			}
		}
		return label_rs;
	}	
	
	//return true: carry a reservation other than this
	public synchronized boolean carryOtherReservation(Resource nc_rs,String requestURI){
		boolean carry=false;
		Statement st=null;
		Resource rs=null;
		
		StmtIterator stit=nc_rs.listProperties(carryReservation);
		
		if(stit!=null){
			while(stit.hasNext()){
				st=stit.nextStatement();
				rs=st.getResource();
				logger.info("Carried Reservation:"+nc_rs.getURI()+":"+requestURI+":carried reservation:"+rs.getLocalName());
				if(!rs.getURI().equals(requestURI)){
					carry=true;
					break;
				}
			}
		}
		logger.info("Carried other reservation:"+carry);
		return carry;
	}	
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("nc_intf_list: \n");
		for(Entry<OntResource, OntResource[]> e: nc_intf_list.entrySet()) {
			sb.append(e.getKey() + ": " + e.getValue()+ "\n");
		}
		return sb.toString();
	}
	
}
