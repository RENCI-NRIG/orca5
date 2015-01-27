package orca.embed.cloudembed.controller;

import java.io.IOException;
import java.util.Collection;
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
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Resource;

public class UnboundRequestHandler extends MultiPointHandler {
	
	protected InfModel cloudInfModel;
	
	public UnboundRequestHandler(IConnectionManager icm) throws NdlException {
		super(icm);
	}

	public UnboundRequestHandler() throws NdlException {
		super();
	}

	
	/**
	 * Create handler with in-memory model
	 * @param substrateFile
	 * @throws IOException
	 * @
	 */
	public UnboundRequestHandler(String substrateFile) throws IOException, NdlException {
		super(substrateFile);
	}
	
	/**
	 * Create handler with TDB-backed model in a directory with specified path prefix
	 * @param substrateFile
	 * @param tdbPrefix
	 * @throws IOException
	 * @throws NdlException
	 */
	public UnboundRequestHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
		super(substrateFile, tdbPrefix);
	}
	
	/**
	 * Create a handler with TDB-backed blank model or try to recover existing TDB model
	 * @param tdbPrefix
	 * @param recover
	 * @throws IOException
	 * @throws NdlException
	 */
	public UnboundRequestHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
		super(tdbPrefix, recover);
	}
	
	public SystemNativeError runEmbedding(RequestReservation rr,
			DomainResourcePools domainResourcePools,boolean bound){
		
		//this.request = rr;
		//OntModel requestModel = rr.getModel();
		String requestEntryType,max_domain=null,second_domain=null;
		int requestUnits;
		LinkedList <DomainResourceType> rTypeDomainList;
		SystemNativeError error=null;		
		int delta, max = 0,second = 0;

		for(Entry <String, Integer> requestEntry: rr.getTypeTotalUnits().entrySet()){
			requestEntryType = requestEntry.getKey();
			requestUnits = requestEntry.getValue();
			rTypeDomainList = domainResourcePools.getDomainResourceTypeList(requestEntryType);
			
			if( (rTypeDomainList == null) || (rTypeDomainList.size()==0)){
				error = new SystemNativeError();
				error.setErrno(10);
				error.setMessage("No available substrate for the required resource:"+requestEntryType+":"+requestUnits);
				return error;
			}
			int totalCount=0;
			for(DomainResourceType drt:rTypeDomainList){
				totalCount=totalCount+drt.getCount();
			}
			if(requestUnits>totalCount){
				error = new SystemNativeError();
				error.setErrno(1);
				error.setMessage("Insufficient available resources for the request:"+requestEntryType+":"+requestUnits);
				return error;
			}
		}
		
		int max_index=0;
		boolean domain_avai=true;
		for(Entry <String, Integer> requestEntry: rr.getTypeTotalUnits().entrySet()){
			requestEntryType = requestEntry.getKey();
			if( (!requestEntryType.equals("vm")) && (!requestEntryType.equals("baremetalce")))
				continue;
			requestUnits = requestEntry.getValue();
			rTypeDomainList = domainResourcePools.getDomainResourceTypeList(requestEntryType);

			for(max_index=0;max_index<rTypeDomainList.size();max_index++){

				max = rTypeDomainList.get(max_index).getCount();
				max_domain=rTypeDomainList.get(max_index).getDomainURL();
				domain_avai=checkResourceAvailability(max_domain,rr,domainResourcePools);
				if(domain_avai==true)
					break;
			}
			if(domain_avai==false){
				error = new SystemNativeError();
				error.setErrno(1);
				error.setMessage("Insufficient available resources for all the requested types!");
				return error;
			}
			
			if(max_index<rTypeDomainList.size()-1){
				second = rTypeDomainList.get(max_index+1).getCount();
				second_domain = rTypeDomainList.get(max_index+1).getDomainURL();
			}
			delta=(int) (max-Math.ceil((double)second/3));
			
			if(requestUnits<=delta){   //required units smaller than the threshold, embedding in one domain.
				String domain = max_domain;
				int index = domain.indexOf("/"+requestEntryType);
				if(index>0)
					domain=domain.substring(0, index);
				error=runEmbedding(domain, rr, domainResourcePools);
				rr.setReservationDomain(max_domain);
			}else if(requestUnits<=max+second){  //split into two sites
				if(second>0){
					int x1 = (max-second+requestUnits)/2;
					int x2=requestUnits-x1;
					RequestReservation twoWayRequest = generateVCConnection(rr,requestEntryType,x1,x2,
							max_domain,second_domain);
					try {
						error=runEmbedding(twoWayRequest, domainResourcePools);
						rr.setElements(twoWayRequest.getElements());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//process master node 
					processNodeDependency(rTypeDomainList);
				}else{
					error = new SystemNativeError();
					error.setErrno(100);
					error.setMessage("No splitting, only one site available: Requested type:"+requestEntryType+":"+requestUnits);
				}
			}else{
				/*VTRequestMapping vtMapper;
				try {
					vtMapper = new VTRequestMapping(this.getIdm());
					RequestReservation request = vtMapper.handleMapping(rr,domainResourcePools,requestEntryType);
					error=runEmbedding(request, domainResourcePools);
					rr.setElements(request.getElements());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				/*error = new SystemNativeError();
				error.setErrno(100);
				error.setMessage("More than two-way splitting is not supported: Requested type:"+requestEntryType+":"+requestUnits);*/
			}
			break;
		}
		return error;
	}
	
	private boolean checkResourceAvailability(String max_domain,
			RequestReservation rr, DomainResourcePools domainResourcePools) {
		if(max_domain==null)
			return false;
		boolean avai=false;
		String requestEntryType,drt_name;
		int requestUnits;
		LinkedList <DomainResourceType> rTypeDomainList;
		for(Entry <String, Integer> requestEntry: rr.getTypeTotalUnits().entrySet()){
			avai=false;
			requestEntryType = requestEntry.getKey();
			requestUnits = requestEntry.getValue();
			rTypeDomainList = domainResourcePools.getDomainResourceTypeList(requestEntryType);
			for(DomainResourceType drt:rTypeDomainList){
				drt_name=drt.getDomainURL().split(requestEntryType)[0];
				if(max_domain.contains(drt_name)){
					avai=true;
					break;
				}					
			}
		}
		return avai;
	}

	public void processNodeDependency(LinkedList <DomainResourceType> rTypeDomainList){
		
	}
	
	//generate a inter-cloud request containing a connection with two clusters for routing using InterdomainHandler
	protected RequestReservation generateVCConnection(RequestReservation rr, String rType,int x1,int x2,
			String max_domain,String second_domain) {
		RequestReservation request=new RequestReservation();
		request.setModel(rr.getModel());
		String reservation = rr.getReservation();
		OntModel requestModel = rr.getModel();

		Resource max_domain_rs=this.idm.createResource(max_domain);
		Resource second_domain_rs=this.idm.createResource(second_domain);
		
		String connectionName = reservation+"/Connection";
		Individual rs_connection = requestModel.createIndividual(connectionName,NdlCommons.networkDomainOntClass);
		rs_connection.addProperty(NdlCommons.domainHasResourceTypeProperty, NdlCommons.vlanResourceTypeClass);
		DomainResourceType cType = NdlCommons.getDomainResourceType(rs_connection);		

		NetworkConnection connection=new NetworkConnection(requestModel, rs_connection);
		connection.setBandwidth(NdlCommons.Default_Bandwidth);		
		connection.setResourceType(cType);
		//default connection type is VLAN
		//connection.setType("http://geni-orca.renci.org/owl/domain.owl#VLAN");
		connection.setResourceType(cType);

		String master_str = reservation+"/MasterGroup";
		String slave_str = reservation+"/SlaveGroup";
		Individual master_ont =  requestModel.createIndividual(master_str,NdlCommons.computeElementClass);
		if(max_domain_rs!=null)
			master_ont.addProperty(NdlCommons.inDomainProperty , max_domain_rs);
		Individual slave_ont =  requestModel.createIndividual(slave_str,NdlCommons.computeElementClass);
			slave_ont.addProperty(NdlCommons.inDomainProperty , second_domain_rs);
		DomainResourceType dType = new DomainResourceType(rType,x1);

		dType.setDomainURL(max_domain);
		dType.setRank(10);
		ComputeElement master_ce = new ComputeElement(requestModel,master_ont);
		master_ce.setResourceType(dType);
		dType = new DomainResourceType(rType,x2);
		dType.setDomainURL(second_domain);
		dType.setRank(10);
		ComputeElement slave_ce = new ComputeElement(requestModel,slave_ont);
		slave_ce.setResourceType(dType);
		//add interfaces....
		
		getVMInfo(master_ce,rr);
		getVMInfo(slave_ce,rr);
		
		connection.setNe1(master_ce);
		connection.setNe2(slave_ce);

		//process node dependency,image information, etc.
		//request.setRequest(connection);
		request.setRequest(requestModel, connection, rr.getTerm(), rr.getReservationDomain(),rr.getReservation(),rr.getReservation_rs());
				
		logger.info("InterCloud VC connection:dType="+rType+"--"+x1+":"+x2+";"+connectionName+":"+master_str+":"+slave_str+":"+max_domain+":"+second_domain);
		
		return request;
	}
	
	public void getVMInfo(ComputeElement ce, RequestReservation rr){
		Collection <NetworkElement> elements = rr.getElements();
		ComputeElement ce_r =null;
		NetworkElement ee;
		Iterator <NetworkElement> it = elements.iterator();
		while(it.hasNext()){
			ee = it.next();
			if(ee instanceof ComputeElement){
				ce_r=(ComputeElement) ee;

				ce.setImageInfo(ce_r.getImage(), ce_r.getVMImageURL(),ce_r.getVMImageHash());
				break;
			}
		}
	}
	
}
