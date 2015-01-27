/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.util.ReservationState;
import orca.util.ResourceType;
import orca.util.persistence.CustomRecoverable;
import orca.util.persistence.Persistable;
import orca.util.persistence.Referenceable;


/**
 * <code>IReservation</code> defines the the core API for a Shirako
 * reservation. Most of the methods described in the interface allow the
 * programmer to inspect the state of the reservation, access some of its core
 * objects, and wait for the occurrence of a particular event.
 */
public interface IReservation extends Persistable, CustomRecoverable, Referenceable, IReservationStatus, IReservationResources
{
    /*
     * Reservation categories constants.
     */

    /**
     * Unspecified reservation category.
     */
    public static final int CategoryAll = 0;

    /**
     * Client-side reservations.
     */
    public static final int CategoryClient = 1;

    /**
     * Broker-side reservations.
     */
    public static final int CategoryBroker = 2;

    /**
     * Site authority-side reservations.
     */
    public static final int CategoryAuthority = 3;

    /**
     * Serialization property name: reservation identifier.
     */
    public static final String PropertyID = "ReservationID";

    /**
     * Serialization property name: reservation category.
     */
    public static final String PropertyCategory = "ReservationCategory";

    /**
     * Serialization property name: reservation slice name.
     */
    public static final String PropertySlice = "ReservationSliceName";

    /**
     * Serialization property name: term.
     */
    public static final String PropertyTerm = "ReservationTerm";

    /**
     * Serialization property name: resource set.
     */
    public static final String PropertyResources = "ReservationResources";

	/**
	 * Serialization property name: reservation state.
	 */
	public static final String PropertyState = "ReservationState";

	/**
	 * Serialization property name: reservation pending state.
	 */
	public static final String PropertyPending = "ReservationPending";
    String PropertyStateJoined = "ReservationClientStateJoined";

    /**
     * Marks that the reservation has no uncommitted updates or state
     * transitions.
     */
    public void clearDirty();

    /**
     * Returns the actor in control of the reservation.
     *
     * @return the actor in control of the reservation
     */
    public IActor getActor();

    /**
     * Returns the reservation category.
     *
     * @return reservation category
     */
    public int getCategory();

    /**
     * Returns the current pending reservation state.
     *
     * @return current pending reservation state
     */
    public int getPendingState();

    /**
     * Returns the name of the current pending reservation state.
     *
     * @return name of current pending reservation state
     */
    public String getPendingStateName();
    /**
     * Returns the reservation identifier.
     *
     * @return reservation identifier
     */
    public ReservationID getReservationID();

    /**
     * Returns the current composite reservation state.
     *
     * @return composite reservation state
     */
    public ReservationState getReservationState();

    /**
     * Returns the slice the reservation belongs to.
     *
     * @return slice the reservation belongs to
     */
    public ISlice getSlice();

    /**
     * Returns the slice GUID. Use this method for reservations that
     * have not been fully recovered (revisit() has not yet been invoked).
     *
     * @return slice guid
     */
    public SliceID getSliceID();

    /**
     * Returns the name of the slice the reservation belongs to. se
     * this method for reservations that have not been fully recovered
     * (revisit() has not yet been invoked).
     *
     * @return slice name
     */
    public String getSliceName();

    /**
     * Returns the current reservation state.
     *
     * @return current reservation state
     */
    public int getState();

    /**
     * Returns the name of the current reservation state.
     *
     * @return name of current reservation state
     */
    public String getStateName();

    /**
     * Returns the resource type allocated to the reservation. If no
     * resources have yet been allocated to the reservation, this method will
     * return null.
     *
     * @return resource type allocated to the reservation. null if no resources
     *         have been allocated to the reservation.
     */
    public ResourceType getType();

    /**
     * Checks if the reservation has uncommitted state transitions.
     *
     * @return true if the reservation has an uncommitted transition
     */
    public boolean hasUncommittedTransition();

    /**
     * Checks if the reservation has uncommitted updates.
     *
     * @return true if the reservation has uncommitted updates
     */
    public boolean isDirty();

    /**
     * Checks if a recovery operation is in progress for the
     * reservation.
     *
     * @return true if a recovery operation for the reservation is in progress
     */
    public boolean isPendingRecover();

    /**
     * Marks the reservation as containing uncommitted updates.
     */
    public void setDirty();

    /**
     * Indicates if a recovery operation for the reservation is/is
     * going to be in progress.
     *
     * @param pendingRecover true, a recovery operation is in progress, false -
     *        no recovery operation is in progress.
     */
    public void setPendingRecover(boolean pendingRecover);

    /**
     * Sets the slice the reservation belongs to.
     *
     * @param slice slice the reservation belongs to
     */
    public void setSlice(ISlice slice);

    /**
     * Construct a String useful for outputting to the log.
     * 
     * At a minimum; hash all guids, explode all nested properties
     * 
     * @return loggable string
     */
	public String toLogString();
	
    public void setLocalProperty(String key, String value);
    
    public String getLocalProperty(String key);
    
    /**
     * Returns the error message associated with this reservation.
     * @return
     */
    public String getNotices();
    public void setup();

    /**
     * Transitions this reservation into a new state.
     *
     * @param prefix DOCUMENT ME!
     * @param state the new state
     * @param pending if reservation is pending
     */
    public void transition(String prefix, int state, int pending);
}