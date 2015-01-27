package orca.handlers.network;

import orca.handlers.network.os.OSDevice;
import orca.handlers.network.os.PolatisOSDevice;

public class PolatisOSDeviceTest extends NetworkDeviceTest {
    public static final String PropertyOS = "os";
    public static final String PropertyOSUser = "os.user";
    public static final String PropertyOSPassword = "os.password";
    public static final String PropertyOSInputPort = "os.inputPort";
    public static final String PropertyOSOutputPort = "os.outputPort";
    public static final String PropertyOSCtag = "os.ctag";

    protected OSDevice getDevice() {
        OSDevice device = new PolatisOSDevice(props.getProperty(PropertyOS), props.getProperty(PropertyOSUser), props.getProperty(PropertyOSPassword));
        configureDevice(device);
        return device;
    }

    public void testCreatePatch() throws Exception {
        OSDevice device = getDevice();
        device.createPatch(props.getProperty(PropertyOSInputPort), props.getProperty(PropertyOSOutputPort), props.getProperty(PropertyOSCtag));
    }

    public void testDeletePatch() throws Exception {
        OSDevice device = getDevice();
        device.deletePatch(props.getProperty(PropertyOSInputPort), props.getProperty(PropertyOSCtag));
    }
}
