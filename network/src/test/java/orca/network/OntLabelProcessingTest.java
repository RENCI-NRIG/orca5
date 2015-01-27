package orca.network;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;

import junit.framework.TestCase;

public class OntLabelProcessingTest extends TestCase {
	String inputFileName,substrateFileName;

	RequestMapping map;
	
	public OntLabelProcessingTest(){
		super();
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		inputFileName="orca/network/ben-dell.rdf";
		substrateFileName="orca/network/ben-dell.rdf";

		map=new RequestMapping(inputFileName,substrateFileName);
	}

	public void testChangeAvailableLabelSetLowerBound(){
		String uri="http://geni-orca.renci.org/owl/ben-6509.rdf#Duke/Cisco/6509/TenGigabitEthernet/2/1/ethernet";
		String lb="http://geni-orca.renci.org/owl/ben-6509.rdf#Duke/Cisco/6509/VLANLabel/100";
		String ub="http://geni-orca.renci.org/owl/ben-6509.rdf#Duke/Cisco/6509/VLANLabel/200";
		Resource lb_rs=map.ontModel.createResource(lb);
		Resource ub_rs=map.ontModel.createResource(ub);
		OntResource rs=map.ontModel.getOntResource(uri);
		String lower="101";
		float lowerBound=101;
		float upperBound=200;
		String test=uri.replaceAll("\\d+$", lower);
		System.out.println(test);
		//map.ontLabelUpdate(new Interface(map,rs,true),lowerBound,upperBound,lb_rs,ub_rs);
		assertTrue(test!=null);
	}
}
