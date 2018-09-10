/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.manage;

import java.security.cert.Certificate;
import java.util.List;

import net.exogeni.orca.manage.beans.EventMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ReservationStateMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;

public interface IOrcaActor extends IOrcaComponent
{
	/**
	 * Obtains the actor certificate.
	 * @return returns  the actor certificate
	 */
	public Certificate getCertificate();
	/**
	 * Adds the specified certificate in the actor keystore under the
	 * given alias.
	 * @param certificate certificate to be registered
	 * @param alias alias
	 * @return true for success; false otherwise
	 */
	public boolean registerCertificate(Certificate certificate, String alias);
	/**
	 * Removes the certificate under the given alias from the actor keystore.
	 * @param alias alias
	 * @return true for success; false otherwise
	 */
	public boolean unregisterCertificate(String alias);
	/**
	 * Obtains the certificate with the specified alias
	 * @param alias alias
	 * @return returns the certifciate
	 */
	public Certificate getCertificate(String alias);
	/**
	 * Obtains all slices.
	 * @return returns list of all the slices
	 */
	public List<SliceMng> getSlices();
	/**
	 * Obtains the specified slice
	 * @param sliceId slice id
	 * @return returns the specified slice
	 */
	public SliceMng getSlice(SliceID sliceId);
	/**
	 * Adds a new slice
	 * @param slice slice
	 * @return returns slice id
	 */
	public SliceID addSlice(SliceMng slice);
	/**
	 * Removes the specified slice
	 * @param sliceId slice id
	 * @return true for success; false otherwise
	 */
	public boolean removeSlice(SliceID sliceId);
	/**
	 * Updates the specified slice.
	 * The only updatable slice attributes are:
	 * <ul>
	 * <li>desription</li>
	 * <li>all properties lists</li>
	 * </ul>
	 * @param slice slice 
	 * @return true for success; false otherwise
	 */
	public boolean updateSlice(SliceMng slice);
	/**
	 * Obtains all reservations
	 * @return returns list of the reservations
	 */
    public List<ReservationMng> getReservations();
    /**
     * Obtains all reservations in the specified state.
     * See OrcaConstants.ReservationState*.
     * @param state state
     * @return list of the reservations in the specified state
     */
    public List<ReservationMng> getReservations(int state);
    /**
     * Obtains all reservations in the specified slice.
     * @param sliceID slice ID
     * @return list of the reservations for the specific slice
     */
    public List<ReservationMng> getReservations(SliceID sliceID);
    /**
     * Obtains all reservations in the given slice in the specified state.
     * See OrcaConstants.ReservationState*.
     * @param sliceID slice id
     * @param state state
     * @return list of the reservations for specific slice in specific state
     */
    public List<ReservationMng> getReservations(SliceID sliceID, int state);
    /**
     * Obtains the specified reservation
     * @param reservationID reservation id
     * @return returns the reservation identified by id
     */
    public ReservationMng getReservation(ReservationID reservationID);    
    /**
     * Updates the specified reservation
     * The fields that can be updated are:
     * <ul>
     * <li>all properties lists</li>
     * </ul> 
     * @param reservation reservation to be updated
     * @return true for success; false otherwise
     */
    public boolean updateReservation(ReservationMng reservation);
    /**
     * Closes the specified reservation
     * @param reservationID reservation id
     * @return true for success; false otherwise
     */
    public boolean closeReservation(ReservationID reservationID);
    /**
     * Closes all reservations in the specified slice.
     * @param sliceID slice ID
     * @return true for success; false otherwise
     */
    public boolean closeReservations(SliceID sliceID);
    /**
     * Removes the specified reservation.
     * Note only closed reservations can be removed.
     * @param reservationID reservation id of the reservation to be removed
     * @return true for success; false otherwise
     */
    public boolean removeReservation(ReservationID reservationID);    
    
    /**
     * Returns the state of the specified reservation.
     * @param reservationID reservation id
     * @return returns the state of the specific reservation
     */
    public ReservationStateMng getReservationState(ReservationID reservationID);
    /**
     * Returns the state of each of the specified reservations.
     * The order in the return list matches the order in the @reservations list.
     * @param reservations list of reservations
     * @return list of state of the specified reservations
     */
    public List<ReservationStateMng> getReservationState(List<ReservationID> reservations);
    /**
     * Returns the name of the actor.
     * @return returns name of the actor
     */
    public String getName();
    /**
     * Returns the guid of the actor.
     * @return returns guid of the actor
     */
    public ID getGuid();
    /**
     * Creates an event subscription.
     * @return the identity of the subscription.
     */
    public ID createEventSubscription();
    /**
     * Deletes the specified event subscription.
     * @param subscriptionID subscription id
     * @return true for success; false otherwise
     */
    public boolean deleteEventSubscription(ID subscriptionID);
    /**
     * Drains all events from the specified subscription.
     * @param subscriptionID subscription id
     * @param timeout timeout
     * @return list of the events drained out
     */
    public List<EventMng> drainEvents(ID subscriptionID, int timeout);
    /**
     * Creates clone of this proxy to make it possible to be used by a separate thread.
     * @return returns the cloned actor
     */
    public IOrcaActor clone();
}
