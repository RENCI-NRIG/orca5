/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import java.util.Date;


/**
 * <code>IReservationStatus</code> defines a set of predicates that can be
 * used to query the state of a reservation.
 */
public interface IReservationStatus
{
    /**
     * Marks an operation failure. Transitions the reservation to the
     * failed state and logs the message as an error. <br>
     * <b>Note</b>Does not throw exception.
     *
     * @param message the error message
     */
    public void fail(String message);

    /**
     * Marks an operation failure. Transitions the reservation to the
     * failed state and logs the message as an error. <br>
     * <b>Note</b>Does not throw exception.
     *
     * @param message the error message
     * @param e The exception
     */
    public void fail(String message, Exception e);

    /**
     * Marks an operation failure. Transitions the reservation to the
     * failed state and logs the message as an error. <br>
     * <b>Note</b>Does not throw exception.
     *
     * @param message the error message
     */
    public void failWarn(String message);

    /**
     * Checks if the reservation is active.
     *
     * @return true if the reservation is active
     */
    public boolean isActive();

    /**
     * Checks if the reservation is activeTicketed.
     *
     * @return true if the reservation is activeTicketed
     */
    public boolean isActiveTicketed();

    /**
     * Checks if the reservation is closed.
     *
     * @return true if the reservation is closed
     */
    public boolean isClosed();

    /**
     * Checks if the reservation is closing.
     *
     * @return true if the reservation pending is closing
     */
    public boolean isClosing();

    /**
     * Checks if the reservation has expired.
     *
     * @return true if the reservation has expired
     */
    public boolean isExpired();

    /**
     * Checks if the reservation expires before time t.
     *
     * @param t target date
     *
     * @return true if the reservation expires before t
     */
    public boolean isExpired(Date t);

    /**
     * Checks if the reservation has extended at least once.
     *
     * @return true if the reservation has extended at least once
     */
    public boolean isExtended();

    /**
     * Checks if the reservation is extending a lease.
     *
     * @return true if the reservation is extending a lease
     */
    public boolean isExtendingLease();

    /**
     * Checks if the reservation is extending a ticket.
     *
     * @return true if the reservation is extending a ticket
     */
    public boolean isExtendingTicket();

    /**
     * Checks if the reservation has failed.
     *
     * @return true if the reservation has failed
     */
    public boolean isFailed();

    /**
     * Checks if the reservation is nascent
     *
     * @return true if the reservation pending is nascent
     */
    public boolean isNascent();

    /**
     * Checks if there is no pending operation.
     *
     * @return true if there is no pending operation
     */
    public boolean isNoPending();

    /**
     * Checks if the reservation is priming.
     *
     * @return true if the reservation is priming
     */
    public boolean isPriming();

    /**
     * Checks if the reservation is redeeming.
     *
     * @return true if the reservation is redeeming
     */
    public boolean isRedeeming();

    /**
     * Checks if the reservation is terminal, e.g., closing, closed, or
     * failed.
     *
     * @return true if the reservation is terminal.
     */
    public boolean isTerminal();

    /**
     * Check if the reservation is ticketed.
     *
     * @return true iff the reservation is ticketed
     */
    public boolean isTicketed();

    /**
     * Checks if the reservation is obtaining a new ticket.
     *
     * @return true if the reservation is obtaining a ticket
     */
    public boolean isTicketing();

    /**
     * Sets the expiration flag.
     *
     * @param value true if the reservation is expired
     */
    public void setExpired(boolean value);
}
