package orca.handlers.network;

import orca.handlers.network.router.Cisco6509Device;

public class Cisco6509DeviceTest extends NetworkDeviceTest {
    public static final String PropertyAdminPassword = "router.6509.adminpassword";
    public static final String PropertyUser = "router.6509.user";
    public static final String PropertyPassword = "router.6509.password";
    public static final String PropertyRouter = "router.6509";
    public static final String PropertyVlanTag = "vlan.tag";
    public static final String PropertyPorts = "router.6509.ports";
    public static final String PropertyRouterSrcVlan = "router.6509.src.vlan.tag";
    public static final String PropertyRouterDstVlan = "router.6509.dst.vlan.tag";
    public static final String PropertyRouterMapPort = "router.6509.map.port";
    public static final String PropertyDefaultPrompt = "router.default.prompt";


    protected Cisco6509Device getDevice() {
        Cisco6509Device device = new Cisco6509Device(props.getProperty(PropertyRouter), props.getProperty(PropertyUser), props.getProperty(PropertyPassword), props.getProperty(PropertyAdminPassword), props.getProperty(PropertyDefaultPrompt));
        configureDevice(device);
        return device;
    }

    public void testCreateVLAN() throws Exception {
        Cisco6509Device device = getDevice();
        device.createVLAN(props.getProperty(PropertyVlanTag), "500000", "50000");
    }

    public void testDeleteVLAN() throws Exception {
        Cisco6509Device device = getDevice();
        device.deleteVLAN(props.getProperty(PropertyVlanTag), false);
    }

    public void testMapVLAN() throws Exception {
        Cisco6509Device device = getDevice();
        device.mapVLANs(props.getProperty(PropertyRouterSrcVlan), props.getProperty(PropertyRouterDstVlan), props.getProperty(PropertyRouterMapPort));
    }

    public void testUnmapVLAN() throws Exception {
        Cisco6509Device device = getDevice();
        device.unmapVLANs(props.getProperty(PropertyRouterSrcVlan), props.getProperty(PropertyRouterDstVlan), props.getProperty(PropertyRouterMapPort));
    }

}
