package orca.embed.cloudembed.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.UUID;

import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.cloudembed.IConnectionManager;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.ModifyElement;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.ModifyReservations;
import orca.embed.workflow.RequestParserListener;
import orca.ndl.DomainResourceType;
import orca.ndl.INdlModifyModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.NdlRequestParser;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.IPAddress;
import orca.ndl.elements.IPAddressRange;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.RequestSlice;
import orca.shirako.container.Globals;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDB;

public class ModifyHandler extends UnboundRequestHandler {

	ModifyReservations modifies = new ModifyReservations();
	LinkedList <Device> addedDevices = new LinkedList <Device> ();
	LinkedList <Device> modifiedDevices = new LinkedList <Device> ();
	
	public ModifyHandler() throws NdlException {
		super();
	}
	
	
	public ModifyHandler(IConnectionManager icm) throws NdlException {
		super(icm);
	}
	
	
	/**
	 * Create handler with in-memory model
	 * @param substrateFile
	 * @throws IOException
	 * @
	 */
	public ModifyHandler(String substrateFile) throws IOException, NdlException {
		super(substrateFile);
	}
	
	/**
	 * Create handler with TDB-backed model in a directory with specified path prefix
	 * @param substrateFile
	 * @param tdbPrefix
	 * @throws IOException
	 * @throws NdlException
	 */
	public ModifyHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
		super(substrateFile, tdbPrefix);
	}
	
	/**
	 * Create a handler with TDB-backed blank model or try to recover existing TDB model
	 * @param tdbPrefix
	 * @param recover
	 * @throws IOException
	 * @throws NdlException
	 */
	public ModifyHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
		super(tdbPrefix, recover);
	}
	
	public SystemNativeError modifySlice(DomainResourcePools domainResourcePools,
			Collection <ModifyElement> modifyElements, OntModel manifestOnt, String sliceId,
			HashMap <String,Collection <DomainElement>> nodeGroupMap,
			HashMap <String,DomainElement> firstGroupElement, OntModel requestModel) throws UnknownHostException, InetNetworkException{
		this.modifyVersion++;
		
		SystemNativeError error=null;
		if( (modifyElements==null) || (modifyElements.size()==0) ){
			error = new SystemNativeError();
			error.setErrno(8);
			error.setMessage("modifyElements is empty!");
			logger.error(error.toString());
			return error;
		}
		logger.debug("ModifyHandler.modifySlice() starts....");
		LinkedList <ModifyElement> addList = new LinkedList<ModifyElement>();
		try{
			Iterator<ModifyElement> mei = modifyElements.iterator();
			while(mei.hasNext()){
				ModifyElement me = mei.next();
				logger.debug("ModifyHandler.modifySlice():"+me.getModType());
				if(me.getModType().equals(INdlModifyModelListener.ModifyType.REMOVE)){
					error=removeElement(me, manifestOnt, nodeGroupMap, deviceList);
				}
			
				if(me.getModType().equals(INdlModifyModelListener.ModifyType.ADD) || me.getModType().equals(INdlModifyModelListener.ModifyType.MODIFY)){
					addList.add(me);
				}
			
				if(me.getModType().equals(INdlModifyModelListener.ModifyType.INCREASE)){
					error = addElements(me, manifestOnt, nodeGroupMap, firstGroupElement, requestModel, deviceList);
				}
			}
			this.addElement(domainResourcePools,addList,manifestOnt,sliceId);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		//modify the manifest
		OntResource manifest=NdlCommons.getOntOfType(manifestOnt, "request:Manifest");
		this.createManifest(manifestOnt, manifest, addedDevices);
		return error;
	}
	
	public void modifyComplete(){
		this.modifies.clear();
		this.addedDevices.clear();
		this.modifiedDevices.clear();
	}
	
	public void addElement(DomainResourcePools domainResourcePools,LinkedList <ModifyElement> meList,OntModel manifestOntModel, String sliceId) throws NdlException, IOException{
		//generating new reservation in the format of a request RDF model
		if(meList.isEmpty())
			return;
		Resource me=meList.element().getObj();
		String ns_str=me.getNameSpace();
		OntModel modifyRequestModel = NdlModel.createModel(OntModelSpec.OWL_MEM, true);
		Resource reservation = modifyRequestModel.createIndividual(ns_str,NdlCommons.reservationOntClass);
		for(ModifyElement mee:meList){
			me = mee.getObj();
			OntResource connection_rs = modifyRequestModel.createOntResource(me.getURI());
			modifyRequestModel.add(reservation,NdlCommons.collectionElementProperty, connection_rs);
			copyProperty(modifyRequestModel, me);
		}
		
		String fileName = "mod-req.rdf";
        OutputStream fsw = new FileOutputStream(fileName);
        modifyRequestModel.write(fsw);
		
		//parsing request
		RequestParserListener parserListener = new RequestParserListener();		
		// run the parser (to create Java objects)
		NdlRequestParser nrp = new NdlRequestParser(modifyRequestModel, parserListener, NdlModel.ModelType.TdbPersistent, 
				Globals.TdbPersistentDirectory + Globals.PathSep + "controller" + Globals.PathSep + "modify-" + sliceId);
		nrp.processRequest();		
		RequestReservation request = parserListener.getRequest();
		OntModel modifyModel = request.getModel();
		RequestSlice slice=request.getSlice();
		Collection<NetworkElement> requestElements = request.getElements();		
		SystemNativeError err = request.getError();
		if(err!=null){
			logger.error("Ndl request parser unable to parse request:"+err.toString());
			return;
		}
		// TODO: check if the request is already fully bound in one domain, preparing for topology splitting 				
		boolean bound = request.generateGraph(requestElements);
		String reservationDomain = request.getReservationDomain();

		if(reservationDomain == null){// invoke the embedding code
			err = this.runEmbedding(bound, request, domainResourcePools);
		}else{  //intra-domain embedding
			err = this.runEmbedding(reservationDomain,request,domainResourcePools);
		}
		
		for(NetworkElement device:this.getDeviceList()){
			DomainElement dd = (DomainElement) device;
			if(dd.getModifyVersion() < this.modifyVersion){
				if(dd.isModify()){
					modifies.addModifedElement(device.getResource());
					modifiedDevices.add(dd);
				}else{
					modifies.addAddedElement(device.getResource());
					addedDevices.add(dd);
				}
			}
		}	
		
		TDB.sync(modifyModel);
	}
	
	private void copyProperty(OntModel domainRequestModel, Resource me_rs){
		StmtIterator it = me_rs.listProperties();
		if(it==null)
			return;
		Statement st = null;
		Property pr=null;
		Resource sb_rs=null,ob_rs=null; 
		while(it.hasNext()){
			st = it.next();
			pr = st.getPredicate();
			sb_rs = domainRequestModel.createResource(st.getSubject().getURI());
			try{
				ob_rs = domainRequestModel.createResource(st.getResource().getURI());
				domainRequestModel.add(sb_rs,pr,ob_rs);
				copyProperty(domainRequestModel,st.getResource());
			}catch(ResourceRequiredException e){
				domainRequestModel.add(sb_rs,pr,st.getLiteral());
			}
		}
	}
	
	protected SystemNativeError  addElements(ModifyElement me,OntModel manifestOntModel, 
			HashMap <String,Collection <DomainElement>> nodeGroupMap,
			HashMap <String,DomainElement> firstGroupElement, 
			OntModel requestModel, LinkedList<NetworkElement> deviceList) throws InetNetworkException, UnknownHostException, Exception{
		SystemNativeError error = null;	
		
		int units = me.getModifyUnits();
		String n = me.getSub().getURI();
		int i = n.lastIndexOf("#");
		String group = i>=0 ? n.substring(i+1):n;
		logger.info("addElements() starts, units="+units);
		logger.debug("nodeGroupMap size="+nodeGroupMap.size());
		Collection <DomainElement> cde = nodeGroupMap.get(group);
		if((cde==null) || (cde.isEmpty()) ){
			error = new SystemNativeError();
			error.setErrno(8);
			error.setMessage("Adding element: group doesn't exist: group: "+group+";cde="+cde+";nodeGroup size="+nodeGroupMap.size());
			logger.error(error.toString());
			return error;
		}
		
		DomainElement firstElement = firstGroupElement.get(group);
		if(firstElement==null){
			logger.error("Original element in the gorup is missed! group="+group);
			Iterator <DomainElement> cde_it = cde.iterator();
			while(cde_it.hasNext()){
				firstElement = cde_it.next();
				break;
			}
		}else{
			logger.info("First element:"+firstElement.getURI()+";cde size="+cde.size());
		}
		
		HashMap <String,IPAddressRange> group_base_ip = null;
		try{
			group_base_ip = getIPRange(firstElement,cde); //<base IP network address,IP range BitSet>
			if(group_base_ip==null)
				throw new Exception("group_base_ip is null!");
		}catch(Exception e){
			e.printStackTrace();
		}
		
		int hole=-1;	
		HashMap <DomainElement,Integer> parentMap = null;
		String domainName = firstElement.getResourceType().getDomainURL();
		
		for(i=0;i<units;i++){
			logger.info("create new node from firstElement:i="+i);

			DomainElement link_device = null;
			if(firstElement.getPrecededBy()!=null){
				parentMap = new HashMap <DomainElement,Integer>();
				for(Entry <DomainElement,OntResource> parent:firstElement.getPrecededBySet()){
					link_device = parent.getKey();
					logger.debug("parent link_device="+link_device.getName());
					if(link_device.getCe()!=null) //node dependency
						continue;
					String ip_addr = null;
					if(parent.getValue().getProperty(NdlCommons.layerLabelIdProperty)!=null){
						ip_addr=parent.getValue().getProperty(NdlCommons.layerLabelIdProperty).getString();
						hole = findIPRangeHole(group_base_ip,ip_addr);
						parentMap.put(link_device, new Integer(hole));
					}
				}
				DomainElement edge_device=null;
				for(Entry <DomainElement,Integer> entry: parentMap.entrySet()){
					link_device = entry.getKey();
					hole = entry.getValue();
					if(edge_device==null){
						edge_device=createNewNode(firstElement,hole,link_device,domainName,manifestOntModel, requestModel, deviceList);
					}
					createInterface(firstElement, edge_device,hole,link_device);
				}
			}else{
				logger.debug("firstElement has no parent!");
				createNewNode(firstElement,-1,null,domainName, manifestOntModel, requestModel, deviceList);
			}
		}		
		
		for(i=0;i<addedDevices.size();i++){
			NetworkElement ne = addedDevices.get(i);
			ne.setModifyVersion(this.modifyVersion);
            cde.add((DomainElement) ne);
			logger.debug("ModifyHandler:added:"+i+":"+ne.getURI()+":"+ne.getName());
        }
		
		return error;
	}	
	
	protected DomainElement createNewNode(DomainElement element,int hole,DomainElement link_device, 
			String domainName,OntModel manifestModel, OntModel requestModel, LinkedList<NetworkElement> deviceList) throws UnknownHostException, InetNetworkException{
		
		ComputeElement ce=null,element_ce=null;

		logger.info("Creating new node:hole="+hole+"domainName="+domainName);
		DomainResourceType dType = new DomainResourceType(element.getResourceType().getResourceType(),element.getResourceType().getCount());
		dType.setDomainURL(domainName);
		dType.setCount(1);
		int index = element.getName().lastIndexOf("/");
		String name = index>=0? element.getName().substring(0, index):element.getName();
		if(hole>=0)
			name=name+("/")+String.valueOf(hole)+"/"+Math.abs(UUID.randomUUID().toString().hashCode());
		else
			name=name+"/NoIP/"+UUID.randomUUID().toString();	
		element_ce = element.getCe();
		//String url=element.getURI();
		String url=name;
		ce = element_ce.copy(manifestModel, requestModel,url,name);
		OntResource ce_ont = ce.getModel().createOntResource(name);
		if(element.getResource()!=null && element.getResource().hasProperty(NdlCommons.specificCEProperty))
			ce_ont.addProperty(NdlCommons.specificCEProperty, 
					element.getResource().getProperty(NdlCommons.specificCEProperty).getResource());
		ce.setResourceType(dType);
		
		DomainElement edge_device=new DomainElement(manifestModel,url,name);
		edge_device.setCe(ce);
		edge_device.setResourceType(dType);
		edge_device.setModifyVersion(edge_device.getModifyVersion()+1);
		
		deviceList.add(edge_device);
		modifies.addAddedElement(edge_device.getResource());
		addedDevices.add(edge_device);

		return edge_device;
	}
	
	protected void createInterface(DomainElement element,DomainElement edge_device,int hole,DomainElement link_device) throws InetNetworkException, UnknownHostException{
		if(link_device==null)
			return;
		
		ComputeElement ce = edge_device.getCe();

		ComputeElement element_ce=element.getCe();
		NetworkConnection ncByInterface=null;
		
		OntResource intf_ont=element.getPrecededBySetByElement(link_device.getURI());
		if(intf_ont==null)
			return;
		Interface intf=element.getClientInterfaceByURI(intf_ont.getURI());
		
		if(intf!=null){				
			IPAddress ip = (IPAddress) intf.getLabel();
			InetNetwork ip_str_IP= new InetNetwork(ip.address,ip.netmask);
			String network_str= null;
			if(ip.address!=null)
				network_str=ip_str_IP.getNetwork();
			IPAddress new_ip = null;
			String url = ip.getURI();
			int index = url.lastIndexOf("/intf");
			url = index>=0? url.substring(0, index):url;
			try {
				new_ip = ip.getNewIpAddress(edge_device.getModel(), network_str, ip.netmask, url, hole);
				url = new_ip.getURI()+"/intf";
			} catch (Exception e) {
				e.printStackTrace();
			}
			Interface new_intf = new Interface(edge_device.getModel(), url, url);
			new_intf.getResource().addProperty(NdlCommons.ip4LocalIPAddressProperty, new_ip.getResource(edge_device.getModel()));
			if(intf_ont.getProperty(NdlCommons.hostInterfaceName)!=null){
				String site_host_interface = intf_ont.getProperty(NdlCommons.hostInterfaceName).getString();
				new_intf.getResource().addProperty(NdlCommons.hostInterfaceName,site_host_interface);
			}
			if(new_ip.cidr!=null)
				new_intf.getResource().addProperty(NdlCommons.layerLabelIdProperty,new_ip.cidr);
			
			new_intf.setLabel(new_ip);
			ce.addClientInterface(new_intf);
			edge_device.addClientInterface(new_intf);
			new_intf.getResource().addProperty(NdlCommons.OWL_sameAs, intf.getResource());	
			
			ncByInterface = element_ce.getConnectionByInterfaceName(intf);
			ce.setInterfaceName(ncByInterface, new_intf);	
			
			logger.debug("New intf="+new_intf.getURI()+";ip="+new_ip.getResource(edge_device.getModel())
					+";cidr="+new_ip.cidr+";intf.model="+new_intf.getModel().equals(edge_device.getModel())
					+";ncByInterface="+ncByInterface);
			
			setEdgeNeighbourhood(edge_device,link_device, new_intf, ncByInterface);
		}else{
			String domain_name = null;
			if(link_device.getResource()!=null)
				domain_name = link_device.getResource().getProperty(NdlCommons.inDomainProperty).getResource().getURI(); 
			else{
				logger.error("No resource, url="+link_device.getURI());
				return;
			}
			logger.debug("ModifyHandler:no IP interface! intf_ont="+intf_ont.getURI()+";domain="+domain_name);
			OntResource edge_intf = getEdgeInterface(domain_name,null);
			if(edge_intf!=null){
				link_device.setFollowedBy(edge_device, edge_intf);
				edge_device.setPrecededBy(link_device, edge_intf);
			}
		}
	}
	
	protected SystemNativeError removeElement(ModifyElement me,OntModel manifestOntModel, 
			HashMap <String,Collection <DomainElement>> nodeGroupMap, LinkedList<NetworkElement> deviceList){
		SystemNativeError error = null;
		Iterator <NetworkElement> bei = deviceList.iterator();
		NetworkElement device = null;
		boolean remove=false;
		while(bei.hasNext()){
			device = (NetworkElement) bei.next();
			logger.info("Remove Device name="+device.getName()+";url="+device.getURI()+";obj url="+me.getObj().getURI());
			if(device.getURI().equals(me.getObj().getURI()) || device.getName().equals(me.getObj().getURI())){
				remove=true;
				break;
			}
		}
		
		if(remove==true){
			//clear dependency if any
			DomainElement de = (DomainElement) device;
			HashMap<DomainElement, OntResource> preds = de.getPrecededBy();
			DomainElement pe = null;
			LinkedList <DomainElement> pes = new LinkedList <DomainElement> ();
			if (preds != null) {  
				for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
					pe = parent.getKey();
					pes.add(pe);
					//pe.removeFollowedByElement(de);
					//de.removePrecededByElement(pe);
				}
				Iterator <DomainElement> pes_it = pes.iterator();
				while(pes_it.hasNext()){
					pe=pes_it.next();
					pe.removeFollowedByElement(de);
					//de.removePrecededByElement(pe);
				}
				pes.clear();
			}
			preds = de.getFollowedBy();
			if (preds != null) {  
				for (Entry<DomainElement, OntResource> parent : de.getFollowedBySet()) {
					pe = parent.getKey();
					pes.add(pe);
					//pe.removePrecededByElement(de);
					//de.removeFollowedByElement(pe);
				}
				Iterator <DomainElement> pes_it = pes.iterator();
				while(pes_it.hasNext()){
					pe=pes_it.next();
					pe.removePrecededByElement(de);
					//de.removeFollowedByElement(pe);
				}
				pes.clear();
			}
			
			String n = me.getSub().getURI();
			int i = n.lastIndexOf("#");
			String group = i>=0 ? n.substring(i+1):n;
			Collection <DomainElement> cde = nodeGroupMap.get(group);
			cde.remove(device);
			
			//remove from domainInConnectionList and deviceList
			String name = device.getName();			
			OntResource device_ont = manifestOntModel.getOntResource(name);
			logger.debug("device_ont:"+device_ont.getURI()+";device domain="+device_ont.getProperty(NdlCommons.hasURLProperty));
			boolean inDomainList = this.domainInConnectionList.remove(device_ont);
			boolean inDeviceList = deviceList.remove(device); 
			if( (!inDomainList) && (!inDeviceList)){
				error = new SystemNativeError();
				error.setErrno(7);
				error.setMessage("Removed element doesn't exist: name: "+device.getName()+";url=" + device.getURI());
				logger.error(error.toString());
			}else{
				logger.debug("to be removed ont:"+device_ont.getURI());

				modifies.addRemovedElement(device_ont);
			}
			//close reservation and modify manifest will be done in ReservationConverter in the controller.
		}
		return error;
	}
	
	public ModifyReservations getModifies() {
		return modifies;
	}

	public LinkedList<Device> getAddedDevices() {
		return addedDevices;
	}

	public LinkedList<Device> getModifiedDevices() {
		return modifiedDevices;
	}

}
