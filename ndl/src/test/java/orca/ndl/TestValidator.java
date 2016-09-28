package orca.ndl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import orca.ndl.NdlModel.LocatorJarURL;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.vocabulary.RDF;

/** 
 * Test request model validation using Jena built in and external rules
 * @author ibaldin
 *
 */
public class TestValidator {
	OntModel requestModel = null;
	protected String[] inferenceModels = { "topology.owl", "compute.owl"};
	//protected String[] inferenceModels = { "topology.owl", "storage.owl", "compute.owl", "geni.owl", "eucalyptus.owl", "planetlab.owl", "protogeni.owl", "ec2.owl", "exogeni.owl"};

	public TestValidator() {
		NdlCommons.init();
		OntDocumentManager dm = OntDocumentManager.getInstance();
		dm.getFileManager().addLocator(new NdlModel.LocatorJarURL());
		PrintUtil.registerPrefix("dom", "http://geni-orca.renci.org/owl/domain.owl#");
		PrintUtil.registerPrefix("comp", "http://geni-orca.renci.org/owl/compute.owl#");
		PrintUtil.registerPrefix("req", "http://geni-orca.renci.org/owl/request.owl#");
		PrintUtil.registerPrefix("orca", "http://geni-orca.renci.org/owl/orca.owl#");
		PrintUtil.registerPrefix("storage", "http://geni-orca.renci.org/owl/storage.owl#");
		PrintUtil.registerPrefix("col", "http://geni-orca.renci.org/owl/collections.owl#");
		PrintUtil.registerPrefix("topo", "http://geni-orca.renci.org/owl/topology.owl#");
	}
	
	/**
	 * This shows no problems
	 * @param fName
	 * @throws NdlException
	 */
	public void openModel(String fName) throws NdlException {
		String ndlRequest = loadFile(fName);
		
		ByteArrayInputStream modelStream = new ByteArrayInputStream(ndlRequest.getBytes());
		
		requestModel = NdlModel.getRequestModelFromStream(modelStream, OntModelSpec.OWL_MEM_RDFS_INF, true);
		
		// need some imports for inference to work
		for (String model: inferenceModels)
			requestModel.read(NdlCommons.ORCA_NS + model);
		
		ValidityReport rep = requestModel.validate();
		
		getValidityOutput(rep);
	}
	
	/**
	 * This shows problems with e.g. storage ontology, but takes a VERY long time
	 * @param fName
	 * @throws NdlException
	 */
	public void openModel1(String fName) throws NdlException {
		
		//Model schema = ModelFactory.createDefaultModel();
		ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();
		
		Model allSchemas = ModelFactory.createOntologyModel();
		for (String mode: inferenceModels) {
			System.out.println("Validating against " + mode);
			URL schemaowl = cl.getResource("orca/ndl/schema/" + mode);
			System.out.println("Trying to load " + schemaowl.toString());
			Model schema1 = FileManager.get().loadModel(schemaowl.toString());
			Reasoner reasoner = ReasonerRegistry.getOWLReasoner();

			reasoner = reasoner.bindSchema(schema1);
			
			Model data = FileManager.get().loadModel(fName);

			InfModel model = ModelFactory.createInfModel(reasoner, data);

			ValidityReport rep = model.validate();

			getValidityOutput(rep);
			
			allSchemas.add(schema1);
		}
		
		System.out.println("Validating against all");
		Reasoner reasonerAll = ReasonerRegistry.getOWLReasoner();
		
		reasonerAll.bindSchema(allSchemas);
		
		Model data = FileManager.get().loadModel(fName);
		
		InfModel model = ModelFactory.createInfModel(reasonerAll, data);
		
		ValidityReport rep = model.validate();
		
		getValidityOutput(rep);
	}
	
	public static void testData(String dataF, String schemaF) {
		Model schema = FileManager.get().loadModel(schemaF);
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
		reasoner.bindSchema(schema);
		Model data = FileManager.get().loadModel(dataF);
		InfModel model = ModelFactory.createInfModel(reasoner, data);
		ValidityReport rep = model.validate();
		getValidityOutput(rep);
		
		Resource res = model.getResource("http://www.semanticweb.org/ontologies/2013/8/untitled-ontology-45#Image1");
		System.out.println("Image types:");
		printStatements(model, res, RDF.type, null);
	}
	
	public static void printStatements(Model m, Resource s, Property p, Resource o) {
        for (StmtIterator i = m.listStatements(s,p,o); i.hasNext(); ) {
            Statement stmt = i.nextStatement();
            System.out.println(" - " + PrintUtil.print(stmt));
        }
    }
	
	public static void getValidityOutput(ValidityReport rep) {
		if (rep.isValid()) {
			System.out.println("Model is valid");
		} 
		if (rep.isClean())
			System.out.println("Model is clean");
		System.out.println("Report: ");
		for (Iterator<Report> i = rep.getReports(); i.hasNext();) {
			System.out.println("   - " + i.next());
		}
	}
	
	public void validateModelWithRules(String fName) {
		System.out.println("Using rule file " + fName);
		ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();
		URL url = cl.getResource("orca/ndl/rules/" + fName);

		List<Rule> rules = Rule.rulesFromURL(url.toString());

		GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
		reasoner.setOWLTranslation(true);
		reasoner.setTransitiveClosureCaching(true);
		
		InfModel testModel = ModelFactory.createInfModel(reasoner, requestModel);
		
		getValidityOutput(testModel.validate());
	}
	
	public static String loadFile(String name) {
        StringBuilder sb = null;
        try {
                BufferedReader bin = null;
                File f = new File(name);
                FileInputStream is = new FileInputStream(f);
                bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                sb = new StringBuilder();
                String line = null;
                while((line = bin.readLine()) != null) {
                        sb.append(line);
                        // re-add line separator
                        sb.append(System.getProperty("line.separator"));
                }

                bin.close();
        } catch (Exception e) {
                System.err.println("Error "  + e + " encountered while readling file " + name);
                System.exit(1);
        } finally {
                ;
        }
        if (sb != null)
        	return sb.toString();
        return null;
	}
	
	public static void main(String[] argv) {
		
		System.out.println("Checking validity of request " + argv[0]);
		TestValidator tv = new TestValidator();
		long now = System.currentTimeMillis();		
		try {
			tv.openModel(argv[0]);
		} catch (Exception e) {
			System.err.println("Exception encountered: " + e.getMessage());
		}
		long then = System.currentTimeMillis();
		System.out.println("Validation took " + (then-now));
		
		//now = System.currentTimeMillis();
		//try {
		//	tv.openModel1(argv[0]);
		//} catch (Exception e) {
		//	System.err.println("Exception with more validation: " + e.getMessage());
		//	e.printStackTrace();
		//}
		//then = System.currentTimeMillis();
		//System.out.println("Validation took " + (then-now));
		
		now = System.currentTimeMillis();
		try {
			tv.validateModelWithRules(argv[1]);
		} catch (Exception e) {
			System.err.println("Exception with rules: " + e.getMessage());
			e.printStackTrace();
		}
		then = System.currentTimeMillis();
		System.out.println("Validation took " + (then-now));
		
//		TestValidator.testData("/Users/ibaldin/Desktop/data.owl", "/Users/ibaldin/Desktop/schema.owl");
	}
	
}
