/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.policy.core.util;

import net.exogeni.orca.util.ID;


public class ResourceEntry implements Comparable
{
    /**
     * Identifier for the resource
     */
    protected ID id;

    /**
     * An array of resource elements
     */
    protected long[] resources;

    public ResourceEntry(long[] resources, ID id)
    {
        this.resources = new long[resources.length];
        System.arraycopy(resources, 0, this.resources, 0, resources.length);
    }

    public ResourceEntry(int numElements, ID id)
    {
        resources = new long[numElements];
        this.id = id;
    }

    /**
     * Creates an array with the amount of space remaining on a machine. Assumes
     * the the ordering of the resources is the same on the request as in this
     * entry
     * @param request request
     * @param multiplier multiplier
     * @return array with the amount of space remaining on a machine
     */
    public long[] space(ResourceEntry request, int multiplier)
    {
        long[] space = new long[resources.length];

        for (int i = 0; i < space.length; i++) {
            space[i] = resources[i] - request.getResources()[i];
        }

        return space;
    }

    /**
     * Determines if a this entry satisfies the resource constraints of another
     * entry. Assumes the the ordering of the resources is the same on the
     * request as in this entry
     * @param request request
     * @param multiplier multiplier
     * @return true or false
     */
    public boolean satisfies(ResourceEntry request, int multiplier)
    {
        int index = 0;

        while (index < resources.length) {
            if ((resources[index] - (request.getResources()[index] * multiplier)) < 0) {
                return false;
            }

            index++;
        }

        return true;
    }

    /**
     * Determine if there could be resources further in the table that could
     * satisfy the request
     * @param request request
     * @param multiplier multiplier
     * @param forward forward
     * @return true or false
     */
    public boolean shouldContinue(ResourceEntry request, int multiplier, boolean forward)
    {
        long[] r = request.getResources();

        if (forward) {
            return true;
        } else {
            for (int i = 0; i < resources.length; i++) {
                if (resources[i] > (r[i] * multiplier)) {
                    return true;
                } else if (resources[i] < (r[i] * multiplier)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Add units to an element
     * @param element element
     * @param units units
     */
    public void addUnits(int element, long units)
    {
        resources[element] = resources[element] + units;
    }

    /**
     * Remove units from an element
     * @param element element
     * @param units units
     */
    public void removeUnits(int element, long units)
    {
        resources[element] = resources[element] - units;
    }

    /**
     * Return the number of available units of a particular resource element
     * @param element element 
     * @return number of available units
     */
    public long getUnits(int element)
    {
        return resources[element];
    }

    /**
     * Returns the resources
     * @return the resources
     */
    public long[] getResources()
    {
        return resources;
    }

    /**
     * Return the resource's id
     * @return id
     */
    public ID getId()
    {
        return id;
    }

    /**
     * Compares this instance to another instance of the same class
     * @param other other
     * @return int
     */
    public int compareTo(Object other)
    {
        ResourceEntry otherEntry = (ResourceEntry) other;
        int size = resources.length;

        for (int i = 0; i < size; i++) {
            if (resources[i] < otherEntry.getResources()[i]) {
                return -1;
            } else if (resources[i] > otherEntry.getResources()[i]) {
                return 1;
            }
        }

        if (this.id.equals(otherEntry.getId())) {
            return -1;
        }

        return 0;
    }

    public String toString()
    {
        String s = id.toHashString() + "--";

        for (int i = 0; i < resources.length; i++) {
            s = s + " " + resources[i];
        }

        return s;
    }
}
