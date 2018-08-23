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

import java.util.Iterator;
import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.shirako.api.*;
import orca.shirako.core.PropertiesManager;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.IKernelSlice;
import orca.shirako.time.Term;
import orca.shirako.util.Bids;
import orca.shirako.util.ReservationSet;


/**
 * A simple implementation of a Service Manager policy. This version makes the
 * following assumptions:
 * <ul>
 * <li>1 broker</li>
 * <li>Always bid on only the first open auction</li>
 * <li>Extend all reservations expiring at bidding time, if renewable</li>
 * <li>Close - <code>ADVANCE_CLOSE</code> cycles early</li>
 * </ul>
 */
public class ServiceManagerSimplePolicy extends ServiceManagerCalendarPolicy
{
    /**
     * The amount of time over specific policy decisions the SM must add when
     * communicating with other actors (e.g. redeem() and renew()). Clock skew
     * must be at least one if the SM is ticked after the agent and/or authority
     * At some point in time we may want this to not be static and learn it from
     * what we see in the system, but for now it is static.
     */
    public static final long CLOCK_SKEW = 1;

    /**
     * How far in advance a reservation should initiate the close. This allows
     * for the SM to property close its reservation before the authority does a
     * close on its behalf, which would eliminate any state the SM needs to
     * save.
     */
    protected static final long ADVANCE_CLOSE = 1;

    /**
     * Creates a new instance.
     */
    public ServiceManagerSimplePolicy()
    {
    }

    /**
     * Form bids for expiring reservations and new demands. Return sets of
     * reservations for new bids and renewals.<br>
     * formulateBids is unlocked on SM. Note that a bidding policy never changes
     * the state of reservations, except to suggest terms and brokers. It just
     * returns sets of reservations ready to bid for and extend. It also never
     * changes the membership of any reservation sets maintained by the server,
     * although it does walk through them. <br>
     * {@inheritDoc}
     */
    public Bids formulateBids(final long cycle)
    {
        ReservationSet extending = null;
        ReservationSet bidding = null;

        try {
            extending = processRenewing(cycle);

            /*
             * Select new reservations to bid, and bind to bid and term. Note:
             * here we issue all bids immediately. If we use a different policy,
             * it is our responsibility here to issue bids ahead of their
             * intended start cycles.
             */
            bidding = processDemand(cycle);

            logger.debug("bidForSources: cycle " + cycle + " bids " + bidding.size());
        } catch (Exception e) {
            logger.error("an error in formulateBids: ", e);
        }

        return new Bids(bidding, extending);
    }

    /**
     * Very simple policy - based on <code>ADVANCE_CLOSE</code> <br>
     * {@inheritDoc}
     */
    protected long getClose(final IClientReservation reservation, final Term term)
                     throws Exception
    {
        if (lazyClose) {
            return -1;
        } else {
            long endCycle = actor.getActorClock().cycle(term.getEndTime());

            return endCycle - ADVANCE_CLOSE;
        }
    }

    /**
     * Returns the extension term for a reservation.
     * @param suggestedTerm suggested term
     * @param currentTerm current term
     * @return extension term
     * @throws Exception in case of error
     */
    protected Term getExtendTerm(final Term suggestedTerm, final Term currentTerm)
                          throws Exception
    {
        Term extendTerm = null;

        if (suggestedTerm != null) {
            if (suggestedTerm.extendsTerm(currentTerm)) {
                extendTerm = suggestedTerm;
            } else {
                // extend the current term with the length of the term specified
                // in suggestedTerm;
                long length = suggestedTerm.getLength();
                extendTerm = currentTerm.extend(length);
            }
        } else {
            // Extend the term by its previous length
            extendTerm = currentTerm.extend();
        }

        return extendTerm;
    }

    /**
     * {@inheritDoc}
     */
    protected long getRedeem(IClientReservation reservation) throws Exception
    {
        long newStart = clock.cycle(reservation.getTerm().getNewStartTime());
        long result = newStart - CLOCK_SKEW;

        if (result < actor.getCurrentCycle()) {
            result = actor.getCurrentCycle();
        }

        return result;
    }

    /**
     * Call up to the agent to receive the advanceTime. Do time based on
     * newStart so that requests are aligned. <br>
     * {@inheritDoc}
     */
    public long getRenew(final IClientReservation reservation) throws Exception
    {
        long newStartCycle = actor.getActorClock().cycle(reservation.getTerm().getEndTime()) + 1;
        return newStartCycle - BrokerSimplePolicy.ADVANCE_TIME - CLOCK_SKEW;
    }

    /**
     * {@inheritDoc}
     */
    public void prepare(long cycle)
    {
        try {
            checkPending();
        } catch (Exception e) {
            logger.error("Exception in prepare:", e);
        }
    }

    /**
     * For each newly requested reservation, assigns a term to request, and a
     * broker to bid from.
     * @param cycle cycle
     * @return non-null set of new bids
     * @throws Exception in case of error rare
     */
    protected ReservationSet processDemand(final long cycle) throws Exception
    {
        ReservationSet outgoing = new ReservationSet();

        ReservationSet demand = calendar.getDemand();

        if (demand == null) {
            return new ReservationSet();
        }

        if (logger.isTraceEnabled()){
            for (IReservation reservation : demand){
                IKernelSlice slice = (IKernelSlice) reservation.getSlice();
                for (IReservation sliceReservation : slice.getReservations()){
                    logger.trace("Reservation " + sliceReservation.getReservationID() +
                            " is in state: " + OrcaConstants.getReservationStateName(sliceReservation.getState()));
                }
            }
        }

        IBrokerProxy broker = ((IServiceManager) actor).getDefaultBroker();

        Iterator<IReservation> i = demand.iterator();

        while (i.hasNext()) {
            IServiceManagerReservation r = (IServiceManagerReservation) i.next();
            /*
             * We are about to add a ticketing reservation to the pending list
             */
            onTicket(r);

            if (r.getBroker() == null) {
                r.setBroker(broker);
            }

            ResourceSet rset = r.getSuggestedResources();
            Term term = r.getSuggestedTerm();

            r.setApproved(term, rset);

            outgoing.add(r);
            calendar.addPending(r);
            calendar.removeDemand(r);
        }

        return outgoing;
    }

    /**
     * Returns a fresh ReservationSet of expiring reservations to try to renew
     * in this bidding cycle, and suggest new terms for them.
     * @param cycle cycle
     * @return non-null set of renewals
     * @throws Exception in case of error rare
     */
    protected ReservationSet processRenewing(final long cycle) throws Exception
    {
        ReservationSet result = new ReservationSet();

        ReservationSet renewing = calendar.getRenewing(cycle);

        if ((renewing == null) || (renewing.size() == 0)) {
            return result;
        }

        logger.debug("Renewing = " + renewing.size());

        Iterator<IReservation> i = renewing.iterator();

        while (i.hasNext()) {
            IServiceManagerReservation r = (IServiceManagerReservation) i.next();

            if (logger.isDebugEnabled()) {
                logger.debug("Renewing res: " + r.toString());
            }

            /*
             * Fire the event here, not inside the loop. In this way we give a
             * chance to the subscriber to change the renewable property
             */
            onBeforeExtendTicket(r);

            if (r.isRenewable()) {
                logger.debug("Found a renewable reservation that needs an extension.");

                if (r.isClosed() || r.isClosing() || r.isFailed()) {
                    logger.debug("Found a renewable reservation that is closing/closed/or failed");
                } else {
                    // notify listeners that we are about to extend this
                    // reservation
                    Term suggestedTerm;
                    ResourceSet suggestedResources;

                    suggestedTerm = r.getSuggestedTerm();
                    suggestedResources = r.getSuggestedResources();

                    Term currentTerm = r.getTerm();

                    // for now ignore the suggested resources until we figure
                    // out
                    // how to actually use them
                    ResourceSet approvedResources = r.getResources().abstractClone();
                    Properties p = approvedResources.getRequestProperties();
                    PropertiesManager.setElasticTime(approvedResources, false);

                    // no elastic time for extends:
                    // PropList.setBooleanProperty(p,
                    // ResourceReservation.ElasticTime, false);
                    Term approvedTerm = getExtendTerm(suggestedTerm, currentTerm);

                    if (suggestedResources != null) {
                        approvedResources.setUnits(suggestedResources.getUnits());
                        approvedResources.setType(suggestedResources.getType());
                        approvedResources.getResourceData()
                                         .merge(suggestedResources.getResourceData());
                        PropertiesManager.setElasticTime(approvedResources, false);
                    }

                    r.setApproved(approvedTerm, approvedResources);

                    /*
                     * We are about to add a renewing reservation to the pending
                     * list
                     */
                    onExtendTicket(r);

                    // XXX: canRew is now checked in the kernel
                    // if (r.canRenew()) {
                    result.add(r);
                    calendar.addPending(r);

                    // }
                }
            } else {
                logger.error("A non-renewable reservation is on the renewing list");
            }
        }

        return result;
    }
}
