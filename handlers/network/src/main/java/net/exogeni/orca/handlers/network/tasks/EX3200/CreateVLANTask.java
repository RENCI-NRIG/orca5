package net.exogeni.orca.handlers.network.tasks.EX3200;

import org.apache.tools.ant.BuildException;

public class CreateVLANTask extends EX3200BaseTask {
    protected String vlanTag;
    protected String qosRate = "";
    protected String qosBurstSize;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (vlanTag == null) {
                throw new Exception("Missing vlan tag property");
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
