package orca.handlers.network.tasks.Cisco3400;

import org.apache.tools.ant.BuildException;

public class CreateVLANTask extends Cisco3400BaseTask {
    protected String vlanTag;
    protected String qosRate = "";
    protected String qosBurstSize;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (vlanTag == null) {
                throw new Exception("Missing vlan tag");
            }

            router.createVLAN(vlanTag, qosRate, qosBurstSize);
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

    public void setVlanQosRate(String qosRate) {
        this.qosRate = qosRate;
    }

    public void setVlanQoSBurstSize(String qosBurstSize) {
        this.qosBurstSize = qosBurstSize;
    }
}
