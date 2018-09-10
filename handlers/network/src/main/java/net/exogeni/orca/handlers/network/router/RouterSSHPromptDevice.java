package net.exogeni.orca.handlers.network.router;

import net.exogeni.orca.handlers.network.core.SSHConsolePromptDevice;

public abstract class RouterSSHPromptDevice extends SSHConsolePromptDevice implements RouterConstants, IRouterDevice {
    public RouterSSHPromptDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
    }
}
