package net.exogeni.orca.plugins.ben.broker;

import java.io.IOException;
import java.util.Properties;
import java.util.zip.DataFormatException;

import net.exogeni.orca.embed.workflow.Domain;
import net.exogeni.orca.ndl.DomainResource;
import net.exogeni.orca.ndl.DomainResources;
import net.exogeni.orca.ndl.NdlException;
import net.exogeni.orca.policy.core.util.InventoryForType;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.common.meta.RequestProperties;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.util.CompressEncode;

public class NDLVlanInventory extends InventoryForType {
    protected int available;
    protected int total;
    protected DomainResources ifaces;

    @Override
    public void donate(IClientReservation source) {
        super.donate(source);
        // resource set
        ResourceSet rset = source.getResources();
        // ticket
        Ticket cset = (Ticket) rset.getResources();
        // resource ticket
        ResourceTicket ticket = cset.getTicket();
        available = total = ticket.getUnits();

        ResourcePoolAttributeDescriptor attr = rpd.getAttribute(ResourceProperties.ResourceNdlAbstractDomain);
        if (attr == null) {
            throw new RuntimeException("Missing abstract NDL model");
        }

        String model = null;
        // extract the domain resources
        try {
            model = CompressEncode.decodeDecompress(attr.getValue());
        } catch (DataFormatException dfe) {
            // maybe it is not compressed so just keep going
            model = attr.getValue();
        }

        try {
            Domain domain = new Domain();
            ifaces = domain.getDomainResources(model, total);
            model = domain.getDomain_model_str();
            if (model != null) {
                attr.setValue(CompressEncode.compressEncode(model));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NdlException ee) {
            ee.printStackTrace();
        } catch (RuntimeException eee) {
            eee.printStackTrace();
            available = total = 0;
            ticket.getDelegation().setUnits(0);
        }
        // note: we ignore the type and count embedded in ifaces since
        // these are passed by orca's core
    }

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
                if (sdr.getBandwidth() >= bw && edr.getBandwidth() >= bw) {
                    sdr.reserveBandwidth(bw);
                    edr.reserveBandwidth(bw);
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
                if (dr.getBandwidth() >= bw) {
                    dr.reserveBandwidth(bw);
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

    public Properties allocate(int count, Properties request) {
        if (count != 1) {
            throw new IllegalArgumentException("Count can only be 1");
        }
        if (available <= 0) {
            throw new IllegalStateException("No available units");
        }

        Properties result = new Properties();
        String bw = request.getProperty(RequestProperties.RequestBandwidth);
        String start = request.getProperty(RequestProperties.RequestStartIface);
        String end = request.getProperty(RequestProperties.RequestEndIface);

        allocate(bw, start, end, result);
        return result;
    }

    public Properties allocate(int count, Properties request, Properties resource) {
        throw new IllegalStateException("Extends with increase are not valid for VLANS");
    }

    public void allocateRevisit(int count, Properties resource) {
        if (count != 1) {
            throw new IllegalStateException("Count can only be 1");
        }

        if (available <= 0) {
            throw new IllegalStateException("No available units");
        }

        String bw = resource.getProperty(ResourceProperties.ResourceBandwidth);
        String start = resource.getProperty(ResourceProperties.ResourceStartIface);
        String end = resource.getProperty(ResourceProperties.ResourceEndIface);

        allocate(bw, start, end, null);
    }

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
            }
            if (edr != null) {
                edr.releaseBandwidth(bw);
            }
        }
        available++;
    }

    public Properties free(int count, Properties request, Properties resource) {
        throw new IllegalStateException("Extends with decrease are not valid for VLANS");
    }

    public int getFree() {
        return available;
    }

    public int getAllocated() {
        return total - available;
    }
}
