package orca.ndl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import orca.ndl.elements.NdlPath;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;

/**
 * Query NDL Manifest model for salient features, invoking callbacks
 * of the INdlManifestModelListener
 * @author ibaldin
 *
 */
public class NdlManifestParser extends NdlCommons {
	INdlManifestModelListener listener;
	OntModel manifestModel;
	Set<Resource> nodesAndLinks = new HashSet<Resource>();
	
	public NdlManifestParser(OntModel ndlManifestModel, INdlManifestModelListener l) throws NdlException {
		if ((ndlManifestModel == null) || (l == null))
			throw new NdlException("Null parameters to the NdlManifestParser constructor");
		
		listener = l;
		// by default use new document manager and TDB
		manifestModel = ndlManifestModel;
	}
	
	public NdlManifestParser(String ndlManifest, INdlManifestModelListener l) throws NdlException {
		if ((ndlManifest == null) || (l == null))
			throw new NdlException("Null parameters to the NdlManifestParser constructor");
		
		listener = l;
		// by default use new document manager and TDB
		manifestModel = NdlModel.getModelFromString(ndlManifest, null, true);
	}
	
	public NdlManifestParser(String ndlManifest, INdlManifestModelListener l, NdlModel.ModelType t, String folderName) throws NdlException {
		if ((ndlManifest == null) || (l == null))
			throw new NdlException("Null parameters to the NdlManifestParser constructor");
		
		listener = l;
		// by default use new document manager and TDB
		manifestModel = NdlModel.getModelFromString(ndlManifest, null, true, t, folderName);
	}

	/**
	 * Return true if a new element, false otherwise
	 * @param r
	 * @return
	 */
	private boolean checkNodeOrLink(Resource r) {
		return nodesAndLinks.add(r);
	}
	
	/**
	 * You will see things in the following order (never twice):
	 * - Separate nodes and NetworkConnections
	 * 	 - Nodes or Crossconnects within NetworkConnection
	 *   - LinkConnections within NetworkConnection
	 *   - path of network connection
	 * - Stand-alone LinkConnections
	 * - Interfaces
	 */
	public synchronized void processManifest() throws NdlException {
		if (manifestModel == null)
			return;
		
		Set<Resource> interfaces = new HashSet<Resource>();
		
		// reservation query from which everything flows
		String query = NdlCommons.createQueryStringManifestDetails();
		ResultSet rs = OntProcessor.rdfQuery(manifestModel, query);
		
		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource man = (Resource)result.get("manifest");
			{
				// manifest
				listener.ndlManifest(man, manifestModel);
				
				// FIXME: do we need the term? 
			}
			
			{
				
				// look for collections:elements, which currently (10/28/11) can be NetworkConnections (for inter-domain)
				// nodes and LinkConnections (for intra-domain)
				for (StmtIterator resEl = man.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource resourceElement = resEl.next().getResource();
					//if (resourceElement.hasProperty(RDF_TYPE, computeElementClass) || resourceElement.hasProperty(RDF_TYPE, serverCloudClass)) {
					if (resourceElement.hasProperty(RDF_TYPE, computeElementClass) &&
							(resourceElement.hasProperty(domainHasResourceTypeProperty, vmResourceTypeClass) ||
							 resourceElement.hasProperty(domainHasResourceTypeProperty, bmResourceTypeClass) ||
							 resourceElement.hasProperty(domainHasResourceTypeProperty, fourtygbmResourceTypeClass))) {
									// resourceElement.hasProperty(specificCEProperty))) {
						Resource ceClass = getResourceType(resourceElement);  // CE, ServerCloud etc
						if (ceClass == null)
							ceClass = computeElementClass; // default
						
						if (!checkNodeOrLink(resourceElement)) 
							continue;
						
						listener.ndlNode(resourceElement, manifestModel, ceClass, getResourceInterfaces(resourceElement));
						
						// add all its interfaces to the set
						interfaces.addAll(getResourceInterfaces(resourceElement));
					}
					
					if (NdlCommons.isNetworkStorage(resourceElement)) {
						Resource ceClass = null;
						listener.ndlNode(resourceElement, manifestModel, ceClass, getResourceInterfaces(resourceElement));
						
						interfaces.addAll(getResourceInterfaces(resourceElement));
					}
									
					// Deal with Network Connections
					// as of 08/30/13 network connections may have a DEVICE with multicast capability
					// which makes them star-like /ib
					if (resourceElement.hasProperty(RDF_TYPE, topologyNetworkConnectionClass)) {
						
						if (!checkNodeOrLink(resourceElement))
							continue;
						
						listener.ndlNetworkConnection(resourceElement, manifestModel, getResourceBandwidth(resourceElement), 
								getResourceLatency(resourceElement), getResourceInterfaces(resourceElement));
						NdlPath npath = new NdlPath();
						
						// add all its interfaces
						interfaces.addAll(getResourceInterfaces(resourceElement));
						
						Set<Resource> stitchInterfaces = new HashSet<Resource>();
						
						// process items within each NetworkConnection. node-like things first
						for (StmtIterator conItemIter = resourceElement.listProperties(collectionItemProperty); conItemIter.hasNext();) {
							Resource conItem = conItemIter.next().getResource();
							
							// catch crossconnects only (nodes and clouds are linked separately)
							if (conItem.hasProperty(RDF_TYPE, topologyCrossConnectClass)) {
								npath.addElement(conItem);
							}
							
							// roots of multicast domains are also nodes
							if (NdlCommons.isMulticastDevice(conItem)) {
								npath.addRoot(conItem);
								listener.ndlNode(conItem, manifestModel, deviceOntClass, NdlCommons.getResourceInterfaces(conItem));
								continue;
							}
							
							// catch stitching nodes
							if (NdlCommons.isStitchingNodeInManifest(conItem)) {
								// collect interfaces of this stitch node. below when
								// we look at links, we force links adjacent to stitch
								// nodes to be endpoints of paths
								stitchInterfaces.addAll(NdlCommons.getResourceInterfaces(conItem));
								listener.ndlNode(conItem, manifestModel, deviceOntClass, NdlCommons.getResourceInterfaces(conItem));
								// with stitch port paths, instead of ending on a penultimate vlan (next to node), we end with the 
								// stitchport 09/10/14 /ib
								npath.addEndElement(conItem);
								continue;
							}
							
							// either a compute element, a crossconnect or LinkConnection or ?
							// only deal with nodes first. A node group with count of 1 is just a node.
							if ((conItem.hasProperty(RDF_TYPE, computeElementClass) && 
									!conItem.hasProperty(RDF_TYPE, serverCloudClass)) || 
									//conItem.hasProperty(RDF_TYPE, serverCloudClass) ||
									conItem.hasProperty(RDF_TYPE, topologyCrossConnectClass) ||
									(conItem.hasProperty(RDF_TYPE, serverCloudClass) && 
											conItem.hasLiteral(numCEProperty, 1))) {
								
								Resource ceClass = getResourceType(conItem);
								if (ceClass == null)
									ceClass = computeElementClass; // default

								if (!checkNodeOrLink(conItem)) 
									continue;
								
								if (ceClass.equals(topologyCrossConnectClass))
									listener.ndlCrossConnect(conItem, manifestModel,  
											getResourceBandwidth(conItem), getResourceLabel(conItem), 
											getResourceInterfaces(conItem), resourceElement);
								else 
									listener.ndlNode(conItem, manifestModel, ceClass, getResourceInterfaces(conItem));
								
								interfaces.addAll(getResourceInterfaces(conItem));
							}
						}
						
						// now link-like things
						for (StmtIterator conItemIter = resourceElement.listProperties(collectionItemProperty); conItemIter.hasNext();) {
							Resource conItem = conItemIter.next().getResource();
							
							// either a LinkConnection or ?
							if (conItem.hasProperty(RDF_TYPE, topologyLinkConnectionClass)) {
								npath.addElement(conItem);
								
								//if (!checkNodeOrLink(conItem))
								//	continue;
								listener.ndlLinkConnection(conItem, manifestModel, getResourceInterfaces(conItem), resourceElement);
								
								interfaces.addAll(getResourceInterfaces(conItem));
							}
						}
						
						// now we have the path(s)
						listener.ndlNetworkConnectionPath(resourceElement, manifestModel, npath.getPaths(), npath.getRoots());
					}
				}
				
				for (StmtIterator resEl = man.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource resourceElement = resEl.next().getResource();
					if (resourceElement.hasProperty(RDF_TYPE, topologyLinkConnectionClass)) {
						//if (!checkNodeOrLink(resourceElement))
						//	continue;
						
						listener.ndlLinkConnection(resourceElement, manifestModel, getResourceInterfaces(resourceElement), null);
						
						interfaces.addAll(getResourceInterfaces(resourceElement));
					}
				}
			}
			
			{
				// now all interface details (IP/netmask)
				for (Iterator<Resource> it = interfaces.iterator(); it.hasNext(); ) {
					Resource tmpR = it.next();
					// run a query on what is connected to this interface
					// FIXME: turn on inference and check that one is a link and the 
					// other is a node
					query = NdlCommons.createQueryStringWhoHasInterface(tmpR);
					rs = OntProcessor.rdfQuery(manifestModel, query);
					if ((rs == null) || (!rs.hasNext())) {
						continue;
					}
					Resource conn = null, node = null;
					while(rs.hasNext()) {
						result = (ResultBinding)rs.next();
						if (result != null) {
							Resource u = (Resource)result.get("item");
							if (u.hasProperty(RDF_TYPE, topologyNetworkConnectionClass) &&
									!u.hasProperty(RDF_TYPE, topologyLinkConnectionClass))
								continue;
							if (u.hasProperty(RDF_TYPE, topologyLinkConnectionClass)
									|| u.hasProperty(RDF_TYPE, topologyCrossConnectClass)) {
								if (conn == null)
									conn = u;
							} else {
								if (node == null)
									node = u;
							}
						}
					}

					// retrieve IP address, netmask (may need to turn inference on and
					// do the inverse of hasInterface if we need a list of what it connects to)
					// does it have localIPAddress?
					Statement locIPStmt = tmpR.getProperty(ip4LocalIPAddressProperty);
					if (locIPStmt != null) {
						listener.ndlInterface(tmpR, manifestModel, conn, node, 
								getAddressIP(locIPStmt.getResource()), 
								getAddressNetmask(locIPStmt.getResource()));
					} else
						listener.ndlInterface(tmpR, manifestModel, conn, node, 
								null, null);
				}

			}
					
			// signal the end
			listener.ndlParseComplete();
		}
		
	}	
	
	/**
	 * Explicitly tear down the model
	 */
	public synchronized void freeModel() {
		NdlModel.closeModel(manifestModel);
		manifestModel = null;
	}
}
