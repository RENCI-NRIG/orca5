package orca.handlers.network;

import orca.handlers.network.router.Ciena8700Device;

public class Ciena8700DeviceTest extends NetworkDeviceTest {
    public static final String PropertyAdminPassword = "router.8700.adminpassword";
    public static final String PropertyUser = "router.8700.user";
    public static final String PropertyPassword = "router.8700.password";
    public static final String PropertyRouter = "router.8700";
    public static final String PropertyVlanTag = "vlan.tag";
    public static final String PropertyPorts = "router.8700.ports";
    public static final String PropertyRouterSrcVlan = "router.8700.src.vlan.tag";
    public static final String PropertyRouterDstVlan = "router.8700.dst.vlan.tag";
    public static final String PropertyRouterMapPort = "router.8700.map.port";


    protected Ciena8700Device getDevice() {
        Ciena8700Device device = new Ciena8700Device(props.getProperty(PropertyRouter), props.getProperty(PropertyUser), props.getProperty(PropertyPassword));
        configureDevice(device);
        return device;
    }

    public void testCreateVLAN() throws Exception {
        Ciena8700Device device = getDevice();
        device.createVLAN(props.getProperty(PropertyVlanTag), "500000", "50000");
    }

    public void testDeleteVLAN() throws Exception {
        Ciena8700Device device = getDevice();
        device.deleteVLAN(props.getProperty(PropertyVlanTag), false);
    }

    public void testMapVLAN() throws Exception {
        Ciena8700Device device = getDevice();
        device.mapVLANs(props.getProperty(PropertyRouterSrcVlan), props.getProperty(PropertyRouterDstVlan), props.getProperty(PropertyRouterMapPort));
    }

    public void testUnmapVLAN() throws Exception {
        Ciena8700Device device = getDevice();
        device.unmapVLANs(props.getProperty(PropertyRouterSrcVlan), props.getProperty(PropertyRouterDstVlan), props.getProperty(PropertyRouterMapPort));
    }

}
