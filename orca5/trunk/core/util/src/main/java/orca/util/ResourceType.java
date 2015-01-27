/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.util;


/**
 * <code>ResourceType</code> is used to name a particular resource type.
 * <code>ResourceType</code> consists of a single string describing the
 * particular resource type. <code>ResourceType</code> is a read-only class:
 * once created it cannot be modified.
 */
public class ResourceType {
    /**
     * The internal representation of the resource type.
     */
	private String type;

	public ResourceType() {
	}
	
    /**
     * Creates a new instance.
     * @param type type name
     */
    public ResourceType(final int type) {
        this(Integer.toString(type));
    }

    /**
     * Creates a new instance.
     * @param type type name
     */
    public ResourceType(final String type) {
        this.type = type;
    }

    
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ResourceType) {
            ResourceType other = (ResourceType) obj;

            return type.equals(other.type);
        }

        return false;
    }

    /**
     * Returns the string representation of this resource type name.
     * @return string representation of the resource type name
     */
    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type;
    }
}
