package net.exogeni.orca.policy.core.util;

import java.util.HashSet;

import net.exogeni.orca.shirako.core.Unit;

public class Vmm
{
    /**
     * The VMM host.
     */
    protected Unit host;
    /**
     * Hosted VMs
     */
    protected HashSet<Unit> hosted;
    /**
     * Total number of vms that can be allocated
     * on this host.
     */
    protected int capacity;
 
    /**
     * Creates a new VMM record.
     * @param host VMM host
     * @param capacity capacity
     */
    public Vmm(Unit host, int capacity)
    {
        if (host == null){
            throw new IllegalArgumentException("host cannot be null");
        }
        
        if (capacity < 1){
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        
        this.host = host;
        this.capacity = capacity;
        this.hosted = new HashSet<Unit>(capacity);
    }   
    
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Vmm){
            return false;
        }
        
        Vmm other = (Vmm)o;
        return host.getID().equals(other.host.getID());
    }
    
    @Override
    public int hashCode()
    {
        return host.getID().hashCode();
    }
    
    /**
     * Releases a VM hosted on this VMM.
     * @param vm VM to release
     */
    public void release(Unit vm)
    {
        if (vm == null){
            throw new IllegalArgumentException("vm cannot be null");
        }
        
        if (!hosted.contains(vm)){
            throw new IllegalStateException("the specified node is not hosted on this vmm");
        }
        
        hosted.remove(vm);
    }
    
    /**
     * Adds the specified VM to be hosted on this VMM.
     * @param vm VM to be hosted
     */
    public void host(Unit vm)
    {
        if (vm == null){
            throw new IllegalArgumentException("vm cannot be null");
        }
        
        if (hosted.contains(vm)){
            throw new IllegalStateException("the specified node is already being hosted on this VMM");
        }

        hosted.add(vm);        
    }
    
    /**
     * Returns the number of hosted VMs on this host.
     * @return number of hosted VMs
     */
    public int getHostedCount()
    {
        return hosted.size();
    }
    
    /**
     * Returns the number of VMs that can be hosted on this VMM.
     * @return number of VMs that can be hosted.
     */
    public int getCapacity()
    {
        return capacity;
    }
    
    /**
     * Returns the number of VMs that can be added to this VMM.
     * @return number of VMs that can be added
     */
    public int getAvailable()
    {
        return capacity - hosted.size();
    }
    
    /**
     * Returns the VMM host.
     * @return VMM host
     */
    public Unit getHost()
    {
        return host;
    }
 
    /**
     * Returns the set of all hosted VMs on this VMM.
     * @return set of all hosted VMs
     */
    public HashSet<Unit> getHostedVMs()
    {
        return (HashSet<Unit>)hosted.clone();
    }
}
