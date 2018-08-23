package orca.policy.core;

import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import orca.policy.core.util.Inventory;
import orca.policy.core.util.InventoryForType;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.meta.QueryProperties;
import orca.shirako.core.PropertiesManager;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.ReservationSet;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

/**
 * A very simple broker policy.
 * @author aydan
 */
public class BrokerSimplerUnitsPolicy extends BrokerPriorityPolicy implements QueryProperties {
    /**
     * The policy inventory.
     */
	@NotPersistent
    protected Inventory inventory;

    /**
     * Creates a new instance.
     */
    public BrokerSimplerUnitsPolicy() {
        inventory = new Inventory();
    }

    @Override
    public void donate(final IClientReservation r) throws Exception {
        super.donate(r);
        inventory.getNew(r);
    }

    
    @Override
    public void allocate(final long cycle) throws Exception {
        // should we allocate?
        if (getNextAllocation(cycle) != cycle) {
            return;
        }

        // update the last allocation cycle
        lastAllocation = cycle;

        // get the allocation start time
        long startCycle = getStartForAllocation(cycle);
        long advanceCycle = getEndForAllocation(cycle);
        
        // get all pending requests
        ReservationSet requests = calendar.getAllRequests(advanceCycle);

        // do we have any work to do?
        if ((requests == null) || (requests.size() == 0)) {
            if (queue == null || queue.size() == 0) {
                logger.debug("no requests for auction start cycle " + startCycle);
                return;
            }
        }

        logger.debug("allocating resources for cycle " + startCycle);

        allocateExtending(requests, startCycle);
        allocateQueue(startCycle);
        allocateTicketing(requests, startCycle);
    }

    @Override
    public Properties query(final Properties properties) {
        String action = getQueryAction(properties);
        if (!QueryActionDisctoverPools.equals(action)) {
            return super.query(properties);
        }

        Properties response = inventory.getResourcePools();
        response.setProperty(QueryResponse, QueryActionDisctoverPools);
        return response;
    }

    /**
     * Returns the default pool id.
     * @return default pool id
     */
    protected String getDefaultPoolID() {
        String result = null;
        Iterator<String> iterator = inventory.getInventory().keySet().iterator();

        if (iterator.hasNext()) {
            result = iterator.next();
        }

        return result;
    }

    protected void allocateExtending(final ReservationSet requests, final long startCycle) throws Exception {
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
                    InventoryForType inv = inventory.get(poolID);

                    if (inv != null) {
                        Term extTerm = new Term(r.getTerm().getStartTime(), end, start);
                        extend(r, inv, extTerm);
                        // extend the ticket
                    } else {
                        // request from an invalid pool. fail the reservation
                        r.fail("there is no pool to satisfy this request");
                    }
                }
            }
        }
    }

    protected void allocateTicketing(final ReservationSet requests, final long startCycle) throws Exception {
        if (requests != null) {
            for (IReservation res : requests) {
                IBrokerReservation r = (IBrokerReservation) res;
                // is this a new request?
                if (!r.isTicketing()) {
                    continue;
                }

                if (ticket(r, startCycle)) {
                    continue;
                }

                if (queue == null) {
                    if (!r.isFailed()) {
                        r.fail("Insufficient resources");
                    }
                    continue;
                }

				if (!r.isFailed()) {
					if (PropertiesManager.isElasticTime(r.getRequestedResources())) {
                    	logger.debug("Adding reservation + " + r.getReservationID() + " to the queue");
                    	queue.add(r);
                	}else{
                        r.fail("Insufficient resources for specified start time, Failing reservation:"+r.getReservationID());
					}
           		}
            }
		}
    }

    public void allocateQueue(long startCycle) throws Exception {
        if (queue == null) {
            return;
        }

        Iterator<?> i = queue.iterator();

        while (i.hasNext()) {
            IBrokerReservation r = (IBrokerReservation) i.next();
            if (!ticket(r, startCycle)) {
                // Determine if we should keep the request on the queue
                // or return a failed reservation
                long threshold = PropList.getLongProperty(r.getRequestedResources().getRequestProperties(), QueueThreshold);
                long start = clock.cycle(r.getRequestedTerm().getNewStartTime());
                if ((threshold != 0) && ((startCycle - start) > threshold)) {
                    r.failWarn("Request has exceeded its threshold on the queue " + r);
                    i.remove();
                }
            } else {
                // we managed to allocate the queued reservation
                // remove it from the queue
                i.remove();
            }
        }

    }

    protected boolean ticket(IBrokerReservation r, long startCycle) {
        Date startTime = clock.date(startCycle);

        if (logger.isDebugEnabled()) {
            logger.debug("cycle: " + actor.getCurrentCycle() + " new ticket request: " + r);
        }

        // get the cycle-aligned start/end time
        Date start = alignStart(r.getRequestedTerm().getNewStartTime());
        Date end = alignEnd(r.getRequestedTerm().getEndTime());

        /*
         * if start is before the start time for this auction and the request is
         * elastic in time, we shift it to start at startTime
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
        if ((poolID == null) || !inventory.containsType(poolID)) {
            poolID = getDefaultPoolID();
        }

        if (poolID != null) {
            // get the inventory for the pool
            InventoryForType inv = inventory.get(poolID);
            if (inv != null) {
                // allocate the ticket
                return ticket(r, inv, new Term(start, end));
            } else {
                // cannot determine a pool for this request. Fail
                // the reservation
                r.fail("there is no pool to satisfy this request");
            }
        } else {
            // cannot determine a pool for this request. Fail
            // the reservation
            r.fail("there is no pool to satisfy this request");
        }

        return false;
    }

    protected boolean ticket(IBrokerReservation r, InventoryForType inv, Term term) {
        try {
            ResourceSet rset = r.getRequestedResources();
            int needed = rset.getUnits();
            int available = inv.getFree();
            int toAllocate = Math.min(needed, available);

            if (toAllocate == 0) {
                return false;
            }

            if (toAllocate < needed) {
                if (!PropertiesManager.isElasticSize(r.getRequestedResources())) {
                    return false;
                }
            }

            // make the allocation
            Properties p = inv.allocate(toAllocate, rset.getRequestProperties());
            // merge with the rest of the resource properties
            PropList.mergeProperties(inv.getProperties(), p);
            if (toAllocate < needed) {
                logger.error("partially satisfied request: allocated=" + toAllocate + " needed=" + needed);
            }
            // issue the ticket
            issueTicket(r, toAllocate, inv.getType(), term, p, inv.getSource());
            return true;
        } catch (Exception e) {
            logger.error(e);
            r.fail(e.toString());
            return false;
        }
    }

    protected void extend(IBrokerReservation r, InventoryForType inv, Term term) {
        try {
            ResourceSet rset = r.getRequestedResources();
            // what the client wants
            int needed = rset.getUnits();
            // what the client currently has
            int current = r.getResources().getUnits();
            // difference
            int difference = needed - current;
            int units = current;
            
            Properties p = null;
            if (difference > 0) {
                // client wants more units
                int available = inv.getFree();
                int toAllocate = Math.min(difference, available);
                // allocate
                if (toAllocate > 0) {
                    p = inv.allocate(toAllocate, rset.getRequestProperties(), rset.getResourceProperties());
                }

                if (toAllocate < difference) {
                    logger.error("partially satisfied request: allocated=" + toAllocate + " needed=" + difference);
                }
                units += toAllocate;
            } else if (difference < 0) {
                // client wants less units
                p = inv.free(-difference, rset.getRequestProperties(), rset.getResourceProperties());
                units += difference;
            }
            // merge with the rest of the resource properties
            PropList.mergeProperties(inv.getProperties(), p);
            // issue the ticket
            issueTicket(r, units, inv.getType(), term, p, inv.getSource());
        } catch (Exception e) {
            logger.error(e);
            r.fail(e.toString());
        }
    }

    protected void issueTicket(IBrokerReservation r, int units, ResourceType type, Term term, Properties properties, IClientReservation source) throws Exception {
        // make the new delegation
        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation((int) units, null, term, type, null, null, properties, getClientID(r));
        // extract a new resource set
        ResourceSet mine = extract(source.getResources(), del);

        // attach the current request properties so that we can look at
        // them in the future
        Properties p = r.getRequestedResources().getRequestProperties();
        mine.setRequestProperties(p);

        // the allocation may have added/updates resource properties
        // merge the allocation properties to the resource properties list.
        mine.setResourceProperties(properties);
        
        if ((mine != null) && !r.isFailed()) {
            r.setApproved(term, mine);
            r.setSource(source);
            if (logger.isDebugEnabled()) {
                logger.debug("allocated: " + mine.getUnits() + " for term: " + term.toString());
                logger.debug("resourceShare = " + units + " mine = " + mine.getUnits());
            }

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
    protected void release(final IBrokerReservation r) {
        super.release(r);

        if (r.isClosedInPriming()) {
            releaseResources(r.getApprovedResources(), r.getApprovedTerm());
        } else {
            releaseResources(r.getResources(), r.getTerm());
        }
    }
    
    @Override
    public void release(final IClientReservation reservation) {
        super.release(reservation);
        inventory.remove((IClientReservation)reservation);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseNotApproved(final IBrokerReservation r) {
        super.releaseNotApproved(r);
        releaseResources(r.getApprovedResources(), r.getApprovedTerm());
    }

    /**
     * Releases the resources assigned to this reservation.
     * @param set resource set
     * @param t term
     */
    protected void releaseResources(final ResourceSet set, final Term t) {
        try {
            if ((set == null) || (t == null) || (set.getResources() == null)) {
                logger.warn("Reservation does not have resources to release");

                return;
            }

            InventoryForType inv = inventory.get(set.getType().toString());
            if (inv == null) {
                throw new Exception("Cannot release resources: missing inventory");
            }

            inv.free(set.getUnits(), set.getResourceProperties());
        } catch (Exception e) {
            logger.error("releaseReources", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void revisitServer(final IBrokerReservation reservation) throws Exception {
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
    protected void revisitTicketed(final IBrokerReservation reservation) throws Exception {
        ResourceSet rset = reservation.getResources();
        ResourceType type = rset.getType();
        /* get the resource pool */
        InventoryForType inv = inventory.get(type.toString());
        if (inv == null) {
            throw new IllegalStateException("cannot free resources: no inventory");
        }
        inv.allocateRevisit(rset.getUnits(), rset.getResourceProperties());
    }
}
