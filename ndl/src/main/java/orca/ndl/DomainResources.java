package orca.ndl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class DomainResources {
    protected HashMap<String, DomainResourceType> resType;
    protected HashMap<String, DomainResource> constraintMap; // interfaces, bandwidth

    /*
     * public DomainResources(String resourceType, int count) { map = new HashMap<String, DomainResource>();
     * this.resourceType = resourceType; this.count = count; }
     */

    public DomainResources() {
        resType = new HashMap<String, DomainResourceType>();
        constraintMap = new HashMap<String, DomainResource>();
    }

    public List<DomainResourceType> getResourceType() {
        ArrayList<DomainResourceType> t = new ArrayList<DomainResourceType>(resType.values().size());
        for (DomainResourceType r : resType.values()) {
            t.add(r);
        }
        return t;
    }

    public DomainResourceType getResourceType(String type) {
        return resType.get(type);
    }

    public void addResourceType(DomainResourceType type) {
        resType.put(type.getResourceType(), type);
    }

    public void addResourceType(String type, int units) {
        resType.get(type).add(units);
    }

    public boolean hasType(String type) {

        return resType.get(type) == null ? false : true;

    }

    public List<DomainResource> getResources() {
        ArrayList<DomainResource> l = new ArrayList<DomainResource>(constraintMap.values().size());
        for (DomainResource r : constraintMap.values()) {
            l.add(r);
        }
        return l;
    }

    public DomainResource getResource(String iface) {
        return constraintMap.get(iface);
    }

    public void addResource(DomainResource resource) {
        constraintMap.put(resource.getInterface(), resource);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DomainResources resType: \n");
        for (Entry<String, DomainResourceType> e : resType.entrySet()) {
            sb.append(e.getKey() + ": " + e.getValue() + "\n");
        }
        sb.append("DomainResources constriaintMap:\n");
        for (Entry<String, DomainResource> e : constraintMap.entrySet()) {
            sb.append(e.getKey() + ": " + e.getValue() + "\n");
        }
        return sb.toString();
    }
}