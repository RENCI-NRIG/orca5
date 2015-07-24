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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ReservationStates;


/**
 * <code>ReservationSet</code> is a collection of reservations indexed by
 * <code>ReservationID</code>.
 */
public class ReservationSet implements Cloneable, Iterable<IReservation>
{
    /**
     * The internal hash map.
     */
    protected LinkedHashMap<ReservationID, IReservation> reservations;

    /**
     * Crates a new set
     */
    public ReservationSet()
    {
        reservations = new LinkedHashMap<ReservationID, IReservation>();
    }

    /**
     * Creates a new set using the specified set.
     * @param set set to clone
     */
    @SuppressWarnings("unchecked")
    private ReservationSet(final ReservationSet set)
    {
        this.reservations = (LinkedHashMap<ReservationID, IReservation>) set.reservations.clone();
    }

    /**
     * Adds the reservation to the set.
     *
     * @param reservation the reservation to add
     */
    public void add(final IReservation reservation)
    {
        reservations.put(reservation.getReservationID(), reservation);
    }

    /**
     * Adds the given reservation set to the reservation set.
     *
     * @param set the set to add
     */
    public void add(final ReservationSet set)
    {
        for (IReservation r : set) {
            reservations.put(r.getReservationID(), r);
        }
    }

    /**
     * Removes all reservations from the set.
     */
    public void clear()
    {
        reservations.clear();
    }

    /**
     * Creates a clone of the set.
     *
     * @return DOCUMENT ME!
     */
    public ReservationSet clone()
    {
        return new ReservationSet(this);
    }

    /**
     * Checks if the reservation is part of the set.
     *
     * @param reservation reservation to check
     *
     * @return true if the set contains the specified reservation; false otherwise
     */
    public boolean contains(final IReservation reservation)
    {
        return reservations.containsKey(reservation.getReservationID());
    }

    /**
     * Checks if the set contains a reservation with the specified
     * identifier.
     *
     * @param rid reservation identifier
     *
     * @return true if the set contains the specified reservation; false otherwise
     */
    public boolean contains(final ReservationID rid)
    {
        return reservations.containsKey(rid);
    }

    /**
     * Tallies up resources in the ReservationSet. Note: "just a hint"
     * unless kernel lock is held.
     *
     * @param rc holder for counts
     * @param when date relative to which to do the counting
     */
    public void count(final ResourceCount rc, final Date when)
    {
        Iterator<IReservation> i = iterator();

        while (i.hasNext()) {
            IReservation r = i.next();
            r.count(rc, when);
        }
    }

    /**
     * Retrieves a reservation from the set.
     *
     * @param rid reservation identifier
     *
     * @return DOCUMENT ME!
     */
    public IReservation get(final ReservationID rid)
    {
        return reservations.get(rid);
    }

    /**
     * Returns the specified reservation. If the reservation is not
     * present in the set, throws an exception.
     *
     * @param rid the reservation identifier
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception if the requested reservation is not present in the set
     */
    public IReservation getException(final ReservationID rid) throws Exception
    {
        IReservation r = (IReservation) reservations.get(rid);

        if (r == null) {
            throw new Exception("No reservation with ID " + rid);
        }

        return r;
    }

    /**
     * Checks if the set is empty.
     *
     * @return true if the set is empty
     */
    public boolean isEmpty()
    {
        return reservations.isEmpty();
    }

    /**
     * Returns an iterator for the set.
     *
     * @return an iterator for the set
     */
    public Iterator<IReservation> iterator()
    {
        return reservations.values().iterator();
    }

    /**
     * Removes the specified reservation.
     *
     * @param reservation reservation to remove
     */
    public void remove(final IReservation reservation)
    {
        reservations.remove(reservation.getReservationID());
    }

    /**
     * Returns the number of reservations in the set.
     *
     * @return the size of the reservation set
     */
    public int size()
    {
        return reservations.size();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("");

        Iterator<?> i = this.iterator();

        while (i.hasNext()) {
            IReservation r = (IReservation) i.next();
            sb.append(r.toString() + "; ");
        }

        return sb.toString();
    }
    
    public String toLogString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("");

        Iterator<?> i = this.iterator();

        while (i.hasNext()) {
            IReservation r = (IReservation) i.next();
            sb.append(r.toLogString() + "; ");
        }

        return sb.toString();
    }

    /**
     * Creates a string of all reservations in the set, which are in
     * the given state.
     *
     * @param prefix prefix to prepend
     * @param state state reservation must be in
     * @param when time instance
     *
     * @return a string representation of the reservations in the set, which
     *         are in the given state
     */
    public String toStringByState(final String prefix, final int state, final Date when)
    {
        int count = 0;
        Iterator<IReservation> i = iterator();
        StringBuffer sb = new StringBuffer();
        sb.append(prefix + " in state " + state + ": ");

        while (i.hasNext()) {
            IReservation r = i.next();

            if (r.getState() == state) {
                String rids = (r.getReservationID() != null) ? r.getReservationID().toHashString()
                                                             : "anon";
                sb.append(
                    r.getResources().getUnits() + "/" + r.getResources().getConcreteUnits(when) +
                    "(#" + rids + ", " + r.getTerm().toString() + ") ");
                count = count + 1;
            }
        }

        if (count == 0) {
            return "";
        } else {
            return sb.toString() + "\n";
        }
    }

    /**
     * Creates a string of all reservations in the set grouped by
     * reservation state.
     *
     * @param prefix prefix to prepend to each state summary
     * @param when time instance
     *
     * @return a string representation of the reservations in the set
     */
    public String toStringSummaryByState(final String prefix, final Date when)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("");

        for (int state = ReservationStates.Nascent; state < ReservationStates.Failed; state++) {
            sb.append(toStringByState(prefix, state, when));
        }

        return sb.toString();
    }
}