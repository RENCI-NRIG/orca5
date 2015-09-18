package orca.embed.workflow;

import java.util.Collection;
import java.util.LinkedList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import orca.embed.policyhelpers.ModifyElement;
import orca.ndl.INdlModifyModelListener;

public class ModifyParserListener implements INdlModifyModelListener {
	protected OntModel model = null;
	protected Collection <ModifyElement> modifyElements = new LinkedList <ModifyElement>();

	public void ndlModifyReservation(Resource i, Literal name, OntModel m) {
		model=m;
	}

	public void ndlModifyElement(Resource i, Resource subject, ModifyType t,Resource object, int unit, OntModel m) {
		
		ModifyElement me = new ModifyElement(subject,t,object,unit);
		
		modifyElements.add(me);
	}
	
	public OntModel getModel() {
		return model;
	}

	public void setModel(OntModel model) {
		this.model = model;
	}

	public Collection<ModifyElement> getModifyElements() {
		return modifyElements;
	}

	public void setModifyElements(Collection<ModifyElement> modifyElements) {
		this.modifyElements = modifyElements;
	}

	public void ndlParseComplete() {

	}

}
