package orca.ndl;

import java.util.List;
import java.util.Map;

import orca.ndl.elements.LabelSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * for the current (02-2012) abstract delegation model, there are no internal nodes
 * or network connections to expose, only the networkdomain and its interfaces
 * @author ibaldin
 *
 */
public interface INdlAbstractDelegationModelListener extends INdlCommonModelListener {

	/**
	 * Network domain with its service(s), available labelSet(s) and interfaces
	 * @param dom
	 * @param m
	 * @param netServices - network services of this domain 
	 * @param interfaces - list of interfaces
	 * @param labelSets - all label sets (net and non-net)
	 * @param netLabelSets - network label sets with associated interfaces
	 */
	public void ndlNetworkDomain(Resource dom, OntModel m, List<Resource> netServices, List<Resource> interfaces, List<LabelSet> labelSets, Map<Resource, List<LabelSet>> netLabelSets);
}
