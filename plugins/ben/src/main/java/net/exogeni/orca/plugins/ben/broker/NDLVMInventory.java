package net.exogeni.orca.plugins.ben.broker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.DataFormatException;

import net.exogeni.orca.embed.workflow.Domain;
import net.exogeni.orca.ndl.DomainResource;
import net.exogeni.orca.ndl.DomainResources;
import net.exogeni.orca.ndl.NdlException;
import net.exogeni.orca.policy.core.util.SimplerUnitsInventory;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.common.meta.RequestProperties;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.util.CompressEncode;

public class NDLVMInventory extends SimplerUnitsInventory {
    protected int available;
    protected int total;
    protected DomainResources resourceConstraint;
    protected String[] con_key = { "numCPUCores", "memoryCapacity", "storageCapacity" };
    protected String[] rp = { ResourceProperties.ResourceNumCPUCores, ResourceProperties.ResourceMemoryCapacity,
            ResourceProperties.ResourceStorageCapacity };

    public NDLVMInventory() {
        // TODO Auto-generated constructor stub
    }

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
        // add the inventory one unit at a time
        for (int i = 1; i <= ticket.getUnits(); i++) {
            set.addInventory(new Integer(i));
        }

        // logger.info("Available Resource delegated to broker:"+available);

        // To deal with the case when the units are partially donated to one of a few brokers, need to change the
        // abstract RDF
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
            resourceConstraint = domain.getDomainResources(model, ticket.getUnits());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NdlException ee) {
            ee.printStackTrace();
        } catch (RuntimeException eee) {
            eee.printStackTrace();
            available = total = 0;
            set.free(ticket.getUnits());
        }
    }

    protected void allocate(HashMap<String, String> constraints, Properties result) {
        int i = 0;
        String key = null;
        long value = 0;
        DomainResource c = null;

        for (Entry<String, String> con : constraints.entrySet()) {
            try {
                key = con.getKey();
                value = Long.parseLong(con.getValue());
                c = resourceConstraint.getResource(key);
                i = 0;
                if (c != null) {
                    if (c.getBandwidth() >= value) {
                        c.reserveBandwidth(value);
                        for (String key_str : con_key) {
                            if (key.equals(key_str))
                                break;
                            i++;
                        }
                        if (result != null) {
                            result.setProperty(rp[i], con.getValue());
                        }
                    } else {
                        throw new RuntimeException(
                                "Insufficient <" + key + "," + c.getBandwidth() + "> to meet request:" + value);
                    }
                }
                i++;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid constraint: " + con_key[i] + "=" + con);
            }

        }
        available--;
        // System.out.println("Available Resource after an allocation:"+available);
    }

    public Properties allocate(int count, Properties request) {
        if (count != 1) {
            throw new IllegalArgumentException("Count can only be 1");
        }
        if (available <= 0) {
            throw new IllegalStateException("No available units");
        }
        // for(Entry entry:request.entrySet()){
        // System.out.println("r_p key="+entry.getKey()+";value="+entry.getValue());
        // }
        Properties result = new Properties();
        String numCores = request.getProperty(RequestProperties.RequestNumCPUCores);
        String memory = request.getProperty(RequestProperties.RequestMemoryCapacity);
        String storage = request.getProperty(RequestProperties.RequestStorageCapacity);
        HashMap<String, String> constraints = new HashMap<String, String>();
        if (numCores != null)
            constraints.put(con_key[0], numCores);
        if (memory != null)
            constraints.put(con_key[1], memory);
        if (storage != null)
            constraints.put(con_key[2], storage);

        allocate(constraints, result);
        return result;
    }

    public Properties allocate(int count, Properties request, Properties resource) {
        throw new IllegalStateException("Extends with increase are not valid for VLANS");
    }

    // called at recovery
    public void allocateRevisit(int count, Properties resource) {
        if (count != 1) {
            throw new IllegalStateException("Count can only be 1");
        }

        if (available <= 0) {
            throw new IllegalStateException("No available units");
        }

        String numCores = resource.getProperty(ResourceProperties.ResourceNumCPUCores);
        String memory = resource.getProperty(ResourceProperties.ResourceMemoryCapacity);
        String storage = resource.getProperty(ResourceProperties.ResourceStorageCapacity);

        HashMap<String, String> constraints = new HashMap<String, String>();
        if (numCores != null)
            constraints.put(con_key[0], numCores);
        if (memory != null)
            constraints.put(con_key[1], memory);
        if (storage != null)
            constraints.put(con_key[2], storage);
        allocate(constraints, null);
    }

    public void free(int count, Properties resource) {
        if (count != 1) {
            throw new IllegalStateException("Count can only be 1");
        }

        String numCores = resource.getProperty(ResourceProperties.ResourceNumCPUCores);
        String memory = resource.getProperty(ResourceProperties.ResourceMemoryCapacity);
        String storage = resource.getProperty(ResourceProperties.ResourceStorageCapacity);

        if (numCores != null) {
            long value = 0;
            DomainResource c = null;
            try {
                value = Long.parseLong(numCores);
                c = resourceConstraint.getResource("numCPUCores");
                c.releaseBandwidth(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid cpu cores: " + numCores);
            }
        }

        if (memory != null) {
            long value = 0;
            DomainResource c = null;
            try {
                value = Long.parseLong(memory);
                c = resourceConstraint.getResource("memoryCapacity");
                c.releaseBandwidth(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid memory: " + memory);
            }
        }

        if (storage != null) {
            long value = 0;
            DomainResource c = null;
            try {
                value = Long.parseLong(storage);
                c = resourceConstraint.getResource("storageCapacity");
                c.releaseBandwidth(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid storage: " + storage);
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
