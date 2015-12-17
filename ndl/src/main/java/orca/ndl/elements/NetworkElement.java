package orca.ndl.elements;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import orca.ndl.DomainResource;
import orca.ndl.DomainResourceType;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.PersistenceUtils;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class NetworkElement implements LayerConstant, Comparable, Persistable {

	@NotPersistent 
	protected OntModel model; // TDB
	
	@NotPersistent 
	protected boolean isModify;
	
	@Persistent
	protected int modifyVersion;
	
	@Persistent
	protected String uri;
	@Persistent
	protected String name;
	@Persistent
	protected String atLayer;
	
	@Persistent // DONE
	protected DomainResourceType resourceType;
	
	@Persistent // used for modifying, exisitng #interface
	protected int numInterface;
	
	@Persistent // recursive
	protected LinkedList<Interface> clientInterface;
	
	@Persistent // recursive
	protected HashMap<String, DomainResource> map;  //interfaces, bandwidth
	
	@Persistent
	protected String castType;
	
	@Persistent
	protected String GUID;
	
	@Persistent
	protected Integer sn;
	@Persistent
	protected String hostName;
	@Persistent
	protected String managementIP;

	@NotPersistent
	protected Logger logger=NdlCommons.getNdlLogger();

	public NetworkElement(OntModel m, OntResource rs){
		this.model=m;
		this.uri=rs.getURI();
		this.name=this.uri;
	}
	
	public NetworkElement(OntModel m,Resource rs){
		this.model=m;
		this.uri=m.getOntResource(rs).getURI();
		this.name=this.uri;
	}
	
	public NetworkElement(OntModel m, String u, String n) {
		model=m;
		uri=u;
		name=n;
	}
	
	public NetworkElement(){
		
	}
	
	/**
	 * Comparison of networkelements limited to comparing URIs and names
	 *
	public boolean equals(Object o) {
		if ((o instanceof NetworkElement) && (uri != null)) {
			NetworkElement neNew = (NetworkElement)o;
			if (name != null)
				return (uri.equals(neNew.uri) && name.equals(neNew.name));
			else
				return uri.equals(neNew.uri);
		}
		return false;
	}*/
	
	public int compareTo(Object o) {
		int compare=0;

		if(o == null) return 1;
		
		NetworkElement ne = (NetworkElement) o;
	
		if(this.getRank()<ne.getRank()) compare=-1;
		else if(this.getRank()>ne.getRank()) compare=1;
		else if(this.getRank()==ne.getRank()){
			if(ne.getType()!=null)
				if(ne.getType().equals("Server")) compare=-1;
			if(this.getType()!=null)
				if(this.getType().equals("Server")) compare=1;	
		}
		return compare;
	}
	
	public static class LayerComparator implements Comparator {
		public int compare(Object s,Object o){
	
			NetworkElement se=(NetworkElement) s;
			NetworkElement ne=(NetworkElement) o; 
			
			int compare=0;
			if(se==null) return -1;
			if(ne==null) return 1;
			
			if(se.getRank()<ne.getRank()) compare=-1;
			else if(se.getRank()>ne.getRank()) compare=1;
			else if(se.getRank()==ne.getRank()){
				if(ne.getType()!=null)
					if(ne.getType().equals("Server")) compare=-1;
				if(se.getType()!=null)
					if(se.getType().equals("Server")) compare=1;	
			}
			return compare;
		}
	}
	
	public String getCastType() {
		return castType;
	}

	public void setCastType(String castType) {
		this.castType = castType;
	}

	public Interface getDefaultClientInterface() {	
		if(clientInterface==null)
			return null;
		return clientInterface.getFirst();
	}
	
	public LinkedList <Interface> getClientInterface() {
		return clientInterface;
	}
	
	public Interface getClientInterfaceByURI(String url){
		if(url==null)
			return null;
		if(clientInterface==null)
			return null;
		Interface inter = null;
		for(Interface intf:clientInterface){
			if(url.equalsIgnoreCase(intf.getURI())){
				inter = intf;
				break;
			}	
		}
		return inter;
	}
	
	public void setClientInterface(LinkedList <Interface> clientInterface) {
		this.clientInterface = clientInterface;
	}
	
	public void addClientInterface(Interface intf){
		if(intf==null)
			return;
		if(clientInterface==null) 
			clientInterface=new LinkedList <Interface> ();
		if(!clientInterface.contains(intf))
			clientInterface.add(intf);
	}
	
	public DomainResourceType getResourceType() {
		return resourceType;
	}
	public void setResourceType(DomainResourceType resourceType) {
		this.resourceType = resourceType;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public void print(){
		logger.debug("URL:"+this.uri);
		logger.debug("Name:"+this.getName());
		logger.debug("At Layer:"+atLayer);
		logger.debug("#sn:"+sn);
	}

	public void print(Logger logger){
		logger.info("URL:"+this.uri);
		logger.info("Name:"+this.getName());
		logger.info("At Layer:"+atLayer);
		logger.info("#sn:"+sn);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Type: " + this.getClass().getSimpleName() + "\n");
		sb.append("URL: " + uri + " ");
		sb.append("Name: " + getName() + " ");
		sb.append("At Layer: " + atLayer + " ");
		sb.append("#sn:" + sn + " ");
		sb.append("ResourceType: " + resourceType);
		sb.append("ClientInterface: ");
		if (clientInterface != null) {
			for(Interface ii: clientInterface) {
				sb.append(ii);
			}
		}
		sb.append("\n");
		sb.append("Interface Bandwidth: ");
		if (map != null) {
			for(Entry<String, DomainResource> ee: map.entrySet()) {
				sb.append("[ " + ee.getKey() + " <> " + ee.getValue() + " ] ");
			}
		}
		sb.append("\n");
		return sb.toString();
	}
	
	public int getNumUnits() {
		return resourceType.getCount();
	}

	public void setNumUnits(int numUnits) {
		this.resourceType.setCount(numUnits);
	}

	public String getInDomain() {
		return resourceType.getDomainURL();
	}

	public void setInDomain(String inDomain) {
		this.resourceType.setDomainURL(inDomain);
	}

	public int getSn() {
		return sn;
	}

	public void setSn(int sn) {
		this.sn = sn;
	}

	public int getRank() {
		return resourceType.getRank();
	}

	public void setRank(int rank) {
		this.resourceType.setRank(rank);
	}
	
	public OntResource getResource() {
		if (uri != null && !model.isClosed())
			return model.getOntResource(uri);
		return null;
	}
	
	/**
	 * If the model of the element was not set in 
	 * the constructor, this method will set it
	 * based on the model of the resource that is passed in
	 * @param r
	 */
	public void setResource(OntResource r) {
		if (r != null) {
			if (model == null)
				model = r.getOntModel();
			uri = r.getURI();
		}
		else
			uri = null;
	}
	
	public String getURI(){
		return uri;
	}
	
	public OntModel getModel(){
		return model;
	}
	
	public String getName(){
		return name;
	}
	
	public void setURI(String uri) {
		this.uri = uri;
	}

	public void setModel(OntModel model) {
		this.model = model;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAtLayer() {
		return atLayer;
	}

	public void setAtLayer(String atLayer) {
		this.atLayer = atLayer;
	}
	public String getType() {
		if(resourceType==null) return null;
		return resourceType.getResourceType();
	}

	public void setType(String type) {
		if(resourceType==null)
			resourceType = new DomainResourceType();
		this.resourceType.setResourceType(type);
	}
	
	public void setHostName(String n){
		hostName=n;
	}
	public void setManagementIP(String ip){
		managementIP=ip;
	}
	
	//constraints
    public List<DomainResource> getResources() {
    	if(map==null)
    		return null;
        ArrayList<DomainResource> l = new ArrayList<DomainResource>(map.values().size());
        for (DomainResource r : map.values()) {
            l.add(r);
        }
        return l;
    }
    
    public HashMap<String, DomainResource> getResourcesMap(){
    	return map;
    }

    public void setResourcesMap(HashMap<String, DomainResource> m){
        map=m;
    }
    
    public DomainResource getResource(String iface) {
    	if(map==null)
    		return null;
    	return map.get(iface);
    }
    
    public void addResource(DomainResource resource) {
    	if(map==null)
    		map = new HashMap<String, DomainResource>();
        map.put(resource.getInterface(), resource);
    }

	public boolean isModify() {
		return isModify;
	}

	public void setModify(boolean isModify) {
		this.isModify = isModify;
	}

	public int getModifyVersion() {
		return modifyVersion;
	}

	public void setModifyVersion(int modifyVersion) {
		this.modifyVersion = modifyVersion;
	}
	
	public int getNumInterface() {
		return numInterface;
	}

	public void setNumInterface(int numInterface) {
		this.numInterface = numInterface;
	}

	public String getGUID() {
		return GUID;
	}

	public void setGUID(String gUID) {
		GUID = gUID;
	}
}


