/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.util;



/**
 * <code>Bid</code> contains the result of running the bidding algorithm
 * for a service manager or broker. It consists of two collections:
 *  <ol>
 *      <li>ticketing: set of new reservations to obtain tickets for</li>
 *      <li>extending: set of existing reservations that need to be
 *      extended</li>
 *  </ol>
 */
public class Bids
{
    /**
     * Ticketing set.
     */
    protected ReservationSet ticketing;

    /**
     * Extending ticket set.
     */
    protected ReservationSet extending;

    /**
         * Creates a new instance.
         * @param ticketing set of ticketing reservations
         * @param extending set of extending reservations
         */
    public Bids(final ReservationSet ticketing, final ReservationSet extending)
    {
        this.ticketing = ticketing;
        this.extending = extending;
    }

    /**
     * Returns the set of reservations for which the policy has decided
     * to extend the existing tickets.
     *
     * @return set of extending reservations
     */
    public ReservationSet getExtending()
    {
        return extending;
    }

    /**
     * Returns the set of reservations for which the policy has decided
     * to obtain new tickets.
     *
     * @return set of ticketing reservations
     */
    public ReservationSet getTicketing()
    {
        return ticketing;
    }
}
