package orca.ndl.elements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import orca.ndl.NdlCommons;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class ComputeElement extends NetworkElement {
	@Persistent
	protected String image = null;
	@Persistent
	protected String VMImageURL;
	@Persistent
	protected String VMImageHash;
	@Persistent
	protected String postBootScript;
	@Persistent
	protected String group;
	@Persistent
	protected String nodeGroupName;
	
	//for storage element
	@Persistent
	protected String CHAP_User=null,CHAP_Password=null;
	@Persistent
	protected String FSParam;
	@Persistent
	protected String FSType;
	@Persistent
	protected String MntPoint;
	@Persistent
	protected boolean doFormat;
	
	@Persistent
	protected String specificCEType;
	
	@Persistent
	protected String specificCETypeurl;
	
	@Persistent
	protected boolean isSplittable;
	
	@NotPersistent
	protected LinkedList <NetworkElement> ceGroup;
	
	@NotPersistent
	protected Set<NetworkElement> dependencies = new HashSet<NetworkElement>();
	
	@NotPersistent
	protected HashMap <NetworkConnection, Interface> interfaces;
	
	public ComputeElement(OntModel m,OntResource rs){
		super(m,rs);
	}
	public ComputeElement(OntModel m,Resource rs){
		super(m,rs);
	}
	
	public ComputeElement(OntModel m, String url, String name) {
		super(m,url,name);
	}
	
	public ComputeElement copy(OntModel m, OntModel req_m,String url, String name){
		ComputeElement ce = new ComputeElement(m,url,name);
		ce.setImageInfo(this.getImage(), this.getVMImageURL(),this.getVMImageHash());
		ce.setResourceType(this.getResourceType());
		ce.setDependencies(this.getDependencies());
		ce.setGroup(this.group);
		String group_url = this.getNodeGroupName();
		String script=null;
		if(group_url!=null){
			OntResource ng_rs = req_m.getOntResource(group_url);
			if(ng_rs!=null){
				if(ng_rs.hasProperty(NdlCommons.requestPostBootScriptProperty)){
					script = ng_rs.getProperty(NdlCommons.requestPostBootScriptProperty).getString();
				}
			}
		}
		if(script!=null)
			ce.setPostBootScript(script);
		
		ce.setSpecificCEType(this.getSpecificCEType());
		ce.setNodeGroupName(this.getNodeGroupName());		

		ce.setFSType(this.getFSType());
		ce.setFSParam(this.getFSParam());
		ce.setMntPoint(this.getMntPoint());
		ce.setDoFormat(this.isDoFormat());
		
		return ce;
	}
	
	public IPAddress getDefaultClientInterfaceIP(){
		IPAddress ip=null;
		if(this.getClientInterface()==null)
			return null;
        for(Interface intf:this.getClientInterface()){
         	if(intf.getLabel() != null){
         		ip = (IPAddress) intf.getLabel();
             	break;
         	}
         }
        return ip;
	}
	
	public String getDefaultClientInterfaceIPAddress(){
		String ipaddr=null;
        IPAddress ip = getDefaultClientInterfaceIP();
        if(ip!=null)
        	ipaddr = ip.address;
        return ipaddr;
	}

	public OntResource getDefaultClientInterfaceIPAddressRS(){
		OntResource ip_ont=null;
        IPAddress ip = getDefaultClientInterfaceIP();
        if(ip!=null && !model.isClosed())
        	// NOTE: I'm assuming that 'model' is the right place to look for the label resource /ib
        	ip_ont = ip.getResource(model);
        return ip_ont;
	}
	
	public HashMap<NetworkConnection, Interface> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(HashMap<NetworkConnection, Interface> interfaces) {
		this.interfaces = interfaces;
	}

	public void setInterfaceName(NetworkConnection l, Interface ifName) {
		if ((l == null) || (ifName == null))
			return;
		if(interfaces==null) 
			interfaces = new HashMap <NetworkConnection, Interface> ();
		interfaces.put(l, ifName);
	}
	
	public NetworkConnection getConnectionByInterfaceName(Interface intf) {
		if ((intf != null) && (interfaces!=null)){
			for(Entry <NetworkConnection,Interface> entry: interfaces.entrySet()){
				if(entry.getValue() == intf){
					return entry.getKey();
				}
			}
		}
		return null;
	}
	
	public void removeConnectionByInterfaceName(Interface intf) {
		NetworkConnection nc=null;
		if ((intf != null) && (interfaces!=null)){
			for(Entry <NetworkConnection,Interface> entry: interfaces.entrySet()){
				if(entry.getValue() == intf){
					nc=entry.getKey();
					break;
				}
			}
		}
		if(nc!=null)
			interfaces.remove(nc);
	}
	
	public NetworkConnection getConnectionByInterfaceURI(String intf) {
		if ((intf != null) && (interfaces!=null)){
			for(Entry <NetworkConnection,Interface> entry: interfaces.entrySet()){
				if(entry.getValue().getURI().equals(intf)){
					return entry.getKey();
				}
			}
		}
		return null;
	}
	
	public Interface getInterfaceName(NetworkConnection l) {
		if ( (l != null) && (interfaces!=null))
			return interfaces.get(l);
		return null;
	}
	
	public Interface getInterfaceByName(String ln) {
		if (ln == null)
			return null;
		for(Entry<NetworkConnection, Interface> entry:interfaces.entrySet()){
			if(ln.equals(entry.getKey().getName())){
				return entry.getValue();
			}
		}
		return null;
	}
	
	public boolean existConnectionInterface(){
		if(interfaces==null)
			return false;
		if(interfaces.isEmpty())
			return false;
		return true;
	}
	
	public void addDependency(NetworkElement n) {
		if (n != null)
			dependencies.add(n);
	}
	
	public void removeDependency(NetworkElement n) {
		if (n != null)
			dependencies.remove(n);
	}
	
	public void clearDependencies() {
		dependencies = new HashSet<NetworkElement>();
	}
	
	public Set<NetworkElement> getDependencies() {
		return dependencies;
	}
	
	public boolean isDependency(NetworkElement n) {
		if (n == null)
			return false;
		return dependencies.contains(n);
	}
	
	public String getSpecificCEType() {
		return specificCEType;
	}
	
	public String getSpecificCETypeurl() {
		return specificCETypeurl;
	}

	public void setSpecificCEType(String specificCEType) {
		this.specificCEType = specificCEType;
	}
	
	public void setSpecificCEType(Resource rs) {
		Resource ceType_rs=NdlCommons.getSpecificCE(rs);
		if(ceType_rs!=null)
			this.specificCETypeurl=ceType_rs.getURI();
		String specificCEType=NdlCommons.getEC2VMSize(rs);
		this.specificCEType = specificCEType;
	}

	public boolean isSplittable() {
		return isSplittable;
	}

	public void setSplittable(boolean isSplittable) {
		this.isSplittable = isSplittable;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getVMImageURL() {
		return VMImageURL;
	}

	public void setVMImageURL(String vMImageURL) {
		VMImageURL = vMImageURL;
	}

	public String getVMImageHash() {
		return VMImageHash;
	}

	public void setVMImageHash(String aHash) {
		VMImageHash = aHash;
	}

	public void setImageInfo(String image, String  vMImageURL, String aHash){
		this.image = image;
		VMImageURL = vMImageURL;
		VMImageHash = aHash;
	}
	
	public String getPostBootScript() {
		return postBootScript;
	}

	public void setPostBootScript(String postBootScript) {
		this.postBootScript = postBootScript;
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getNodeGroupName() {
		return nodeGroupName;
	}
	public void setNodeGroupName(String nodeGroupName) {
		this.nodeGroupName = nodeGroupName;
	}
	public void setDependencies(Set<NetworkElement> dependencies) {
		this.dependencies = dependencies;
	}
	public String getFSParam() {
		return FSParam;
	}
	public void setFSParam(String fSParam) {
		FSParam = fSParam;
	}
	public String getFSType() {
		return FSType;
	}
	public void setFSType(String fSType) {
		FSType = fSType;
	}
	public String getMntPoint() {
		return MntPoint;
	}
	public void setMntPoint(String mntPoint) {
		MntPoint = mntPoint;
	}
	public boolean isDoFormat() {
		return doFormat;
	}
	public void setDoFormat(boolean doFormat) {
		this.doFormat = doFormat;
	}
	public String getCHAP_User() {
		return CHAP_User;
	}
	public void setCHAP_User(String cHAP_User) {
		CHAP_User = cHAP_User;
	}
	public String getCHAP_Password() {
		return CHAP_Password;
	}
	public void setCHAP_Password(String cHAP_Password) {
		CHAP_Password = cHAP_Password;
	}
	
	public LinkedList<NetworkElement> getCeGroup() {
		return ceGroup;
	}
	
	public void setCeGroup(LinkedList<NetworkElement> cg) {
		this.ceGroup=cg;
	}
	
	public void setCeGroup(NetworkElement ce) {
		if(this.ceGroup==null)
			ceGroup = new LinkedList <NetworkElement>();
		this.ceGroup.add(ce);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		sb.append("CE Type: " + specificCEType + "/" + isSplittable + "\n");
		sb.append("Image: " + image + "/" + VMImageURL + "/" + VMImageHash + "\n");
		sb.append("Group: " + group + "\n");
		sb.append("nodeGroupName: " + nodeGroupName + "\n");
		sb.append("iSCSI: " + CHAP_User + "/" + CHAP_Password + "/" + FSParam + "/" + MntPoint + "/" + doFormat + "\n");
		
		return sb.toString();
	}
}
