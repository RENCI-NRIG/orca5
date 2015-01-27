package orca.embed.policyhelpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import orca.ndl.DomainResourceType;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.shirako.common.meta.ResourceProperties;
import orca.util.ResourceType;

import org.apache.log4j.Logger;

public class DomainResourcePools {
	protected HashMap <String,DomainResourceType> domainResourcePools; //List of <domain, rType<type,unit>> 
	protected Map <String, LinkedList<DomainResourceType>> domainResourceTypeList; 
	protected HashMap <String,Integer> typeTable; //accumulated units of each resourceType over all sites.
    public Logger logger;
    
    public DomainResourcePools(){
    	logger = Logger.getLogger(this.getClass());
    }
    
	//Get the domain resource pools from SM<-broker.
	public void getDomainResourcePools(ResourcePoolsDescriptor pools){
		typeTable=new HashMap <String,Integer>();
		domainResourceTypeList = new HashMap <String, LinkedList<DomainResourceType>> ();
		String pureType=null,domainName=null;
		int count=0;
		domainResourcePools = new HashMap <String,DomainResourceType> ();
		for (ResourcePoolDescriptor rpd : pools) {
            ResourceType type = rpd.getResourceType();
            DomainResourceType dType = new DomainResourceType(); 
            dType.setResourceType(type.getType());
            dType.setCount(rpd.getAttribute(ResourceProperties.ResourceAvailableUnits).getIntValue());
            ResourcePoolAttributeDescriptor a = rpd.getAttribute(ResourceProperties.ResourceDomain);
            
            if (a == null) {
                throw new RuntimeException("Missing domain information for resource pool:  " + type);
            }
            domainName=DomainResourceType.generateDomainResourceName(a.getValue());
            dType.setDomainURL(domainName);
            logger.debug("Domain Resource Pool: " + a.getValue() + " with " + dType.toString()+"-:"+domainName+":;domainURL="+dType.getDomainURL());
            domainResourcePools.put(domainName, dType);
           
            a = rpd.getAttribute(ResourceProperties.ResourceNdlAbstractDomain);
            if (a == null) {
                logger.debug("Found no abstract model for resource pool: " + type);
            }
            
            pureType=type.getType().split("\\.")[1];
            if(typeTable.containsKey(pureType)){
            	count=typeTable.get(pureType);
            	count=count+dType.getCount();
            	typeTable.remove(pureType);
            	typeTable.put(pureType, count);
            	addDomainResourceType(pureType, dType);
            }
            else{
            	typeTable.put(pureType, dType.getCount());
            	LinkedList <DomainResourceType> domainList = new LinkedList <DomainResourceType> ();
            	domainList.add(dType);
            	domainResourceTypeList.put(pureType, domainList);
            }
            sortDomainResourceTypeList(domainResourceTypeList);
            logger.debug("Pure Type:"+pureType+":"+typeTable.get(pureType));
        }
	}
	
	public void addDomainResourceType(String type, DomainResourceType dType){
		LinkedList <DomainResourceType> domainList = domainResourceTypeList.get(type);
		domainList.add(dType);
	}
	
	public void sortDomainResourceTypeList(Map <String, LinkedList<DomainResourceType>> domainResourceTypeList){
		for(Entry<String, LinkedList<DomainResourceType>> entry:domainResourceTypeList.entrySet()){
			LinkedList <DomainResourceType> domainList = entry.getValue();
			Collections.sort(domainList);
		}
	}
	//domainType: domainname.type
	public DomainResourceType getDomainResourceType(String domainName){
		DomainResourceType dType=this.domainResourcePools.get(domainName);

		return dType;
	}
	
    public HashMap<String, DomainResourceType> getDomainResourcePools() {
		return domainResourcePools;
	}

	public HashMap<String, Integer> getTypeTable() {
		return typeTable;
	}

	public int getTypeUnits(String type) {
		return typeTable.get(type);
	}
	
	public LinkedList <DomainResourceType> getDomainResourceTypeList(String type){
		return domainResourceTypeList.get(type);
	}
}
