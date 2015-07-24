package orca.controllers.openflow;

import orca.manage.extensions.api.PackageId;
import orca.manage.extensions.api.PluginId;
import orca.shirako.common.ResourceType;

public interface OpenFlowControllerConstants {
	// add a new package id 
	public static final PackageId MyPackageId = new PackageId("e11fd4ce-a727-434c-b17d-00449328b67f");
	// Add SimpleOpenFlowControllerId
	// Use PluginId "1" to avoid Invalid plugin type
	public static final PluginId OpenFlowControllerId = new PluginId("1");
	
	/** Add a new request id property: OpenFlow */
    // Where is it used?
    public static final String OpenFlowRequestIDProperty = "openflow.requestid";

    // add new variable: UnitOfSlice, unit of OpenFlow resources
    public static final String UnitOfSlice = "unit.of.slice";
    
    // add new variable: OpenFlowBrokerName
    public static final String OpenFlowBrokerName = "ndl-broker";   
    
    /**
     * VM broker name.
     */
    public static final String VMBrokerName = "ndl-broker";
    
    /** Add a new resource type: OpenFlow */
    public static final ResourceType ResourceTypeOpenflow = new ResourceType("openflow");
    
    
    /**
     * Resource type: VM at RENCI
     */
    public static final ResourceType ResourceTypeVmRenci = new ResourceType("unc.vm");
    /**
     * Resource type: VM at DUKE
     */
    public static final ResourceType ResourceTypeVmDuke = new ResourceType("unc.vm");
    
}
