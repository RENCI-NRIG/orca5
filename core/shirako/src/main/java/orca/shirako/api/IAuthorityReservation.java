/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import orca.shirako.kernel.ResourceSet;


/**
 * <code>IAuthorityReservation</code> defines the reservation interface for
 * authorities processing requests for resources.
 */
public interface IAuthorityReservation extends IServerReservation
{
    /**
     * Serialization property name: ticket.
     */
    public static final String PropertyTicket = "AuthorityReservationTicket";

    /**
     * Returns the number of concrete units this reservation is short
     * or in excess of.
     *
     * @return number of concrete units this reservation is short or in excess
     *         of
     */
    public int getDeficit();

    /**
     * Returns the ticket backing the reservation.
     *
     * @return ticket
     */
    public ResourceSet getTicket();

    /**
     * Sets the "send with deficit" flag.
     *
     * @param value true, reservations with no pending operations but with a
     *        deficit will be sent back without attempting to fix the deficit,
     *        false - if a deficit exists, the system will attempt to fix it.
     */
    public void setSendWithDeficit(boolean value);
}