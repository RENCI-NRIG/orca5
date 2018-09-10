package net.exogeni.orca.handlers.network.os;

public class PolatisOSDevice extends OSDevice {
    public PolatisOSDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
        basepath = "/net/exogeni/orca/handlers/network/os/polatis";
        telnetPort = 3082;
    }
}