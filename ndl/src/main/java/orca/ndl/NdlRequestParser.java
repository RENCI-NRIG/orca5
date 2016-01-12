package orca.ndl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;

/** 
 * Query NDL request for known salient features and invoke callbacks
 * of the INdlRequestModelListener object to do something with them
 * @author ibaldin
 *
 */
public class NdlRequestParser extends NdlParserHelper {
	public static final String USER_REQUEST_RULES_FILE_PROPERTY = "NDL_REQUEST_RULE_FILE";
	private static final String RULES_FILE = "orca/ndl/rules/requestRules.rules";
	INdlRequestModelListener listener;
	INdlColorRequestListener colorListener = null;
	OntModel requestModel;
	Set<Resource> interfaces = new HashSet<Resource>();
	Set<Resource> nodesAndLinks = new HashSet<Resource>();
	
	protected String[] inferenceModels = { "topology.owl", "layer.owl", "ethernet.owl", "compute.owl", "exogeni.owl", "storage.owl", "geni.owl", "eucalyptus.owl", "planetlab.owl", "protogeni.owl", "ec2.owl" };
	
	/**
	 * Create a parser on existing model
	 * @param reqModel
	 * @param l
	 * @throws NdlException
	 */
	public NdlRequestParser(OntModel reqModel, INdlRequestModelListener l) throws NdlException {
		listener = l;
		requestModel = reqModel;
		
		// need some imports for inference to work
		for (String model: inferenceModels)
			requestModel.read(ORCA_NS + model);
	}
	
	/**
	 * Create a parser with a model that is explicitly in-memory or TDB persistent or ephemeral
	 * @param ndlRequest
	 * @param l
	 * @param t
	 * @param folderName
	 * @throws NdlException
	 */
	public NdlRequestParser(OntModel reqModel, INdlRequestModelListener l, NdlModel.ModelType t, String folderName) throws NdlException {
		
		if (l == null)
			throw new NdlException("Null parameters to the NdlRequestParser constructor");
		
		listener = l;
		
		requestModel = reqModel;
		// need some imports for inference to work
		for (String model: inferenceModels)
			requestModel.read(ORCA_NS + model);
	}
	
	/**
	 * Create a request parser with in-memory model
	 * @param ndlRequest
	 * @param l
	 * @throws NdlException
	 */
	public NdlRequestParser(String ndlRequest, INdlRequestModelListener l) throws NdlException {
		
		if ((ndlRequest == null) || (l == null))
			throw new NdlException("Null parameters to the NdlRequestParser constructor;ndlRequest="+ndlRequest+";l="+l);
		
		listener = l;
		
		ByteArrayInputStream modelStream = new ByteArrayInputStream(ndlRequest.getBytes());
		
		//requestModel = OntProcessor.getModelFromString(ndlRequest);
		
		// create a model with inference and unique document manager and TDB
		//requestModel = getRequestModelFromStream(modelStream, OntModelSpec.RDFS_MEM_RDFS_INF, true);
		requestModel = NdlModel.getRequestModelFromStream(modelStream, OntModelSpec.OWL_MEM_RDFS_INF, true);
		
		// need some imports for inference to work
		for (String model: inferenceModels)
			requestModel.read(ORCA_NS + model);
	}

	/**
	 * Create a parser with a model that is explicitly in-memory or TDB persistent or ephemeral
	 * @param ndlRequest
	 * @param l
	 * @param t
	 * @param folderName
	 * @throws NdlException
	 */
	public NdlRequestParser(String ndlRequest, INdlRequestModelListener l, NdlModel.ModelType t, String folderName) throws NdlException {
		
		if ((ndlRequest == null) || (l == null))
			throw new NdlException("Null parameters to the NdlRequestParser constructor");
		
		listener = l;
		
		ByteArrayInputStream modelStream = new ByteArrayInputStream(ndlRequest.getBytes());
		
		//requestModel = OntProcessor.getModelFromString(ndlRequest);
		
		// create a model with inference and unique document manager and optional TDB
		//requestModel = getRequestModelFromStream(modelStream, OntModelSpec.RDFS_MEM_RDFS_INF, true);
		requestModel = NdlModel.getRequestModelFromStream(modelStream, OntModelSpec.OWL_MEM_RDFS_INF, true, t, folderName);
		
		// need some imports for inference to work
		for (String model: inferenceModels)
			requestModel.read(ORCA_NS + model);
	}
	
	public void addColorListener(INdlColorRequestListener ci) {
		colorListener = ci;
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
	 * Query the request invoking the listener callbacks
	 * @throws NdlException
	 */
	public synchronized void processRequest() throws NdlException {
		if (requestModel == null)
			return;
		
		if (!lessStrictChecking) {
			validateRequest(RULES_FILE, USER_REQUEST_RULES_FILE_PROPERTY, requestModel);
		}
		
		List<Resource> resources = null;
 		
		// reservation query from which everything flows
		String query = OntProcessor.createQueryStringReservationTerm();
		ResultSet rs = OntProcessor.rdfQuery(requestModel, query);
        //outputQueryResult(rs);
        //rs = OntProcessor.rdfQuery(requestModel, query);
        
		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource res = (Resource)result.get("reservation");
			{
				// process sub-slices
				for(StmtIterator sliceEl = res.listProperties(geniSliceProperty); sliceEl.hasNext();) {
					Resource tmpR = sliceEl.next().getResource();
					// FIXME: need inference to check the class (subclass of geni:Slice)
					listener.ndlSlice(tmpR, requestModel);
				}
			}
			
			{
				// reservation
				listener.ndlReservation(res, requestModel);
				// start date
				Literal sd = (Literal)result.get("beginningTime");
				listener.ndlReservationStart(sd, requestModel, getTermDate(sd));
				
				// end date
				Literal ed = (Literal)result.get("endTime");
				listener.ndlReservationEnd(ed, requestModel, getTermDate(ed));

				// term duration
				Resource term = (Resource)result.get("term");
				
				Literal yr = (Literal)result.get("years");
				Literal mo = (Literal)result.get("months");
				Literal day = (Literal)result.get("days");
				Literal hr = (Literal)result.get("hours");
				Literal min = (Literal)result.get("minutes");
				Literal sec = (Literal)result.get("seconds");

				listener.ndlReservationTermDuration(term, requestModel, getLiteralInt(yr), 
						getLiteralInt(mo), getLiteralInt(day), getLiteralInt(hr), 
						getLiteralInt(min), getLiteralInt(sec));
			}
			
			{
				// process compute elements explicitly named within reservation
				resources = new ArrayList<Resource>();
				for (StmtIterator resEl = res.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource tmpR = resEl.next().getResource();
					resources.add(tmpR);
					// if node, call for node (we do all nodes first, then all connections)
					if (tmpR.hasProperty(RDF_TYPE, computeElementClass) 
							|| tmpR.hasProperty(RDF_TYPE, serverCloudClass) 
							|| tmpR.hasProperty(RDF_TYPE, deviceOntClass)
							|| tmpR.hasProperty(RDF_TYPE, networkStorageClass)) {
		
						Resource ceClass = getResourceType(tmpR);  // CE, ServerCloud etc
						if (ceClass == null)
							ceClass = computeElementClass; // default
						
						// don't insert repeats
						if (!checkNodeOrLink(tmpR)) 
							continue;
						listener.ndlNode(tmpR, requestModel, ceClass, getResourceInterfaces(tmpR));
						
						// add all its interfaces to the set
						interfaces.addAll(getResourceInterfaces(tmpR));
					}
				}
			}
			
			{
				// now look for nodes hanging off network connections that we may have missed
				for (StmtIterator resEl = res.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource tmpR = resEl.next().getResource();
					// if a connection
					if (tmpR.hasProperty(RDF_TYPE, topologyNetworkConnectionClass) || 
							tmpR.hasProperty(RDF_TYPE, topologyBroadcastConnectionClass)) {
						// get the interfaces and what is attached to them 
						for (Resource intF: getResourceInterfaces(tmpR)) {
							
							for (Resource attached: getWhoHasInterface(intF, requestModel)) {
								// if not the link, may be a node
								if (!attached.equals(tmpR)) {
									if (attached.hasProperty(RDF_TYPE, computeElementClass) 
											|| attached.hasProperty(RDF_TYPE, serverCloudClass)
											|| attached.hasProperty(RDF_TYPE, deviceOntClass)
											|| attached.hasProperty(RDF_TYPE, networkStorageClass)) {
										Resource ceClass = getResourceType(attached);  // CE, ServerCloud etc
										if (ceClass == null)
											ceClass = computeElementClass; // default
										
										// don't insert repeats
										if (!checkNodeOrLink(attached)) 
											continue;
										listener.ndlNode(attached, requestModel, ceClass, getResourceInterfaces(attached));
										
										// add all its interfaces to the set
										interfaces.addAll(getResourceInterfaces(attached));
									}
								}
							}
						}
					}
				}
			}
			
			{
				// process dependencies (separately to be sure we know all nodes first)
				for (StmtIterator resEl = res.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource tmpR = resEl.next().getResource();
					Set<Resource> nodeDeps = new HashSet<Resource>();
					for(StmtIterator depsIt = tmpR.listProperties(requestDependOnProperty); depsIt.hasNext();) {
						Resource depR = depsIt.next().getResource();
						if (depR != null) 
							nodeDeps.add(depR);
					}
					listener.ndlNodeDependencies(tmpR, requestModel, nodeDeps);
				}
			}
			
			{
				// connections (p-to-p and broadcast)
				for (StmtIterator resEl = res.listProperties(collectionElementProperty); resEl.hasNext();) {
					Resource tmpR = resEl.next().getResource();
					// if a connection
					if (tmpR.hasProperty(RDF_TYPE, topologyNetworkConnectionClass)) {
						// don't insert if seen before
						if (!checkNodeOrLink(tmpR)) 
							continue;
						
						listener.ndlNetworkConnection(tmpR, requestModel, getResourceBandwidth(tmpR), 
								getResourceLatency(tmpR), getResourceInterfaces(tmpR));
						// add all its interfaces
						interfaces.addAll(getResourceInterfaces(tmpR));
					} else if (tmpR.hasProperty(RDF_TYPE, topologyBroadcastConnectionClass)) {
						if (!checkNodeOrLink(tmpR))
							continue;
						
						listener.ndlBroadcastConnection(tmpR, requestModel, getResourceBandwidth(tmpR), getResourceInterfaces(tmpR));
						interfaces.addAll(getResourceInterfaces(tmpR));
					}
				}
			}

			{
				// now all interface details (IP/netmask)
				for (Iterator<Resource> it = interfaces.iterator(); it.hasNext(); ) {
					Resource tmpR = it.next();
					
					// run a query on what is connected to this interface
					query = OntProcessor.createQueryStringWhoHasInterface(tmpR);
					rs = OntProcessor.rdfQuery(requestModel, query);
					if ((rs == null) || (!rs.hasNext()))
						continue;
					result = (ResultBinding)rs.next();
					Resource conn = null, node = null;
					if (result != null) {
						Resource u = (Resource)result.get("item");
						if ((u.hasProperty(RDF_TYPE, topologyNetworkConnectionClass)) ||
								(u.hasProperty(RDF_TYPE, topologyBroadcastConnectionClass)))
							conn = u;
						else
							node = u;
					}
					// some nodes may have an interface without a link
					if (rs.hasNext()) {
						result = (ResultBinding)rs.next();
						if (result != null) {
							Resource u = (Resource)result.get("item");
							if ((u.hasProperty(RDF_TYPE, topologyNetworkConnectionClass)) ||
									(u.hasProperty(RDF_TYPE, topologyBroadcastConnectionClass)))
								conn = u;
							else
								node = u;
						}
					}
					// retrieve IP address, netmask (may need to turn inference on and
					// do the inverse of hasInterface if we need a list of what it connects to)
					// does it have localIPAddress?
					Statement locIPStmt = tmpR.getProperty(ip4LocalIPAddressProperty);
					
					// this can probably be solved with cardinality restrictions, but we can't
					// have those on non-simple properties like hasInterface
					
					/** this doesn't pass multi-stitchport on the same URL
					 * disabling for now /ib 02/20/15
					 */
					/*
					if (!lessStrictChecking) {
						if (conn == null)
							throw new NdlException("Interface " + tmpR + " is not part of a link or connection");
						if (node == null)
							throw new NdlException("Interface " + tmpR + " is not part of a node");
					}
					*/
					if (locIPStmt != null) {
						listener.ndlInterface(tmpR, requestModel, conn, node, 
								getAddressIP(locIPStmt.getResource()), 
								getAddressNetmask(locIPStmt.getResource()));
					} else
						listener.ndlInterface(tmpR, requestModel, conn, node, 
								null, null);
				}
			}
			
			// all resources attached to the reservation
			listener.ndlReservationResources(resources, requestModel);
			
			if (colorListener != null) {
				// colors attached to individual resources
				query = NdlCommons.createQueryStringHasColor();
				rs = NdlCommons.rdfQuery(requestModel, query);
				
				while(rs.hasNext()) {
					result = (ResultBinding)rs.next();
					Resource ne = (Resource)result.get("netelement");
					Resource color = (Resource)result.get("color");
										
					if ((ne != null) && (color != null)) {
						String label = NdlCommons.getLiteralProperty(color, NdlCommons.hasColorLabel);
						colorListener.ndlResourceColor(ne, color, label);
					}
				}
				
				// color links between resources
				query = NdlCommons.createQueryStringColorDependency();
				rs = NdlCommons.rdfQuery(requestModel, query);
				
				while(rs.hasNext()) {
					result = (ResultBinding)rs.next();
					Resource fromNe = (Resource)result.get("fromNe");
					Resource toNe = (Resource)result.get("toNe");
					Resource color = (Resource)result.get("color");
					
					if ((fromNe != null) && (toNe != null) && (color != null)) {
						String label = NdlCommons.getLiteralProperty(color, NdlCommons.hasColorLabel);
						colorListener.ndlColorDependency(fromNe, toNe, color, label);
					}
				}
			}
			// signal the end
			listener.ndlParseComplete();
		}
	}
	
	public synchronized void freeModel() {
		NdlModel.closeModel(requestModel);
		requestModel = null;
	}
}
