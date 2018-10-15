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

/**
 * <code>IServerPolicy</code> defines the policy interface for an actor acting
 * as a server for another actor (broker or a site authority).
 */
public interface IServerPolicy extends IPolicy {
    /**
     * Allocates resources to all clients who have requested them. This method
     * is called by the policy once per cycle. The method should determine
     * whether to perform resource allocation on the given cycle and what
     * requests to consider in that process.
     * 
     * @param cycle
     *            the cycle for this allocation
     * 
     * @throws Exception in case of error
     */
    public void allocate(long cycle) throws Exception;

    /**
     * Handles an incoming request to allocate resources and issue a ticket for
     * the reservation. The requested resources can be obtained by calling
     * <code>reservation.getRequestedResources()</code>. The requested lease
     * term can be obtained by calling
     * <code>reservation.getRequestedTerm()</code>. Properties specific to the
     * allocation protocol can be obtained by calling
     * <code>reservation.getRequestedResources().getRequestProperties()</code>.
     * <p>
     * If the policy completed processing this request, the functions should
     * return true. If no further intervention is required, e.g., approval by an
     * administrator, the policy should also clear the <code>bidPending</code>
     * flag.
     * </p>
     * <p>
     * The policy may decide to defer the request for a later time. In this case
     * the function should return false and the <code>bidPending</code> flag
     * should remain unchanged.
     * </p>
     * <p>
     * This method may be invoked multiple times for a given reservation, i.e.,
     * if the policy delays the allocation, the system will continue invoking
     * this method at later times until the policy completes processing this
     * request.
     * </p>
     * 
     * @param reservation
     *            reservation to allocate resources for.
     * 
     * @return true, if the request has been fulfilled, false, if the allocation
     *         of resources will be delayed until a later time.
     * @throws Exception in case of error
     */
    public boolean bind(IBrokerReservation reservation) throws Exception;

    /**
     * Accepts ticketed resources to be used for allocation of client requests.
     * The policy should add the resources represented by this reservation to
     * its inventory.
     * <p>
     * <b>Note:</b> This method will be invoked only for resources not directly
     * requested by the policy. For example, exported resources claimed manually
     * by an administrator. The policy is itself responsible to "donate"
     * resources to its inventory when resources it requests become available.
     * </p>
     * 
     * @param r
     *            reservation representing resources to be used for allocation
     * 
     * @throws Exception in case of error
     */
    public void donate(IClientReservation r) throws Exception;

    /**
     * Handles an incoming request to extend previously allocated resources and
     * issue a ticket for the reservation. The requested resources can be
     * obtained by calling <code>reservation.getRequestedResources()</code>.
     * Properties specific to the allocation protocol can be obtained by calling
     * <code>reservation.getRequestedResources().getRequestProperties()</code>.
     * The requested lease term can be obtained by calling
     * <code>reservation.getRequestedTerm()</code>. The new term must extend the
     * currently allocated term.
     * <p>
     * If the policy completed processing this request, the functions should
     * return true. If no further intervention is required, e.g., approval by an
     * administrator, the policy should also clear the <code>bidPending</code>
     * flag.
     * </p>
     * <p>
     * The policy may decide to defer the request for a later time. In this case
     * the function should return false and the <code>bidPending</code> flag
     * should remain unchanged.
     * </p>
     * <p>
     * This method may be invoked multiple times for a given reservation, i.e.,
     * if the policy delays the allocation, the system will continue invoking
     * this method at later times until the policy completes processing this
     * request.
     * </p>
     * <p>
     * While the policy is free to modify the term as it wishes, care must be
     * taken that the client reservation is not closed before the extension is
     * actually granted.
     * </p>
     * 
     * @param r
     *            reservation to allocate resources for.
     * 
     * @return true, if the request has been fulfilled, false, if the allocation
     *         of resources will be delayed until a later time.
     * @throws Exception in case of error
     */
    public boolean extend(IBrokerReservation r) throws Exception;
}
