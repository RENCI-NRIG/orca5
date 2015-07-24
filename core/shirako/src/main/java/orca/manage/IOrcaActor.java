/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage;

import java.security.cert.Certificate;
import java.util.List;

import orca.manage.beans.EventMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationStateMng;
import orca.manage.beans.SliceMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.ID;

public interface IOrcaActor extends IOrcaComponent
{
	/**
	 * Obtains the actor certificate.
	 * @return
	 */
	public Certificate getCertificate();
	/**
	 * Adds the specified certificate in the actor keystore under the
	 * given alias.
	 * @param certificate
	 * @param alias
	 * @return
	 */
	public boolean registerCertificate(Certificate certificate, String alias);
	/**
	 * Removes the certificate under the given alias from the actor keystore.
	 * @param alias
	 * @return
	 */
	public boolean unregisterCertificate(String alias);
	/**
	 * Obtains the certificate with the specified alias
	 * @param alias
	 * @return
	 */
	public Certificate getCertificate(String alias);
	/**
	 * Obtains all slices.
	 * @return
	 */
	public List<SliceMng> getSlices();
	/**
	 * Obtains the specified slice
	 * @param sliceId
	 * @return
	 */
	public SliceMng getSlice(SliceID sliceId);
	/**
	 * Adds a new slice
	 * @param slice
	 * @return
	 */
	public SliceID addSlice(SliceMng slice);
	/**
	 * Removes the specified slice
	 * @param sliceId
	 * @return
	 */
	public boolean removeSlice(SliceID sliceId);
	/**
	 * Updates the specified slice.
	 * The only updatable slice attributes are:
	 * <ul>
	 * <li>desription</li>
	 * <li>all properties lists</li>
	 * </ul>
	 * @param slice
	 * @return
	 */
	public boolean updateSlice(SliceMng slice);
	/**
	 * Obtains all reservations
	 * @return
	 */
    public List<ReservationMng> getReservations();
    /**
     * Obtains all reservations in the specified state.
     * See OrcaConstants.ReservationState*.
     * @param state
     * @return
     */
    public List<ReservationMng> getReservations(int state);
    /**
     * Obtains all reservations in the specified state.
     * @param sliceID
     * @return
     */
    public List<ReservationMng> getReservations(SliceID sliceID);
    /**
     * Obtains all reservations in the given slice in the specified state.
     * See OrcaConstants.ReservationState*.
     * @param sliceID
     * @param state
     * @return
     */
    public List<ReservationMng> getReservations(SliceID sliceID, int state);
    /**
     * Obtains the specified reservation
     * @param reservationID
     * @return
     */
    public ReservationMng getReservation(ReservationID reservationID);    
    /**
     * Updates the specified reservation
     * The fields that can be updated are:
     * <ul>
     * <li>all properties lists</li>
     * </ul> 
     * @param reservation
     * @return
     */
    public boolean updateReservation(ReservationMng reservation);
    /**
     * Closes the specified reservation
     * @param reservationID
     * @return
     */
    public boolean closeReservation(ReservationID reservationID);
    /**
     * Closes all reservations in the specified slice.
     * @param sliceID
     * @return
     */
    public boolean closeReservations(SliceID sliceID);
    /**
     * Removes the specified reservation.
     * Note only closed reservations can be removed.
     * @param reservationID
     * @return
     */
    public boolean removeReservation(ReservationID reservationID);    
    
    /**
     * Returns the state of the specified reservation.
     * @param reservationID
     * @return
     */
    public ReservationStateMng getReservationState(ReservationID reservationID);
    /**
     * Returns the state of each of the specified reservations.
     * The order in the return list matches the order in the @reservations list.
     * @param reservations
     * @return
     */
    public List<ReservationStateMng> getReservationState(List<ReservationID> reservations);
    /**
     * Returns the name of the actor.
     * @return
     */
    public String getName();
    /**
     * Returns the guid of the actor.
     * @return
     */
    public ID getGuid();
    /**
     * Creates an event subscription.
     * @return the identity of the subscription.
     */
    public ID createEventSubscription();
    /**
     * Deletes the specified event subscription.
     * @param subscriptionID
     * @return
     */
    public boolean deleteEventSubscription(ID subscriptionID);
    /**
     * Drains all events from the specified subscription.
     * @param subscriptionID
     * @param timeout
     * @return
     */
    public List<EventMng> drainEvents(ID subscriptionID, int timeout);
    /**
     * Creates clone of this proxy to make it possible to be used by a separate thread.
     * @return
     */
    public IOrcaActor clone();
}
