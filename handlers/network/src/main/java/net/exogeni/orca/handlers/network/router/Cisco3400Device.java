package net.exogeni.orca.handlers.network.router;

import java.util.Properties;

import net.exogeni.orca.handlers.network.core.CommandException;

public class Cisco3400Device extends Cisco6509Device {

    public Cisco3400Device(String deviceAddress, String uid, String password, String adminPassword,
            String defaultPrompt) {
        super(deviceAddress, uid, password, adminPassword, defaultPrompt);
        basepath = "/net/exogeni/orca/handlers/network/router/cisco/3400";
    }

}
