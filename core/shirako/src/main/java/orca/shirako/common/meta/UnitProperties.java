package orca.shirako.common.meta;

public interface UnitProperties { 
    /*
     * NOTE: Conventions used in this file:
     * 
     * 1. Each property name starts with Unit
     * 2. Each property value starts with "unit."
     */
    
    /*
     * Core unit properties.
     */
    
    public static final String UnitID               = "unit.id";
    public static final String UnitResourceType     = "unit.resourceType"; // if you change this also change Config.PropertyResourceType
    public static final String UnitInternalState    = "unit.state";    
    public static final String UnitActionSequence   = "unit.sequence";        
    public static final String UnitReservationID    = "unit.rid";
    public static final String UnitSliceID          = "unit.sliceid";
    public static final String UnitActorID          = "unit.actorid";
    public static final String UnitSite             = "unit.site";  // site guid
    public static final String UnitSliceName        = "unit.slice.name"; //slice urn/url
    public static final String UnitURL              = "unit.url";  //unit url
    
    public static final String UnitInventoryIP      = "unit.inventory.ip";
    public static final String UnitControl          = "unit.control";
    
    public static final String UnitStatus           = "unit.status";
    
    public static final String UnitHostName         = "unit.hostname";
    
    public static final String UnitHostNameUrl      = "unit.hostname.url";
    
    public static final String UnitDomain         = "unit.domain";

    /*
     * Information about parent unit.
     */
    
    public static final String UnitParentID         = "unit.parent.id";
    public static final String UnitParentIP         = "unit.parent.ip";
    public static final String UnitParentHostName   = "unit.parent.hostname";
    

    /*
     * Unit resource properties.
     */
    public static final String UnitDeviceNum        = "unit.device.num";
    public static final String UnitActionNum        = "unit.action.num";
    public static final String UnitMemory           = "unit.memory";
    public static final String UnitCPU              = "unit.cpu";
    public static final String UnitBandwidth        = "unit.bandwidth";
    public static final String UnitVlanUrl          = "unit.vlan.url";
    public static final String UnitVlanTag          = "unit.vlan.tag";
    public static final String UnitVlanTags          = "unit.vlan.tags";
    public static final String UnitVlanQoSRate      = "unit.vlan.qos.rate";
    public static final String UnitVlanQoSBurstSize = "unit.vlan.qos.burst.size";
    public static final String UnitVlanHostEth      = "unit.vlan.hosteth";
    public static final String UnitQuantumNetname   = "unit.quantum.netname";
    public static final String UnitQuantumNetUUID   = "unit.quantum.net.uuid";
    public static final String UnitNumberInterface  = "unit.number.interface";
    
    
    public static final String UnitPortList = "unit.portlist";

    
    /*
     * Management network settings (can be either v4/v6, depends on handler/driver/network).
     */
    
    public static final String UnitManagementIP     = "unit.manage.ip";
    public static final String UnitManagementPort   = "unit.manage.port";
    public static final String UnitManageSubnet     = "unit.manage.subnet";
    public static final String UnitManageGateway    = "unit.manage.gateway";
    public static final String UnitManageInterface  = "unit.manage.interface";
    
    /*
     * Data network settings
     */
 
    public static final String UnitDataIP           = "unit.data.ip";
    public static final String UnitDataSubnet       = "unit.data.subnet";
    public static final String UnitDataGateway      = "unit.data.gateway";
    public static final String UnitDataInterface    = "unit.data.interface";
    
    
    /* 
     * EC2/Eucalyptus
     */
    public static final String UnitEC2Instance      = "unit.ec2.instance";
    public static final String UnitEC2PrivateIP     = "unit.ec2.private.ip";
    public static final String UnitEC2Host          = "unit.ec2.host";

    /* 
     * xCAT
     */
    public static final String UnitXCATNodename     = "unit.xcat.nodename";
    
    
    /*
     * Network interface-specific configuration.
     * These are of the form:
     * unit.ethX.vlan.tag or unit.ethX.ip,
     * where X is the interface number. In general, Orca, or
     * its components/dependencies may reserve control over eth0, so 
     * the first configurable interface would generally be eth1. 
     */
    
    public static final String UnitEthPrefix           = "unit.eth";    
    public static final String UnitEthVlanSuffix       = ".vlan.tag";  
    public static final String UnitEthIPSuffix         = ".ip"; 
    public static final String UnitEthModeSuffix       = ".mode";
    public static final String UnitHostEthSuffix       = ".hosteth";
    public static final String UnitEthMacSuffix        = ".mac";
    public static final String UnitEthStateSuffix      = ".state";
    public static final String UnitEthIPVersionSuffix  = ".ipversion";
    public static final String UnitEthNetworkUUIDSuffix= ".net.uuid";

    /* Routing options NEuca v1.0 */
    public static final String UnitRouter              = "unit.isrouter";

    public static final String UnitRoutePrefix         = "unit.route";
    public static final String UnitRouteNetworkSuffix  = ".network";
    public static final String UnitRouteNexthopSuffix  = ".nexthop";


    /* 
     * Storage specific configuration.  For now only iSCSI
     *
     * These are of the form:
     * unit.storageX.vlan.tag or unit.storageX.ip, 
     * where X is the store number. 
     */
    public static final String UnitLUNTag          = "unit.target.lun"; 
    public static final String UnitStorageCapacity        = "unit.target.capacity";
    
    public static final String UnitVMGuid        = "unit.vm.guid";
    public static final String UnitVMIP        = "unit.vm.ip";
    public static final String UnitLUNGuid        = "unit.lun.guid";
    public static final String UnitSliceGuid        = "unit.slice.guid";
    
    public static final String UnitISCSIInitiatorIQNPrefix   = "unit.initiator.iqn_prefix";
    public static final String UnitISCSIInitiatorIQN         = "unit.iscsi.initiator.iqn";
    public static final String UnitTargetPrefix             = "unit.target";
    public static final String UnitStoragePrefix             = "unit.storage";
    public static final String UnitStoreTypeSuffix           = ".type";
    public static final String UnitTargetIPSuffix            = ".target.ip";
    public static final String UnitTargetPortSuffix          = ".target.port";
    public static final String UnitTargetNameSuffix          = ".target.name";
    public static final String UnitTargetLunSuffix           = ".target.lun.num";
    public static final String UnitTargetLunGuid			 = ".target.lun.guid";
    public static final String UnitTargetShouldAttachSuffix  = ".target.should_attach";
    public static final String UnitTargetChapUserSuffix      = ".target.chap_user";
    public static final String UnitTargetChapSecretSuffix    = ".target.chap_password";
    public static final String UnitFSTypeSuffix              = ".fs.type";
    public static final String UnitFSOptionsSuffix           = ".target.options";
    public static final String UnitFSShouldFormatSuffix      = ".target.should_format";
    public static final String UnitFSMountPointSuffix        = ".target.mount_point";



    /**
     * A configuration script (contents of the script, not the path to it) to
     * be executed when the instance starts. The script would be executed on the instance. 
     */
    public static final String UnitInstanceConfig = "unit.instance.config";
    public static final String UnitScriptPrefix   = "unit.script";
    

    public static final String UnitNotices = "unit.notices";
    
    /**
     * Miscellaneous properties
     */
    public static final String UserDN = "xmlrpc.user.dn";
}
