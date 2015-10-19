package orca.ndl;

import java.io.File;

import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;

import com.hp.hpl.jena.rdf.model.Resource;

public class DomainResourceType implements LayerConstant, Comparable<DomainResourceType>, Persistable {
	// Define more resource types here as needed
	public static String BM_RESOURCE_TYPE="baremetalce";
	public static String FourtyGBM_RESOURCE_TYPE="fourtygbaremetalce";
	public static String VM_RESOURCE_TYPE="vm";
	public static String VLAN_RESOURCE_TYPE="vlan";
	public static String LUN_RESOURCE_TYPE="lun";	
	
	@Persistent
	protected String domainURL;
	@Persistent
    protected String resourceType;
	@Persistent
    protected Integer count;
	@Persistent
    protected Integer rank;
	
    public DomainResourceType(){
    	count=0;
    }
    
	public DomainResourceType(String resourceType, int count) {
        this.resourceType = resourceType;
        this.count = count;
    }
	
	public Resource getTypeResource(){
		Resource type_rs=null;
		if(this.resourceType==null)
			return null;
		if(this.resourceType.toLowerCase().endsWith(DomainResourceType.VM_RESOURCE_TYPE))
			return NdlCommons.vmResourceTypeClass;	
		if(this.resourceType.toLowerCase().endsWith(DomainResourceType.BM_RESOURCE_TYPE))
			return NdlCommons.bmResourceTypeClass;
		if(this.resourceType.toLowerCase().endsWith(DomainResourceType.FourtyGBM_RESOURCE_TYPE))
			return NdlCommons.fourtygbmResourceTypeClass;
		if(this.resourceType.toLowerCase().endsWith(DomainResourceType.LUN_RESOURCE_TYPE))
			return NdlCommons.lunResourceTypeClass;
		if(this.resourceType.toLowerCase().endsWith(DomainResourceType.VLAN_RESOURCE_TYPE))
			return NdlCommons.vlanResourceTypeClass;
		return type_rs;
		
	}
	
	public void print(){
		System.out.println(toString());
	}
	
	/**
	 * Create a name of resource.domain attribute based on RDF file name
	 * and resource type name. 
	 * @param rdf file name
	 * @param typeName
	 * @return domain/type, e.g. rencivmsite/vm, nlr/vlan
	 */
	public static String generateDomainName(String rdf, String typeName) {
		if (rdf == null)
			return "undefined";
		String fName = new File(rdf).getName();
		String domName = fName.split("[.]")[0];
		domName=domName.split("\\-")[0];
	
		if (domName.length() == 0)
			domName = "undefined";
		
		String[] rType = typeName.split("[.]");
		
		typeName = rType[rType.length-1];
		
		//if (typeName.toLowerCase().equals(VM_RESOURCE_TYPE))
		//	return domName;
		return domName + "/" + typeName;
	}
	
	//reverve above function
	//@param: domain[/type]
	// @return: domain url, e.g. http://geni-orca.renci.org/rencivmsite.rdf/Domain[/vm], that would appear in the abstract domain model.
	public static String generateDomainResourceName(String domainName) {
		if (domainName == null)
			return "undefined";
		
		String [] value12=domainName.split("\\/");
		String value1=null,value2=null;
		value1=value12[0];
		String domainNameURL=NdlCommons.ORCA_NS+value1+".rdf#";
		
		domainNameURL=domainNameURL+value1+"/Domain";
		if(value12.length==2){
			value2=value12[1];
			domainNameURL=domainNameURL+"/"+value2;
		}
		//System.out.println("Value1:"+value1+":"+value2+":"+domainNameURL+"\n");
		return domainNameURL;
	}

	@Override
	public String toString(){
		return resourceType+":"+count+":"+rank+":"+domainURL;
	}
    
	//for sort in a List, by Collections::sort()
	public int compareTo(DomainResourceType ne) {
		int compare=0;
		
		if (ne==null) return 1;
	
		if(this.getCount()<ne.getCount()) compare=1;
		else if(this.getCount()>ne.getCount()) compare=-1;
		
		return compare;
	}
	
    public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public void setCount(int count) {
		this.count = count;
	}

    public String getDomainURL() {
		return domainURL;
	}

	public void setDomainURL(String domainURL) {
		this.domainURL = domainURL;
	}

	public String getResourceType() {
        return resourceType;
    }
    
	public String getResourceTypeURL(){
		if(resourceType.endsWith("vm"))
			return NdlCommons.vmResourceTypeClass.getURI();
		if(resourceType.endsWith("vlan"))
			return NdlCommons.vlanResourceTypeClass.getURI();
		return null;
	}
	
    public int getCount() {
        return count;
    }
    public void add(int units) {
        count+=units;
    }
    
	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
	
}
