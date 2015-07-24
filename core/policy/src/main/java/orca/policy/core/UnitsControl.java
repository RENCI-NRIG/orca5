package orca.policy.core;

import java.util.Iterator;
import java.util.Properties;

import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

/**
 * @author aydan
 */
public class UnitsControl extends ResourceControl {
    @NotPersistent
    protected int total;
    @NotPersistent
    protected int allocated;
    @NotPersistent
    protected Properties resourceProperties;

    /**
     * Resource type.
     */
    @NotPersistent
    protected ResourceType type;

    /**
     * Creates a new instance.
     */
    public UnitsControl() {
        resourceProperties = new Properties();
    }

    @Override
    public void donate(IClientReservation r) throws Exception {
        ResourceSet set = r.getResources();
        if (type != null) {
            if (!type.equals(r.getType())) {
                throw new Exception("Donated reservation has a different resource type: current="
                        + type + " new=" + r.getType());
            }
        }
        type = r.getType();
        Properties resprops = set.getResourceProperties();
        PropList.mergeProperties(resprops, resourceProperties);
        // note: ignoring term
        total += r.getUnits();
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

        if (total == 0) {
            throw new Exception("no inventory");
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

        UnitSet gained = null;
        UnitSet lost = null;

        int available = total - allocated;
        // determine if this is a new lease or an extension request
        if (current == null) {
            // this is a new request
            // how much does the client want?
            int needed = ticket.getUnits();

            if (needed == 0) {
                r.fail("Request must be for at least one unit");
            }

            if (available == 0) {
                return null;
            }

            gained = getUnits(needed);
        } else {
            int currentUnits = current.getUnits();
            int difference = ticket.getUnits() - currentUnits;
            if (difference > 0) {
                gained = getUnits(difference);
            } else if (difference < 0) {
                UnitSet uset = (UnitSet) current.getResources();
                String victims = requestProperties.getProperty(ConfigVictims);
                Units toTake = uset.selectExtract(-difference, victims);
                lost = new UnitSet(authority.getShirakoPlugin(), toTake);
            }
        }
        return new ResourceSet(gained, lost, null, type, null);
    }

    protected UnitSet getUnits(int needed) {
        UnitSet uset = new UnitSet(authority.getShirakoPlugin());
        int available = Math.min(needed, total - allocated);
        allocated += available;

        for (int i = 0; i < available; i++) {
            Unit u = new Unit();
            // set the resource type
            u.setResourceType(type);
            u.mergeProperties(resourceProperties);
            uset.add(u);
        }
        return uset;
    }

    protected void free(Units set) throws Exception {
        // ASSUMPTION: since this is a "simple" control, it assumes that even
        // failed nodes can be reused.
        if (set != null) {
            Iterator<?> i = set.iterator();
            while (i.hasNext()) {
                // consume it!
                i.next();
                allocated--;
            }
        }
    }

    public void revisit(IReservation r) throws Exception {
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
                    allocated++;
                    break;
                case CLOSED:
                    break; // no-op
                }
            } catch (Exception e) {
                fail(u, "revisit with vlancontrol", e);
            }
        }
    }
}
