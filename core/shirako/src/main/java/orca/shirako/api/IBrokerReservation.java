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


/**
 * <code>IBrokerReservation</code> defines the reservation interface for
 * brokers processing requests for resources.
 */
public interface IBrokerReservation extends IServerReservation
{
    /**
     * Returns a proxy to the authority in control of the resources
     * represented by the reservation.
     *
     * @return authority proxy
     */
    public IAuthorityProxy getAuthority();

    /**
     * Returns source for this reservation. For optional use by policy
     * to track where it filled this reservation from, e.g., for use on
     * extends.
     *
     * @return the source reservation
     */
    public IClientReservation getSource();

    /**
     * Checks if the reservation was closed while it was in the Priming
     * state. Reservations closed in the priming state have a resources field
     * that does not accurately represent the last allocation. This method is
     * intended for use by policy classes when processing the closed() event.
     * If the policy class does not keep track what resources it last
     * allocated to a given reservation, the policy class must then use this
     * method to determine where the information about allocated resources for
     * the reservation is stored. If this method returns true, the last
     * allocation, before the current update, will be in
     * <code>getPreviousResources()</code>, and the current update that was
     * applied to the reservation will be in
     * <code>getApprovedResources()</code>
     *
     * @return true if the reservation was closed while in the Priming state
     */
    public boolean isClosedInPriming();

    /**
     * Sets the source for this reservation. For optional use by policy
     * to track where it filled this reservation from, e.g., for use on
     * extends.
     *
     * @param source the source reservation.
     */
    public void setSource(IClientReservation source);
}