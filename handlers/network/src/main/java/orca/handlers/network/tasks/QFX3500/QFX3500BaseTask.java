package orca.handlers.network.tasks.QFX3500;

import orca.handlers.network.router.IMappingRouterDevice;
import orca.handlers.network.router.QFX3500RouterDevice;
import orca.handlers.network.tasks.SyncNetworkBaseTask;

public abstract class QFX3500BaseTask extends SyncNetworkBaseTask {
    
    public static final String QFX3500 = "qfx3500";
    protected IMappingRouterDevice router;
    
    @Override
    protected void makeDevice() {
    	super.makeDevice();
    	
        if (deviceInstance.equalsIgnoreCase(QFX3500)) {
            router = new QFX3500RouterDevice(deviceAddress, user, password);
        } else {
            throw new RuntimeException("Unsupported router: " + deviceInstance);
        }        
        configureDevice(router);
    }

}
