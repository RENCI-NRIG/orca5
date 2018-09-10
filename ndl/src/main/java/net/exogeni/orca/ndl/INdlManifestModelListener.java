package net.exogeni.orca.ndl;

import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Interface defines callbacks for classes interested in NDL Manifests
 * 
 * @author ibaldin
 *
 */
public interface INdlManifestModelListener extends INdlCommonModelListener {

    /**
     * Sets manifest URI
     * 
     * @param i i
     * @param m m
     */
    public void ndlManifest(Resource i, OntModel m);

    /**
     * A LinkConnection object.
     * 
     * @param l l
     * @param m m
     * @param interfaces interfaces
     * @param parent
     *            NetworkConnection (if any)
     */
    public void ndlLinkConnection(Resource l, OntModel m, List<Resource> interfaces, Resource parent);

    /**
     * A CrossConnect object with bandwidth and label. Never repeated.
     * 
     * @param c c
     * @param m m
     * @param bw bw
     * @param label label
     * @param interfaces interfaces
     * @param parent
     *            NetworkConnection (if any)
     */
    public void ndlCrossConnect(Resource c, OntModel m, long bw, String label, List<Resource> interfaces,
            Resource parent);

    /**
     * Return the proper sequence of resources (nodes, linkconnections, crossconnects etc) for a NetworkConnection.
     * Never repeated.
     * 
     * @param c c
     * @param m m
     * @param path
     *            - list of paths (for multipoint connections).
     * @param roots
     *            - list of multicast roots (if any - can be null)
     */
    public void ndlNetworkConnectionPath(Resource c, OntModel m, List<List<Resource>> path, List<Resource> roots);

}
