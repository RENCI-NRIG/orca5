package net.exogeni.orca.handlers.network.tasks.QFX3500;

import org.apache.tools.ant.BuildException;

public class CreateVLANTask extends QFX3500BaseTask {
    protected String vlanTag;
    protected String qosRate = "";
    protected String qosBurstSize;

    protected Long maxBurstSize = 268435456L;
    protected Long minBurstSize = 512L;

    protected Long maxBandwidth = 50000000000L;
    protected Long minBandwidth = 8000L;

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

    public void setVlanQosRate(String qr) {
        Long rate = 0L;
        try {
            rate = Long.parseLong(qr);
        } catch (NumberFormatException e) {
            throw new BuildException("Error parsing QoS rate: " + qr);
        }
        if (rate > maxBandwidth)
            rate = maxBandwidth;
        if (rate < minBandwidth)
            rate = minBandwidth;
        this.qosRate = "" + rate;
    }

    public void setVlanQoSBurstSize(String qbs) {
        Long bs = 0L;
        try {
            bs = Long.parseLong(qbs);
        } catch (NumberFormatException e) {
            throw new BuildException("Error parsing QoS burst size: " + qbs);
        }
        if (bs > maxBurstSize)
            bs = maxBurstSize;
        if (bs < minBurstSize)
            bs = minBurstSize;
        this.qosBurstSize = "" + bs;
    }
}
