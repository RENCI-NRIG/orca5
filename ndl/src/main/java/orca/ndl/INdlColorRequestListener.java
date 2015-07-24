package orca.ndl;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Listener of color properties of a request
 * @author ibaldin
 *
 */
public interface INdlColorRequestListener {
	
	/**
	 * Provides a color attached to network element (can be many)
	 * @param ne 
	 * @param color
	 * @param label
	 */
	public void ndlResourceColor(Resource ne, Resource color, String label);
	
	/**
	 * Provides a color dependency between two network elements
	 * @param fromNe
	 * @param toNe
	 * @param color
	 * @param label
	 */
	public void ndlColorDependency(Resource fromNe, Resource toNe, Resource color, String label);
}
