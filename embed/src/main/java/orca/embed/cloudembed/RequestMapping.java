package orca.embed.cloudembed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;

import orca.embed.cloudembed.controller.InterDomainHandler;
import orca.embed.policyhelpers.RequestMappingException;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.OntProcessor;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.Interface;
import orca.ndl.elements.Label;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchMatrix;
import orca.ndl.elements.SwitchingAction;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/*
 * ontModel:
 * requestModel:
 * phymodel:
 * requestMapModel: mapped request connections in OntModel
 * 
 */
public class RequestMapping extends OntProcessor implements IConnectionManager{
	protected OntModel requestModel;
	protected NetworkConnection deviceConnection, releaseNetworkConnection;
	
	protected Stack <Label> labelStack = null;
	protected HashMap <String,BitSet> lableSetPerLayer;
	protected HashMap <String,BitSet> usedLabelSetPerLayer;
	
	protected Properties pdomain_properties;
	
	//public static String outFile_int=PathGuesser.getOrcaControllerHome() + "logs" + System.getProperty("file.separator") + "perf_int.dat";
	//public static File file_int = new File(outFile_int);
    //public static String outFile=PathGuesser.getOrcaControllerHome() + "logs" + System.getProperty("file.separator") + "perf.dat";
    //public static File file = new File(outFile);
    //Writer output = null;
    
	public RequestMapping(OntModel rModel, OntModel substrateModel,boolean inter) throws IOException{
		requestModel = rModel;
		ontModel = substrateModel;
		/*try {
			if(inter)
				output = new BufferedWriter(new FileWriter(file_int,true));
			else
				output = new BufferedWriter(new FileWriter(file,true));
			output.write(ontModel.size()+":"+ontModel.listOntologies().toSet().size()+":"+ontModel.listIndividuals().toSet().size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public RequestMapping(OntModel substrateModel) {
		ontModel = substrateModel;
		/*try {
			output = new BufferedWriter(new FileWriter(file));
			output.write(ontModel.size()+":");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
    
	public SystemNativeError createConnection(NetworkConnection requestConnection,boolean interDomainRequest,boolean needExchange,String nc_of_version) {
		SystemNativeError error =null;
		
		Resource rs1=requestConnection.getNe1().getResource();
		Resource rs2=requestConnection.getNe2().getResource();
		
		if((rs1==null) || (rs2==null)){
			error = new SystemNativeError();
			error.setErrno(4);
			error.setMessage("No Edge Exist:"+rs1+":"+rs2);
			return error;
		}
		
		Resource domain_rs1=null,domain_rs2=null;
		long bw=requestConnection.getBandwidth();
		String rType1_str = requestConnection.getNe1().getResourceType().getResourceType();
		String rType2_str = requestConnection.getNe2().getResourceType().getResourceType();
		
		if(interDomainRequest){
			domain_rs1=toDomain(rs1,rType1_str);
			domain_rs2=toDomain(rs2,rType2_str);
			if((domain_rs1==null) || (domain_rs2==null)){
				error = new SystemNativeError();
				error.setErrno(4);
				error.setMessage("No Edge Domain Exist:"+rs1.getURI()+":"+rs2.getURI());
				return error;
			}
			rs1=domain_rs1;
			rs2=domain_rs2;
		}
		logger.info("Starting path computation....");	
//long start = System.nanoTime();
		ArrayList<ArrayList<OntResource>> solution = findShortestPath(ontModel,rs1,rs2,bw,rType1_str,rType2_str,nc_of_version);
//long time =System.nanoTime()-start;
/*
try {
	output.write((solution.size()/2+1)+":"+time+"\n");
	output.close();
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}*/
//System.out.println("Path Finding Time:"+time);		
		int connected = solution==null ? 0:solution.size(); 
		if(connected==0){
			error = new SystemNativeError();
			error.setErrno(2);
			error.setMessage("Path does NOT exist for:"+requestConnection.getURI());
		}else{
			if(!interDomainRequest){
				if(needExchange && checkExchange(solution)){
					error = new SystemNativeError();
					error.setErrno(1000);
					error.setMessage("Path needs label exchange:"+requestConnection.getURI());
				}else{
					deviceConnection = toConnection(solution,requestConnection,interDomainRequest);		
				}
			}else{
				deviceConnection = toConnection(solution,requestConnection,interDomainRequest);	
			}
			if(error==null){
				if(deviceConnection!=null){
					removeInConnectionProperty("ndl:inConnection",inConnection);
				}
				else{
					error = new SystemNativeError();
					error.setErrno(3);
					error.setMessage("Path is NOT switcheabally connected for:"+requestConnection.getURI());
				}
			}
		}
		if(error!=null)
			System.out.println("Error:"+error.toString());
		return error;
	}
	
	public boolean checkExchange(ArrayList<ArrayList<OntResource>> path){
		boolean cc =false;
		ArrayList<OntResource> hop = null,next_hop = null;
    	OntResource intf_ont,next_intf_ont;
		for(int i=0;i<path.size()-1;i=i+2){
			hop=path.get(i);
			intf_ont=hop.get(1);
			
			next_hop=path.get(i+1);
			next_intf_ont=next_hop.get(1);
			
			if(! intf_ont.hasProperty(NdlCommons.linkTo, next_intf_ont)){
				cc=true;
				break;
			}
		}
		return cc;
	}
	
	public NetworkConnection releaseConnection(NetworkConnection conn,String requestURI){
		return null;
	}
	
	public NetworkConnection releaseInModel(NetworkConnection conn,String requestURI){
		return null;
	}
	
    // Generate the device and the switch actions with the interface connectivity between two connected devices:
    //(1) the identify the two devices, to be put in device list, make sure no repeat; 
    //(2) (non-adapted interfaces involved to generate switching action
    
    @SuppressWarnings("unchecked")
	private synchronized NetworkConnection toConnection(ArrayList<ArrayList<OntResource>> path,NetworkConnection requestConnection,boolean interDomainRequest){
    	NetworkConnection deviceConnection = new NetworkConnection();    
    	
    	LinkedList<Device> deviceList = (LinkedList<Device>)deviceConnection.getConnection();
    	
    	ArrayList<OntResource> hop = null,next_hop = null;
    	LinkedList <OntResource> deviceOntList = null;
    	Device device=null, next_device=null;
    	Interface intf=null,next_intf=null;
    	OntResource device_ont,intf_ont,next_device_ont,next_intf_ont;
    	ComputeElement ne1=null,ne2=null;
    	if(requestConnection.getNe1() instanceof ComputeElement)
    		ne1=(ComputeElement) requestConnection.getNe1(); 
    	if(requestConnection.getNe1() instanceof ComputeElement)
    		ne2=(ComputeElement) requestConnection.getNe2();
    	if(path!=null) {
			if (!path.isEmpty()) {				
				HashMap <OntResource,OntResource> intf_List;	
				deviceOntList = new LinkedList <OntResource> ();
				DomainResourceType type = null, edgeType = null;

				for(int i=0;i<path.size()-1;i=i+2){
					hop=path.get(i);
					device_ont=hop.get(0);
					intf_ont=hop.get(1);
					
					next_hop=path.get(i+1);
					next_device_ont=next_hop.get(0);
					next_intf_ont=next_hop.get(1);
					
					logger.info("Link "+ i+ ": Creat Link Connection:"  + device_ont+":"+intf_ont+"\n");
					logger.info(" : ----- :"  + next_device_ont+":"+next_intf_ont+"\n");

					if(i==0) {//first device
						deviceOntList.add(device_ont);
						if(interDomainRequest){
							device = new DomainElement(ontModel, device_ont);
							DomainElement d = (DomainElement) device;
							d.setCe(ne1);
							if(ne1!=null){
								d.getCe().setNodeGroupName(ne1.getNodeGroupName());
								d.getCe().setGroup(ne1.getGroup());
								d.setModify(ne1.isModify());
							}
						}
						else
							device = new Device(ontModel, device_ont);	
					    deviceList.add(device);
					}
					else { //head of next hop = tail of last hop, already in, 
						device = deviceList.getLast();
					}
					device.setDownLocal(intf_ont);
					device.setDownNeighbour(next_intf_ont);
					intf = new Interface(this.ontModel, intf_ont, true);
					intf_ont.addProperty(NdlCommons.inConnection, "true", XSDDatatype.XSDboolean);
		            boolean validSwitching = processAction(intf, device,requestConnection.getBandwidth());
		            if(!validSwitching) {
		            	deviceConnection = null;
		            	break;
		            }
					
					if(!deviceOntList.contains(next_device_ont)){
						deviceOntList.add(device_ont);
						if(interDomainRequest){
							next_device = new DomainElement(ontModel, next_device_ont);
							DomainElement d = (DomainElement) next_device;
							if(i==path.size()-2){	//destination edge domain	
								d.setCe(ne2);
								if(ne2!=null){	
									d.getCe().setNodeGroupName(ne2.getNodeGroupName());
									d.getCe().setGroup(ne2.getGroup());
									d.setModify(ne2.isModify());
								}
							}
						}
						else{
							next_device = new Device(ontModel, next_device_ont);	
						}
						deviceList.add(next_device);
					}
					else next_device = getDevice(next_device_ont, deviceList);
					
					next_device.setUpLocal(next_intf_ont);
					next_device.setUpNeighbour(intf_ont);
					next_intf = new Interface(this.ontModel, next_intf_ont, true);
					next_intf_ont.addProperty(NdlCommons.inConnection, "true", XSDDatatype.XSDboolean);
		            validSwitching = processAction(next_intf, next_device,requestConnection.getBandwidth());
		            if(!validSwitching) {
		            	deviceConnection = null;
		            	break;
		            }
		            
		            //add requested resource to the edge domains and intermedium domain according to the label set of the border interface.		            
					if(i==0){ //the source domain
						type=requestConnection.getNe1().getResourceType();
						type.setDomainURL(device.getURI());
						device.setResourceType(type);
						device.setName(requestConnection.getNe1().getName());
						deviceConnection.setNe1(device);
					}
					
					if(i==path.size()-2){ //the last (destination) domain 
						type=requestConnection.getNe2().getResourceType();
						type.setDomainURL(next_device.getURI());
						next_device.setResourceType(type);
						next_device.setName(requestConnection.getNe2().getName());
						deviceConnection.setNe2(next_device);
					}
					else{//other domains
						if(interDomainRequest){
							type=getResourceType(requestConnection.getResourceType().getResourceType());
							type.setDomainURL(next_device.getURI());
							int count = 0;
							if(next_device.getResourceType()!=null)
								count = next_device.getResourceType().getCount();
							type.setCount(count+1);
							type.setRank(next_device.getRank());
							next_device.setResourceType(type);
						}
					}
					//Directly add a connectedTO between the two interfaces, in case they're connected via pre-switched path.
		            boolean cc=false;
		            Resource j_res=null;
		            for (StmtIterator j=intf_ont.listProperties(NdlCommons.connectedTo);j.hasNext();){
		            	j_res=j.next().getResource();
		            	if(next_intf_ont.getURI() == j_res.getURI()){
		     				cc=true;
		     				break;
		     			}
		            }
		            if(!cc){
		            	intf_ont.addProperty(NdlCommons.connectedTo, next_intf_ont);
		            	next_intf_ont.addProperty(NdlCommons.connectedTo, intf_ont);
		            	logger.debug("Wired via existed crossconnect"+intf_ont+":"+next_intf_ont);
		            }		            
				}
				//transfer the requested resource to the path connection object
				deviceConnection.setName(requestConnection.getName());
				deviceConnection.setBandwidth(requestConnection.getBandwidth());
				deviceConnection.setAtLayer(requestConnection.getAtLayer());
			}
		}
    	return deviceConnection;
    }

    // generate switching action for a given device
    // two interfaces per action for now, the interfaces are non-adapted, to be processed/replaced in later stage 
    public boolean processAction(Interface intf, Device device,long bw)
    {
        // device.print();
        // System.out.println("-----Process Action---------");
        boolean validSwitching = false;
        SwitchingAction action = null;

        int action_size = 0;
        if (device.getActionList() != null) { // another request??
            action_size = device.getActionList().size();
            action = (SwitchingAction) device.getActionList().get(action_size - 1);
        } else {
            action = new SwitchingAction(ontModel);
            device.addSwitchingAction(action);
        }
        
        action.addClientInterface(intf);
        logger.debug("add a new interface to the action:"+intf.getURI()+":"+action.getClientInterface().size()+"\n");

        int matrix_rank;
        int max_rank = 0;
        String action_layer = null;
        SwitchMatrix matrix = null;
        LinkedList<SwitchMatrix> matrixList = device.getSwitchingMatrix();
        Iterator<SwitchMatrix> it = matrixList == null ? null : matrixList.iterator();
        while (it != null && it.hasNext()) {
            matrix = (SwitchMatrix) it.next();
	    if(matrix.getAtLayer()!=null){
            	matrix_rank = Layer.valueOf(matrix.getAtLayer()).rank();
            
            	if (matrix.getAtLayer().equals(intf.getAtLayer()))
                	action_layer = intf.getAtLayer();
            	else {
                	if (matrix_rank > max_rank) {
                    		action_layer = matrix.getAtLayer();
                    		max_rank = matrix_rank;
                	}
            	}
	    }
            // System.out.println(intf.getURI()+":"+matrix.getAtLayer()+":"+matrix_rank+":"+max_rank+":"+getLayerRank(intf.getAtLayer()));
        }
        if (action_layer != null) {
            action.setAtLayer(action_layer);
            action.setDefaultAction(getLayerAction(action_layer));
            DomainResourceType dType = new DomainResourceType(Layer.valueOf(action_layer).getLabelP().toString(),1);
            dType.setDomainURL(device.getURI());
            dType.setRank(Layer.valueOf(action_layer).rank());
            device.setResourceType(dType);
        }

        action.setBw(bw);
        // action.print();
        validSwitching = true;

        return validSwitching;
    }
	
	public Resource toDomain(Resource rs,String resourceType){
		if(rs.getProperty(NdlCommons.inDomainProperty)==null) {
			return null;
		}
		Resource domain=rs.getProperty(NdlCommons.inDomainProperty).getResource();
		String rType = resourceType; 
		Resource rDomain=null;
		//ontModel.write(System.out);
		if(NdlCommons.isStitchingNodeInManifest(rs)){
			rDomain=domain;
		}else{
			boolean found = false;		
			for(ResIterator j = ontModel.listResourcesWithProperty(NdlCommons.RDF_TYPE,NdlCommons.networkDomainOntClass); j.hasNext();) {
				rDomain = j.nextResource();
				//if(rDomain.getURI()==domain.getURI()) {
				//	found = true;
				//	break;
				//}
				if((rDomain.getLocalName().equalsIgnoreCase(rType)) && (rDomain.getURI().startsWith(domain.getURI()))){
					found = true;
					break;
				}
			}	
			if(!found) 
				rDomain = null;
		}
		logger.info("End Point Domain:"+rDomain+" OF "+rs);
	
		return rDomain;
	}


	//****************************
	//Step 2: Further process after the device connection: 
	//(1)find the uni interface switching; (2) finalize the action with correct label and adapted client intf
	//****************************
	@SuppressWarnings("unchecked")
	public SystemNativeError processDeviceConnection(NetworkConnection deviceConnection){
		SystemNativeError error=null;
		logger.info("Step 2: Process connection.\n");
		Label static_label=null;
		LinkedList <Device> deviceList=null;
		Iterator <Device> it;
		labelStack = new Stack <Label> ();

		 if(deviceConnection!=null){
			deviceList=(LinkedList<Device>)deviceConnection.getConnection();
			try{
				static_label = findCommonLabel(deviceList);
			}catch (Exception e){
				error = new SystemNativeError();
				error.setErrno(99);
				error.setMessage("Exception in finding common label:"+e.toString());
				e.printStackTrace();
				return error;
			} 
			int numDevice = deviceList.size(),count=0;
			Device d=null;
			it=deviceList.iterator();
			while(it.hasNext()){
				count++;
				d=it.next();
				if(static_label!=null){
					logger.info("Num of actions:"+d.getActionCount()+";"+d.getActionList().size()+";static label="+static_label.toString());
					
					d.getDefaultSwitchingAction().setLabel(static_label);
				}
				//if( (count==1) || (count == numDevice))
				//	continue;
				try {
					processInterface(d);
				} catch (RequestMappingException e) {
					error = new SystemNativeError();
					error.setErrno(1);
					error.setMessage("ProceseeInterface fail:"+d.toString());
					e.printStackTrace();
					return error;
				}
			}
			
			Collections.sort(deviceList,new NetworkElement.LayerComparator()); //sort the devices according to their layers, lower layer first
		 }
		
		return error;
	}	
	
	//(1) Layer adaptation process
	//(2) Label processing
	//(3) change for the uni switching interface (eg, Polatis)
	public void processInterface(Device device) throws RequestMappingException{
		logger.debug("----processing Interface-----"+device.getURI());
		
		SwitchMatrix matrix=null;
		LinkedList <SwitchMatrix> matrixList=device.getSwitchingMatrix();	
		if(matrixList!=null){
			int size=matrixList.size();

			for(int i=0;i<size;i++){
				matrix=(SwitchMatrix) matrixList.get(i);
				if(matrix==null) logger.error("No Switching Matrix at this position, error!");
				else {
					logger.debug(i+":SwitchingMatrix layer:"+matrix.getAtLayer()+":size="+size);
					if(matrix.getAtLayer()!=null){
						if(matrix.getDirection().equals(Direction.UNIDirectional.toString())){
							logger.debug("----------Unidirectional-------");
							device.processUNIInterface(matrix.getAtLayer());
						}
						//processing adaptation, Labels...
						processAdaptation(device, matrix.getAtLayer());
					}
				}
			}
		}
		else{
			logger.error("No Switching Matrix at this position, error!");
			throw new RequestMappingException ("No Switching Matrix at this position, error!");
		}
		device.getResource().removeAll(inConnection);
	}
	
	//process the layer adaptation: 
	//1. create the inferred client interface from adaptation
	//2. label assignment
	//3. update available labels
	public void processAdaptation(Device device, String currentLayer){
		LinkedList <SwitchingAction> actionList=device.getActionList();
		
		SwitchingAction action=null;
		Interface intf;
		Interface intf_client=null;
		String layer = null;
		int layer_rank,size,i;
		
		//int currentLayer_rank=getLayerRank(currentLayer);
		int currentLayer_rank = Layer.valueOf(currentLayer).rank();
		Label labelFromStack;
		Label currentLabel=null;
		int actionCount=actionList==null ? 0:actionList.size();
		for(int j=0;j<actionCount;j++){
			action = (SwitchingAction) actionList.get(j);
			logger.info("---------Adaptation Client --------"+device.getURI()+":"+actionCount+":"+currentLayer+":"+action.getAtLayer());
			if(action.getAtLayer().equals(currentLayer)){
				
				labelFromStack=checkLabelStack(currentLayer);
				logger.info("Label from the stack:"+labelFromStack);
				
				LinkedList <Interface> interfaceList=action.getClientInterface();
				size=interfaceList.size();
				for(i=0;i<size;i++){
					currentLabel= action.getLabel();
					
					//replace this interface w/ adapted client interface when available: check label
					intf=interfaceList.get(i);
					layer=intf.getAtLayer();
					
					markOccupy(intf);
					logger.info(intf.getURI()+";"+layer+":"+currentLayer+":"+currentLayer_rank+";"+i);
					//layer_rank=getLayerRank(layer);
					layer_rank=Layer.valueOf(layer).rank();
					logger.info(intf.getURI()+";"+layer+":"+layer_rank+";"+currentLayer+":"+currentLayer_rank+";"+i);
					if(!layer.equals(currentLayer)){
						if(layer_rank<currentLayer_rank){
							intf_client=getAdaptationClientInterface(intf,currentLayer,currentLabel,labelFromStack);
						}else{
							logger.error("Upper layer comes in earlier, error!");
						}
					}
					else{
						intf_client=processClientInterface(intf,currentLabel,labelFromStack);
					}
					
					if(intf_client!=null){
						interfaceList.remove(i);
						interfaceList.add(i, intf_client);
						if(currentLabel==null) {
							action.setLabel(intf_client.getLabel());
						}
						else{
							if(currentLabel.label<=0){
								action.setLabel(intf_client.getLabel());
							}
						}
						logger.debug("Current action:"+intf_client.getURI()+":"+action.getLabel_ID());
					}else{
						logger.error("Client interface not defined, probably due to lack of available label");
					}
				}
				//pass the right para to the interface not assigning label. 
				Interface tmp=null;
				Resource tmp_set=null;
				for(i=0;i<size;i++){
					intf=interfaceList.get(i);
					if(action.getLabel()==null){
						break;
					}
					if(intf.getLabel()==null){
						setLabel(intf,action.getLabel().getResource(intf.getModel()));
					}
					else{
						if(intf.getLabel().label<=0){
							setLabel(intf,action.getLabel().getResource(intf.getModel()));
						}
					}
					if(intf.getUsedLabelSet(ontModel)==null){
						tmp=intf;
					}
					else{
						tmp_set=intf.getUsedLabelSet(ontModel);
					}
				}
				if (tmp != null) {
					layer = tmp.getAtLayer();
					Property usedLabelSet =null;
					if (tmp_set==null) {
						String usedSet_str = tmp.getURI() + "/" + Layer.valueOf(layer).getUSet();
						tmp_set = ontModel.createProperty(usedSet_str);
					}
					usedLabelSet = ontModel.createProperty(NdlCommons.ORCA_NS + Layer.valueOf(layer).getPrefix() +".owl#" + Layer.valueOf(layer).getUSet());
					tmp.setUsedLabelSet(tmp_set);
			        tmp.getResource().addProperty(usedLabelSet, tmp_set);
				}
				if(action.getLabel()!=null){
					logger.debug("Label to the stack?:"+action.getLabel().toString());
					if(action.getLabel().label>0)	//the edge device may not have a label ID
						labelStack.add(action.getLabel());
				}
				//System.out.println("Action info:"+actionCount);
				//action.print();
				interfaceList=action.getClientInterface();
				Iterator <Interface> it=interfaceList.iterator();
				size=interfaceList.size();
				i=0;
				Resource rs,rs_next;
				while(it.hasNext()){
					intf=it.next();
					i++;
					rs=intf.getResource();
					for(int k=i;k<size;k++){
						rs_next=interfaceList.get(k).getResource();
						logger.info("---SwitchedTo:"+rs+"----"+rs_next);
						rs.addProperty(NdlCommons.switchedTo, rs_next);
						rs_next.addProperty(NdlCommons.switchedTo, rs);
						String labelP=NdlCommons.ORCA_NS+Layer.valueOf(layer).getPrefix()+".owl#"+Layer.valueOf(layer).getLabelP();
						ObjectProperty label_p=ontModel.getObjectProperty(labelP);
						Resource label_rs = null;
						if(intf.getLabel()!=null){
							label_rs = intf.getLabel().getResource(intf.getModel())!=null?intf.getLabel().getResource(intf.getModel()):intf.getLabel().getResource(ontModel);
							if(label_rs!=null)
								rs.addProperty(label_p,intf.getLabel().getResource(intf.getModel()));
							else
								logger.error("label_rs is null: label="+intf.getLabel().getURI());
						}
						Label k_label = interfaceList.get(k).getLabel();
						if(k_label !=null){
							label_rs = k_label.getResource(interfaceList.get(k).getModel())!=null?k_label.getResource(interfaceList.get(k).getModel()):intf.getLabel().getResource(ontModel);
							if(label_rs!=null)
								rs_next.addProperty(label_p,interfaceList.get(k).getLabel().getResource(interfaceList.get(k).getModel()));
							else
								logger.error("label_rs is null: label="+k_label.getURI());
						}
					}
				}
				
			}
				
		}	
	}	
	
	
	//find/create the client interface with the adaptation
	public Interface getAdaptationClientInterface(Interface parent, String currentLayer, Label currentLabel, Label labelFromStack){
		LinkedList <Interface> clientList=parent.getClientInterface();
		ListIterator <Interface> it;
		Interface intf=null;
		OntResource rs_client=null;
		String url,name;
		int labelID=-1;
	
		Resource label_rs=null;
		logger.info("Client:"+parent.getURI()+":"+parent.getAtLayer()+";"+currentLayer+"\n");
		if(clientList==null) {
			if(currentLayer.equals(Layer.LambdaNetworkElement.toString())){
				if(parent.getAtLayer().equals(Layer.OCGNetworkElement.toString())){
					//use an available label from the server interface
					label_rs=processLabel(parent,currentLabel,labelFromStack);
					if(label_rs!=null) 
						labelID=label_rs.getProperty(NdlCommons.layerLabelIdProperty).getInt();
					else{
						logger.error("No more available label form this OCG group!");
						return null;
					}
					Integer ID= new Integer(labelID);
					url=parent.getURI()+"/Lambda/"+ID.toString();
					name=parent.getName()+"-"+ID.toString();
					intf=new Interface(this.getOntModel(), url,name);
					intf.setAtLayer(currentLayer);
					
					OntClass layerOntClass=ontModel.getOntClass(NdlCommons.ORCA_NS+"dtn.owl#LambdaNetworkElement");
					
					rs_client=ontModel.createIndividual(url,layerOntClass);
					rs_client.addProperty(atLayer, layerOntClass);
					rs_client.addRDFType(NdlCommons.interfaceOntClass);
					rs_client.setLabel(name,null);
					intf.setResource(rs_client);
					logger.info("Interface setLabel:"+url+"--"+label_rs.getURI());
					setLabel(intf,ontModel.getOntResource(label_rs));
					intf.addServerInterface(parent);
					parent.addClientInterface(ontModel,intf,AdaptationProperty.WDM.toString());	
					intf.getResource().addProperty(NdlCommons.topologyInterfaceOfProperty,parent.getResource().getProperty(NdlCommons.topologyInterfaceOfProperty).getResource());
				}
			}
		}
		else{
			it=clientList.listIterator();

			while(it.hasNext()){
				intf=(Interface) it.next();
				intf.getResource().addProperty(NdlCommons.topologyInterfaceOfProperty,parent.getResource().getProperty(NdlCommons.topologyInterfaceOfProperty).getResource());
				if(intf.getAtLayer().equals(currentLayer)) {
					intf=processClientInterface(intf,currentLabel,labelFromStack);
					break;
				}
				else {
					intf=getAdaptationClientInterface(intf,currentLayer,currentLabel,labelFromStack);
					if(intf!=null) break;
				}
			}
		}
		
		return intf;
	}
	
	//1. generate vlan interface for Ethernet interface
	//2. get the correct label update and assignment
	public Interface processClientInterface(Interface intf, Label currentLabel,Label labelFromStack){
		Interface vlanInterface=null;
		int label=-1;
		Label vlan=null;
		Resource label_rs=null;
		String layer=null;
		if(intf.getAtLayer().equals(Layer.EthernetNetworkElement.toString())){	
			layer = intf.getAtLayer();
			label_rs = processLabel(intf,currentLabel,labelFromStack);
			if(label_rs!=null && label_rs.getProperty(NdlCommons.layerLabelIdProperty)!=null){ 
				label=label_rs.getProperty(NdlCommons.layerLabelIdProperty).getInt();
				String layer_url = NdlCommons.ORCA_NS+Layer.valueOf(layer).getPrefix()+".owl#"+layer;
				String url=intf.getURI();
				if(label>0)
					url=intf.getURI()+"/VLAN/"+label;
				//String name=intf.getName().concat("-")+label;
				String name=intf.getName();
				vlanInterface=new Interface(this.getOntModel(), url,name);
				vlanInterface.setAtLayer(intf.getAtLayer());
				Individual vlan_interface_ont =  ontModel.createIndividual(url,NdlCommons.interfaceOntClass);
				vlan_interface_ont.addProperty(atLayer, ontModel.createResource(layer_url));
				setLabel(vlanInterface,ontModel.getOntResource(label_rs));
				vlanInterface.addServerInterface(intf);
				intf.addClientInterface(ontModel,vlanInterface,AdaptationProperty.TaggedEthernet.toString());
			}else{
				logger.error("label_rs is null");
			}
			
		}
		else if(intf.getAtLayer().equals(Layer.LambdaNetworkElement.toString())){
			label_rs=processLabel(intf,currentLabel,labelFromStack);
			if(label_rs!=null)
				setLabel(intf,ontModel.getOntResource(label_rs));
			vlanInterface=intf;
		}
		logger.info("Obtained Label ID:"+label_rs+":"+intf.getURI());
		return vlanInterface;
	}
	
	public int getLayerLabel(String layer){
		if(layer==null) return -1;
		BitSet lableSet = lableSetPerLayer.get(layer);
		if(lableSet!=null){
			return lableSet.nextSetBit(0);
		}
		else return -1;
	}
	
	public Label findCommonLabel(LinkedList <Device> deviceList) throws Exception{
		Iterator <Device> it=deviceList.iterator();
		int numDevice = deviceList.size(),count=0;
		Device device=null;
		String currentLayer=null,static_layer=null,d_swapping = null;;
		lableSetPerLayer = new HashMap <String,BitSet> ();
		HashMap <String,String> swappingPerLayer = new HashMap <String,String> ();
		BitSet currentLableSet=null,lableSet;
		int static_label=0,p_tag=0;
		logger.debug("------Find Common Label range-----\n");
		Resource static_label_rs = null;
		Label label = null;
		
		for(Device d:deviceList){
			static_layer = d.getAtLayer();
			if(d.getSwappingCapability()!=null && d.getSwappingCapability().equalsIgnoreCase(static_layer)){
				d_swapping=d.getSwappingCapability();
				if(swappingPerLayer.get(static_layer)==null)
					swappingPerLayer.put(static_layer, d_swapping);
			}
		}
		
		while(it.hasNext()){
			count++;
			device = it.next();
			static_layer = device.getAtLayer();
			d_swapping = swappingPerLayer.get(static_layer);
			logger.debug("Find Common Label range:"+device.getURI()+":"+count+"\n");
			if( (d_swapping==null) && ((count==1) || (count==numDevice)) ){ //the first and last devices are the servers that may carry labels 
				SwitchingAction action = device.getDefaultSwitchingAction();
				if(action!=null){
					Interface intf = action.getDefaultClientInterface();
					if(intf!=null){
						List<Resource>static_label_rs_list = new ArrayList<Resource>();
						Resource p_intf_rs=null;
						OntResource p_intf_ont=null;
						if(intf.getResource().hasProperty(NdlCommons.linkTo)){
							p_intf_rs=intf.getResource().getProperty(NdlCommons.linkTo).getResource();
							p_intf_ont=this.ontModel.getOntResource(p_intf_rs);
							logger.info("intf="+intf.getResource().getURI()+";p_intf="+p_intf_rs.getURI());
						}
					
						if(intf.getResource().hasProperty(NdlCommons.layerLabel)){
							static_label_rs = null;
							for (StmtIterator j=intf.getResource().listProperties(NdlCommons.layerLabel);j.hasNext();){
								static_label_rs=j.next().getResource();
								if(static_label_rs.getProperty(NdlCommons.visited)!=null){  //defined in InterDomainHandler.createManifest()
									if(static_label_rs.getProperty(NdlCommons.visited).getBoolean()!=true){
										static_label_rs_list.add(static_label_rs);
									}
								}else
									static_label_rs_list.add(static_label_rs);
							}
							for(Resource static_rs:static_label_rs_list){
								if(static_rs.hasProperty(NdlCommons.layerLabelIdProperty)){
									static_label = static_rs.getProperty(NdlCommons.layerLabelIdProperty).getInt();
									logger.info("static_rs="+static_rs.getURI()+";static_label="+static_label);
									//always using the tag from parent reservation first.
									p_tag=get_pdomain_tag(p_intf_rs.getURI(),static_label);
									//if((p_tag==static_label) || (p_tag==0)){
									if(p_tag==static_label){
										static_rs.addProperty(NdlCommons.visited, "true", XSDDatatype.XSDboolean);
										static_label_rs=static_rs;
										intf.getResource().removeProperty(NdlCommons.layerLabel, static_rs);
										logger.info("Passed label:intf="+p_intf_rs.getURI()+";static Label="+static_label_rs.getURI()+";p_tag="+p_tag+";static_layer="+static_layer);
										break;
									}else{
										//static_label = p_tag;
										static_label = -1;
										static_label_rs=this.ontModel.createIndividual(p_intf_rs.getNameSpace()+String.valueOf(p_tag),NdlCommons.labelOntClass);
										static_label_rs=intf.getModel().createIndividual(p_intf_rs.getNameSpace()+String.valueOf(p_tag),NdlCommons.labelOntClass);
										static_label_rs.addProperty(NdlCommons.layerLabelIdProperty,String.valueOf(p_tag),XSDDatatype.XSDint);
										logger.error("Passed in static label is not in the available labelset:static="+static_label+";set=");
										throw new Exception("Passed in static label is not in the available labelset:static="+static_label+";set=");
										
									
									}
									//intf.getResource().removeProperty(NdlCommons.layerLabel, static_rs);
								}
							}
			            }else{
							p_tag=get_pdomain_tag(p_intf_rs.getURI(), -1);
							if(p_tag!=0){
								static_label = p_tag;
								//static_label_rs=this.ontModel.createIndividual(p_intf_rs.getNameSpace()+String.valueOf(p_tag),NdlCommons.labelOntClass);
								static_label_rs=intf.getModel().createIndividual(p_intf_rs.getNameSpace()+String.valueOf(p_tag),NdlCommons.labelOntClass);
								static_label_rs.addProperty(NdlCommons.layerLabelIdProperty,String.valueOf(p_tag),XSDDatatype.XSDint);
								/*OntResource static_label_ont = this.ontModel.createOntResource(static_label_rs.getURI());	            			
								label = new Label(static_label_ont,p_tag,static_layer);
								action.setLabel(label);
								action.setLabel_ID(p_tag);
								p_intf_rs.addProperty(NdlCommons.layerLabel, static_label_rs);
								*/
								logger.info("Passed label from domain reservation:intf="+p_intf_rs.getURI()+";tag="+static_label+";static_layer="+static_layer);
							}
						}
					}
				}
				continue;
			}
			
			SwitchMatrix matrix=null;
			LinkedList <SwitchMatrix> matrixList=device.getSwitchingMatrix();			
			if(matrixList!=null){
				int size=matrixList.size();
				for(int i=0;i<size;i++){
					matrix=(SwitchMatrix) matrixList.get(i);
					logger.debug("Device:"+device.getResource().getLocalName()+":"+i+":SwitchingMatrix layer:"+matrix.getAtLayer()+":size="+size);
					currentLayer=matrix.getAtLayer();		
					if(matrix.getAtLayer()!=null){
						if(Layer.valueOf(currentLayer).rank()>1){  //OCG Layer above for now
							currentLableSet=findAvailableLabel(device,matrix.getAtLayer());
							logger.debug("currentLabelSet="+currentLableSet);
							if(lableSetPerLayer.containsKey(currentLayer)){
								lableSet=lableSetPerLayer.get(currentLayer);
								logger.debug("labelSetFromLayer="+lableSet);
								if(currentLableSet!=null)
									lableSet.and(currentLableSet);
							}
							else{
								if(currentLableSet!=null)
									lableSetPerLayer.put(currentLayer,currentLableSet);
							}
						}
					}
				}
			}
			else{
				try {
					throw new RequestMappingException ("No Switching Matrix at:"+device.getName() +", error!");
				} catch (RequestMappingException e) {
					logger.error("No Switching Matrix at:"+device.getName() +", error!");
				}
			}
		}
		d_swapping = swappingPerLayer.get(currentLayer);
		logger.debug("findCommonLabel:static_label="+static_label+";d_swapping="+d_swapping+"currentLayer="+currentLayer);
		if(this.usedLabelSetPerLayer!=null)
			logger.debug("findCommonLabel:used_label_set="+this.usedLabelSetPerLayer.get(currentLayer));
		BitSet staticLabelSet=null;
		if(lableSetPerLayer!=null)
			staticLabelSet=lableSetPerLayer.get(currentLayer);
		logger.debug("Original staticLabelSet:"+staticLabelSet);
		if(staticLabelSet!=null){
			if(this.usedLabelSetPerLayer!=null && this.usedLabelSetPerLayer.get(currentLayer)!=null){
				staticLabelSet.andNot(this.usedLabelSetPerLayer.get(currentLayer));
				logger.debug("staticLabelSet+usedLabelSet:"+staticLabelSet);
			}
		}

		logger.debug("findCommonLabel:Final label_set="+staticLabelSet);
		int current_static_label=0;
		if(static_label>0 && d_swapping==null){
			if(staticLabelSet==null){
				logger.error("No avaialbel label set: current layer="+static_layer);
				throw new Exception("No avaialbel label set: current layer="+static_layer);
			}else{
				logger.debug("findCommonLabel:check match:"+current_static_label+";static_label="+static_label);
				
				if(!staticLabelSet.get((int)static_label)){
					current_static_label=staticLabelSet.nextSetBit(0);
					logger.error("Passed in static label is not in the available labelset:static="+static_label+";set="+staticLabelSet);
					throw new Exception("Passed in static label is not in the available labelset:static="+static_label+";set="+staticLabelSet);
					
					/*Note: if the site becomes d_swapping, it should pick a local available tag, and swap the passed in tag.
					  
					logger.error("AM choose one available instead"+current_static_label);
					String label_namespace=null;
					if(static_label_rs!=null)
						label_namespace=static_label_rs.getNameSpace();
					else
						label_namespace=device.getResource().getNameSpace();
					
					static_label_rs=this.ontModel.createResource(label_namespace+String.valueOf(current_static_label));
					static_label_rs.addProperty(NdlCommons.layerLabelIdProperty,String.valueOf(current_static_label),XSDDatatype.XSDint);
					static_label=current_static_label;
					*/
				}
			}
		}

		if(static_label_rs!=null){
			OntResource static_label_ont = this.ontModel.createIndividual(static_label_rs.getURI(),NdlCommons.labelOntClass);
			label = new Label(static_label_ont,static_label,static_layer);
		}
		return label;
	}
	
	public int get_pdomain_tag(String intf_url,int static_label){
		int tag = get_pdomain_tag(intf_url,static_label,this.pdomain_properties);
		return tag;
	}
	
	public int get_pdomain_tag(String intf_url,int static_label,Properties pdomain_properties){	
		String tag_str=null,tag_m=null;
		String [] tag_str_list=null;
		int tag=0,num_tag=0;
		boolean flag=false;
		logger.info("get_pdomain_tag,intf_url="+intf_url+";static_label="+static_label);
		if(pdomain_properties==null){
			logger.warn("No pdomain_property!");
			return 0;
		}
		logger.info("pdomain_properties:"+pdomain_properties);
		for(Entry entry:pdomain_properties.entrySet()){
			logger.info("tag:"+entry.getKey()+";intf="+entry.getValue());
			if(intf_url.equalsIgnoreCase((String)entry.getValue())){
				tag_str = (String) entry.getKey();
				tag_str_list = tag_str.split(",");
				num_tag=tag_str_list.length;
				for(String tag_s:tag_str_list){
					tag_m=tag_s;
					tag=Integer.valueOf(tag_s);
					if( (tag==static_label) || (static_label==-1)){
						flag=true;
						break;
					}
				}
				break;
			}
		}
		if(tag_str==null)
			return 0;
		if(!flag){
			tag_m=tag_str_list[0];
			tag=Integer.valueOf(tag_m);
		}
		String tag_str_new=null,tag_s=null;
		for(int i=0;i<num_tag;i++){
			tag_s=tag_str_list[i];
			if(!tag_s.equalsIgnoreCase(tag_m)){
				if(i==0 || tag_str_new==null)
					tag_str_new=tag_s;
				else
					tag_str_new = tag_str_new.concat(","+tag_s);
			}
		}
		logger.info("tag="+tag+";tag_str_new:"+tag_str_new);
		pdomain_properties.remove(tag_str);
		if(tag_str_new!=null)
			pdomain_properties.put(tag_str_new, intf_url);
		
		return tag;
	}
	
	public BitSet findAvailableLabel(Device device, String currentLayer){
		BitSet lableBitSet = null,currentLableBitSet=null;
		
		if(currentLayer.equals(Layer.EthernetNetworkElement.toString())){
			lableBitSet=new BitSet(InterDomainHandler.max_vlan_tag);
			lableBitSet.set(2,InterDomainHandler.max_vlan_tag);
		}
		
		if(currentLayer.equals(Layer.LambdaNetworkElement.toString())){
			lableBitSet=new BitSet(11);
			lableBitSet.set(1,11);
		}
		
		LinkedList <SwitchingAction> actionList=device.getActionList();
		SwitchingAction action=null;
		Interface intf;
		Interface intf_client=null;
		int actionCount=actionList==null ? 0:actionList.size();
		int size,layer_rank;
		String layer;
		int currentLayer_rank = Layer.valueOf(currentLayer).rank();
		for(int j=0;j<actionCount;j++){	
			action = (SwitchingAction) actionList.get(j);
			logger.info("--Find Available Label:"+device.getURI()+":"+actionCount+":"+currentLayer+":"+action.getAtLayer());
			if(action.getAtLayer().equals(currentLayer)){
				LinkedList <Interface> interfaceList=action.getClientInterface();
				size=interfaceList.size();
				for(int i=0;i<size;i++){
					intf=interfaceList.get(i);
					layer=intf.getAtLayer();
					layer_rank=Layer.valueOf(layer).rank();
					logger.debug("Interface Layer:"+intf.getURI()+";"+layer+":"+layer_rank+";"+currentLayer+":"+currentLayer_rank+";"+i);
					if(!layer.equals(currentLayer)){
						if(layer_rank<currentLayer_rank){
							intf_client=getAdaptationClient(intf,currentLayer);
							intf=intf_client;
						}
					}
					currentLableBitSet=getAvailableLabelRange(intf,currentLayer);
					if(currentLableBitSet!=null)
						lableBitSet.and(currentLableBitSet);
				}
			}
		}
		
		return lableBitSet;
	}
	
	public Interface getAdaptationClient(Interface parent, String currentLayer){
		LinkedList <Interface> clientList=parent.getClientInterface();
		ListIterator <Interface> it;
		Interface intf=null;
		OntResource rs_client=null;
		String url,name;
		int labelID=-1;
	
		Resource label_rs=null;
		logger.debug("Client:"+parent.getURI()+":"+parent.getAtLayer()+";"+currentLayer+"\n");
		if(clientList==null) {
			logger.debug(currentLayer+";"+parent.getResource());
			if(currentLayer.equals(Layer.LambdaNetworkElement.toString())){
				if(parent.getAtLayer().equals(Layer.OCGNetworkElement.toString())){
					return parent;
				}
			}
		}
		else{
			it=clientList.listIterator();

			while(it.hasNext()){
				intf=(Interface) it.next();
				if(intf.getAtLayer().equals(currentLayer)) {
					break;
				}
				else {
					intf=getAdaptationClient(intf,currentLayer);
					if(intf!=null) break;
				}
			}
		}
					
		return intf;
	}
	
	public BitSet getAvailableLabelRange(Interface intf, String currentLayer){
		BitSet lableBitSet=new BitSet();
		
		String rsURI = intf.getResource().getURI();
		String layer = intf.getAtLayer();
        String availableLabelSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getASet();
        
		ResultSet results= getAvailableLabelSet(rsURI,availableLabelSet);		
		//outputQueryResult(results);
		//results= getAvailableLabelSet(rsURI,availableLabelSet);
		
		int lower=0;
		int upper=0;
		String lowerBound=(String) results.getResultVars().get(0);
		String upperBound=(String) results.getResultVars().get(1);
		String l=(String) results.getResultVars().get(2);
		String u=(String) results.getResultVars().get(3);
		String setElement = (String) results.getResultVars().get(4);
		String availableSet = (String) results.getResultVars().get(5);
		
		Resource lowerLabel=null;
		Resource upperLabel=null; 
		Resource labelRange_rs=null;
		Resource availableSet_rs = null;
		QuerySolution solution=null;
		
		if (!results.hasNext()) {
			logger.error("No available label!\n");
			return null;
		}
		
		while (results.hasNext()){
			solution=results.nextSolution();
			availableSet_rs = solution.getResource(availableSet);
			labelRange_rs=solution.getResource(setElement);
			
			if(solution.getLiteral(lowerBound) != null){
				lower=solution.getLiteral(lowerBound).getInt();
				upper=solution.getLiteral(upperBound).getInt();
				lowerLabel=solution.getResource(l);
				upperLabel=solution.getResource(u);
				if(upper<lower){
					logger.error("Wrong range: lower="+lower+":upper="+upper);
					continue;
				}
				lableBitSet.set(lower,upper+1);
			}
			else{
				lowerLabel=labelRange_rs;
				if(lowerLabel.getProperty(NdlCommons.layerLabelIdProperty)!=null){
					lower = lowerLabel.getProperty(NdlCommons.layerLabelIdProperty).getInt();
					lableBitSet.set(lower);
					upper=0;
					upperLabel=null;
				}
			}
			logger.info("\n getAvailableLabelRange:-------:"+intf.getURI()+":"+availableSet_rs+"----:"+lower+":"+upper+":"+lowerLabel+":"+upperLabel+"\n");
		}
		
		return lableBitSet;
	}		
	//first fit label assignment
	public Resource processLabel(Interface intf, Label currentLabel,Label labelFromStack){
		String rsURI = intf.getURI();
		String layer = intf.getAtLayer();
        String availableLableSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getASet();
        String usedLabelSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getUSet();
        String usedLabelSet_str = NdlCommons.ORCA_NS+Layer.valueOf(layer).getPrefix() + ".owl#" + Layer.valueOf(layer).getUSet();

        String usedSet = intf.getURI() + "/" + Layer.valueOf(layer).getUSet();
        
        String currentLayer = intf.getAtLayer();
        
        Resource label_rs = processLabel(intf.getModel(), rsURI,availableLableSet,usedLabelSet,usedLabelSet_str,usedSet,currentLabel,labelFromStack,currentLayer);
        
        return label_rs;
	}
	
	public Resource processLabel(OntModel m, String rsURI, String availableLableSet_str, String usedLabelSet, String usedLabelSet_str, String usedSet_str, 
			Label currentLabel, Label labelFromStack, String currentLayer){
		
		if(currentLabel!=null){
			if( (currentLabel.getResource(m)!=null) && (currentLabel.label>0) && (currentLayer.equals(Layer.EthernetNetworkElement.toString()))) {
				return currentLabel.getResource(m);
			}
		}
	
	
		if(currentLayer.equals(Layer.OCGNetworkElement.toString())){
			currentLayer=Layer.LambdaNetworkElement.toString();
		}
		
		int label=getLayerLabel(currentLayer);

		logger.info("From available label range:"+rsURI+":"+currentLayer+":"+label+"\n");
		
		Float stackLabel = 0f, localLabel = 0f;
		int lower=0,upper=0,lowest=0,lowestUpper=0;
		if(labelFromStack!=null)
			stackLabel=labelFromStack.label;
		
		if(currentLabel!=null && currentLabel.label>0)
			stackLabel = currentLabel.label;

		if(stackLabel!=0){
            if(label==0){
            	logger.error("The passed label from the stack is not within the available label range!:"+stackLabel);
            	return null;  //depends on the label continuity requirement
            }
		}else{//no stack label, pick label locally
            if(label==0){
                label=lowest;
            } else {
            	stackLabel= label + 0f;
            }
		}
		
		ResultSet results;
		QuerySolution solution=null;
		String availableSet = null;
		
		Resource lowerLabel=null,lowestLabel=null,lowestUpperLabel=null;
		Resource upperLabel=null; 
		Resource labelRange_rs=null,lowestLabelRange_rs=null;
		Resource availableSet_rs = null;
		if(label>0){
			results= getAvailableLabelSet(rsURI,availableLableSet_str);		
			//outputQueryResult(results);
			//results= getAvailableLabelSet(rsURI,availableLableSet_str);
		
			String lowerBound=(String) results.getResultVars().get(0);
			String upperBound=(String) results.getResultVars().get(1);
			String l=(String) results.getResultVars().get(2);
			String u=(String) results.getResultVars().get(3);
			String setElement = (String) results.getResultVars().get(4);
		
			availableSet = (String) results.getResultVars().get(5);
		
			if (!results.hasNext()) {
				logger.error("No available label!\n");
				return null;
			}
			int i=0;
			while (results.hasNext()){
				solution=results.nextSolution();
				availableSet_rs = solution.getResource(availableSet);
				labelRange_rs=solution.getResource(setElement);
			
				if(solution.getLiteral(lowerBound) != null){
					lower=solution.getLiteral(lowerBound).getInt();
					upper=solution.getLiteral(upperBound).getInt();
					lowerLabel=solution.getResource(l);
					upperLabel=solution.getResource(u);
					if((stackLabel>=lower) && (stackLabel<=upper)){
						localLabel=stackLabel;
						lowest=lower;
	                	lowestLabel=lowerLabel;
        	        	lowestUpper=upper;
                		lowestUpperLabel=upperLabel;
                    	lowestLabelRange_rs=labelRange_rs;

                    	break;
					}
				}
				else{
					upper=0;
					upperLabel=null;
					lowerLabel=labelRange_rs;
					if(lowerLabel.getProperty(NdlCommons.layerLabelIdProperty)!=null)
						lower = lowerLabel.getProperty(NdlCommons.layerLabelIdProperty).getInt();
					if(lower==stackLabel){
						localLabel=stackLabel;
						lowest=lower;
						lowestLabel=lowerLabel;
						lowestUpper=upper;
						lowestUpperLabel=upperLabel;
						lowestLabelRange_rs=labelRange_rs;

						break;
					}
				}
				if((i==0) || (lower<=lowest)){
					lowest=lower;
					lowestLabel=lowerLabel;
					lowestUpper=upper;
					lowestUpperLabel=upperLabel;
					lowestLabelRange_rs=labelRange_rs;
				}
				logger.info("\nProcessLabel -- Label Range:"+availableSet_rs+"-"+lowestLabelRange_rs+"----:"+lower+
						":lowestUpper="+lowestUpper+":"+lowerLabel+":"+lowestUpperLabel+":StackLabel="+stackLabel+":Label="+label+":currentLabel"+currentLabel);
				i++;
			}
		}	

		//double check the picked label was not in the usedLabelSet		
		results=getUsedLabelSet(rsURI,usedLabelSet);
		String used=(String) results.getResultVars().get(0);
		String usedSet = (String) results.getResultVars().get(1);
		Resource usedSet_rs = null;
		while(results.hasNext()){
			solution=results.nextSolution();
			usedSet_rs = solution.getResource(usedSet);
			if (label == solution.getResource(used).getProperty(NdlCommons.layerLabelIdProperty).getInt()){
				logger.error("Existing Used Label:"+solution.getResource(used) + "\n");
			}
		}
		Resource picked_label_rs = ontLabelUpdate(availableSet_rs, lowestLabelRange_rs, lowest, lowestUpper, lowestLabel, lowestUpperLabel, stackLabel.intValue());	
				
		//send the picked one to the usedLabelSet
		
		setUsedLabelSet(rsURI,usedLabelSet_str,usedSet_str,usedSet_rs, picked_label_rs);
	
		Resource label_rs=getLabelResource(picked_label_rs,label);
		
		return label_rs;		
	}
	
	public void setLabel(Interface intf, OntResource label_rs){
		
		Label label=new Label();

		Resource intf_rs=intf.getResource();
		
		if( label_rs!=null &&  label_rs.getProperty(NdlCommons.layerLabelIdProperty)!=null ) {
			label.setResource(label_rs);
			label.label=label_rs.getProperty(NdlCommons.layerLabelIdProperty).getFloat();
			String layer=intf.getAtLayer();
			label.type=layer;
			String prefix=Layer.valueOf(layer).getPrefix().toString();
			String labelP=NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getLabelP();
			ObjectProperty label_p=ontModel.getObjectProperty(labelP);
			//System.out.println(labelP);
			intf_rs.addProperty(label_p, label_rs);
		}
		else{
			logger.info("Label is bad:"+label_rs);
			label.label=-1f;
		}
		
		intf.setLabel(label);
	}
	// for now, it only works on the client port of DTN
	public void markOccupy(Interface intf){
		Resource intf_rs=intf.getResource();
		if(intf_rs.hasProperty(NdlCommons.ocgLine)){
			intf_rs.addLiteral(NdlCommons.portOccupied, true);
		}	
	}
	
	//check the label stack to get the label from previous hops.
	public Label checkLabelStack(String currentLayer){
		if(labelStack.empty()) return null;
		Label exist = labelStack.peek();
		logger.debug(exist.toString()+":"+currentLayer);
		if(exist.type.equals(currentLayer)){
			exist = labelStack.pop();
		}
		else{
			if(Layer.valueOf(exist.type).rank() < Layer.valueOf(currentLayer).rank()){
				labelStack.pop();
				if(!labelStack.empty()) 
					exist=labelStack.pop();
				else
					exist=null;
			}
			else{
				exist=null;
			}
		}
		return exist;
	}	
	public NetworkConnection getDeviceConnection() {
		// TODO Auto-generated method stub
		return deviceConnection;
	}

	public NetworkConnection getReleaseNetworkConnection() {
		return releaseNetworkConnection;
	}

	public void setReleaseNetworkConnection(
			NetworkConnection releaseNetworkConnection) {
		this.releaseNetworkConnection = releaseNetworkConnection;
	}

	public Properties getPdomain_properties() {
		return pdomain_properties;
	}

	public void setPdomain_properties(Properties pdomain_properties) {
		this.pdomain_properties = pdomain_properties;
	}

	public HashMap<String, BitSet> getUsedLabelSetPerLayer() {
		return usedLabelSetPerLayer;
	}

	public BitSet getUsedLabelSetPerLayer(String layer) {
		if(usedLabelSetPerLayer==null)
			return null;
		return usedLabelSetPerLayer.get(layer);
	}
	
	public void setUsedLabelSetPerLayer(
			String layer, BitSet usedLabelSet) {
		if(this.usedLabelSetPerLayer == null){
			this.usedLabelSetPerLayer = new HashMap<String, BitSet>();
			this.usedLabelSetPerLayer.put(layer, usedLabelSet);
		}
		else{
			if(this.usedLabelSetPerLayer.get(layer)==null){
				this.usedLabelSetPerLayer.put(layer, usedLabelSet);
			}else{
				this.usedLabelSetPerLayer.get(layer).or(usedLabelSet);
			}
		}
		System.out.println("Used label in mapper:"+this.usedLabelSetPerLayer.get(layer));
	}
}

