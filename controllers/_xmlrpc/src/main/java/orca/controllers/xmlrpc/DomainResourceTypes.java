package orca.controllers.xmlrpc;

import java.util.HashMap;

import orca.shirako.common.ResourceType;

public class DomainResourceTypes {
    private String domain;
    private HashMap<ResourceType, DomainResourceType> map = new HashMap<ResourceType, DomainResourceType>();
    
    public DomainResourceTypes(String domain) {
        this.domain = domain;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public Iterable<DomainResourceType> getResources() {
        return map.values();
    }
    
    public DomainResourceType getResource(ResourceType type) {
        return map.get(type);
    }
    
    public DomainResourceType getDefaultResource() {
        return getResources().iterator().next();
    }
    
    public void addResource(DomainResourceType resource){
        if (map.get(resource.getResourceType()) != null) {
            throw new IllegalStateException("Resource type " + resource.getResourceType() + " is already present");
        }
        map.put(resource.getResourceType(), resource);
    }
}