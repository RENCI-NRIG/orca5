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

import net.exogeni.orca.shirako.kernel.ResourceSet;

/**
 * <code>IAuthorityPolicy</code> defines the policy interface for an actor
 * acting in the authority role.
 */
public interface IAuthorityPolicy extends IServerPolicy {
    /**
     * Accepts inventory resources to be used for allocation of client requests.
     * The policy should add the resources represented by this resource set to
     * its inventory.
     * 
     * @param resources
     *            resource set representing resources to be used for allocation
     * 
     * @throws Exception in case of error
     */
    public void donate(ResourceSet resources) throws Exception;

    /**
     * Ejects resources from the inventory. Resource ejection is unconditional:
     * the policy must remove the specified concrete nodes from its inventory.
     * Any nodes that reside on ejected hosts should be marked as failed. The
     * policy should take no action to destroy those nodes.
     * 
     * @param resources resources to be ejected
     * 
     * @throws Exception in case of error
     */
    public void eject(ResourceSet resources) throws Exception;

    /**
     * Handles a requests to allocate resources for a ticketed reservation. The
     * requested resources can be obtained by calling
     * <code>reservation.getRequestedResources()</code>. The requested lease
     * term can be obtained by calling
     * <code>reservation.getRequestedTerm()</code>. Properties specific to the
     * lease protocol can be obtained by calling
     * <code>reservation.getRequestedResources().getConfigurationProperties()</code>
     * .
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
    public boolean bind(IAuthorityReservation reservation) throws Exception;

    /**
     * Assigns leases to incoming tickets. This method is called by the policy
     * once per cycle. The method should determine whether to perform resource
     * allocation on the given cycle and what requests to consider in that
     * process.
     * 
     * @param cycle
     *            the cycle the authority is making assignment for
     * 
     * @throws Exception in case of error
     */
    public void assign(long cycle) throws Exception;

    /**
     * Informs the policy that a reservation has a deficit and allows the policy
     * to correct the deficit. The policy can attempt to correct the deficit,
     * fail the reservation, or indicate that the reservation should be sent
     * back to the client with the deficit.
     * See {@link IAuthorityReservation#setSendWithDeficit(boolean)}
     * 
     * @param reservation
     *            reservation with deficit
     * 
     * @throws Exception in case of error
     * 
     */
    public void correctDeficit(IAuthorityReservation reservation) throws Exception;

    /**
     * Handles a requests to extend the allocation of previously allocated
     * resources. The requested resources can be obtained by calling
     * <code>reservation.getRequestedResources()</code>. The requested lease
     * term can be obtained by calling
     * <code>reservation.getRequestedTerm()</code>. Properties specific to the
     * lease protocol can be obtained by calling
     * <code>reservation.getRequestedResources().getConfigurationProperties()</code>
     * .
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
    public boolean extend(IAuthorityReservation reservation) throws Exception;

    /**
     * Releases allocated resources that are no longer in use. The set may
     * represent active as well as failed resources. The policy must decide what
     * to do with the released resources. Resources that have been properly
     * closed/terminated are safe to be considered free for future use. Failed
     * resources, however, are problematic. If the policy has no information
     * about the cause of the failure and does not posses the means to recover
     * the failure it should not consider the resources as free. In such cases,
     * and administrator may need to correct the failure manually. When/if the
     * failure is corrected and the resources are safe to be reused, the
     * administrator will issue a call to {@link #freed(ResourceSet)}, which is
     * used to free resources unconditionally.
     * See {@link #freed(ResourceSet)}
     * 
     * @param resources
     *            the resource set to be released
     * 
     * @throws Exception in case of error
     * 
     */
    public void release(ResourceSet resources) throws Exception;

    /**
     * Informs the policy that a set of allocated resources can be considered as
     * free. Most probably these resources represent previously failed
     * resources, which have been repaired by an administrator. The policy must
     * update its data structures to reflect the fact that the incoming
     * resources are no longer in use. The policy should disregard any state
     * information that individual resource units may contain.
     * 
     * @param resources resources
     * 
     * @throws Exception in case of error
     */
    public void freed(ResourceSet resources) throws Exception;

    /**
     * Informs the policy that inventory resources are about to become
     * unavailable. The policy should stop using the concrete resources but
     * should not remove them from its inventory. If the policy currently has
     * active allocations on at least one unit from the concrete resources it
     * should return -1 to indicate that at least one resource cannot be marked
     * unavailable. If the caller still wants to mark a host with hosted nodes
     * as unavailable, the caller may use some administrative interface to move
     * hosts away from the problematic host and then it can retry the operation.
     * Alternatively, the caller may use eject to force the removal of the
     * concrete resources from the policy inventory.
     * 
     * @param resources
     *            set of unavailable inventory resources
     * 
     * @return 0 success, -1, at least one resource has hosted units on it and
     *         cannot be marked unavailable
     * 
     * @throws Exception in case of error
     */
    public int unavailable(ResourceSet resources) throws Exception;

    /**
     * Informs the policy that inventory resources previously marked as
     * unavailable are now available.
     * 
     * @param resources resources to be marked available
     * 
     * @throws Exception in case of error
     */
    public void available(ResourceSet resources) throws Exception;

    /**
     * Informs the policy that inventory resources have failed. This is a new
     * method, which may change in the future.
     * 
     * @param resources
     *            set of failed inventory resources
     */
    public void failed(ResourceSet resources);

    /**
     * Informs the policy that previously failed inventory nodes have been
     * recovered and now are ready to use. This is a new method and may change
     * in the future.
     * 
     * @param resources
     *            set of recovered inventory resources
     */
    public void recovered(ResourceSet resources);

}
