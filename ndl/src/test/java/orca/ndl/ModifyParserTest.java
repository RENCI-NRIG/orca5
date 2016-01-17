package orca.ndl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

public class ModifyParserTest implements INdlModifyModelListener {

	public void ndlModifyElement(Resource i, Resource subject, ModifyType t,
			OntModel m) {
		System.out.println("See modify element " + i + " with subject " + subject + " of type " + t); 
	}

	public void ndlModifyElement(Resource i, Resource subject, ModifyType t,
			Resource object, int modifyUnit, OntModel m) {
		System.out.println("See modify element " + i + " with subject " + subject);
		
	}
	
	public void ndlModifyReservation(Resource i, Literal name, OntModel m) {
		System.out.println("See modify " + i + " by name " + name);
	}

	public void ndlParseComplete() {
		System.out.println("DONE");
		
	}
	
	public void runTest(String reqName) throws NdlException  {
		InputStream is = this.getClass().getResourceAsStream(reqName);
		assert(is != null);
		
		String r = new Scanner(is).useDelimiter("\\A").next();

		NdlModifyParser p = new NdlModifyParser(r, this);
		System.out.println("Processing " + p);
		p.processModifyRequest();

		System.out.println("Rewriting");
		List<Resource> rr = p.rewriteModifyRequest();

		System.out.println("Rewrote ");
		for(Resource i: rr ) {
			System.out.println(" " + i);
		}

		System.out.println("Processing again");
		p.processModifyRequest();

	}
	
	private static String[] validRequests={ "/modify-request.rdf", "/modify-test-request-valid.rdf", "/removeGlobalMP.rdf" };
	private static String[] invalidRequests={ "/ub-storage.rdf", "/modifyGlobalMP.rdf" };
	
	@Test
	public void run() throws Exception, NdlException, IOException {
		System.out.println("Running valid reuqests");
		for(String r: validRequests) {
			System.out.println("++++++++++");
			System.out.println("Running request " + r);
			runTest(r);
		}
		System.out.println("Running invalid requets");
		for(String r: invalidRequests) {
			System.out.println("++++++++++");
			System.out.println("Running request " + r);
			try {
				runTest(r);
				throw new Exception("Expected validation exception, nothing happened for " + r);
			} catch(NdlException ne) {
				if (!ne.toString().contains("Request validation failed"))
					throw new Exception("Expected validation exception, got " + ne);
			}
		}
	}
	
	public static void main(String[] argv) {
		ModifyParserTest mpt = new ModifyParserTest();
		
		try {
			mpt.run();
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			e.printStackTrace();
		}
	}
}
