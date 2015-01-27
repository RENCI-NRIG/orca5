package orca.embed.cloudembed.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

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
		if(multicastDomain==null){
			error = new SystemNativeError();
			error.setErrno(1);
			error.setMessage("There is no multicast capable domain"+"!");
			return error;
		}
		ComputeElement root = null;
		Iterator<NetworkElement> it = elements.iterator();
		while(it.hasNext()){
			NetworkConnection element = (NetworkConnection) it.next();
			OntResource root_rs=requestModel.createIndividual(multicastDomain.getURI(),NdlCommons.computeElementClass);
			root_rs.addProperty(NdlCommons.inDomainProperty, multicastDomain);
			root = new ComputeElement(requestModel,multicastDomain);
			mpRequest = generateConnectionRequest(requestModel, element, rr,root);
			try {
				error=runEmbedding(mpRequest, domainResourcePools);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		setCastType(root, deviceList);
		
		return error;
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
        	
        		//name = root.getResource().getLocalName() + "-" + bcNodeList.get(i).getResource().getLocalName();
        		//url = e.getResource().getNameSpace()+"root";
        		
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
        		request.setRequest(m,c_e,t,reservationD,r, r_rs);
        	}
        }
		return request;	
	}
}
