package net.exogeni.orca.policy.core.util;

import java.util.HashMap;
import java.util.Properties;

import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.common.meta.QueryProperties;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeType;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.util.Misc;
import net.exogeni.orca.util.PropList;

public class Inventory {
    /**
     * Maps resource type to inventory for that type.
     */
    protected HashMap<String, InventoryForType> map;

    public Inventory() {
        map = new HashMap<String, InventoryForType>();
    }

    public boolean containsType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return map.containsKey(type);
    }

    public InventoryForType get(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return map.get(type);
    }
    
    /**
     * Removes the inventory derived from the specified source. 
     * @param source source reservation
     * @return true if the inventory was update, false otherwise
     */
    public boolean remove(final IClientReservation source) {
        String type = source.getType().toString();        
        InventoryForType inv = map.get(type);
        if (inv != null) {
            if (inv.source == source) {
                InventoryForType iv2 = map.remove(type);
                return true;
            }
        }
        return false;
    }

    public InventoryForType getNew(final IClientReservation r) throws ConfigurationException {
        if (r == null) {
            throw new IllegalArgumentException("r cannot be null");
        }

        // resource type
        String type = r.getType().toString();
        if (map.containsKey(type.toString())) {
            throw new IllegalStateException("There is already inventory for type: " + type.toString());
        }

        Properties p = new Properties();
        // resource set
        ResourceSet rset = r.getResources();
        // ticket
        Ticket cset = (Ticket) rset.getResources();
        // resource ticket
        ResourceTicket ticket = cset.getTicket();

        // absorb the properties from the resource ticket (signed properties)
        PropList.mergeProperties(ticket.getProperties(), p);
        // absorb the resource properties (unsigned)
        PropList.mergeProperties(rset.getResourceProperties(), p);
        // create the resource pool descriptor
        ResourcePoolDescriptor rpd = new ResourcePoolDescriptor();
        rpd.reset(p, null);
        // determine the class for the inventory for this type
        ResourcePoolAttributeDescriptor desc = rpd.getAttribute(net.exogeni.orca.shirako.common.meta.ResourceProperties.ResourceClassInventoryForType);
        InventoryForType inv = null;
        if (desc != null) {
            try {
                System.out.println("Creating InventoryForType: class=" + desc.getValue());
                inv = (InventoryForType) Misc.createInstance(desc.getValue());
            } catch (Exception e) {
                throw new ConfigurationException("Cannot instantiate inventory class: " + desc.getValue(), e);
            }
        } else {
            // use the default
            inv = new SimplerUnitsInventory();
        }

        // set the type
        inv.setType(r.getType());
        // set the descriptor
        inv.setDescriptor(rpd);
        // donate the source reservation to the inventory and let it populate itself
        inv.donate(r);

        // register the new inventory
        map.put(type, inv);
        return inv;
    }

    public HashMap<String, InventoryForType> getInventory() {
        return map;
    }

    public Properties getResourcePools() {
        int count = 0;
        Properties result = new Properties();
        for (InventoryForType inv : map.values()) {
            // make a clone of the pool descriptor
            ResourcePoolDescriptor rpd = inv.getDescriptor().clone();
            // add the currently available units
            ResourcePoolAttributeDescriptor attr = new ResourcePoolAttributeDescriptor();
            attr.setType(ResourcePoolAttributeType.INTEGER);
            attr.setKey(ResourceProperties.ResourceAvailableUnits);
            attr.setValue(Integer.toString(inv.getFree()));
            attr.setLabel("Available units at time of query");
            rpd.addAttribute(attr);                    
            rpd.save(result, QueryProperties.PoolPrefix + count + ".");
            count++;
        }
        PropList.setProperty(result, QueryProperties.PoolsCount, map.size());
        return result;
    }
}
