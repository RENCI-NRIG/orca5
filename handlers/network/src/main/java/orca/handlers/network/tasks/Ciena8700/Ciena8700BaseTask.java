package orca.handlers.network.tasks.Ciena8700;

import orca.handlers.network.router.Ciena8700Device;
import orca.handlers.network.router.Cisco6509Device;
import orca.handlers.network.router.IMappingRouterDevice;
import orca.handlers.network.tasks.SyncNetworkBaseTask;


public abstract class Ciena8700BaseTask extends SyncNetworkBaseTask {
    
    public static final String Ciena8700 = "ciena8700";
    protected IMappingRouterDevice router;
    protected String adminPassword;

    @Override
    protected void makeDevice() {
    	super.makeDevice();
    	
        if (deviceInstance.equalsIgnoreCase(Ciena8700)) {
            router = new Ciena8700Device(deviceAddress, user, password);
        } else {
            throw new RuntimeException("Unsupported router: " + deviceInstance);
        }        
        configureDevice(router);
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
