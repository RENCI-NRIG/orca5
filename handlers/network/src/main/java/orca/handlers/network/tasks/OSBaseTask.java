package orca.handlers.network.tasks;

import orca.handlers.network.os.IOSDevice;
import orca.handlers.network.os.PolatisOSDevice;

public abstract class OSBaseTask extends SyncNetworkBaseTask {
    public static final String PolatisOS = "polatisOS";
    protected IOSDevice device;

    @Override
    protected void makeDevice() {
    	super.makeDevice();
    	
        if (deviceInstance.equalsIgnoreCase(PolatisOS)) {
            device = new PolatisOSDevice(deviceAddress, user, password);
        } else {
            throw new RuntimeException("Unsupported OS device: " + deviceInstance);
        }
        configureDevice(device);
    }
}
