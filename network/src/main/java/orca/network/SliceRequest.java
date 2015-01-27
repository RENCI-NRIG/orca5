
package orca.network;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import orca.shirako.api.IServiceManagerReservation;

import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.OntologyException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.XSD;

import edu.emory.mathcs.backport.java.util.Collections;

import orca.ndl.*;
import orca.ndl.elements.Device;
import orca.ndl.elements.NetworkConnection;

public class SliceRequest extends OntProcessor {

	String requestFileName;
	OntModel requestModel;
	public Resource reservation;
	public String startTime;
	public String endTime;
	public long termDuration;
	public String layer;
	public String requestID;
	public String domain;
	public long defaultBandwidth;

	private String random_reservation_url; 
	
	private int connectionType;
	private int numVMSite;
	private int numNetworkService;
	
	public SliceRequest(String requestFileName, String substrateFileName) throws IOException {
		
		super(substrateFileName);  //this created 'ontModel': the configured substrate ontModel
		addRequest(requestFileName);
	}
	
	public SliceRequest(String substrateFileName) throws IOException {
		super(substrateFileName);  //this created 'ontModel': the configured substrate ontModel
	}
	
	public SliceRequest(){
		super();
	}
	public SliceRequest(OntModel ontModel) {
		super(ontModel);
	}

	public SliceRequest(InputStream stream) throws IOException{
		super(stream);
	}
	
	public Hashtable <String,NetworkConnection> parseRequestSite(OntModel model) throws UnknownHostException, InetNetworkException{
		requestModel=model;
		DomainResourceType rType=getResourceType("http://geni-orca.renci.org/owl/domain.owl#VLAN");
		//this.requestModel.write(System.out);
		String queryPhrase=createQueryStringSite();
        ResultSet results =rdfQuery(this.requestModel,queryPhrase);
		//model.write(System.out);
        //outputQueryResult(results);
        //results = rdfQuery(this.requestModel,queryPhrase);;
        
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);
		String var2=(String) results.getResultVars().get(2);
		String var3=(String) results.getResultVars().get(3);
		String var4=(String) results.getResultVars().get(4);
		String var5=(String) results.getResultVars().get(5);
		String var6=(String) results.getResultVars().get(6);
		String var7=(String) results.getResultVars().get(7);
		String var8=(String) results.getResultVars().get(8);
		String var9=(String) results.getResultVars().get(9);
		String var10=(String) results.getResultVars().get(10);
		String var11=(String) results.getResultVars().get(11);
		
		QuerySolution solution=null;
		Resource rs0=null, rs1 = null,rs2 = null,rs3=null,rs9=null;
		String rs0_str=null,rs1_str=null,rs3_str=null,rs4_str=null,rs5_str=null,rs6_str=null,rs7_str=null,rs8_str=null,rs10_str=null,rs11_str=null;
		String connectionName;
		
		Hashtable <String, NetworkConnection> requestMap = new Hashtable <String, NetworkConnection> ();
		long bw=0;
		int units=0,i=0;
		NetworkConnection requestConnection=null;
		Device cluster=null;
		DomainResourceType resourceType=null;
		logger.info("Start Virtual Cluster Parsing......\n");
		while (results.hasNext()){
			solution=results.nextSolution();
			rs0=solution.getResource(var0);	//connection
			rs1=solution.getResource(var1);	//cluster
			rs2=solution.getResource(var2); //rType
			units=0;
			if(solution.getLiteral(var3)!=null){
				rs3_str=solution.getLiteral(var3).getValue().toString();
				units=Integer.valueOf(rs3_str).intValue();
			}
			if(solution.getLiteral(var4)!=null){
				rs4_str=solution.getLiteral(var4).getValue().toString();
				bw=Long.valueOf(rs4_str).longValue();
			}else{
				bw=this.defaultBandwidth;
			}
				
			
			requestConnection=new NetworkConnection();
			rs1_str=rs1.getURI();
			cluster=new Device();
			cluster.setResource(rs1);
			cluster.setGroup(rs1_str.substring(rs1_str.indexOf("#")+1));
			if(rs0!=null){			
				requestConnection.setResource(rs0);
				connectionName=rs0.getURI();		
				requestConnection.setType(rType.getResourceType());
			}
			else{
				connectionName="None"+String.valueOf(i);
				requestConnection.setConnectionType("None");
			}
			requestConnection.setBw(bw);
			i++;
			requestConnection.setEndPoint1(rs1.getURI());
			
			/*if(units==0){
				rType = requestResourceType(rs1);
				units=rType.getCount();
			}
			else{
				rType=getResourceType(rs2.getURI());
				rType.setCount(units);
			}*/
			
			if(rs2!=null){
				rType=getResourceType(rs2.getURI());
			}else{
				rType = requestResourceType(rs1);
			}
			if(units!=0)
				rType.setCount(units);
			else
				units=rType.getCount();
			
			logger.debug("Edge resource:"+rType.toString());
			
			requestConnection.setResourceCount1(units);
			cluster.setResourceType(rType);
			
			if(solution.getLiteral(var5)!=null){//IP address 1
				rs5_str=solution.getLiteral(var5).getValue().toString();
				InetAddress addr1 = InetAddress.getByName(rs5_str);
				cluster.setIPAddress(rs5_str);
			}
			if(solution.getLiteral(var6)!=null){//IP address 1 netmask
				rs6_str=solution.getLiteral(var6).getValue().toString();
				InetAddress addr1_netmask = InetAddress.getByName(rs6_str);
				InetNetwork addr1_network = new InetNetwork(rs5_str,rs6_str);
				rs5_str= addr1_network.networkIdentifierCIDR();
				cluster.setIPAddress(rs5_str);
				if(rs5_str.indexOf("/")>=0){
					cluster.setIPNetmask(rs5_str.split("/")[1]);
				}
				else{
					cluster.setIPNetmask("24");
				}
			}
			//System.out.println("Request IP Address:"+rs5_str+":"+rs6_str);
			
			requestConnection.setEndPoint1_ip(rs5_str);
			if(rs5_str!=null){
				connectionName=connectionName+"/"+rs5_str;
				requestConnection.setType(rType.getResourceType());
			}
			requestConnection.setName(connectionName);
			
			String vmImageURL,vmImageGUID;
			if(solution.getLiteral(var7)!=null){//VM Image url
				vmImageURL = solution.getLiteral(var7).getValue().toString();
				cluster.setVMImageURL(vmImageURL);
			}
			if(solution.getLiteral(var8)!=null){//VM Image guid
				vmImageGUID = solution.getLiteral(var8).getValue().toString();
				cluster.setVMImageGUID(vmImageGUID);
			}	
			rs9=solution.getResource(var9);  //Master slave dependency
			cluster.setNodeDependency(rs9);
			cluster.setUpNeighbour(rs9);
			
			if(solution.getLiteral(var10)!=null){//postBootScript
				rs10_str = solution.getLiteral(var10).getValue().toString();
				cluster.setPostBootScript(rs10_str);
			}	
			
			if(solution.getResource(var11)!=null){//postBootScript
				rs11_str = solution.getResource(var11).getURI();
				cluster.setPostBootScript(rs10_str);
				requestConnection.setEndPoint1_domain(rs11_str);
				this.domain=rs11_str;
			}	
			
			requestConnection.setDevice1(cluster);
			requestMap.put(connectionName, requestConnection);	
		}	
		
		return requestMap;
	}

	
	//Return a table of <connectionName, <source,destination,bw>>
	public Hashtable <String,NetworkConnection> parseRequest(OntModel model) throws UnknownHostException, InetNetworkException{
		requestModel=model;
		ResultSet results = connectedDevicePair(model);
		//model.write(System.out);
        //outputQueryResult(results);
        //results = connectedDevicePair(model);
        
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);
		String var2=(String) results.getResultVars().get(2);
		String var3=(String) results.getResultVars().get(3);
		String var4=(String) results.getResultVars().get(4);
		String var5=(String) results.getResultVars().get(5);
		String var6=(String) results.getResultVars().get(6);
		String var7=(String) results.getResultVars().get(7);
		String var8=(String) results.getResultVars().get(8);
		String var9=(String) results.getResultVars().get(9);
		String var10=(String) results.getResultVars().get(10);
		String var11=(String) results.getResultVars().get(11);
		
		QuerySolution solution=null;
		Resource rs0=null, rs1 = null,rs2 = null,rs3=null;
		String rs0_str=null,rs1_str=null,rs3_str=null,rs4_str=null,rs5_str=null,rs6_str=null,rs7_str=null,rs8_str=null,rs9_str=null;
		String connectionName;
		
		DomainResourceType resourceType;
		
		LinkedList <NetworkConnection> visitedConnection = new LinkedList <NetworkConnection> ();
		Hashtable <String,NetworkConnection > connectionNodeList = new Hashtable <String,NetworkConnection> ();

		NetworkConnection connection=null;
		logger.info("Start......\n");
		while (results.hasNext()){
			solution=results.nextSolution();
			rs0=solution.getResource(var0);	//source
			rs1=solution.getResource(var1);	//destination
			rs2=solution.getResource(var2); //connection Name 
			rs0_str=rs0.getURI();
			rs1_str=rs1.getURI();
			if(rs2==null) {
				connectionName=rs0_str+"-"+rs1_str;
			}
			else{ 
				connectionName=rs2.getURI();
			}
			
			if(solution.getLiteral(var3)!=null) //bw
				rs3_str=solution.getLiteral(var3).getValue().toString();
			
			if(solution.getLiteral(var4)!=null){//IP address 1
				rs4_str=solution.getLiteral(var4).getValue().toString();
				InetAddress addr1 = InetAddress.getByName(rs4_str);
			}
			if(solution.getLiteral(var5)!=null){//IP address 1 netmask
				rs5_str=solution.getLiteral(var5).getValue().toString();
				InetAddress addr1_netmask = InetAddress.getByName(rs5_str);
				InetNetwork addr1_network = new InetNetwork(rs4_str,rs5_str);
				rs4_str= addr1_network.networkIdentifierCIDR();
			}
					
			if(solution.getLiteral(var6)!=null){//IP address 2
				rs6_str=solution.getLiteral(var6).getValue().toString();
				InetAddress addr2 = InetAddress.getByName(rs6_str);
			}
			if(solution.getLiteral(var7)!=null){//IP address 2 netmask
				rs7_str=solution.getLiteral(var7).getValue().toString();
				InetAddress addr2_netmask = InetAddress.getByName(rs7_str);
				InetNetwork addr2_network = new InetNetwork(rs6_str,rs7_str);
				rs6_str= addr2_network.networkIdentifierCIDR();
			}
	
			
			if(solution.getLiteral(var8)!=null){//host interface 1
				rs8_str=solution.getLiteral(var8).getValue().toString();
				if(rs4_str!=null)
					rs4_str+="@"+rs8_str;
			}
			if(solution.getLiteral(var9)!=null){//host interface 2
				rs9_str=solution.getLiteral(var9).getValue().toString();
				if(rs6_str!=null)
					rs6_str+="@"+rs9_str;
			}
			
			if(existEdge(visitedConnection,rs0_str,rs1_str)) continue;
			
			connection=new NetworkConnection();
			connection.setResource(rs2);
			connection.setName(connectionName);
			connection.setEndPoint1(rs0_str);
			connection.setEndPoint1_ip(rs4_str);
			//check if this end point bounded or not, if yes, add it to the conection.endpoint1-domain
			if(solution.getResource(var10)!=null){//domain 1
				connection.setEndPoint1_domain(solution.getResource(var10).getURI());
			}
			if(solution.getResource(var11)!=null){//domain 2
				connection.setEndPoint2_domain(solution.getResource(var11).getURI());
			}
			connection.setEndPoint2(rs1_str);
			connection.setEndPoint2_ip(rs6_str);
			//check if this end point bounded or not, if yes, add it to the conection.endpoint2-domain
			
			//assume the requested resource type on both end are the same
			resourceType = requestResourceType(rs0);
			connection.setResourceCount1(resourceType.getCount());
			connection.setEndPoint1_type(resourceType.getResourceType());
			
			resourceType = requestResourceType(rs1);
			connection.setResourceCount2(resourceType.getCount());
			connection.setEndPoint2_type(resourceType.getResourceType());
			if(connection.getEndPoint1_type() != connection.getEndPoint2_type()){
				logger.error("Requested resource type are not the same at both ends:"+connection.getEndPoint1_type() +":"+ connection.getEndPoint2_type());
				throw new RuntimeException("Requested resource type are not the same at both ends:"+connection.getEndPoint1_type() +":"+ connection.getEndPoint2_type());
			}
			
			if (rs3_str!=null) connection.setBw(Long.valueOf(rs3_str).longValue());
			
			//default connection type is VLAN
			connection.setType("http://geni-orca.renci.org/owl/domain.owl#VLAN");
			
			
			logger.info("Request connection:"+connectionName+":"+rs0+"("+rs4_str+")-"+rs1 +"("+ rs6_str+"):"+rs3_str+":"+connection.getEndPoint1_type()+":"+connection.getResourceCount1()+"-"+connection.getEndPoint2_type()+":"+connection.getResourceCount2());
			
			connectionNodeList.put(connectionName,connection);
			visitedConnection.add(connection);
		}	
		
		return connectionNodeList;
	}
	

	//return domainResourceType<resourceType, count>
	public DomainResourceType requestResourceType(Resource rs){
		DomainResourceType type = new DomainResourceType();

		if(rs.getProperty(numGigabitEthernetPort)!=null){
			type.setResourceType("http://geni-orca.renci.org/owl/domain.owl#GEPort");
			type.setCount(rs.getProperty(numGigabitEthernetPort).getInt());	
		}else if(rs.getProperty(numTenGigabitEthernetPort)!=null){
			type.setResourceType("http://geni-orca.renci.org/owl/domain.owl#TenGEPort");
			type.setCount(rs.getProperty(numTenGigabitEthernetPort).getInt());	
		}
		else if(rs.getProperty(numCE)!=null){
			type.setResourceType("http://geni-orca.renci.org/owl/compute.owl#VM");
			type.setCount(rs.getProperty(numCE).getInt());	
		}
		else if(rs.getProperty(numVLAN)!=null){
			type.setResourceType("http://geni-orca.renci.org/owl/domain.owl#VLAN");
			type.setCount(1);	
		}else{
			//default type is 1 VM
			type.setResourceType("http://geni-orca.renci.org/owl/compute.owl#VM");
			type.setCount(1);	
		}

		return type;
	}
	
	//add a request model
	public OntModel addRequest(OntModel model) {
		requestModel=model;   
		init();
		return requestModel;   
	}
	
	public OntModel addRequest(InputStream is) {
		//requestModel=ontCreate(is); //this creates 'requestModel' : the request rdf ontModel   
		Model schemaModel=FileManager.get().loadModel("http://geni-orca.renci.org/owl/request.owl");
		schemaModel.add(FileManager.get().loadModel("http://geni-orca.renci.org/owl/compute.owl"));
		
		schemaModel.read(is,"");
		//requestModel.addSubModel(schemaModel,true);
		requestModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,schemaModel);
		//requestModel.read(is, "");
		init();
		return requestModel;
			       
	}
	
	public OntModel addRequest(String requestFile) throws IOException{
		setRequestFileName(requestFile);
		InputStream in = FileManager.get().openNoMap(requestFile);
	    if (in == null) {
	    	throw new IllegalArgumentException("File: " + requestFile + " not found");
	    }
	    requestModel=addRequest(in);
		
		return requestModel;
	}
	
	public void init(){
		getRequest();
		
		//short form term representation
		if(startTime==null)
			startTime=setRequest("request:startingTime",true);
		if(endTime==null)
			endTime=setRequest("request:endingTime",true);
		
		if(reservation!=null){
			if(reservation.getProperty(this.hasURN)!=null){
				requestID = reservation.getProperty(this.hasURN).getLiteral().getString();
			}
			if(reservation.getProperty(this.inDomain)!=null){
				domain = reservation.getProperty(this.inDomain).getResource().getURI();
			}
			if(reservation.getProperty(this.atLayer)!=null){
				layer= reservation.getProperty(this.atLayer).getResource().getURI();
			}
			if(reservation.getProperty(this.bandwidth)!=null){
				defaultBandwidth=reservation.getProperty(this.bandwidth).getLong();
			}
			
			logger.info(reservation+":"+domain+":"+layer+":Term:"+startTime+":"+endTime+":"+termDuration+":default bandwidth:"+defaultBandwidth);
		}
	}
	
	//set parameters
	public String setRequest(String p,boolean dataProperty){
		String ob=null;
		
		String queryPhrase=createQueryStringObject(null,p);
		ResultSet results=rdfQuery(requestModel,queryPhrase);
		String varName=(String) results.getResultVars().get(0);
		if(results.hasNext()){
			if(dataProperty)
				ob=results.nextSolution().getLiteral(varName).toString();
			else ob=results.nextSolution().getResource(varName).toString();
		}
		//System.out.println(queryPhrase+":"+ob);
		
		return ob;
	}

	//get the urn of a reservation
	public Resource getRequest(){
		String queryPhrase=createQueryStringReservationTerm();
		ResultSet results=rdfQuery(requestModel,queryPhrase);
		//outputQueryResult(results);
		//results=rdfQuery(requestModel,queryPhrase);
		
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);
		String var2=(String) results.getResultVars().get(2);
		String var3=(String) results.getResultVars().get(3);
		String var4=(String) results.getResultVars().get(4);
		String var5=(String) results.getResultVars().get(5);
		String var6=(String) results.getResultVars().get(6);
		String var7=(String) results.getResultVars().get(7);
		String var8=(String) results.getResultVars().get(8);
		String var9=(String) results.getResultVars().get(9);
		String var10=(String) results.getResultVars().get(10);
		
		QuerySolution solution=null;
		float years=0,months=0,weeks=0,days=0,hours=0,minutes=0,seconds=0;
		while (results.hasNext()){
			solution=results.nextSolution();
			reservation=solution.getResource(var0);
			if(solution.getLiteral(var2)!=null){
				startTime=solution.getLiteral(var2).getString();
			}
			if(solution.getLiteral(var3)!=null){
				endTime=solution.getLiteral(var3).getString();
			}

			if(solution.getLiteral(var4)!=null)
				years=solution.getLiteral(var4).getFloat();
			if(solution.getLiteral(var5)!=null)
				months=solution.getLiteral(var5).getFloat();
			if(solution.getLiteral(var6)!=null)
				weeks=solution.getLiteral(var6).getFloat();
			if(solution.getLiteral(var7)!=null)
				days=solution.getLiteral(var7).getFloat();
			if(solution.getLiteral(var8)!=null)
				hours=solution.getLiteral(var8).getFloat();
			if(solution.getLiteral(var9)!=null)
				minutes=solution.getLiteral(var9).getFloat();
			if(solution.getLiteral(var10)!=null)
				seconds=solution.getLiteral(var10).getFloat();
			
			termDuration = (long) ((((years*365+months*30+weeks*7+days)*24+hours)*60+minutes)*60+seconds);
		}
		if(reservation==null){
			reservation=requestModel.createResource("http://geni-orca.renci.org/owl/"+UUID.randomUUID().toString(), (Resource) requestModel.createOntResource(NdlCommons.ORCA_NS+"request.owl#Reservation"));
		}
		
		random_reservation_url=reservation.getURI()+"/"+UUID.randomUUID().toString();
		
		return reservation;	
	}
	
	public String getRequestURI(){
		return random_reservation_url;
	}
	public String getVMImageURL(String rs_str){
		return getVMImageURL(requestModel.getResource(rs_str));
	}
	public String getVMImageURL(Resource rs){
		String vmImageURL=null;
		Resource vmImage=null;

		if(rs!=null){
			//ObjectProperty hasVMImage = requestModel.createObjectProperty(NdlCommons.ORCA_NS+"compute.owl#diskImage");
	        if(rs.hasProperty(diskImage))
	        	vmImage=rs.getRequiredProperty(diskImage).getResource();
	        if(vmImage!=null){
	        	//DatatypeProperty hasURL=requestModel.createDatatypeProperty(NdlCommons.ORCA_NS+"topology.owl#hasURL");
	        	
	        	if(vmImage.hasProperty(hasURL))
	        		vmImageURL=vmImage.getRequiredProperty(hasURL).getString();
	        	
	        }
		}
		return vmImageURL;
	}
	
	public String getVMImageGUID(String rs_str){
		
		return getVMImageGUID(requestModel.getResource(rs_str));
		
	}
	public String getVMImageGUID(Resource rs){
		String vmImageGUID=null;
		Resource vmImage=null;

		if(rs!=null){
			//ObjectProperty hasVMImage = requestModel.createObjectProperty(NdlCommons.ORCA_NS+"compute.owl#diskImage");
	        if(rs.hasProperty(diskImage))
	        	vmImage=rs.getRequiredProperty(diskImage).getResource();
	        if(vmImage!=null){
	        	//DatatypeProperty hasGUID=requestModel.createDatatypeProperty(NdlCommons.ORCA_NS+"topology.owl#hasGUID");
	        	
	        	if(vmImage.hasProperty(hasGUID))
	        		vmImageGUID=vmImage.getRequiredProperty(hasGUID).getString();
	        }
		}
		return vmImageGUID;
	}
	public String getPostBootScript(String rs_str){
		return getPostBootScript(requestModel.getResource(rs_str));
	}
	
	//identify weather a node (url) is the source or destination 
	public int sdDevice(OntModel model,String url){
		int gress=0;
		String queryPhrase=createQueryStringSubject("ndl:connectedTo",url);

        ResultSet results=rdfQuery(model,queryPhrase);
        
        if(!results.hasNext()){
        	gress=1;  //source: nobody connectedTo
        }
        else{
        	queryPhrase=createQueryStringObject(url,"ndl:connectedTo");	
        	results=rdfQuery(model,queryPhrase);
        	if(!results.hasNext()) gress=-1;  //destination: connectedTo nobody
        }	
        return gress;
	}
	
	//Check if all the devices in the request are valid devices in the substrate model
	
	public boolean isDeviceValid(){
		boolean valid=true;
		Resource resource=null;
		
        String queryPhrase=createQueryStringType("ndl:Device");
		
        ResultSet results = rdfQuery(requestModel,queryPhrase);
        if (!results.hasNext()) {
        	queryPhrase=createQueryStringType("compute:Server");
        	results = rdfQuery(requestModel,queryPhrase);
        	if (!results.hasNext())
        		throw new IllegalArgumentException( "No device or server in: " + requestFileName + "\n");
        }
        //outputQueryResult(results);
        String varName=(String) results.getResultVars().get(0);

        QuerySolution solution;
        
		while (results.hasNext()){
			solution=results.nextSolution();
			resource=solution.getResource(varName);

			if(!existResource(resource,ontModel,queryPhrase)) {
				logger.error(resource+" is a unanmed device\n");
				valid=false;
				//setOfDevices.close();
				break;
			}
			else{
				logger.debug(resource+" valid.\n");
			}
		}
		return valid;
	}
	
	// Check if the Reqeust is valid in term of connectivity in the substrate
	public boolean isRequestValid(){
		boolean valid=true;
		ResultSet results = connectedDevicePair(requestModel);
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);
		QuerySolution solution=null;
		String rs1,rs2, queryPhrase;
		int connected=-1;
		
		//outputQueryResult(results);
		
		while (results.hasNext()){
			solution=results.nextSolution();
			rs1=solution.getResource(var0).getURI();
			rs2=solution.getResource(var1).getURI();
			connected=deviceConnected(rs1,rs2);
			if(connected==-1){ 
				valid = false;
				//System.out.println(rs1+" is not connected to "+rs2 + " in "+inputFileName);
				break;
			}
			if(connected==0){
				logger.debug(rs1+" is connected to "+rs2 + " in "+inputFileName);
				//outputQueryResult(getSubGraphLinkTo(rs1, rs2));
				connected=-1;
				continue;
			}
				
		}
		//ontModel.write(System.out);
		return valid;
	}
	
	//Decide if two device are connected in the substrate
	public int deviceConnected(String url1, String url2) throws OntologyException{
		int connected=-1;
		
		String subPath=" ('([ndl:hasInterface]+/([ndl:hasInputInterface]|[ndl:hasOutputInterface])*/([ndl:linkTo]|[ndl:connectedTo])+/[ndl:interfaceOf]+)+'";
		
		ResultSet results=listConnectedDevice(url1,ontModel, subPath);
		ResultSet subGraphResults;
		//outputQueryResult(results);
		
		Resource rs2 = ontModel.getResource(url2);
		Resource rs1=ontModel.getResource(url1);
		
		//System.out.println(url1+";"+url2);
		
		if(existResource(rs2,results)){
			connected=0;  //directly connected/linked
			subGraphResults=getSubGraphLinkTo(url1, url2);
			
			//outputQueryResult(subGraphResults);
		}	
		return connected;
	}
	
	//Decide if two device are connected in the substrate
	public int deviceConnectedDetail(String url1, String url2) throws OntologyException{
		int connected=-1;
				
		ResultSet results=listConnectedDevice(url1,ontModel);
		ResultSet subGraphResults;
		//outputQueryResult(results);
		
		Resource rs2 = ontModel.getResource(url2);
		Resource rs1=ontModel.getResource(url1);
		
		logger.debug(url1+";"+url2);
		DatatypeProperty visited = ontModel.createDatatypeProperty("http://geni-orca.renci.org/owl/topology.owl#visited");
		visited.addRange(XSD.xboolean);
		rs1.addProperty(visited, "true",XSDDatatype.XSDboolean); //starting point
		
		if(existResource(rs2,results)){
			connected=0;  //directly connected/linked
			subGraphResults=getSubGraphLinkTo(url1, url2);
			
			outputQueryResult(subGraphResults);
			return connected;
		}
		//results.reset();
		String var0=(String) results.getResultVars().get(0);
		String rsURI;
		Resource rs;
		Statement v;
		
		if(connected==-1){	//looking for indirectly connected? 
			//System.out.println(url1+" is not connected to "+url2 + " in "+inputFileName);
			results=listConnectedDevice(url1,ontModel);
			//outputQueryResult(results);
			//results=listConnectedDevice(url1,ontModel,onPath, subPath,true);
			while (results.hasNext()){
				rs=results.nextSolution().getResource(var0);
				rsURI=rs.getURI();
				//System.out.println(rsURI+";"+url1);
				
				v=rs.getProperty(visited);
				if(v!=null){
					//System.out.println(rs.getProperty(visited).getObject().toString());
					if(v.getBoolean()){
						//System.out.println("This device was visited already.");
						continue;
					}
				}	
				if(deviceConnected(rsURI,url2)!=-1){
					subGraphResults=getSubGraphLinkTo(url1,rsURI);					
					outputQueryResult(subGraphResults);
					connected=1;
					break;
				}
			}
		}
		
		return connected;
	}
	
	//List all interfaces of a device
	public ResultSet listDeviceInterface(String deviceURL, OntModel model){
		ResultSet results=null;
		String queryPhrase=createQueryStringObject(deviceURL, "ndl:hasInterface");
		logger.debug(queryPhrase);
		results = rdfQuery(model,queryPhrase);
		
		return results;
	}
	
	public OntModel createOntModel(NetworkConnection deviceList){
		OntModel model=ModelFactory.createOntologyModel();
		return model;
	}
	
	
	public OntModel getRequestOntModel(){
		return requestModel;
	}

	
	public Hashtable getSetOfNetworkService(){
		return getSetOfNetworkService();
	}

	public int getConnectionType(){
		return connectionType;
	}
	public void setConnectionType(int type){
		 connectionType=type;
	}
	public void setRequestFileName(String name){
		requestFileName=name;
	}

	public String getAtLayer() {
		return layer;
	}

	public void setAtLayer(String layer) {
		this.layer = layer;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public OntModel getRequestModel() {
		return requestModel;
	}

	public void setRequestModel(OntModel requestModel) {
		this.requestModel = requestModel;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getRequestFileName() {
		return requestFileName;
	}
}
