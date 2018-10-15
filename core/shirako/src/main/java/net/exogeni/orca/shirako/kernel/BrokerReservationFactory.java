/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.kernel;

import net.exogeni.orca.shirako.api.IBrokerReservation;
import net.exogeni.orca.shirako.api.IBrokerReservationFactory;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.time.Term;


/**
 * Factory for broker reservations.
 */
public class BrokerReservationFactory implements IBrokerReservationFactory
{
    /**
     * Singleton instance.
     */
    private static final BrokerReservationFactory instance = new BrokerReservationFactory();

    /**
     * Returns the factory instance.
     *
     * @return factory instance.
     */
    public static IBrokerReservationFactory getInstance()
    {
        return instance;
    }

    /**
         * Creates a new instance.
         */
    private BrokerReservationFactory()
    {
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerReservation create()
    {
        return new BrokerReservation();
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerReservation create(final ReservationID rid, final ResourceSet resources,
                                     final Term term, final ISlice slice)
    {
        return new BrokerReservation(rid, resources, term, (IKernelSlice) slice);
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerReservation create(final ResourceSet resources, final Term term,
                                     final ISlice slice)
    {
        return new BrokerReservation(resources, term, (IKernelSlice) slice);
    }
}
