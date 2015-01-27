package orca.handlers.network.tasks.EX3200;

import orca.handlers.network.router.EX3200RouterDevice;
import orca.handlers.network.router.IRouterDevice;
import orca.handlers.network.tasks.SyncNetworkBaseTask;

public abstract class EX3200BaseTask extends SyncNetworkBaseTask {
    
    public static final String EX3200 = "ex3200";
    protected IRouterDevice router;
    
    @Override
    protected void makeDevice() {
    	super.makeDevice();
    	
        if (deviceInstance.equalsIgnoreCase(EX3200)) {
            router = new EX3200RouterDevice(deviceAddress, user, password);
        } else {
            throw new RuntimeException("Unsupported router: " + deviceInstance);
        }        
        configureDevice(router);
    }

}
