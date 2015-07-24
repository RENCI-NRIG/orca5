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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

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
		
		RequestParserTest rpt = new RequestParserTest();
		NdlRequestParser nrp = new NdlRequestParser(r, rpt);
		nrp.processRequest();
		nrp.freeModel();
	}

	private static String[] requests={ "/test-color-extension.rdf", "/large-osg-request.rdf" };
	
	@Test
	public void run() throws NdlException, IOException {
		for(String r: requests) {
			System.out.println("++++++++++");
			System.out.println("Running request " + r);
			run_(r);
		}
	}
	
	public static void main(String[] argv) {
		try {
			new RequestParserTest().run_(argv[0]);
		} catch (Exception e) {
			System.err.println("Error: " + e);
		}
	}
}
