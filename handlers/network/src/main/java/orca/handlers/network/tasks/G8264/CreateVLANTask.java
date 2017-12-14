package orca.handlers.network.tasks.G8264;

import org.apache.tools.ant.BuildException;

public class CreateVLANTask extends G8264BaseTask {
    protected String vlanTag;
    protected String qosRate = "";
    protected String qosBurstSize;

    // in kbps
    protected Long maxBandwidth = 40000000L;
    protected Long minBandwidth = 64L;

    private static Long[] burstSizes = { 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L };

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

    /**
     * Needs to convert to kbps in multiples of 64kbps
     * 
     * @param qr
     */
    public void setVlanQosRate(String qr) {
        // for when qos is off
        if ("0".equals(qr.trim())) {
            this.qosRate = qr;
            return;
        }
        Long rate = 0L;
        try {
            rate = Long.parseLong(qr) / 1000L;
        } catch (NumberFormatException e) {
            throw new BuildException("Error parsing QoS rate: " + qr);
        }
        if (rate > maxBandwidth)
            rate = maxBandwidth;
        if (rate < minBandwidth)
            rate = minBandwidth;

        Double dRate = Math.ceil(rate / 64.0) * 64L;

        this.qosRate = "" + dRate.intValue();
    }

    /**
     * Needs to convert to one of the available values (and in kbps)
     * 
     * @param qbs
     */
    public void setVlanQoSBurstSize(String qbs) {
        Long bs = 0L;
        try {
            bs = Long.parseLong(qbs) / 1000L;
        } catch (NumberFormatException e) {
            throw new BuildException("Error parsing QoS burst size: " + qbs);
        }

        // find closest burst size
        if (bs < burstSizes[0])
            bs = burstSizes[0];
        if (bs > burstSizes[burstSizes.length - 1])
            bs = burstSizes[burstSizes.length - 1];

        Long power = Math.round(Math.log(bs) / Math.log(2));

        this.qosBurstSize = "" + (1 << power.intValue());
    }
}
