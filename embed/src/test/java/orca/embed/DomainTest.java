package orca.embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import orca.embed.policyhelpers.VMPolicyNDLPoolPropertyExtractor;
import orca.embed.policyhelpers.VlanPolicyNDLPoolPropertyExtractor;
import orca.embed.workflow.Domain;
import orca.ndl.DomainResource;
import orca.ndl.DomainResourceType;
import orca.ndl.DomainResources;
import orca.ndl.NdlCommons;
import orca.ndl.NdlModel;
import orca.ndl.NdlException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

public class DomainTest extends TestCase {
	String inputFileNameTest;
	Domain domainTest;
	String [] inputFileName={"orca/ndl/substrate/rcivmsite.rdf","orca/ndl/substrate/bbnvmsite.rdf",
			"orca/ndl/substrate/uncvmsite.rdf","orca/ndl/substrate/mass.rdf",
			"orca/ndl/substrate/ben-6509.rdf",
			"orca/ndl/substrate/nlr.rdf","orca/ndl/substrate/starlight.rdf","orca/ndl/substrate/ion.rdf",
			"orca/ndl/substrate/rciNet.rdf","orca/ndl/substrate/bbnNet.rdf","orca/ndl/substrate/uncNet.rdf"};
	
	//String [] inputFileName={"orca/network/rencivmsite.rdf"};
	
	protected void setUp() throws Exception {
		super.setUp();
		inputFileNameTest="orca/ndl/substrate/cienavmsite.rdf";
		domainTest=new Domain(inputFileNameTest);
	}

	public void testGetDomain() throws NdlException {
		//OntModel aM=domainTest.getDomainElement().getModel();
		OntModel aM = NdlModel.getModelFromFile(inputFileNameTest,OntModelSpec.OWL_MEM_RDFS_INF, false);
		//aM.write(System.out);
		Resource domain=null;
		String type="ndl:NetworkDomain";
		String queryPhrase=NdlCommons.createQueryStringType(type);
		ResultSet results = NdlCommons.rdfQuery(aM,queryPhrase);
		String var0=(String) results.getResultVars().get(0);
		NdlCommons.outputQueryResult(results);
		//System.out.println(domainTest.getDomain());
		
		
        String queryPhrase4=
        	"SELECT ?d ?r " +
        	"WHERE {" +
        	"?d" +" a ndl:NetworkDomain"+
        	"      }"; 
        
        String queryString=NdlCommons.ontPrefix.concat(queryPhrase4);
        System.out.println(queryString);
        /*Query query = QueryFactory.create(queryString);

//      Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query,aM);
        results = qe.execSelect();*/
        results = NdlCommons.rdfQuery(aM,queryPhrase);
        NdlCommons.outputQueryResult(results);
	}
	
	public void testGetDomainPrefix() {
		System.out.println(domainTest.getDomainElement().getDomainPrefix(domainTest.getDomainElement().getResource()));
	}
	
	public void testFindBorderInterface() {		
		ArrayList <OntResource> intfs = domainTest.findBorderInterface(domainTest.getDomainElement().getModel());
		for(OntResource intf:intfs){
			System.out.println(intf.getURI()+"\n");
		}
	}
	
	public void testDelegateDomainModel(){
		//System.out.println(domainTest.delegateDomainModelToString("site.baremetalce"));
		OntModel abstractModel = domainTest.abstractDomain("vlan");

        ArrayList <Resource> virtList = (ArrayList<Resource>) NdlCommons.getXOVirtualizationType(abstractModel, "vm");
        
        for(Resource virt:virtList){
			System.out.println(virt.getURI()+"\n");
		}
	}
	
	public void testGetDomainResources() throws IOException, NdlException {
		//System.out.println(domainTest.delegateDomainModelToString("site.baremetalce"));
		String abstractModel_str = domainTest.delegateDomainModelToString("fourtygbaremetalce");
		DomainResources res = domainTest.getDomainResources(abstractModel_str, 3);
		System.out.println(res.toString());
	}
	
	public void testDelegateDomain() throws IOException, NdlException {
		int numDomain=inputFileName.length;	
		String [] type = {"site.vm","site.vlan"};
		Domain domain=null;
		String abstractModel=null;
		VMPolicyNDLPoolPropertyExtractor vmProperty;
		VlanPolicyNDLPoolPropertyExtractor vlanProperty;
		
		for(int i=0;i<numDomain;i++){
			System.out.println("\n Substrate: " + inputFileName[i]);
			if(i<3){
				for (String j : type){
					domain = new Domain(inputFileName[i]);
					abstractModel = domain.delegateDomainModelToString(j);
					//System.out.println(abstractModel);
					DomainResources res = domain.getDomainResources(abstractModel, 20);
					assertNotNull(res);
					List <DomainResourceType> types = res.getResourceType();
					assertNotNull(types);
					System.out.println("Resource Type count: " + types.size());
					for(DomainResourceType t: types){
						System.out.println("Resource type: " +t.getResourceType() + ": Count= " +t.getCount());
					}
					
					List<DomainResource> all = res.getResources();
					assertNotNull(all);
					System.out.println("Interface count: " + all.size());
					for (DomainResource r : all) {
						System.out.println("Interface: " + r.getInterface() + ": bw=" + r.getBandwidth());
					}	
					
					if(j.equals("site.vm")){
						vmProperty=new VMPolicyNDLPoolPropertyExtractor(abstractModel);
						System.out.println(vmProperty.getPoolAttributes().toString());
					}
					else{
						vlanProperty=new VlanPolicyNDLPoolPropertyExtractor(abstractModel);
						System.out.println(vlanProperty.getPoolProperties().toString());
					}
				}
				//continue;
			}
			else if(i==3){
				domain = new Domain(inputFileName[i]);
				abstractModel = domain.delegateDomainModelToString("vise.vm");
				vmProperty=new VMPolicyNDLPoolPropertyExtractor(abstractModel);
				System.out.println(vmProperty.getPoolAttributes().toString());
			}
			else {
				domain = new Domain(inputFileName[i]);
				abstractModel = domain.delegateDomainModelToString("site.vlan");
				vlanProperty=new VlanPolicyNDLPoolPropertyExtractor(abstractModel);
				System.out.println(vlanProperty.getPoolProperties().toString());
			}
			
			DomainResources res = domain.getDomainResources(abstractModel, 20);
			assertNotNull(res);
			List <DomainResourceType> types = res.getResourceType();
			assertNotNull(types);
			System.out.println("Resource Type count: " + types.size());
			for(DomainResourceType t: types){
				System.out.println("Resource type: " +t.getResourceType() + ": Count= " +t.getCount());
			}
			
			List<DomainResource> all = res.getResources();
			assertNotNull(all);
			System.out.println("Interface count: " + all.size());
			for (DomainResource r : all) {
				System.out.println("Interface: " + r.getInterface() + ": bw=" + r.getBandwidth());
			}
		}
	}
	
	public void testAbstractDomain() {		
		OntModel m = domainTest.abstractDomain(domainTest.getDomainElement().getModel(),null);
		if(m!=null){
			m.write(System.out);
			assertTrue(true);
		}
	}

	public void testDelegateFullModelToString(){
		System.out.println(domainTest.delegateFullModelToString());
	}
}
