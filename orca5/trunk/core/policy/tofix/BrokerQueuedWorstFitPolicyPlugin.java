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

import orca.policy.core.util.AllotmentTable;
import orca.policy.core.util.FIFOQueue;
import orca.policy.core.util.PropertiesManager;
import orca.policy.core.util.RequestTypePriority;
import orca.policy.core.util.ResourceEntry;
import orca.policy.core.util.ResourceTable;

import orca.shirako.common.ReservationSet;
import orca.shirako.kernel.BrokerReservation;
import orca.shirako.kernel.ResourceReservation;

import orca.util.LoggingTool;
import orca.util.PropList;

import java.util.Iterator;


/**
 * The <code> AgentQueuedWorstFitPolicyPlugin</code> performs a worst fit
 * allocation algorithm over an inventory of machines.<br>
 * This uses the priorities for new reservations but extends
 * <code>AgentWorstFitPolicyPlugin</code> to use a queue, so if a reservation
 * cannot be allocated "now" and is elasticTime, then it is placed on the queue
 * and the system tries to allocate it in the next allocation cycle.
 * @author grit
 */
public class BrokerQueuedWorstFitPolicyPlugin extends BrokerWorstFitPolicyPlugin
{
    /**
     * Queue of requests that failed but have been deferred: not sure how to
     * recover queue
     *
     */
    protected FIFOQueue queue;

    public BrokerQueuedWorstFitPolicyPlugin()
    {
        super();
        queue = new FIFOQueue();
    }

    /**
     * Allocate pending requests in the queue before issuing new requests.
     * {@inheritDoc}
     */
    public void allocate(long cycle) throws Exception
    {
        if (LoggingTool.logTime()) {
            String s = "LogicalMachines at cycle " + cycle + "::" + inventory.dumpStats(cycle);
            logger.time(s);

            // logger.time("Agent failed=" + failed + " allocated=" +
            // allocated);
        }

        if (getNextAllocation(cycle) != cycle) {
            return;
        }

        lastAllocation = cycle;

        long startTime = getStartForAllocation(cycle);
        ReservationSet requests = calendar.getRequests(startTime);

        if ((requests == null) || (requests.size() == 0)) {
            logger.debug("No new requests for auction start cycle " + startTime);
        }

        logger.debug("Allocating resources for cycle " + startTime);

        // We currently treat requests as a queue, so allocate queued requests
        // before new requests
        // allocateQueue(startTime);

        /*
         * Allocate resources in priority order of requestTypes
         */
        for (RequestTypePriority rtp : requestTypePriorities) {
            ReservationSet nextRequests = new ReservationSet();
            Iterator i = requests.iterator();

            while (i.hasNext()) {
                ResourceReservation r = (ResourceReservation) i.next();
                String requestType = r.getRequestedResources().getRequestProperties()
                                      .getProperty(REQUEST_TYPE);

                if (rtp.getRequestTypes().contains(requestType)) {
                    nextRequests.add(r);
                    i.remove();
                }
            }

            allocate(nextRequests, startTime);

            // We allocate the new NIMO requests first and then get the queue
            // if (rtp.getRequestTypes().contains("sm0")) {
            allocateQueue(startTime);

            // }
        }

        /*
         * Allocate any remaining requests
         */
        if (requests.size() > 0) {
            allocate(requests, startTime);
        }
    }

    /**
     * Allocate queued requests. Iterate through all the requests, create a
     * table representing the available resources for the duration of the
     * request, and determine if it is a new request or an extend request.
     * @param requests
     * @throws Exception
     */
    public void allocateQueue(long startCycle) throws Exception
    {
        Iterator i = queue.iterator();

        while (i.hasNext()) {
            BrokerReservation r = (BrokerReservation) i.next();
            long start = clock.cycle(r.getRequestedTerm().getNewStartTime());
            long end = clock.cycle(r.getRequestedTerm().getEndTime());
            long length = end - start;
            long newEnd = startCycle + length;

            ResourceTable table = constructTable(startCycle, newEnd);

            if (ticket(r, table, startCycle, newEnd)) {
                i.remove();
            } else {
                /*
                 * If ticket returns true put it on the queue if it is elastic
                 * time
                 */
                if (PropertiesManager.isElasticTime(r.getRequestedResources())) {
                    if (queue.contains(r)) {
                        // Determine if we should keep the request on the queue
                        // or return a failed reservation
                        long threshold = PropList.getLongProperty(
                            r.getRequestedResources().getRequestProperties(),
                            "queueThreshold");

                        if ((threshold != 0) && ((startCycle - start) > threshold)) {
                            String s = "Request has exceeded its threshold on the queue " + r;
                            r.failWarn(s);
                            i.remove();
                        } else {
                            logger.info("Keeping res " + r.getReservationID().toHashString() + " in the queue");
                        }
                    }
                }
            }
        }
    }

    /**
     * If a reservation fails to find an allotment, if it's elasticTime, put it
     * on the queue for next allotment cycle instead of failing it.
     * {@inheritDoc}
     */
    public void allocate(ReservationSet requests, long startCycle) throws Exception
    {
        Iterator i;

        /*
         * Fulfill extending reservations first
         */
        if (requests != null) {
            i = requests.iterator();

            while (i.hasNext()) {
                BrokerReservation r = (BrokerReservation) i.next();

                if (r.isExtendingTicket()) {
                    long start = clock.cycle(r.getRequestedTerm().getNewStartTime());
                    long end = clock.cycle(r.getRequestedTerm().getEndTime());
                    ResourceTable table = constructTable(start, end);

                    extendTicket(r, table, start, end);
                }
            }
        }

        /*
         * Fulfill new requests
         */
        if (requests != null) {
            i = requests.iterator();

            while (i.hasNext()) {
                BrokerReservation r = (BrokerReservation) i.next();

                if (!r.isExtendingTicket()) {
                    long start = clock.cycle(r.getRequestedTerm().getNewStartTime());
                    long end = clock.cycle(r.getRequestedTerm().getEndTime());

                    // Request arrived too late so we must adjust the term
                    if (start < startCycle) {
                        long diff = startCycle - start;
                        start += diff;
                        assert start == startCycle;
                        end += diff;
                    }

                    ResourceTable table = constructTable(start, end);

                    if (!ticket(r, table, start, end)) {
                        if (PropertiesManager.isElasticTime(r.getRequestedResources())) {
                            queue.add(r);
                            logger.info("Putting res " + r.getReservationID().toHashString() + " in the queue");
                        } else {
                            String s = "Logical inventory has insufficient resources: request " +
                                       r;
                            r.failWarn(s);
                        }
                    }
                }
            }
        }
    }

    /**
     * Do not fail a reservation because we may put it on the queue if it's
     * elastic {@inheritDoc}
     */
    protected boolean ticket(BrokerReservation r, ResourceTable table, long start, long end)
                      throws Exception
    {
        AllotmentTable allotment = new AllotmentTable(start, end);
        ResourceEntry minRequest = new ResourceEntry(getMin(r.getRequestedResources()), null);
        allotment = findAllotment(minRequest, table, r.getRequestedUnits(), allotment, 0, r, 0);

        if (allotment == null) {
            // Do not fail, but instead just return that there was no ticket
            // issued for the request
            return false;
        } else {
            long[] resourceLimit = getMax(r.getRequestedResources());
            long[] maxResources = growResources(allotment, minRequest, resourceLimit, r);
            end = checkRequestEndTime(maxResources, start, end, r);
            issueTicket(r, allotment, maxResources, start, end);

            return true;
        }
    }
}