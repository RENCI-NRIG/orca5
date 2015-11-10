package orca.embed.cloudembed.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import orca.embed.cloudembed.IConnectionManager;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.ndl.elements.SwitchingAction;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class MultiPointHandler extends InterDomainHandler implements LayerConstant {
	
	public RequestReservation mpRequest;
	
	public MultiPointHandler() throws NdlException {
		super();
	}
	
	public MultiPointHandler(IConnectionManager icm) throws NdlException {
		super(icm);
	}
	
	
	/**
	 * Create handler with in-memory model
	 * @param substrateFile
	 * @throws IOException
	 * @
	 */
	public MultiPointHandler(String substrateFile) throws IOException, NdlException {
		super(substrateFile);
	}
	
	/**
	 * Create handler with TDB-backed model in a directory with specified path prefix
	 * @param substrateFile
	 * @param tdbPrefix
	 * @throws IOException
	 * @throws NdlException
	 */
	public MultiPointHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
		super(substrateFile, tdbPrefix);
	}
	
	/**
	 * Create a handler with TDB-backed blank model or try to recover existing TDB model
	 * @param tdbPrefix
	 * @param recover
	 * @throws IOException
	 * @throws NdlException
	 */
	public MultiPointHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
		super(tdbPrefix, recover);
	}
	
	//Interdomain path computation
	public SystemNativeError runEmbedding(RequestReservation rr,
			DomainResourcePools domainResourcePools, boolean bound,int num){
		SystemNativeError error=null;
		
		OntModel requestModel=rr.getModel();
		Collection <NetworkElement> elements = rr.getElements();
		//LinkedList<NetworkElement> deviceList = new LinkedList<NetworkElement>();

		Resource multicastDomain = NdlCommons.getDomainHasCastType(NdlCommons.multicast,"domain:hasService",this.idm);
		Iterator<NetworkElement> it = elements.iterator();
		while(it.hasNext()){
			ComputeElement root = null;
			HashMap <String, LinkedList <NetworkElement>> domainCount = null;
			NetworkConnection element = (NetworkConnection) it.next();
			if(element.getResource()!=null && element.getResource().getPropertyResourceValue(NdlCommons.inDomainProperty)!=null){
				multicastDomain = element.getResource().getPropertyResourceValue(NdlCommons.inDomainProperty);
			}else if(element.getCastType().equalsIgnoreCase("Multicast") && element.isModify()){
				multicastDomain=element.getResource();
			}else
				domainCount = ifMPConnection(element);
			if((domainCount!=null)&&(domainCount.size()<=2) && !element.isModify()){	//in-rack broadcasting
					
			}else{	
				if(multicastDomain==null){
					error = new SystemNativeError();
					error.setErrno(1);
					error.setMessage("There is no multicast capable domain"+"!");
					return error;
				}
				OntResource root_rs=requestModel.createIndividual(multicastDomain.getURI(),NdlCommons.computeElementClass);
				root_rs.addProperty(NdlCommons.inDomainProperty, multicastDomain);
				root = new ComputeElement(requestModel,multicastDomain);
				root.setModify(element.isModify());
				root.setCastType(element.getCastType());
				logger.debug("MultiPointHandler:root="+root.getName()+";isModify="+root.isModify());
			}
			RequestReservation request=null;
			if(root!=null)
				request = generateConnectionRequest(requestModel, element, rr,root);
			else
				request = generateConnectionRequest(requestModel, domainCount, element, rr);
			if(this.mpRequest==null)
				this.mpRequest=request;
			else{
				for(NetworkElement ne:request.getElements())
					this.mpRequest.setRequest(ne);
			}
			try {
				if(domainCount.size()>1)
					error=runEmbedding(request, domainResourcePools);
				else if(domainCount.size()==1)
					error=runEmbedding(domainCount.keySet().iterator().next(),request, domainResourcePools);
				if(root!=null)
					setCastType(root, deviceList);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return error;
	}

	@SuppressWarnings("unchecked")
	public RequestReservation generateConnectionRequest(OntModel m,HashMap <String, LinkedList <NetworkElement>> domainCount,NetworkConnection e,RequestReservation rr){
		OrcaReservationTerm t = rr.getTerm();
		String reservationD = rr.getReservationDomain();
		String r = rr.getReservation();
		Resource r_rs=rr.getReservation_rs();
		
		RequestReservation request = new RequestReservation();

		//new request network connection
		NetworkConnection c_e = new NetworkConnection(m,e.getURI(),e.getName());
		c_e.setBandwidth(e.getBandwidth());
		c_e.setLatency(e.getLatency());
		c_e.setOpenflowCapable(e.getOpenflowCapable());
		DomainResourceType dType=e.getResourceType();
		if(dType.getResourceType()==null)
			dType.setResourceType(DomainResourceType.VLAN_RESOURCE_TYPE);
		c_e.setResourceType(dType);
		String layer=e.getAtLayer();
		if(layer==null)
			layer = "EthernetNetworkElement";
		c_e.setAtLayer(layer);
		
		LinkedList<NetworkElement> ne1_elements = null,ne2_elements = null;
		String ne1_domain_str = null,ne2_domain_str = null;
		Resource ne1_domain_rs=null,ne2_domain_rs=null;
		if(domainCount.size()==2){
			ne1_elements = (LinkedList<NetworkElement>) domainCount.values().toArray()[0];
			ne2_elements = (LinkedList<NetworkElement>) domainCount.values().toArray()[1];
			ne1_domain_str = (String) domainCount.keySet().toArray()[0];
			ne2_domain_str = (String) domainCount.keySet().toArray()[1];
			ne1_domain_rs = m.getResource(ne1_domain_str);
			ne2_domain_rs = m.getResource(ne2_domain_str);
	
			c_e.setInDomain(RequestReservation.Interdomain_Domain);
			//ne1
			ComputeElement ne = createNE(m,ne1_domain_rs,ne1_elements,c_e);
			c_e.setNe1(ne);
			//ne2
			ne = createNE(m,ne2_domain_rs,ne2_elements,c_e);
			c_e.setNe2(ne);
		}else if(domainCount.size()==1){
			c_e.setCastType(e.getCastType());
			for(NetworkElement e_e:e.getConnection()){
				c_e.addConnection(e_e);
				c_e.setInDomain(e_e.getInDomain());
			}
		}
		
		request.setRequest(m,c_e,t,reservationD,r, r_rs);
		
		return request;
	}
	
	public ComputeElement createNE(OntModel m, Resource rs, LinkedList <NetworkElement> cg,NetworkConnection c_e){
		OntResource ne1_rs=null;
		ComputeElement ne = null;
		if(rs.getURI().contains(NdlCommons.stitching_domain_str))
			ne=(ComputeElement) cg.getFirst();
		else{
			ne1_rs=m.createIndividual(rs.getURI(),NdlCommons.computeElementClass);
			ne1_rs.addProperty(NdlCommons.inDomainProperty, rs);
			ne = new ComputeElement(m,ne1_rs);
		
			DomainResourceType dType=((NetworkElement) cg.toArray()[0]).getResourceType();
			int res_count=0;
			for(NetworkElement e:cg){
				res_count = res_count+e.getResourceType().getCount();
			}
			DomainResourceType rType = new DomainResourceType(dType.getResourceType(),res_count);
			rType.setRank(dType.getRank());
			rType.setDomainURL(rs.getURI());
			ne.setResourceType(rType);
		
			ne.setCeGroup(cg);
			
			ne.addDependency(c_e);
		}
		return ne;
	}
	
	@SuppressWarnings("unchecked")
	public RequestReservation generateConnectionRequest(OntModel m,NetworkConnection e,RequestReservation rr, ComputeElement root){
		OrcaReservationTerm t = rr.getTerm();
		String reservationD = rr.getReservationDomain();
		String r = rr.getReservation();
		Resource r_rs=rr.getReservation_rs();
		RequestReservation request = new RequestReservation();
		NetworkConnection c_e=null;

		if(e.getConnection()!=null){
        	LinkedList <NetworkElement> bcNodeList = (LinkedList<NetworkElement>)e.getConnection();
        	String url,name;
        	ComputeElement bc_ce=null;
        	Interface intf_nc=null;
        	for(int i =0;i<bcNodeList.size();i++){
        		request.setPureType(bcNodeList.get(i).getResourceType(),rr.getTypeTotalUnits());
        		
        		bc_ce=(ComputeElement)bcNodeList.get(i);
        		intf_nc = bc_ce.getInterfaceName(e);
        		NetworkConnection bc_ce_nc = bc_ce.getConnectionByInterfaceName(intf_nc);
        		
        		name=bcNodeList.get(i).getResource().getLocalName() + "-" + bc_ce_nc.getResource().getLocalName();
        		url = bc_ce_nc.getURI();
        		
        		c_e = new NetworkConnection(m,url,name);
        		c_e.setBandwidth(e.getBandwidth());
        		c_e.setLatency(e.getLatency());
        		c_e.setOpenflowCapable(e.getOpenflowCapable());
        		DomainResourceType dType=e.getResourceType();
        		if(dType.getResourceType()==null)
        			dType.setResourceType(DomainResourceType.VLAN_RESOURCE_TYPE);
        		c_e.setResourceType(dType);
        		String layer=e.getAtLayer();
        		if(layer==null)
        			layer = "EthernetNetworkElement";
        		c_e.setAtLayer(layer);
        		c_e.setInDomain(RequestReservation.Interdomain_Domain);
        		
        		c_e.setNe1(bc_ce);
        		if(intf_nc!=null){
        			bc_ce.removeConnectionByInterfaceName(intf_nc);
        			bc_ce.setInterfaceName(c_e, intf_nc);
        		}
        		DomainResourceType rType = new DomainResourceType(dType.getResourceType(),dType.getCount());
        		rType.setRank(dType.getRank());
        		rType.setDomainURL(root.getURI());
        		root.setResourceType(rType);
        		c_e.setNe2(root);
        		c_e.setModify(root.isModify());
        		c_e.setCastType(root.getCastType());
        		request.setRequest(m,c_e,t,reservationD,r, r_rs);
        	}
        }
		return request;	
	}
	
	
	protected void setCastType(NetworkElement root, LinkedList<NetworkElement> deviceList){
		for(NetworkElement d: deviceList){
			if(root.getURI().equalsIgnoreCase(d.getURI())){
				d.setCastType(NdlCommons.multicast);
				LinkedList <SwitchingAction> action_list = ((Device) d).getActionList();
				int size=action_list.size();
				SwitchingAction action = null;
				for(int i=0;i<size;i++){
					action=(SwitchingAction) action_list.get(i);
					if(action==null) logger.info("No Action");
					else action.setCastType(NdlCommons.multicast);
				}
				logger.info("The multicast domain:"+d.getURI());
				break;
			}
		}
	}
	
	//decide to call unboundhandler or mphandler which is for bounded inter-domain mp connection
	@SuppressWarnings("unchecked")
	public HashMap <String, LinkedList <NetworkElement>> ifMPConnection(NetworkConnection rc){
		LinkedList <NetworkElement> con_elements = (LinkedList<NetworkElement>) rc.getConnection();
		String e_domain=null;
		LinkedList <NetworkElement> elements = null;
		ComputeElement c_e = null;
		HashMap <String, LinkedList <NetworkElement>> domainCount = new HashMap <String, LinkedList <NetworkElement>>();
		if(con_elements.size()>0){
			for(NetworkElement e:con_elements){
				e_domain = e.getInDomain();
				c_e= (ComputeElement) e;
				if(e_domain==null){
					domainCount = new HashMap <String, LinkedList <NetworkElement>>();
					break;
				}
				if(domainCount.containsKey(e_domain)){
					domainCount.get(e_domain).add(e);
					if(c_e.getGroup()==null)
						c_e.setGroup(c_e.getName());
				}else{
					elements = new LinkedList <NetworkElement>();
					elements.add(e);
					if(c_e.getGroup()==null)
						c_e.setGroup(c_e.getName());
					domainCount.put(e_domain, elements);
				}
			}
		}
		return domainCount;
	}
}
