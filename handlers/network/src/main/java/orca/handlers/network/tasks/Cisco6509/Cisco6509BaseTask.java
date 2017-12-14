package orca.handlers.network.tasks.Cisco6509;

import orca.handlers.network.router.Cisco6509Device;
import orca.handlers.network.router.IMappingRouterDevice;
import orca.handlers.network.tasks.SyncNetworkBaseTask;

public abstract class Cisco6509BaseTask extends SyncNetworkBaseTask {

    public static final String Cisco6509 = "cisco6509";
    protected IMappingRouterDevice router;
    protected String adminPassword;
    protected String defaultPrompt;

    @Override
    protected void makeDevice() {
        super.makeDevice();

        if (deviceInstance.equalsIgnoreCase(Cisco6509)) {
            router = new Cisco6509Device(deviceAddress, user, password, adminPassword, defaultPrompt);
        } else {
            throw new RuntimeException("Unsupported router: " + deviceInstance);
        }
        configureDevice(router);
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void setDefaultPrompt(String prompt) {
        this.defaultPrompt = prompt;
    }
}
