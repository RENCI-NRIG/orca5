package orca.policy.core.util;

import java.util.Properties;

import orca.shirako.api.IClientReservation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.util.FreeAllocatedSet;

public class SimplerUnitsInventory extends InventoryForType {
    protected FreeAllocatedSet<Integer> set;

    public SimplerUnitsInventory() {
        set = new FreeAllocatedSet<Integer>();
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

        // add the inventory one unit at a time
        for (int i = 1; i <= ticket.getUnits(); i++) {
            set.addInventory(new Integer(i));
        }
    }

    public Properties allocate(int count, Properties request) {
        // note: we do not keep track of the identity of the allocated items
        set.allocate(count);
        return new Properties();
    }

    public Properties allocate(int count, Properties request, Properties resource) {
        // note: we do not keep track of the identity of the allocated items
        set.allocate(count);
        return new Properties();
    }
    
    public void allocateRevisit(int count, Properties resource) {
        // note: we do not keep track of the identity of the allocated items
        set.allocate(count);
    }
    
    public Properties free(int count, Properties request, Properties resource) {
        // note: we do not keep track of the identify of the allocated/freed items
        set.free(count);
        return new Properties();
    }
    
    public void free(int count, Properties resource) {
        set.free(count);
    }
    
    public int getFree() {
        return set.getFree();
    }
    
    public int getAllocated() {
        return set.getAllocated();
    }
}
