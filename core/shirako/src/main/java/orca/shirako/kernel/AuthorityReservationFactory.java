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

import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IAuthorityReservationFactory;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.time.Term;


/**
 * Factory for authority reservations.
 */
public class AuthorityReservationFactory implements IAuthorityReservationFactory
{
    /**
     * Singleton instance.
     */
    private static final AuthorityReservationFactory instance = new AuthorityReservationFactory();

    /**
     * Returns the factory instance.
     *
     * @return factory instance
     */
    public static IAuthorityReservationFactory getInstance()
    {
        return instance;
    }

    /**
         * Creates a new instance.
         */
    private AuthorityReservationFactory()
    {
    }

    /**
     * {@inheritDoc}
     */
    public IAuthorityReservation create()
    {
        return new AuthorityReservation();
    }

    /**
     * {@inheritDoc}
     */
    public IAuthorityReservation create(final ReservationID rid, final ResourceSet resources,
                                        final Term term, final ISlice slice)
    {
        return new AuthorityReservation(rid, resources, term, (IKernelSlice) slice);
    }

    /**
     * {@inheritDoc}
     */
    public IAuthorityReservation create(final ResourceSet resources, final Term term, final ISlice slice)
    {
        return new AuthorityReservation(resources, term, (IKernelSlice)slice);
    }
}