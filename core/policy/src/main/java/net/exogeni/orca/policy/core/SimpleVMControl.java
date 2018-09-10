package net.exogeni.orca.policy.core;

import java.util.HashMap;
import java.util.Properties;

import net.exogeni.orca.policy.core.util.IPv4Set;
import net.exogeni.orca.shirako.api.IAuthorityReservation;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitSet;
import net.exogeni.orca.shirako.core.Units;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.NotPersistent;

public class SimpleVMControl extends ResourceControl {
    protected class PoolData {
        protected int total;
        protected int free;
        protected ResourceType type;
        protected ResourcePoolDescriptor pd;

        public PoolData(ResourceType type, Properties properties) throws ConfigurationException {
            this.type = type;
            pd = new ResourcePoolDescriptor();
            pd.reset(properties, null);
        }

        public void addUnits(int count) {
            total += count;
            free += count;
        }

        public void allocate(int count) {
            if (free < count) {
                throw new RuntimeException("insufficient units (allocate): needed=" + count
                        + " available: " + free);
            }
            free -= count;
        }

        public void free(int count) {
            if (free + count > total) {
                throw new RuntimeException("too many units to free");
            }
            free += count;
        }

        public void reserve(int count) {
            if (free < count) {
                throw new RuntimeException("insufficient units (reserve): needed=" + count
                        + " available: " + free);
            }
            free -= count;
        }

        public int getFree() {
            return free;
        }

        public int getTotal() {
            return total;
        }

        public int getAllocated() {
            return total - free;
        }

        public ResourcePoolDescriptor getDescriptor() {
            return pd;
        }

        public ResourceType getType() {
            return type;
        }

    }

    /**
     * The inventory, organized by resource type.
     */
    @NotPersistent
    protected HashMap<ResourceType, PoolData> inventory;
    @NotPersistent
    protected IPv4Set ipset;
    @NotPersistent
    protected String subnet;
    @NotPersistent
    protected String gateway;
    @NotPersistent
    protected String dataSubnet;
    @NotPersistent
    protected boolean useIpSet = false;

    /**
     * Creates a new instance of the control.
     */
    public SimpleVMControl() {
        inventory = new HashMap<ResourceType, PoolData>();
        ipset = new IPv4Set();
    }

    @Override
    public void donate(IClientReservation r) throws Exception {
        ResourceSet set = r.getResources();
        ResourceType type = r.getType();
        Properties resource = set.getResourceProperties();
        Properties local = set.getLocalProperties();

        PoolData pool = inventory.get(type);
        if (pool == null) {
            pool = new PoolData(type, resource);
            pool.addUnits(set.getUnits());
            subnet = local.getProperty(VMControl.PropertyIPSubnet);
            gateway = local.getProperty(VMControl.PropertyIPGateway);
            dataSubnet = local.getProperty(VMControl.PropertyDataSubnet);
            String temp = local.getProperty(VMControl.PropertyIPList);
            if (temp != null) {
                ipset.add(temp);
                useIpSet = true;
            }
            inventory.put(type, pool);
        } else {
            pool.addUnits(set.getUnits());
        }
    }

    public ResourceSet assign(IAuthorityReservation r) throws Exception {
        /*
         * Send back reservations if they have a deficit. For now we want to
         * avoid reservations stuck forever looping between allocation and
         * priming with failures, eventually exhausting the inventory.
         */
        r.setSendWithDeficit(true);

        if (inventory.size() == 0) {
            throw new IllegalStateException("no inventory");
        }

        // get the requested resources
        ResourceSet requested = r.getRequestedResources();
        // request properties
        Properties requestProperties = requested.getRequestProperties();
        // get the requested resource type
        ResourceType type = requested.getType();
        // get the currently assigned resources (if any)
        ResourceSet current = r.getResources();
        // get the ticket
        Ticket ticket = (Ticket) requested.getResources();
        // get the requested lease term
        Term term = r.getRequestedTerm();
        // convert to cycles
        long start = authority.getActorClock().cycle(term.getNewStartTime());
        long end = authority.getActorClock().cycle(term.getEndTime());

        UnitSet gained = null;
        UnitSet lost = null;

        if (current == null) {
            PoolData pool = inventory.get(type);
            if (pool == null) {
                throw new IllegalStateException("no resources of the specified pool");
            }
            int needed = ticket.getUnits();
            gained = getVMs(pool, needed);
            if (gained == null || gained.getUnits() == 0) {
                logger.warn("Could not allocate any units for r: " + r.getReservationID());
                return null;
            }
        } else {
            type = current.getType();
            PoolData pool = inventory.get(type);
            int currentUnits = current.getUnits();
            int difference = ticket.getUnits() - currentUnits;
            if (difference > 0) {
                gained = getVMs(pool, difference);
            } else if (difference < 0) {
                UnitSet uset = (UnitSet) current.getResources();
                String victims = requestProperties.getProperty(ConfigVictims);
                Units toTake = uset.selectExtract(-difference, victims);
                lost = new UnitSet(authority.getShirakoPlugin(), toTake);
            }
        }
        return new ResourceSet(gained, lost, null, type, null);
    }

    protected UnitSet getVMs(PoolData pool, int needed) {
        UnitSet uset = new UnitSet(authority.getShirakoPlugin());
        int available = Math.min(needed, pool.getFree());
        if (useIpSet) {
            available = Math.min(available, ipset.getFreeCount());
        }

        // allocate the units from the pool
        pool.allocate(available);
        logger.debug("Allocated: " + available + " units");
        for (int i = 0; i < available; i++) {
            Unit vm = new Unit();
            // set the resource type
            vm.setResourceType(pool.getType());
            // set the management ip
            if (useIpSet) {
                vm.setProperty(UnitManagementIP, ipset.allocate());
            }
            // set the management subnet mask (if present)
            if (subnet != null) {
                vm.setProperty(UnitManageSubnet, subnet);
            }
            if (dataSubnet != null) {
                vm.setProperty(UnitDataSubnet, dataSubnet);
            }
            if (gateway != null) {
                vm.setProperty(UnitManageGateway, gateway);
            }
            // set each attribute that has a value
            for (ResourcePoolAttributeDescriptor att : pool.getDescriptor().getAttributes()) {
                if (att.getValue() != null) {
                    String key = att.getKey();
                    // if this is a resource attribute convert it
                    // to a unit property
                    key = key.replaceFirst("^resource\\.", "unit.");
                    vm.setProperty(key, att.getValue());
                }
            }
            uset.add(vm);

        }
        return uset;
    }

    protected void free(Units set) throws Exception {
        if (set != null) {
            for (Unit n : set) {
                try {
                    logger.debug("Freeing 1 unit");
                    ResourceType type = n.getResourceType();
                    PoolData pool = inventory.get(type);
                    pool.free(1);
                    if (useIpSet) {
                        ipset.free(n.getProperty(UnitManagementIP));
                    }
                } catch (Exception e) {
                    logger.error("Failed to release vm", e);
                }
            }
        }
    }

    public void revisit(IReservation r) throws Exception {
        UnitSet ng = (UnitSet) (r.getResources().getResources());
        Units ns = ng.getSet();

        for (Unit n : ns) {
            try {
                /*
                 * No need for locking. No other thread is accessing nodes of
                 * recovered reservations. State cannot be Closed, since closed
                 * nodes are filtered in NodeGroup.revisit(Actor).
                 */
                switch (n.getState()) {
                case DEFAULT:
                case FAILED:
                case CLOSING:
                case PRIMING:
                case ACTIVE:
                case MODIFYING:
                    ResourceType type = n.getResourceType();
                    PoolData pool = inventory.get(type);
                    pool.reserve(1);
                    String manIp = n.getProperty(UnitManagementIP);
                    if ( manIp != null)
                    	ipset.reserve(manIp);
                    break;
                case CLOSED:
                    break; // no-op
                }
            } catch (Exception e) {
                fail(n, "revisit with simplevmcontrol", e);
            }
        }
    }
}
