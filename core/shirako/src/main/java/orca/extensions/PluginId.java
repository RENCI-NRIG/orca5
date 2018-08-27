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
 * Identifier for plugins. New instances of this class created using
 * the default constructor generate a GUID. The string constructor generates a
 * <code>PackageId</code> from the given string.
 * <p>
 * This class is safe to be used in hashtables and other indexing data
 * structures.
 * </p>
 */
public class PluginId implements Cloneable
{
    /**
     * The underlying string representing this identifier
     */
    protected String id;

    /**
     * Creates a new globally unique identifier
     */
    public PluginId()
    {
        VMID vmid = new VMID();
        id = vmid.toString();
    }

    /**
     * Loads the specified string as an identifier
     * @param id plugin id
     */
    public PluginId(String id)
    {
        this.id = id;
    }

    /*
     * =======================================================================
     * Overridden methods
     * =======================================================================
     */
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
        if (!(other instanceof PluginId)) {
            return false;
        }

        PluginId otherId = (PluginId) other;

        if (otherId == null) {
            return false;
        }

        return id.equals(otherId.id);
    }

    public Object clone()
    {
        return new PluginId(new String(id));
    }
}
