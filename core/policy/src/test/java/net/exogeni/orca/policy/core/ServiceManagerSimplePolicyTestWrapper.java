/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.policy.core;

import net.exogeni.orca.shirako.api.IClientReservation;


public class ServiceManagerSimplePolicyTestWrapper extends ServiceManagerSimplePolicy
{
    @Override
    public long getRenew(final IClientReservation reservation) throws Exception
    {
        // renew as soon as the term becomes active
        return clock.cycle(reservation.getTerm().getNewStartTime());
    }
}
