package orca.shirako.common.meta;

import java.util.HashMap;
import java.util.Iterator;

import orca.util.ResourceType;

public class ResourcePoolsDescriptor implements Iterable<ResourcePoolDescriptor>  {
    private HashMap<ResourceType, ResourcePoolDescriptor> pools;
    
    public ResourcePoolsDescriptor() {
        pools = new HashMap<ResourceType, ResourcePoolDescriptor>();
    }
    
    public void add(ResourcePoolDescriptor pool) {
        if (pools.containsKey(pool.getResourceType())) {
            throw new IllegalArgumentException("Resource pool: " + pool.getResourceTypeLabel() + " is already present");
        }
        pools.put(pool.getResourceType(), pool);
    }
    
    public ResourcePoolDescriptor getPool(ResourceType type) {
        return pools.get(type);
    }

    public Iterator<ResourcePoolDescriptor> iterator() {
        return pools.values().iterator();
    }
    
    public int size() {
        return pools.size();
    }
}