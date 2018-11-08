/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core;

import orca.policy.core.util.DeadlinePriorityQueue;
import orca.policy.core.util.FIFOQueue;
import orca.policy.core.util.QueueWrapper;
import orca.policy.core.util.RequestTypePriority;

import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.common.ConfigurationException;
import orca.shirako.util.ReservationSet;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;


/**
 * <code>BrokerriorityPolicy</code> allocates requests based on requestType
 * priorities set in the configuration at the broker. There may be multiple
 * requestTypes with the same priority. Within each priority class, requests are
 * allocated FIFO. Within each priority class the policy gives priority to
 * extending reservations followed by new reservations.
 */
public class BrokerPriorityPolicy extends BrokerSimplePolicy
{
    public static final String PropertyRequestTypeCount = "requestType.count";
    public static final String PropertyRequestTypeName = "requestType.name";
    public static final String PropertyRequestTypePriority = "requestType.priority";
    public static final String PropertyQueueType = "queue.type";
    
    public static final String QueueTypeNone = "none";
    public static final String QueueTypeFifo = "fifo";
    public static final String QueueTypePriority = "priority";
    
    public static final String QueueThreshold = "queueThreshold";
    
    @NotPersistent
    protected Vector<RequestTypePriority> requestTypePriorities;
    @NotPersistent
    protected QueueWrapper queue;
    
    /**
     * Creates a new instance.
     */
    public BrokerPriorityPolicy()
    {
        super();
        requestTypePriorities = new Vector<RequestTypePriority>();
    }

    /**
     * Adds a new request type.
     * @param priority priority
     * @param requestType request type
     */
    public void addRequestType(final int priority, final String requestType)
    {
        if (requestTypePriorities.size() == 0) {
            RequestTypePriority r = new RequestTypePriority(priority, requestType);
            requestTypePriorities.add(r);
        } else {
            int location = 0;
            boolean addRequestTypePriority = true;

            for (RequestTypePriority rtp : requestTypePriorities) {
                if (rtp.getPriority() == priority) {
                    rtp.addRequestType(requestType);
                    addRequestTypePriority = false;
                } else if (rtp.getPriority() < priority) {
                    break;
                }

                location++;
            }

            if (addRequestTypePriority) {
                RequestTypePriority r = new RequestTypePriority(priority, requestType);
                requestTypePriorities.add(location, r);
            }
        }
    }

    /**
     * Allows additional configuration of priority between multiple types of
     * requests as specified in <code>REQUEST_TYPE</code>. For example,
     * requests for "master" nodes may be given priority over "worker" requests.
     * {@inheritDoc}
     */
    public void allocate(final long cycle) throws Exception
    {
        /*
         * This method should first decide whether to run an allocation on the
         * current cycle. If the answer is yes, then it has to decide on the
         * start time of reservations it is going to allocate.
         */
        if (getNextAllocation(cycle) != cycle) {
            logger.debug("Next allocation cycle " + getNextAllocation(cycle) + " != " + cycle);

            return;
        }

        lastAllocation = cycle;

        /*
         * Determine the cycle for which the agent is allocating resources
         */
        long startTime = getStartForAllocation(cycle);

        ReservationSet allBids = calendar.getRequests(startTime);

        /*
         * If there are no extending and no new requests - return
         */
        if ((allBids == null) || (allBids.size() == 0)) {
            logger.debug("No requests for allocation start cycle " + startTime);

            return;
        }

        logger.debug("There are " + allBids.size() + " bids for cycle " + startTime);

        /*
         * Put source on a Hashtable for the new request allocation
         */
        ReservationSet holdings = calendar.getHoldings(clock.cycleStartDate(startTime));
        Hashtable<ResourceType, IClientReservation> sourceHash = createSourceHashtable(
            holdings.iterator());

        /*
         * Move extending reservations that have changed their types to an
         * appropriate source
         */
        switchSource(holdings.iterator(), startTime, sourceHash);

        for (RequestTypePriority rtp : requestTypePriorities) {
            /*
             * Allocate extending requests
             */
            allocateExtending(holdings.iterator(), startTime, rtp.getRequestTypes());

            /*
             * Allocate the remaining bids
             */
            allocateNewBids(allBids.iterator(), sourceHash, startTime, rtp.getRequestTypes());
        }
    }

    /**
     * Processes a list of configuration properties
     * @param p p
     * @throws Exception in case of error
     */
    @Override
    public void configure(final Properties p) throws Exception
    {
        super.configure(p);
        int count = 0;

        if (p.getProperty(PropertyRequestTypeCount) != null) {
            count = Integer.valueOf(p.getProperty(PropertyRequestTypeCount));
        }

        for (int i = 0; i < count; i++) {
            String prioProperty = PropertyRequestTypePriority + "." + i;
            String typeProperty = PropertyRequestTypeName + "." + i;
            int priority = Integer.valueOf(p.getProperty(prioProperty));
            String requestType = p.getProperty(typeProperty);
            addRequestType(priority, requestType);
        }
        
        String temp = p.getProperty(PropertyQueueType);
        if (temp != null) {
            if (temp.equalsIgnoreCase(QueueTypeFifo)) {
                queue = new FIFOQueue();
            } else if (temp.equalsIgnoreCase(QueueTypePriority)) {
                queue = new DeadlinePriorityQueue();
            } else if (temp.equalsIgnoreCase(QueueTypeNone)) {                
            } else {
                throw new ConfigurationException("Unsupported queue type: " + temp);
            }
        }
    }
    
    /**
     * Aligns the specified date with the end of the closest cycle.
     *
     * @param date date to align
     *
     * @return date aligned with the end of the closes cycle
     */
    protected Date alignEnd(final Date date)
    {
        long cycle = clock.cycle(date);
        long time = clock.cycleEndInMillis(cycle);

        return new Date(time);
    }

    /**
     * Aligns the specified date with the start of the closest cycle.
     *
     * @param date date to align
     *
     * @return date aligned with the start of the closes cycle
     */
    protected Date alignStart(final Date date)
    {
        long cycle = clock.cycle(date);
        long time = clock.cycleStartInMillis(cycle);

        return new Date(time);
    }

    // fixme: move further down the hierarchy
    
    /**
     * Pool the client requested its resources to be allocated from.
     */
    public static final String PropertyPoolId = "pool.id";

    /**
     * Returns the pool id for the specified reservation.
     * @param r reservation
     * @return pool id
     */
    protected String getCurrentPoolID(IBrokerReservation r)
    {
        return r.getSource().getType().toString();
    }

    /**
     * Returns the pool id for resources requested by the specified reservation.
     * @param r reservation
     * @return pool identifier
     */
    protected String getRequestedPoolID(final IBrokerReservation r)
    {
        if (r.getRequestedResources() != null) {
            if (r.getRequestedResources().getRequestProperties() != null) {
                return r.getRequestedResources().getRequestProperties().getProperty(PropertyPoolId);
            }
        }

        return null;
    }
}
