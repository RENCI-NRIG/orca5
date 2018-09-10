package net.exogeni.orca.handlers.network.dtn;

/**
 * This file contains constants used to configure an Infinera DTN.
 * 
 * @author dee
 */
public interface DTNConstants {
    public static final String PropertyDeviceAddress = "deviceAddress";
    public static final String PropertyDeviceUID = "UID";
    public static final String PropertyDevicePWD = "PWD";
    public static final String PropertySrcPort = "srcPort";
    public static final String PropertyDstPort = "dstPort";
    public static final String PropertyCTag = "ctag";
    public static final String PropertyPayloadType = "payloadType";

    public static final String CommandCreateCRS = "DTNCreateCRSRequest";
    public static final String CommandDeleteCRS = "DTNDeleteCRSRequest";

    public static final int ErrorMissingSrcPort = -20000;
    public static final int ErrorMissingDstPort = -20002;
    public static final int ErrorMissingCTag = -20006;
    public static final int ErrorMissingPayload = -20008;
    public static final int ErrorMissingDeviceAddress = -20010;
    public static final int ErrorUndefined = -20099;

}
