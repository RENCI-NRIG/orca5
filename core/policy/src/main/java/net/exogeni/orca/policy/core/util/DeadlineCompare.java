/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.policy.core.util;

import net.exogeni.orca.shirako.api.IBrokerReservation;

import java.util.Comparator;


/**
 * Compares two reservations with deadlines
 */
public class DeadlineCompare implements Comparator<IBrokerReservation>
{
    public int compare(IBrokerReservation a, IBrokerReservation b)
    {
        long deadlineA = Long.MAX_VALUE;
        long deadlineB = Long.MAX_VALUE;

        if (a.getRequestedResources().getRequestProperties().getProperty("deadline") != null) {
            deadlineA = Long.parseLong(
                a.getRequestedResources().getRequestProperties().getProperty("deadline"));
        }

        if (b.getRequestedResources().getRequestProperties().getProperty("deadline") != null) {
            deadlineB = Long.parseLong(
                b.getRequestedResources().getRequestProperties().getProperty("deadline"));
        }

        return ((Comparable<Long>) deadlineA).compareTo(deadlineB);
    }
}
