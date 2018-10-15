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

import java.util.Iterator;
import java.util.PriorityQueue;


public class DeadlinePriorityQueue implements QueueWrapper
{
    PriorityQueue<IBrokerReservation> queue;

    public DeadlinePriorityQueue()
    {
        queue = new PriorityQueue<IBrokerReservation>(11, new DeadlineCompare());
    }

    /**
     * Should not use this method - elements are returned in no particular order
     */
    public Iterator<IBrokerReservation> iterator()
    {
        return null;
    }

    public IBrokerReservation firstElement()
    {
        return queue.poll();
    }

    public void add(IBrokerReservation r)
    {
        queue.add(r);
    }

    public void remove(IBrokerReservation r)
    {
        queue.remove(r);
    }

    public boolean contains(IBrokerReservation r)
    {
        return queue.contains(r);
    }

    public int size()
    {
        return queue.size();
    }
}
