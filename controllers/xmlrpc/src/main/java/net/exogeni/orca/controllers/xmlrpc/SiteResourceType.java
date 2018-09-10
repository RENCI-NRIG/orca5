package net.exogeni.orca.controllers.xmlrpc;

import net.exogeni.orca.ndl.elements.LabelSet;
import net.exogeni.orca.util.ResourceType;

public class SiteResourceType extends LabelSet {

    private ResourceType resourceType;
    private int availableUnits;

    public SiteResourceType(ResourceType rType) {
        resourceType = rType;
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