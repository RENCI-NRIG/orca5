package orca.handlers.network.tasks.QFX3500;

import org.apache.tools.ant.BuildException;

public class DeleteVLANTask extends QFX3500BaseTask {
    protected String vlanTag;
    protected String withQoS;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (vlanTag == null) {
                throw new Exception("Missing vlan name");
            }

            if (withQoS.equals("true"))
                router.deleteVLAN(vlanTag, true);
            else
                router.deleteVLAN(vlanTag, false);
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

    public void setVlanWithQoS(String withQoS) {
        this.withQoS = withQoS.toLowerCase();
    }
}
