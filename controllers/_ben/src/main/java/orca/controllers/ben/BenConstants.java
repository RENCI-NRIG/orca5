package orca.controllers.ben;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.shirako.common.ResourceType;

public interface BenConstants {
    /**
     * Package identifier.
     */
    public static final PackageId MyPackageId = new PackageId("f7ec610c-d838-4239-87fc-869a8cbf848e");    
    
    public static final PluginId SimpleBenControllerId = new PluginId("1");
    
    public static final PluginId BenNlrControllerId = new PluginId("2");
    
    public static final PluginId InterndomainControllerId = new PluginId("7");
    
    /**
     * VM broker name.
     */
    public static final String VMBrokerName = "vm-broker";
    /**
     * Ben broker name.
     */
    public static final String VlanBrokerName = "vlan-broker";
    /**
     * Resource type: BEN VLAN
     */
    public static final ResourceType ResourceTypeBenVlan = new ResourceType("ben.vlan");
    /**
     * Resource type: NLR VLAN
     */
    public static final ResourceType ResourceTypeNlrVlan = new ResourceType("nlr.vlan");
    /**
     * Resource type: DUKE VLAN
     */
    public static final ResourceType ResourceTypeDukeVlan = new ResourceType("duke.vlan");
    
    /**
     * Resource type: VM at RENCI
     */
    public static final ResourceType ResourceTypeVmRenci = new ResourceType("renci.vm");
    /**
     * Resource type: VM at DUKE
     */
    public static final ResourceType ResourceTypeVmDuke = new ResourceType("duke.vm");
    /**
     * Resource type: VM at UNC
     */
    public static final ResourceType ResourceTypeVmUnc = new ResourceType("unc.vm");  
    
    public static final String BenRequestIDProperty = "ben.requestid"; 
    
    public static final String PropertyNlrTagDuke = "nlr.vlan.duke";
    public static final String PropertyNlrTagRenci = "nlr.vlan.renci";
        
}