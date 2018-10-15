package net.exogeni.orca.handlers.network;

import net.exogeni.orca.handlers.network.router.QFX3500RouterDevice;

public class QFX3500DeviceTest extends NetworkDeviceTest {
    public static final String PropertyUser = "router.qfx3500.user";
    public static final String PropertyPassword = "router.qfx3500.password";
    public static final String PropertyRouter = "router.qfx3500";
    public static final String PropertyVlanTag = "vlan.tag";
    public static final String PropertyPorts = "router.qfx3500.ports";
    public static final String PropertyRouterSrcVlan = "router.qfx3500.src.vlan.tag";
    public static final String PropertyRouterDstVlan = "router.qfx3500.dst.vlan.tag";
    public static final String PropertyRouterMapPort = "router.qfx3500.map.port";

    protected QFX3500RouterDevice getDevice() {
        QFX3500RouterDevice device = new QFX3500RouterDevice(props.getProperty(PropertyRouter),
                props.getProperty(PropertyUser), props.getProperty(PropertyPassword));
        configureDevice(device);
        return device;
    }

    public void testCreateVLAN() throws Exception {
        QFX3500RouterDevice device = getDevice();
        device.createVLAN(props.getProperty(PropertyVlanTag), "3000000000", "5000000");
    }

    public void testDeleteVLAN() throws Exception {
        QFX3500RouterDevice device = getDevice();
        device.deleteVLAN(props.getProperty(PropertyVlanTag), false);
    }

}
