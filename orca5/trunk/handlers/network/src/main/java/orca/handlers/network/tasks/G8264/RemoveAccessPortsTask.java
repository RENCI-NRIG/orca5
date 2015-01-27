package orca.handlers.network.tasks.G8264;

import org.apache.tools.ant.BuildException;

public class RemoveAccessPortsTask extends G8264BaseTask {
    protected String vlanTag;
    protected String ports;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (vlanTag == null) {
                throw new Exception("Missing vlan tag");
            }

            if (ports == null) {
                throw new Exception("Missing ports");
            }
            router.removeAccessPortsFromVLAN(vlanTag, ports);
            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setVlanTag(String value) {
        this.vlanTag = value;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }
}
