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


public interface QueueWrapper
{
    /**
     * Return an Iterator
     * @return
     */
    public Iterator<IBrokerReservation> iterator();

    /**
     * Get the first element of the queue
     * @return first element of the queue
     */
    public IBrokerReservation firstElement();

    /**
     * Add an element to the queue
     * @param r new queue element
     */
    public void add(IBrokerReservation r);

    /**
     * Remove an element from a queue
     * @param r element to remove from the queue
     */
    public void remove(IBrokerReservation r);

    /**
     * See if the queue contains this element
     * @param r
     * @return true if it contains the element, false otherwise
     */
    public boolean contains(IBrokerReservation r);

    /**
     * The size of the queue
     * @return size
     */
    public int size();
}