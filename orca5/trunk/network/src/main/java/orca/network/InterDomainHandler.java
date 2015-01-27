

package orca.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.xml.bind.DatatypeConverter;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import orca.embed.workflow.Domain;
import orca.ndl.*;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.LabelSet;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchingAction;
import orca.shirako.api.IReservation;
import orca.shirako.plugins.substrate.ISubstrateDatabase;

import net.jwhoisserver.utils.InetNetworkException;

import org.apache.log4j.Logger;

public class InterDomainHandler extends MappingHandler implements LayerConstant{

	private Hashtable <String,OntModel> domainModel;
	private Reasoner idmReasoner;
	public boolean cloudRequest=false,interDomainRequest=false;
	
	@SuppressWarnings("static-access")
	public InterDomainHandler(){
		super();
		Model schemaModel=FileManager.get().loadModel("http://geni-orca.renci.org/owl/domain.owl");
		
		OntModel idmBase = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);		
		
		idmReasoner = idmBase.getReasoner();
		idmReasoner=idmReasoner.bindSchema(schemaModel);

		domainModel = new Hashtable <String, OntModel>();
	}
	
	public void addAbstractDomainString(String abstractModel) throws IOException {
	    ByteArrayInputStream modelStream = new ByteArrayInputStream(abstractModel.getBytes());        
        Domain domain = new Domain(modelStream);
        if (mapper==null) {
			mapper= new RequestMapping(domain.getOntProcessor().getOntModel());
			logger = domain.getOntProcessor().getLogger();
			mapper.setLogger(logger);
			//mapper.setPrefix(idm);
		}
		domainModel.put(domain.getURI(), domain.getModel());
		logger.info("Added Domain:"+domain.getURI());	
	}
	
	public void addDomain(String inputFile) throws IOException{
		Domain domain = new Domain(inputFile);
		init(domain);
	}
	
	public void addDomain(InputStream stream ) throws IOException{
		Domain domain = new Domain(stream);
		init(domain);
	}
	
	public void init(Domain domain){
		OntModel model=domain.abstractDomain(null);
		if (mapper==null) {
			mapper= new RequestMapping(domain.getOntProcessor().getOntModel());
			logger = domain.getOntProcessor().getLogger();
			mapper.setLogger(logger);
			//mapper.setPrefix(idm);
		}
		domainModel.put(domain.getURI(), model);		
	}
	
	/*public void addDomain(String inputFile,mapper processor,boolean importFlag) throws IOException{

		Domain domain = new Domain(inputFile,processor,importFlag);
		OntModel model=domain.abstractDomain();
		domainModel.put(domain.getName(), model);
	}*/
	
	public OntModel abstractModel(){
		
		idm=ModelFactory.createOntologyModel();
		
		mapper.setPrefix(idm);
		
		for (Enumeration <OntModel> i= domainModel.elements();i.hasMoreElements();){
			idm.add(i.nextElement());
		}
		
		InfModel idmInf=ModelFactory.createInfModel(idmReasoner,idm);
		
		//SimpleSelector s= new SimpleSelector(mapper.deviceOntClass,mapper.interfaceOf,mapper.deviceOntClass);
		//idmInf.listStatements(s);
		
		//idmInf.write(System.out);
		
		//idm=ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, idmInf);
		
		//System.out.println("idmReasoner #statement:"+idmInf.size()+":#Base Statements:"+idm.size());
		
		return idm;
	}
	
	public Hashtable <String,LinkedList <Device> > handleRequest(InputStream requestStream) throws IOException, InetNetworkException{
        mapper.setOntModel(idm);
        return handleRequest(mapper.ontCreate(requestStream));		
	}
	
    public Hashtable <String,LinkedList <Device> > handleRequest(String requestFile) throws IOException, InetNetworkException{
        mapper.setOntModel(idm);
        return handleRequest(mapper.ontCreate(requestFile));
    }
    
    public Hashtable <String,LinkedList <Device> > handleRequest(OntModel model) throws InetNetworkException, IOException {       	 
        //idm.write(System.out);
    	//request connections
        requestModel=model;
        mapper.addRequest(model);
    	requestMap = mapper.parseRequest(model);
        
    	//VT mapping within one cloud domain, the CloudHandler will be called in the controller
    	if(mapper.domain!=null){
    		logger.warn("Within one domain:"+mapper.domain);
    		return null;
    	}
    	return handleRequest(requestMap);
    }
    
    public Hashtable <String,LinkedList <Device> > handleRequest(Hashtable <String, NetworkConnection> requestMap) throws InetNetworkException, IOException { 
    		
    	//otherwise, do it in the inter-domain abstract topology
        Hashtable <String,LinkedList <Device> > connectionList = new Hashtable <String,LinkedList<Device>> ();
        LinkedList <Device> dependList = new LinkedList<Device> ();
        LinkedList <Device> domainList=null;
        domainConnectionList = new Hashtable <String,LinkedList <Device> > ();
        
        Resource rs1 = null,rs2 = null,rs3;
        Resource rs1_ip=null,rs2_ip=null;
		String rs1_str = null,rs2_str = null,resourceType;
		String rs1_ip_str,rs2_ip_str;
		long bw = 0;
		String connectionName=null;
        
		if(requestMap.isEmpty()) return null;
		
    	boolean success=true;	
    	NetworkConnection requestConnection=null;
		for(Entry <String,NetworkConnection > entry:requestMap.entrySet()){
			connectionName=entry.getKey();
			requestConnection=entry.getValue();
			rs1_str=requestConnection.getEndPoint1();
			rs2_str=requestConnection.getEndPoint2();
			bw=requestConnection.getBw();
			rs1_ip_str=requestConnection.getEndPoint1_ip();
			rs2_ip_str=requestConnection.getEndPoint2_ip();
			if(rs1_ip_str!=null)
				rs1_ip=requestModel.createOntResource(rs1_ip_str);
			
			if(rs2_ip_str!=null)
				rs2_ip=requestModel.createOntResource(rs2_ip_str);
			
			resourceType= requestConnection.getType();
			rs1=requestModel.getResource(rs1_str);
			rs2=requestModel.getResource(rs2_str);
			
            NetworkConnection connection = new NetworkConnection ();
            connection.setName(connectionName);
            connection.setEndPoint1(rs1_str);
            connection.setEndPoint2(rs2_str);
            connection.setEndPoint1_ip(rs1_ip_str);
            connection.setEndPoint2_ip(rs2_ip_str);
            connection.setEndPoint1_type(requestConnection.getEndPoint1_type());
            connection.setResourceCount1(requestConnection.getResourceCount1());
            connection.setEndPoint1_type(requestConnection.getEndPoint1_type());
            connection.setResourceCount2(requestConnection.getResourceCount2());
            connection.setType(resourceType);
    		mapper.setDeviceConnection(connection);
			
    		NetworkConnection resultConnection;
    		
			Resource sourceDomain=toDomain(rs1,requestConnection.getEndPoint1_type());
			Resource destDomain=toDomain(rs2,requestConnection.getEndPoint1_type());
			if( (sourceDomain==null) || (destDomain==null)){
				//System.out.println("End Points Not in a domain! Source: "+sourceDomain+" Dest: " + destDomain);
				logger.error("End Points Not in a domain! Source: "+sourceDomain+" Dest: " + destDomain);
				return null;
			}else{
				if(sourceDomain==destDomain){
					requestConnection.setConnectionType("ClouRequest");
					Hashtable <String,NetworkConnection > localRequestMap = new Hashtable <String,NetworkConnection > ();
					localRequestMap.put(connectionName, connection);
					resultConnection = VCHandler(sourceDomain.getURI(), localRequestMap, true);
				}else{
					requestConnection.setConnectionType("InterDomainRequest");
		            ArrayList<ArrayList<OntResource>> path = mapper.findShortestPath(idm,sourceDomain, destDomain, bw,resourceType);
		            
		            if(path==null){
		    			logger.error("No Path found!");
		    			success=false;
		    			break;
		    		}

		    		boolean validSwitching = mapper.toConnection(path,requestConnection);
		    		resultConnection = mapper.deviceConnection;
				}
			}
            
			//LinkedList <Resource> deviceList = new LinkedList <Resource> ();
			//LinkedList <Device> domainList=domainConnection(deviceList,bw); //Return list of domain device with connections interfaces in the action.
    		Device source = resultConnection.getDevice1();
    		if(rs1.getProperty(this.mapper.dependOn)!=null){
				source.setNodeDependency(rs1.getProperty(this.mapper.dependOn).getResource());
			}
    		source.setVMImageURL(this.mapper.getVMImageURL(requestConnection.getEndPoint1()));
    		source.setVMImageGUID(this.mapper.getVMImageGUID(requestConnection.getEndPoint1()));
    		source.setPostBootScript(this.mapper.getPostBootScript(requestConnection.getEndPoint1()));
    		logger.info("Source ="+source.getResource() + " imageURL=" + source.getVMImageURL()+" imageGUID="+source.getVMImageGUID()+" postBootScript="+source.getPostBootScript());
    		
    		//destination
    		source = resultConnection.getDevice2();
    		if(rs2.getProperty(this.mapper.dependOn)!=null){
				source.setNodeDependency(rs2.getProperty(this.mapper.dependOn).getResource());
			}
    		source.setVMImageURL(this.mapper.getVMImageURL(requestConnection.getEndPoint2()));
    		source.setVMImageGUID(this.mapper.getVMImageGUID(requestConnection.getEndPoint2()));
    		source.setPostBootScript(this.mapper.getPostBootScript(requestConnection.getEndPoint2()));
    		logger.info("Destination ="+source.getResource()+" imageURL=" + source.getVMImageURL()+" imageGUID="+source.getVMImageGUID()+" postBootScript="+source.getPostBootScript());
    		
    		domainList = resultConnection.getConnection();

    		if(sourceDomain==destDomain){
    			dependList.addAll(domainList);
    		}else{
    			Device root = domainDepend(domainList,dependList,rs1_ip,rs2_ip);
    		}
			
			domainConnectionList.put(connectionName, domainList);
			
			mapper.removeInConnectionProperty("ndl:inConnection",mapper.inConnection);

			logger.info("Created connection:"+connectionName+" ;size of dependList:"+dependList.size());
		}
		
		if(mapper.getDeviceConnection()!=null)
			mapper.getDeviceConnection().setConnection(dependList);	
		connectionList.put(connectionName, dependList);
		
		//requestModel.close();
		if(success) return connectionList;
		else return null;
	}
    
	//VC request in one cloud site
	public NetworkConnection VCHandler(String domain, Hashtable <String,NetworkConnection > requestMap,boolean isConnection) throws IOException{
		logger.info("VT mapping within one domain:"+domain);
		NetworkConnection connection =null;
		try{
		CloudHandler cloudHandler = new CloudHandler(this.getIdm());
		cloudHandler.setRequestMap(requestMap);
		//FIXME: need the correct substrate model here.
		cloudHandler.addRequest(this.getRequestModel());
		cloudHandler.mapper.domain=domain.split("\\/vm")[0];
		LinkedList <Device>  deviceList=cloudHandler.handleMapping(isConnection);
		connection = new NetworkConnection ();
        connection.setName(cloudHandler.getCurrentRequestURI());
        connection.setConnection(deviceList);
        connection.setBw(cloudHandler.mapper.defaultBandwidth);
        int edge=1;
        for(Device d:deviceList){
			DomainResourceType dType = d.getResourceType();
			
			if(dType.getResourceType().endsWith("VM")){ //two end domains
				logger.debug("Cloud partition:"+d.getUri()+":"+dType.toString()+":"+d.getIPAddress());
				if(edge==1)
					connection.setDevice1(d);
				if(edge==2)
					connection.setDevice2(d);
				edge++;
			}
		}
		
	} catch (Exception e){
        e.printStackTrace();
        return null;
    }
        
		return connection;
	}	
	public Resource toDomain(Resource rs,String resourceType){
		if(rs.getProperty(mapper.inDomain)==null) {
			return null;
		}
		Resource domain = null;
		if(rs.getProperty(mapper.inDomain)!=null)
			domain=rs.getProperty(mapper.inDomain).getResource();
		
		String rType = resourceType.split("\\#")[1]; 	
	
		Resource rDomain=null;
        boolean found = false;		
		for(ResIterator j = idm.listResourcesWithProperty(mapper.RDF_TYPE,mapper.networkDomainOntClass); j.hasNext();) {
			rDomain = j.nextResource();
			//logger.debug("1."+rDomain.getURI()+":"+rDomain.getLocalName()+":"+domain+":"+rType);
			if(rDomain.getURI()==domain.getURI()) {
				found = true;
				break;
			}
			if((rDomain.getLocalName().equalsIgnoreCase(rType)) && (rDomain.getURI().startsWith(domain.getURI()))){
				found = true;
				break;
			}
			if((rDomain.getLocalName().equalsIgnoreCase("Testbed")) && (rDomain.getURI().startsWith(domain.getURI()))){
				found = true;
				break;
			}
		}	
		if(!found) rDomain = null;		
		logger.info("End Point Domain:"+rDomain+"\n");
	
		return rDomain;
	}
	
	@SuppressWarnings("static-access")
	public LinkedList <Device> domainConnection(LinkedList <Resource> deviceList,int bw){
		if(deviceList==null) return null;
		if (deviceList.isEmpty()) return null;
		
		Resource start=deviceList.getFirst();
		Resource end=deviceList.getLast();
		
		NetworkConnection connection = new NetworkConnection ();
		//connection.setConnection(deviceList);
		
		mapper.setDeviceConnection(connection);
	
		//mapper.toConnection(start, end, 1);
		Resource next_Hop;
		start=deviceList.remove(0);
		int size=deviceList.size();
		for(int i=0;i<size;i++){
			next_Hop=deviceList.remove(0);
			mapper.toConnection(start,next_Hop,1,bw);
			//Statement st=start.getProperty(mapper.hasSwitchMatrix);	
			//System.out.println(mapper.hasSwitchMatrix+":"+st);
			start=next_Hop;
		}	
		return connection.getConnection();
	}
	
	//generate request NDL for each domain reservation
	public OntModel domainRequest(Device domain){
		OntModel domainRequestModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntResource up_device_rs=null,down_device_rs=null,connection_rs;
		Resource up_intf_rs,down_intf_rs, sm_rs;
		Statement s,s_type;
		String phrase;
		Resource rs=null,up_rs,down_rs;
    	long intf_bw=0;
    	ResultSet results=null;
    	String varName=null;
    	
		if(domain.getUpNeighbour()!=null){
			up_device_rs=domainRequestModel.createIndividual(domain.getURI()+"/up",mapper.deviceOntClass);
			s=domain.getResource().getProperty(mapper.hasSwitchMatrix);
			if(s!=null){
				sm_rs = s.getResource();
				domainRequestModel.add(up_device_rs,mapper.hasSwitchMatrix,sm_rs);
				domainRequestModel.add(sm_rs.getProperty(mapper.switchingCapability));
			}
			
			up_intf_rs=domain.getUpNeighbour();
			up_rs=domainRequestModel.createIndividual(up_intf_rs.getURI(),mapper.interfaceOntClass);
			up_device_rs.addProperty(mapper.hasInterface,up_rs);  //the upstream domain interface.
			//have to add adaptation layer client interfaces
			rs=up_intf_rs;
			while(true){
				up_rs=domainRequestModel.createIndividual(rs.getURI(),mapper.interfaceOntClass);
				for (StmtIterator j=rs.listProperties();j.hasNext();){
					s_type = (Statement) j.next();
					domainRequestModel.add(up_rs,s_type.getPredicate(),s_type.getObject());
				}
				results=mapper.getLayerAdapatation(idm,rs.getURI());
				//mapper.outputQueryResult(results);
				varName=(String) results.getResultVars().get(0);
				if (results.hasNext()){
					rs=results.nextSolution().getResource(varName);
				}
				else break;
			}	
	
			domainRequestModel.removeAll(up_intf_rs,mapper.interfaceOf,null);
			domainRequestModel.removeAll(up_intf_rs,mapper.connectedTo,null);
			domainRequestModel.add(up_intf_rs, mapper.interfaceOf, up_device_rs);
		}
		
		if(domain.getDownNeighbour()!=null){
			down_device_rs=domainRequestModel.createIndividual(domain.getURI()+"/down",mapper.deviceOntClass);	
			s=domain.getResource().getProperty(mapper.hasSwitchMatrix);
			if(s!=null) {
				sm_rs = s.getResource();
				domainRequestModel.add(down_device_rs,mapper.hasSwitchMatrix,sm_rs);
				domainRequestModel.add(sm_rs.getProperty(mapper.switchingCapability));
			}
			
			down_intf_rs=domain.getDownNeighbour();
			down_rs=domainRequestModel.createIndividual(down_intf_rs.getURI(),mapper.interfaceOntClass);
			down_device_rs.addProperty(mapper.hasInterface, down_rs);  //the downstream domain interface.
			
			rs=down_intf_rs;		
			while(true){
				down_rs=domainRequestModel.createIndividual(rs.getURI(),mapper.interfaceOntClass);
				for (StmtIterator j=rs.listProperties();j.hasNext();){
					s_type = (Statement) j.next();
					domainRequestModel.add(down_rs,s_type.getPredicate(),s_type.getObject());
				}
				results=mapper.getLayerAdapatation(idm,rs.getURI());
				//mapper.outputQueryResult(results);
				varName=(String) results.getResultVars().get(0);
				if (results.hasNext()){
					rs=results.nextSolution().getResource(varName);
				}
				else break;
			}
			
			domainRequestModel.removeAll(down_intf_rs,mapper.interfaceOf,null);
			domainRequestModel.removeAll(down_intf_rs,mapper.connectedTo,null);
			domainRequestModel.add(down_intf_rs, mapper.interfaceOf, down_device_rs);
		}
		
		long bw = 0;
		LinkedList <SwitchingAction> actionList=domain.getActionList();
		if(actionList!=null){
			SwitchingAction action=(SwitchingAction) actionList.element();
			bw = action.getBw();
		}
		
		DomainResourceType rType=domain.getResourceType();
		//Resource reservation = mapper.reservation;
		Resource reservation = domainRequestModel.createIndividual(mapper.getRequestURI(),mapper.reservationOntClass);
		if(reservation!=null){
			//domainRequestModel.add(reservation,mapper.RDF_TYPE,mapper.reservationOntClass);
			connection_rs = domainRequestModel.createIndividual(reservation.getURI()+"/conn",mapper.networkConnectionOntClass);
			if(up_device_rs!=null) connection_rs.addProperty(mapper.hasInterface,up_device_rs);
			if(down_device_rs!=null) connection_rs.addProperty(mapper.hasInterface,down_device_rs);
			if(rType!=null){
				connection_rs.addProperty(mapper.hasResourceType, domainRequestModel.createResource(rType.getResourceType()));
				connection_rs.addProperty(mapper.numResource, String.valueOf(rType.getCount()), XSDDatatype.XSDint);
			}
			connection_rs.addLiteral(mapper.bandwidth, Long.valueOf(bw));
			
			domainRequestModel.add(reservation,mapper.element, connection_rs);
		}
		
		//pass the reservation time to each domain 
		DateFormat df = DateFormat.getDateInstance();
		Date startingTime = null,endingTime=null;
		/*if(reservation!=null){
			if(reservation.getProperty(mapper.startingTime)!=null) 
				startingTime = DatatypeConverter.parseDateTime(reservation.getProperty(mapper.startingTime).getString()).getTime();
			if(reservation.getProperty(mapper.endingTime)!=null) 
				endingTime = DatatypeConverter.parseDateTime(reservation.getProperty(mapper.endingTime).getString()).getTime();
		}*/
		domain.setStartingTime(startingTime);
		domain.setEndingTime(endingTime);
		
		return domainRequestModel;
	}
	
	//If the VLAN is fixed in the interface, then no need for dependency
	public void domainNoDepend(Device domain){
		int action_size=0;
		SwitchingAction action = null;
		if(domain.getActionList()!=null){		//another request??
			action_size=domain.getActionList().size();
			action=(SwitchingAction) domain.getActionList().get(action_size-1);
		}
		Resource intf_rs=null;
		Resource vlan_rs=null;
		boolean depend=false;
		if(action!=null){
			for (Interface intf: action.getSwitchingInterface()){
				vlan_rs=null;
				intf_rs = intf.getResource();
				if(intf_rs.getProperty(mapper.vlan)!=null) vlan_rs=intf_rs.getProperty(mapper.vlan).getResource();
				//System.out.println(intf_rs+":"+vlan_rs+"\n");
				if(vlan_rs==null){
					depend = true;
					break;
				}
			}
		}
		domain.setDepend(depend);
	}
	
	public Device domainDepend(LinkedList <Device> domainList, LinkedList <Device> dependList,Resource rs1_ip,Resource rs2_ip){
		
		Device start, next_Hop,root = null;
		start=domainList.get(0);
		domainNoDepend(start);
		if(!start.isDepend()){
			if(mapper.getDevice(start,dependList)==null){
				dependList.add(start);
			}
		}
		int path_len = domainList.size();
		for(int i=1;i<domainList.size();i++){
			next_Hop=domainList.get(i);
			domainNoDepend(next_Hop);
			if(i==1 & !start.isDepend()){
				dependList.add(next_Hop);
			}
			if(start.isDepend() & next_Hop.isDepend()) {
				start = domainDepend(start,next_Hop,dependList,i,path_len,rs1_ip,rs2_ip);
			}else{
				dependList.add(next_Hop);
			}
			if(start!=null) root=start;	
			start=next_Hop;
		}
		//last domain
		if(!start.isDepend()){
			if(mapper.getDevice(start,dependList)==null){
				dependList.add(start);
			}
		}
		logger.info("End of Dependency Computation! dependList size:"+dependList.size()+" ;domainList size:"+domainList.size());
		return root;
	}
	
	//not covered all combinations yet: (1)label producer;(2)swapping capability;(3) node degree.
	public Device domainDepend(Device start,Device next,LinkedList <Device> dependList,int hop,int path_len,Resource rs1_ip,Resource rs2_ip){
		LabelSet sSet,nSet;
		int sSetSize,nSetSize;
		boolean flag=false;
		Device root=null;
		DomainResourceType sRType=start.getResourceType(),nRType=next.getResourceType();		
		int sRank=0,nRank=0;
		if(sRType!=null) sRank= sRType.getRank();
		if(nRType!=null) nRank= nRType.getRank();
		//System.out.println(start.getName() + ":"+ start.isLabelProducer() +":" + sRType + ":"+ sRank + ":" + start.getSwappingCapability());
		//System.out.println(next.getName() + ":"+ next.isLabelProducer() +":" +nRType + ":"+ nRank + ":" + next.getSwappingCapability()+"\n");
		if(start.isLabelProducer()) {
			if(next.isLabelProducer()){
				if(sRank == nRank){
					if((start.getTunnelingCapability()!=null) || (next.getTunnelingCapability()!=null)){
						
						if(start.getTunnelingCapability()!=null){
							flag=true;
						}else{
							flag=false;
							logger.info("TunnelingCapability:"+start.getTunnelingCapability()+":"+next.getTunnelingCapability()+":"+hop+":"+path_len);
							//find the common label for start and next+1
							if(hop<=path_len-3){
								Device next_next=(Device) this.mapper.deviceConnection.getConnection().get(hop+1);
								sSet = start.getLabelSet(sRType.getResourceType().split("#")[1]);
								nSet = next_next.getLabelSet(nRType.getResourceType().split("#")[1]);
								int commonLabel =  this.mapper.findCommonLabel(sSet,nSet);
								start.setStaticLabel(commonLabel);

								next_next.setStaticLabel(commonLabel);
								logger.info(start.getURI()+":"+next.getURI()+":"+next_next.getURI()+"----Next Next Common Label:"+commonLabel);

							}
						}
						
					}else{
						if(start.getSwappingCapability()!=null){
							if(next.getSwappingCapability()!=null){
								if(start.getDegree()<=next.getDegree()) 
									flag=true;
							}
							else flag=true; 
						} 
						else if(next.getSwappingCapability()!=null) {
							flag=false;
						}else{ //both have no swappingCapability, check who has a specific available label range
							sSet = start.getLabelSet(sRType.getResourceType().split("#")[1]);
							nSet = next.getLabelSet(nRType.getResourceType().split("#")[1]);
							sSetSize=sSet.getLabelRangeSize();
							nSetSize=nSet.getLabelRangeSize();
							logger.info("LabelSet Size:"+start.getName()+":"+sSetSize+":"+sSet.getMinLabel_ID()+"-"+sSet.getMaxLabe_ID()+"****"+next.getName()+":"+nSetSize+":"+nSet.getMinLabel_ID()+"-"+nSet.getMaxLabe_ID());
							if(sSetSize==0){
								if(nSetSize>0){
									start.setStaticLabel(-1);
									flag=true;
								}
								else{ //both == 0, needs to use the common label 
									flag=true;
									start.setStaticLabel(-1);
									next.setStaticLabel(-1);
								}
							}else if(nSetSize==0){
								flag=false;
							}else{ //both >0, needs to find the common label to use
								int commonLabel = this.mapper.findCommonLabel(sSet,nSet);
								logger.info("Found common label:"+commonLabel);
								start.setStaticLabel(commonLabel);
								next.setStaticLabel(commonLabel);
							}
						}
					}
				}
				else if(sRank< nRank){
					flag=false;
				}
				else flag=true;
			}
			else flag=false;
		}
		else if(next.isLabelProducer()){
			flag=true;
		}
		
		Resource nextUpNeighbour=next.getUpNeighbour();
		Resource startDownNeighbour=start.getDownNeighbour();
		Device device = mapper.getDevice(start,dependList);
		if((device!=null) && (device.getResourceType().getResourceType()=="http://geni-orca.renci.org/owl/compute.owl#VM")){
			start=device;
		}
		else{
			if(hop==1)
				dependList.add(start);
		}
		
		device = mapper.getDevice(next,dependList);

		if((device!=null) && (device.getResourceType().getResourceType()=="http://geni-orca.renci.org/owl/compute.owl#VM")){
			next=device;
		}
		else{
			dependList.add(next);
		}
		
		if(flag){//start depends on next
			if((hop==1) && (rs1_ip!=null)){   //First device should be the VM and check if IP is given from the request
				start.setPrecededBy(next,rs1_ip);
				next.setFollowedBy(start,rs1_ip);
			}
			else{
				start.setPrecededBy(next,nextUpNeighbour);
				next.setFollowedBy(start,startDownNeighbour);
			}
			root = next;
		}else{//next depends on start
			if((hop==path_len-1) && (rs2_ip!=null)){   //Last device should be the VM and check if IP is given from the request
				next.setPrecededBy(start,rs2_ip);
				start.setFollowedBy(next,rs2_ip);
			}
			else{
				next.setPrecededBy(start,startDownNeighbour);
				start.setFollowedBy(next,nextUpNeighbour);
			}
		}
		
		logger.info("start:"+start.getURI()+":"+start.getName()+"\n");
		logger.info("next:"+next.getURI()+":"+next.getName()+"\n");
		
		//System.out.println("flag:"+flag+"\n");
		return root;
	}
	
}
