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

import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.core.ServiceManagerPolicy;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.time.calendar.ServiceManagerCalendar;
import orca.shirako.util.ReservationSet;
import orca.util.OrcaException;
import orca.util.persistence.NotPersistent;


/**
 * The base class for all calendar-based service manager policy
 * implementations.
 */
public abstract class ServiceManagerCalendarPolicy extends ServiceManagerPolicy
{
    /**
     * The calendar of this service manager
     */
	@NotPersistent
    protected ServiceManagerCalendar calendar;

    /**
     * Contains reservations for which we may have completed performing
     * bookkeeping actions but may need to wait for some other event to take
     * place before we raise the corresponding event.
     */
	@NotPersistent
    protected ReservationSet pendingNotify;

    /**
     * If the actor is initialized
     */
	@NotPersistent
    private boolean initialized;

    /**
         * Creates a new instance.
         */
    public ServiceManagerCalendarPolicy()
    {
        this.pendingNotify = new ReservationSet();
    }

    /**
     * Checks pending operations, and installs successfully completed
     * requests in the holdings calendar. Note that the policy module must add
     * bids to the pending set, or they may not install in the calendar.
     *
     * @throws Exception in case of error
     */
    protected void checkPending() throws Exception
    {
        ReservationSet rvset = calendar.getPending();

        if (rvset == null) {
            return;
        }

        Iterator<IReservation> i = rvset.iterator();

        while (i.hasNext()) {
            IServiceManagerReservation r = (IServiceManagerReservation) i.next();

            if (r.isFailed()) {
                /*
                 * This reservation has failed. Remove it from the list. This is
                 * a separate case, because we may fail but not satisfy the
                 * condition of the else statement.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing failed reservation from the pending list: " + r);
                }

                calendar.removePending(r);
                pendingNotify.remove(r);
            } else if (r.isNoPending() && !r.isPendingRecover()) {
                /*
                 * No pending operation and we are not about the reissue a
                 * recovery operation on this reservation.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("SlottedSM pending request completed " + r.toLogString());
                }

                if (r.getState() == ReservationStates.Closed) {
                    /*
                     * Just completed closing the lease
                     */
                     // this case is handled by
                     // closed(IReservation);
                } else if (r.isActiveTicketed()) {
                    /*
                     * An active reservation extended its ticket.
                     */

                    // cancel the current close
                    calendar.removeClosing(r);
                    // schedule a new close
                    calendar.addClosing(r, getClose(r, r.getTerm()));

                    /*
                     * Add from start to end instead of close. It is possible
                     * that holdings may not accurately reflect the actual
                     * number of resources towards the end of a lease. This is
                     * because we assume that we still have resources even after
                     * an advanceClose. When looking at this value, see if the
                     * reservation has closed.
                     */

                    // calendar.addHolding(r, getClose(r,
                    // r.getLeasedTerm()), getClose(r, r.getTerm()));
                    calendar.addHolding(r, r.getTerm().getNewStartTime(), r.getTerm().getEndTime());
                    calendar.addRedeeming(r, getRedeem(r));

                    if (r.isRenewable()) {
                        long cycle = getRenew(r);
                        r.setRenewTime(cycle);
                        /*
                         * mark as dirty so that the renew time gets committed
                         * to the database
                         */
                        r.setDirty();
                        calendar.addRenewing(r, cycle);
                    }

                    // Notify subscribers
                    onExtendTicketComplete(r);
                    pendingNotify.remove(r);
                } else if (r.isTicketed()) {
                    /*
                     * The reservation obtained a ticket for the first time
                     */
                    calendar.addHolding(r, r.getTerm().getNewStartTime(), r.getTerm().getEndTime());
                    calendar.addRedeeming(r, getRedeem(r));

                    calendar.addClosing(r, getClose(r, r.getTerm()));

                    if (r.isRenewable()) {
                        long cycle = getRenew(r);
                        r.setRenewTime(cycle);
                        r.setDirty();
                        calendar.addRenewing(r, cycle);
                    }

                    // Notify Subscribers
                    onTicketComplete(r);
                    pendingNotify.remove(r);
                } else if (r.getState() == ReservationStates.Active) {
                    if (pendingNotify.contains(r)) {
                        /*
                         * We are waiting for transfer in operations to complete
                         * so that we can raise the lease complete event.
                         */
                        if (r.isActiveJoined()) {
                            if (r.isExtended()) {
                                onExtendLeaseComplete(r);
                            } else {
                                onLeaseComplete(r);
                            }

                            pendingNotify.remove(r);
                        }
                    } else {
                        /*
                         * Just completed a lease call (redeem or extendLease).
                         * We need to remove this reservation from closing,
                         * because we added it using r.getTerm(), and add this
                         * reservation to closing using r.getLeasedTerm() [the
                         * site could have changed the term of the reservation].
                         * This assumes that r.getTerm has not changed in the
                         * mean time. This is true now, since the state machine
                         * does not allow more than one pending operation.
                         * Should we change this, we will need to update the
                         * code below.
                         */
                        calendar.removeClosing(r);

                        calendar.addClosing(r, getClose(r, r.getLeaseTerm()));

                        if (r.getRenewTime() == 0) {
                            /*
                             * This is a recovered reservation for which we did
                             * not have the renew time at the time of recovery.
                             * For now the policy is to issue renew on the next
                             * cycle.
                             */
                            r.setRenewTime(actor.getCurrentCycle() + 1);
                            r.setDirty();
                            calendar.addRenewing(r, r.getRenewTime());
                        }

                        if (r.isActiveJoined()) {
                            if (r.isExtended()) {
                                onExtendLeaseComplete(r);
                            } else {
                                onLeaseComplete(r);
                            }
                        } else {
                            /*
                             * add to the pending notify list so that we can
                             * raise the event when transfer in operations
                             * complete.
                             */
                            pendingNotify.add(r);
                        }
                    }
                } else if ((r.getState() == ReservationStates.CloseWait) ||
                               (r.getState() == ReservationStates.Failed)) {
                    pendingNotify.remove(r);
                } else {
                    logger.warn(
                        "Invalid state on reservation. We may be still recovering: " +
                        r.toString());

                    continue;
                }

                if (!pendingNotify.contains(r)) {
                    logger.debug("Removing from pending: " + r);
                    calendar.removePending(r);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close(final IReservation reservation)
    {
        // raise the onClose event
        onClose(reservation);
        /* ignore any scheduled/in progress operations */
        calendar.removeScheduledOrInProgress(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public void closed(final IReservation reservation)
    {
        /* remove the reservation from all calendar structures */
        calendar.removeHolding(reservation);
        calendar.removeRedeeming(reservation);
        calendar.removeRenewing(reservation);
        calendar.removeClosing(reservation);
        pendingNotify.remove(reservation);
        // raise the onCloseComplete event
        onCloseComplete(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public void demand(final IClientReservation reservation)
    {
        if (!reservation.isNascent()) {
            logger.error("demand reservation is not fresh");
        } else {
            calendar.addDemand(reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extend(final IReservation reservation, final ResourceSet resources, final Term term)
    {
        /* cancel any previously scheduled extends */
        calendar.removeRenewing(reservation);
        /* do not cancel the close: the extend may fail */
        /* cancel any pending redeem: we will redeem after the extension */
        calendar.removeRedeeming(reservation);

        /*
         * There should be no pending operations for this reservation at this
         * time
         */

        /*
         * Add to the pending list so that we can track the progress of the
         * reservation.
         */
        calendar.addPending(reservation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish(final long cycle)
    {
        super.finish(cycle);
        calendar.tick(cycle);
    }

    /**
     * Returns the time that a reservation should be closed.
     *
     * @param reservation reservation
     * @param term term
     *
     * @return the close time of the reservation (cycle)
     *
     * @throws Exception in case of error
     */
    protected abstract long getClose(IClientReservation reservation, Term term)
                              throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationSet getClosing(long cycle)
    {
        ReservationSet result = new ReservationSet();
        ReservationSet closing = calendar.getClosing(cycle);
        Iterator<IReservation> i = closing.iterator();

        while (i.hasNext()) {
            IReservation r = i.next();

            if (!r.isFailed()) {
                calendar.addPending(r);
                result.add(r);
            } else {
                logger.warn("Removing failed reservation from the closing list: " + r);
            }
        }

        return result;
    }

    /**
     * Returns the time when the reservation should be redeemed.
     *
     * @param reservation the reservation
     *
     * @return the redeem time of the reservation (cycle)
     *
     * @throws Exception in case of error
     */
    protected abstract long getRedeem(final IClientReservation reservation)
                               throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationSet getRedeeming(final long cycle)
    {
        ReservationSet redeeming = calendar.getRedeeming(cycle);
        Iterator<IReservation> i = redeeming.iterator();

        while (i.hasNext()) {
            IReservation r = i.next();

            if (r.isActiveTicketed()) {
                onExtendLease(r);
                calendar.addPending(r);
            } else {
                onLease(r);
                calendar.addPending(r);
            }
        }

        return redeeming;
    }

    /**
     * Returns the time when the reservation should be renewed.
     *
     * @param reservation the reservation
     *
     * @return the renew time of the reservation (cycle)
     *
     * @throws Exception in case of error
     */
    protected abstract long getRenew(final IClientReservation reservation)
                              throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws OrcaException
    {
        if (!initialized) {
            super.initialize();
            this.calendar = new ServiceManagerCalendar(clock);
            initialized = true;
        }
    }

    /**
     * Checks if the reservation has expired.
     *
     * @param r reservation to check
     *
     * @return true or false
     */
    protected boolean isExpired(final IReservation r)
    {
        Term t = r.getTerm();
        long end = clock.cycle(t.getEndTime());

        return (actor.getCurrentCycle() > end);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(final IReservation reservation)
    {
        /* remove the reservation from the calendar */
        calendar.remove(reservation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revisit(final IReservation reservation) throws Exception
    {
        super.revisit(reservation);

        IServiceManagerReservation r = (IServiceManagerReservation) reservation;

        switch (r.getState()) {
            case ReservationStates.Nascent:
                calendar.addPending(r);

                break;

            case ReservationStates.Ticketed:

                switch (r.getPendingState()) {
                    case ReservationStates.None:

                        /* pending list */
                        if (r.isPendingRecover()) {
                            calendar.addPending(reservation);
                        } else {
                            /* calculate redeem time and schedule */
                            calendar.addRedeeming(r, getRedeem(r));
                        }

                        /* holdings */
                        calendar.addHolding(r, r.getTerm().getNewStartTime(),
                                            r.getTerm().getEndTime());
                        /* calculate close time and schedule */
                        calendar.addClosing(r, getClose(r, r.getTerm()));

                        /* schedule for renewal */
                        if (r.isRenewable()) {
                            // r.setRenewTime(getRenew(r));
                            /*
                             * Scheduling renewal is a bit tricky, since it may
                             * involve communication with the upstream broker.
                             * However, in some recovery cases, typical in one
                             * container deployment, the broker and the service
                             * manager will be recovering at the same time. In
                             * this case the query may fail and we will have to
                             * fail the reservation.
                             */

                            /*
                             * Our approach here is as follows: we cache the
                             * renew time in the reservation class and persist
                             * it in the database. When we recover, we will
                             * check the renewTime field of the reservation if
                             * it is non-zero, we will use it, otherwise we will
                             * schedule the renew after we get the lease back
                             * from the authority.
                             */
                            if (r.getRenewTime() != 0) {
                                calendar.addRenewing(r, r.getRenewTime());
                            } // else {
                              // see checkPending for the else part.
                              // }
                        }

                        break;

                    case ReservationStates.Redeeming:
                        throw new Exception("This state should not be reached during recovery");
                }

                break;

            case ReservationStates.Active:

                switch (r.getPendingState()) {
                    case ReservationStates.None:

                        /* pending list */
                        if (r.isPendingRecover()) {
                            calendar.addPending(r);
                        }

                        /* renewing */
                        if (r.isRenewable()) {
                            // r.setRenewTime(getRenew(r));
                            assert r.getRenewTime() != 0;
                            calendar.addRenewing(r, r.getRenewTime());
                        }

                        /* holdings */
                        calendar.addHolding(r, r.getTerm().getNewStartTime(),
                                            r.getTerm().getEndTime());
                        /* closing */
                        calendar.addClosing(r, getClose(r, r.getLeaseTerm()));

                        break;

                    case ReservationStates.ExtendingTicket:
                        throw new Exception("This state should not be reached during recovery");
                }

                break;

            case ReservationStates.ActiveTicketed:

                switch (r.getPendingState()) {
                    case ReservationStates.None:

                        if (r.isPendingRecover()) {
                            calendar.addPending(r);
                        } else {
                            calendar.addRedeeming(r, getRedeem(r));
                        }

                        /* holdings */
                        calendar.addHolding(r, r.getTerm().getNewStartTime(),
                                            r.getTerm().getEndTime());
                        /* close time */
                        calendar.addClosing(r, getClose(r, r.getTerm()));

                        /* renew time */
                        if (r.isRenewable()) {
                            assert r.getRenewTime() != 0;
                            calendar.addRenewing(r, r.getRenewTime());
                        }

                        break;

                    case ReservationStates.ExtendingLease:
                        throw new Exception("This state should not be reached during recovery");
                }

                break;
        }
    }
}
