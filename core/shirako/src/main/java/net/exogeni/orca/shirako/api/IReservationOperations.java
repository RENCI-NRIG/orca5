/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.ReservationSet;


/**
 * <code>IReservationOperations</code> defines the core reservations
 * management operations supported by each Shirako actor.
 */
public interface IReservationOperations
{
    /**
     * Fails the specified reservation.
     * @param rid reservation id
     * @param message message
     * @throws Exception in case of error
     */
    public void fail(ReservationID rid, String message) throws Exception;
    /**
     * Closes the reservation. Note: the reservation must have already
     * been registered with the actor. This method may involve either a client
     * or a server side action or both. For example, on a service manager this
     * call will close the reservation locally and issue a close request to
     * the authority (if the reservation is holding an active lease). When
     * called on a broker, this method will only close the broker reservation.
     *
     * @param reservation reservation to close
     *
     * @throws Exception in case of error
     *
     * @see IReservationOperations#register(IReservation)
     */
    public void close(IReservation reservation) throws Exception;

    /**
     * Closes the reservation. Note: the reservation must have already
     * been registered with the actor. This method may involve either a client
     * or a server side action or both. For example, on a service manager this
     * call will close the reservation locally and issue a close request to
     * the authority (if the reservation is holding an active lease). When
     * called on a broker, this method will only close the broker reservation.
     *
     * @param rid identifier of reservation to close
     *
     * @throws Exception in case of error
     *
     * @see IReservationOperations#register(IReservation)
     */
    public void close(ReservationID rid) throws Exception;

    /**
     * Issues a close for every reservation in the set. All exceptions
     * are caught and logged but no exception is propagated. No information
     * will be delivered to indicate that some failure has taken place, e.g.,
     * failure to communicate with a broker. Inspect the state of individual
     * reservations to determine whether/what failures have taken place.
     *
     * @param reservations set of reservations to close
     *
     * @see IReservationOperations#close(IReservation)
     */
    public void close(ReservationSet reservations);

    /**
     * Extends the reservation. Note: the reservation must have already
     * been registered with the actor. This method may involve either a client
     * or a server side action or both. For example, on a service manager this
     * call will issue an extend ticket request to the reservation's broker,
     * and (once the new ticket arrives) an extend lease with the
     * reservation's authority. When called on a broker, this method will
     * extend the current ticket and send an update back to the client. When
     * called on a site authority, this method will extend the lease and send
     * the new lease back to the service manager.
     *
     * @param reservation reservation to extend
     * @param resources resource set describing the resources desired for the
     *        extension
     * @param term term for extension (must extend the current term)
     *
     * @throws Exception in case of error
     *
     * @see IReservationOperations#register(IReservation)
     */
    public void extend(IReservation reservation, ResourceSet resources, Term term)
                throws Exception;

    /**
     * Extends the reservation. Note: the reservation must have already
     * been registered with the actor. This method may involve either a client
     * or a server side action or both. For example, on a service manager this
     * call will issue an extend ticket request to the reservation's broker,
     * and (once the new ticket arrives) an extend lease to the reservation's
     * authority. When called on a broker, this method will extend the current
     * ticket and send an update back to the client. When called on a site
     * authority, this method will extend the lease and send the new lease
     * back to the service manager.
     *
     * @param rid reservation identifier
     * @param rset resource set representing the resource required for the
     *        extension
     * @param term term for extension (must extend the current term)
     *
     * @throws Exception in case of error
     *
     * @see IReservationOperations#register(IReservation)
     */
    public void extend(ReservationID rid, ResourceSet rset, Term term) throws Exception;

    /**
     * Returns the specified reservation.
     *
     * @param rid reservation id
     *
     * @return reservation
     */
    public IReservation getReservation(ReservationID rid);

    /**
     * Returns all reservations in the given slice.
     *
     * @param sliceID slice identifier
     *
     * @return an array of reservations
     */
    public IReservation[] getReservations(SliceID sliceID);

    /**
     * Registers the reservation with the actor. The reservation must
     * not have been previously registered with the actor: there should be no
     * database record for the reservation.
     *
     * @param reservation reservation to register
     *
     * @throws Exception in case of error
     */
    public void register(IReservation reservation) throws Exception;

    /**
     * Removes the specified reservation. Note: the reservation must
     * have already been registered with the actor. This method will
     * unregister the reservation and remove it from the underlying database.
     * Only closed and failed reservations can be removed.
     *
     * @param reservation reservation to remove
     *
     * @throws Exception if an error occurs or when trying to remove a
     *         reservation that is neither failed or closed.
     *
     * @see IReservationOperations#register(IReservation)
     */
    public void removeReservation(IReservation reservation) throws Exception;

    /**
     * Removes the specified reservation. Note: the reservation must
     * have already been registered with the actor. This method will
     * unregister the reservation and remove it from the underlying database.
     * Only closed and failed reservations can be removed.
     *
     * @param rid identifier of reservation to be removed
     *
     * @throws Exception if an error occurs or when trying to remove a
     *         reservation that is neither failed or closed.
     *
     * @see IReservationOperations#register(IReservation)
     */
    public void removeReservation(ReservationID rid) throws Exception;

    /**
     * Registers a previously registered reservation with the actor.
     * The reservation must have a database record.
     *
     * @param reservation reservation to register
     *
     * @throws Exception in case of error
     */
    public void reregister(IReservation reservation) throws Exception;

    /**
     * Unregisters the reservation with the actor. The reservation's
     * database record will not be removed.
     *
     * @param reservation reservation to unregister
     *
     * @throws Exception in case of error
     */
    public void unregister(IReservation reservation) throws Exception;

    /**
     * Unregisters the reservation with the actor.
     *
     * @param rid identifier of reservation to unregister
     *
     * @throws Exception in case of error
     */
    public void unregister(ReservationID rid) throws Exception;
}
