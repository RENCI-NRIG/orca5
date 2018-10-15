package net.exogeni.orca.handlers.network.dtn;

import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.INetworkDevice;
import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.INetworkDevice;

public interface IDTNDevice extends INetworkDevice {
    public void createCrossConnect(String sourcePort, String destinationPort, String payloadType, String ctag)
            throws CommandException;

    public void deleteCrossConnect(String sourcePort, String destinationPort, String ctag) throws CommandException;
}