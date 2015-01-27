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
import orca.shirako.util.ResourceCount;
import orca.util.ResourceType;

import java.util.Date;


/**
 * <code>IReservationResources</code> defines the API for resources associated
 * with a reservation. Each reservation has a number of resource sets associated
 * with it:
 * <ul>
 * <li>requested: resources that have been requested either to an upstream
 * actor or by a downstream actor. </li>
 * <li> approved: resources that have been approved either for making a request
 * to an upstream actor, or to be sent back to a downstream actor to satisfy its
 * request. </li>
 * <li> resources: the resources currently bound to the reservation </li>
 * <li>previousResources: the previous resource set bound to the reservation</li>
 * <li>leasedResources: the concrete resources bound to the reservation.</li>
 * </ul>
 */
public interface IReservationResources
{
    /*
     * =======================================================================
     * Counting
     * =======================================================================
     */

    /**
     * Counts the number of resources in the reservation relative to
     * the specified time. The <code>ResourceCount</code> object is updated
     * with the count of active, pending, expired, failed, etc. units.<p><b>Note:</b>
     * "just a hint" unless the kernel lock is held.</p>
     *
     * @param rc holder for counts
     * @param time time instance
     */
    public void count(ResourceCount rc, Date time);

    /**
     * Returns the resources approved for this reservation by the last
     * policy decision. If the policy has never made a decision about the
     * reservation, this method will return null.
     *
     * @return resources last approved for the reservation. null if no
     *         resources have ever been approved.
     */
    public ResourceSet getApprovedResources();

    /**
     * Returns the term approved for the reservation by the last policy
     * decision. If the policy has never made a decision about the
     * reservation, this method will return null.
     *
     * @return term last approved for the reservation. null if no resources
     *         have ever been approved.
     */
    public Term getApprovedTerm();

    /**
     * Returns the resource type approved for this reservation by the
     * last policy decision. If the policy has never made a decision about the
     * reservation, this method will return null.
     *
     * @return resource type last approved for the reservation. null if no
     *         resources have ever been approved.
     */
    public ResourceType getApprovedType();

    /**
     * Returns the number of units approved by the last policy
     * decision. If the policy has never made a decision about this
     * reservation, the method will return 0.
     *
     * @return number of units approved by the last policy decision
     */
    public int getApprovedUnits();

    /**
     * Returns the number of abstract units leased by the reservation.
     * If the reservation does not represent leased resources or has not yet
     * leased any resources, e.g., holds only a ticket, the method will return
     * 0.
     *
     * @return number of abstract units leased
     */
    public int getLeasedAbstractUnits();

    /**
     * Returns the number of concrete units leased by the reservation.
     * If the reservation does not represent leased resources or has not yet
     * leased any resources, e.g., holds only a ticket, the method will return
     * 0.<p><b>Note:</b> This call will always return 0 for
     * reservations that have not recreated their concrete sets, e.g.,
     * reservations fetched from the database as a result of a query. For such
     * reservations use #getLeasedAbstractUnits() or obtain the actual
     * reservation object.</p>
     *
     * @return number of leased units
     */
    public int getLeasedUnits();

    /*
     * =======================================================================
     * Getters and Setters
     * =======================================================================
     */

    /**
     * Returns the resources represented by/allocated to the
     * reservation at the time before the last update. Can be null.
     *
     * @return resource represented by the reservation at the time before the
     *         last update. Can be null.
     */
    public ResourceSet getPreviousResources();

    /**
     * Returns the previously allocated term for the reservation. Can
     * be null.
     *
     * @return previously allocated term. null if reservation has not yet been
     *         extended.
     */
    public Term getPreviousTerm();

    /**
     * Returns the resources requested for the reservation. If the
     * kernel has not yet issued the resource request this method will return
     * null.
     *
     * @return resources requested for the reservation. null if no request has
     *         been made yet.
     */
    public ResourceSet getRequestedResources();

    /**
     * Returns the last requested term. If the kernel has not yet
     * issued the resource request this method will return null.
     *
     * @return last requested term. null if no request has been made yet.
     */
    public Term getRequestedTerm();

    /**
     * Returns the requested resource type.
     *
     * @return requested resource type
     */
    public ResourceType getRequestedType();

    /**
     * Returns the number of requested units. If no units have yet been
     * requested, the method will return 0.
     *
     * @return number of requested units
     */
    public int getRequestedUnits();

    /**
     * Returns the resources represented by/allocated to the
     * reservation. If no resources have yet been allocated to the
     * reservation, this method will return null.
     *
     * @return resources represented by the reservation. null if no resources
     *         have been allocated to the reservation.
     */
    public ResourceSet getResources();

    /**
     * Returns the currently allocated term for the reservation. If no
     * resources have yet been allocated, this method will return null.
     *
     * @return currently allocated term. null if resources have not yet been
     *         allocated.
     */
    public Term getTerm();

    /**
     * Returns the currently assigned resource units. If the
     * reservation has not yet been assigned units, the method will return 0.
     * For extended reservations this method will return the number of units
     * from the latest extension. In case of tickets, this number may
     * represent resources in the future and may be different from the number
     * of units from before the extension. To obtain the number of units at a
     * given point in time, use {@link #getUnits(Date)}.
     *
     * @return number of assigned/allocated units
     */
    public int getUnits();

    /**
     * Returns the number of units assigned to the reservation at the
     * specific time instance. If the time instance falls outside of the
     * reservation term, this method will return 0.
     *
     * @param when time instance
     *
     * @return number of units
     */
    public int getUnits(Date when);

    /**
     * Checks if the policy has made a decision for the reservation.
     *
     * @return true if the policy has made a decision for the reservation
     */
    public boolean isApproved();

    /**
     * Indicates that the policy completed making its decisions about
     * the reservation. Sets the approved flag. This flag is used when
     * performing unit counts. If the flag is set, the number of units in the
     * approved resource set will be counted as pending. Failure to set this
     * flag will only affect resource counts.
     */
    public void setApproved();

    /**
     * Sets the term and resources approved for the reservation. This
     * method should be called by the actor policy after it determines the
     * resources and term for the reservation. The method also sets the
     * approved flag.
     *
     * @param approvedTerm term the policy approved
     * @param approvedResources resources the policy approved
     */
    public void setApproved(Term approvedTerm, ResourceSet approvedResources);

    /**
     * Sets the resources approved for the reservation. This method
     * should be called by the actor policy after it determines the resources
     * for the reservation. This method will not set the approved flag. Once
     * all approval decisions are complete, {@link #setApproved()} must be
     * invoked.
     *
     * @param approvedResources resources the policy approved
     *
     * @see #setApproved()
     * @see #setApprovedTerm(Term)
     * @see #setApproved(Term, ResourceSet)
     */
    public void setApprovedResources(ResourceSet approvedResources);

    /**
     * Sets the term approved for the reservation. This method should
     * be called by the actor policy after it determines the term for the
     * reservation. This method will not set the approved flag. Once all
     * approval decisions are complete, {@link #setApproved()} must be
     * invoked.
     *
     * @param term approved term
     *
     * @see #setApproved()
     * @see #setApprovedResources(ResourceSet)
     * @see #setApproved(Term, ResourceSet)
     */
    public void setApprovedTerm(Term term);
}