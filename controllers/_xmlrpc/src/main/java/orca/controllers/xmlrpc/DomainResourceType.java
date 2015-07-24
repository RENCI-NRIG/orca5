package orca.controllers.xmlrpc;

import orca.shirako.common.ResourceType;

public class DomainResourceType {
    private ResourceType resourceType;
    private int availableUnits;
    
    public DomainResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }
    
    public DomainResourceType(ResourceType resourceType, int availableUnits) {
        this.resourceType = resourceType;
        this.availableUnits = availableUnits;
    }
    
    public ResourceType getResourceType() {
        return resourceType;
    }
    
    public int getAvailableUnits() {
        return availableUnits;
    }
    
    public void setAvailableUnits(int availableUnits) {
        this.availableUnits = availableUnits;
    }
}