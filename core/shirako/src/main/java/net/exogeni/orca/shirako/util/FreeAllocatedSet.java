package net.exogeni.orca.shirako.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * <code>FreeAllocatedSet</code> is a simple data structure that maintains two sets: free and allocated.
 * The structure can be used to track free and allocated items. Use {@link #addInventory(Object)} to
 * add inventory items, {@link #allocate()} to allocate an item, and {@link #free(Object)} to free an item.
 * @author aydan
 *
 * @param <T> type of unit
 */
public class FreeAllocatedSet<T>
{
    /**
     * Free set.
     */
    protected HashSet<T> free;
    /**
     * Allocated set.
     */
    protected HashSet<T> allocated;
    
    /**
     * Creates a net set.
     */
    public FreeAllocatedSet()
    {
        free = new HashSet<T>();
        allocated = new HashSet<T>();
    }
    
    /**
     * Creates a new set of the specified capacity.
     * @param capacity initial capacity of the set
     */
    public FreeAllocatedSet(int capacity)
    {
        if (capacity <= 0){
            throw new IllegalArgumentException("Invalid capacity");
        }
        free = new HashSet<T>(capacity);
        allocated = new HashSet<T>(capacity);
    }
    
    /**
     * Adds an inventory item to the set.
     * @param item item to add
     */
    public void addInventory(T item)
    {
        if (item == null){ 
            throw new IllegalArgumentException("item cannot be null"); 
        }
        if (allocated.contains(item)){
            throw new IllegalStateException("item is already in allocated");
        }
        free.add(item);
    }
    
    /**
     * Allocates an item from the set.
     * @return an allocated item, or null if the set does not have a free item
     */
    public T allocate()
    {
        T item = null;
        if (free.size() > 0){
            item = free.iterator().next();
            free.remove(item);
            allocated.add(item);
        }
        return item;
    }
    
    /**
     * Allocates an known item from the set.
     * @param tag tag
     * @param configTag configTag
     * @return an allocated item, or null if the set does not have a free item
     */
    public T allocate(T tag,boolean configTag)
    {
        T item = null;
        if (free.size() > 0){
            item = free.iterator().next();
            if(configTag){
            	item=tag;
            	if(!free.contains(item)){
            		 throw new IllegalStateException("item is already in allocated:"+item.toString());
            	}
            }
            free.remove(item);
            allocated.add(item);
        }
        return item;
    }
    
    
    /**
     * Allocates the specified number of units.
     * @param count number of units to allocate.
     * @return list of units
     */
    public List<T> allocate(int count)
    {
        List<T> l = new ArrayList<T>(count);
        for (int i = 0; i < count; i++){
            T item = allocate();
            if (item != null) {
                l.add(item);
            }
        }
        return l;
    }
    
    /**
     * Frees the specified item.
     * @param item item to free
     */
    public void free(T item)
    {
        if (item == null){
            throw new IllegalArgumentException("item cannot be null");
        }
        if (!allocated.contains(item)){
            throw new IllegalStateException("item has not been allocated");
        }
        if (free.contains(item)){
            throw new IllegalStateException("item has already been freed");
        }
        allocated.remove(item);
        free.add(item);
    }
    
    /**
     * Frees an item.
     */
    public void free()
    {
        if (allocated.size() == 0){
            throw new IllegalStateException("no items have been allocated");
        }
        T item = allocated.iterator().next();
        allocated.remove(item);
        free.add(item);
    }
    
    /**
     * Frees the specified number of units.
     * @param count number of units to free
     */
    public void free(int count)
    {
        for (int i = 0; i < count; i++){
            free();
        }
    }
    
    /**
     * Frees the specified items.
     * @param items items
     */
    public void free(List<T> items) {
        for(T t : items) {
            free(t);
        }
    }
    
    /**
     * Returns the number of free items.
     * @return number of free items
     */
    public int getFree()
    {
        return free.size();
    }
    
    /**
     * Returns the number of allocated items.
     * @return number of allocated items
     */
    public int getAllocated()
    {
        return allocated.size();
    }
    
    /**
     * Returns the number of items represented by this set.
     * @return number of item in the set (free + allocated)
     */
    public int size()
    {
        return free.size() + allocated.size();
    }
    
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Free: " + free + "\n");
    	sb.append("Allocated: " + allocated);
    	
    	return sb.toString();
    }
    
}
