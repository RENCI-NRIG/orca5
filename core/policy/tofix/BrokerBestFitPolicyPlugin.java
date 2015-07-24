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

import orca.policy.core.util.AllotmentEntry;
import orca.policy.core.util.AllotmentTable;
import orca.policy.core.util.LogicalInventory;
import orca.policy.core.util.MachineState;
import orca.policy.core.util.PropertiesManager;
import orca.policy.core.util.RequestTypePriority;
import orca.policy.core.util.ResourceEntry;
import orca.policy.core.util.ResourceProperties;
import orca.policy.core.util.ResourceTable;

import orca.shirako.common.ReservationSet;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.BrokerReservation;
import orca.shirako.kernel.ReservationClient;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceReservation;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;

import orca.util.ID;
import orca.util.LoggingTool;
import orca.util.PropList;
import orca.util.Serializer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * The <code> AgentBestFitPolicyPlugin</code> performs a best fit allocation
 * algorithm over an inventory of machines. It gives priority to extending
 * reservations. It also gives priority based on preset priorities of request
 * types.
 * @author grit
 */
public class BrokerBestFitPolicyPlugin extends BrokerPriorityPolicy
{
    /**
     * Used for testing to force an id switch
     */
    public static String PropertyForceNewId = "force.id";

    /**
     * Saves list of machine ids for recovery
     */
    public static String PropertyIDs = "AgentBestFitPolicyPluginIDs";
    public static int Dimensions = 4;
    /**
     * Holds list of logical machine ids. Must be saved/reset on recovery.
     */
    ArrayList<ID> idList = null;

    /**
     * Holds a set of logical machines it has from tickets - it represents the
     * agent's holdings
     */
    LogicalInventory inventory;

    /**
     * If a new id should be forced
     */
    boolean forceNewId = false;

    /**
     * Used to count how many requests have failed
     */
    protected int failed = 0;

    /**
     * Used to count how many requests have been successfully allocated
     */
    protected int allocated = 0;

    public BrokerBestFitPolicyPlugin()
    {
        super();
        idList = new ArrayList<ID>();
        inventory = new LogicalInventory();
    }

    /*
     * ===================================================================
     * Donated ticket handling
     * ===================================================================
     */

    /**
     * Convert incoming ticket information into the agent's machine
     * representation and add it to its logical inventory. <br>
     * {@inheritDoc}
     */
    public void donate(ReservationClient r) throws Exception
    {
        super.donate(r);

        // Assume that resources from the donateTicket are divisible
        Term term = r.getTerm();
        Properties p = r.getResources().getResourceProperties();

        for (int i = 0; i < r.getUnits(); i++) {
            ID id;

            try {
                id = idList.get(i);
            } catch (IndexOutOfBoundsException e) {
                id = null;
            }

            if (id == null) {
                id = new ID();
                idList.add(id);
            }

            MachineState machine = new MachineState(term.getStartTime().getTime(),
                                                    term.getEndTime().getTime(),
                                                    id,
                                                    PropList.getIntegerProperty(
                                                                                p,
                                                                                ResourceProperties.PropertyCpu),
                                                    PropList.getIntegerProperty(
                                                                                p,
                                                                                ResourceProperties.PropertyMemory),
                                                    PropList.getIntegerProperty(
                                                                                p,
                                                                                ResourceProperties.PropertyBandwidth),
                                                    PropList.getIntegerProperty(
                                                                                p,
                                                                                ResourceProperties.PropertyStorage));
            inventory.addMachine(machine);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void allocate(long cycle) throws Exception
    {
        if (LoggingTool.logTime()) {
            String s = "LogicalMachines at cycle " + cycle + "::" + inventory.dumpStats(cycle);
            logger.time(s);
            logger.time("Agent failed=" + failed + " allocated=" + allocated);
        }

        if (getNextAllocation(cycle) != cycle) {
            return;
        }

        lastAllocation = cycle;

        long startTime = getStartForAllocation(cycle);
        ReservationSet requests = calendar.getRequests(startTime);

        if ((requests == null) || (requests.size() == 0)) {
            logger.debug("No requests for auction start cycle " + startTime);

            return;
        }

        logger.debug("Allocating resources for cycle " + startTime);

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
        }

        /*
         * Allocate any remaining requests
         */
        if (requests.size() > 0) {
            allocate(requests, startTime);
        }
    }

    /**
     * Obtains the identifiers from a properties list
     * @param p the properties
     * @return a set of ids
     * @throws Exception
     */
    protected Vector<ID> getIdentifiers(Properties p) throws Exception
    {
        String idString = PropList.getProperty(p, Ticket.PropertyIdentifiers);

        return Serializer.stringToIDs(idString);
    }

    /**
     * Removes <code>count</code> identifiers from the specified list
     * @param ids set of ids
     * @param count number of ids to remove
     * @throws Exception
     */
    protected void removeIds(Vector<ID> ids, int count) throws Exception
    {
        if (ids.size() < count) {
            throw new Exception("Tried to remove mode ids than present in the set");
        } else {
            for (int j = 0; j < count; j++) {
                ids.remove(0);
            }
        }
    }

    /**
     * Constructs a table of available resources for the given interval
     * @param start start of the requested ticket
     * @param end end of the requested ticket
     * @return a table representing the available machines and their resources
     *         for the specified time period
     */
    protected ResourceTable constructTable(long start, long end)
    {
        int numResources = Dimensions;
        ResourceTable table = new ResourceTable();
        Iterator j = inventory.iterator();

        while (j.hasNext()) {
            MachineState machine = (MachineState) j.next();
            ResourceEntry entry = new ResourceEntry(numResources, machine.getId());
            entry.addUnits(MachineState.CpuUnits, machine.getCpu().getMinUnits(start, end));
            entry.addUnits(MachineState.MemoryUnits, machine.getMemory().getMinUnits(start, end));
            entry.addUnits(MachineState.BandwidthUnits,
                           machine.getBandwidth().getMinUnits(start, end));
            entry.addUnits(MachineState.StorageUnits, machine.getStorage().getMinUnits(start, end));
            table.add(entry);
        }

        return table;
    }

    /**
     * Rebuilds the allotment table for the reservation represented by the given
     * ids
     * @param machines set of the machines
     * @param ids the ids of machines from which to rebuild allotment table
     * @param start start of the requested ticket
     * @param end end of the requested ticket
     * @return an allotment table
     */
    protected AllotmentTable rebuildAllotment(ResourceTable machines, Vector<ID> ids, long start,
                                              long end)
    {
        AllotmentTable allotment = new AllotmentTable(start, end);

        for (int i = 0; i < ids.size(); i++) {
            ID id = (ID) ids.elementAt(i);
            ResourceEntry re = machines.get(id);

            // XXX check if state is not null
            AllotmentEntry entry = new AllotmentEntry(1, re);
            allotment.addEntry(entry);
        }

        return allotment;
    }

    /**
     * Obtains the upper bound on resource dimensions
     * @param set contains the properties
     * @return the maximum available over all resources
     * @throws Exception
     */
    protected long[] getMax(ResourceSet set) throws Exception
    {
        long[] resourceLimit = new long[Dimensions];
        Properties p = set.getRequestProperties();
        resourceLimit[MachineState.CpuUnits] = ResourceProperties.getMax(p,
                                                                         ResourceProperties.PropertyCpu);
        resourceLimit[MachineState.MemoryUnits] = ResourceProperties.getMax(p,
                                                                            ResourceProperties.PropertyMemory);
        resourceLimit[MachineState.BandwidthUnits] = ResourceProperties.getMax(p,
                                                                               ResourceProperties.PropertyBandwidth);
        resourceLimit[MachineState.StorageUnits] = ResourceProperties.getMax(p,
                                                                             ResourceProperties.PropertyStorage);

        return resourceLimit;
    }

    /**
     * Obtains the lower bound on resource dimensions
     * @param set contains the properties
     * @return the minimum available over all resources
     * @throws Exception
     */
    protected long[] getMin(ResourceSet set) throws Exception
    {
        long[] resourceLimit = new long[Dimensions];
        Properties p = set.getRequestProperties();
        resourceLimit[MachineState.CpuUnits] = ResourceProperties.getMin(p,
                                                                         ResourceProperties.PropertyCpu);
        resourceLimit[MachineState.MemoryUnits] = ResourceProperties.getMin(p,
                                                                            ResourceProperties.PropertyMemory);
        resourceLimit[MachineState.BandwidthUnits] = ResourceProperties.getMin(p,
                                                                               ResourceProperties.PropertyBandwidth);
        resourceLimit[MachineState.StorageUnits] = ResourceProperties.getMin(p,
                                                                             ResourceProperties.PropertyStorage);

        return resourceLimit;
    }

    /**
     * Returns the current resource shares held by this resource set
     * @param set resource set
     * @return
     * @throws Exception
     */
    protected long[] getShares(ResourceSet set) throws Exception
    {
        long[] resourceLimit = new long[Dimensions];
        Properties p = ((Ticket) set.getResources()).getProperties();

        resourceLimit[MachineState.CpuUnits] = PropList.getIntegerProperty(p,
                                                                           ResourceProperties.PropertyCpu);
        resourceLimit[MachineState.MemoryUnits] = PropList.getIntegerProperty(p,
                                                                              ResourceProperties.PropertyMemory);
        resourceLimit[MachineState.BandwidthUnits] = PropList.getIntegerProperty(p,
                                                                                 ResourceProperties.PropertyBandwidth);
        resourceLimit[MachineState.StorageUnits] = PropList.getIntegerProperty(p,
                                                                               ResourceProperties.PropertyStorage);

        return resourceLimit;
    }

    /**
     * Reserves the allotment being allocated to the ticket from the agent's
     * inventory and creates the ticket to be given to the request.
     * @param r request
     * @param allotment what is being given to the request
     * @param maxResources resources being allocated to the ticket
     * @param start when the ticket will start
     * @param end when the ticket will end
     * @throws Exception
     */
    protected void issueTicket(BrokerReservation r, AllotmentTable allotment, long[] maxResources,
                               long start, long end) throws Exception
    {
        /*
         * Reserve the allotment from the inventory
         */
        try {
            logger.debug(inventory.dumpStats(start));
            inventory.reserve(allotment, maxResources, start, end);
            logger.debug(inventory.dumpStats(start));
        } catch (Exception e) {
            String error = "There was an error in reserving the allotment in the inventory: " +
                           e.toString();
            r.fail(error);
        }

        /*
         * Satisfy this allocation Attach identifiers from the logical inventory
         * to the request
         */

        // XXX For now we assume we only have one ticket
        ReservationClient source = (ReservationClient) calendar.getHoldings(start).iterator().next();
        ResourceSet mine = null;
        int resourceShare = allotment.totalUnits();

        // If the start and end times have been shifted during the allocation
        // process, this will shift the
        // allocated term for the reservation
        Term approved = new Term(r.getRequestedTerm());

        if (start != clock.cycle(r.getRequestedTerm().getNewStartTime())) {
            if (PropertiesManager.isElasticTime(r.getRequestedResources())) {
                logger.info(
                    "Shifting term to " + start + " from " +
                    r.getRequestedTerm().getNewStartTime());
                approved = approved.shift(clock.cycleStartDate(start));
            }
        }

        mine = extractTicket(r, source, approved, resourceShare);

        // Attach identifiers to send to the site
        String identifiers = allotment.getIdentifiers();
        Ticket myTicket = (Ticket) mine.getResources();
        Properties ticketProperties = myTicket.getProperties(true);
        ticketProperties.setProperty(Ticket.PropertyIdentifiers, identifiers);

        PropList.setBooleanProperty(ticketProperties, Ticket.PropertyDivisible, false);

        // Ordering is based on MachineState
        PropList.setProperty(ticketProperties,
                             ResourceProperties.PropertyCpu,
                             maxResources[MachineState.CpuUnits]);
        PropList.setProperty(ticketProperties,
                             ResourceProperties.PropertyMemory,
                             maxResources[MachineState.MemoryUnits]);
        PropList.setProperty(ticketProperties,
                             ResourceProperties.PropertyBandwidth,
                             maxResources[MachineState.BandwidthUnits]);
        PropList.setProperty(ticketProperties,
                             ResourceProperties.PropertyStorage,
                             maxResources[MachineState.StorageUnits]);

        if (requireApproval) {
            /*
             * Allocations require administrative approval. Add it to the set.
             */
            addForApproval(r);
        } else {
            addToCalendar(mine, r, source, approved);
            /*
             * Whatever happened up there, this bid is no longer pending. It
             * either succeeded or is now marked failed. (Could/should assert.)
             */
            r.setBidPending(false);
        }
    }

    /**
     * Determine if this request can be resized. Max becomes the min amount
     * @param r request
     * @param available the resources that are available for this request
     * @return <code>true</code> if the all the minimum resources requested by
     *         the request is less than what's available; <code>false</code>
     *         otherwise.
     * @throws Exception
     */
    protected boolean canResize(BrokerReservation r, long[] available) throws Exception
    {
        long[] requestMin = getMin(r.getRequestedResources());

        for (int j = 0; j < requestMin.length; j++) {
            if (available[j] < requestMin[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extend a ticket. Agent tries to minimize the number of id changes that
     * are made to this request by first seeing if it can satisfy the request
     * with the previously assigned logical machines. If not, then it performs
     * the same algorithm as a new ticket request.<br>
     * Extends are tricky. Currently we are not allowed to change identifiers.
     * First, we shrink the reservation (if necessary) Second, we try to resize.
     * We compute how much space we have left on each logical node this
     * reservation is currently assigned to. If this space is not within the
     * resource requirements of the reservation we will fail it. This is
     * somewhat conservative: if we have multiple nodes hosted on the same
     * logical node, we may be able to scrub more free space and actually
     * satisfy the resizing part. Finally, if we need to add nodes, we try to
     * allocate them.
     * @param r the request
     * @param table the available machines from the logical inventory
     * @param start requested restart
     * @param end requested end
     * @throws Exception
     */
    protected void extendTicket(BrokerReservation r, ResourceTable table, long start, long end)
                         throws Exception
    {
        int currentUnits = r.getResources().getUnits();
        int requestedUnits = r.getRequestedUnits();
        Vector<ID> ids = getIdentifiers(((Ticket) r.getResources().getResources()).getProperties());

        if (requestedUnits < currentUnits) {
            removeIds(ids, currentUnits - requestedUnits);
        }

        AllotmentTable allotment = rebuildAllotment(table, ids, start, end);
        long[] available = allotment.findMinAvailable(Dimensions);
        ResourceEntry minRequest = new ResourceEntry(getMin(r.getRequestedResources()), null);

        // long[] max = new long[Dimensions];
        if (!canResize(r, available)) {
            r.failWarn("Cannot satisfy the resize request: not enough of resources");
        } else {
            if (requestedUnits > currentUnits) {
                // Update the ResourceTable entries for the old allotment (do a
                // reserve)
                Iterator i = allotment.iterator();
                int count = 0;

                while (i.hasNext()) {
                    AllotmentEntry a = (AllotmentEntry) i.next();
                    ID id = a.getId();
                    ResourceEntry entry = table.get(id);
                    long[] reserve = minRequest.getResources();

                    for (int j = 0; j < reserve.length; j++) {
                        entry.removeUnits(j, reserve[j] * a.getUnits());
                    }

                    table.sort(entry);
                    count++;
                }

                // Determine the allotment for the growing units
                AllotmentTable newAllotment = new AllotmentTable(start, end);
                newAllotment = findAllotment(minRequest,
                                             table,
                                             requestedUnits - allotment.totalUnits(),
                                             newAllotment,
                                             0,
                                             r,
                                             allotment.totalUnits());

                // Go through the old allotment table and release the resources
                i = allotment.iterator();

                while (i.hasNext()) {
                    AllotmentEntry a = (AllotmentEntry) i.next();
                    ID id = a.getId();
                    ResourceEntry entry = table.get(id);
                    long[] reserve = minRequest.getResources();

                    for (int j = 0; j < reserve.length; j++) {
                        entry.addUnits(j, reserve[j] * a.getUnits());
                    }

                    // table.sort(entry);
                }

                if (newAllotment == null) {
                    int minUnits = ResourceProperties.getMin(
                        r.getRequestedResources().getRequestProperties(),
                        ResourceProperties.PropertyUnits);

                    if (allotment.totalUnits() < minUnits) {
                        String s = "Logical inventory has insufficient resources: request " + r;
                        r.failWarn(s);
                    }
                } else {
                    // Merge the two allotment tables
                    allotment.mergeAllotments(newAllotment);
                }
            }

            if (allotment == null) {
                String s = "Logical inventory has insufficient resources: request " + r;
                r.failWarn(s);
            } else {
                // Need to grow the allotment before issuing ticket
                long[] maxResources = growResources(allotment,
                                                    minRequest,
                                                    getMax(r.getRequestedResources()),
                                                    r);
                end = checkRequestEndTime(maxResources, start, end, r);
                issueTicket(r, allotment, maxResources, start, end);
            }
        }
    }

    /**
     * Issue a new ticket to a request.<br>
     * This is the first request for a ticket We try to satisfy the request for
     * the lower bound of requested resources per dimension. If this succeeds,
     * we find the maximum amount by which we can grow each of the pre-allocated
     * nodes. We compute the final allocation and commit it to the inventory.
     * Finally, we issue a ticket will all required properties.
     * @param r the request
     * @param table the available machines from the logical inventory
     * @param start requested start
     * @param end requested end
     * @return true if ticket successfully completed, false otherwise
     * @throws Exception
     */
    protected boolean ticket(BrokerReservation r, ResourceTable table, long start, long end)
                      throws Exception
    {
        AllotmentTable allotment = new AllotmentTable(start, end);
        ResourceEntry minRequest = new ResourceEntry(getMin(r.getRequestedResources()), null);
        allotment = findAllotment(minRequest, table, r.getRequestedUnits(), allotment, 0, r, 0);

        if (allotment == null) {
            String s = "Logical inventory has insufficient resources: request " + r;
            r.failWarn(s);

            return false;
        } else {
            long[] resourceLimit = getMax(r.getRequestedResources());
            long[] maxResources = growResources(allotment, minRequest, resourceLimit, r);
            end = checkRequestEndTime(maxResources, start, end, r);
            issueTicket(r, allotment, maxResources, start, end);

            return true;
        }
    }

    /**
     * Determines how far we can grow the minimum request, but not exceed the
     * maximum request
     * @param allotment the current allotment
     * @param minRequest the minimum request
     * @param maxRequest the maximum a request may grow to
     * @param r the request
     * @return the new resource vector with possibly more resources than the
     *         minimum request
     */
    protected long[] growResources(AllotmentTable allotment, ResourceEntry minRequest,
                                   long[] maxRequest, BrokerReservation r)
    {
        return allotment.findMaxResources(minRequest.getResources(), maxRequest);
    }

    /**
     * Determine if the new growth affects the endtime of the request - would
     * only affect the endtime
     * @param maxResources current resource vector for the request
     * @param start when the lease will begin
     * @param end when the lease will end
     * @param r the request
     * @return the new endtime
     */
    protected long checkRequestEndTime(long[] maxResources, long start, long end,
                                       BrokerReservation r)
    {
        return end;
    }

    /**
     * Allocate requests. Iterate through all the requests, create a table
     * representing the available resources for the duration of the request, and
     * determine if it is a new request or an extend request.
     * @param requests
     * @throws Exception
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

                    // if (r.failed()) {
                    // failed++;
                    // } else {
                    // allocated++;
                    // }
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
                    ResourceTable table = constructTable(start, end);
                    ticket(r, table, start, end);

                    // if (forceNewId){
                    // r.hack++;
                    // }
                }

                // if (r.failed()) {
                // failed++;
                // } else {
                // allocated++;
                // }
            }
        }
    }

    /**
     * Recursively determines the machines and number of units per machine that
     * will be allocated to a request
     * @param request the request's resources
     * @param table the resources from the logical inventory available for this
     *            request
     * @param unitsNeeded number of units needed to successfully satisfy this
     *            request
     * @param allotment what has been allocated to this request
     * @param minIndex how far the search should continue
     * @param r the request
     * @param extendedUnits additional units that have already been extended -
     *            only for <code>extendTicket</code>
     * @return the allotment for this request
     * @throws Exception
     */
    public AllotmentTable findAllotment(ResourceEntry request, ResourceTable table,
                                        int unitsNeeded, AllotmentTable allotment, int minIndex,
                                        BrokerReservation r, int extendedUnits)
                                 throws Exception
    {
        boolean successful = true;
        int unitsAcquired = allotment.totalUnits();
        int unitsAcquiredThisCycle = 0;
        int machineIndex = 0;

        if (unitsAcquired < unitsNeeded) {
            while (machineIndex < table.size()) {
                ResourceEntry machine = table.get(machineIndex);

                /*
                 * Determine how many units have been allocated to this machine
                 * entry
                 */
                int multiplier = 1;

                if (allotment.getAllotment().containsKey(machine.getId())) {
                    multiplier = allotment.getAllotment().get(machine.getId()).getUnits() + 1;
                }

                /*
                 * See if the machine entry satisfies the requested resource
                 * requirements and if not, see if we should continue to loop to
                 * check for an appropriate machine
                 */
                if (machine.satisfies(request, multiplier)) {
                    AllotmentEntry entry = new AllotmentEntry(1, machine);
                    /*
                     * Add the entry to the allotment table and indicate that
                     * we've acquired another unit
                     */
                    allotment.addEntry(entry);
                    unitsAcquired++;
                    unitsAcquiredThisCycle++;

                    if (unitsAcquired == unitsNeeded) {
                        break;
                    }
                } else {
                    machineIndex++;
                }
            }

            if (unitsAcquired < unitsNeeded) {
                successful = false;
            }
        }

        /*
         * If the allocation was not fully successful and there is no space
         * elsewhere in the table for a possible allocation, see if the request
         * is flexible in number of units, and return either the allotment if it
         * is, or null if it is not since we didn't satisfy the request
         */
        int maxUnits = ResourceProperties.getMax(r.getRequestedResources().getRequestProperties(),
                                                 ResourceProperties.PropertyUnits);

        if (!successful && (minIndex == table.size())) {
            int minUnits = ResourceProperties.getMin(r.getRequestedResources().getRequestProperties(),
                                                     ResourceProperties.PropertyUnits);

            if ((minUnits < maxUnits) && (minUnits <= (allotment.totalUnits() + extendedUnits))) {
                return allotment;
            } else {
                return null;
            }
        } else if (!successful && (unitsAcquiredThisCycle == 0)) {
            int minUnits = ResourceProperties.getMin(r.getRequestedResources().getRequestProperties(),
                                                     ResourceProperties.PropertyUnits);

            if ((minUnits < maxUnits) && (minUnits <= (allotment.totalUnits() + extendedUnits))) {
                return allotment;
            } else {
                return null;
            }
        } else if (successful && (unitsAcquiredThisCycle == 0)) {
            int minUnits = ResourceProperties.getMin(r.getRequestedResources().getRequestProperties(),
                                                     ResourceProperties.PropertyUnits);

            if ((minUnits < maxUnits) && (minUnits <= (allotment.totalUnits() + extendedUnits))) {
                return allotment;
            } else {
                return null;
            }
        } else if (!successful || (unitsAcquired != (maxUnits - extendedUnits))) {
            /*
             * If the allocation was not successful and there is hope for
             * recursive allotment or we do not have all of our required units,
             * recurse to see if we can improve the allotment
             */
            findAllotment(request, table, unitsNeeded, allotment, minIndex, r, extendedUnits);
        }

        /*
         * Since we were successful and got all of the resources we needed,
         * return the allotment
         */
        return allotment;
    }

    /**
     * Clean inventory <br>
     * {@inheritDoc}
     */
    public void finish(long cycle)
    {
        super.finish(cycle);

        //inventory.tick(cycle);
        // NOTE: inventory is organized using real time!
        inventory.tick(clock.cycleEndInMillis(cycle));
    }

    protected void release(BrokerReservation r)
    {
        super.release(r);
        releaseResources(r.getResources(), r.getTerm());
    }

    protected void releaseNotApproved(BrokerReservation r)
    {
        super.releaseNotApproved(r);
        releaseResources(r.getApprovedResources(), r.getApprovedTerm());
    }

    /**
     * Releases the resources assigned to this reservation
     * @param r
     */
    protected void releaseResources(ResourceSet set, Term t)
    {
        try {
            if ((set == null) || (t == null) || (set.getResources() == null)) {
                logger.warn("Reservation does not have resources to release");

                return;
            }

            long[] shares = getShares(set);
            long start = clock.cycle(t.getNewStartTime());
            long end = clock.cycle(t.getEndTime()) - 1;
            ResourceEntry entry = new ResourceEntry(shares, null);

            Vector<ID> ids = getIdentifiers(((Ticket) set.getResources()).getProperties());

            for (int i = 0; i < ids.size(); i++) {
                ID id = (ID) ids.elementAt(i);
                MachineState machine = inventory.getMachine(id);
                machine.releaseConditional(start, end, entry);
            }
        } catch (Exception e) {
            logger.exception("releaseReources", e);
        }
    }

    /**
     * Do any configuration items that should occur
     * @param p properties
     * @throws Exception
     */
    public void configure(Properties p) throws Exception
    {
        forceNewId = PropList.getBooleanProperty(p, PropertyForceNewId);
        super.configure(p);
    }

    public Properties save() throws Exception
    {
        Properties p = new Properties();
        save(p);

        return p;
    }

    public void save(Properties p) throws Exception
    {
        StringBuffer sb = new StringBuffer();
        Iterator i = this.idList.iterator();

        while (i.hasNext()) {
            ID id = (ID) i.next();
            sb.append(id.toString() + " ");
        }

        p.setProperty(PropertyIDs, sb.toString());
    }

    public void reset(Properties p) throws Exception
    {
        StringTokenizer st = new StringTokenizer(p.getProperty(PropertyIDs), " ");

        while (st.hasMoreTokens()) {
            ID id = new ID(st.nextToken());
            this.idList.add(id);
        }
    }

    public void revisit(ResourceReservation reservation) throws Exception
    {
        super.revisit(reservation);

        if (reservation instanceof BrokerReservation) {
            if (reservation.getState() == ReservationStates.Ticketed) {
                ResourceSet rset = reservation.getResources();
                Properties p = ((Ticket) rset.getResources()).getProperties();
                String ids = p.getProperty(Ticket.PropertyIdentifiers);
                long cpu = Long.parseLong(p.getProperty(ResourceProperties.PropertyCpu));
                long memory = Long.parseLong(p.getProperty(ResourceProperties.PropertyMemory));
                long bandwidth = Long.parseLong(p.getProperty(ResourceProperties.PropertyBandwidth));
                long io = Long.parseLong(p.getProperty(ResourceProperties.PropertyStorage));
                long[] maxResources = new long[Dimensions];
                maxResources[MachineState.CpuUnits] = cpu;
                maxResources[MachineState.MemoryUnits] = memory;
                maxResources[MachineState.BandwidthUnits] = bandwidth;
                maxResources[MachineState.StorageUnits] = io;

                // XXX: these used to call getRequestedTerm.
                // it seems more appropriate that they call getTerm()
                long start = clock.cycle(reservation.getTerm().getNewStartTime());
                long end = clock.cycle(reservation.getTerm().getEndTime());

                StringTokenizer st = new StringTokenizer(ids, ",");

                while (st.hasMoreTokens()) {
                    ID id = new ID(st.nextToken());
                    inventory.reserve(id, maxResources, start, end);
                }
            }

            // must still recover pending reservations never issued initially
            // problem is they are added to the calendar
        }
    }

    public LogicalInventory getLogicalInventory()
    {
        return inventory;
    }

    public MachineState getLogicalMachine(ID machineId)
    {
        return inventory.getMachine(machineId);
    }
}