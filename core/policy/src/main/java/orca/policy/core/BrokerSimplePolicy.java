/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.container.Globals;
import orca.shirako.core.PropertiesManager;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.Bids;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceCount;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;


/**
 * <code>BrokerSimplePolicy</code> is a simple implementation of the broker
 * policy interface. It buffers requests for allocation periods and when it
 * performs allocations of resources it does so in FIFO order giving
 * preference to extending requests.
 */
public class BrokerSimplePolicy extends BrokerCalendarPolicy
{
    /**
     * Specifies the type of ticket being requested.
     */
    public static final String REQUEST_TYPE = "requestType";
    public static final String PropertyAllocationHorizon = "allocation.horizon";
    /**
     * The amount of time over specific policy decisions the broker
     * must add when communicating with other brokers as a client (e.g.
     * renew()). Clock skew must be at least one if the broker is ticked after
     * the upstream broker(s). At some point in time we may want this to not
     * be static and learn it from what we see in the system, but for now it
     * is static.
     */
    public static final long CLOCK_SKEW = 1;

    /**
     * Number of cycles between two consecutive allocations.
     */
    
    public static final long CALL_INTERVAL = 1;

    /**
     * How far in the future is the broker allocating resources
     * (starting time).
     */
    public static final long ADVANCE_TIME = 3;

    /**
     * Last time we allocated requests
     */
    // FIXME: check
    @NotPersistent
    protected long lastAllocation;
    // FIXME: check
    @NotPersistent
    protected long allocationHorizon = 0;
    /**
     * Indicates if the broker is ready to accept requests.
     */
    // FIXME: check
    @NotPersistent
    protected boolean ready = false;

    /**
         * Creates a new instance.
         */
    public BrokerSimplePolicy()
    {
        lastAllocation = -1;
    }

    public void configure(final Properties p) throws Exception
    {
        if (p.containsKey(PropertyAllocationHorizon)) {
            try {
                allocationHorizon = PropList.getIntegerProperty(p, PropertyAllocationHorizon);
            } catch (Exception e) {
                Globals.Log.warn("Invalid value for property. key=" + PropertyAllocationHorizon + ", value=" + p.getProperty(PropertyAllocationHorizon));
            }
        }        
    }
    
    /**
     * Runs an allocation of requests against all sources, given a set
     * of pending bids by using FCFS giving extends priority. This simple
     * policy runs through the bids in iterator order, and gives every request
     * <code>min(requested,available)</code> until we run out. It gives
     * priority to extending requests compared to new requests.<p>The
     * implementation assumes that there is only 1 source reservation per
     * resource type. It also makes the assumption that we allocate resources
     * sequentially in time, so there are never less resources available in
     * the future.</p>
     *  {@inheritDoc}
     *
     * @param cycle DOCUMENT ME!
     */
    public void allocate(final long cycle) throws Exception
    {
        /*
         * This method should first decide whether to run an allocation on the
         * current cycle. If the answer is yes, then it has to decide on the
         * start time of reservations it is going to allocate.
         */
        if (getNextAllocation(cycle) != cycle) {
            logger.debug("Next allocation cycle " + getNextAllocation(cycle) + " != " + cycle);

            return;
        }

        lastAllocation = cycle;

        /*
         * Determine the cycle for which the agent is allocating resources
         */
        long startTime = getStartForAllocation(cycle);

        ReservationSet allBids = calendar.getRequests(startTime);

        /*
         * If there are no extending and no new requests - return
         */
        if ((allBids == null) || (allBids.size() == 0)) {
            logger.debug("No requests for allocation start cycle " + startTime);

            return;
        }

        logger.debug("There are " + allBids.size() + " bids for cycle " + startTime);

        /*
         * Put source on a Hashtable for the new request allocation
         */
        ReservationSet holdings = calendar.getHoldings(clock.cycleStartDate(startTime));
        Hashtable<ResourceType, IClientReservation> sourceHash = createSourceHashtable(
            holdings.iterator());

        /*
         * Move extending reservations that have changed their types to an
         * appropriate source
         */
        switchSource(holdings.iterator(), startTime, sourceHash);

        /*
         * Allocate extending requests
         */
        allocateExtending(holdings.iterator(), startTime, null);

        /*
         * Allocate the remaining bids
         */
        allocateNewBids(allBids.iterator(), sourceHash, startTime, null);
    }

    /**
     * Iterates through all of the sources and allocates resources to
     * their extending bids based on FIFO. Only allocates requests with types
     * specified in <code>requestTypes</code> or all requests if
     * <code>requestTypes</code> is null.
     *
     * @param sources sources for this allocation
     * @param startTime when the allocation begins
     * @param requestTypes determines which request type is being allocated. A
     *        requestType of <code>null</code> indicates that any requestType
     *        may be allocated.
     *
     * @throws Exception in case of error
     */
    protected void allocateExtending(final Iterator<IReservation> sources, final long startTime,
                                     final Vector<String> requestTypes) throws Exception
    {
        while (sources.hasNext()) {
            IClientReservation source = (IClientReservation) sources.next();
            ReservationSet extendingForSource = calendar.getRequests(source, startTime);

            if ((extendingForSource == null) || (extendingForSource.size() == 0)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("There are no extends for source: " + source.toString());
                }
            } else {
                logger.debug("There are " + extendingForSource.size() + " extend reservations");

                Iterator<IReservation> extend = extendingForSource.iterator();

                while (extend.hasNext()) {
                    IBrokerReservation reservation = (IBrokerReservation) extend.next();

                    if (logger.isDebugEnabled()) {
                        logger.info("ExtendRes = " + reservation.toString());
                    }

                    String requestType = reservation.getRequestedResources().getRequestProperties().getProperty(
                        REQUEST_TYPE);

                    // null request type indicates allocate all bids
                    if ((requestTypes == null) || requestTypes.contains(requestType)) {
                        int wanted = reservation.getRequestedUnits();

                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                "ASPPlugin:allocateBids: allocating source rid(" +
                                source.getReservationID().toHashString() + ") to extendDynamicRes(" +
                                reservation.getReservationID().toHashString() + ") that has props:" +
                                reservation.getRequestedResources().getRequestProperties());
                        }

                        boolean satisfy = verifyWantedResources(reservation, source, startTime,
                                                                wanted);

                        if (satisfy) {
                            satisfyAllocation(reservation, source, wanted, startTime);
                        }
                    }

                    /*
                     * Do not want to keep reservation around to minimize future
                     * looping through extends
                     */
                    extend.remove();
                }
            }
        }
    }

    /**
     * Iterates through all of the new bids and allocates resources
     * based on FIFO. Only allocates requests with types specified in
     * <code>requestTypes</code> or all requests if <code>requestTypes</code>
     * is null. Requests are associated with their appropriate source.
     *
     * @param newBids new bids to be allocated
     * @param sourceHash the sources for this allocation
     * @param startTime when the allocation begins
     * @param requestTypes determines which request type is being allocated. A
     *        requestTypes of <code>null</code> indicates that any requestType
     *        may be allocated.
     *
     * @throws Exception in case of error
     */
    protected void allocateNewBids(Iterator<IReservation> newBids,
                                   Hashtable<ResourceType, IClientReservation> sourceHash,
                                   long startTime, Vector<String> requestTypes)
                            throws Exception
    {
        while (newBids.hasNext()) {
            IBrokerReservation reservation = (IBrokerReservation) newBids.next();

            String requestType = reservation.getRequestedResources().getRequestProperties().getProperty(
                REQUEST_TYPE);

            // null request type indicates allocate all bids
            if (!reservation.isExtendingTicket() &&
                    ((requestTypes == null) ||
                        ((requestType != null) && requestTypes.contains(requestType)))) {
                IClientReservation source = (IClientReservation) sourceHash.get(
                    reservation.getRequestedType());

                if (source == null) {
                    reservation.fail("There are no sources of type" +
                                     reservation.getRequestedType());
                } else {
                    reservation.setSource(source);

                    int wanted = reservation.getRequestedUnits();

                    boolean satisfy = verifyWantedResources(reservation, source, startTime, wanted);

                    if (satisfy) {
                        satisfyAllocation(reservation, source, wanted, startTime);
                    }
                }

                /*
                 * Remove so we do not loop through this bid in future
                 * allocations
                 */
                newBids.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean bind(IBrokerReservation reservation) throws Exception
    {
        Term term = reservation.getRequestedTerm();
        logger.info(
            "SlottedAgent bind arrived at cycle " + actor.getCurrentCycle() + " requested term " +
            term.toString());

        /*
         * Choose an allocation time for this reservation
         */
        long bidCycle = getAllocation(reservation);

        /*
         * Save the bid on the allocation slot in holdings calendar.
         */
        calendar.addRequest(reservation, bidCycle);

        /*
         * Defer the request
         */
        return false;
    }

    /**
     * Calculates the available resources from the specified source for
     * the specified start time.
     *
     * @param startTime start of proposed ticket
     * @param source source whose resources are being counted
     *
     * @return number of available units
     */
    protected long calculateAvailable(long startTime, IClientReservation source)
    {
        Date startDate = clock.cycleStartDate(startTime);

        // get the active reservations allocated from the given source
        ReservationSet outlays = calendar.getOutlays(source, startDate);

        ResourceCount c = count(outlays, startDate);
        long active = c.countActive(source.getType());

        // determine how many are available
        long available = source.getUnits(startDate) - active;

        if (logger.isDebugEnabled()) {
            logger.debug("There are " + available + " resource available - extend");
            logger.debug("There are " + active + " resource active");
        }

        return available;
    }

    /**
     * Create a Hashtable of the sources by type. Only allows one
     * source per type.
     *
     * @param sources sources for this allocation
     *
     * @return a Hashtable of the sources
     */
    protected Hashtable<ResourceType, IClientReservation> createSourceHashtable(Iterator<IReservation> sources)
    {
        Hashtable<ResourceType, IClientReservation> sourceHash = new Hashtable<ResourceType, IClientReservation>();

        while (sources.hasNext()) {
            IClientReservation source = (IClientReservation) sources.next();

            // add to Hashtable
            ResourceType type = source.getType();

            if (sourceHash.containsKey(type)) {
                logger.error("sourceHash already contains key - only allow one source per type");
            } else {
                sourceHash.put(type, source);
            }
        }

        return sourceHash;
    }

    /**
     * {@inheritDoc}
     */
    public boolean extend(IBrokerReservation reservation) throws Exception
    {
        Term requested = reservation.getRequestedTerm();
        logger.info(
            "SlottedAgent extend arrived at cycle " + actor.getCurrentCycle() + " requested term " +
            requested.toString());

        IClientReservation source = reservation.getSource();

        if (source == null) {
            error("cannot find parent ticket for extend");
        }

        if (source.isFailed()) {
            error("parent ticket could not be renewed");
        }

        long bidCycle = getAllocation(reservation);

        /*
         * Save bid on the outlay calendar for this source reservation. Also
         * save the bid on the allocation slot in holdings calendar, so we have
         * one record of all demands for this allocation time.
         */
        calendar.addRequest(reservation, bidCycle, source);
        calendar.addRequest(reservation, bidCycle);

        /*
         * Defer the request
         */
        return false;
    }

    /**
     * Extracts the ticket from the source and checks to see if the
     * resulting ticket satisfies the request.
     *
     * @param reservation reservation being requested
     * @param source source request is being extracted from
     * @param approved the approved term for this ticket
     * @param resourceShare number of resources being allocated to this ticket
     *
     * @return the newly extracted ticket
     *
     * @throws Exception in case of error
     */
    protected ResourceSet extractTicket(final IBrokerReservation reservation,
                                        final IClientReservation source, final Term approved,
                                        long resourceShare) throws Exception
    {
        ResourceSet mine = null;
        ResourceSet ticket = source.getResources();

        try {
            if (!source.getTerm().contains(approved)) {
                String error = "Source term does not contain requested term or reservation is not elastic in time: sourceterm=" +
                               source.getTerm().toString() + " resterm=" + approved.toString();
                reservation.fail(error);
            } else {
            	   
                // make the new delegation                
                ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().
                							makeDelegation((int)resourceShare, approved, ticket.getType(), getClientID(reservation));
                
                // make the new resource set
                mine = extract(ticket, del);

                // attach the current request properties so that we can look at
                // them in the future
                Properties p = reservation.getRequestedResources().getRequestProperties();
                mine.setRequestProperties(p);
            }
        } catch (Exception e) {
            logger.error(
                "Term not satisfied: Mapper extract failed: has:" + ticket.getConcreteUnits(),
                e);
            throw new Exception(
                "Term not satisfied: Mapper extract failed: " + e.toString() + " has:" +
                ticket.getConcreteUnits());
        }

        if ((mine != null) && !reservation.isFailed()) {
            reservation.setApproved(approved, mine);
            reservation.setSource(source);

            if (logger.isDebugEnabled()) {
                logger.debug("allocated: " + mine.getUnits() + " for term: " + approved.toString());
                logger.debug("resourceShare = " + resourceShare + " mine = " + mine.getUnits());
            }
        }

        return mine;
    }

    /**
     * Formulate a collection of bids to extend. AgentSimple does not
     * request new reservations, only extends existing ones. {@inheritDoc}
     *
     * @param cycle DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Bids formulateBids(long cycle)
    {
        ReservationSet pending = calendar.getPending();

        /*
         * Select reservations to extend, and bind to bid and term.
         */
        ReservationSet renewing = calendar.getRenewing(cycle);
        ReservationSet extending = processRenewing(renewing, pending);

        return new Bids(new ReservationSet(), extending);
    }

    /**
     * Returns the allocation time for this reservation
     *
     * @param reservation reservation being assigned to an allocation
     *
     * @return allocation cycle for this reservation
     * @throws Exception in case of error 
     */
    protected long getAllocation(IBrokerReservation reservation) throws Exception
    {
        if (!ready) {
            error("Agent not ready to accept bids");
        }

        long start = clock.cycle(reservation.getRequestedTerm().getNewStartTime());

        start -= ADVANCE_TIME;

        long intervals = (start - lastAllocation) / CALL_INTERVAL;

        if (intervals <= 0) {
            intervals = 1;
        }

        start = lastAllocation + (intervals * CALL_INTERVAL) + ADVANCE_TIME;

        return start;
    }

    /**
     * Determine the approved term of a reservation
     *
     * @param reservation reservation
     *
     * @return the approved term
     */
    protected Term getApprovedTerm(IBrokerReservation reservation)
    {
        Term approved = new Term(reservation.getRequestedTerm());

        return approved;
    }

    /**
     * Returns the cycle of the next allocation period. Does not update
     * state.
     *
     * @param currentCycle the current cycle
     *
     * @return allocation cycle
     */
    protected long getNextAllocation(long currentCycle)
    {
        return lastAllocation + CALL_INTERVAL;
    }

    public long getRenew(IClientReservation reservation) throws Exception {
        // FIXME: we used to make a synchronous query call to the broker here to find out the
        // advanceTime used by the broker. We can no longer do this here (we risk a deadlock).
        // Modify the broker policy to set advanceTime on the reservation so that 
        // we do not need to make a query to find out.   
        // For now all policies use the same ADVANCE_TIME.
        long newStartCycle = actor.getActorClock().cycle(reservation.getTerm().getEndTime()) + 1;
        return newStartCycle - ADVANCE_TIME - CLOCK_SKEW;        
    }

    /*
     * =====================================================================
     * Getters and Setters
     * =====================================================================
     */

    /**
     * Returns the start time of reservations for an allocation at the
     * given cycle.
     *
     * @param allocationCycle allocation cycle
     *
     * @return the start time for reservation in this allocation cycle
     */
    protected long getStartForAllocation(long allocationCycle)
    {
        return allocationCycle + ADVANCE_TIME;
    }

    protected long getEndForAllocation(long allocationCycle) {
        return allocationCycle + ADVANCE_TIME + allocationHorizon;
    }
    
    /**
     * {@inheritDoc}
     */
    public void prepare(long cycle)
    {
        if (!ready) {
            lastAllocation = cycle - CALL_INTERVAL;
            ready = true;
        }

        try {
            checkPending();
        } catch (Exception e) {
            logger.error("Exception in prepare:", e);
        }
    }

    /**
     * Performs checks on renewing reservations. Updates the terms to
     * suggest new terms, stores the extend on the pending list. Returns a
     * fresh ReservationSet of expiring reservations to try to renew in this
     * bidding cycle.
     *
     * @param renewing collection of the renewing reservations
     * @param pending collection of reservations that are pending
     *
     * @return non-null set of renewals
     *
     */
    protected ReservationSet processRenewing(ReservationSet renewing, ReservationSet pending)
    {
        ReservationSet result = new ReservationSet();

        if (renewing == null) {
            return result;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Expiring = " + renewing.size());
        }

        Iterator<IReservation> i = renewing.iterator();

        while (i.hasNext()) {
            IClientReservation r = (IClientReservation) i.next();

            if (logger.isDebugEnabled()) {
                logger.debug("Expiring res: " + r.toString());
            }

            if (r.isRenewable()) {
                logger.debug("This is a renewable expiring res");

                /*
                 * This reservation is in a renewable state, it will expire, and
                 * we haven't tried to renew it yet. Pick a term and select it
                 * as a candidate.
                 */
                Term term = r.getTerm();

                /*
                 * Extend the term by its previous length
                 */
                term = term.extend();

                // XXX: shouldn't this be setApproved?

                // r.setSuggested(term, r.getResources().abstractClone());
                r.setApproved(term, r.getResources().abstractClone());

                result.add(r);
                calendar.addPending(r);
            } else {
                logger.debug("This is not a renewable expiring res");
            }
        }

        return result;
    }

    /**
     * Returns the <code>ADVANCE_TIME</code> of an agent's allocation
     * in the properties. This is used so that service managers and downstream
     * brokers know how early to bid. If the requested properties is null, the
     * agent returns all of the properties that it has defined.
     * <br>{@inheritDoc}
     *
     * @param properties DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Properties query(Properties properties)
    {
        Properties returnProp = super.query(properties);

        /*
         * If properties is null, return everything about the agent
         */
        if (properties == null) {
            returnProp.put("advanceTime", Long.toString(ADVANCE_TIME));
        } else if (properties.containsKey("advanceTime")) {
            returnProp.put("advanceTime", Long.toString(ADVANCE_TIME));
        }

        return returnProp;
    }

    /*
     * =====================================================================
     * Saving and storing requests
     * =====================================================================
     */

    /*
     * These are request upcalls from the mapper with the manager lock held.
     * Each request is a "bid". Here we simply validate the bids and save them
     * in various calendar structures, pending action by an allocation policy
     * that runs periodically ("auction").
     */

    /**
     * Performs all of the checks and necessary functions to allocate a
     * ticket to a reservation from a source. Performs time shifting of a
     * request's term if allowed, extracts units from the source, and assigns
     * reservation.
     *
     * @param reservation request being allocated resources
     * @param source source request is being allocated from
     * @param resourceShare number of resources being allocated to the request
     * @param startResTime when the ticket for the request starts
     *
     * @throws Exception in case of error
     */
    protected void satisfyAllocation(final IBrokerReservation reservation,
                                     final IClientReservation source, final long resourceShare,
                                     final long startResTime) throws Exception
    {
        ResourceSet mine = null;
        Term approved = getApprovedTerm(reservation);

        /*
         * Shift the requested term to start at the start time
         */
        if (clock.cycle(approved.getNewStartTime()) != startResTime) {
            if (PropertiesManager.isElasticTime(reservation.getRequestedResources())) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Reservation " + reservation.toString() + " was scheduled to start at " +
                        clock.cycle(approved.getNewStartTime()) + " is being shifted to " +
                        startResTime);
                }

                approved = approved.shift(clock.cycleStartDate(startResTime));
            } else {
                String error = "Reservation has a different start time and time shifting is not allowed";
                reservation.fail(error);
            }
        }

        /*
         * Make sure the term end is aligned on a cycle boundary. NOTE: this
         * should also be aligned on call interval boundary!
         */

        // calculate the cycle
        long cycle = clock.cycle(approved.getEndTime());

        // get the date that represents the end of the cycle
        Date alignedEnd = new Date(clock.cycleEndInMillis(cycle));
        // update the term end
        approved.setEndTime(alignedEnd);

        /*
         * Get the ticket with the correct resources
         */
        mine = extractTicket(reservation, source, approved, resourceShare);

        /*
         * Add to the calendar
         */
        addToCalendar(reservation);

        /*
         * Whatever happened up there, this bid is no longer pending. It either
         * succeeded or is now marked failed. (Could/should assert.)
         */
        reservation.setBidPending(false);
    }

    /**
     * Determines if a reservation has switched its source and places
     * the request onto the correct source. This should be rare.
     *
     * @param sources sources for this allocation
     * @param startTime start of the requested tickets
     * @param sourceHash all of the sources for this allocation sorted by type
     *
     * @throws Exception in case of error
     */
    protected void switchSource(final Iterator<IReservation> sources, final long startTime,
                                final Hashtable<ResourceType, IClientReservation> sourceHash)
                         throws Exception
    {
        while (sources.hasNext()) {
            IClientReservation source = (IClientReservation) sources.next();

            ReservationSet extendingForSource = calendar.getRequests(source, startTime);
            Iterator<IReservation> extend = extendingForSource.iterator();

            while (extend.hasNext()) {
                IBrokerReservation r = (IBrokerReservation) extend.next();

                if (!r.getRequestedType().equals(source.getType())) {
                    // this reservation has changed its type
                    IClientReservation otherSource = sourceHash.get(r.getRequestedType());

                    if (otherSource == null) {
                        r.failWarn(
                            "This agent has no resources to satisfy a request for type: " +
                            r.getRequestedType());
                    } else {
                        calendar.removeRequest(source, r);
                        calendar.addRequest(r, startTime, otherSource);
                    }
                }
            }
        }
    }

    /**
     * Ensures that there are sufficient resources to allocate the
     * number of requested resources.
     *
     * @param reservation reservation being allocated
     * @param source source proposed for the requesting reservation
     * @param startTime start of the proposed ticket
     * @param wanted number of resources being requested
     *
     * @return <code>true</code> if there are sufficient resources,
     *         <code>false</code> otherwise
     *
     * @throws Exception in case of error
     */
    protected boolean verifyWantedResources(final IBrokerReservation reservation,
                                            final IClientReservation source, final long startTime,
                                            final int wanted) throws Exception
    {
        // calculate the number of available units
        long available = calculateAvailable(startTime, source);
        int mywanted = wanted;

        if (logger.isDebugEnabled()) {
            logger.debug(
                "ASPPlugin:allocateBids: allocating source rid(" + source.getReservationID().toHashString() +
                ") to newDynamicRes(" + reservation.getReservationID().toHashString() + ") that has props:" +
                reservation.getRequestedResources().getRequestProperties());
        }

        /*
         * Check if we have sufficient resources. If the reservation is flexible
         * adjust its resource demand to what is available.
         */
        if (wanted > available) {
            if (PropertiesManager.isElasticSize(reservation.getRequestedResources()) &&
                    (available > 0)) {
                logger.debug("broker is shrinking an elastic request");
                mywanted = (int) available;
            } else {
                String s = "Selected source holding has insufficient resources: source " + source +
                           " available " + available + "; request " + reservation;
                reservation.failWarn(s);
            }
        }

        /*
         * If the (possibly adjusted) demand fits in what we've got, then try to
         * allocate.
         */
        if (mywanted <= available) {
            return true;
        } else {
            return false;
        }
    }    
}
