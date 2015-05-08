package orca.ndl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import orca.ndl.elements.LabelSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

public class NdlTDBLeakTest implements INdlManifestModelListener, INdlRequestModelListener, INdlAbstractDelegationModelListener {

	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	public void ndlInterface(Resource l, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		// TODO Auto-generated method stub

	}

	public void ndlParseComplete() {
		// TODO Auto-generated method stub

	}

	public void ndlManifest(Resource i, OntModel m) {
		// TODO Auto-generated method stub

	}

	public void ndlLinkConnection(Resource l, OntModel m,
			List<Resource> interfaces, Resource parent) {
		// TODO Auto-generated method stub

	}

	public void ndlCrossConnect(Resource c, OntModel m, long bw, String label,
			List<Resource> interfaces, Resource parent) {
		// TODO Auto-generated method stub

	}

	public void ndlNetworkConnectionPath(Resource c, OntModel m,
			List<List<Resource>> paths, List<Resource> roots) {
		System.out.println("Network Connection Path: " + c);
		if (roots != null) {
			System.out.println("Printing roots");
			for (Resource rr: roots) {
				System.out.println(rr);
			}
		}
		if (paths != null) {
			System.out.println("Printing paths");
			for (List<Resource> p: paths) {
				StringBuilder sb =  new StringBuilder();
				sb.append("   Path: ");
				for (Resource r: p) {
					sb.append(r + " ");
				}
				System.out.println(sb.toString());
			}

		} else 
			System.out.println("   None");

	}

	private static String[] manifests={ "/test-color-extension-manifest.rdf", "/manifest-node-sharedvlan.rdf", "/manifest-node-intra.rdf" };


	public void ndlReservation(Resource i, OntModel m) {
		// TODO Auto-generated method stub
		
	}

	public void ndlReservationTermDuration(Resource d, OntModel m, int years,
			int months, int days, int hours, int minutes, int seconds) {
		// TODO Auto-generated method stub
		
	}

	public void ndlReservationResources(List<Resource> r, OntModel m) {
		// TODO Auto-generated method stub
		
	}

	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		// TODO Auto-generated method stub
		
	}

	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		// TODO Auto-generated method stub
		
	}

	public void ndlNodeDependencies(Resource ni, OntModel m,
			Set<Resource> dependencies) {
		// TODO Auto-generated method stub
		
	}

	public void ndlSlice(Resource sl, OntModel m) {
		// TODO Auto-generated method stub
		
	}

	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		// TODO Auto-generated method stub
		
	}

	public void ndlNetworkDomain(Resource dom, OntModel m,
			List<Resource> netServices, List<Resource> interfaces,
			List<LabelSet> labelSets, Map<Resource, List<LabelSet>> netLabelSets) {
		// TODO Auto-generated method stub
		
	}
	
	private void runM(String reqFile) throws NdlException, FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream(reqFile);
		if (is == null) {
			is = new FileInputStream(reqFile);
		}
		assert(is != null);
		Scanner s = new Scanner(is);
		
		String r = s.useDelimiter("\\A").next();
		
		s.close();
		
		NdlManifestParser mp = new NdlManifestParser(r, this, NdlModel.ModelType.TdbPersistent, "/tmp/tdb/" + UUID.randomUUID());
		mp.processManifest();
		mp.freeModel();
	}
	
	private final String RDF_START = "<rdf:RDF>";
	private final String RDF_END = "</rdf:RDF>";
	
	private void runA(String reqFile) throws NdlException, FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream(reqFile);
		if (is == null) {
			is = new FileInputStream(reqFile);
		}
		assert(is != null);
		Scanner s = new Scanner(is);
		
		String r = s.useDelimiter("\\A").next();
		
		s.close();
		
//		NdlAbstractDelegationParser ap = new NdlAbstractDelegationParser(r, this, true);
//		ap.processDelegationModel();
//		ap.freeModel();
		
		boolean done = false;

		while (!done) {
			// find <rdf:RDF> and </rdf:RDF>
			int start = r.indexOf(RDF_START);
			int end = r.indexOf(RDF_END);
			if ((start == -1) || (end == -1)) {
				done = true;
				continue;
			}
			String ad = r.substring(start, end + RDF_END.length());

			// parse out
			NdlAbstractDelegationParser nadp = new NdlAbstractDelegationParser(ad, this, NdlModel.ModelType.TdbPersistent, "/tmp/tdb/" + UUID.randomUUID());

			// this will call the callbacks
			nadp.processDelegationModel();
			
			nadp.freeModel();
			
			// advance pointer
			r = r.substring(end + RDF_END.length());
		}
	}

	private void runR(String reqFile) throws NdlException, FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream(reqFile);
		if (is == null) {
			is = new FileInputStream(reqFile);
		}
		assert(is != null);
		Scanner s = new Scanner(is);
		
		String r = s.useDelimiter("\\A").next();
		
		s.close();
		
		NdlRequestParser rp = new NdlRequestParser(r, this, NdlModel.ModelType.TdbPersistent, "/tmp/tdb/" + UUID.randomUUID());
		rp.processRequest();
		rp.freeModel();
	}
	
	public static void main(String[] argv) {
		NdlTDBLeakTest t = new NdlTDBLeakTest();
		
		List<Long> meas = new ArrayList<Long>();
		try {
			for(int i=0; true; i++) {
			//for(int i=0; i< 10; i++) {
				System.out.println("Iteration " + i + " of " + 100);
				long now = System.currentTimeMillis();
				t.runM(argv[0]);
				t.runA(argv[1]);
				t.runR(argv[2]);
				long later = System.currentTimeMillis();
				meas.add(later-now);
				
				long heapSize = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				
				System.out.println("Heap: " + heapSize/1000000L);
				
				//Thread.sleep(1000);
			}
		} catch (Exception e) {
			System.out.println("UNABLE: " + e);
			e.printStackTrace();
		}
		
		Long avg  = 0L;
		int n = 0;
		for(Long v: meas) {
			avg = (avg*n + v ) / (n+1);
		}
		
		System.out.println("Average:" + avg);
	}

}