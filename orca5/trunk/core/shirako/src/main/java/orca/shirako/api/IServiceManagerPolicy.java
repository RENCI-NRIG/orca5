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
import orca.shirako.util.ReservationSet;

/**
 * <code>IServiceManagerPolicy</code> defines the policy interface for an actor
 * acting in the service manager role.
 */
public interface IServiceManagerPolicy extends IClientPolicy {
    /**
     * Returns a set of reservations that must be redeemed.
     * 
     * @param cycle
     *            the current cycle
     * 
     * @return reservations to redeem
     */
    public ReservationSet getRedeeming(long cycle);

    /**
     * Checks if the resources and term received in a lease are in compliance
     * with what was initially requested. The policy can prevent the application
     * of the incoming update if it disagrees with it.
     * 
     * @param requestedResources
     *            resources requested from site authority
     * @param actualResources
     *            resources received from site authority
     * @param requestedTerm
     *            term requested from site authority
     * @param actualTerm
     *            term received from site authority
     */
    public void leaseSatisfies(ResourceSet requestedResources, ResourceSet actualResources,
            Term requestedTerm, Term actualTerm) throws Exception;
}