package orca.embed.cloudembed;

import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.ModifyElement;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.IRequestEmbedder;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.NdlModel.ModelType;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.IPAddress;
import orca.ndl.elements.IPAddressRange;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.util.persistence.NotPersistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;

public class MappingHandler implements IRequestEmbedder {
	@NotPersistent
	protected OntModel idm;  // only getters and setters here - has to be TDB-backed by the original owner
	
	@NotPersistent
	protected OntModel manifestModel;
	
	@NotPersistent
	protected Logger logger = NdlCommons.getNdlLogger();

	// Recreated in revisit of the owner
	@NotPersistent
	protected HashMap <String, NetworkConnection> requestMap = new HashMap<String, NetworkConnection>(); // bookkeeping of the provisioned connections for releasing
	
	@NotPersistent
	protected boolean debugOn = false;
	
	@NotPersistent
	protected boolean isModify = false;
	
	protected int modifyVersion;
	
	
	public void setDebugOn() {
		debugOn = true;
	}
	
	/**
	 * Create a handler with NO model (idm is not set)
	 */
	public MappingHandler() throws NdlException {
		NdlCommons.setGlobalJenaRedirections();
		idm = NdlModel.createModel(OntModelSpec.OWL_MEM_RDFS_INF, true);
	}
	
	/**
	 * Create handler with in-memory model
	 * @param substrateFile
	 * @throws IOException
	 * @
	 */
	public MappingHandler(String substrateFile) throws IOException, NdlException {
		idm = NdlModel.getModelFromFile(substrateFile, OntModelSpec.OWL_MEM_RDFS_INF, true);
	}
	
	/**
	 * Create handler with TDB-backed model in a directory with specified path prefix. If
	 * substrate file is null, a blank model is created, otherwise it is filled in from
	 * specified file.
	 * @param substrateFile
	 * @param tdbPrefix
	 * @throws IOException
	 * @throws NdlException
	 */
	public MappingHandler(String substrateFile, String tdbPrefix) throws IOException, NdlException {
		if (substrateFile != null)
			idm = NdlModel.getModelFromFile(substrateFile, OntModelSpec.OWL_MEM_RDFS_INF, true, ModelType.TdbPersistent, 
					tdbPrefix + System.getProperty("file.separator") + this.getClass().getSimpleName());
		else
			idm = NdlModel.createModel(OntModelSpec.OWL_MEM_RDFS_INF, true, ModelType.TdbPersistent, tdbPrefix);
	}

	/**
	 * Create a handler based on existing model
	 * @param idm_model
	 */
	public MappingHandler(OntModel idm_model) {
		idm = idm_model;
	}	
	
	/**
	 * Create a blank TDB-backed model or recover existing model from the same folder
	 * @param tdbPrefix
	 * @param recover
	 */
	public MappingHandler(String tdbPrefix, Boolean recover) throws IOException, NdlException {
		if (recover) {
			idm = NdlModel.getModelFromTDB(tdbPrefix, OntModelSpec.OWL_MEM_RDFS_INF);
		} else {
			idm = NdlModel.createModel(OntModelSpec.OWL_MEM_RDFS_INF, true, ModelType.TdbPersistent, tdbPrefix);
		}
	}
	
	public SystemNativeError modifySlice(
			DomainResourcePools domainResourcePools, Collection<ModifyElement> modifyElements, OntModel manifestOnt, 
			String sliceId, HashMap <String,Collection <DomainElement>> nodeGroupMap, 
			HashMap<String, DomainElement> firstGroupElement, OntModel requestModel, OntModel modifyRequestModel) throws UnknownHostException, InetNetworkException {
		return null;
	}
	
	public SystemNativeError runEmbedding(boolean bound, RequestReservation request,
			DomainResourcePools domainResourcePools) {
		return null;
	}

	public SystemNativeError runEmbedding(String domainName,RequestReservation request,
			DomainResourcePools domainResourcePools) {
		return null;
	}
	
	public OntModel createManifest(Collection<NetworkElement> boundElements,
			RequestReservation request, String userDN, String controller_url, String sliceId) {
		return null;
	}
	
	public int findIPRangeHole(HashMap <String,IPAddressRange> group_base_ip, String ip_str) throws InetNetworkException, UnknownHostException{
		int hole=-1;
		if(ip_str==null || group_base_ip==null){
			logger.error("ip_str="+ip_str+";base ip="+group_base_ip);
			return hole;
		}
		InetNetwork ip_str_IP= new InetNetwork(ip_str);
		String network_str= ip_str_IP.getNetwork();
		logger.debug("findIPRangeHole:group base ip="+group_base_ip.get(network_str)
				+";network_str="+network_str);
		hole = findIPRangeHole(group_base_ip.get(network_str));
		logger.info("IP hole = " + hole);
		return hole;
	}
	
	public int findIPRangeHole(IPAddressRange ip_range){
		int hole=-1;
		if(ip_range!=null){
			BitSet bSet = ip_range.getbSet();
			hole = bSet.nextClearBit(0);
			if(hole>=0)
				bSet.set(hole);
		}
		return hole;
	}
	
	public HashMap <String,IPAddressRange>  getIPRange(DomainElement firstElement,Collection <DomainElement> cde) throws Exception{
		//find the last ce in the group with the IP, dependency, etc..
		HashMap <DomainElement,String> group_parent_ip = new HashMap<DomainElement,String>();
		HashMap <String,IPAddressRange> group_base_ip = new HashMap <String,IPAddressRange> ();		
		String ip_str=null;
		OntResource intf_ont=null; 
		DomainElement parent_de=null;
			
		if(cde == null){ 
			logger.error("findIPRange,cde is null!");
			return null;
		}
		Iterator <DomainElement> cde_it = cde.iterator();
		if(firstElement==null)
			firstElement = cde_it.next();

		if(firstElement.getClientInterface()==null){
			logger.warn("findIPRange,firstElement client interface is null!");
			return null;
		}
		for(Interface intf:firstElement.getClientInterface()){
			IPAddress ip = (IPAddress) intf.getLabel();
			
			if(ip==null)
				logger.warn("findIPRange,ip="+ip+";intf="+intf.getURI());
			else{
				logger.debug("findIPRange,ip="+ip+";intf="+intf.getURI());
				IPAddress base_IPAddr = ip.base_IP;	
				IPAddressRange ip_range = new IPAddressRange(base_IPAddr.address, base_IPAddr.netmask, ip.getResource(intf.getModel()));
				InetNetwork base_IP= ip_range.getBase_IP();
				group_base_ip.put(base_IP.getNetwork(),ip_range);
			}
		}
		if(firstElement.getPrecededBy()!=null){
			for(Entry <DomainElement,OntResource> parent:firstElement.getPrecededBySet()){
				parent_de = parent.getKey();
				intf_ont = parent.getValue();
				logger.debug("findIPRange,parent="+parent_de.getURI()+";intf="+intf_ont.getURI());
				if(intf_ont.getProperty(NdlCommons.layerLabelIdProperty)!=null){
					ip_str=intf_ont.getProperty(NdlCommons.layerLabelIdProperty).getString();
					InetNetwork ip_str_IP= new InetNetwork(ip_str);
					group_parent_ip.put(parent_de,ip_str_IP.getNetwork());
				}
			}
		}

		for(Entry <DomainElement,String> p_entry: group_parent_ip.entrySet()){
			logger.debug("findIPRange:group_parent_ip="+p_entry.getValue());
			IPAddressRange ip_range = group_base_ip.get(p_entry.getValue());
			if(ip_range==null){
				logger.error("Modifying: No IP address range:" + p_entry.getKey().getName());
				continue;
			}
			BitSet bSet = ip_range.getbSet();
			BigInteger bi_base_IP = ip_range.getBi_base_IP();
			if (bSet!=null) {
				if(p_entry.getKey().getFollowedBy()!=null){
					for(Entry <DomainElement,OntResource> c_entry:p_entry.getKey().getFollowedBySet()){
						intf_ont = c_entry.getValue();
						logger.debug("findIPRange:follower intf="+intf_ont.getURI()+";ip="+intf_ont.getProperty(NdlCommons.layerLabelIdProperty));
						if(intf_ont.getProperty(NdlCommons.layerLabelIdProperty)!=null){
							ip_str=intf_ont.getProperty(NdlCommons.layerLabelIdProperty).getString();
							int index = ip_str.indexOf("/");
							ip_str=index>0?ip_str.split("\\/")[0]:ip_str;
							ip_range.modify(ip_str,null,null);
						}
					}
				}
			} else {
				logger.error("Modifying: No IP address range bitset:" + p_entry.getKey().getName());
			}
		}
		return group_base_ip;
	}
	
	public NetworkElement existingDevice(NetworkElement device, LinkedList<NetworkElement>	deviceList){
		if ((device == null) || (deviceList==null))
			return null;
		for (NetworkElement ne: deviceList){
			if (device.getName().equals(ne.getName()) ){
				return ne;
			}
		}
		return null;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public NetworkConnection getConnection(String uri) {
		return requestMap.get(uri);
	}
	
	public void setConnection(String uri, NetworkConnection nc) {
		requestMap.put(uri, nc);
	}

	public OntModel getIdm() {
		return idm;
	}
	
	public OntModel getManifestModel() {
		return manifestModel;
	}

	public void setManifestModel(OntModel manifestModel) {
		this.manifestModel = manifestModel;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("requestMap: \n");
		for(Entry<String, NetworkConnection> e: requestMap.entrySet()) {
			sb.append(e.getKey() + ": " + e.getValue() + "\n");
		}
		return sb.toString();
	}

	public boolean isModify() {
		return isModify;
	}

	public void setModify(boolean isModify) {
		this.isModify = isModify;
	}

}
