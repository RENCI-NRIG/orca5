package orca.controllers.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import orca.embed.RequestWorkflowTest;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestMappingException;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.elements.NetworkElement;

import orca.ndl.elements.OrcaReservationTerm;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

public class ReservationConverterTest extends RequestWorkflowTest{
	
	//String manifestRecoverFile = "/Users/yxin/ORCA/controller-recovery/embed/src/test/resources/orca/embed/ng-1-manifest.rdf";
	String manifestRecoverFile = "../../ndl/src/test/resources/shared-vlan.rdf";
	
	@Before
	public void setUp() throws Exception {
		ORCA_SRC_HOME = "../../"; //calling tests outside this package
		super.setUp();
		//requestFileGush = "/Users/yxin/ORCA/controller-recovery/embed/src/test/resources/orca/embed/SFDemo_RLS.rdf";
		requestFileGush = "../../embed/src/test/resources/orca/embed/TS2/TS2-8.rdf";
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

	/**
	 * Set the lease term to the maximum allowable. Should succeed without changes.
	 *
	 */
	public void testSetLeaseTermValid() {
		ReservationConverter orc = new ReservationConverter();
		OrcaReservationTerm term = new OrcaReservationTerm();
		term.setStart(new Date());
		term.setDuration(14, 0, 0, 0);

		orc.setLeaseTerm(term);

		assertTrue("Valid reservation term should not have been modified", term.getEnd() == orc.leaseEnd);
		System.out.println("Term end date set to: " + orc.leaseEnd);
	}

	/**
	 * Attempt to set the lease term to greater than the maximum allowable.
	 * The lease term should be updated without errors, but only up to the maximum allowable.
	 *
	 */
	public void testSetLeaseTermInvalid() {
		ReservationConverter orc = new ReservationConverter();
		OrcaReservationTerm term = new OrcaReservationTerm();
		term.setStart(new Date());
		term.setDuration(30, 0, 0, 0);

		orc.setLeaseTerm(term);

		assertFalse("Invalid reservation term not updated", term.getEnd() == orc.leaseEnd);
		System.out.println("Term end date updated to: " + orc.leaseEnd);
	}

}
