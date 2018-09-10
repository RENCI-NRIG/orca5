package net.exogeni.orca.policy.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import net.exogeni.orca.policy.core.util.IPv4Set;
import net.exogeni.orca.policy.core.util.Vmm;
import net.exogeni.orca.policy.core.util.VmmPool;
import net.exogeni.orca.shirako.api.IAuthorityReservation;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.UnitID;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitSet;
import net.exogeni.orca.shirako.core.Units;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.NotPersistent;

public class VMControl extends ResourceControl {
    public static final String PropertyCapacity = "capacity";
    public static final String PropertyIPList = "ip.list";
    public static final String PropertyIPSubnet = "ip.subnet";
    public static final String PropertyIPGateway = "ip.gateway";
    public static final String PropertyDataSubnet = "data.subnet";

    /**
     * The inventory, organized by resource type.
     */
    @NotPersistent
    protected HashMap<ResourceType, VmmPool> inventory;
    @NotPersistent
    protected IPv4Set ipset;
    @NotPersistent
    protected String subnet;
    @NotPersistent
    protected String gateway;
    @NotPersistent
    protected boolean useIpSet = false;

    /**
     * Creates a new instance of the control.
     */
    public VMControl() {
        inventory = new HashMap<ResourceType, VmmPool>();
        ipset = new IPv4Set();
    }

    /**
     * {@inheritDoc}
     */
    public void donate(IClientReservation r) throws Exception {
        // since this control deals with physical, not logical resources
        // this operation is a no-op.
    }

    /**
     * {@inheritDoc}
     */
    public void donate(ResourceSet set) throws Exception {
        ResourceType type = set.getType();
        Properties resource = set.getResourceProperties();
        Properties local = set.getLocalProperties();

        VmmPool pool = inventory.get(type);
        if (pool == null) {
            pool = new VmmPool(type, resource);
            ResourcePoolDescriptor rd = new ResourcePoolDescriptor();
            rd.reset(resource, null);
            int memory = Integer.parseInt(rd.getAttribute(ResourceMemory).getValue());
            int capacity = PropList.getRequiredIntegerProperty(local, PropertyCapacity);
            subnet = local.getProperty(PropertyIPSubnet);
            gateway = local.getProperty(PropertyIPGateway);
            String temp = local.getProperty(PropertyIPList);
            if (temp != null) {
                ipset.add(temp);
                useIpSet = true;
            }
            pool.setMemory(memory);
            pool.setCapacity(capacity);
            inventory.put(type, pool);
        } else {
            // FIXME: how should we treat the new properties
        }

        UnitSet ng = (UnitSet) set.getResources();
        for (Unit host : ng.getSet()) {
            Vmm vmm = new Vmm(host, pool.getCapacity());
            pool.donate(vmm);
        }
    }

    /**
     * {@inheritDoc}
     */
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
            VmmPool pool = inventory.get(type);
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
            VmmPool pool = inventory.get(type);
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

    protected UnitSet getVMs(VmmPool pool, int needed) {
        UnitSet uset = new UnitSet(authority.getShirakoPlugin());
        Collection<Vmm> vmms = pool.getVmmSet();
        int allocated = 0;
        for (Vmm vmm : vmms) {
            // how many can we take from this machine?
            int available = vmm.getAvailable();
            if (useIpSet) {
                available = Math.min(available, ipset.getFreeCount());
            }

            if (available > 0) {
                // how many do we still need
                int toAllocate = Math.min(available, needed - allocated);
                for (int i = 0; i < toAllocate; i++) {
                    Unit vm = new Unit();
                    vm.setResourceType(pool.getType());
                    // attach the parent information, so that the handler knows
                    // where to create this VM.
                    vm.setParentID(vmm.getHost().getID());
                    vm.setProperty(UnitParentHostName, vmm.getHost().getProperty(UnitHostName));
                    vm.setProperty(UnitControl, vmm.getHost().getProperty(UnitControl));
                    // set the unit properties
                    vm.setProperty(UnitMemory, Integer.toString(pool.getMemory()));
                    if (useIpSet) {
                        vm.setProperty(UnitManagementIP, ipset.allocate());
                    }
                    if (subnet != null) {
                        vm.setProperty(UnitManageSubnet, subnet);
                    }
                    if (gateway != null) {
                        vm.setProperty(UnitManageGateway, gateway);
                    }
                    vmm.host(vm);
                    uset.add(vm);
                }
                allocated += toAllocate;
            }
            if (allocated == needed || ipset.getFreeCount() == 0) {
                break;
            }
        }
        return uset;
    }

    protected void free(Units set) throws Exception {
        if (set != null) {
            for (Unit n : set) {
                try {
                    ResourceType type = n.getResourceType();
                    VmmPool pool = inventory.get(type);
                    UnitID host = n.getParentID();
                    Vmm vmm = pool.getVmm(host);
                    vmm.release(n);
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
                    VmmPool pool = inventory.get(type);
                    UnitID id = n.getParentID();
                    Vmm vmm = pool.getVmm(id);
                    vmm.host(n);
                    logger.debug("VMControl.revisit(); recovering management IP " + n.getProperty(UnitManagementIP));
                    ipset.reserve(n.getProperty(UnitManagementIP));
                    break;
                case CLOSED:
                    break; // no-op
                }
            } catch (Exception e) {
                fail(n, "revisit with vmcontrol", e);
            }
        }
    }
    
	@Override
	public void recoveryStarting() {
		logger.info("Beginning VMControl recovery");
	}

	@Override
	public void recoveryEnded() {
		logger.info("Completing VMControl recovery");
		logger.debug("Restored VMControl with subnet " + subnet + " gateway " + gateway + " ipset " + ipset);
		logger.debug("and inventory " + inventory);
	}
}
