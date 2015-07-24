package orca.ndl;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Interface for classes interested in various features of an NDL request model. These
 * methods are callbacks to be invoked by NDL querying code. Matching individual
 * and model are passed it to aid in deeper queries.
 * @author ibaldin
 *
 */
public interface INdlRequestModelListener extends INdlCommonModelListener {
	
	/**
	 * Sets reservation URI
	 * @param i
	 * @param m
	 */
	public void ndlReservation(Resource i, OntModel m);
	
//	/**
//	 * Get the domain of the reservation
//	 * @param d
//	 * @param m
//	 */
//	public void ndlReservationDomain(Resource d, OntModel m);
	
	/**
	 * Sets reservation term
	 * @param t term resource
	 * @param m
	 * @param years
	 * @param months
	 * @param days
	 * @param hours
	 * @param minutes
	 * @param seconds
	 */
	public void ndlReservationTermDuration(Resource d, OntModel m, int years, int months, int days, int hours, int minutes, int seconds);
	
	/**
	 * List of resources in the reservation
	 * @param r
	 * @param m
	 */
	public void ndlReservationResources(List<Resource> r, OntModel m);
	
//	/**
//	 * Disk Image attached to reservation
//	 * @param di
//	 * @param m
//	 * @param res
//	 * @param name
//	 * @param url
//	 * @param hash
//	 */
//	public void ndlReservationDiskImage(Resource di, OntModel m, Resource res, String url, String hash);
	
	/**
	 * Sets reservation start date
	 * @param s
	 * @param m
	 * @param start
	 */
	public void ndlReservationStart(Literal s, OntModel m, Date start);
	
	/**
	 * sets reservation end date
	 * @param e
	 * @param m
	 * @param end
	 */
	public void ndlReservationEnd(Literal e, OntModel m, Date end);
	
//	/**
//	 * Return a post boot script of a node
//	 * @param script
//	 * @param m
//	 * @param node
//	 */
//	public void ndlNodePostBootScript(String script, OntModel m, Resource node);
	
	/**
	 * All declared dependencies of this node
	 * @param ni
	 * @param m
	 * @param dependencies
	 */
	public void ndlNodeDependencies(Resource ni, OntModel m, Set<Resource> dependencies);
	
	/**
	 * All sub-slices within this reservation
	 * @param sl
	 * @param m
	 */
	public void ndlSlice(Resource sl, OntModel m);
	
	/**
	 * Broadcast connection, similar to NetworkConnection
	 * @param bl
	 * @param om
	 * @param bandwidth
	 * @param interfaces
	 */
	public void ndlBroadcastConnection(Resource bl, OntModel om, long bandwidth, List<Resource> interfaces);
	
//	/**
//	 * 
//	 * @param ni
//	 * @param m
//	 * @param declared
//	 * @param splittable
//	 */
//	public void ndlNodeSplittable(Resource ni, OntModel m, boolean declared, boolean splittable);
}
