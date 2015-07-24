package orca.policy.core;

import java.util.Iterator;
import java.util.Properties;

import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.util.FreeAllocatedSet;
import orca.shirako.util.ResourceData;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

/**
 * @author aydan
 */
public class VlanControl extends ResourceControl {
    public static final String ConfigUnitTag = "config.unit.tag";
    public static final String PropertyStartVlan = "vlan.tag.start";
    public static final String PropertyEndVlan = "vlan.tag.end";
    public static final String PropertyVlanRangeNum = "vlan.range.num";

    /**
     * Set of available and allocated tags. No persistence: this state is
     * rebuilt during recovery.
     */
    // recovered in revisit
    @NotPersistent
    protected FreeAllocatedSet<Integer> tags;
    // recovered in donate
    @NotPersistent
    protected ResourceType type;

    /**
     * Creates a new instance.
     */
    public VlanControl() {
        tags = new FreeAllocatedSet<Integer>();
    }

    @Override
    public void donate(IClientReservation r) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("VlanControl.donate(): donating resouces r=" + r.toString());
        }

        if (tags.size() != 0) {
            throw new Exception("only a single source reservation is supported");
        }

        ResourceSet set = r.getResources();
        ResourceType rtype = r.getType();
        Properties p = set.getLocalProperties(); // note: local properties
        if (p == null) {
            throw new Exception("Missing local properties");
        }
        // FIXME: ignoring term
        type = rtype;
        int start = 0, end = 0, size = 0;
        String startP, endP;
        int numRange = PropList.getRequiredIntegerProperty(p, PropertyVlanRangeNum);
        for (int i = 1; i <= numRange; i++) {
            startP = PropertyStartVlan + String.valueOf(i);
            endP = PropertyEndVlan + String.valueOf(i);
            start = PropList.getRequiredIntegerProperty(p, startP);
            end = PropList.getRequiredIntegerProperty(p, endP);
            if ((start == 0) && (end == 0)) {
                break;
            }
            for (int j = start; j <= end; j++) {
                tags.addInventory(new Integer(j));
            }
            size = size + (end - start + 1);
            logger.info("VlanControl.donate(): Tag Donation:" + rtype + ":" + start + "-" + end + ":" + size);
        }
        if (size < r.getUnits()) {
            throw new Exception("Insufficient vlan tags specified in donated reservation: donated "
                    + size + " rset says: " + r.getUnits());
        }
    }

    public void donate(ResourceSet set) throws Exception {
        // we ignore this call since it deals with physical resources
    }

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

        // COMMENTED OUT BECAUSE NOT USED /ib 04/08/14
        // get the requested lease term
        //Term term = r.getRequestedTerm();
        // convert to cycles
        //long start = authority.getActorClock().cycle(term.getNewStartTime());
        //long end = authority.getActorClock().cycle(term.getEndTime());
        Properties configuration_properties = requested.getConfigurationProperties();

        // determine if this is a new lease or an extension request
        if (current == null) {
            // this is a new request
            // how much does the client want?
            long needed = ticket.getUnits();
            if (needed > 1) {
                r.fail("Cannot assign more than 1 VLAN per reservation");
            }

            String configTag = configuration_properties.getProperty(ConfigUnitTag);

            Integer staticTag = null;
            if (configTag != null)
                staticTag = Integer.valueOf(configTag);
            if (tags.getFree() > 0) {

                Integer tag;
                if (staticTag == null) {
                    tag = tags.allocate();
                } else {
                    tag = tags.allocate(staticTag, true);
                }

                ResourceData rd = new ResourceData();
                PropList.setProperty(rd.getResourceProperties(), UnitVlanTag, tag);
                UnitSet gained = new UnitSet(authority.getShirakoPlugin());
                Unit u = new Unit();
                u.setResourceType(type);
                u.setProperty(UnitVlanTag, tag.toString());
                // if the broker allocated specific bandwidth, set it as a
                // property on the unit
                if (ticketProperties.containsKey(ResourceProperties.ResourceBandwidth)) {
                    long bw = Long.parseLong(ticketProperties.getProperty(ResourceProperties.ResourceBandwidth));
                    long burst = bw / 8;
                    u.setProperty(UnitVlanQoSRate, Long.toString(bw));
                    u.setProperty(UnitVlanQoSBurstSize, Long.toString(burst));
                } else {
                    // System.out.println("ticketProperties doesn't contain bandwidth key");
                }
                gained.add(u);
                return new ResourceSet(gained, null, null, type, rd);
            } else {
                // no resources - delay the allocation
                return null;
            }
        } else {
            // extend automatically with no changes
            return new ResourceSet(type);
        }
    }

    protected Integer getTag(Unit u) {
        Integer tag = null;
        String t = u.getProperty(UnitVlanTag);
        if (t != null) {
            tag = new Integer(t);
        }
        return tag;
    }

    protected void free(Units set) throws Exception {
        // ASSUMPTION: it is ok to reuse the tag of a failed VLAN node.
        // FIXME: since we use a single unit for a VLAN request, the passed in
        // set should always have 1 elements in it? Assert if that's the case.

        if (set != null) {
            Iterator<?> i = set.iterator();
            while (i.hasNext()) {
                Unit u = (Unit) i.next();
                try {
                    Integer tag = getTag(u);
                    if (tag == null) {
                        logger.error("VlanControl.free(): attempted to free a unit with a missing tag");
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("VlanControl.free(): freeing tag: " + tag);
                        }
                    }
                    tags.free(tag);
                } catch (Exception e) {
                    logger.error("VlanControl.free(): Failed to release vlan tag", e);
                    // FIXME: what happens here? I know that this is unlikely,
                    // but if it happens,
                    // we leak the tag.
                }
            }
        }
    }

    public void revisit(IReservation r) throws Exception {
        // Recovery policy: we start by assuming that no resources are
        // allocated.
        // As we see recovered reservations, we mark as in use all vlan tags for
        // nodes that are not marked as CLOSED.

        UnitSet uset = (UnitSet) (r.getResources().getResources());
        Units set = uset.getSet();

        for (Unit u : set) {
            try {
                switch (u.getState()) {
                case DEFAULT:
                case FAILED:
                case CLOSING:
                case PRIMING:
                case ACTIVE:
                case MODIFYING:
                    Integer tag = getTag(u);
                    if (tag == null) {
                        logger.error("VlanControl.revisit(): Recoverying a unit without a tag");
                    } else {
                        logger.debug("VlanControl.revisit(): reserving tag " + tag + " during recovery");
                        tags.allocate(tag, true);
                    }
                    break;
                case CLOSED:
                    logger.debug("VlanControl.revisit(): node is closed. Nothing to recover");
                    break;

                }
            } catch (Exception e) {
                fail(u, "revisit with vlancontrol", e);
            }
        }
    }
    
	@Override
	public void recoveryStarting() {
		logger.info("Beginning VlanControl recovery");
	}

	@Override
	public void recoveryEnded() {
		logger.info("Completing VlanControl recovery");
		logger.debug("Restored VlanControl resource type " + type + " with tags: \n" + tags );
	}
}
