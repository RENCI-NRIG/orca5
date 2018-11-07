package orca.handlers.network.tasks.openflow;

import org.apache.tools.ant.BuildException;

public class AddVlanFlowSpaceTask extends OpenFlowBaseTask {
    protected int tag;
    protected String tagString;
    protected String switchPorts;

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {
            try {
                this.tag = Integer.decode(tagString);
            } catch (NumberFormatException e) {
                throw new Exception("Vlan tag is not numeric: " + tagString);
            }

            device.addVlanFlowSpace(name, dpid, priority, tag, switchPorts);

            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("[OpenFlow.AddIpFlowSpaceTask] An error occurred: " + e.getMessage(), e);
        }
    }

    public void setTag(String tag) {
        tagString = tag;
    }

    public void setSwitchPorts(String ports) {
        switchPorts = ports;
    }
}
