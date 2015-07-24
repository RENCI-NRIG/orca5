package orca.network;

import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map.Entry;

import junit.framework.TestCase;
import net.jwhoisserver.utils.InetNetworkException;
import orca.ndl.elements.NetworkConnection;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;

public class SliceRequestTest extends TestCase {

	String requestFileName, substrateFileName;
	SliceRequest request;
	
	public SliceRequestTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		requestFileName="orca/network/id-mp-Request2.rdf";
		substrateFileName="orca/network/ben-dell.rdf";
		request= new SliceRequest(requestFileName,substrateFileName);
	}
	
	public void testParseRequest() throws UnknownHostException, InetNetworkException{
		System.out.println("------------test parseRequest:--------------");
		Hashtable <String,NetworkConnection > connectionNodeList = request.parseRequest(request.getRequestModel());
		String rs1_str = null,rs2_str = null,rs4_str=null,resourceType;
		long bw=0;
		String connectionName;
        
		if(connectionNodeList.isEmpty()) assertTrue(false);
				
		for(Entry <String,NetworkConnection> entry:connectionNodeList.entrySet()){
			connectionName=entry.getKey();
			rs1_str=entry.getValue().getEndPoint1();
			rs2_str=entry.getValue().getEndPoint2();
			bw=entry.getValue().getBw();
			resourceType= entry.getValue().getType();
			System.out.println(rs1_str+":"+rs2_str+":"+bw+":"+resourceType+"\n");
		}
	}
	
	public void testSetReservation(){
		System.out.println("------------test SetReservation:--------------");
		System.out.println(request.requestID+"\n"+request.startTime+"\n"+request.endTime+"\n"+request.layer);
		assertTrue(true);
	}
	
	public void testGetReservation(){
		System.out.println("------------test getReservation:--------------");
		System.out.println(request.getRequest());
		assertTrue(true);
	}
	
	public void testIsDeviceValid(){
		System.out.println("------------test isDeviceValid:--------------");
		request.isDeviceValid();
	}
	
	public void testListConnectedDevice(){
		System.out.println("------------test listConnectedDevice:--------------");
		String deviceURL="http://geni-orca.renci.org/owl/ben.rdf#UNC/Infinera/DTN";
		OntModel ontModel=request.getOntModel();
		//requestOntModel.write(new PrintWriter(System.out));
	    //System.out.println();
		ResultSet results = request.listConnectedDevice(deviceURL, ontModel);
		request.outputQueryResult(results);
		assertTrue(results!=null);
	}
	
	public  void testListConnectedLinkedDevice(){
		System.out.println("------------test listConnectedLinkedDevice:--------------");		
		String deviceURL="http://geni-orca.renci.org/owl/ben.rdf#UNC/Cisco/6509";

		//String subPath=" ('[ndl:hasInterface]+/([wdm:WDM]|[wdm:TenGbase-R])+/([ndl:linkTo]|[ndl:connectedTo])+/[ndl:interfaceOf]+'";
		
		String subPath=" ('[ndl:hasInterface]+/([ndl:linkTo]|[ndl:connectedTo])+/[ndl:interfaceOf]+'";
		ResultSet results = request.listConnectedDevice(deviceURL, request.getOntModel(),subPath);
		request.outputQueryResult(results);
		assertTrue(results!=null);
	}
	
	public void testSubGraph(){
		System.out.println("------------test subGraph:--------------");
		String url1="http://geni-orca.renci.org/owl/ben.rdf#Renci/Polatis";
		String url2="http://geni-orca.renci.org/owl/ben.rdf#Renci/Infinera/DTN";
		
		request.outputQueryResult(request.getSubGraphLinkTo(url1, url2));
	}
	
	public void testDeviceConnected(){
		System.out.println("------------test deviceConnected:--------------");
		String d1="http://geni-orca.renci.org/owl/ben.rdf#Duke/Polatis";
		String d2="http://geni-orca.renci.org/owl/ben.rdf#Renci/Polatis";
		
		int c=request.deviceConnected(d2,d1);
		System.out.println(c);
		assertTrue(c!=-1);
	}
	
	public void testListDeviceInterface(){
		System.out.println("------------test listDeviceInterface:--------------");
		String deviceURL="http://geni-orca.renci.org/owl/ben.rdf#Renci/Cisco/6509";
		ResultSet results = request.listDeviceInterface(deviceURL, request.getOntModel());
		request.outputQueryResult(results);
		assertTrue(results!=null);
	}
	
	public void testConnectedDevicePair(){
		System.out.println("------------test connectedevicePair:--------------");
		ResultSet results=request.connectedDevicePair(request.getRequestOntModel());
		request.outputQueryResult(results);
		assertTrue(results!=null);
	}

}
