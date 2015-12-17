package orca.ndl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.util.PrintUtil;

public class RequestParserTest implements INdlRequestModelListener {

	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		System.out.println("Broadcast connection " + bl + " of bw " + bandwidth + " with interfaces ");
		for(Resource i: interfaces) {
			System.out.println("  " + i);
		}
	}

	public void ndlNodeDependencies(Resource ni, OntModel m,
			Set<Resource> dependencies) {
		System.out.println("Node " + ni + " has dependencies: " );
		for(Resource d: dependencies) {
			System.out.println("  " + d);
		}
	}

	public void ndlReservation(Resource i, OntModel m) {
		System.out.println("Reservation object " + i);
	}

	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		System.out.println("Reservation end " + end);
	}

	public void ndlReservationResources(List<Resource> r, OntModel m) {
		System.out.println("Reservation resources: ");
		for (Resource rr: r) {
			System.out.println("  " + rr);
		} 
	}

	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		System.out.println("Reservation start " + start);
	}

	public void ndlReservationTermDuration(Resource d, OntModel m, int years,
			int months, int days, int hours, int minutes, int seconds) {
		System.out.println("Reservation Duration " + years + " " + months + " " + days + " " + hours + " " + minutes + " " + seconds);
	}

	public void ndlSlice(Resource sl, OntModel m) {
		System.out.println("Reservation ndl slice "+ sl);

	}

	public void ndlInterface(Resource l, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		System.out.println("Interface " + l);
		System.out.println("  connected to " + conn);
		if (conn == null) {
			System.err.println("***************");
		} 
		System.out.println("  and node " + node);
		System.out.println("  with ip " + ip + " and mask " + mask);
	}

	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		System.out.println("Network connection " + l);
		System.out.println("  bandwidth " + bandwidth);
		System.out.println("  latency " + latency);
	}

	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		System.out.println("Node " + ce);
		System.out.println("  of class " + ceClass);
	}

	public void ndlParseComplete() {
		// TODO Auto-generated method stub

	}
	
	private void run_(String reqFile) throws NdlException, FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream(reqFile);
		if (is == null) {
			is = new FileInputStream(reqFile);
		}
		assert(is != null);
		String r = new Scanner(is).useDelimiter("\\A").next();
		
		NdlRequestParser nrp = new NdlRequestParser(r, this);

		nrp.processRequest();
		nrp.freeModel();
	}

	
	private void tempQuery(InfModel om) {
		String selectStr = "SELECT ?node";
		String fromStr = "";
		String whereStr = " WHERE {?node domain:hasResourceType compute:BareMetalCE.}";
		
		String queryString = NdlCommons.createQueryString(selectStr, fromStr, whereStr);
		
        Query query = QueryFactory.create(queryString);

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, om);
        ResultSet rs = qe.execSelect();
		
		while(rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource node = (Resource)result.get("node");
			System.out.println("Found node " + node.getURI());
		}
		System.out.println("DONE");
	}
	
	private void printModel(InfModel om) {
		for (StmtIterator i = om.listStatements(); i.hasNext(); ) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
		} 
	}
	
	private static String[] validRequests={ "/test-color-extension.rdf", "/large-osg-request.rdf", "/group-storage.rdf", "/node-storage.rdf" };
	private static String[] invalidRequests={ "/broadcast-storage-invalid.rdf", "/node-storage-bound-invalid.rdf" };
	
	@Test
	public void run() throws NdlException, IOException {
		System.out.println("Running valid requests");
		for(String r: validRequests) {
			System.out.println("++++++++++");
			System.out.println("Running request " + r);
			run_(r);
		}
		System.out.println("Running invalid requests");
		for(String r: invalidRequests) {
			System.out.println("++++++++++");
			System.out.println("Running request " + r);
			try {
				run_(r);
				throw new NdlException("Expected validation failure for " + r);
			} catch (NdlException ne) {
				if (!ne.toString().contains("Request validation failed"))
					throw ne;
			}
		}
	}
	
	public static void main(String[] argv) {
		try {
			System.out.println("Reading " + argv[0]);
			new RequestParserTest().run_(argv[0]);
		} catch (Exception e) {
			System.err.println("Error: " + e);
		}
//		try {
//			new RequestParserTest().run();
//		} catch (Exception e) {
//			System.err.println("Error: " + e);
//		}
	}
}
