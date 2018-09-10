package net.exogeni.orca.handlers.network;

import net.exogeni.orca.handlers.network.openflow.FlowVisorDevice;

public class FlowVisorDeviceTest extends NetworkDeviceTest {

    public static final String PropertyFlowVisorUrl = "flowvisor.url";
    public static final String PropertyFlowVisorUser = "flowvisor.user";
    public static final String PropertyFlowVisorPasswd = "flowvisor.passwd";

    public static final String PropertyFlowVisorSliceName = "flowvisor.slice.name";
    public static final String PropertyFlowVisorSlicePasswd = "flowvisor.slice.passwd";
    public static final String PropertyFlowVisorSliceController = "flowvisor.slice.controller";
    public static final String PropertyFlowVisorSliceEmail = "flowvisor.slice.email";

    public static final String PropertyFlowVisorFlowSpaceSrcIP = "flowvisor.slice.flowspace.src.ip";
    public static final String PropertyFlowVisorFlowSpaceDstIP = "flowvisor.slice.flowspace.dst.ip";

    protected FlowVisorDevice getDevice() {
        FlowVisorDevice device = new FlowVisorDevice(props.getProperty(PropertyFlowVisorUrl),
                props.getProperty(PropertyFlowVisorUser), props.getProperty(PropertyFlowVisorPasswd));
        configureDevice(device);
        device.enableEmulation();
        return device;
    }

    public void testCreateSlice() throws Exception {
        FlowVisorDevice device = getDevice();

        String name = props.getProperty(PropertyFlowVisorSliceName);
        String passwd = props.getProperty(PropertyFlowVisorSlicePasswd);
        String controller = props.getProperty(PropertyFlowVisorSliceController);
        String email = props.getProperty(PropertyFlowVisorSliceEmail);
        device.createSlice(name, passwd, controller, email);
    }

    public void AddFlowSpace() throws Exception {
        FlowVisorDevice device = getDevice();

        String name = props.getProperty(PropertyFlowVisorSliceName);
        String srcIP = props.getProperty(PropertyFlowVisorFlowSpaceSrcIP);
        String dstIP = props.getProperty(PropertyFlowVisorFlowSpaceDstIP);
        device.addIPFlowSpace(name, "any", "0", srcIP, dstIP);
    }

    public void testDeleteSlice() throws Exception {
        FlowVisorDevice device = getDevice();

        String name = props.getProperty(PropertyFlowVisorSliceName);
        device.deleteSlice(name);
    }
}
