package orca.policy.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.core.PropertiesManager;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.InventoryList;
import orca.shirako.util.ReservationSet;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

public class BrokerSimpleUnitsPolicy extends BrokerPriorityPolicy
{
    @NotPersistent
    protected HashMap<String, InventoryList> inventory;

    public BrokerSimpleUnitsPolicy()
    {
        inventory = new HashMap<String, InventoryList>();
    }

    @Override
    public void donate(final IClientReservation r) throws Exception
    {
        super.donate(r);

        ResourceType rtype = r.getType();
        ResourceSet rset = r.getResources();
        Ticket cset = (Ticket) rset.getResources();
        ResourceTicket ticket = cset.getTicket();

        // get the assignment forest for this type
        InventoryList inv = inventory.get(rtype.toString());
        if (inv == null) {
            inv = new InventoryList();
            inv.setType(rtype);
            PropList.mergeProperties(ticket.getProperties(), inv.getProperties());
            PropList.mergeProperties(r.getResources().getResourceProperties(), inv.getProperties());
            inv.setProxy(cset.getSiteProxy());
            inventory.put(rtype.toString(), inv);
        }

        inv.addInventory(ticket.getTerm().getNewStartTime().getTime(), ticket.getTerm().getEndTime().getTime(), ticket.getUnits());
    }

    @Override
    public void allocate(final long cycle) throws Exception
    {
        // should we allocate?

        if (getNextAllocation(cycle) != cycle) {
            return;
        }

        // update the last allocation cycle
        lastAllocation = cycle;

        // get the allocation start time
        long startTime = getStartForAllocation(cycle);
        // get all pending requests
        ReservationSet requests = calendar.getRequests(startTime);
        // do we have any work to do?
        if ((requests == null) || (requests.size() == 0)) {
            logger.debug("No requests for auction start cycle " + startTime);
            return;
        }

        logger.debug("Allocating resources for cycle " + startTime);

        if (requests.size() > 0) {
            allocateExtending(requests, startTime);
            allocateTicketing(requests, startTime);
        }
    }

    @Override
    public Properties query(final Properties properties)
    {
        if ((properties != null) && properties.containsKey(PropertyDiscoverTypes)) {
            Properties p = new Properties();
            int count = 0;

            for (Map.Entry<String, InventoryList> entry : inventory.entrySet()) {
                InventoryList inv = entry.getValue();
                String type = entry.getKey();
                p.setProperty(PropertyTypeNamePrefix + count, type);
                PropList.setProperty(p, PropertyTypeDescriptionPrefix + count, inv.getProperties());
                count++;
            }

            PropList.setProperty(p, PropertyTypeCount, count);

            return p;
        } else {
            return super.query(properties);
        }
    }

    /**
     * Returns the default pool id.
     * @return default pool id
     */
    protected String getDefaultPoolID()
    {
        String result = null;
        Iterator<String> iterator = inventory.keySet().iterator();

        if (iterator.hasNext()) {
            result = iterator.next();
        }

        return result;
    }

    protected void allocateExtending(final ReservationSet requests, final long startCycle) throws Exception
    {
        if (requests != null) {
            // process extension requests first
            for (IReservation res : requests) {
                IBrokerReservation r = (IBrokerReservation) res;

                // is this an extension request
                if (r.isExtendingTicket() && !r.isClosed()) {
                    /*
                     * Start time should already by cycle aligned, if not, we
                     * will find out later.
                     */
                    Date start = r.getRequestedTerm().getNewStartTime();

                    // align the end
                    Date end = alignEnd(r.getRequestedTerm().getEndTime());

                    // which pool?
                    String poolID = getCurrentPoolID(r);
                    assert poolID != null;

                    // get the inventory for the pool
                    InventoryList inv = inventory.get(poolID);

                    if (inv != null) {
                        // FIXME: for now we simply allocate a fresh ticket
                        ticket(r, inv, new Term(r.getTerm().getStartTime(), end, start));
                        // extend the ticket
                    } else {
                        // request from an invalid pool. fail the reservation
                        r.fail("No such pool.");
                    }
                }
            }
        }
    }

    protected void allocateTicketing(final ReservationSet requests, final long startCycle) throws Exception
    {
        Date startTime = clock.date(startCycle);

        if (requests != null) {
            for (IReservation res : requests) {
                IBrokerReservation r = (IBrokerReservation) res;

                // is this a new request?
                if (r.isTicketing()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("cycle: " + actor.getCurrentCycle() + " new ticket request: " + r);
                    }

                    // get the cycle-aligned start/end time
                    Date start = alignStart(r.getRequestedTerm().getNewStartTime());
                    Date end = alignEnd(r.getRequestedTerm().getEndTime());

                    /*
                     * if start is before the start time for this auction and
                     * the request is elastic in time, we shift it to start at
                     * startTime
                     */
                    if (start.before(startTime) && PropertiesManager.isElasticTime(r.getRequestedResources())) {
                        // calculate the term length (milliseconds)
                        long length = end.getTime() - start.getTime();
                        // shift the start
                        start = new Date(clock.cycleStartInMillis(startCycle));
                        // shift the end
                        end = new Date(start.getTime() + length);
                    }

                    // which pool: we use the resource type to map the pool
                    String poolID = r.getRequestedResources().getType().toString();
                    if (!inventory.containsKey(poolID)){
                        poolID = getDefaultPoolID();
                    }

                    if (poolID != null) {
                        // get the inventory for the pool
                        InventoryList inv = inventory.get(poolID);
                        if (inv != null) {
                            // allocate the ticket
                            ticket(r, inv, new Term(start, end));
                        } else {
                            // cannot determine a pool for this request. Fail
                            // the reservation
                            r.fail("Invalid pool");
                        }
                    } else {
                        // cannot determine a pool for this request. Fail
                        // the reservation
                        r.fail("Invalid pool");
                    }
                }
            }
        }
    }

    protected void ticket(IBrokerReservation r, InventoryList inv, Term term)
    {
        try {
            ResourceSet rset = r.getRequestedResources();
            int needed = rset.getUnits();
            int available = (int) inv.getMinUnits(term.getNewStartTime().getTime(), term.getEndTime().getTime());
            int toAllocate = Math.min(needed, available);
            if (toAllocate > 0) {
                inv.reserve(term.getNewStartTime().getTime(), term.getEndTime().getTime(), toAllocate);
            } else {
                throw new Exception("Insufficient resources");
            }
            if (toAllocate < needed) {
                logger.error("partially satisfied request: allocated=" + toAllocate + " needed=" + needed);
            }
            issueTicket(r, toAllocate, inv.getType(), term, inv.getProperties());
            if (requireApproval) {
                /*
                 * Allocations require administrative approval. Add it to the
                 * set.
                 */
                addForApproval(r);
            } else {
                addToCalendar(r);
                /*
                 * Whatever happened up there, this bid is no longer pending. It
                 * either succeeded or is now marked failed. (Could/should
                 * assert.)
                 */
                r.setBidPending(false);
            }

        } catch (Exception e) {
            logger.error(e);
            r.fail(e.toString());
        }
    }

    protected IClientReservation getSource(Date startTime, ResourceType type)
    {
        ReservationSet holdings = calendar.getHoldings(startTime, type);

        if (holdings.size() > 0) {
            return (IClientReservation) holdings.iterator().next();
        } else {
            return null;
        }
    }

    protected void issueTicket(IBrokerReservation r, int units, ResourceType type, Term term, Properties properties) throws Exception
    {
        IClientReservation source = getSource(term.getNewStartTime(), type);
        if (source == null) {
            throw new Exception("Could not find a source reservation");
        }

        // make the new delegation
        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation((int) units, null, term, type, null, null, properties, getClientID(r));

        ResourceSet mine = extract(source.getResources(), del);

        // attach the current request properties so that we can look at
        // them in the future
        Properties p = r.getRequestedResources().getRequestProperties();
        mine.setRequestProperties(p);

        if ((mine != null) && !r.isFailed()) {
            r.setApproved(term, mine);
            r.setSource(source);
            if (logger.isDebugEnabled()) {
                logger.debug("allocated: " + mine.getUnits() + " for term: " + term.toString());
                logger.debug("resourceShare = " + units + " mine = " + mine.getUnits());
            }
        } else {
            if (mine == null) {
                throw new Exception("There was an error extracting a ticket from the source ticket");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void release(final IBrokerReservation r)
    {
        super.release(r);

        if (r.isClosedInPriming()) {
            if (r.getPreviousResources() != null) {
                releaseResources(r.getPreviousResources(), r.getPreviousTerm());
            }

            releaseResources(r.getApprovedResources(), r.getApprovedTerm());
        } else {
            releaseResources(r.getResources(), r.getTerm());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseNotApproved(final IBrokerReservation r)
    {
        super.releaseNotApproved(r);
        releaseResources(r.getApprovedResources(), r.getApprovedTerm());
    }

    /**
     * Releases the resources assigned to this reservation.
     * @param set resource set
     * @param t term
     */
    protected void releaseResources(final ResourceSet set, final Term t)
    {
        try {
            if ((set == null) || (t == null) || (set.getResources() == null)) {
                logger.warn("Reservation does not have resources to release");

                return;
            }

            InventoryList inv = inventory.get(set.getType().toString());
            if (inv == null) {
                throw new Exception("Cannot release resources: missing inventory");
            }

            inv.release(t.getNewStartTime().getTime(), t.getEndTime().getTime(), set.getUnits());
        } catch (Exception e) {
            logger.error("releaseReources", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void revisitServer(final IBrokerReservation reservation) throws Exception
    {
        super.revisitServer(reservation);

        switch (reservation.getState()) {
            case ReservationStates.Ticketed:
                revisitTicketed(reservation);

                break;
        }
    }

    /**
     * Recovers a ticketed reservation.
     * @param reservation reservation
     * @throws Exception in case of error
     */
    protected void revisitTicketed(final IBrokerReservation reservation) throws Exception
    {
        switch (reservation.getPendingState()) {
            case ReservationStates.None:
                revisitTicketedNone(reservation);

                break;

            case ReservationStates.Priming:
                revisitTicketedPriming(reservation);

                break;
        }
    }

    /**
     * Recovers an active ticketed reservation
     * @param reservation reservation
     * @throws Exception in case of error
     */
    protected void revisitTicketedNone(final IBrokerReservation reservation) throws Exception
    {
        ResourceSet rset = reservation.getResources();
        ResourceType type = rset.getType();
        Term term = reservation.getTerm();
        /* get the resource pool */
        InventoryList inv = inventory.get(type.toString());

        inv.reserve(term.getNewStartTime().getTime(), term.getEndTime().getTime(), rset.getUnits());
    }

    /**
     * Recovers a ticketed priming reservation.
     * @param reservation reservation
     * @throws Exception in case of error
     */
    protected void revisitTicketedPriming(final IBrokerReservation reservation) throws Exception
    {
        ResourceSet rset = reservation.getApprovedResources();
        ResourceType type = rset.getType();
        Term term = reservation.getTerm();
        /* get the resource pool */
        InventoryList inv = inventory.get(type.toString());

        inv.reserve(term.getNewStartTime().getTime(), term.getEndTime().getTime(), rset.getUnits());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish(final long cycle)
    {
        super.finish(cycle);
        tickInventory(cycle);
    }

    /**
     * Removes irrelevant state from the resource pools.
     * @param cycle cycle
     */
    protected void tickInventory(final long cycle)
    {
        long realTime = clock.cycleEndInMillis(cycle);

        for (InventoryList inv : inventory.values()) {
            // inventory.tick(cycle);
            inv.tick(realTime);
        }
    }
}
