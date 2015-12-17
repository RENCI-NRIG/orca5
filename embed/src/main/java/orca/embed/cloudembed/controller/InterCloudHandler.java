package orca.embed.cloudembed.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import orca.embed.cloudembed.IConnectionManager;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.elements.Device;
import orca.ndl.elements.NetworkElement;
import orca.shirako.container.Globals;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.util.FileManager;

public class InterCloudHandler extends ModifyHandler {
	
	public InterCloudHandler() throws NdlException {
		super();
	}

	public InterCloudHandler(IConnectionManager icm) throws NdlException {
		super(icm);
	}
	/**
	 * Create handler with in-memory model
	 * @param substrateFile
	 * @throws IOException
	 * @
	 */
	public InterCloudHandler(String substrateFile) throws IOException, NdlException {
		super(substrateFile);
	}
	
	/**
	 * Create handler with TDB-backed model in a directory with specified path prefix
	 * @param substrateFile
	 * @param tdbPrefix
	 * @throws IOException
	 * @throws NdlException
	 */
	public InterCloudHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
		super(substrateFile, tdbPrefix);
	}
	
	/**
	 * Create a handler with TDB-backed blank model or try to recover existing TDB model
	 * @param tdbPrefix
	 * @param recover
	 * @throws IOException
	 * @throws NdlException
	 */
	public InterCloudHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
		super(tdbPrefix, recover);
	}
	
	public SystemNativeError runEmbedding(boolean bound,RequestReservation request,
			DomainResourcePools domainResourcePools) {

		//OntModel requestModel = request.getModel();
		HashMap <String, RequestReservation> dRR = request.getDomainRequestReservation();
		if(!bound){
			buildCloudOntModel(domainResourcePools.getDomainResourcePools()); 
		}
		
		String domain;
		RequestReservation rr;
		SystemNativeError error=null;
		if(dRR.size()<=0){
			error = new SystemNativeError();
			error.setErrno(1);
			error.setMessage("Unknown domain in the embedding request"+"!");
			return error;
		}
		RequestReservation old_mpRequest=null;
		Collection <NetworkElement> mp_elements = null;
		LinkedList <Entry <String, RequestReservation>> dRR_list= new LinkedList<Entry <String, RequestReservation>>();

		for(Entry <String, RequestReservation> entry:dRR.entrySet()){
			domain=entry.getKey();
			if(domain.equals(RequestReservation.MultiPoint_Domain))
				dRR_list.addFirst(entry);
			else
				dRR_list.addLast(entry);
		}
		for(Entry <String, RequestReservation> entry:dRR_list){
			domain=entry.getKey();
			rr=entry.getValue();
			logger.info("Request element:"+domain);
			if(domain.equals(RequestReservation.Unbound_Domain)){  //unbound request, may need splitting
				this.multipointRequest=false;
				error = runEmbedding(rr,domainResourcePools,bound);
			}
			else if(domain.equals(RequestReservation.Interdomain_Domain)){  //call @ InterDomainHandler
				try {
					this.multipointRequest=false;
					error=runEmbedding(request.getDomainRequestReservation(domain), domainResourcePools);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(domain.equals(RequestReservation.MultiPoint_Domain)){	//call MultiPointHandler
				this.multipointRequest=true;
				old_mpRequest = request.getDomainRequestReservation(domain);
                error=runEmbedding(old_mpRequest, domainResourcePools,bound,0);
			}
			else{ //call @ CloudHandler
				this.multipointRequest=false;
				error=runEmbedding(domain,request.getDomainRequestReservation(domain), domainResourcePools);
			}
		}
		if(old_mpRequest != null && mpRequest != null){
			mp_elements = old_mpRequest.getElements();
			HashMap<String, RequestReservation> s_rr = request.getDomainRequestReservation();
			s_rr.remove(RequestReservation.MultiPoint_Domain);
            request.getElements().removeAll(mp_elements);
            for(NetworkElement ne:this.mpRequest.getElements()){
            	//ne.setInDomain(RequestReservation.Interdomain_Domain);
            	String domainName=ne.getInDomain();
            	String pureType=ne.getResourceType().getResourceType();
            	if(domainName.endsWith(pureType)){
    				String tmpType = "/"+pureType;
    				domainName=domainName.split(tmpType)[0];
    				ne.setInDomain(domainName);
    			}
            	request.setDomainRequestReservation(ne,s_rr);
            }
		}else{
			logger.error("Null request::old_mpRequest="+old_mpRequest+";mpRequest="+mpRequest);
		}
			
		return error;
	}
	

	//Build the ontmodel graph of edge cloud sites interconnected by interdomain paths
	protected InfModel buildCloudOntModel(HashMap<String, DomainResourceType> domainResourcePools){
		ArrayList <DomainResourceType> setOfCloudSite = new ArrayList <DomainResourceType> ();
		ArrayList <DomainResourceType> setOfTransitSite = new ArrayList <DomainResourceType> ();
		
		OntModel cloudModel=ModelFactory.createOntologyModel();
		Model schemaModel=FileManager.get().loadModel("http://geni-orca.renci.org/owl/domain.owl");
		
		OntModel cloudModelBase = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);		
		Reasoner cloudModelReasoner = cloudModelBase.getReasoner();
		cloudModelReasoner=cloudModelReasoner.bindSchema(schemaModel);
		
		InfModel cloudInf=ModelFactory.createInfModel(cloudModelReasoner,cloudModel);
		
		String sourceDomain=null,destinationDomain=null;
		Resource c_s_rs=null,c_d_rs;
		String domainURL=null;
		for(Entry <String,DomainResourceType> entry:domainResourcePools.entrySet()){
			domainURL=entry.getKey();
			//System.out.println("Substrate site:"+entry.getKey()+":"+domainURL+"\n");
			if(entry.getValue().getResourceType().endsWith("VM")){
				domainURL=domainURL+"/vm";
				entry.getValue().setDomainURL(domainURL);
				setOfCloudSite.add(entry.getValue());
			}
			if(entry.getValue().getResourceType().endsWith("VLAN")){
				domainURL=domainURL+"/vm";
				entry.getValue().setDomainURL(domainURL);
				setOfTransitSite.add(entry.getValue());
			}
		}
		int size = setOfCloudSite.size();
		int i=0,j=0;
		long pathMinBW=0;
		Resource source_rs,destination_rs;
		Resource source_intf_rs,destination_intf_rs;
		String rType="http://geni-orca.renci.org/owl/domain.owl#VLAN";
		ArrayList<ArrayList<OntResource>> path=null;
		for(i=0;i<size;i++){
			sourceDomain=setOfCloudSite.get(i).getDomainURL();
			source_rs=idm.getResource(sourceDomain);
			for(j=i+1;j<size;j++){
				destinationDomain=setOfCloudSite.get(j).getDomainURL();
				destination_rs=idm.getResource(destinationDomain);
				
				path=mapper.findShortestPath(idm,source_rs,destination_rs,0,rType,rType,null);
				pathMinBW=mapper.minBW(path);
				int path_len = path.size();
				c_s_rs=cloudInf.createResource(path.get(0).get(0).getURI());
				source_intf_rs=cloudInf.createResource(path.get(0).get(1).getURI());
				c_s_rs.addProperty(NdlCommons.topologyHasInterfaceProperty , source_intf_rs);
				source_intf_rs.addProperty(NdlCommons.topologyInterfaceOfProperty,c_s_rs);
				source_intf_rs.addLiteral(NdlCommons.layerBandwidthProperty, pathMinBW);
				source_intf_rs.addLiteral(this.mapper.numHop, path_len/2);
				
				c_d_rs=cloudInf.createResource(path.get(path_len-1).get(0).getURI());
				destination_intf_rs=cloudInf.createResource(path.get(path_len-1).get(1).getURI());
				c_d_rs.addProperty(NdlCommons.topologyHasInterfaceProperty , destination_intf_rs);
				destination_intf_rs.addProperty(NdlCommons.topologyInterfaceOfProperty,c_d_rs);
				destination_intf_rs.addLiteral(NdlCommons.layerBandwidthProperty, pathMinBW);
				destination_intf_rs.addLiteral(this.mapper.numHop, path_len/2);
				
				source_intf_rs.addProperty(NdlCommons.connectedTo , destination_intf_rs);
				destination_intf_rs.addProperty(NdlCommons.connectedTo , source_intf_rs);
				//System.out.println("Meshed link:"+path.get(0).get(0)+":"+path.get(path_len-1).get(0)+":"+path_len+"\n");
				logger.debug("Cloud:"+c_s_rs+":"+c_d_rs+":"+source_intf_rs.getProperty(this.mapper.numHop).getInt()+":"+destination_intf_rs.getProperty(NdlCommons.layerBandwidthProperty).getLong()+"\n");
			}
		}
		//cloudInf.write(System.out);
		return cloudInf;
	}
}
