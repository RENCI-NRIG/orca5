package orca.handlers.network.dtn;

public class InfineraDTNDevice extends DTNDevice {
    public InfineraDTNDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
        basepath = "/orca/handlers/network/dtn/InfineraDTN";
        telnetPort = 9090;
    }
}
