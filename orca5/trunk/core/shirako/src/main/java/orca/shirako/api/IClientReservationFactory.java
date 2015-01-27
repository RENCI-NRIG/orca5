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

import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;


/**
 * Factory for client reservations.
 */
public interface IClientReservationFactory
{
    /**
     * Creates an "empty" instance of <code>IClientReservation</code>.
     *
     * @return an instance of <code>IClientReservation</code>
     */
    public IClientReservation create();

    /**
     * Creates an "empty" instance of <code>IClientReservation</code>
     * with the specified reservation id.
     *
     * @param rid reservation id
     *
     * @return an instance of <code>IClientReservation</code>
     */
    public IClientReservation create(ReservationID rid);

    /**
     * Creates an instance of <code>IClientReservation</code>.
     *
     * @param rid reservation id
     * @param resources resource set
     * @param term term
     * @param slice slice
     *
     * @return an instance of <code>IClientReservation</code>
     */
    public IClientReservation create(ReservationID rid, ResourceSet resources, Term term,
                                     ISlice slice);

    /**
     * Creates an instance of <code>IClientReservation</code>.
     *
     * @param rid reservation id
     * @param resources resource set
     * @param term term
     * @param slice slice
     * @param broker broker
     *
     * @return an instance of <code>IClientReservation</code>
     */
    public IClientReservation create(ReservationID rid, ResourceSet resources, Term term,
                                     ISlice slice, IBrokerProxy broker);

    /**
     * Creates an instance of <code>IClientReservation</code>.
     *
     * @param resources resource set
     * @param term term
     * @param slice slice
     *
     * @return an instance of <code>IClientReservation</code>
     */
    public IClientReservation create(ResourceSet resources, Term term, ISlice slice);

    /**
     * Creates an instance of <code>IClientReservation</code>.
     *
     * @param resources resource set
     * @param term term
     * @param slice slice
     * @param proxy broker
     *
     * @return an instance of <code>IClientReservation</code>
     */
    public IClientReservation create(ResourceSet resources, Term term, ISlice slice,
                                     IBrokerProxy proxy);

    
    /**
     * Updates the reservation to represent a source for a site resource pool.
     * @param rc
     */
    public void setAsSource(IClientReservation rc);
}