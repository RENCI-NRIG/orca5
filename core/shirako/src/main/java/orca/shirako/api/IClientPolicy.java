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

import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.Bids;

/**
 * <code>IClientPolicy</code> defines the policy interface for an actor acting
 * as a client of another actor (broker or a service manager).
 */
public interface IClientPolicy extends IPolicy {
    /**
     * Injects a new resource demand into the demand stream. The reservation
     * must be pre-initialized with resource set, term, properties, etc. The
     * policy should use this request as an indication that new resources are
     * required. The exact mapping of the request to actual requests for
     * resources is policy-specific. For example, the policy may choose to
     * combine several reservation requests into one, split a reservation
     * request onto multiple brokers, etc.
     * 
     * @param reservation
     *            reservation representing resource demand
     * 
     * @see #formulateBids(long)
     */
    public void demand(IClientReservation reservation);

    /**
     * Formulates bids to the upstream broker(s). The method should determine
     * whether to issue bids in the current cycle. This method should consider
     * the current demand, call broker(s) to obtain necessary information and
     * decide how to distribute its resource demand. When deciding how to bid,
     * also consider any expiring reservation and decide whether to renew and
     * adjust their units. The code should only formulate the bids: the actor
     * will then issue them.
     * <p>
     * Here are some guidelines for implementing this method:
     * <ul>
     * <li>Determine the final demand for each resource type.</li>
     * <li>Obtain policy-specific information from upstream brokers.</li>
     * <li>Determine how to split the demand across the brokers and the
     * currently renewing reservation.</li>
     * <li>Select candidates to request and renew, and prime them with suggested
     * terms, unit counts, and brokers (for new reservations), and specify
     * whether the reservation is renewable or not. Set properties as needed for
     * e.g., economic bidding.</li>
     * <li>Return a ReservationSet of new reservations to be requested, and a
     * ReservationSet of reservations to extend. The returned sets may be empty,
     * but not null.</li>
     * </ul>
     * <br>
     * </p>
     * 
     * @param cycle
     *            The current time
     * 
     * @return Two collections:
     *         <ul>
     *         <li>ticketing - set of new reservations
     *         <li>extending - set of reservations to be extended. Can be null
     *         if no action should be taken
     *         </ul>
     */
    public Bids formulateBids(long cycle) throws Exception;

    /**
     * Checks if the resources and term received in a ticket are in compliance
     * with what was initially requested. The policy can prevent the application
     * of the incoming update if it disagrees with it.
     * 
     * @param requestedResources
     *            resources requested from broker
     * @param actualResources
     *            resources received from broker
     * @param requestedTerm
     *            term requested from broker
     * @param actualTerm
     *            term received from broker
     */
    public void ticketSatisfies(ResourceSet requestedResources, ResourceSet actualResources,
            Term requestedTerm, Term actualTerm) throws Exception;

    /**
     * Notifies the policy that a ticket update operation has completed. The
     * policy may use this upcall to update its internal state.
     * 
     * @param reservation
     *            reservation for which an update ticket operation has completed
     */
    public void updateTicketComplete(IClientReservation reservation) throws Exception;
}