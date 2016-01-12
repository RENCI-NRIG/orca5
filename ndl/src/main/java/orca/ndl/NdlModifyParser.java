package orca.ndl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import orca.ndl.INdlModifyModelListener.ModifyType;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.util.ResourceUtils;

/**
 * Parser for modify requests. Has an option of rewriting the namespace of 
 * repeatable elements of the modify request by invoking rewriteModifyRequest()
 * function. 
 * 
 * @author ibaldin
 *
 */
public class NdlModifyParser extends NdlParserHelper {
	public static final String USER_MODIFY_RULES_FILE_PROPERTY = "NDL_MODIFY_RULE_FILE";
	private static final String RULES_FILE = "orca/ndl/rules/modifyRules.rules";
	INdlModifyModelListener listener;
	OntModel modifyModel;
	boolean rewritten=false;
	private boolean lessStrictChecking = false;

	public NdlModifyParser(String ndlModifyRequest, INdlModifyModelListener l) throws NdlException {

		if ((ndlModifyRequest == null) || (l == null))
			throw new NdlException("Null parameters to the NdlRequestParser constructor");

		listener = l;

		ByteArrayInputStream modelStream = new ByteArrayInputStream(ndlModifyRequest.getBytes());

		// by default use new document manager and TDB
		modifyModel = NdlModel.getModelFromStream(modelStream, null, true);		
	}
	
	public NdlModifyParser(String ndlModifyRequest, INdlModifyModelListener l, NdlModel.ModelType t, String folderName) throws NdlException {

		if ((ndlModifyRequest == null) || (l == null))
			throw new NdlException("Null parameters to the NdlRequestParser constructor");

		listener = l;

		ByteArrayInputStream modelStream = new ByteArrayInputStream(ndlModifyRequest.getBytes());

		// by default use new document manager and TDB
		modifyModel = NdlModel.getModelFromStream(modelStream, null, true, t, folderName);		
	}

	private static class RewriteListener implements INdlModifyModelListener {
		private List<Resource> toRename = new ArrayList<Resource>();
		private String newNs = null;
		
		public RewriteListener(String ns) {
			if (ns != null)
				newNs = ns;
			else
				newNs = NdlCommons.ORCA_NS + "modify/" + UUID.randomUUID().toString() + "#";
		}
		
		public RewriteListener() {
			newNs = NdlCommons.ORCA_NS + "modify/" + UUID.randomUUID().toString() + "#";
		}
		
		public void ndlModifyElement(Resource i, Resource subject,
				ModifyType t, Resource object, int modU, OntModel m) {
			toRename.add(i);
			// for type add element, need to rewrite the element itself
			if (t == ModifyType.ADD) {
				Statement s = i.getProperty(modifyAddElementProperty);
				if (s != null) {
					toRename.add((Resource)s.getObject());
				}
			}
		}

		public void ndlModifyReservation(Resource i, Literal name, OntModel m) {
			toRename.add(i);			
		}

		public void ndlParseComplete() {
			// rename the resources
			for (Resource i: toRename) {
				String tn = NdlCommons.getTrueName(i);
				ResourceUtils.renameResource(i, newNs + tn);
			}
		}
		
		/**
		 * Get a copy of renamed items
		 * @return
		 */
		public List<Resource> listRenamed() {
			return new ArrayList<Resource>(toRename);
		}
	}
	
	/**
	 * Rewrite the namespaces of the modify request into a new and unique prefix. The side
	 * effect of this call is that the underlying model is modified and only the re-written
	 * statements are used from then on. This call is idempotent.
	 * @throws NdlException
	 * @return List<Resource> list of renamed items in the model
	 */
	public List<Resource> rewriteModifyRequest() throws NdlException {
		return rewriteModifyRequest(null);
	}

	/**
	 * Rewrite the namespaces of the modify request into a new namespace prefix. The side
	 * effect of this call is that the underlying model is modified and only the re-written
	 * statements are used from then on. This call is idempotent.
	 * @param ns - the new prefix (can be null, auto-generated guid is used in this case)
	 * @return List<Resource> list of renamed items in the model
	 * @throws NdlException
	 */
	public synchronized List<Resource> rewriteModifyRequest(String ns) throws NdlException {
		if (rewritten)
			return null;

		if (modifyModel == null)
			return null;

		rewritten = true;
		
		// create a simple listener and pass through the parser to rewrite
		// the URIs of non-unique resources
		
		// replace the user-provided listener with ours, temporarily
		INdlModifyModelListener save = listener;
		listener = new RewriteListener(ns);

		processModifyRequest();

		List<Resource> ret = ((RewriteListener)listener).listRenamed();
		
		listener = save;
		
		return ret;
	}

	/**
	 * Parse the modification request. The model passed on to the listener depends 
	 * on whether rewriteModifyRequest() has been called. If it has, the statements
	 * in the model are guaranteed unique compared to other requests. If not, you may
	 * see a repetition of the same statements.
	 * @throws NdlException
	 */
	public synchronized void processModifyRequest() throws NdlException {
		if (modifyModel == null)
			return;
		
		if (!lessStrictChecking) {
			validateRequest(RULES_FILE, USER_MODIFY_RULES_FILE_PROPERTY, modifyModel);
		}
		
		// reservation query from which everything flows
		String query = OntProcessor.createQueryStringModifyReservation();
		ResultSet rs = OntProcessor.rdfQuery(modifyModel, query);

		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource res = (Resource)result.get("modifyReservation");
			Literal nm = (Literal)result.get("modifyName");

			// reservation
			listener.ndlModifyReservation(res, nm, modifyModel);

			// modify elements (collection)
			{
				for (StmtIterator resEl = res.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource tmpR = resEl.next().getResource();

					if (tmpR != null) {
						// should have a subject and action
						if (tmpR.hasProperty(modifySubjectProperty)) {
							Statement stm = tmpR.getProperty(modifySubjectProperty);
							Resource sub = stm.getResource();
							Resource obj=null;
							int n=0;

							INdlModifyModelListener.ModifyType modType = null;

							if (tmpR.hasProperty(modifyAddElementProperty)){ 
								modType = ModifyType.ADD;
								obj = tmpR.getProperty(modifyAddElementProperty).getResource();
							} else if (tmpR.hasProperty(modifyElementProperty)){ 
								modType = ModifyType.MODIFY;
								obj = tmpR.getProperty(modifyElementProperty).getResource();
							} else if (tmpR.hasProperty(modifyRemoveElementProperty)){
								modType = ModifyType.REMOVE;
								obj = tmpR.getProperty(modifyRemoveElementProperty).getResource();
							} else if (tmpR.hasProperty(modifyIncreaseByProperty)){
								modType = ModifyType.INCREASE;
								n = tmpR.getProperty(modifyIncreaseByProperty).getInt();
							}

							listener.ndlModifyElement(tmpR, sub, modType, obj, n, modifyModel);
						}
					}
				}
			}
		}
		listener.ndlParseComplete();
	}
	
	public void doLessStrictChecking() {
		lessStrictChecking = true;
	}
	
	/**
	 * Free the model that was passed in to the listener interface. It is highly
	 * recommended you use this function, rather than freeing the model yourself.
	 * Note that the model (or any model based on it) cannot be used after you
	 * call this. This call is idempotent.
	 */
	public synchronized void freeModel() {
		NdlModel.closeModel(modifyModel);
		modifyModel = null;
	}

}
