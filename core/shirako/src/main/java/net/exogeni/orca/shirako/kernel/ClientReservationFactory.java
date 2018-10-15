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

import net.exogeni.orca.shirako.api.IBrokerProxy;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IClientReservationFactory;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.time.Term;


/**
 * Factory for instances of IClientReservationFactory.
 */
public class ClientReservationFactory extends ReservationFactory
    implements IClientReservationFactory
{
    /**
     * The singleton instance.
     */
    private static final ClientReservationFactory instance = new ClientReservationFactory();

    /**
     * Returns the factory instance.
     *
     * @return factory instance
     */
    public static IClientReservationFactory getInstance()
    {
        return instance;
    }

    /**
         * Creates the singleton instance.
         */
    private ClientReservationFactory()
    {
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation create()
    {
        return new ReservationClient();
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation create(final ReservationID rid)
    {
        return new ReservationClient(rid);
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation create(final ReservationID rid, final ResourceSet resources,
                                     final Term term, final ISlice slice)
    {
        return new ReservationClient(rid, resources, term, (IKernelSlice) slice);
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation create(final ReservationID rid, final ResourceSet resources,
                                     final Term term, final ISlice slice, final IBrokerProxy broker)
    {
        return new ReservationClient(rid, resources, term, (IKernelSlice) slice, broker);
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation create(final ResourceSet resources, final Term term,
                                     final ISlice slice)
    {
        return new ReservationClient(resources, term, (IKernelSlice) slice);
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation create(final ResourceSet resources, final Term term,
                                     final ISlice slice, final IBrokerProxy broker)
    {
        return new ReservationClient(resources, term, (IKernelSlice) slice, broker);
    }
    
    public void setAsSource(IClientReservation rc) {
    	((ReservationClient)rc).transition("[source]", ReservationStates.Ticketed, ReservationStates.None);
    }
}
