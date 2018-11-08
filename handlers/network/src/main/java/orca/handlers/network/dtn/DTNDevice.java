package orca.handlers.network.dtn;

import java.util.Properties;

import orca.handlers.network.core.CommandException;
import orca.handlers.network.core.TelnetConsoleDevice;

public class DTNDevice extends TelnetConsoleDevice implements DTNConstants, IDTNDevice {
    public DTNDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
    }

    @Override
    protected Properties getProperties() {
        Properties p = super.getProperties();
        p.setProperty(PropertyDeviceUID, uid);
        p.setProperty(PropertyDevicePWD, password);
        return p;
    }

    public void createCrossConnect(String sourcePort, String destinationPort, String payloadType, String ctag)
            throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertySrcPort, sourcePort);
        p.setProperty(PropertyDstPort, destinationPort);
        String payload = getPayload(sourcePort, destinationPort, payloadType);
        p.setProperty(PropertyPayloadType, payload);
        p.setProperty(PropertyCTag, ctag);
        executeScript(CommandCreateCRS, p);
    }

    public void deleteCrossConnect(String sourcePort, String destinationPort, String ctag) throws CommandException {
        Properties p = getProperties();
        p.setProperty(PropertySrcPort, sourcePort);
        p.setProperty(PropertyDstPort, destinationPort);
        p.setProperty(PropertyCTag, ctag);
        executeScript(CommandDeleteCRS, p);
    }

    // for the line port to line port express crossconnect
    public String getPayload(String src, String dst, String payloadType) {
        String payload = payloadType;
        String line = "l";

        int index = src.indexOf(line);
        int index_d = dst.indexOf(line);

        if (index > 0 & index_d > 0) {
            if (payloadType.toLowerCase().startsWith("4xoc192") || payloadType.toLowerCase().startsWith("oc768"))
                payload = "40G";
            if (payloadType.toLowerCase().startsWith("10g") || payloadType.toLowerCase().startsWith("oc192"))
                payload = "10G";
            if (payloadType.toLowerCase().startsWith("1g") || payloadType.toLowerCase().startsWith("oc48"))
                payload = "25G";
        }

        return payload;
    }
}
