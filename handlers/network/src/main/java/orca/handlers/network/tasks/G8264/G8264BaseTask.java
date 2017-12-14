package orca.handlers.network.tasks.G8264;

import orca.handlers.network.router.G8264RouterDevice;
import orca.handlers.network.router.IRouterDevice;
import orca.handlers.network.tasks.SyncNetworkBaseTask;

public abstract class G8264BaseTask extends SyncNetworkBaseTask {

    public static final String G8264 = "g8264";
    protected IRouterDevice router;

    @Override
    protected void makeDevice() {
        super.makeDevice();

        if (deviceInstance.equalsIgnoreCase(G8264)) {
            try {
                router = new G8264RouterDevice(deviceAddress, user, password);
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate " + G8264 + " device: " + e);
            }
        } else {
            throw new RuntimeException("Unsupported router: " + deviceInstance);
        }
        configureDevice(router);
    }

}
