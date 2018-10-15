package net.exogeni.orca.handlers.network.os;

import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.INetworkDevice;
import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.INetworkDevice;

public interface IOSDevice extends INetworkDevice {
    public void createPatch(String inputPort, String outputPort, String ctag) throws CommandException;

    public void deletePatch(String port, String ctag) throws CommandException;
}