package orca.handlers.network.router;

/**
 * This file contains constants used to configure 6509 router.
 * @author dee
 */
public interface RouterConstants {
    public static final String PropertyVLANTagNm = "VLANTagName";
    public static final String PropertyDeviceAddress = "deviceAddress";
    public static final String PropertyDeviceUID = "UID";
    public static final String PropertyDevicePWD = "PWD";
    public static final String PropertyDeviceAdminPWD = "adminPWD";
    public static final String PropertyDefaultPrompt = "DefaultPrompt";
    public static final String PropertyVLANNm = "VLANName";
    public static final String PropertyQoSPolicyNm = "QoSPolicyName";
    public static final String PropertyQoSRateNm = "QoSRate";
    public static final String PropertyQoSBurstSizeNm = "QoSBurstSize";
    public static final String PropertySrcVLAN = "srcVLAN";
    public static final String PropertyDstVLAN = "dstVLAN";
    public static final String PropertySrcPort = "srcPort";
    public static final String PropertyDstPort = "dstPort";
    public static final String PropertyPorts = "ports";
    public static final String PropertyVMAP = "VMAP";
//    public static final String PropertyGbPorts = "GbPorts";
//    public static final String Property10GbPorts = "10GbPorts";
    public static final String PropertyAccessPorts = "AccessPorts";
    public static final String PropertyTrunkPorts = "TrunkPorts";
    public static final String PropertyPort = "port";
    public static final String PropertyVLANNamePrefix ="orca_vlan_";
    public static final String PropertyPolicyNamePrefix = "orca_policy_";
    
    public static final String CommandCreateVLAN = "CreateVLANRequest";
    public static final String CommandDeleteVLAN = "DeleteVLANRequest";
    public static final String CommandCreateQoSVLAN = "CreateQoSVLANRequest";
    public static final String CommandDeleteQoSVLAN = "DeleteQoSVLANRequest";
    public static final String CommandMapVLANS = "MapVLANSRequest";
    public static final String CommandUnmapVLANS = "UnmapVLANSRequest";
    public static final String CommandAddTrunkPorts = "AddTrunkPortsRequest";
    public static final String CommandAddAccessPorts = "AddAccessPortsRequest";
    public static final String CommandRemoveTrunkPorts = "RemoveTrunkPortsRequest";
    public static final String CommandRemoveAccessPorts = "RemoveAccessPortsRequest";

    public static final int ErrorMissingTagName = -20000;
    public static final int ErrorMissingDeviceAddress = -20010;
    public static final int ErrorUndefined = -20099;
    public static final String TenGigabitInterface = "tengigabitethernet";

    public static final String resourcePath = "/orca/drivers/network/Cisco6509/";
    public static final String propsFileNm = "Cisco6509.properties";
    public static final String propsFilePath = resourcePath + propsFileNm;
    public static final String DeviceTypeNm = "Cisco6509";
}
