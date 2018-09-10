package net.exogeni.orca.shirako.common.meta;

public interface ResourceProperties {
    
    
    /*
     * NOTE: Conventions used in this file:
     * 
     * 1. Each property name starts with Resource
     * 2. Each property value starts with "resource."
     */
    
    
    public static final String ResourceMemory   = "resource.memory";
    public static final String ResourceCPU      = "resource.cpu";
    public static final String ResourceBandwidth = "resource.bandwidth";  
    
    public static final String ResourceNumCPUCores   = "resource.numCPUCores";
    public static final String ResourceMemoryCapacity      = "resource.memeoryCapacity";
    public static final String ResourceStorageCapacity = "resource.storageCapacity"; 
    /**
     * Abstract ndl representation: full NDL file (not just URL to the file)
     */
    public static final String ResourceNdlAbstractDomain = "resource.ndl.adomain";
    /**
     * Class implementing the inventory for type interface (in broker policies).
     */
    public static final String ResourceClassInventoryForType = "resource.class.invfortype";
    
    public static final String ResourceStartIface = "resource.siface";
    public static final String ResourceEndIface = "resource.eiface";
    /**
     * The domain from which the resources originate.
     */
    public static final String ResourceDomain = "resource.domain";
    
    public static final String ResourceAvailableUnits = "resource.units.now";
}
