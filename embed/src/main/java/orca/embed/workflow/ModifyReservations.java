package orca.embed.workflow;

import java.util.LinkedList;

import com.hp.hpl.jena.ontology.OntResource;

public class ModifyReservations {
	protected LinkedList <OntResource> removedElements;
	protected LinkedList <OntResource> addedElements;
	
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
	public LinkedList<OntResource> getRemovedElements() {
		return removedElements;
	}
	public LinkedList<OntResource> getAddedElements() {
		return addedElements;
	}
	public void clear(){
		if(removedElements!=null)
			removedElements.clear();
		if(addedElements!=null)
			addedElements.clear();
	}
}
