package net.exogeni.orca.handlers.network.tasks.Cisco3400;

import net.exogeni.orca.handlers.network.router.Cisco3400Device;
import net.exogeni.orca.handlers.network.router.IMappingRouterDevice;
import net.exogeni.orca.handlers.network.router.Cisco3400Device;
import net.exogeni.orca.handlers.network.router.IMappingRouterDevice;
import net.exogeni.orca.handlers.network.tasks.SyncNetworkBaseTask;

public abstract class Cisco3400BaseTask extends SyncNetworkBaseTask {

    public static final String Cisco3400 = "cisco3400";
    protected IMappingRouterDevice router;
    protected String adminPassword;
    protected String defaultPrompt;

    @Override
    protected void makeDevice() {
        super.makeDevice();

        if (deviceInstance.equalsIgnoreCase(Cisco3400)) {
            router = new Cisco3400Device(deviceAddress, user, password, adminPassword, defaultPrompt);
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
