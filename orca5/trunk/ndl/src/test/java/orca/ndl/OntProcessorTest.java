package orca.ndl;

import java.util.ArrayList;
import java.util.LinkedList;

import junit.framework.TestCase;
import orca.ndl.OntProcessor;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class OntProcessorTest extends TestCase{

	String inputFileName;
	OntProcessor ontProcessor;
	
	public OntProcessorTest(String arg0) {
		super(arg0);
		
	}

	protected void setUp() throws Exception {
		super.setUp();
		inputFileName="orca/ndl/substrate/ben-6509.rdf";
		ontProcessor=new OntProcessor(inputFileName);
		
	}
	
	public void testListProperty(){
		System.out.println("--------ListProperty-------\n");
		String uri="http://geni-orca.renci.org/owl/ben.rdf#Renci/Cisco/6509";
		Resource rs=ontProcessor.getOntModel().getResource(uri);
		StmtIterator stit=rs.listProperties(ontProcessor.hasSwitchMatrix);
		while(stit!=null && stit.hasNext()){
			Statement st=stit.nextStatement();
			System.out.println(st);
		}
	}
	
	@SuppressWarnings("static-access")
	public void testGetLayer(){
		System.out.println("--------GetLayer-------\n");
		String uri="http://geni-orca.renci.org/owl/ben-dtn.rdf#Duke/Infinera/DTN/fB/1/ocgB/1";
		System.out.println(ontProcessor.getOntModel().getResource(uri).getLocalName());
		ResultSet results= ontProcessor.getLayer(ontProcessor.getOntModel(),uri);
		String layerName=null;
		String varName=(String) results.getResultVars().get(0);
		while (results.hasNext()){
			layerName=results.nextSolution().getResource(varName).getLocalName();
			System.out.println(layerName);

		}
		
		assertTrue(layerName!=null);
	}
	
	@SuppressWarnings("static-access")
	public void testConnectedTo(){
		System.out.println("--------ConnectedTo-------\n");
			String url1="http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/1/fiber";
			String selectStr = "SELECT ?intf ";
	        String fromStr="";
	        String whereStr=
	        	"WHERE {" +      	
	        	"<"+url1+">" + " ndl:linkTo ?intf. "+
	        	"      }";
			
			OntModel ont1=ontProcessor.getOntModel();
			
			String queryPhrase=ontProcessor.createQueryString(selectStr, fromStr, whereStr);
			
			System.out.println(queryPhrase);
			
			ontProcessor.outputQueryResult(ontProcessor.rdfQuery(ont1,queryPhrase));
	}
	
	@SuppressWarnings("static-access")
	public void testListResourceInModel() {
		System.out.println("--------ListResourceInModel-------\n");
		String device="http://geni-orca.renci.org/owl/ben.rdf#Renci/Cisco/6509";
        String selectStr = "SELECT ?resource ";
        String fromStr="";
        String whereStr=
        	"WHERE {" +
        	"?resource a ndl:Device"+
        	"      }";
		
		OntModel ont1=ontProcessor.getOntModel();
		
		String queryPhrase=ontProcessor.createQueryString(selectStr, fromStr, whereStr);
		
		ontProcessor.outputQueryResult(ontProcessor.rdfQuery(ont1,queryPhrase));
		System.out.println(ontProcessor.numResource(ont1,queryPhrase));
		
		assertTrue(ontProcessor.existResourceStr(device,ont1,queryPhrase));
	}
	
	public void testGetConnectionSubGraphSwitchedTo(){
		System.out.println("--------GetConnectionSubGraphSwitchedTo-------\n");
		String url1="http://geni-orca.renci.org/owl/ben-6509.rdf#Duke/Cisco/6509/TenGigabitEthernet/2/1/fiber";	
		
        String selectStr = "SELECT ?intf ?p ?a ?intf_peer ?b ";
        String fromStr="";
        String whereStr=
        	"WHERE {" +
        	"?p a layer:AdaptationProperty. "+
        	"<"+url1+">" + " ndl:linkTo ?intf. "+
        	"?intf ?p ?a."+
        	"?inf_peer layer:switchedTo ?b "+
        	"      }";
		
		OntModel ont1=ontProcessor.getOntModel();
		
		String queryPhrase=OntProcessor.createQueryString(selectStr, fromStr, whereStr);
		
		ontProcessor.outputQueryResult(ontProcessor.rdfQuery(ont1,queryPhrase));

		url1="http://geni-orca.renci.org/owl/ben.rdf#Renci/Polatis";
		String url2="http://geni-orca.renci.org/owl/ben.rdf#Duke/Polatis";
		String s ="SELECT ?a ?b ?c ";
	    String f="";
	    String w=
	        	"WHERE {" +
	        	"?p a layer:AdaptationProperty."+
	        	"(<"+url1+"> '[ndl:hasInterface]+/[ndl:linkTo]+/["+"?p"+"]*/[layer:switchedTo]*/[ndl:interfaceOf]+)+' <"+url2+">) gleen:Subgraph (?a ?b ?c)"+
	        	"      }";
	    queryPhrase=ontProcessor.createQueryString(s,f, w);
	    ontProcessor.outputQueryResult(ontProcessor.rdfQuery(ont1,queryPhrase));
	    
	}
	
	public void testListConnectedDevice(){
		System.out.println("-------Test listConnectedDevice--------");
		OntModel model=ontProcessor.getOntModel();
		String url1="http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/7/fiber";
		String url2="http://geni-orca.renci.org/owl/ben-6509.rdf#UNC/Cisco/6509/TenGigabitEthernet/3/7/fiber";
		String subPath=" ('[ndl:linkTo]+'";
		String queryPhrase=ontProcessor.createQueryStringOnPath(url1, subPath);
		System.out.println(queryPhrase);
		ResultSet results = ontProcessor.rdfQuery(model,queryPhrase);
		ontProcessor.outputQueryResult(results);
	}
	
	public void testGetSwitchedToAdaptation(){
		System.out.println("-------Test getSwitchedToAdaptation--------");
		OntModel model=ontProcessor.getOntModel();
		String url1="http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509";
		ResultSet results=ontProcessor.getSwitchedToAdaptation(url1);
		ontProcessor.outputQueryResult(results);
	}
	
	public void testFindShortestPath(){
		System.out.println("-------Test findShortestPath--------");
		OntModel model=ontProcessor.getOntModel();
		String url1="http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/7/fiber";
		String url2="http://geni-orca.renci.org/owl/ben-6509.rdf#UNC/Cisco/6509/TenGigabitEthernet/3/7/fiber";
		ArrayList<ArrayList<OntResource>> path = ontProcessor.findShortestPath(model,model.getOntResource(url1), model.getOntResource(url2),100000, "VM","VM",null);
		ArrayList<OntResource> hop = null;
		if(path==null){
			System.out.println("No Path");
		}
		else{
			LinkedList <OntResource> deviceList=ontProcessor.getDeviceListInPath(path);
			for(int i=0;i<deviceList.size();i++){
				System.out.println(deviceList.get(i));
			}
			System.out.println("\n");
			for(int i=0;i<path.size();i++){
				hop = path.get(i);
				
	    		System.out.println(hop.get(0)+":"+hop.get(1));
				
				System.out.println("\n");
			}
			
		}
	}
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
