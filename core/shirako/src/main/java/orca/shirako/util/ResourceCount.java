/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.util;

import java.util.HashMap;

import orca.util.ResourceType;


/**
 * <code>ResourceCount</code> is a utility class used to count reservations
 * in a <code>ReservationSet</code> based on the reservation state and
 * resource type. An instance of this class is used as an argument to the
 * "tally" methods in <code>ReservationSet</code>.<p>Note: no internal
 * locking.</p>
 *
 * @see ReservationSet
 */
public class ResourceCount
{
    /**
     * Inner class representing counts for a given resource type.
     */
    protected class CountsPerType
    {
        public String type;
        public long active;
        public long pending;
        public long expired;
        public long failed;
        public long closed;

        public CountsPerType(final String type)
        {
            this.type = type;
            active = 0;
            pending = 0;
            expired = 0;
            failed = 0;
            closed = 0;
        }
    }

    /**
     * Counts per resource type.
     */
    protected HashMap<String, CountsPerType> map;

    /**
         * Creates a new instance.
         */
    public ResourceCount()
    {
        map = new HashMap<String, CountsPerType>();
    }

    /**
     * Returns the number of active units from the given resource type.
     *
     * @param type resource type
     *
     * @return number of active units
     */
    public long countActive(final ResourceType type)
    {
        CountsPerType c = getCounts(type);

        if (c != null) {
            return c.active;
        } else {
            return 0L;
        }
    }

    /**
     * Returns the number of closed units from the given resource type.
     *
     * @param type resource type
     *
     * @return number of closed units
     */
    public long countClose(final ResourceType type)
    {
        CountsPerType c = getCounts(type);

        if (c != null) {
            return c.closed;
        } else {
            return 0L;
        }
    }

    /**
     * Returns the number of expired units from the given resource
     * type.
     *
     * @param type resource type
     *
     * @return number of expired units
     */
    public long countExpired(final ResourceType type)
    {
        CountsPerType c = getCounts(type);

        if (c != null) {
            return c.expired;
        } else {
            return 0L;
        }
    }

    /**
     * Returns the number of failed units from the given resource type.
     *
     * @param type resource type
     *
     * @return number of failed units
     */
    public long countFailed(final ResourceType type)
    {
        CountsPerType c = getCounts(type);

        if (c != null) {
            return c.failed;
        } else {
            return 0L;
        }
    }

    /**
     * Returns the number of pending units from the given resource
     * type.
     *
     * @param type resource type
     *
     * @return number of pending units
     */
    public long countPending(final ResourceType type)
    {
        CountsPerType c = getCounts(type);

        if (c != null) {
            return c.pending;
        } else {
            return 0L;
        }
    }

    /**
     * Returns the count entry for the given resource type.
     *
     * @param type resource type
     *
     * @return count entry for the given resource type
     */
    protected CountsPerType getCounts(final ResourceType type)
    {
        return (CountsPerType) map.get(type.getType());
    }

    /**
     * Returns or creates a new count entry for the given resource
     * type.
     *
     * @param type resource type
     *
     * @return count entry
     */
    protected CountsPerType getOrCreateCounts(final ResourceType type)
    {
        CountsPerType result = (CountsPerType) map.get(type.getType());

        if (result == null) {
            result = new CountsPerType(type.getType());
            map.put(type.getType(), result);
        }

        return result;
    }

    /**
     * Increments with <code>count</code> the internal counter for
     * active units of the specified type.
     *
     * @param type resource type
     * @param count DOCUMENT ME!
     */
    public void tallyActive(final ResourceType type, final long count)
    {
        CountsPerType c = getOrCreateCounts(type);
        c.active += count;
    }

    /**
     * Increments with <code>count</code> the internal counter for
     * closed units of the specified type.
     *
     * @param type resource type
     * @param count DOCUMENT ME!
     */
    public void tallyClose(final ResourceType type, final long count)
    {
        CountsPerType c = getOrCreateCounts(type);
        c.closed += count;
    }

    /**
     * Increments with <code>count</code> the internal counter for
     * expired units of the specified type.
     *
     * @param type resource type
     * @param count DOCUMENT ME!
     */
    public void tallyExpired(final ResourceType type, final long count)
    {
        CountsPerType c = getOrCreateCounts(type);
        c.expired += count;
    }

    /**
     * Increments with <code>count</code> the internal counter for
     * failed units of the specified type.
     *
     * @param type resource type
     * @param count DOCUMENT ME!
     */
    public void tallyFailed(final ResourceType type, final long count)
    {
        CountsPerType c = getOrCreateCounts(type);
        c.failed += count;
    }

    /**
     * Increments with <code>count</code> the internal counter for
     * pending units of the specified type.
     *
     * @param type resource type
     * @param count DOCUMENT ME!
     */
    public void tallyPending(final ResourceType type, final long count)
    {
        CountsPerType c = getOrCreateCounts(type);
        c.pending += count;
    }
}
