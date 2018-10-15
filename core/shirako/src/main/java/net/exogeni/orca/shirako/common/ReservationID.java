/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.common;

import net.exogeni.orca.util.ID;


/**
 * <code>ReservationID</code> represents the globally unique identifier for
 * a reservation.
 */
public class ReservationID extends ID
{
    /**
         * Creates a new globally unique reservation identifier.
         */
    public ReservationID()
    {
    }

    /**
         * Creates a new reservation identifier with the given string representation.
         * @param s string representation of the reservation identifier
         */
    public ReservationID(final String s)
    {
        super(s);
    }
}
