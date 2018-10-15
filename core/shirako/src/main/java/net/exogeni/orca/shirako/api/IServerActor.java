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

import java.security.cert.Certificate;

import net.exogeni.orca.security.AuthToken;

import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.Client;
import net.exogeni.orca.util.ID;


/**
 * <code>IServerActor</code> defines the common functionality for actors
 * acting as servers for other actors (brokers and site authorities).
 */
public interface IServerActor extends IActor, IServerPublic
{
    /**
     * Accepts ticketed resources to be used for allocation of client
     * requests.
     *
     * @param reservation reservation representing resources to be used for
     *        allocation
     *
     * @throws Exception in case of error
     */
    public void donate(IClientReservation reservation) throws Exception;

    /**
     * Exports the resources described by the reservation to the
     * client.
     *
     * @param reservation reservation describing resources to export
     * @param client identity of the client resources will be exported to
     *
     * @throws Exception in case of error
     */
    public void export(IBrokerReservation reservation, AuthToken client) throws Exception;

    /**
     * Exports the specified resources for the given period of time to
     * the client.
     *
     * @param resources resources to export
     * @param term period the export will be valid
     * @param client identity of the client resources will be exported to
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception in case of error
     */
    public ReservationID export(ResourceSet resources, Term term, AuthToken client)
                         throws Exception;
    
    /**
     * Registers a new client slice.
     * @param slice client slice
     * @throws Exception in case of error
     */
    public void registerClientSlice(ISlice slice) throws Exception;
    
    /**
     * Registers the specified client.
     * @param client client to register
     * @param certificate client certificate
     * @throws Exception in case of error
     */
    public void registerClient(final Client client, Certificate certificate) throws Exception;
    /**
     * Unregisters the specified client.
     * @param guid client guid
     * @throws Exception in case of error
     */
    public void unregisterClient(final ID guid) throws Exception;
    
    /**
     * Get a client specified by GUID
     * @param guid guid
     * @return client specified by GUID
     * @throws Exception in case of error
     */
    public Client getClient(final ID guid) throws Exception;
    
    ///////////////////////////////////////////////////////
    /**
	 * Processes an incoming claim request.
	 * @param reservation reservation
	 * @param callback callback
	 * @param caller caller
	 * @throws Exception in case of error
	 */
	public void claim(final IReservation reservation, final IClientCallbackProxy callback, final AuthToken caller)
			throws Exception;

	/**
	 * Processes an incoming ticket request.
	 * @param reservation reservation
	 * @param callback callback
	 * @param caller caller
	 * @throws Exception in case of error
	 */
    public void ticket(final IReservation reservation, final IClientCallbackProxy callback,
            final AuthToken caller) throws Exception;
    
    /**
     * Processes an incoming extendTicket request.
     * @param reservation reservation
     * @param caller caller
     * @throws Exception in case of error
     */
    public void extendTicket(final IReservation reservation, final AuthToken caller)
            throws Exception;
    
    /**
     * Processes an incoming relinquish request.
     * @param reservation reservation
     * @param caller caller
     * @throws Exception in case of error
     */
    public void relinquish(final IReservation reservation, final AuthToken caller) throws Exception;

}
