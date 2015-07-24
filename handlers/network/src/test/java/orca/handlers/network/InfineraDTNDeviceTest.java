package orca.handlers.network;

import orca.handlers.network.dtn.DTNDevice;
import orca.handlers.network.dtn.InfineraDTNDevice;

public class InfineraDTNDeviceTest extends NetworkDeviceTest {
    public static final String PropertyDTN = "dtn";
    public static final String PropertyDTNUser = "dtn.user";
    public static final String PropertyDTNPassword = "dtn.password";
    public static final String PropertyDTNSourcePort = "dtn.srcPort";
    public static final String PropertyDTNDestinationPort = "dtn.dstPort";
    public static final String PropertyDTNPayloadType = "dtn.payloadType";
    public static final String PropertyDTNCtag = "dtn.ctag";

    protected DTNDevice getDevice() {
        DTNDevice device = new InfineraDTNDevice(props.getProperty(PropertyDTN), props.getProperty(PropertyDTNUser), props.getProperty(PropertyDTNPassword));
        configureDevice(device);
        return device;
    }

    public void testCreateCrossConnect() throws Exception {
        DTNDevice device = getDevice();
        device.createCrossConnect(props.getProperty(PropertyDTNSourcePort), props.getProperty(PropertyDTNDestinationPort), props.getProperty(PropertyDTNPayloadType), props.getProperty(PropertyDTNCtag));
    }

    public void testDeleteCrossConnect() throws Exception {
        DTNDevice device = getDevice();
        device.deleteCrossConnect(props.getProperty(PropertyDTNSourcePort), props.getProperty(PropertyDTNDestinationPort), props.getProperty(PropertyDTNCtag));
    }
}
