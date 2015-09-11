package orca.ndl;

import java.util.UUID;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.Individual;

/**
 * Test cases for modify
 * @author ibaldin
 *
 */
public class NdlGeneratorModifyTest extends TestCase {
	NdlGenerator ngen = new NdlGenerator(UUID.randomUUID().toString(), Logger.getLogger(NdlGeneratorModifyTest.class), true);
	
	public NdlGeneratorModifyTest(String a) {
		super(a);
	}
	
	public void testModifyModifyNode() throws NdlException {
		System.out.println("--------\nTesting adding modify node");
		Individual res = ngen.declareModifyReservation("test-modify-add-node");
		
		Individual el = ngen.declareModifiedComputeElement("modifiedCE", UUID.randomUUID().toString());
		
		ngen.declareModifyElementAddElement(res, el);
	}
	
	public void testModifyAddNodeAndLink() throws NdlException {
		System.out.println("--------\nTesting adding new nodes/links");
		
		Individual node = ngen.declareComputeElement("MyNewNode");
		ngen.addGuid(node, UUID.randomUUID().toString());
		
		Individual link = ngen.declareNetworkConnection("MyNewLink");
		ngen.addGuid(link, UUID.randomUUID().toString());
		
		Individual ifc = ngen.declareInterface("NewLinkNodeInterface");
		ngen.addIPToIndividual("1.1.1.1", ifc);
		
		ngen.addInterfaceToIndividual(ifc, node);
		ngen.addInterfaceToIndividual(ifc, link);
		
		Individual ifc1 = ngen.declareInterface("NewLinkOldNodeInterface");
		ngen.addIPToIndividual("2.2.2.2", ifc1);
		
		Individual link1 = ngen.declareNetworkConnection("MyOtherNewLink");
		ngen.addGuid(link1, UUID.randomUUID().toString());
		
		Individual mNode = ngen.declareModifiedComputeElement("OldNode", UUID.randomUUID().toString());
		ngen.addInterfaceToIndividual(ifc1, mNode);
		ngen.addInterfaceToIndividual(ifc1, link1);
		
		Individual res = ngen.declareModifyReservation("addCEAndLink");
		ngen.declareModifyElementAddElement(res, node);
		ngen.declareModifyElementAddElement(res, link);
		ngen.declareModifyElementAddElement(res, link1);
		ngen.declareModifyElementModifyNode(res, mNode);
	}
	

	public void testModifyNG() throws NdlException {
		
	}
	
	@Override
	public void tearDown() {
		System.out.println(ngen.toN3String());
		ngen.done();
	}
}
