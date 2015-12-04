package orca.ndl.elements;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class DomainElement extends Device {
	
	protected ComputeElement ce;
	
	protected HashMap <DomainElement, OntResource> followedBy;
	protected HashMap <DomainElement, OntResource> precededBy;
	
	protected IPAddressRange ip_range;
	
	protected boolean needFollowerInterface = false;
	protected OntResource followerInterface=null;
	
	public DomainElement(OntModel model, OntResource domain) {
		super(model,domain);
		setParameters(model, domain);
		//this.name=getDomainPrefix(resource);
		ce=null;
		//this.print();
	}
	
	public DomainElement(OntModel idm, String url, String name) {
		super(idm,url,name);
		ce=null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(super.toString());

		sb.append("IP Range: " + ip_range + "\n");

		sb.append("followedBy: \n");
		if (followedBy != null) {
			for(Entry<DomainElement, OntResource> ee: followedBy.entrySet()) {
				sb.append(ee.getKey().getURI() + " <> " + ee.getValue());
			}
			sb.append("\n");
		}

		sb.append("precededBy: \n");
		if (precededBy != null) {
			for(Entry<DomainElement, OntResource> ee: precededBy.entrySet()) {
				sb.append(ee.getKey().getURI() + " <> " + ee.getValue());
			}
			sb.append("\n");
		}

		return sb.toString();
	}
	
	public String getDomainPrefix(Resource domain){
		String prefix=domain.getURI().split("\\#")[0];
		return prefix;
	}

	public void setParameters(OntModel ontModel, OntResource rs){
		super.setParameters(ontModel, rs);
		OntResource rs_ont=ontModel.getOntResource(rs);
		//get availableLabels out of networkService, only domain device needs this
		if(rs_ont.hasProperty(NdlCommons.domainHasServiceProperty)){
			Resource networkService = rs_ont.getRequiredProperty(NdlCommons.domainHasServiceProperty).getResource();
			Resource set=null,label_set=null,label=null;
			Statement typeStm,elementStm,lbStm,ubStm;
			String rType;
			LabelSet lSet;
			labelSets = new HashMap <String,LinkedList <LabelSet> > ();
			
			for (StmtIterator j=networkService.listProperties(NdlCommons.availableLabelSet);j.hasNext();){
				set = j.next().getResource();
				typeStm=set.getProperty(NdlCommons.domainHasResourceTypeProperty);
				rType=typeStm.getResource().getLocalName().toLowerCase();
				if(set.hasProperty(NdlCommons.domainIsAllocatable)){
					if(set.getProperty(NdlCommons.domainIsAllocatable).getBoolean()==false){
						continue;
					}
				}					
				LinkedList <LabelSet> lSetList = NdlCommons.getLabelSet(set.getURI(), rType,ontModel);
				if(lSetList!=null)
					labelSets.put(rType, lSetList);
			}
		}
	}

	//generate request NDL for each domain reservation
	public OntModel domainRequest() throws NdlException {
		OntModel domainRequestModel = NdlModel.createModel(OntModelSpec.OWL_MEM, true);
		Resource reservation = domainRequestModel.createIndividual(this.getURI(),NdlCommons.reservationOntClass);
		reservation.addProperty(NdlCommons.inDomainProperty, this.getResource());
		
		String castType = null;
		LinkedList <SwitchingAction> action_list = this.getActionList();
		int size=action_list.size();
		SwitchingAction action = null;
		for(int i=0;i<size;i++){
			action=(SwitchingAction) action_list.get(i);
			if(action==null) logger.info("No Action");
			else{
				castType= action.getCastType();
				if( (castType!=null) && (castType.equalsIgnoreCase(NdlCommons.multicast)) )
					break;
			};
		}
		
		OntResource connection_rs = null;
		int numPreceded=0,numFollowed=0;
		if( (castType!=null) && (castType.equalsIgnoreCase(NdlCommons.multicast)) ){
			if(this.getPrecededBySet()!=null)
				numPreceded=this.getPrecededBySet().size();
			if(this.getFollowedBy()!=null)
				numFollowed= this.getFollowedBySet().size();
			if(numPreceded+numFollowed>=2)
				castType=NdlCommons.multicast;
			else
				castType=NdlCommons.unicast;
		}
		if( (castType!=null) && (castType.equalsIgnoreCase(NdlCommons.multicast)) ){
			connection_rs = domainRequestModel.createIndividual(reservation.getURI()+"/conn",NdlCommons.topologyBroadcastConnectionClass);		
		}
		else
			connection_rs = domainRequestModel.createIndividual(reservation.getURI()+"/conn",NdlCommons.topologyNetworkConnectionClass);
		domainRequestModel.add(reservation,NdlCommons.collectionElementProperty, connection_rs);
		
		long bw = 0;
		LinkedList <SwitchingAction> actionList=this.getActionList();
		if(actionList!=null){
			action=(SwitchingAction) actionList.element();
			bw = action.getBw();
		}
		DomainResourceType rType=this.getResourceType();
		if(rType!=null){
			String rTypeURL = rType.getResourceTypeURL();
			Resource rType_rs=null;
			if((rTypeURL!=null) && (model!=null)){
				rType_rs=model.createResource(rTypeURL);
				connection_rs.addProperty(NdlCommons.domainHasResourceTypeProperty, rType_rs);
			}
			connection_rs.addProperty(NdlCommons.numResource, String.valueOf(rType.getCount()), XSDDatatype.XSDint);
		}
		connection_rs.addLiteral(NdlCommons.layerBandwidthProperty, Long.valueOf(bw));
		
		OntResource up_device_rs=null,down_device_rs=null;
		DomainElement parent_de = null;
		OntResource intf_ont = null,label_ont=null;
		if( (castType!=null) && (castType.equalsIgnoreCase(NdlCommons.multicast)) ){
			if(this.getPrecededBySet()!=null){
				for (Entry<DomainElement, OntResource> parent : this.getPrecededBySet()) {
					Resource intf_link_rs=null;
					intf_ont = parent.getValue();
					if(intf_ont.hasProperty(NdlCommons.linkTo)){
						intf_link_rs = intf_ont.getProperty(NdlCommons.linkTo).getResource();
						domainRequestModel.removeAll(intf_link_rs,NdlCommons.topologyInterfaceOfProperty,null);
						domainRequestModel.removeAll(intf_link_rs,NdlCommons.connectedTo,null);
					}else
						continue;
				}
			}
			if(this.getPrecededBySet()!=null){
				int i=0;
				for (Entry<DomainElement, OntResource> parent : this.getPrecededBySet()) {
					i++;
					parent_de=parent.getKey();
					intf_ont = parent.getValue();
					Resource intf_rs = null,intf_link_rs=null;
					
					if(intf_ont.hasProperty(NdlCommons.linkTo)){
						intf_link_rs = intf_ont.getProperty(NdlCommons.linkTo).getResource();
						up_device_rs=setNeighborDevice(domainRequestModel, this.getURI()+"/up/"+i, intf_link_rs,null);
					}else
						continue;
					
					if(up_device_rs!=null) {
						intf_rs = up_device_rs.getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
						connection_rs.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_rs);
						if(intf_rs.hasProperty(NdlCommons.linkTo)){
							intf_link_rs = intf_rs.getProperty(NdlCommons.linkTo).getResource();
							intf_link_rs.addProperty(NdlCommons.linkTo,intf_rs);
						}
					}
					
					if(parent_de.getStaticLabel()!=0){
						int label_id = (int) parent_de.getStaticLabel();
						label_ont = domainRequestModel.createOntResource(parent_de.getResource().getURI()+String.valueOf(label_id));
						label_ont.addProperty(NdlCommons.layerLabelIdProperty,String.valueOf(label_id),  XSDDatatype.XSDfloat);
						domainRequestModel.add(intf_rs,NdlCommons.layerLabel, label_ont);
					}
					logger.info("The multicast domain:precedent="+parent_de.getURI()+":"+parent_de.getStaticLabel()+":"+intf_rs.getURI()+":"+intf_link_rs.getURI());
				}				
			}
			if(this.getFollowedBy()!=null){
				for (Entry<DomainElement, OntResource> parent : this.getFollowedBySet()) {
					Resource intf_link_rs=null;
					intf_ont = parent.getValue();
					if(intf_ont.hasProperty(NdlCommons.linkTo)){
						intf_link_rs = intf_ont.getProperty(NdlCommons.linkTo).getResource();
						domainRequestModel.removeAll(intf_link_rs,NdlCommons.topologyInterfaceOfProperty,null);
						domainRequestModel.removeAll(intf_link_rs,NdlCommons.connectedTo,null);
					}else
						continue;
				}
			}
			if(this.getFollowedBy()!=null){
				int i=0;
				for (Entry<DomainElement, OntResource> parent : this.getFollowedBySet()) {
					i++;
					parent_de=parent.getKey();
					intf_ont = parent.getValue();
					Resource intf_rs = null,intf_link_rs=null;
					
					if(intf_ont.hasProperty(NdlCommons.linkTo)){
						intf_link_rs = intf_ont.getProperty(NdlCommons.linkTo).getResource();
						up_device_rs=setNeighborDevice(domainRequestModel, this.getURI()+"/down/"+i, intf_link_rs,null);
					}else
						continue;
					
					if(up_device_rs!=null) {
						intf_rs = up_device_rs.getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
						connection_rs.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_rs);
						if(intf_rs.hasProperty(NdlCommons.linkTo)){
							intf_link_rs = intf_rs.getProperty(NdlCommons.linkTo).getResource();
							intf_link_rs.addProperty(NdlCommons.linkTo,intf_rs);
						}
					}
					
					if(parent_de.getStaticLabel()!=0){
						int label_id = (int) parent_de.getStaticLabel();
						label_ont = domainRequestModel.createOntResource(parent_de.getResource().getURI()+String.valueOf(label_id));
						label_ont.addProperty(NdlCommons.layerLabelIdProperty,String.valueOf(label_id),  XSDDatatype.XSDfloat);
						domainRequestModel.add(intf_rs,NdlCommons.layerLabel, label_ont);
					}
					logger.info("The multicast domain:child="+parent_de.getURI()+":"+parent_de.getStaticLabel()+":"+intf_ont.getURI()+":"+intf_link_rs.getURI());
				}
			}
		}else{
			if(getUpNeighbour(getModel())!=null){
				domainRequestModel.removeAll(getUpNeighbour(getModel()),NdlCommons.topologyInterfaceOfProperty,null);
				domainRequestModel.removeAll(getUpNeighbour(getModel()),NdlCommons.connectedTo,null);
				up_device_rs=setNeighborDevice(domainRequestModel, this.getURI() + "/up", getUpNeighbour(getModel()), getUpLocal(getModel()));
			}
			
			if(getDownNeighbour(getModel())!=null){
				domainRequestModel.removeAll(getDownNeighbour(getModel()),NdlCommons.topologyInterfaceOfProperty,null);
				domainRequestModel.removeAll(getDownNeighbour(getModel()),NdlCommons.connectedTo,null);
				down_device_rs=setNeighborDevice(domainRequestModel,this.getURI()+"/down", getDownNeighbour(getModel()),getDownLocal(getModel()));
			}
			Resource intf_rs = null,intf_link_rs=null;
			if(up_device_rs!=null) {
				intf_rs = up_device_rs.getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
				connection_rs.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_rs);
				if(intf_rs.hasProperty(NdlCommons.linkTo)){
					intf_link_rs = intf_rs.getProperty(NdlCommons.linkTo).getResource();
					intf_link_rs.addProperty(NdlCommons.linkTo,intf_rs);
				}
			}
			
			if(down_device_rs!=null) {
				intf_rs = down_device_rs.getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
				connection_rs.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_rs);
				if(intf_rs.hasProperty(NdlCommons.linkTo)){
					intf_link_rs = intf_rs.getProperty(NdlCommons.linkTo).getResource();
					intf_link_rs.addProperty(NdlCommons.linkTo,intf_rs);
				}			
			}
		}	
		return domainRequestModel;
	}
	//Create a fake edge device to the sub-request to a domain.
	public OntResource setNeighborDevice(OntModel domainRequestModel, String domain_url,Resource neighbor_intf_rs, OntResource local){
		if(neighbor_intf_rs==null)
			return null;
		OntResource device_rs=domainRequestModel.createIndividual(domain_url,NdlCommons.computeElementClass);
		device_rs.addLiteral(NdlCommons.domainIsAllocatable, false);
		Statement s=this.getResource().getProperty(NdlCommons.hasSwitchMatrix);
		Resource sm_rs;
		if(s!=null){
			sm_rs = s.getResource();
			domainRequestModel.add(device_rs,NdlCommons.hasSwitchMatrix,sm_rs);
			domainRequestModel.add(sm_rs.getProperty(NdlCommons.switchingCapability));
			domainRequestModel.addLiteral(sm_rs, NdlCommons.domainIsAllocatable, false);
		}
		
		Resource rs=neighbor_intf_rs;
		neighbor_intf_rs=domainRequestModel.createIndividual(neighbor_intf_rs.getURI(),NdlCommons.interfaceOntClass);
		device_rs.addProperty(NdlCommons.topologyHasInterfaceProperty,neighbor_intf_rs);  //the upstream domain interface.
		//domainRequestModel.removeAll(neighbor_intf_rs,NdlCommons.topologyInterfaceOfProperty,null);
		//domainRequestModel.removeAll(neighbor_intf_rs,NdlCommons.connectedTo,null);
		domainRequestModel.add(neighbor_intf_rs,NdlCommons.topologyInterfaceOfProperty, device_rs);
		while(true){
			neighbor_intf_rs=domainRequestModel.createIndividual(rs.getURI(),NdlCommons.interfaceOntClass);
			for (StmtIterator j=rs.listProperties();j.hasNext();){
				Statement s_type = (Statement) j.next();
				domainRequestModel.add(neighbor_intf_rs,s_type.getPredicate(),s_type.getObject());
			}
			ResultSet results=NdlCommons.getLayerAdapatation(this.getModel(),rs.getURI());
			String varName=(String) results.getResultVars().get(0);
			if (results.hasNext()){
				rs=results.nextSolution().getResource(varName);
			}
			else break;
		}	
		return device_rs;
	}
	
	public void copyDependency(DomainElement dd){
		DomainElement parent_de=null,child_de=null;
		OntResource intf_ont=null,child_intf_ont=null;
		
		if(dd.getClientInterface()!=null)
			for(Interface intf:dd.getClientInterface()){
				if(this.getClientInterface()!=null && this.getClientInterface().contains(intf))
					continue;
				this.addClientInterface(intf);
			}
		
		if(dd.getPrecededBySet()!=null){
			for (Entry<DomainElement, OntResource> parent : dd.getPrecededBySet()) {
				parent_de=parent.getKey();
				intf_ont = parent.getValue();
				if(this.getPrecededBy()!=null && this.getPrecededBySetByElement(parent_de.getURI())!=null)
					continue;
				this.setPrecededBy(parent_de, intf_ont);
				
				if(parent_de.getFollowedBySet()!=null){
					for (Entry<DomainElement, OntResource> child : parent_de.getFollowedBySet()) {
						child_de=child.getKey();
						child_intf_ont = child.getValue();
						if(child_de==dd){
							parent_de.setFollowedBy(this, child_intf_ont);
							break;
						}
					}
					parent_de.removeFollowedByElement(child_de);
				}else
					logger.error("Missed follower:"+parent_de.getName());
			}
		}
		
		if(dd.getFollowedBySet()!=null){
			for (Entry<DomainElement, OntResource> parent : dd.getFollowedBySet()) {
				parent_de=parent.getKey();
				intf_ont = parent.getValue();
				if(this.getFollowedBy()!=null && this.getFollowedBy().get(parent_de)!=null)
					continue;
				this.setFollowedBy(parent_de, intf_ont);
				
				if(parent_de.getPrecededBySet()!=null){
					for (Entry<DomainElement, OntResource> child : parent_de.getPrecededBySet()) {
						child_de=child.getKey();
						child_intf_ont = child.getValue();
						if(child_de==dd){
							parent_de.setPrecededBy(dd, child_intf_ont);
							break;
						}
					}
					parent_de.removePrecededByElement(child_de);
				}else
					logger.error("Missed follower:"+parent_de.getName());
			}
		}
		
	}
	
	public ComputeElement getCe() {
		return ce;
	}

	public void setCe(ComputeElement ce) {
		this.ce = ce;
	}

	public void setFollowedBy(HashMap <DomainElement,OntResource> followedBy) {
		this.followedBy = followedBy;
	}
	
	public void setPrecededBy(HashMap <DomainElement,OntResource> precededBy) {
		this.precededBy = precededBy;
	}
	
	public HashMap <DomainElement,OntResource> getFollowedBy() {
		return followedBy;
	}
	public Set <Entry <DomainElement,OntResource>> getFollowedBySet(){
		if(this.followedBy==null) return null;
		return this.followedBy.entrySet();
	}
	public OntResource getFollowedBySetByElement(String de){
		if(followedBy==null)
			return null;
		for(Entry <DomainElement,OntResource> entry:followedBy.entrySet()){
			if(entry.getKey().getURI()==de || entry.getKey().getName()==de)
				return entry.getValue();
		}
		return null;
	}
	
	public void setFollowedBy(DomainElement follower,Resource remote_intf) {
		setFollowedBy(follower,this.model.getOntResource(remote_intf));
	}	
	public void setFollowedBy(DomainElement follower,OntResource remote_intf) {
		if(this.followedBy ==null) followedBy = new HashMap <DomainElement,OntResource>();
		followedBy.put(follower,remote_intf);
	}
	public void removeFollowedByElement(DomainElement de){
		if(followedBy!=null)
			this.followedBy.remove(de);
	}
	public HashMap <DomainElement,OntResource> getPrecededBy() {
		return precededBy;
	}
	public OntResource getPrecededBySetByElement(String de){
		if(precededBy==null)
			return null;
		for(Entry <DomainElement,OntResource> entry:precededBy.entrySet()){
			if(entry.getKey().getURI()==de)
				return entry.getValue();
		}
		return null;
	}	
	public Set <Entry <DomainElement,OntResource>> getPrecededBySet(){
		if(this.precededBy==null) return null;
		return this.precededBy.entrySet();
	}
	public void setPrecededBy(DomainElement precededBy, Resource remote_intf) {
		setPrecededBy(precededBy,this.model.getOntResource(remote_intf));
	}	
	public void setPrecededBy(DomainElement precededBy, OntResource remote_intf) {
		if(this.precededBy ==null) this.precededBy = new HashMap <DomainElement,OntResource>();
		this.precededBy.put(precededBy,remote_intf);
	}
	public void removePrecededByElement(DomainElement de){
		if(precededBy!=null)
			this.precededBy.remove(de);
	}
	
	public IPAddressRange getIp_range() {
		return ip_range;
	}

	public void setIp_range(IPAddressRange ip_range) {
		this.ip_range = ip_range;
	}
	
	public boolean isNeedFollowerInterface() {
		return needFollowerInterface;
	}

	public void setNeedFollowerInterface(boolean needFolloerInterface) {
		this.needFollowerInterface = needFolloerInterface;
	}

	public OntResource getFollowerInterface() {
		return followerInterface;
	}

	public void setFollowerInterface(Resource followerInterface) {
		this.followerInterface = this.model.getOntResource(followerInterface);
	}

	public String getDomainName() {
		String UriSeparator = "#";
		String UriSuffix = "/Domain";
		String domain_url = this.getInDomain();
		int index = domain_url.indexOf(UriSeparator);
		if (index >= 0) {
			int index2 = domain_url.indexOf(UriSuffix, index);
			if (index2 >= 0) {
				return domain_url.substring(index+1, index2);
			}
			else{
				return domain_url.substring(index+1, domain_url.length());
			}
		}
		return null;
	}
}
