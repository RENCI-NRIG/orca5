/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core.util;

import orca.shirako.api.IBrokerReservation;

import java.util.Iterator;
import java.util.Vector;


public class FIFOQueue implements QueueWrapper
{
    protected Vector<IBrokerReservation> queue;

    public FIFOQueue()
    {
        queue = new Vector<IBrokerReservation>();
    }

    public Iterator<IBrokerReservation> iterator()
    {
        return queue.iterator();
    }

    public IBrokerReservation firstElement()
    {
        return queue.firstElement();
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