package net.exogeni.orca.handlers.network.os;

import java.util.Properties;

import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.TelnetConsoleDevice;
import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.TelnetConsoleDevice;

public abstract class OSDevice extends TelnetConsoleDevice implements OSConstants, IOSDevice {
    public OSDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
    }

    @Override
    protected Properties getProperties() {
        Properties p = super.getProperties();
        p.setProperty(PropertyDeviceUID, uid);
        p.setProperty(PropertyDevicePWD, password);
        return p;
    }

    public void createPatch(String inputPort, String outputPort, String ctag) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyInputPort, inputPort);
        p.setProperty(PropertyOutputPort, outputPort);
        p.setProperty(PropertyCTAG, ctag);
        executeScript(CommandCreatePatch, p);
    }

    public void deletePatch(String port, String ctag) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertyPort, port);
        p.setProperty(PropertyCTAG, ctag);
        executeScript(CommandDeletePatch, p);
    }
}
