package orca.handlers.network.os;

import orca.handlers.network.core.CommandException;
import orca.handlers.network.core.INetworkDevice;

public interface IOSDevice extends INetworkDevice {
    public void createPatch(String inputPort, String outputPort, String ctag) throws CommandException;

    public void deletePatch(String port, String ctag) throws CommandException;
}