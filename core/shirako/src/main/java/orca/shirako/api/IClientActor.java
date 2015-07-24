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

import java.util.Properties;

import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.util.ReservationSet;
import orca.util.ID;

/**
 * <code>IClientActor</code> defines the common functionality for actors acting
 * as clients of other actors (service managers and brokers). Every client actor
 * is connected to one or more server actors. Each server actor is represented
 * as a proxy object. Client actors maintain a registry of proxies to server
 * actors that they are connected to. Proxies to actors acting in the broker
 * role are explicitly managed. Proxies for actors acting in the site authority
 * role are automatically managed as they are embedded in tickets sent from
 * brokers.
 */
public interface IClientActor extends IActor, IClientPublic {
	/**
	 * Registers a broker. If this is the first broker to be registered, it is
	 * set as the default broker.
	 * 
	 * @param broker
	 *            broker to register
	 */
	public void addBroker(IBrokerProxy broker);

	/**
	 * Claims already exported resources from the default upstream broker. The
	 * reservation will be stored in the default slice.
	 * 
	 * @param reservationID
	 *            reservation identifier of the exported reservation
	 * @param resources
	 *            resource set describing the resources to claim
	 * 
	 * @return DOCUMENT ME!
	 */
	public IClientReservation claim(ReservationID reservationID, ResourceSet resources) throws Exception;

	/**
	 * Claims already exported resources from the given broker. The reservation
	 * will be stored in the default slice.
	 * 
	 * @param reservationID
	 *            reservation identifier of the exported reservation
	 * @param resources
	 *            resource set describing the resources to claim
	 * @param broker
	 *            broker proxy
	 * 
	 * @return DOCUMENT ME!
	 */
	public IClientReservation claim(ReservationID reservationID, ResourceSet resources, IBrokerProxy broker)
			throws Exception;

	/**
	 * Claims already exported resources from the given broker.
	 * 
	 * @param reservationID
	 *            reservation identifier of the exported reservation
	 * @param resources
	 *            resource set describing the resources to claim
	 * @param slice
	 *            slice in which to store the claimed reservation. As of now, it
	 *            is assumed that the exported reservation is stored under a
	 *            slice with the same name in the upstream broker.
	 * @param broker
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public IClientReservation claim(ReservationID reservationID, ResourceSet resources, ISlice slice,
			IBrokerProxy broker) throws Exception;

	public void demand(ReservationID reservation) throws Exception;
	/**
	 * Issues a ticket extend request for the given reservation. Note: the
	 * reservation must have already been registered with the actor.
	 * 
	 * @param reservation
	 *            reservation to extend the ticket for
	 * 
	 * @throws Exception
	 * 
	 * @see IReservationOperations#register(orca.shirako.kernel.Reservation)
	 */
	public void extendTicket(IClientReservation reservation) throws Exception;

	/**
	 * Issues a ticket request for every reservation in the set. All exceptions
	 * are caught and logged but no exception is propagated. No information will
	 * be delivered to indicate that some failure has taken place, e.g., failure
	 * to communicate with a broker. Inspect the state of individual
	 * reservations to determine whether/what failures have taken place.
	 * 
	 * @param set
	 *            set of reservations to extend tickets for
	 */
	public void extendTicket(ReservationSet set);

	/**
	 * Gets the broker proxy with the given guid
	 * 
	 * @param guid
	 *            broker guid
	 * 
	 * @return requested broker
	 */
	public IBrokerProxy getBroker(ID guid);

	/*
	 * =======================================================================
	 * Functions for brokers.
	 * =======================================================================
	 */

	/**
	 * Returns all brokers registered with the actor.
	 * 
	 * @return an array of brokers
	 */
	public IBrokerProxy[] getBrokers();

	/**
	 * Returns the default broker.
	 * 
	 * @return the default broker
	 */
	public IBrokerProxy getDefaultBroker();

	/**
	 * Issues a ticket request for the given reservation. Note: the reservation
	 * must have already been registered with the actor.
	 * 
	 * @param reservation
	 *            reservation to obtain a ticket for
	 * 
	 * @throws Exception
	 * 
	 * @see IReservationOperations#register(orca.shirako.kernel.Reservation)
	 */
	public void ticket(IClientReservation reservation) throws Exception;

	/**
	 * Issues a ticket request for every reservation in the set. All exceptions
	 * are caught and logged but no exception is propagated. No information will
	 * be delivered to indicate that some failure has taken place, e.g., failure
	 * to communicate with a broker. Inspect the state of individual
	 * reservations to determine whether/what failures have taken place.
	 * 
	 * @param set
	 *            set of reservations to obtain tickets for
	 */
	public void ticket(ReservationSet set);
	
	/**
     * Issue modify request for given reservation. Note: the reservation
     * must have already been registered with the actor.
     *
     * @param reservationID reservationID for the reservation to modify
     * @param modifyProperties property list for modify
     * @throws Exception
     */
    public void modify(ReservationID reservationID, Properties modifyProperties) throws Exception;
	
}