/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.api;

import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;

/**
 * Factory for authority reservations.
 */
public interface IAuthorityReservationFactory
{
    /**
     * Creates a new instance of <code>IAuthorityReservation</code>
     * @param rid reservation identifier
     * @param resources resource set
     * @param term term
     * @param slice slice
     * @return an instance of <code>IAuthorityReservation</code>
     */
    public IAuthorityReservation create(ReservationID rid, ResourceSet resources, Term term, ISlice slice);

    /**
     * Creates a new instance of <code>IAuthorityReservation</code>
     * @param resources resource set
     * @param term term 
     * @param slice slice
     * @return an instance of <code>IAuthorityReservation</code>
     */
    public IAuthorityReservation create(ResourceSet resources, Term term, ISlice slice);
}
