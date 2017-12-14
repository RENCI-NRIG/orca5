package orca.handlers.network;

import orca.handlers.network.router.EX3200RouterDevice;

public class EX3200DeviceTest extends NetworkDeviceTest {
    public static final String PropertyUser = "router.ex3200.user";
    public static final String PropertyPassword = "router.ex3200.password";
    public static final String PropertyRouter = "router.ex3200";
    public static final String PropertyVlanTag = "vlan.tag";
    public static final String PropertyPorts = "router.ex3200.ports";
    public static final String PropertyRouterSrcVlan = "router.ex3200.src.vlan.tag";
    public static final String PropertyRouterDstVlan = "router.ex3200.dst.vlan.tag";
    public static final String PropertyRouterMapPort = "router.ex3200.map.port";

    protected EX3200RouterDevice getDevice() {
        EX3200RouterDevice device = new EX3200RouterDevice(props.getProperty(PropertyRouter),
                props.getProperty(PropertyUser), props.getProperty(PropertyPassword));
        configureDevice(device);
        return device;
    }

    public void testCreateVLAN() throws Exception {
        EX3200RouterDevice device = getDevice();
        device.createVLAN(props.getProperty(PropertyVlanTag), "500000", "50000");
    }

    public void testDeleteVLAN() throws Exception {
        EX3200RouterDevice device = getDevice();
        device.deleteVLAN(props.getProperty(PropertyVlanTag), false);
    }

}
