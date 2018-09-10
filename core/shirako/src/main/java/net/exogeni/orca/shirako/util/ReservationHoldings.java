/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.util;

import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.util.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * This class maintains a collection of reservations. Each reservation is
 * associated with a validity interval. The class allows to answer intersection
 * queries: what reservations are valid at a given time instance.
 * <p>
 * As time goes by, the class can be purged from irrelevant reservation records
 * by invoking {@link #tick(long)}. Purging is strongly recommended as it
 * reduces the cost of intersection queries.
 * <p>
 * An attempt has been made to optimize the cost of using this data structure.
 * Inserts are O(log(n)). Queries, however, may take between O(log(n)) and O(n).
 */
public class ReservationHoldings
{
    /**
     * Internal class to represent a reservation.
     */
    protected class ReservationWrapper implements Comparable<ReservationWrapper>
    {
        public long start;
        public long end;
        public IReservation reservation;

        public ReservationWrapper(final IReservation reservation, final long start, final long end)
        {
            this.start = start;
            this.end = end;
            this.reservation = reservation;
        }

        public int compareTo(final ReservationWrapper other)
        {
            /*
             * First compare the cycles. If the cycles are the same, break the
             * tie using the reservation id
             */
            int result = 0;

            if (this.end < other.end) {
                return -1;
            } else if (this.end > other.end) {
                return 1;
            }

            if ((this.reservation != null) && (other.reservation != null)) {
                result = this.reservation.getReservationID()
                                         .compareTo(other.reservation.getReservationID());
            }

            return result;
        }
    }

    /**
     * List of reservation wrappers sorted by increasing end time.
     */
    protected ArrayList<ReservationWrapper> list;

    /**
     * All reservations stored in this collection.
     */
    protected ReservationSet set;

    /**
     * Map of reservations to ReservationWrappers. Needed when removing a
     * reservation.
     */
    protected HashMap<ReservationID, ReservationWrapper> map;

    /**
     * Creates a new instance.
     */
    public ReservationHoldings()
    {
        list = new ArrayList<ReservationWrapper>();
        set = new ReservationSet();
        map = new HashMap<ReservationID, ReservationWrapper>();
    }

    /**
     * Adds a reservation to the collection for the specified period of time.
     * The interval is closed on both sides.
     * @param reservation reservation to add
     * @param start start time
     * @param end end time
     */
    public void addReservation(final IReservation reservation, final long start, final long end)
    {
        /*
         * If this is an extended reservation, we may already have it in the
         * list (with potentially different start and end times). Remove the
         * previous entry if this is the case.
         */
        long mystart = start;
        ReservationWrapper entry = map.get(reservation.getReservationID());

        if (entry != null) {
            // make sure the gap is not too big so that it disconnects the lease
            assert ((start - entry.end) <= 1);
            mystart = entry.start;
            removeReservation(reservation);
        }

        // create the new entry
        entry = new ReservationWrapper(reservation, mystart, end);

        // add to the sorted array
        addToList(entry);
        // add to the reservation set
        set.add(reservation);
        // add a map entry
        map.put(reservation.getReservationID(), entry);
    }

    /**
     * Adds the entry to the linked list. Maintains the list in sorted order.
     * Cost: O(log(n)).
     * @param entry entry to add
     */
    protected void addToList(ReservationWrapper entry)
    {
        // find the location
        int index = Collections.binarySearch(list, entry);

        if (index < 0) {
            index = -index - 1;
        }

        list.add(index, entry);
    }

    /**
     * Clears the collection.
     */
    public void clear()
    {
        map.clear();
        list.clear();
        set.clear();
    }

    /**
     * Returns a reservation set containing all reservations present in the
     * collection.
     * @return a reservation set containing all reservations present in the
     *         collection
     */
    public ReservationSet getReservations()
    {
        return (ReservationSet) set.clone();
    }

    /**
     * Performs an intersection query: returns all reservations present in the
     * collection that are active at the specified time instance.
     * @param time time instance
     * @return reservations set containing active reservations
     */
    public ReservationSet getReservations(long time)
    {
        return getReservations(time, null);
    }

    /**
     * Performs an intersection query: returns all reservations from the
     * specified resource type present in the collection that are active at the
     * specified time instance.
     * @param time time instance
     * @param type resource type
     * @return reservations set containing active reservations
     */
    public ReservationSet getReservations(long time, ResourceType type)
    {
        ReservationSet result = new ReservationSet();

        // key element
        ReservationWrapper key = new ReservationWrapper(null, time, time);

        /*
         * Find the location of key in the list.
         */
        int index = Collections.binarySearch(list, key);

        if (index < 0) {
            index = -index - 1;
        }

        /*
         * Scan the upper part of the list. We need to scan the whole list.
         */
        int i = index;
        int count = size();

        while (i < count) {
            ReservationWrapper entry = (ReservationWrapper) list.get(i);

            if ((type == null) || type.equals(entry.reservation.getType())) {
                if ((entry.start <= time) && (entry.end >= time)) {
                    result.add(entry.reservation);
                } else {
                }
            }

            i++;
        }

        /**
         * Scan the lower part of the list until no further intersections are
         * possible
         */
        i = index - 1;

        while (i >= 0) {
            ReservationWrapper entry = (ReservationWrapper) list.get(i);

            if (entry.end < time) {
                // we are done
                break;
            }

            if (entry.start <= time) {
                if ((type == null) || type.equals(entry.reservation.getType())) {
                    result.add(entry.reservation);
                }
            }

            i--;
        }

        return result;
    }

    /**
     * Removes the entry from the linked list. Cost: O(log(n)).
     * @param entry entry to remove
     */
    protected void removeFromList(ReservationWrapper entry)
    {
        // find the location
        int index = Collections.binarySearch(list, entry);

        if (index >= 0) {
            list.remove(index);
        }
    }

    /**
     * Removes a reservation from the collection.
     * @param reservation reservation to remove
     */
    public void removeReservation(final IReservation reservation)
    {
        ReservationWrapper entry = map.get(reservation.getReservationID());

        if (entry != null) {
            map.remove(reservation.getReservationID());
            set.remove(entry.reservation);
            removeFromList(entry);
        }
    }

    /**
     * Returns the size of the collection.
     * @return size of the collection
     */
    public int size()
    {
        return set.size();
    }

    /**
     * Removes all reservations that have end time not after the given cycle.
     * @param time time
     */
    public void tick(final long time)
    {
        while (true) {
            if (list.size() > 0) {
                ReservationWrapper entry = (ReservationWrapper) list.get(0);

                if (entry.end <= time) {
                    list.remove(0);
                    set.remove(entry.reservation);
                    map.remove(entry.reservation.getReservationID());
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }
}
