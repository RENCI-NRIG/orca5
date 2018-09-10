package net.exogeni.orca.handlers.network.router;

import net.exogeni.orca.handlers.network.core.SSHConsoleDevice;

public abstract class RouterSSHDevice extends SSHConsoleDevice implements RouterConstants, IRouterDevice {
    public RouterSSHDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
    }

}
