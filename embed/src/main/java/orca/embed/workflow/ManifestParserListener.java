package orca.embed.workflow;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jwhoisserver.utils.InetNetwork;
import orca.ndl.DomainResourceType;
import orca.ndl.INdlManifestModelListener;
import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlManifestParser;
import orca.ndl.NdlRequestParser;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.RequestSlice;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ManifestParserListener implements INdlManifestModelListener,
		INdlRequestModelListener {

	protected OntModel model = null;
	protected LinkedList<NetworkElement> deviceList = new LinkedList<NetworkElement>();
	protected LinkedList<DomainElement> domainList = new LinkedList<DomainElement>(); //for dependency recoever
	protected HashMap <String,LinkedList<String>> shared_IP_set = new HashMap <String,LinkedList<String>>(); 
	protected Date creationTime = null;
	protected Date expirationTime = null;
	protected Set<String> logins = new HashSet<String>();
	protected RequestSlice currentSlice=null;
	protected Map<String, Integer> stitchportLabels = new HashMap<String, Integer>();
	private boolean requestPhase = true;
	private Logger logger;
	
	public ManifestParserListener(Logger log) {
		logger=log;
	}

	public void parse(OntModel manifest) throws NdlException {
		model=manifest;
		requestPhase = true;
		// parse as request
		NdlRequestParser nrp = new NdlRequestParser(manifest, this);
		// something wrong with request model that is part of manifest
		// some interfaces belong only to nodes, and no connections
		// for now do less strict checking so we can get IP info
		// 07/2012/ib
		nrp.doLessStrictChecking();
		nrp.processRequest();

		// parse as manifest
		requestPhase = false;
		NdlManifestParser nmp = new NdlManifestParser(manifest, this);
		nmp.processManifest();
	}
	
	@Override
	public void ndlNode(Resource ce, OntModel m, Resource ceClass,
			List<Resource> interfaces) {
		// ignore request items
		if (requestPhase)
			return;
		
		if (ce == null){
			logger.error("ce is null");
			return;
		}
		
		if (NdlCommons.isStitchingNodeInManifest(ce)) {
			if (interfaces.size() == 1) {
				String pl = NdlCommons.getLayerLabelLiteral(interfaces.get(0));
				try {
					Integer pli = Integer.parseInt(pl);
					Resource spurl = NdlCommons.getLinkTo(interfaces.get(0));
					stitchportLabels.put(spurl.getURI(), pli);
					logger.debug("Remembering stitchport label " + pli + " for port url " + spurl.getURI());
				} catch (Exception ee) {
					logger.error("Unable to convert label " + pl + " into integer, skipping");
				}
			} 
			return;
		}
		
		logger.debug("ManifestParser:ce="+ce.getURI()+";interfaces size="+interfaces.size());
		
		String name = ce.getURI();
		String url = name;

		DomainElement edge_device=new DomainElement(m,url,name) ;
		ComputeElement c_e = new ComputeElement(m,url,name);
		edge_device.setCe(c_e);
		
		setCEProperty(c_e,ce,ceClass);
		
		edge_device.setResourceType(c_e.getResourceType());
		String ip_str=null,netmask=null;
		InetNetwork ip_addr=null;
		try {
			for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
				Resource intR = it.next();
				Interface intf = new Interface(m, m.createOntResource(intR.getURI()));
				if(intR.getProperty(NdlCommons.layerLabelIdProperty)!=null){
					ip_str=intR.getProperty(NdlCommons.layerLabelIdProperty).getString();
					ip_addr=new InetNetwork(ip_str);
					int index = ip_str.indexOf("/");
					ip_str=index>0?ip_str.split("\\/")[0]:ip_str;
					netmask=ip_addr.cidr2mask(ip_addr.getCidr(), 4).toString().split("/")[1];
					logger.debug("ip address:"+ip_str+";netmask="+netmask);
					intf.setLabel(ip_str, netmask);
				}
				edge_device.addClientInterface(intf);
				logger.debug("add client interface for:"+edge_device.getURI()+";interface;"+intf.getURI());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		deviceList.add(edge_device);
		domainList.add(edge_device);
		
		logins.addAll(NdlCommons.getNodeLogins(ce));
	}
	
	private void setDependency() {
		Resource ce_rs=null,parent_rs=null,parent_domain_rs=null,interface_rs=null;
		DomainElement parent=null;
		for(DomainElement ce:domainList){
			ce_rs=model.getOntResource(ce.getURI());
			if(ce_rs==null){
				logger.error("No ontresource in model:"+ce.getURI());
				continue;
			}
			String domainName = ce.getInDomain();
			ce_rs.addProperty(NdlCommons.topologyHasURL, domainName);
			if(ce_rs.hasProperty(NdlCommons.manifestHasParent)){
				List<Resource> ifs = new ArrayList<Resource>();
		    	for (StmtIterator sti = ce_rs.listProperties(NdlCommons.manifestHasParent); sti.hasNext();) {
		    		parent_rs=sti.next().getResource();
		    		if(parent_rs.getProperty(NdlCommons.collectionElementProperty)==null){
		    			logger.error("Parent element missed: ce_rs="+ce_rs.getURI()+";parent="+parent_rs.getURI());
		    			continue;
		    		}
		    		parent_domain_rs=parent_rs.getPropertyResourceValue(NdlCommons.collectionElementProperty);
		    		interface_rs=NdlCommons.parentGetInterface(model,ce_rs.getURI(),parent_domain_rs.getURI());
		    		if(storageSharedLink(parent_domain_rs)){
		    			parent=new DomainElement(model,parent_domain_rs.getURI(),parent_domain_rs.getURI());
		    		}else
		    			parent=getDevice(parent_domain_rs);
		    		
		    		if(parent!=null && interface_rs!=null){
		    			ce.setPrecededBy(parent, interface_rs);
		    			parent.setFollowedBy(ce, interface_rs);
		    			Resource nc_rs=null;
		    			if(ce.getCe()!=null){
		    				if(parent_domain_rs.hasProperty(NdlCommons.inRequestNetworkConnection))
		    					nc_rs = parent_domain_rs.getProperty(NdlCommons.inRequestNetworkConnection).getResource();
		    				else
		    					nc_rs=parent_domain_rs;
		    				NetworkConnection nc = new NetworkConnection(parent.getModel(),nc_rs.getURI(),nc_rs.getURI());
		    				ce.getCe().setInterfaceName(nc, ce.getClientInterfaceByURI(interface_rs.getURI()));
		    				ce.getCe().addDependency(nc);
		    				
		    				//save the ip address on shared vlan for recovery
		    				if(storageSharedLink(parent_domain_rs)){
		    					if(parent_domain_rs.hasProperty(NdlCommons.inDomainProperty)){
		    						String inDomainName = parent_domain_rs.getProperty(NdlCommons.inDomainProperty).getResource().getURI();	
		    						logger.debug("recover: shared vlan domain="+inDomainName);
		    						Resource ip_rs=null;
		    						String ip_str=null;
		    						LinkedList<String> ip_str_set = shared_IP_set.get(inDomainName); 
		    						if(ip_str_set==null){
		    							ip_str_set=new LinkedList<String>();
		    							shared_IP_set.put(inDomainName, ip_str_set);
		    						}
		    						if(interface_rs.getProperty(NdlCommons.ip4LocalIPAddressProperty)!=null){
		    							ip_rs = interface_rs.getProperty(NdlCommons.ip4LocalIPAddressProperty).getResource();
		    							ip_str = ip_rs.getProperty(NdlCommons.layerLabelIdProperty).getString();
		    							if(ip_str!=null){
		    								ip_str_set.add(ip_str);
		    								logger.debug("recovered ip="+ip_str);
		    							}
		    						}
		    					}
		    				}
		    			}
		    			logger.debug("recover:dependency:parent="+parent.getURI()
		    					+";child="+ce.getURI()+";intf="+interface_rs.getURI());
		    			if(nc_rs!=null)
		    					logger.debug(";ncByInterface="+nc_rs.getURI());
		    		}
		    		else
		    			logger.error("recover:dependency:something missed:parent="+parent_domain_rs+";intf="+interface_rs);
		    	}
		    }else{
				logger.warn("Manifest Parser: no dependency, ce="+ce.getURI());
			}
		}
	}
	
	public boolean storageSharedLink(Resource domain_rs){
		boolean isStorage = false;
		Resource rc=null;
		if(domain_rs.hasProperty(NdlCommons.domainIsAllocatable)){
			if(domain_rs.getProperty(NdlCommons.domainIsAllocatable).getBoolean()==false){
				if(domain_rs.hasProperty(NdlCommons.collectionItemProperty)){
					List <Statement> st_list=domain_rs.listProperties(NdlCommons.collectionItemProperty).toList();
					for (Statement st:st_list){
						rc = st.getResource();
						if(rc.hasProperty(NdlCommons.domainHasResourceTypeProperty)){
							if(rc.getProperty(NdlCommons.domainHasResourceTypeProperty)
									.getResource().getURI().endsWith("LUN")){
								isStorage=true;
								break;
							}
								
						}
							
					}
				}
			}
		}
		return isStorage;
	}
	
	public HashMap<String, LinkedList<String>> getShared_IP_set() {
		return shared_IP_set;
	}

	private DomainElement getDevice(Resource rs){
		if ((rs == null) || (deviceList==null))
			return null;
		for (NetworkElement ne: deviceList){
			if (rs.getURI().equals(ne.getName()) ){
				return (DomainElement) ne;
			}
		}
		return null;
	}
	
	private void setCEProperty(ComputeElement c_e,Resource ce_rs, Resource ceClass){
		//resourceType
		String rType=null;
		if(ce_rs.hasProperty(NdlCommons.domainHasResourceTypeProperty))
			rType = ce_rs.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getLocalName().toLowerCase();
		DomainResourceType dType = new DomainResourceType(rType,1);		
		String domainName=null;
		if(ce_rs.hasProperty(NdlCommons.inDomainProperty)){
			domainName = ce_rs.getProperty(NdlCommons.inDomainProperty).getResource().getURI();
			dType.setDomainURL(domainName);
		}
		c_e.setResourceType(dType);

		//group
		String group_url = null,group=null;
		if(ce_rs.hasProperty(NdlCommons.hasRequestGroupURL)){
			group_url = NdlCommons.getRequestGroupURLProperty(ce_rs);
			c_e.setNodeGroupName(group_url);
			int i = group_url.indexOf('#');
			if(i>0){
				group=group_url.substring(i+1);
			}
			c_e.setGroup(group);
		}
		//vm
		// post boot script
		c_e.setPostBootScript(NdlCommons.getPostBootScript(ce_rs));
		c_e.setSplittable(NdlCommons.isSplittable(ce_rs));

		c_e.setSpecificCEType(ce_rs);

		// disk image
		Resource di = NdlCommons.getDiskImage(ce_rs);
		if (di != null) {
			String imageURL = NdlCommons.getIndividualsImageURL(ce_rs);
			String imageHash = NdlCommons.getIndividualsImageHash(ce_rs);
			c_e.setImage(di.getURI());
			c_e.setVMImageURL(imageURL);
			c_e.setVMImageHash(imageHash);
			logger.debug("image:url="+imageURL);
		}else
			logger.warn("No image information:ce="+ce_rs.getURI()+";image="+di);
	}
	
	@Override
	public void ndlCrossConnect(Resource c_rs, OntModel m, long bw, String label,
			List<Resource> interfaces, Resource parent) {
		// ignore request items
		if (requestPhase)
			return;
		
		String name = c_rs.getURI();
		String url = name;
		
		String rType=null;
		if(c_rs.hasProperty(NdlCommons.domainHasResourceTypeProperty))
			rType = c_rs.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getLocalName().toLowerCase();
		DomainResourceType dType = new DomainResourceType(rType,1);		
		String domainName=null;
		if(c_rs.hasProperty(NdlCommons.inDomainProperty)){
			domainName = c_rs.getProperty(NdlCommons.inDomainProperty).getResource().getURI();
			dType.setDomainURL(domainName);
		}
		
		DomainElement link_device = new DomainElement(m,url,name);
		link_device.setResourceType(dType);
		
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			Interface intf = new Interface(m, m.createOntResource(intR.getURI()));
			link_device.addClientInterface(intf);
		}
		float staticLabel=0;
		if(c_rs.hasProperty(NdlCommons.RDFS_Label)){
			staticLabel = c_rs.getProperty(NdlCommons.RDFS_Label).getFloat();
		}
		link_device.setStaticLabel(staticLabel);
		
		deviceList.add(link_device);
	}
	
	@Override
	public void ndlLinkConnection(Resource l, OntModel m,
			List<Resource> interfaces, Resource parent) {
		// ignore request items
		if (requestPhase)
			return;
		
		long bw=0;
		String label=null;
		if(l.hasProperty(NdlCommons.requestHasReservationState)){
			bw = NdlCommons.getResourceBandwidth(l);
			label = NdlCommons.getResourceLabel(l);
			ndlCrossConnect(l,m,bw,label,interfaces,parent);
		}
	}


	@Override
	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		// ignore request items
		if (requestPhase)
			return;

	}

	@Override
	public void ndlInterface(Resource l, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		// ignore request items
		if (requestPhase)
			return;
		
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlParseComplete() {
		if (requestPhase)
			return;
		setDependency();
	}

	@Override
	public void ndlReservation(Resource i, OntModel m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlReservationTermDuration(Resource d, OntModel m, int years,
			int months, int days, int hours, int minutes, int seconds) {
		if (creationTime == null)
			return;
		if ((years == 0) && (months == 0) && (days == 0) && (hours == 0) && (minutes == 0) && (seconds == 0))
			return;
		Calendar cal = Calendar.getInstance();
		cal.setTime(creationTime);
		cal.add(Calendar.YEAR, years);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.DAY_OF_YEAR, days);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.SECOND, seconds);
		expirationTime = cal.getTime();
	}

	@Override
	public void ndlReservationResources(List<Resource> r, OntModel m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		creationTime = start;
	}

	@Override
	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		expirationTime = end;
	}

	@Override
	public void ndlNodeDependencies(Resource ni, OntModel m,
			Set<Resource> dependencies) {
		// TODO Auto-generated method stub

	}

	@Override
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

	@Override
	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlManifest(Resource i, OntModel m) {
		// ignore request items
		if (requestPhase)
			return;
		
		model = m;
	}

	@Override
	public void ndlNetworkConnectionPath(Resource c, OntModel m,
			List<List<Resource>> path, List<Resource> roots) {
		// ignore request items
		if (requestPhase)
			return;
	}

	/**
	 * Various post-parse getters
	 */

	public LinkedList<NetworkElement> getDeviceList() {
		return deviceList;
	}
	
	public Date getCreationTime() {
		return creationTime;
	}
	
	public Date getExpirationTime() {
		return expirationTime;
	}
	
	public List<String> getLogins() {
		List<String> ret = new ArrayList<String>();
		ret.addAll(logins);
		return ret;
	}
	
	public RequestSlice getOFSlice() {
		return currentSlice;
	}
	
	public Map<String, Integer> getStitchPortLabels() {
		return stitchportLabels;
	}
}
