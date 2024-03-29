/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.common;

import orca.util.ID;


/**
 * <code>SliceID</code> represents the globally unique identifier for a
 * slice.
 */
public class SliceID extends ID
{
    /**
         * Creates a new globally unique slice identifier.
         */
    public SliceID()
    {
    }

    /**
         * Creates a new reservation identifier with the given string representation.
         * @param s string representation of the slice identifier
         */
    public SliceID(final String s)
    {
        super(s);
    }
}