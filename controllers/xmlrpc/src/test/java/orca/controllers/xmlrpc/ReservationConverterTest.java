package orca.controllers.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;

import orca.embed.RequestWorkflowTest;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestMappingException;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.elements.NetworkElement;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

public class ReservationConverterTest extends RequestWorkflowTest{
	
	String manifestRecoverFile = "/Users/yxin/ORCA/controller-recovery/embed/src/test/resources/orca/embed/ng-1-manifest.rdf"; 
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		requestFileGush = "/Users/yxin/ORCA/controller-recovery/embed/src/test/resources/orca/embed/SFDemo_RLS.rdf"; 	
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRecover() throws NdlException{
		String manifestStr = NdlCommons.readFile(manifestRecoverFile);		
		ByteArrayInputStream modelStream = new ByteArrayInputStream(manifestStr.getBytes());		
		OntModel manifestModel = NdlModel.getModelFromStream(modelStream, OntModelSpec.OWL_MEM_RDFS_INF, true);
		

		ReservationConverter orc = new ReservationConverter();
		workflow.recover(Logger.getLogger(this.getClass()), null, null, manifestModel);
		orc.recover(workflow);
		
		ReservationElementCollection ec = orc.recoverElementCollection(workflow.getBoundElements());
	}
	
	@Test
	public void testRun() throws IOException, RequestMappingException, NdlException {
		String reqStr = NdlCommons.readFile(requestFileGush);
		getAbstractModels();
		DomainResourcePools drp = new DomainResourcePools(); 
		drp.getDomainResourcePools(pools);
		
		workflow.run(drp, abstractModels, reqStr, null,null, "slice-id");
		
		System.out.println(workflow.getErrorMsg());
		
		LinkedList<NetworkElement> connection = (LinkedList<NetworkElement>) workflow.getBoundElements();
		
		print(connection);
		
		
		
		//fail("Not yet implemented");
	}
	
	

}
