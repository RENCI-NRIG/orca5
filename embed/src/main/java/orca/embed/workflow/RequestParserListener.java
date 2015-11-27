package orca.embed.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.ndl.elements.RequestSlice;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Listener for NDL request parser. It gets objects in this order:
 * - Reservation and its details
 * - Nodes
 *   - Node dependencies
 * - Network Connections
 * - Interfaces
 * - All reservation resources (including nodes and network connections again)
 * 
 * This class creates a collection of object that are then passed to the embedder
 * @author ibaldin
 *
 */

public class RequestParserListener implements INdlRequestModelListener {
	protected RequestReservation request;
	protected Collection<NetworkElement> elements;
	private boolean parsingDone = false;
	protected OntModel model = null;
	
	private OrcaReservationTerm term = new OrcaReservationTerm();
	private String reservationDiskImage = null; 
	private String reservationImageURL = null;
	private String reservationImageHash = null;
	private String reservationDomain = null;
	private String reservation = null;
	private Resource reservation_rs = null;
	private RequestSlice currentSlice=null;

	
	private Map<String, NetworkConnection> links = new HashMap<String, NetworkConnection>();
	private Map<String, NetworkElement> nodes = new HashMap<String, NetworkElement>();
	private Map<String, NetworkElement> interfaceToNode = new HashMap<String, NetworkElement>();
	private Map<NetworkElement,String> nodeToInterface = new HashMap<NetworkElement,String>();
	
	SystemNativeError err;	
	
	public String getErrorMsg(){
		if(this.err!=null)
			return "Embedding workflow ERROR: " + err.getErrno()+":"+err.getMessage();
				
		return null;
	}
	
	public RequestParserListener() {
		request = new RequestReservation();
		elements = new LinkedList<NetworkElement>();
	}
	
	 /* Get all available elements after parsing is done
	 * @return
	 */	
	public RequestReservation getRequest() {
		if (!parsingDone)
			return null;
		reservation = reservation.concat("/"+UUID.randomUUID().toString());
		request.setRequest(model,elements,term,reservationDomain,reservation,reservation_rs);
		request.setSlice(currentSlice);
		request.setError(this.err);
		return request;
	}

	public void ndlNodeDependencies(Resource ni, OntModel m,
			Set<Resource> dependencies) {
		
		ComputeElement mainNode = (ComputeElement) nodes.get(ni.getURI());
		if ((mainNode == null) || (dependencies == null))
			return;
		for(Resource r: dependencies) {
			ComputeElement depNode = (ComputeElement) nodes.get(r.getURI());
			if (depNode != null)
				mainNode.addDependency(depNode);
		}

	}

	public OrcaReservationTerm getTerm() {
		return term;
	}

	public void setTerm(OrcaReservationTerm term) {
		this.term = term;
	}

	public void ndlReservation(Resource i, OntModel m) {
		// TODO Auto-generated method stub
		model = m; // we'll need the model later - get it here
		if (i != null) {
			reservation = i.getURI();
			reservation_rs = i;
			Resource domain = NdlCommons.getDomain(i);
			if(currentSlice!=null)
				currentSlice.setOFVersion(NdlCommons.getOpenFlowVersion(i));
			if(domain!=null)
				reservationDomain = domain.getURI();
			Resource di = NdlCommons.getDiskImage(i);
			if (di != null) {
				reservationImageURL = NdlCommons.getIndividualsImageURL(i);
				reservationImageHash = NdlCommons.getIndividualsImageHash(i);
				if ((reservationImageURL != null) && (reservationImageHash != null)) {
					reservationDiskImage = di.getURI();
				}
			}
		}
	}

	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		if(end==null)
			return;
		if(end.before(term.getStart())){
			try {
				throw new NdlException("Term End time " + end + 
						" should be after beginning time: " + term.getStart());
			} catch (NdlException e1) {
				err = new SystemNativeError();
				err.setErrno(-1);
				err.setMessage("Term End time " + end + 
						" should be after beginning time: " + term.toString());

				return;
			}
		}
		term.modifyTerm(end);
	}

	public void ndlReservationResources(List<Resource> r, OntModel m) {
		// TODO Auto-generated method stub
		
	}

	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		if(start==null)
			start = new Date();
		term.setStart(start);
	}

	public void ndlReservationTermDuration(Resource d, OntModel m, int years,
			int months, int days, int hours, int minutes, int seconds) {
		term.setDuration(days, hours, minutes,seconds);
		if(term.getDurationInSeconds()<=0){
			err = new SystemNativeError();
			err.setErrno(-1);
			err.setMessage("Term duration should be greater than 0: " + term.toString());

			return;
		}
	}

	public void ndlInterface(Resource intf, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		if (intf == null)
			return;
		OntResource intf_ont=om.getOntResource(intf);
		ComputeElement on = null;
		NetworkConnection ol = null;

		if (node != null)
			on = (ComputeElement) nodes.get(node.getURI());
		if (conn != null)
			ol = links.get(conn.getURI());
		
		Interface ipInterface = new Interface(om,intf_ont);
		try {
			ipInterface.setLabel(ip,mask);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (on != null) {
			on.addClientInterface(ipInterface);
			if (ol != null) {
				on.setInterfaceName(ol, ipInterface);
			}
		}
	}
	
	public List <Resource> interfaceToNode(Resource intf, OntModel om){
		List<Resource> ceList = NdlCommons.getWhoHasInterface(intf, om);
		if(ceList==null){
			//System.out.println("No Node:");
			return null;
		}
		
		List<Resource> r_ceList = new ArrayList<Resource>(); 
		for(Iterator <Resource> j=ceList.iterator();j.hasNext();){
			Resource tmpR = j.next();
			if (tmpR.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.computeElementClass) 
					|| tmpR.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.serverCloudClass)
					|| tmpR.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.networkStorageClass)){
				r_ceList.add(tmpR);
			}
		}
		return r_ceList;
	}

	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		if ( (l == null) || (interfaces.size()==0))
			return;
		NetworkConnection ol = new NetworkConnection(om,l);
		ol.setModify(NdlCommons.isModify(l));
		ol.setBandwidth(bandwidth);
		ol.setLatency(latency);
		//ol.setOpenflowCapable(NdlCommons.getOpenFlowVersion(l));
		if(currentSlice!=null)
			ol.setOpenflowCapable(currentSlice.getOfNeededVersion());
		DomainResourceType dType=NdlCommons.getDomainResourceType(l);
		if(dType.getResourceType()==null)
			dType.setResourceType(dType.VLAN_RESOURCE_TYPE);
		ol.setResourceType(dType);
		Resource layer_rs = NdlCommons.getLayer(l);
		String layer=null;
		if(layer_rs!=null)
			layer = layer_rs.getLocalName();
		else
			layer = "EthernetNetworkElement";
		ol.setAtLayer(layer);	
		String label_str=NdlCommons.getLayerLabelLiteral(l);
		float label_id=0;
		if(label_str!=null){
			label_id=Float.valueOf(label_str);
			ol.setLabel_ID(label_id);
		}
		
		List <Resource> intfs=null;		
		// find what nodes it connects (should be two)
		Iterator<Resource> it = interfaces.iterator(); 
		if ((interfaces.size() <= 2) && !(l.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.topologyBroadcastConnectionClass))) {
			// point-to-point link
			// the ends
			Resource if1 = null,if2 = null, start_if=null;
			NetworkElement if1Node=null,if2Node=null;
			if(interfaces.size()==2){ 
				if1=it.next();
				if2 = it.next();
				if1Node = interfaceToNode.get(if1.getURI());
				if2Node = interfaceToNode.get(if2.getURI());
				if(if1Node==null || if2Node==null){
					err = new SystemNativeError();
					err.setErrno(-1);
					err.setMessage("Edge node error on the request: if1=" + if1 + ";if2="+if2);
					return;
				}
				//always treat the stitching port as the source of a connection 
				if((if2Node.getInDomain() != null) && (if2Node.getInDomain().contains(NdlCommons.stitching_domain_str))) {
					start_if = if2;
					if2=if1;
					if1=start_if;
				}
			}else{
				err = new SystemNativeError();
				err.setErrno(-1);
				err.setMessage("Only one interface on the request: if1=" + if1 + ";if2="+if2);
				return;
			}
			if (if1 != null) {
				if1Node = interfaceToNode.get(if1.getURI());
				if(if1Node==null){
					List <Resource> ce_list = this.interfaceToNode(if2,om);
					Resource ce=null;
					if((ce_list!=null)&&(!ce_list.isEmpty()))
						ce=ce_list.get(0);
					if1Node=existNode(ce);
					if(if1Node!=null){
						interfaceToNode.put(if1.getURI(), if1Node);					
					}else{
						intfs = new LinkedList<Resource>();
						intfs.add(if1);
						if1Node = this.createNode(ce, om, NdlCommons.computeElementClass, intfs);
					}
				}
				ol.setNe1(if1Node);				
			}
			if (if2 != null) {	
				if2Node = interfaceToNode.get(if2.getURI());
				if(if2Node==null){
					List <Resource> ce_list = this.interfaceToNode(if2,om);
					Resource ce=null;
					if((ce_list!=null)&&(!ce_list.isEmpty()))
						ce=ce_list.get(0);
					if2Node=existNode(ce);
					if(if2Node!=null){						
						interfaceToNode.put(if2.getURI(), if2Node);							
					}else{
						intfs = new LinkedList<Resource>();
						intfs.add(if2);
						if2Node = this.createNode(ce, om, NdlCommons.computeElementClass, intfs);
					}
				}
				ol.setNe2(if2Node);			
			}
		} else {
			// multi-point link
			ol.setCastType("Multicast");
			while(it.hasNext()) {
				Resource iff = it.next();
				NetworkElement ifNode = null;
				List <Resource> ce_list = this.interfaceToNode(iff,om);
				Resource ce=null;
				if((ce_list!=null)&&(ce_list.size()<=1)){
					ifNode = interfaceToNode.get(iff.getURI());
					if(ifNode==null){
						ce=ce_list.get(0);
						ifNode=existNode(ce);
						if(ifNode!=null){						
							interfaceToNode.put(iff.getURI(), ifNode);							
						}else{
							intfs = new LinkedList<Resource>();
							intfs.add(iff);
							ifNode = this.createNode(ce, om, NdlCommons.computeElementClass, intfs);
						}
					}
					ol.addConnection(ifNode);
				}else{
					for(int i=0;i<ce_list.size();i++){
						ce=ce_list.get(i);
						ifNode = existNode(ce);
						if(ifNode!=null){
							nodeToInterface.put(ifNode, iff.getURI());
						}else{
							intfs = new LinkedList<Resource>();
							intfs.add(iff);
							ifNode = this.createNode(ce, om, NdlCommons.computeElementClass, intfs);
						}
						ol.addConnection(ifNode);
					}
				}
			}
		}
		links.put(l.getURI(), ol);
		elements.add(ol);
		return;
	}

	public NetworkElement existNode(Resource ce){
		if(ce==null)
			return null;
		for(Entry <String, NetworkElement> entry:nodes.entrySet()){
			if(ce.getURI().equals(entry.getKey())){
				return entry.getValue();
			}	
		}
		return null;
	}
	
	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {

		if (ce == null)
			return;
		NetworkElement newNode = createNode(ce, om, ceClass, interfaces);
		elements.add(newNode);
		return;
	}
	
	public NetworkElement createNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		if(ce==null) return null;
		NetworkElement newNode;		 
		if ( (ceClass.equals(NdlCommons.serverCloudClass)) 
				|| (ceClass.equals(NdlCommons.computeElementClass))
				|| (ceClass.equals(NdlCommons.deviceOntClass))
				|| (ceClass.equals(NdlCommons.networkStorageClass))) {
			ComputeElement node = new ComputeElement(om,ce);
			
			DomainResourceType dType=NdlCommons.getDomainResourceType(ce,node);
			if(dType.getResourceType()==null){
				dType.setResourceType(dType.VM_RESOURCE_TYPE);
				dType.setRank(4); //IP layer
			}
			node.setResourceType(dType);
			
			node.setSplittable(NdlCommons.isSplittable(ce));
			node.setModify(NdlCommons.isModify(ce));
			node.setGUID(NdlCommons.getGuidProperty(ce));
			
			node.setSpecificCEType(ce);
			// disk image
			Resource di = NdlCommons.getDiskImage(ce);
			if (di != null) {
				String imageURL = NdlCommons.getIndividualsImageURL(ce);
				String imageHash = NdlCommons.getIndividualsImageHash(ce);
				node.setImage(di.getURI());
				node.setVMImageURL(imageURL);
				node.setVMImageHash(imageHash);
			}else{			
				node.setImage(reservationDiskImage);
				node.setVMImageURL(reservationImageURL);
				node.setVMImageHash(reservationImageHash);
			}
			// post boot script
			String script = NdlCommons.getPostBootScript(ce);
			if ((script != null) && (script.length() > 0)) {
				node.setPostBootScript(script);
			}		
			String groupName=NdlCommons.getGroupName(ce);
			if(ceClass.equals(NdlCommons.serverCloudClass)){
				node.setGroup(ce.getLocalName());
				node.setNodeGroupName(ce.getURI());
			}
			newNode=node;
		} else{ // default just a node
			newNode = new ComputeElement(om,ce);
			DomainResourceType dType=NdlCommons.getDomainResourceType(ce,newNode);
			if(dType.getResourceType()==null){
				dType.setResourceType(dType.VM_RESOURCE_TYPE);
				dType.setRank(4); //IP layer
			}
			newNode.setResourceType(dType);
		}
		Resource domain = NdlCommons.getDomain(ce);
		if (domain != null){
			newNode.setInDomain(domain.getURI());
		}else{
			newNode.setInDomain(reservationDomain);
		}

		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			interfaceToNode.put(intR.getURI(), newNode);
		}		

		nodes.put(ce.getURI(), newNode);
		return newNode;
	}
	
	public void ndlSlice(Resource sl, OntModel m) {
		if(currentSlice==null)
			currentSlice=new RequestSlice();
		// check that this is an OpenFlow slice and get its details
		if (sl.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.ofSliceClass)) {
			Resource ofCtrl = NdlCommons.getOfCtrl(sl);
			if (ofCtrl == null)
				return;
			currentSlice.setOfCtrlUrl(NdlCommons.getURL(ofCtrl));
			currentSlice.setOfUserEmail(NdlCommons.getEmail(sl));
			currentSlice.setOfSlicePass(NdlCommons.getSlicePassword(sl));
			if ((currentSlice.getOfUserEmail() == null) ||
					(currentSlice.getOfSlicePass() == null) ||
					(currentSlice.getOfCtrlUrl() == null)) {
					// disable OF if invalid parameters
					currentSlice.setNoOF();
					currentSlice.setOfCtrlUrl(null);
					currentSlice.setOfSlicePass(null);
					currentSlice.setOfUserEmail(null);
			}
		}	
	}
	
	public void ndlParseComplete() {
		// TODO Auto-generated method stub
		// Do any cleanup after model parsing is done
		parsingDone = true;
	}

	public String getReservationDomain() {
		return reservationDomain;
	}

	public void setReservationDomain(String reservationDomain) {
		this.reservationDomain = reservationDomain;
	}

	public String getReservation() {
		return reservation;
	}

	public void setReservation(String reservation) {
		this.reservation = reservation;
	}

	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		// TODO Auto-generated method stub
		if (bl == null)
			return;
		
		ndlNetworkConnection(bl, om, bandwidth,0, interfaces);
	}

}
