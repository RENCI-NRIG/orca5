package orca.ndl;

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
	
	@Test
	public void runTest() throws NdlException  {
		InputStream is = this.getClass().getResourceAsStream("/modify-request.rdf");
		assert(is != null);
		String r = new Scanner(is).useDelimiter("\\A").next();

		ModifyParserTest t = new ModifyParserTest();

		NdlModifyParser p = new NdlModifyParser(r, t);
		System.out.println("Processing");
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
}
