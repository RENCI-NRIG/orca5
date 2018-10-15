package net.exogeni.orca.policy.core;

import java.util.Iterator;
import java.util.Properties;

import net.exogeni.orca.shirako.api.IAuthorityReservation;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitSet;
import net.exogeni.orca.shirako.core.Units;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.FreeAllocatedSet;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.Persistent;

public class LUNControl extends ResourceControl {
    public static final String PropertyStartLUN = "lun.tag.start";
    public static final String PropertyEndLUN = "lun.tag.end";

    /**
     * Set of available and allocated tag.
     */
    @NotPersistent
    protected FreeAllocatedSet<Integer> tags;
    /**
     * Resource type.
     */
    @Persistent
    protected ResourceType type;

    /**
     * Creates a new instance.
     */
    public LUNControl() {
        tags = new FreeAllocatedSet<Integer>();
    }

    @Override
    public void donate(IClientReservation r) throws Exception {
        if (tags.size() != 0) {
            throw new Exception("only a single source reservation is supported");
        }

        ResourceSet set = r.getResources();
        ResourceType rtype = r.getType();
        Properties p = set.getLocalProperties(); // note: local properties
        if (p == null) {
            throw new Exception("Missing local properties");
        }
        // note: ignoring term
        type = rtype;
        int start = 0, end = 0, i = 0, size = 0;
        String startP, endP;
        while (true) {
            i++;
            startP = PropertyStartLUN + String.valueOf(i);
            endP = PropertyEndLUN + String.valueOf(i);
            start = PropList.getRequiredIntegerProperty(p, startP);
            end = PropList.getRequiredIntegerProperty(p, endP);
            if ((start == 0) && (end == 0)) {
                break;
            }
            for (int j = start; j <= end; j++) {
                tags.addInventory(new Integer(j));
            }
            size = size + (end - start + 1);
            logger.info("Tag Donation:" + type + ":" + start + "-" + end + ":" + size);
            // FIXME: why not use a property to say how many blocks?
            if (i > 20) { // just in case, jump out after 20
                break;
            }
        }
        if (size < r.getUnits()) {
            throw new Exception("Insufficient lun tags specified in donated reservation: donated "
                    + size + " rset says: " + r.getUnits());
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

        if (tags.size() == 0) {
            throw new Exception("no inventory");
        }

        // get the requested resources
        ResourceSet requested = r.getRequestedResources();
        // get the requested resource type
        ResourceType type = requested.getType();

        // get the currently assigned resources (if any)
        ResourceSet current = r.getResources();
        // get the ticket
        Ticket ticket = (Ticket) requested.getResources();

        ResourceTicket rt = ticket.getTicket();
        Properties ticketProperties = rt.getProperties();

        // get the requested lease term
        Term term = r.getRequestedTerm();

        // convert to cycles
        long start = authority.getActorClock().cycle(term.getNewStartTime());
        long end = authority.getActorClock().cycle(term.getEndTime());
        Properties configuration_properties = requested.getConfigurationProperties();

        // determine if this is a new lease or an extension request
        if (current == null) {
            // this is a new request
            // how much does the client want?
            long needed = ticket.getUnits();
            if (needed > 1) {
                r.fail("Cannot assign more than 1 LUN per reservation");
            }

            if (tags.getFree() > 0) {

                Integer tag = tags.allocate();

                ResourceData rd = new ResourceData();
                PropList.setProperty(rd.getResourceProperties(), UnitLUNTag, tag);
                UnitSet gained = new UnitSet(authority.getShirakoPlugin());
                Unit u = new Unit();
                u.setResourceType(type);
                u.setProperty(UnitLUNTag, tag.toString());
                // if the broker allocated specific bandwidth, set it as a
                // property on the unit
                if (ticketProperties.containsKey(ResourceProperties.ResourceStorageCapacity)) {

                    long capacity = Long.parseLong(ticketProperties.getProperty(ResourceProperties.ResourceStorageCapacity));
                    u.setProperty(UnitStorageCapacity, Long.toString(capacity));
                } else {
                    // System.out.println("ticketProperties doesn't contain bandwidth key");
                }
                gained.add(u);
                return new ResourceSet(gained, null, null, type, rd);
            } else {
                // no resource - delay the allocation
                return null;
            }
        } else {
            // extend automatically
            return new ResourceSet(null, null, null, type, null);
        }
    }

    protected Integer getTag(Unit u) {
        Integer tag = null;
        String t = u.getProperty(UnitLUNTag);
        if (t != null) {
            tag = new Integer(t);
        }
        return tag;
    }

    protected void free(Units set) throws Exception {
        // FIXME: some of the units in the set can be in the failed state
        // how should this control handle failed units?

        if (set != null) {
            Iterator<?> i = set.iterator();
            while (i.hasNext()) {
                Unit u = (Unit) i.next();
                try {
                    Integer tag = getTag(u);
                    if (tag == null) {
                        logger.error("LUNControl: attempted to free a unit with a missing tag");
                    } else {
                        logger.debug("LUNControl: freeing tag: " + tag);
                    }
                    tags.free(tag);
                } catch (Exception e) {
                    logger.error("Failed to release lun tag", e);
                }
            }
        }
    }

    public void revisit(IReservation r) throws Exception {
        UnitSet uset = (UnitSet) (r.getResources().getResources());
        Units set = uset.getSet();

        for (Unit u : set) {
            try {
                /*
                 * No need for locking. No other thread is accessing units of
                 * recovered reservations. State cannot be Closed, since closed
                 * units are filtered in UnitSet.revisit(Actor).
                 */
                switch (u.getState()) {
                case DEFAULT:
                case FAILED:
                case CLOSING:
                case PRIMING:
                case ACTIVE:
                case MODIFYING:
                    Integer tag = getTag(u);
                    if (tag == null) {
                        logger.error("LUNControl.revisit(): Recoverying a unit without a tag");
                    } else {
                        logger.debug("LUNControl.revisit(): reserving tag " + tag + " during recovery");
                        tags.allocate(tag, true);
                    }
                    break;
                case CLOSED:
                    break; // no-op
                }
            } catch (Exception e) {
                fail(u, "revisit with luncontrol", e);
            }
        }
    }
    
	@Override
	public void recoveryStarting() {
		logger.info("Beginning LUNControl recovery");
	}

	@Override
	public void recoveryEnded() {
		logger.info("Completing LUNControl recovery");
		logger.debug("Restored LUNControl resource type " + type + " with tags: \n" + tags );
	}

}
