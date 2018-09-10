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


public interface IServiceManagerReservationFactory
{
    /**
     * Creates an "empty" instance of
     * <code>IServiceManagerReservation</code>.
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create();

    /**
     * Creates an "empty" instance of
     * <code>IServiceManagerReservation</code> with the specified reservation
     * id.
     *
     * @param rid reservation id
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create(ReservationID rid);

    /**
     * Creates an instance of <code>IServiceManagerReservation</code>.
     *
     * @param rid reservation id
     * @param resources resource set
     * @param term term
     * @param slice slice
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create(ReservationID rid, ResourceSet resources, Term term,
                                             ISlice slice);

    /**
     * Creates an instance of <code>IServiceManagerReservation</code>.
     *
     * @param rid reservation id
     * @param resources resource set
     * @param term term
     * @param slice slice
     * @param broker broker
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create(ReservationID rid, ResourceSet resources, Term term,
                                             ISlice slice, IBrokerProxy broker);

    /**
     * Creates an instance of <code>IServiceManagerReservation</code>.
     *
     * @param resources resource set
     * @param term term term
     * @param resources resources
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create(ResourceSet resources, Term term);

    /**
     * Creates an instance of <code>IServiceManagerReservation</code>.
     *
     * @param resources resource set
     * @param term term
     * @param slice slice
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create(ResourceSet resources, Term term, ISlice slice);

    /**
     * Creates an instance of <code>IServiceManagerReservation</code>.
     *
     * @param resources resource set
     * @param term term
     * @param slice slice
     * @param broker broker
     *
     * @return an instance of <code>IServiceManagerReservation</code>
     */
    public IServiceManagerReservation create(ResourceSet resources, Term term, ISlice slice,
                                             IBrokerProxy broker);
}
