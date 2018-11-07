package orca.handlers.network;

import orca.handlers.network.router.Cisco3400Device;

public class Cisco3400DeviceTest extends NetworkDeviceTest {
    public static final String PropertyAdminPassword = "router.3400.adminpassword";
    public static final String PropertyUser = "router.3400.user";
    public static final String PropertyPassword = "router.3400.password";
    public static final String PropertyRouter = "router.3400";
    public static final String PropertyVlanTag = "vlan.tag";
    public static final String PropertyPorts = "router.3400.ports";
    public static final String PropertyRouterSrcVlan = "router.3400.src.vlan.tag";
    public static final String PropertyRouterDstVlan = "router.3400.dst.vlan.tag";
    public static final String PropertyRouterMapPort = "router.3400.map.port";
    public static final String PropertyDefaultPrompt = "router.default.prompt";

    protected Cisco3400Device getDevice() {
        Cisco3400Device device = new Cisco3400Device(props.getProperty(PropertyRouter), props.getProperty(PropertyUser),
                props.getProperty(PropertyPassword), props.getProperty(PropertyAdminPassword),
                props.getProperty(PropertyDefaultPrompt));
        configureDevice(device);
        return device;
    }

    public void testCreateVLAN() throws Exception {
        Cisco3400Device device = getDevice();
        device.createVLAN(props.getProperty(PropertyVlanTag), "500000", "50000");
    }

    public void testDeleteVLAN() throws Exception {
        Cisco3400Device device = getDevice();
        device.deleteVLAN(props.getProperty(PropertyVlanTag), false);
    }

    public void testMapVLAN() throws Exception {
        Cisco3400Device device = getDevice();
        device.mapVLANs(props.getProperty(PropertyRouterSrcVlan), props.getProperty(PropertyRouterDstVlan),
                props.getProperty(PropertyRouterMapPort));
    }

    public void testUnmapVLAN() throws Exception {
        Cisco3400Device device = getDevice();
        device.unmapVLANs(props.getProperty(PropertyRouterSrcVlan), props.getProperty(PropertyRouterDstVlan),
                props.getProperty(PropertyRouterMapPort));
    }

}
