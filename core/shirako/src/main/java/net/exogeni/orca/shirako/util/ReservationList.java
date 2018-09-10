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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * Maintains a list of reservations associated with a cycle number. Allows for
 * efficient retrieval and removal. Groups reservations by the cycle with which
 * they are associated and maintains the reservation sets using a sorted list
 * and a hash table. The hash table is used for quick lookup of a given
 * reservation set. The sorted list allows for efficient reclaiming of sets,
 * which are no longer needed. <br>
 * Cost of operations:
 * <ul>
 * <li>Insert: first insert to a cycle O(log(cycles)), subsequent inserts to an
 * existing cycle: 0(1)</li>
 * <li>Remove: 0(1)</li>
 * <li>Reclaim: 0(k), where k is the number of cycles to reclaim.</li>
 * </ul>
 */
public class ReservationList
{
    /**
     * Internal representation for all reservations associated with a cycle.
     */
    protected class ReservationSetWrapper implements Comparable<ReservationSetWrapper>
    {
        public ReservationSet set;
        public Long cycle;

        public ReservationSetWrapper(ReservationSet set, Long cycle)
        {
            this.set = set;
            this.cycle = cycle;
        }

        public int compareTo(ReservationSetWrapper other)
        {
            return this.cycle.compareTo(other.cycle);
        }
    }

    /**
     * Total number of reservations.
     */
    protected int count;

    /**
     * A sorted list of reservation sets.
     */
    protected ArrayList<ReservationSetWrapper> list;

    /**
     * An index of the existing reservation sets: maps a cycle to a reservation.
     * set.
     */
    protected HashMap<Long, ReservationSet> map;

    /**
     * Maps each reservation to the containing reservation set.
     */
    protected HashMap<ReservationID, Long> reservationToCycle;

    /**
     * Creates a new instance.
     */
    public ReservationList()
    {
        count = 0;
        list = new ArrayList<ReservationSetWrapper>();
        map = new HashMap<Long, ReservationSet>();
        reservationToCycle = new HashMap<ReservationID, Long>();
    }

    /**
     * Adds a reservation associated with a given cycle.
     *
     * @param reservation the reservation
     * @param cycle the cycle with which to associate the reservation
     */
    public void addReservation(IReservation reservation, long cycle)
    {
        if ((reservation == null) || (cycle < 0) || (reservation.getReservationID() == null)) {
            throw new IllegalArgumentException();
        }

        // check if the reservation is already in the list
        Long temp = reservationToCycle.get(reservation.getReservationID());

        if (temp != null) {
            if (temp.equals(cycle)) {
                // this is a repeat of a previous call.
                return;
            } else {
                throw new RuntimeException(
                    "Reservation: #" + reservation.getReservationID().toHashString() +
                    "is already in the list at a different cycle. Please remove it first, before additng to a different cycle");
            }
        }

        Long oCycle = new Long(cycle);
        ReservationSet set = (ReservationSet) map.get(oCycle);

        if (set == null) {
            set = new ReservationSet();
            map.put(oCycle, set);
            addToList(set, oCycle);
        }

        if (!set.contains(reservation)) {
            set.add(reservation);
            count++;
        }

        // update the reservation to cycle map
        reservationToCycle.put(reservation.getReservationID(), oCycle);
    }

    /**
     * Adds an entry to the sorted list
     *
     * @param set The reservation set
     * @param oCycle The cycle
     */
    protected void addToList(ReservationSet set, Long oCycle)
    {
        ReservationSetWrapper entry = new ReservationSetWrapper(set, oCycle);

        int index = Collections.binarySearch(list, entry);

        if (index < 0) {
            list.add(-index - 1, entry);
        }
    }

    /**
     * Returns all reservations associated with cycles up to and including the
     * specified cycle.
     *
     * @param cycle cycle
     *
     * @return a set of reservations associated with the cycles up to and
     *         including specified cycle. Note that removing from the set will
     *         not affect the <code>ReservationList</code>.
     */
    public ReservationSet getAllReservations(long cycle)
    {
        ReservationSet result = new ReservationSet();

        for (int i = 0; i < list.size(); i++) {
            ReservationSetWrapper entry = (ReservationSetWrapper) list.get(i);

            if (entry.cycle.longValue() <= cycle) {
                result.add(entry.set);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Returns all reservations associated with the specified cycle.
     *
     * @param cycle cycle
     *
     * @return a set of reservations associated with the specified cycle. Note
     *         that removing from the set will not affect the
     *         <code>ReservationList</code>.
     */
    public ReservationSet getReservations(long cycle)
    {
        Long oCycle = new Long(cycle);
        ReservationSet set = (ReservationSet) map.get(oCycle);

        if (set == null) {
            set = new ReservationSet();
        } else {
            set = (ReservationSet) set.clone();
        }

        return set;
    }

    /**
     * Removes a reservation from the list.
     * @param reservation reservation to remove
     */
    public void removeReservation(final IReservation reservation)
    {
        ReservationID rid = reservation.getReservationID();
        Long cycle = reservationToCycle.get(rid);

        if (cycle != null) {
            ReservationSet set = (ReservationSet) map.get(cycle);

            if (set != null) {
                set.remove(reservation);
                count--;
            }

            reservationToCycle.remove(rid);
        }
    }

    /**
     * Returns the number of reservations in this collection
     *
     * @return number of reservations in the collection
     */
    public int size()
    {
        return count;
    }

    /**
     * Removes reservations associated with cycles less than or equal to the
     * given cycle.
     *
     * @param cycle cycle
     */
    public void tick(long cycle)
    {
        while (true) {
            if (list.size() > 0) {
                ReservationSetWrapper entry = (ReservationSetWrapper) list.get(0);

                if (entry.cycle.longValue() <= cycle) {
                    list.remove(0);
                    map.remove(entry.cycle);
                    count -= entry.set.size();

                    for (IReservation r : entry.set) {
                        reservationToCycle.remove(r.getReservationID());
                    }

                    entry.set.clear();
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }
}
