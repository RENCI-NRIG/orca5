package orca.ndl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.PrintUtil;

public class NdlParserHelper extends NdlCommons {
	protected boolean lessStrictChecking = false;
	
	/**
	 * Validate the request using the rules. Merge static rules and user-supplied rules
	 * @throws NdlException
	 */
	protected void validateRequest(String staticRuleFile, String ruleFilePropertyName, OntModel modelToCheck) throws NdlException {
		
		PrintUtil.registerPrefix("topo", "http://geni-orca.renci.org/owl/topology.owl#");
		PrintUtil.registerPrefix("comp", "http://geni-orca.renci.org/owl/compute.owl#");
		PrintUtil.registerPrefix("xo", "http://geni-orca.renci.org/owl/exogeni.owl#");
		PrintUtil.registerPrefix("exogeni", "http://geni-orca.renci.org/owl/exogeni.owl#");
		PrintUtil.registerPrefix("storage", "http://geni-orca.renci.org/owl/storage.owl#");
		PrintUtil.registerPrefix("geni", "http://geni-orca.renci.org/owl/geni.owl#");
		PrintUtil.registerPrefix("dom", "http://geni-orca.renci.org/owl/domain.owl#");
		PrintUtil.registerPrefix("req", "http://geni-orca.renci.org/owl/request.owl#");
		PrintUtil.registerPrefix("orca", "http://geni-orca.renci.org/owl/orca.rdf#");
		PrintUtil.registerPrefix("euca", "http://geni-orca.renci.org/owl/eucalyptus.owl#");
		PrintUtil.registerPrefix("pl", "http://geni-orca.renci.org/owl/planetlab.owl#");
		PrintUtil.registerPrefix("col", "http://geni-orca.renci.org/owl/collections.owl#");
		PrintUtil.registerPrefix("color", "http://geni-orca.renci.org/owl/app-color.owl#");
		PrintUtil.registerPrefix("ip4", "http://geni-orca.renci.org/owl/ip4.owl#");
		PrintUtil.registerPrefix("modify", "http://geni-orca.renci.org/owl/modify.owl#");
		
		ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();

		//Reasoner owlReasoner = ReasonerRegistry.getOWLReasoner();
		
		// look at the first mc models from the list (to save time)
		// and validate against them
		/**
		int mc = 3;
		for (String model: inferenceModels) {
			if (mc-- == 0)
				break;
			URL schemaowl = cl.getResource("orca/ndl/schema/" + model);
			FileManager fm = new FileManager();
			fm.addLocator(new NdlCommons.LocatorJarURL());
			Model schema1 = fm.loadModel(schemaowl.toString());

			owlReasoner = owlReasoner.bindSchema(schema1);

			InfModel owlModel = ModelFactory.createInfModel(owlReasoner, requestModel);

			ValidityReport rep = owlModel.validate();
			
			if (rep.isValid() && rep.isClean()) {
				continue;
			} 
			StringBuilder sb = new StringBuilder("Request validation failed OWL validation due to");
			for (Iterator<Report> i = rep.getReports(); i.hasNext();) {
				sb.append(": " + i.next());
			}
			throw new NdlException(sb.toString());
		}
		*/
		
		// load the rules and create a rule-based reasoner
		getNdlLogger().debug("Reading default rule set " + staticRuleFile);
		String defaultRules = readResourceIntoString(staticRuleFile);
		if (defaultRules == null)
			throw new NdlException("Default rule set " + staticRuleFile + " is empty, unable to validate");

		String userRules = null;
		StringBuilder combinedRules = new StringBuilder();
		if (ruleFilePropertyName != null) {
			String userRuleFile = System.getProperty(ruleFilePropertyName);
			if (userRuleFile != null) {
				getNdlLogger().debug("Merging user rule file " + userRuleFile + " with default rule set " + staticRuleFile);
				userRules = readFileIntoString(userRuleFile);
				if (userRules != null)
					combinedRules.append(userRules);
				else
					getNdlLogger().info("Specified rule file " + userRuleFile + " doesn't exist or is empty, ignoring");
			}
		}
		
		// put user rules first; attempt to parse combined set; if doesn't work use only default rules
		combinedRules.append(defaultRules);
		
		InputStream is = new ByteArrayInputStream(combinedRules.toString().getBytes());
		InputStreamReader isr = new InputStreamReader(is);
		
		//InputStreamReader isr = new InputStreamReader(cl.getResourceAsStream(staticRuleFile));
		
		Rule.Parser rp = Rule.rulesParserFromReader(new BufferedReader(isr));
		
		List<Rule> rules = getRules(combinedRules);
		
		if (rules == null) {
			// try just the default rules
			getNdlLogger().error("Unable to parse combined rule set, using default rules only");
			combinedRules = new StringBuilder();
			combinedRules.append(defaultRules);
			rules = getRules(combinedRules);
		}
		
		if (rules == null) 
			throw new NdlException("Unable to parse default rules");
		
		GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
		reasoner.setOWLTranslation(true);
		reasoner.setTransitiveClosureCaching(true);
		InfModel ruleModel = ModelFactory.createInfModel(reasoner, modelToCheck);
		
		// parse the validity report
		ValidityReport rep = ruleModel.validate();
		if (rep.isValid() && rep.isClean()) {
			return;
		} 
		StringBuilder sb = new StringBuilder("Request validation failed rule validation due to");
		for (Iterator<Report> i = rep.getReports(); i.hasNext();) {
			sb.append(": " + i.next());
		}
		throw new NdlException(sb.toString());
	}
	
	public void doLessStrictChecking() {
		lessStrictChecking = true;
	}
	
	/**
	 * Get Jena rules from a string builder. Null if rules can't be parsed
	 * @param combinedRules
	 * @return
	 */
	private List<Rule> getRules(StringBuilder combinedRules) {
		InputStream is = new ByteArrayInputStream(combinedRules.toString().getBytes());
		InputStreamReader isr = new InputStreamReader(is);
		
		//InputStreamReader isr = new InputStreamReader(cl.getResourceAsStream(staticRuleFile));
		
		Rule.Parser rp = Rule.rulesParserFromReader(new BufferedReader(isr));
		
		List<Rule> rules = null;
		
		try {
			rules = Rule.parseRules(rp);
		} catch(Rule.ParserException rpe) {
			return null;
		}
		return rules;
	}
	
	/**
	 * Read file as String
	 * @param fName
	 * @return
	 */
	public static String readFileIntoString(String fName) {
		String ret = null;
		
		Scanner sc = null;
		try {
			sc = new Scanner(new File(fName));
			ret = sc.useDelimiter("\\A").next();
		} catch (FileNotFoundException fnf) {
			;
		} finally {
			if (sc != null)
				sc.close();
		}
		return ret;
	}
	
	/**
	 * Read resource as String
	 * @param resource
	 * @return
	 */
	public static String readResourceIntoString(String resource) {
		String ret = null;
		
		Scanner sc = null;
		try {
			ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();
			sc = new Scanner(cl.getResourceAsStream(resource));
			ret = sc.useDelimiter("\\A").next();
		} catch (Exception fnf) {
			;
		} finally {
			if (sc != null)
				sc.close();
		}
		return ret;
	}
}
