/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.IServiceManagerReservationFactory;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.time.Term;


/**
 * Factory for service manager reservations.
 */
public class ServiceManagerReservationFactory implements IServiceManagerReservationFactory
{
    /**
     * Singleton instance.
     */
    private static final ServiceManagerReservationFactory instance = new ServiceManagerReservationFactory();

    /**
     * Returns the factory instance.
     *
     * @return factory instance
     */
    public static IServiceManagerReservationFactory getInstance()
    {
        return instance;
    }

    /**
         * Creates a new instance.
         */
    private ServiceManagerReservationFactory()
    {
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create()
    {
        return new ReservationClient();
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create(final ReservationID rid)
    {
        return new ReservationClient();
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create(final ReservationID rid, final ResourceSet resources,
                                             final Term term, final ISlice slice)
    {
        return new ReservationClient(rid, resources, term, (IKernelSlice) slice);
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create(final ReservationID rid, final ResourceSet resources,
                                             final Term term, final ISlice slice,
                                             final IBrokerProxy broker)
    {
        return new ReservationClient(rid, resources, term, (IKernelSlice) slice, broker);
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create(final ResourceSet resources, final Term term){
        return new ReservationClient(resources, term, (IKernelSlice)null);
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create(final ResourceSet resources, final Term term,
                                             final ISlice slice)
    {
        return new ReservationClient(resources, term, (IKernelSlice) slice);
    }

    /**
     * {@inheritDoc}
     */
    public IServiceManagerReservation create(ResourceSet resources, Term term, ISlice slice,
                                             IBrokerProxy broker)
    {
        return new ReservationClient(resources, term, (IKernelSlice) slice, broker);
    }
}