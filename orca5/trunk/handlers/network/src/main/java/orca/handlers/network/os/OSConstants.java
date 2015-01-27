package orca.handlers.network.os;

public interface OSConstants {
    public static final String PropertyDeviceAddress = "deviceAddress";
    public static final String PropertyDeviceUID = "UID";
    public static final String PropertyDevicePWD = "PWD";
    public static final String PropertyInputPort = "inputPort";
    public static final String PropertyOutputPort = "outputPort";
    public static final String PropertyPort = "port";
    public static final String PropertyCTAG = "ctag";
    
    public static final String CommandCreatePatch = "PolatisCreatePatchRequest";
    public static final String CommandDeletePatch = "PolatisDeletePatchRequest";
       
    public static final int ErrorMissingInputPort = -20000;
    public static final int ErrorMissingOutputPort = -20001;
    public static final int ErrorMissingPort = -20002;
    public static final int ErrorMissingDeviceAddress = -20010;
    public static final int ErrorUndefined = -20099;              
}