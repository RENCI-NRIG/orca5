package net.exogeni.orca.policy.core.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import net.exogeni.orca.shirako.common.UnitID;
import net.exogeni.orca.util.ResourceType;

public class VmmPool
{
    /**
     * Resource type for this pool.
     */
    protected ResourceType type;
    /**
     * Resource properties for this pool.
     */
    protected Properties properties;
    /**
     * Set of VMMs.
     */
    protected HashMap<UnitID, Vmm> vmms;
    /**
     * Per VM memory.
     */
    protected int memory;
    /**
     * Per VM CPU share.
     */
    protected int cpu;
    /**
     * Per VM bandwidth share.
     */
    protected int bandwidth;
    /**
     * Per VM disk share.
     */
    protected int disk;
    /**
     * Number of VMs that can be hosted on each VMM.
     */
    protected int capacity;
    
    /**
     * Creates a new <code>VmmPool</code>
     * @param type resource type
     * @param properties properties
     */
    public VmmPool(ResourceType type, Properties properties)
    {
        this.type = type;
        this.properties = properties;        
        vmms = new HashMap<UnitID, Vmm>();
    }
        
    /**
     * Adds a new VMM to the pool.
     * @param vmm VMM to add
     */
    public void donate(Vmm vmm)
    {
        if (vmm == null){
            throw new IllegalArgumentException("vmm cannot be null");
        }
        if (vmms.containsKey(vmm.getHost().getActorID())){
            throw new IllegalStateException("the specified vmm is already in the pool");
        }
        vmms.put(vmm.getHost().getID(), vmm);
    }
    
    public Collection<Vmm> getVmmSet()
    {
        return (Collection<Vmm>)vmms.values();
    }
    
    public Vmm getVmm(UnitID id)
    {
        return vmms.get(id);
    }
    
    /**
     * Returns the number of VMMs in this pool.
     * @return number of VMMs
     */
    public int getVmmsCount()
    {
        return vmms.size();
    }
    
    public Properties getProperties()
    {
        return properties;
    }


    public int getMemory()
    {
        return this.memory;
    }


    public void setMemory(int memory)
    {
        this.memory = memory;
    }


    public int getCpu()
    {
        return this.cpu;
    }


    public void setCpu(int cpu)
    {
        this.cpu = cpu;
    }


    public int getBandwidth()
    {
        return this.bandwidth;
    }


    public void setBandwidth(int bandwidth)
    {
        this.bandwidth = bandwidth;
    }


    public int getDisk()
    {
        return this.disk;
    }


    public void setDisk(int disk)
    {
        this.disk = disk;
    }


    public int getCapacity()
    {
        return this.capacity;
    }


    public void setCapacity(int capacity)
    {
        this.capacity = capacity;
    }
    
    public ResourceType getType()
    {
        return type;
    }
}
