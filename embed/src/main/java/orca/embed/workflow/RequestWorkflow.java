package orca.embed.workflow;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.cloudembed.MappingHandler;
import orca.embed.cloudembed.controller.CloudHandler;
import orca.embed.cloudembed.controller.InterDomainHandler;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.ModifyElement;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.NdlModel.ModelType;
import orca.ndl.NdlModifyParser;
import orca.ndl.NdlRequestParser;
import orca.ndl.OntProcessor;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.ndl.elements.RequestSlice;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.tdb.TDB;

/**
 * This object implements the generic workflow of turning a request
 * into Orca reservations.
 * @author ibaldin
 *
 */
public class RequestWorkflow {
	/**
	 * This interface is used to pass in objects that need things set 
	 * upon recovery
	 * @author geni-orca
	 *
	 */
	public static interface WorkflowRecoverySetter {
		public String getId();
	}
	
	// done
	protected RequestSlice slice;
	// TDB - done
	protected OntModel requestModel = null, manifestModel = null, tmpModifyModel = null;
	// restored from manifest model
	protected OrcaReservationTerm term;
	// typically InterCloudHandler, no need to restore, except it has idm
	protected IRequestEmbedder embedderAlgorithm;
	
	// restored from manifest model
	protected Collection <NetworkElement> boundElements;
	// restored from manifest model
	protected LinkedList <OntResource> domainInConnectionList;
	// gets set at run time
	private HashMap<String,BitSet> globalControllerAssignedLabel;  //from/to XmlrpcOrcaState 
	// recover from reservations?
	private HashMap<String,BitSet> controllerAssignedLabel = new HashMap<String,BitSet>(); //local set

	private HashMap <String,LinkedList<String>> shared_IP_set; //non-persistent
	private HashMap <String,LinkedList<String>> controller_shared_IP_set; //non-persistent
	
	private Logger logger = NdlCommons.getNdlLogger();
	// no save
	SystemNativeError err;	
	/**
	 * Create a workflow based on a specific embedding algorithm
	 * and ndl model parser listener
	 * @param algm
	 */
	public RequestWorkflow(IRequestEmbedder algm) {
		assert(algm != null);
		NdlCommons.setGlobalJenaRedirections();
		embedderAlgorithm = algm;
	}

	public HashMap<String, BitSet> getGlobalControllerAssignedLabel() {
		return globalControllerAssignedLabel;
	}

	public void setGlobalControllerAssignedLabel(
			HashMap<String, BitSet> globalControllerAssignedLabel) {
		this.globalControllerAssignedLabel = globalControllerAssignedLabel;
	}
	
	public HashMap<String, LinkedList<String>> getShared_IP_set() {
		return shared_IP_set;
	}

	public void setShared_IP_set(HashMap<String, LinkedList<String>> shared_IP_set) {
		this.shared_IP_set = shared_IP_set;
	}

	public String getErrorMsg(){
		if(err!=null)
			return "Embedding workflow ERROR: " + err.getErrno()+":"+err.getMessage();
				
		return null;
	}
	/**
	 * Run the workflow
	 * @throws Exception 
	 */
	public synchronized SystemNativeError run(DomainResourcePools domainResourcePools,List<String> abstractModels,
			String resReq, String userDN, String controller_url, String sliceId) throws  NdlException, IOException {
		
		RequestParserListener parserListener = new RequestParserListener();		
		// run the parser (to create Java objects)
		NdlRequestParser nrp = new NdlRequestParser(resReq, parserListener, NdlModel.ModelType.TdbPersistent, 
				Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "request-" + sliceId);
		nrp.processRequest();		
		RequestReservation request = parserListener.getRequest();
		requestModel = request.getModel();
		slice=request.getSlice();
		Collection<NetworkElement> requestElements = request.getElements();		
		err = request.getError();
		if(err!=null){
			logger.error("Ndl request parser unable to parse request:"+err.toString());
			return err;
		}
		// TODO: check if the request is already fully bound in one domain, preparing for topology splitting 				
		boolean bound = request.generateGraph(requestElements);
		String reservationDomain = request.getReservationDomain();
		term=request.getTerm();
		((CloudHandler)embedderAlgorithm).addSubstrateModel(abstractModels);
		
        OntModelSpec s = NdlModel.getOntModelSpec(OntModelSpec.OWL_MEM, true);

        try {
			manifestModel = NdlModel.createModel(s, true, NdlModel.ModelType.TdbPersistent,
	        		Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "manifest-" + sliceId);
		} catch (NdlException e1) {
			logger.error("ModifyHandler.createManifest(): Unable to create a persistent model of manifest");
		}
        manifestModel.add(request.getModel().getBaseModel());
		((CloudHandler) embedderAlgorithm).setManifestModel(manifestModel);
		
		((CloudHandler) embedderAlgorithm).setControllerAssignedLabel(controllerAssignedLabel);
		((CloudHandler) embedderAlgorithm).setGlobalControllerAssignedLabel(globalControllerAssignedLabel);	
		((CloudHandler) embedderAlgorithm).setShared_IP_set(shared_IP_set);
		if(reservationDomain == null){// invoke the embedding code
			err = embedderAlgorithm.runEmbedding(bound, request, domainResourcePools);
		}else{  //intra-domain embedding
			err = embedderAlgorithm.runEmbedding(reservationDomain,request,domainResourcePools);
		}
		modifyGlobalControllerAssignedLabel();
		boundElements = ((CloudHandler) embedderAlgorithm).getDeviceList();
		staticLabelDependency();
		
		manifestModel = ((CloudHandler) embedderAlgorithm).createManifest(boundElements, request, userDN, controller_url, sliceId);
		domainInConnectionList = ((CloudHandler) embedderAlgorithm).getDomainInConnectionList();
		
		//TDB.sync(requestModel);
		//closeCreateModel();
		
		return err;
	}
	
	@SuppressWarnings("unchecked")
	public void staticLabelDependency(){
		LinkedList <NetworkElement> con_elements = ((CloudHandler) embedderAlgorithm).getDeviceList();
		String e_domain=null;
		LinkedList <NetworkElement> elements = null;
		HashMap <String, LinkedList <NetworkElement>> domainCount = new HashMap <String, LinkedList <NetworkElement>>();
		if(con_elements.size()>0){
			for(NetworkElement e:con_elements){
				e_domain = e.getInDomain();
				if(e_domain==null){
					domainCount = new HashMap <String, LinkedList <NetworkElement>>();
					break;
				}
				if(domainCount.containsKey(e_domain)){
					domainCount.get(e_domain).add(e);
				}else{
					elements = new LinkedList <NetworkElement>();
					elements.add(e);
					domainCount.put(e_domain, elements);
				}
			}
		}
		//make network reservations without static tag (local control picks) depending on reservations w/ static tag to avoid tag mismatch
		for(Entry <String, LinkedList <NetworkElement>> entry: domainCount.entrySet()){
			elements = entry.getValue();
			DomainElement de = (DomainElement) elements.get(0);
			LinkedList <DomainElement> static_elements = new LinkedList <DomainElement>();
			LinkedList <DomainElement> free_elements = new LinkedList <DomainElement>();
			if(elements.size()>1 && (de.getCe() == null || (de.getCastType()!=null && de.getCastType().equalsIgnoreCase(NdlCommons.multicast))  )){ //network domain with >1 reservations
				for(NetworkElement dee:elements){
					de = (DomainElement) dee;
					if(de.getStaticLabel()>0)
						static_elements.add(de);
					else
						free_elements.add(de);
				}
				for(DomainElement fde:free_elements){
					for(DomainElement sde:static_elements){
						fde.setPrecededBy(sde, null);
						logger.debug("RequestWorkflow: adding static tag dependency for network reservations: domain="+entry.getKey()+":"+sde.getStaticLabel());
					}
				}
			}
		}
		return;
	}
	
	public synchronized void modify(DomainResourcePools domainResourcePools, String modReq, String sliceId,
			HashMap <String,Collection <DomainElement>> nodeGroupMap, 
			HashMap<String, DomainElement> firstGroupElement) throws NdlException, UnknownHostException, InetNetworkException{
		logger.info("workflow:modify(); starts...");
		ModifyParserListener pl = new ModifyParserListener();
		NdlModifyParser nmp = new NdlModifyParser(modReq,pl);
		//nmp.rewriteModifyRequest();
		nmp.processModifyRequest();
		
		((CloudHandler) embedderAlgorithm).setControllerAssignedLabel(controllerAssignedLabel);
		((CloudHandler) embedderAlgorithm).setGlobalControllerAssignedLabel(globalControllerAssignedLabel);	
		((CloudHandler) embedderAlgorithm).setShared_IP_set(shared_IP_set);
		
		Collection <ModifyElement> modifyElements = pl.getModifyElements();
		
		// delete previous modify model (if any)
		if (tmpModifyModel != null) {
			NdlModel.closeModel(tmpModifyModel);
		}
		
		// create new  ephemeral modify request model in Java tmp space
		tmpModifyModel = NdlModel.createModel(OntModelSpec.OWL_MEM_RDFS_INF, true, ModelType.TdbEphemeral, null);
		manifestModel.add(tmpModifyModel);
		err=((MappingHandler) embedderAlgorithm).modifySlice(domainResourcePools,modifyElements, manifestModel,sliceId, nodeGroupMap, firstGroupElement, requestModel, tmpModifyModel);
		
		modifyGlobalControllerAssignedLabel();
		boundElements = ((CloudHandler) embedderAlgorithm).getDeviceList();
		staticLabelDependency();
		domainInConnectionList = ((CloudHandler) embedderAlgorithm).getDomainInConnectionList();
		
		nmp.freeModel();
		//TDB.sync(manifestModel);
	}
	
	public void setSliceName(String urn, String uuid,String dn){
		String query = OntProcessor.createQueryStringReservationTerm();
		ResultSet rs = OntProcessor.rdfQuery(manifestModel, query);
		
		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource res = (Resource)result.get("reservation");
			
			res.addProperty(NdlCommons.topologyHasName, urn);
			res.addProperty(NdlCommons.hasGUIDProperty, uuid);
			res.addProperty(NdlCommons.hasDNProperty,dn);
		}
	}
	
	//close the Jena models
	public void closeModel(){		
		HashMap <String,OntModel> domainModelMap = ((CloudHandler) embedderAlgorithm).getDomainModel();
		if(domainModelMap!=null){
			for(Entry <String,OntModel> entry:domainModelMap.entrySet())
				NdlModel.closeModel(entry.getValue());
		}
		
		NdlModel.closeModel(((MappingHandler) embedderAlgorithm).getIdm());
	}
	
	//close the Jena models
	public void close(){		
		if (requestModel != null) {
			NdlModel.closeModel(requestModel);
			requestModel = null;
		}
		if (manifestModel != null) {
			NdlModel.closeModel(manifestModel);
			manifestModel = null;
		}
		if (((MappingHandler) embedderAlgorithm).getIdm() != null) 
			closeModel();
		
//		if (((ModifyHandler) embedderAlgorithm).getModifyRequestModelList() != null) 
//			for(OntModel model:((ModifyHandler) embedderAlgorithm).getModifyRequestModelList())
//				NdlModel.closeModel(model);
	}
	
	protected void modifyGlobalControllerAssignedLabel(){
		if(globalControllerAssignedLabel==null){
			logger.error("modifyGlobalControllerAssignedLabel: the map variable not set");
			return;
		}
		String domain=null;
		BitSet bitSet=null,globalBitSet=null;
		for(Entry<String, BitSet> entry:controllerAssignedLabel.entrySet()){
			domain = entry.getKey();
			bitSet=entry.getValue();
			if(globalControllerAssignedLabel.containsKey(domain)){
				bitSet = entry.getValue();
				globalBitSet=globalControllerAssignedLabel.get(domain);
				globalBitSet.or(bitSet);
			}else{
				globalBitSet= new BitSet(InterDomainHandler.max_vlan_tag);
				globalBitSet.or(bitSet);
				globalControllerAssignedLabel.put(domain, globalBitSet);
			}
			logger.debug("modifyGlobalLabel:"+domain+":assignedLabel="+globalControllerAssignedLabel.get(domain)+":controllerLabel="+controllerAssignedLabel.get(domain));
		}
	}
	
	public void clearGlobalControllerAssignedLabel(){
		String domain=null;
		BitSet bitSet=null,globalBitSet=null;
		for(Entry<String, BitSet> entry: controllerAssignedLabel.entrySet()){
			domain = entry.getKey();
			bitSet = entry.getValue();
			logger.debug("ClearGlobalLabel:"+domain+";controller bitset="+bitSet);
			if(globalControllerAssignedLabel.containsKey(domain)){
				globalBitSet = globalControllerAssignedLabel.get(domain);
				globalBitSet.andNot(bitSet);
				logger.debug("ClearGlobalLabel:assignedLabel="+globalControllerAssignedLabel.get(domain)+":removed controllerLabel="+controllerAssignedLabel.get(domain));
			}
		}
	}	
	
	public void clearSharedIPSet(){
		if(controller_shared_IP_set==null || shared_IP_set==null)
			return;
		for(Entry<String,LinkedList<String>> entry:controller_shared_IP_set.entrySet()){
			LinkedList<String> shared_set = shared_IP_set.get(entry.getKey());
			if(shared_set!=null)
				shared_set.removeAll(entry.getValue());
		}
	}
	
	public void modifyTerm(Date newEndDate){
		term.modifyTerm(newEndDate);
	}
	
	public IRequestEmbedder getEmbedderAlgorithm() {
		return embedderAlgorithm;
	}

	public void setEmbedderAlgorithm(IRequestEmbedder embedderAlgorithm) {
		this.embedderAlgorithm = embedderAlgorithm;
	}

	public Collection<NetworkElement> getBoundElements() {
		return boundElements;
	}

	public OrcaReservationTerm getTerm() {
		return term;
	}

	public OntModel getManifestModel() {
		return manifestModel;
	}

	public LinkedList<OntResource> getDomainInConnectionList() {
		return domainInConnectionList;
	}
	
	public RequestSlice getslice(){
		return slice;
	}
	
	/**
	 * Restore the workflow for a slice, that is passed in as WorkflowRecoverySetter interface
	 * @param logger
	 * @param slice
	 */
	public void recover(Logger logger, HashMap<String, BitSet> globalAssignedLabels, WorkflowRecoverySetter slice) {
		// open the models

		logger.info("Restoring request workflow for slice " + slice.getId());
		try {
			logger.info("Opening the request model " + Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "request-" + slice.getId());
			requestModel = NdlModel.getModelFromTDB(Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "request-" + slice.getId(), 
					NdlModel.getOntModelSpec(OntModelSpec.OWL_MEM_RDFS_INF, true));
			logger.info("Opening the manifest model " + Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "manifest-" + slice.getId());
			manifestModel = NdlModel.getModelFromTDB(Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "manifest-" + slice.getId(),
					NdlModel.getOntModelSpec(OntModelSpec.OWL_MEM, true));
			
			setGlobalControllerAssignedLabel(globalAssignedLabels);
			recover(logger, slice, requestModel, manifestModel);
		} catch (NdlException ne) {
			logger.error("Unable to reopen TDB model due to: " + ne.getMessage());
			ne.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param logger
	 * @param slice - an object like a slice but with limited API conforming to WorkflowRecoverySetter interface (optional, can be null)
	 * @param reqModel
	 * @param manModel
	 */
	public void recover(Logger logger, WorkflowRecoverySetter sliceSetter, OntModel reqModel, OntModel manModel) {
		requestModel = reqModel;
		manifestModel = manModel;

		try {
			// get boundElements
			ManifestParserListener parserListener = new ManifestParserListener(logger);
			parserListener.parse(manifestModel); // no need to close here - we need to keep manifest model open until the slice goes away
			
			boundElements=parserListener.getDeviceList();
			StringBuilder sb = new StringBuilder();
			for(NetworkElement ne: boundElements) {
				sb.append(ne.toString() + "***********");
			}
			logger.debug("Slice bound elements are " + sb);
			
			((CloudHandler)embedderAlgorithm).setDeviceList(boundElements);
			((CloudHandler) embedderAlgorithm).setManifestModel(manifestModel);
			
			// recover term
			term = new OrcaReservationTerm();
			term.setStart(parserListener.getCreationTime());
			term.modifyTerm(parserListener.getExpirationTime());
			logger.debug("Slice start date " + parserListener.getCreationTime() + " and end date " + parserListener.getExpirationTime());
			
			domainInConnectionList = recoverDomainInConnectionList();
			((CloudHandler)embedderAlgorithm).setDomainInConnectionList(domainInConnectionList);
			
			logger.debug("Recovered the following domains for the slice:");
			for(OntResource orr: domainInConnectionList) {
				logger.debug("--- " + orr.getLocalName() + " " + orr);
			}
			
			// recover OF properties
			slice = parserListener.getOFSlice();
			
			// recover stitchport labels
			Map<String, Integer> spl = parserListener.getStitchPortLabels();
			for(Entry<String, Integer> e: spl.entrySet()) {
				setControllerLabel(e.getKey(), e.getValue());
				setBitsetLabel(globalControllerAssignedLabel, e.getKey(), e.getValue());
			}
			
			//recover IP address assignment on the shared vlan (storage LUN)
			HashMap <String,LinkedList<String>> IP_set = parserListener.getShared_IP_set();
			if(IP_set!=null){
				controller_shared_IP_set=new HashMap <String,LinkedList<String>>();
				for(Entry<String,LinkedList<String>> entry:IP_set.entrySet()){
					logger.debug("workflow recover:domain="+entry.getKey());
					for(String shared_ip_str:entry.getValue()){
						logger.debug("workflow recover:ip="+shared_ip_str);
					}
					controller_shared_IP_set.put(entry.getKey(),entry.getValue());
					if(shared_IP_set.get(entry.getKey())!=null){
						shared_IP_set.get(entry.getKey()).addAll(entry.getValue());
					}else{
						shared_IP_set.put(entry.getKey(), entry.getValue());
					}
				}
			}
			
		} catch (NdlException ne) {
			logger.error("Unable to reopen TDB model due to: " + ne);
			ne.printStackTrace();
		} finally {
			if (reqModel != null)
				TDB.sync(reqModel);
			
			if (manModel != null)
				TDB.sync(manModel);
		}
	}
	
	public LinkedList <OntResource> recoverDomainInConnectionList(){
		LinkedList <OntResource> domainList = new LinkedList<OntResource>();
		OntResource ne_ont=null,domain_ont=null;
		logger.info("Start to recover domainInConnectionList...");
		for (NetworkElement ne:boundElements){
			ne_ont = ne.getResource();
			if(ne_ont==null){
				logger.error("No ontResource, ne="+ne.getName());
				continue;
			}
			domainList.add(ne_ont);
			logger.debug("domain:"+ne_ont.getURI()+";");
			/*if(ne_ont.hasProperty(NdlCommons.inDomainProperty)){
				domain_ont =manifestModel.getOntResource(ne_ont.getProperty(NdlCommons.inDomainProperty).getResource());
				domainList.add(domain_ont);
				logger.debug("domain:"+domain_ont.getURI()+";");
			}else{
				logger.error("No inDomain property:ne="+ne_ont.getURI());
			}*/
		}
		return domainList;
	}
	
	/**
	 * Set controller label for this domain
	 * @param domain
	 * @param label
	 */
	public void setControllerLabel(String domain, int label) {
		logger.debug("Setting RequestWorkflow.controllerAssignedLabel " + label + " for domain " + domain);
		setBitsetLabel(controllerAssignedLabel, domain, label);
	}
	
	/**
	 * This static function adds label to a map of bitsets (any map)
	 * @param cal
	 * @param domain
	 * @param label
	 */
	public static void setBitsetLabel(Map<String, BitSet> cal, String domain, int label) {
		if(cal.containsKey(domain)){
			cal.get(domain).set(label);
		} else {
			BitSet nbs= new BitSet(InterDomainHandler.max_vlan_tag);
			nbs.set(label);
			cal.put(domain, nbs);
		}
	}
	
	public void syncRequestModel() {
		if (requestModel != null)
			TDB.sync(requestModel);
	}
	
	public void syncManifestModel() {
		if (manifestModel != null)
			TDB.sync(manifestModel);
	}
}
