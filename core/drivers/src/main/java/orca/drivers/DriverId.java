/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers;

import java.rmi.dgc.VMID;

import orca.util.FNVHash;


public class DriverId implements Cloneable
{
    /**
     * The underlying string representing this identifier
     */
    protected String id;

    /**
     * Creates a new globally unique identifier
     */
    public DriverId()
    {
        VMID vmid = new VMID();
        id = vmid.toString();
    }

    /**
     * Loads the specified string as an identifier
     */
    public DriverId(String id)
    {
        this.id = id;
    }

    /*
     * =======================================================================
     * Overridden methods
     * =======================================================================
     */
    public String toHashString()
    {
        return FNVHash.hash(id);
    }

    public String toString()
    {
        return id;
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof DriverId)) {
            return false;
        }

        DriverId otherId = (DriverId) other;

        if (otherId == null) {
            return false;
        }

        return id.equals(otherId.id);
    }

    public Object clone()
    {
        return new DriverId(new String(id));
    }
}
