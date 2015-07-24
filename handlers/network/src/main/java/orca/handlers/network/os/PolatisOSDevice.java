package orca.handlers.network.os;

public class PolatisOSDevice extends OSDevice {
    public PolatisOSDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
        basepath = "/orca/handlers/network/os/polatis";
        telnetPort = 3082;
    }
}