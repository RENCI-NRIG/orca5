package net.exogeni.orca.plugins.ben.broker;

import java.util.Properties;

import net.exogeni.orca.ndl.DomainResource;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;

public class NDLMPInventory extends NDLVlanInventory {

    @Override
    protected void allocate(String sbw, String start, String end, Properties result) {
        if (sbw != null && (start != null || end != null)) {
            long bw = 0;
            try {
                bw = Long.parseLong(sbw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid bandwitdh: " + sbw);
            }

            DomainResource sdr = null, edr = null;
            if (start != null) {
                sdr = ifaces.getResource(start);
                if (sdr == null) {
                    throw new IllegalArgumentException("Interface " + start + " does not exist");
                }
            }
            if (end != null) {
                edr = ifaces.getResource(end);
                if (edr == null) {
                    throw new IllegalArgumentException("Interface " + end + "  does not exist");
                }
            }

            if (sdr != null && edr != null) {
                if (sdr.getBandwidth() >= bw && edr.getBandwidth() >= bw && sdr.getNumLabel() > 0
                        && edr.getNumLabel() > 0) {
                    sdr.reserveBandwidth(bw);
                    edr.reserveBandwidth(bw);
                    sdr.decreaseNumLabel();
                    edr.decreaseNumLabel();
                    if (result != null) {
                        result.setProperty(ResourceProperties.ResourceBandwidth, sbw);
                        result.setProperty(ResourceProperties.ResourceStartIface, start);
                        result.setProperty(ResourceProperties.ResourceEndIface, end);
                    }
                } else {
                    throw new RuntimeException("Insufficient bandwidth to meet request:bw=" + bw + ";sdr bw="
                            + sdr.toString() + ";edr bw=" + edr.toString());
                }
            } else {
                DomainResource dr = sdr;
                if (dr == null) {
                    dr = edr;
                }
                if (dr.getBandwidth() >= bw && dr.getNumLabel() > 0) {
                    dr.reserveBandwidth(bw);
                    dr.decreaseNumLabel();
                    if (result != null) {
                        result.setProperty(ResourceProperties.ResourceBandwidth, sbw);
                        if (dr == sdr) {
                            result.setProperty(ResourceProperties.ResourceStartIface, start);
                        } else {
                            result.setProperty(ResourceProperties.ResourceEndIface, end);
                        }
                    }
                } else {
                    throw new RuntimeException(
                            "Insufficient bandwitdth to meet request:bw=" + bw + ";dr bw=" + dr.toString());
                }
            }
        }
        available--;
    }

    @Override
    public void free(int count, Properties resource) {
        if (count != 1) {
            throw new IllegalStateException("Count can only be 1");
        }

        String sbw = resource.getProperty(ResourceProperties.ResourceBandwidth);
        String start = resource.getProperty(ResourceProperties.ResourceStartIface);
        String end = resource.getProperty(ResourceProperties.ResourceEndIface);

        if (sbw != null && (start != null || end != null)) {
            long bw = 0;
            try {
                bw = Long.parseLong(sbw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid bandwitdh: " + sbw);
            }

            DomainResource sdr = null;
            if (start != null) {
                sdr = ifaces.getResource(start);
                if (sdr == null) {
                    throw new IllegalStateException("Interface " + start + " does not exist");
                }
            }
            DomainResource edr = null;
            if (end != null) {
                edr = ifaces.getResource(end);
                if (edr == null) {
                    throw new IllegalStateException("Interface " + end + "  does not exist");
                }
            }

            if (sdr != null) {
                sdr.releaseBandwidth(bw);
                sdr.increaseNumLabel();
            }
            if (edr != null) {
                edr.releaseBandwidth(bw);
                edr.increaseNumLabel();
            }
        }
        available++;
    }

}
