package orca.embed.workflow;

import java.util.LinkedList;

import com.hp.hpl.jena.ontology.OntResource;

public class ModifyReservations {
	protected LinkedList <OntResource> removedElements;
	protected LinkedList <OntResource> addedElements;
	protected LinkedList <OntResource> modifiedElements;
	protected LinkedList <OntResource> modifiedRemoveElements;
	
	public void addRemovedElement(OntResource d_ont){
		if(removedElements==null)
			removedElements = new LinkedList<OntResource>();
		removedElements.add(d_ont);
	}
	public void addAddedElement(OntResource d_ont){
		if(addedElements==null)
			addedElements = new LinkedList<OntResource>();
		addedElements.add(d_ont);
	}
	public void addModifedElement(OntResource d_ont){
		if(modifiedElements==null)
			modifiedElements = new LinkedList<OntResource>();
		modifiedElements.add(d_ont);
	}
	public void addModifedRemoveElement(OntResource d_ont){
		if(modifiedRemoveElements==null)
			modifiedRemoveElements = new LinkedList<OntResource>();
		modifiedRemoveElements.add(d_ont);
	}
	
	public LinkedList<OntResource> getModifiedElements() {
		return modifiedElements;
	}
	public LinkedList<OntResource> getRemovedElements() {
		return removedElements;
	}
	public LinkedList<OntResource> getAddedElements() {
		return addedElements;
	}	
	public LinkedList<OntResource> getModifiedRemoveElements() {
		return modifiedRemoveElements;
	}

	public void clear(){
		if(removedElements!=null)
			removedElements.clear();
		if(addedElements!=null)
			addedElements.clear();
		if(modifiedElements!=null)
			modifiedElements.clear();
		if(modifiedRemoveElements!=null)
			modifiedRemoveElements.clear();
	}
}
