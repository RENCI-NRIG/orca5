package orca.handlers.network.dtn;

import orca.handlers.network.core.CommandException;
import orca.handlers.network.core.INetworkDevice;

public interface IDTNDevice extends INetworkDevice {
    public void createCrossConnect(String sourcePort, String destinationPort, String payloadType, String ctag)
            throws CommandException;

    public void deleteCrossConnect(String sourcePort, String destinationPort, String ctag) throws CommandException;
}