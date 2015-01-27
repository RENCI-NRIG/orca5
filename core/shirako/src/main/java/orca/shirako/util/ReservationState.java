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

import orca.shirako.api.IState;
import orca.shirako.kernel.ReservationStates;


/**
 * Represents the state of a reservation.
 */
public class ReservationState implements IState
{
    /**
     * State.
     */
    protected final int state;

    /**
     * Pending state.
     */
    protected final int pending;

    /**
     * Joining state.
     */
    protected final int joining;

    /**
         * Creates a new instance.
         * @param state state
         * @param pending pending state
         */
    public ReservationState(final int state, final int pending)
    {
        this(state, pending, -1);
    }

    /**
         * Creates a new instance.
         * @param state state
         * @param pending pending state
         * @param joining joining state
         */
    public ReservationState(final int state, final int pending, final int joining)
    {
        this.state = state;
        this.pending = pending;
        this.joining = joining;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        boolean result = false;

        if (other != null) {
            if (other instanceof ReservationState) {
                ReservationState o = (ReservationState) other;
                result = ((o.state == state) && (o.pending == pending) && (o.joining == joining));
            }
        }

        return result;
    }

    /**
     * Returns the joining sub-state.
     *
     * @return joining sub-state
     */
    public int getJoining()
    {
        return joining;
    }

    /**
     * Returns the name of the joining sub-state.
     *
     * @return joining sub-state name
     */
    public String getJoiningName()
    {
        return ReservationStates.getJoiningName(joining);
    }

    /**
     * Returns the pending sub-state.
     *
     * @return pending sub-state
     */
    public int getPending()
    {
        return pending;
    }

    /**
     * Returns the name of the pending sub-state.
     *
     * @return pending sub-state name
     */
    public String getPendingName()
    {
        return ReservationStates.getPendingName(pending);
    }

    /**
     * Returns the state sub-state.
     *
     * @return state sub-state
     */
    public int getState()
    {
        return state;
    }

    /**
     * Returns the name of the state sub-state.
     *
     * @return state sub-state name
     */
    public String getStateName()
    {
        return ReservationStates.getStateName(state);
    }

    public int hashCode()
    {
        return (10 * joining) + (100 * pending) + (1000 * state);
    }

    public String toString()
    {
        if (joining == -1) {
            return "[" + getStateName() + ", " + getPendingName() + "]";
        } else {
            return "[" + getStateName() + ", " + getPendingName() + ", " + getJoiningName() + "]";
        }
    }
}