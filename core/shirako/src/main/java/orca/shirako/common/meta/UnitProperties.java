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
     * Comet Properties
     */
    public static final String UnitCometReadToken = "unit.comet.readToken";
    public static final String UnitCometWriteToken = "unit.comet.writeToken";
    public static final String SliceCometReadToken = "slice.comet.readToken";
    public static final String SliceCometWriteToken = "slice.comet.writeToken";
    public static final String UnitCometHostsGroupToRead = "unit.comet.hosts.group.read";
    public static final String UnitCometPubKeysGroupToRead = "unit.comet.pubkeys.group.read";
    public static final String UnitCometHostsGroupToWrite = "unit.comet.hosts.group.write";
    public static final String UnitCometPubKeysGroupToWrite = "unit.comet.pubkeys.group.write";
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
    public static final String ONE					   = "1";
	public static final String YES 					   = "yes";
	public static final String NO 					   = "no";
    public static final String DOT                     = ".";
    
    public static final String UnitEthPrefix           = "unit.eth";
    public static final String UnitEthVlan			   = "vlan.tag";
    public static final String UnitEthVlanSuffix       = DOT + UnitEthVlan;
    public static final String UnitEthIP			   = "ip";
    public static final String UnitEthIPSuffix         = DOT + UnitEthIP;
    public static final String UnitEthMode			   = "mode";
    public static final String UnitEthModeSuffix       = DOT + UnitEthMode;
    public static final String UnitHostEth			   = "hosteth";
    public static final String UnitHostEthSuffix       = DOT + UnitHostEth;
    public static final String UnitEthMac			   = "mac";
    public static final String UnitEthMacSuffix        = DOT + UnitEthMac;
    public static final String UnitEthState	 	       = "state";
    public static final String UnitEthStateSuffix 	   = DOT + UnitEthState;
    public static final String UnitEthIPVersion		   = "ipversion";
    public static final String UnitEthIPVersionSuffix  = DOT + UnitEthIPVersion;
    public static final String UnitEthNetworkUUID      = "net.uuid";
    public static final String UnitEthNetworkUUIDSuffix= DOT + UnitEthNetworkUUID;
    public static final String UnitEthUUID			   = "uuid";
    public static final String UnitEthUUIDSuffix       = DOT + UnitEthUUID;
    public static final String UnitEthParentUrl	       = "parent.url";
    public static final String UnitEthParentUrlSuffix  = DOT + UnitEthParentUrl;
    public static final String UnitEthNetmask          = "netmask";
    public static final String UnitEthNetmaskSuffix    = DOT + UnitEthNetmask;

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
    public static final String ISCSIInitiatorIQN             = "iscsi.initiator.iqn";
    public static final String UnitISCSIInitiatorIQN         = "unit." + ISCSIInitiatorIQN;
    public static final String UnitTargetPrefix              = "unit.target";
    public static final String UnitStoragePrefix             = "unit.storage";
    public static final String UnitStoreType                 = "type";
    public static final String UnitStoreTypeSuffix           = DOT + UnitStoreType;
    public static final String UnitTargetIP                  = "target.ip";
    public static final String UnitTargetIPSuffix            = DOT + UnitTargetIP;
    public static final String UnitTargetPort                = "target.port";
    public static final String UnitTargetPortSuffix          = DOT + UnitTargetPort;
    public static final String UnitTargetName                = "target.name";
    public static final String UnitTargetNameSuffix          = DOT + UnitTargetName;
    public static final String UnitTargetLun                 = "target.lun.num";
    public static final String UnitTargetLunSuffix           = DOT + UnitTargetLun;
    public static final String UnitTargetLunGuid     	     = "target.lun.guid";
    public static final String UnitTargetLunGuidSuffix	     = DOT + UnitTargetLunGuid;
    public static final String UnitTargetShouldAttach        = "target.should_attach";
    public static final String UnitTargetShouldAttachSuffix  = DOT + UnitTargetShouldAttach;
    public static final String UnitTargetChapUser            = "target.chap_user";
    public static final String UnitTargetChapUserSuffix      = DOT + UnitTargetChapUser;
    public static final String UnitTargetChapSecret          = "target.chap_password";
    public static final String UnitTargetChapSecretSuffix    = DOT + UnitTargetChapSecret;
    public static final String UnitFSType                    = "fs.type";
    public static final String UnitFSTypeSuffix              = DOT + UnitFSType;
    public static final String UnitFSOptions                 = "target.options";
    public static final String UnitFSOptionsSuffix           = DOT + UnitFSOptions;
    public static final String UnitFSShouldFormat            = "target.should_format";
    public static final String UnitFSShouldFormatSuffix      = DOT + UnitFSShouldFormat;
    public static final String UnitFSMountPoint              = "target.mount_point";
    public static final String UnitFSMountPointSuffix        = DOT + UnitFSMountPoint;

    public static final String DefaultFSOptions				 = "-F -b 1024";
    public static final String DefaultMountPoint			 = "/mnt/target/";
    public static final String DefaultBlockDeviceType		 = "iscsi";
    public static final String DefaultISCSIPort				 = "3260";
    public static final String DefaultFSType				 = "ext3";


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
    
	
	public static final String ModifySubcommandPrefix="modify.subcommand.";
	public static final String ModifyPrefix="modify.";
	public static final String ZERO = "0";
	
	public static final String SliceStitchPrefix = "sliceStitch";
	public static final String SliceStitchAllowed = "allowed";
	public static final String SliceStitchPass = "password";
	public static final String SliceStitchToReservation = "toreservation";
	public static final String SliceStitchToSlice = "toslice";
	public static final String SliceStitchUUID = "stitchUUID";
	public static final String SliceStitchPerformed = "performed";
	public static final String SliceStitchUndone = "undone";
	public static final String SliceStitchDN = "stitch.dn";

}
