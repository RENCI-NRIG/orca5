package net.exogeni.orca.handlers.network.tasks;

import net.exogeni.orca.handlers.network.dtn.IDTNDevice;
import net.exogeni.orca.handlers.network.dtn.InfineraDTNDevice;

public abstract class DTNBaseTask extends SyncNetworkBaseTask {
    public static final String InfineraDTN = "infineraDTN";
    protected IDTNDevice device;

    @Override
    protected void makeDevice() {
        super.makeDevice();

        if (deviceInstance.equalsIgnoreCase(InfineraDTN)) {
            device = new InfineraDTNDevice(deviceAddress, user, password);
        } else {
            throw new RuntimeException("Unsupported DTN device: " + deviceInstance);
        }
        configureDevice(device);
    }
}
