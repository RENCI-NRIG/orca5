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

import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;


/**
 * Factory for broker reservations.
 */
public interface IBrokerReservationFactory
{
    /**
     * Creates a new instance of <code>IBrokerReservation</code>
     *
     * @param rid reservation identifier
     * @param resources resource set
     * @param term term
     * @param slice slice
     *
     * @return an instance of <code>IBrokerReservation</code>
     */
    public IBrokerReservation create(ReservationID rid, ResourceSet resources, Term term,
                                     ISlice slice);

    /**
     * Creates a new instance of <code>IBrokerReservation</code>
     *
     * @param resources resource set
     * @param term term
     * @param slice slice
     *
     * @return an instance of <code>IBrokerReservation</code>
     */
    public IBrokerReservation create(ResourceSet resources, Term term, ISlice slice);
}
