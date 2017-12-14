package orca.ndl;

import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Listener for attributes common to manifests, requests and substrate descriptions/ads
 * 
 * @author ibaldin
 *
 */
public interface INdlCommonModelListener extends INdlSimpleModelListener {

    /**
     * Sets an individual compute element (or cluster), domain, CE type and count (if cluster, otherwise 0) and a list
     * of interfaces. This is called *before* ndlConnection calls. You are guaranteed to see any node only once (even if
     * it appears multiple times).
     * 
     * @param ce
     * @param om
     * @param ceClass
     *            (ComputeElement, ServerCloud, CrossConnect etc)
     * @param interfaces
     */
    public void ndlNode(Resource ce, OntModel om, Resource ceClass, List<Resource> interfaces);

    /**
     * Sets network connection connecting resources (via interfaces) (with bandwidth and latency) and a list of
     * interfaces. This is called *after* ndlComputeElement calls. You are guaranteed to see any network connection only
     * once (even if it appears multiple times).
     * 
     * @param l
     * @param om
     * @param bandwidth
     * @param latency
     * @param interfaces
     */
    public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth, long latency, List<Resource> interfaces);

    /**
     * Sets interface with IP address and netmask and entities connected to this interface. Called after ComputeElement
     * and Connection. You are guaranteed to see each interface only once.
     * 
     * @param l
     * @param om
     * @param conn
     *            is the connection object that hasInterface
     * @param node
     *            is the other object that hasInterface (compute element, crossconnect etc)
     * @param ip
     * @param mask
     */
    public void ndlInterface(Resource l, OntModel om, Resource conn, Resource node, String ip, String mask);

}
