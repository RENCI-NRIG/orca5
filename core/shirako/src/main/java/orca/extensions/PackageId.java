/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.extensions;

import java.rmi.dgc.VMID;


/**
 * Identifier for extension packages. New instances of this class created using
 * the default constructor generate a GUID. The string constructor generates a
 * <code>PackageId</code> from the given string.
 * <p>
 * This class is safe to be used in hashtables and other indexing data
 * structures.
 * </p>
 */
public class PackageId implements Cloneable
{
    /**
     * The underlying string representing this identifier
     */
    protected String id;

    /**
     * Creates a new globally unique identifier
     */
    public PackageId()
    {
        VMID vmid = new VMID();
        id = vmid.toString();
    }

    /**
     * Loads the specified string as an identifier
     */
    public PackageId(String id)
    {
        this.id = id;
    }

    /*
     * =======================================================================
     * Overridden methods
     * =======================================================================
     */

    /**
     * Returns the string representation of the identifier.
     * @return string representation of the identifier
     */
    public String toString()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof PackageId)) {
            return false;
        }

        PackageId otherId = (PackageId) other;

        if (otherId == null) {
            return false;
        }

        return id.equals(otherId.id);
    }

    @Override
    public Object clone()
    {
        return new PackageId(new String(id));
    }
}