package orca.controllers.xmlrpc;

import java.util.HashMap;

import orca.util.ResourceType;

public class SiteResourceTypes {
    private String domain;
    private HashMap<ResourceType, SiteResourceType> map = new HashMap<ResourceType, SiteResourceType>();
    
    public SiteResourceTypes(String domain) {
        this.domain = domain;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public Iterable<SiteResourceType> getResources() {
        return map.values();
    }
    
    public SiteResourceType getResource(ResourceType type) {
        return map.get(type);
    }
    
    public SiteResourceType getDefaultResource() {
        return getResources().iterator().next();
    }
    
    public void addResource(SiteResourceType resource){
        if (map.get(resource.getResourceType()) != null) {
            throw new IllegalStateException("Resource type " + resource.getResourceType() + " is already present");
        }
        map.put(resource.getResourceType(), resource);
    }
}