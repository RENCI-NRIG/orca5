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

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import orca.policy.core.util.AllotmentEntry;
import orca.policy.core.util.AllotmentTable;
import orca.policy.core.util.LogicalInventory;
import orca.policy.core.util.MachineState;
import orca.policy.core.util.ResourceEntry;
import orca.policy.core.util.ResourceProperties;
import orca.policy.core.util.ResourceTable;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.core.PropertiesManager;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.ReservationSet;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.Serializer;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;


/**
 * <code>BrokerWorstFitPolicy</code> runs a worst fit allocation algorithm
 * over an inventory of machines representing virtual machine monitors. It gives
 * priority to extending reservations. It also gives priority based on preset
 * priorities of request types.
 */
public class BrokerWorstFitPolicy extends BrokerPriorityPolicy
{

    /**
     * Properties appended to each donated reservation to indicate the
     * identifiers used for nodes provided by this reservation
     */
    public static final String PropertyIDList = "ids";

    /**
     * Used for testing to force an id switch
     */
    public static String PropertyForceNewId = "force.id";

    /**
     * Saves list of machine ids for recovery.
     */
    public static String PropertyIDs = "BrokerWorstFitPolicyPluginIDs";

    /**
     * Current number of resource dimensions.
     */
    public static int Dimensions = 4;

    /**
     * If a new id should be forced
     */
    @Persistent
    protected boolean forceNewId = false;

    /**
     * Used to count how many requests have failed
     */
    @Persistent
    protected int failed = 0;

    /**
     * Used to count how many requests have been successfully allocated
     */
    @Persistent
    protected int allocated = 0;

    /**
     * Indexes the logical inventory by the id (resource type) associated with
     * it.
     */
    @NotPersistent
    protected Hashtable<String, LogicalInventory> pools;

    /**
     * Creates a new instance.
     */
    public BrokerWorstFitPolicy()
    {
        super();
        pools = new Hashtable<String, LogicalInventory>();
    }

    /**
     * {@inheritDoc}
     */
    public void allocate(final long cycle) throws Exception
    {
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

        if (requests.size() > 0) {
            allocate(requests, startTime);
        }
    }

    /**
     * Allocates requests. For each request, creates a table representing the
     * available resources for the duration of the request. Determines if the
     * request is new or an extension of a previous request.
     *
     * @param requests
     * @param startCycle allocation cycle
     *
     * @throws Exception
     */
    public void allocate(final ReservationSet requests, final long startCycle)
                  throws Exception
    {
        Date startTime = clock.date(startCycle);

        if (requests != null) {
            // process extension requests first
            for (IReservation res : requests) {
                IBrokerReservation r = (IBrokerReservation) res;

                // is this an extension request
                if (r.isExtendingTicket() && !r.isClosed()) {
                    /*
                     * Start time should already by cycle aligned, if not, we
                     * will find out later.
                     */
                    Date start = r.getRequestedTerm().getNewStartTime();

                    // align the end
                    Date end = alignEnd(r.getRequestedTerm().getEndTime());

                    // which pool?
                    String poolID = getCurrentPoolID(r);
                    assert poolID != null;

                    // get the inventory for the pool
                    LogicalInventory inventory = pools.get(poolID);

                    if (inventory != null) {
                        // rebuild the previous allocation
                        ResourceTable table = constructTable(inventory, start, end);
                        // extend the ticket
                        extendTicket(r, table, start, end);
                    } else {
                        // request from an invalid pool. fail the reservation
                        r.fail("No such pool.");
                    }
                }
            }

            // now process the requests for new tickets
            for (IReservation res : requests) {
                IBrokerReservation r = (IBrokerReservation) res;

                // is this a new request?
                if (!r.isExtendingTicket() && !r.isClosed()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("cycle: " + actor.getCurrentCycle() + " new ticket request: " +
                                     r);
                    }

                    // get the cycle-aligned start/end time
                    Date start = alignStart(r.getRequestedTerm().getNewStartTime());
                    Date end = alignEnd(r.getRequestedTerm().getEndTime());

                    /*
                     * if start is before the start time for this auction and
                     * the request is elastic in time, we shift it to start at
                     * startTime
                     */
                    if (start.before(startTime) &&
                            PropertiesManager.isElasticTime(r.getRequestedResources())) {
                        // calculate the term length (milliseconds)
                        long length = end.getTime() - start.getTime();
                        // shift the start
                        start = new Date(clock.cycleStartInMillis(startCycle));
                        // shift the end
                        end = new Date(start.getTime() + length);
                    }

                    // which pool
                    //String poolID = getRequestedPoolID(r);
                    String poolID = r.getRequestedResources().getType().toString();
                    // if no pool, map to the default one
                    if (poolID == null) {
                        poolID = getDefaultPoolID();
                    }

                    if (poolID != null) {
                        LogicalInventory inventory = pools.get(poolID);

                        if (inventory != null) {
                            // calculate what is available
                            ResourceTable table = constructTable(inventory, start, end);
                            // run the allocation
                            ticket(r, table, start, end, startTime);
                        } else {
                            // XXX: no resources for the requested pool
                            r.fail("Insufficient resources");
                        }
                    } else {
                        // XXX: cannot determine a pool for this request. Fail the reservation
                        r.fail("Invalid pool");
                    }
                }
            }
        }
    }

    /**
     * Determines if the requested resource vector can be satisfied from the
     * available resource vector.
     *
     * @param requested requested resource vector
     * @param available available resource vector
     *
     * @return true if the requested vector can be allocated from the available
     *         vector; false otherwise
     */
    protected boolean canAllocate(final long[] requested, final long[] available)
    {
        for (int j = 0; j < requested.length; j++) {
            if (available[j] < requested[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Configures the policy from a properties list.
     *
     * @param p properties
     *
     * @throws Exception
     */
    @Override
    public void configure(final Properties p) throws Exception
    {
        forceNewId = PropList.getBooleanProperty(p, PropertyForceNewId);
        super.configure(p);
    }

    /**
     * Constructs a table of available resources for the given interval.
     * The interval is closed on both sides.
     *
     * @param inventory inventory pool
     * @param start start time
     * @param end end time
     *
     * @return a table representing the available resources
     *         for the specified time period
     */
    protected ResourceTable constructTable(final LogicalInventory inventory, final Date start,
                                           final Date end)
    {
        int numResources = Dimensions;
        ResourceTable table = new ResourceTable();
        Iterator<?> j = inventory.iterator();

        // add a record for each machine from the inventory
        while (j.hasNext()) {
            MachineState machine = (MachineState) j.next();
            ResourceEntry entry = new ResourceEntry(numResources, machine.getId());
            entry.addUnits(MachineState.CpuUnits,
                           machine.getCpu().getMinUnits(start.getTime(), end.getTime()));
            entry.addUnits(MachineState.MemoryUnits,
                           machine.getMemory().getMinUnits(start.getTime(), end.getTime()));
            entry.addUnits(MachineState.BandwidthUnits,
                           machine.getBandwidth().getMinUnits(start.getTime(), end.getTime()));
            entry.addUnits(MachineState.StorageUnits,
                           machine.getStorage().getMinUnits(start.getTime(), end.getTime()));
            table.add(entry);
        }

        // link the inventory
        table.setLogicalInventory(inventory);

        return table;
    }

    /**
     * Converts incoming ticket information into the broker's machine
     * representation and adds it to the brokers logical inventory.
     * <p/>
     * {@inheritDoc}
     */
    public void donate(final IClientReservation r) throws Exception
    {
        /*
         * Assumptions: divisible resources, no extends, donates are forever,
         * regardless of the term in the ticket.
         */
        super.donate(r);

        Term term = r.getTerm();
        Properties p = r.getResources().getResourceProperties();
        ResourceType rtype = r.getType();

        /*
         * Get the logical inventory that corresponds to this resource type
         */
        LogicalInventory inventory = pools.get(rtype.toString());

        if (inventory == null) {
            inventory = new LogicalInventory(rtype);
            inventory.setProperties(p);
            pools.put(rtype.toString(), inventory);
        }

        /*
         * Retrieve the logical ids stored in the reservation.
         */
        ArrayList<ID> list = getIDList(r);

        if (list == null) {
            list = new ArrayList<ID>();
        }

        /*
         * For each unit in the reservation we either reuse already existing
         * logical ids, or we will create new ids as needed. For each unit we
         * create a new MachineState object and add it to the logical inventory.
         */
        Date start = term.getNewStartTime();
        Date end = term.getEndTime();

        for (int i = 0; i < r.getUnits(); i++) {
            ID id = null;

            // do we have an id
            if ((list != null) && (i < list.size())) {
                id = list.get(i);
            }

            // no id, generate one
            if (id == null) {
                id = new ID();
                list.add(id);
            }

            // extract resource dimensions
            // XXX: this has to be automated
            int cpu = PropList.getIntegerProperty(p, ResourceProperties.PropertyCpu);
            int memory = PropList.getIntegerProperty(p, ResourceProperties.PropertyMemory);
            int bandwidth = PropList.getIntegerProperty(p, ResourceProperties.PropertyBandwidth);
            int storage = PropList.getIntegerProperty(p, ResourceProperties.PropertyStorage);

            // construct a new entry and add it to the list
            MachineState machine = new MachineState(start, end, id, cpu, memory, bandwidth, storage);
            inventory.addMachine(machine);
        }

        // store the id list back in the reservation
        storeIDList(r, list);
    }

    /**
     * Extends a ticket. The policy tries to minimize the number of id changes that
     * are made to this request by first seeing if it can satisfy the request
     * with the previously assigned logical machines. If not, then it performs
     * the same algorithm as a new ticket request.
     * <p>
     * Extends are tricky.
     * First, we shrink the reservation (if necessary) Second, we try to resize.
     * We compute how much space we have left on each logical node this
     * reservation is currently assigned to. If this space is not within the
     * resource requirements of the reservation we will fail it. This is
     * somewhat conservative: if we have multiple nodes hosted on the same
     * logical node, we may be able to scrub more free space and actually
     * satisfy the resizing part. Finally, if we need to add nodes, we try to
     * allocate them.
     * </p>
     * @param r the request
     * @param table the available resources
     * @param start requested restart (real time)
     * @param end requested end (real time)
     *
     * @throws Exception
     */
    protected void extendTicket(final IBrokerReservation r, final ResourceTable table,
                                final Date start, final Date end) throws Exception
    {
        int currentUnits = r.getResources().getUnits();
        int requestedUnits = r.getRequestedUnits();

        // get the already assigned identifiers
        Vector<ID> ids = getIdentifiers(((Ticket) r.getResources().getResources()).getProperties());

        // remove identifiers if reservation is shrinking
        if (requestedUnits < currentUnits) {
            removeIds(ids, currentUnits - requestedUnits);
        }

        // rebuild the current allocation
        AllotmentTable allotment = rebuildAllotment(table, ids, start, end);

        // find the min spare resources on each machine that contains an
        // allocated unit from this reservation
        long[] available = allotment.findMinAvailable(Dimensions);

        // get the min requested resources
        ResourceEntry minRequest = new ResourceEntry(getMin(r.getRequestedResources()), null);

        // check if we sufficient space
        if (!canAllocate(getMin(r.getRequestedResources()), available)) {
            // no space
            // XXX: we fail the request now. Another solution is to treat this as a new ticket
            // and try extend it. Another alternative is to keep the current allocation and not
            // kill the reservation. The actual behavior should be controlled by a property.
            r.failWarn("Cannot satisfy the resize request: not enough resources");
        } else {
            // do we need to grow?
            if (requestedUnits > currentUnits) {
                /*
                 * Update the ResourceTable entries for the old allotment (do a
                 * reserve)
                 */
                Iterator<?> i = allotment.iterator();
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
                issueTicket(table.getLogicalInventory(), r, allotment, maxResources, start, end,
                            start);
            }
        }
    }

    /**
     * Extracts a ticket for the reservation from the specified sources.
     * @param reservation reservation for which to extract the ticket
     * @param source source reservation
     * @param approved approved ticket term
     * @param units number of units
     */
    protected ResourceSet extractTicket(final IBrokerReservation reservation,
                                        final IClientReservation source, final Term approved,
                                        final long units, Properties properties) throws Exception
    {
        ResourceSet mine = null;
        ResourceSet ticket = source.getResources();

        try {
            /* no check */
            // if (!source.term.satisfies(approved)) {
            // String error = "Source term does not satisfy requested term or
            // reservation is not elastic in time: sourceterm=" +
            // source.term.toString(clock) + " resterm=" +
            // approved.toString(clock);
            // reservation.fail(error);
            // } else {
            // make the new delegation
                        
            // make the new delegation                
            ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().
            							makeDelegation((int)units, null, approved, ticket.getType(), 
            											null, null, properties, getClientID(reservation));

            // make the new resource set
            mine = extract(ticket, del);
       
            // attach the current request properties so that we can look at
            // them in the future
            Properties p = reservation.getRequestedResources().getRequestProperties();
            mine.setRequestProperties(p);

            // }
        } catch (Exception e) {
            logger.error(
                "Term not satisfied: Mapper extract failed: has:" + ticket.getConcreteUnits(),
                e);
            throw new Exception(
                "Term not satisfied: Mapper extract failed: " + e.toString() + " has:" +
                ticket.getConcreteUnits());
        }

        if ((mine != null) && !reservation.isFailed()) {
            reservation.setApproved(approved, mine);
            reservation.setSource(source);

            if (logger.isDebugEnabled()) {
                logger.debug("allocated: " + mine.getUnits() + " for term: " + approved.toString());
                logger.debug("resourceShare = " + units + " mine = " + mine.getUnits());
            }
        }

        return mine;
    }

    /**
     * Determines recursively  the machines and number of units per machine that
     * will be allocated to a request.
     *
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
     *
     * @return the allotment for this request
     *
     * @throws Exception
     */
    public AllotmentTable findAllotment(final ResourceEntry request, final ResourceTable table,
                                        int unitsNeeded, final AllotmentTable allotment,
                                        int minIndex, final IBrokerReservation r,
                                        final int extendedUnits) throws Exception
    {
        boolean successful = true;
        int unitsAcquired = allotment.totalUnits();
        int unitsAcquiredThisCycle = 0;

        if (unitsAcquired < unitsNeeded) {
            // if () {
            for (int i = table.size() - 1; i >= 0; i--) {
                ResourceEntry machine = table.get(i);

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
                } else if (!machine.shouldContinue(request, multiplier, false)) {
                    /*
                     * This allocation was not successful and we know we cannot
                     * allocate resources further up in the table since they are
                     * smaller than the one we're currently at
                     */
                    successful = false;

                    int minIndexOld = minIndex;
                    minIndex = i + 1;

                    if (minIndexOld == minIndex) {
                        // This would cause us to repeat the exact same loop
                        // again
                        minIndex = table.size();
                    }

                    break;
                }
            }

            // } else {
            // for (int i = 0; i < table.size(); i++) {
            // ResourceEntry machine = table.get(i);
            //
            // /*
            // * Determine how many units have been allocated to this
            // * machine entry
            // */
            // int multiplier = 1;
            // if (allotment.getAllotment().containsKey(machine.getId())) {
            // multiplier =
            // allotment.getAllotment().get(machine.getId()).getUnits() + 1;
            // }
            // /*
            // * See if the machine entry satisfies the requested resource
            // * requirements and if not, see if we should continue to
            // * loop to check for an appropriate machine
            // */
            // if (machine.satisfies(request, multiplier)) {
            // AllotmentEntry entry = new AllotmentEntry(1, machine);
            // /*
            // * Add the entry to the allotment table and indicate
            // * that we've acquired another unit
            // */
            // allotment.addEntry(entry);
            // unitsAcquired++;
            // if (unitsAcquired == unitsNeeded) {
            // break;
            // }
            // }
            // }
            // if (unitsAcquired < unitsNeeded) {
            // successful = false;
            // }
            // }
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
     * {@inheritDoc}
     */
    @Override
    public void finish(final long cycle)
    {
        super.finish(cycle);
        tickPools(cycle);
    }

    /**
     * Returns the default pool id.
     *
     * @return default pool id
     */
    protected String getDefaultPoolID()
    {
        String result = null;
        Iterator<String> iterator = pools.keySet().iterator();

        if (iterator.hasNext()) {
            result = iterator.next();
        }

        return result;
    }

    /**
     * Returns the logical identifiers stored in the specified properties list.
     *
     * @param p properties list
     *
     * @return a vector of logical identifiers
     *
     * @throws Exception
     */
    protected Vector<ID> getIdentifiers(final Properties p) throws Exception
    {
        String idString = PropList.getProperty(p, Ticket.PropertyIdentifiers);

        return Serializer.stringToIDs(idString);
    }

    /**
     * Returns a list of the logical identifiers stored in the specified reservation.
     * @param r reservation
     * @return list of logical identifiers
     */
    protected ArrayList<ID> getIDList(final IClientReservation r)
    {
        ArrayList<ID> list = null;

        Properties p = r.getResources().getLocalProperties();

        if (p != null) {
            String listString = p.getProperty(PropertyIDList);

            if (listString != null) {
                list = getIDList(listString);
            }
        }

        return list;
    }

    /**
     * Parses the specified string and extracts the stored logical identifiers.
     * @param str string representing serialized identifiers
     * @return list of logical identifiers
     */
    protected ArrayList<ID> getIDList(final String str)
    {
        ArrayList<ID> list = new ArrayList<ID>();
        StringTokenizer st = new StringTokenizer(str, " ");

        while (st.hasMoreTokens()) {
            ID id = new ID(st.nextToken());
            list.add(id);
        }

        return list;
    }

    /**
     * Serializes the list of logical identifiers into a string.
     * @param list list of logical identifiers
     * @return string representation
     */
    protected String getIDString(final ArrayList<ID> list)
    {
        StringBuffer sb = new StringBuffer();
        Iterator<ID> i = list.iterator();

        while (i.hasNext()) {
            ID id = i.next();
            sb.append(id.toString() + " ");
        }

        return sb.toString();
    }

    /**
     * Returns the logical inventory associated with the specified resource type.
     * @param type resource type
     * @return logical inventory
     */
    public LogicalInventory getLogicalInventory(final ResourceType type)
    {
        return pools.get(type.toString());
    }

    /**
     * Returns the internal state for the specified logical machine.
     * @param type resource type (pool identifier)
     * @param machineId machine identifier
     * @return internal machine state
     */
    public MachineState getLogicalMachine(final ResourceType type, final ID machineId)
    {
        return getLogicalInventory(type).getMachine(machineId);
    }

    /**
     * Returns the upper bound of resources represented in the specified request.
     *
     * @param set request
     *
     * @return the maximum requested resources for each resource dimension
     *
     * @throws Exception
     */
    protected long[] getMax(final ResourceSet set) throws Exception
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
     * Returns the lower bound of resources represented in the specified request.
     *
     * @param set request
     *
     * @return the minimum requested resources for each resource dimension
     *
     * @throws Exception
     */
    protected long[] getMin(final ResourceSet set) throws Exception
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
     * Returns the logical inventory associated with
     * the specified resource type
     * @param type resource type
     * @return logical inventory
     */
    protected LogicalInventory getPool(final ResourceType type)
    {
        return pools.get(type.toString());
    }


    /**
     * Returns the current resource shares held by the resource set.
     *
     * @param set resource set
     *
     * @return current resource shares
     *
     * @throws Exception
     */
    protected long[] getShares(ResourceSet set) throws Exception
    {
        long[] resourceLimit = new long[Dimensions];

        // resource shares are stored in the ticket properties list
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
     * Determines how far we can grow the minimum request, but not exceed the
     * maximum request.
     *
     * @param allotment the current allotment
     * @param minRequest the minimum request
     * @param maxRequest the maximum a request may grow to
     * @param r the request
     *
     * @return the new resource vector with possibly more resources than the
     *         minimum request
     */
    protected long[] growResources(final AllotmentTable allotment, final ResourceEntry minRequest,
                                   final long[] maxRequest, final IBrokerReservation r)
    {
        return allotment.findMaxResources(minRequest.getResources(), maxRequest);
    }

    /**
     * Reserves the allotment being allocated to the ticket from the brokers's
     * inventory and creates the ticket to be given to the request.
     *
     * @param inventory inventory pool
     * @param r request
     * @param allotment what is being given to the request
     * @param maxResources resources being allocated to the ticket
     * @param start when the ticket will start
     * @param end when the ticket will end
     * @param startTime auction start time. For extending requests this will equal start, but for new requests it may be different.
     *
     * @throws Exception
     */
    protected void issueTicket(final LogicalInventory inventory, final IBrokerReservation r,
                               final AllotmentTable allotment, final long[] maxResources,
                               final Date start, final Date end, final Date startTime)
                        throws Exception
    {
        /*
         * Reserve the allotment from the inventory
         */
        try {
            // logger.debug(inventory.dumpStats(start));
            inventory.reserve(allotment, maxResources, start, end);

            // logger.debug(inventory.dumpStats(start));
        } catch (Exception e) {
            String error = "There was an error in reserving the allotment in the inventory: " +
                           e.toString();
            r.fail(error);
        }

        /*
         * Satisfy this allocation Attach identifiers from the logical inventory
         * to the request
         */
        ReservationSet holdings = calendar.getHoldings(startTime, inventory.getType());

        // slices.ResourceCount rc = new ResourceCount();
        // holdings.count(rc, clock.date(start));
        if (holdings.size() > 0) {
            IClientReservation source = (IClientReservation) holdings.iterator().next(); // (ReservationClient)
                                                                                         // calendar.getHoldings(start).iterator().next();

            ResourceSet mine = null;
            int resourceShare = allotment.totalUnits();

            // // If the start and end times have been shifted during the
            // // allocation
            // // process, this will shift the
            // // allocated term for the reservation
            // if (start != clock.cycle(r.getRequestedTerm().getNewStartTime()))
            // {
            // if (PropertiesManager.isElasticTime(r.getRequestedResources())) {
            // logger.info("Shifting term to " + start + " from " +
            // r.getRequestedTerm().getNewStartTime());
            // r.setRequestedTerm(r.getRequestedTerm().shiftTerm(clock.cycleStartDate(start),
            // clock));
            // }
            // }
            // Term approved = new Term(r.getRequestedTerm());
            Term approved = null;

            if (r.isExtendingTicket()) {
                // approved = new Term(r.getTerm().getStartTime(),
                // clock.date(end), clock.date(start));
                approved = new Term(r.getTerm().getStartTime(), end, start);
            } else {
                // approved = new Term(clock.date(start), clock.date(end));
                approved = new Term(start, end);
            }

            // Attach identifiers to send to the site
            String identifiers = allotment.getIdentifiers();

            Properties ticketProperties = new Properties();
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
            
            mine = extractTicket(r, source, approved, resourceShare, ticketProperties);

            if (mine == null) {
                String error = "There was an error extracting a ticket from the source ticket";
                r.fail(error);

                return;
            }


            if (requireApproval) {
                /*
                 * Allocations require administrative approval. Add it to the
                 * set.
                 */
                addForApproval(r);
            } else {
                addToCalendar(r);
                /*
                 * Whatever happened up there, this bid is no longer pending. It
                 * either succeeded or is now marked failed. (Could/should
                 * assert.)
                 */
                r.setBidPending(false);
            }
        } else {
            logger.error("No resources...");
            r.fail("No resources...");

            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Properties query(final Properties properties)
    {
        if ((properties != null) && properties.containsKey(PropertyDiscoverTypes)) {
            Properties p = new Properties();
            int count = 0;

            for (LogicalInventory inventory : pools.values()) {
                p.setProperty(PropertyTypeNamePrefix + count, inventory.getType().toString());
                p.setProperty(PropertyTypeUnitsPrefix + count, Integer.toString(inventory.getSize()));
                PropList.setProperty(p,
                                     PropertyTypeDescriptionPrefix + count,
                                     inventory.getProperties());
                count++;
            }

            PropList.setProperty(p, PropertyTypeCount, count);

            return p;
        } else {
            return super.query(properties);
        }
    }

    /**
     * Rebuilds the allotment table for the reservation represented by the given
     * ids.
     *
     * @param machines set of the machines
     * @param ids the ids of machines from which to rebuild allotment table
     * @param start start of the requested ticket (real time)
     * @param end end of the requested ticket (real time)
     *
     * @return an allotment table
     */
    protected AllotmentTable rebuildAllotment(final ResourceTable machines, final Vector<ID> ids,
                                              final Date start, final Date end)
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
     * {@inheritDoc}
     */
    @Override
    protected void release(final IBrokerReservation r)
    {
        super.release(r);

        if (r.isClosedInPriming()) {
            if (r.getPreviousResources() != null) {
                releaseResources(r.getPreviousResources(), r.getPreviousTerm());
            }

            releaseResources(r.getApprovedResources(), r.getApprovedTerm());
        } else {
            releaseResources(r.getResources(), r.getTerm());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseNotApproved(final IBrokerReservation r)
    {
        super.releaseNotApproved(r);
        releaseResources(r.getApprovedResources(), r.getApprovedTerm());
    }

    /**
     * Releases the resources assigned to this reservation.
     *
     * @param set resource set
     * @param t term
     */
    protected void releaseResources(final ResourceSet set, final Term t)
    {
        try {
            if ((set == null) || (t == null) || (set.getResources() == null)) {
                logger.warn("Reservation does not have resources to release");

                return;
            }

            long[] shares = getShares(set);
            ResourceEntry entry = new ResourceEntry(shares, null);
            LogicalInventory inventory = pools.get(set.getType().toString());

            if (inventory != null) {
                Vector<ID> ids = getIdentifiers(((Ticket) set.getResources()).getProperties());

                for (int i = 0; i < ids.size(); i++) {
                    ID id = (ID) ids.elementAt(i);
                    MachineState machine = inventory.getMachine(id);
                    machine.releaseConditional(t.getStartTime(), t.getEndTime(), entry);
                }
            } else {
                throw new Exception("Cannot release resources: missing inventory");
            }
        } catch (Exception e) {
            logger.error("releaseReources", e);
        }
    }

    /**
     * Removes <code>count</code> identifiers from the specified list.
     *
     * @param ids list of identifiers
     * @param count number of identifiers to remove
     *
     * @throws Exception
     */
    protected void removeIds(final Vector<ID> ids, final int count) throws Exception
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
     * {@inheritDoc}
     */
    @Override
    protected void revisitServer(final IBrokerReservation reservation) throws Exception
    {
        super.revisitServer(reservation);

        switch (reservation.getState()) {
            case ReservationStates.Ticketed:
                revisitTicketed(reservation);

                break;
        }
    }

    /**
     * Recovers a ticketed reservation.
     * @param reservation reservation
     * @throws Exception
     */
    protected void revisitTicketed(final IBrokerReservation reservation) throws Exception
    {
        switch (reservation.getPendingState()) {
            case ReservationStates.None:
                revisitTicketedNone(reservation);

                break;

            case ReservationStates.Priming:
                revisitTicketedPriming(reservation);

                break;
        }
    }

    /**
     * Recovers an active ticketed reservation
     * @param reservation reservation
     * @throws Exception
     */
    protected void revisitTicketedNone(final IBrokerReservation reservation)
                                throws Exception
    {
        ResourceSet rset = reservation.getResources();
        Properties p = ((Ticket) rset.getResources()).getProperties();

        /* get identifiers */
        String ids = p.getProperty(Ticket.PropertyIdentifiers);

        /* get shares */
        long[] shares = getShares(rset);

        /* get the resource pool */
        LogicalInventory inventory = pools.get(rset.getType().toString());

        /* reserve in the resource pool */
        StringTokenizer st = new StringTokenizer(ids, ",");

        while (st.hasMoreTokens()) {
            ID id = new ID(st.nextToken());
            inventory.reserve(id,
                              shares,
                              reservation.getTerm().getNewStartTime(),
                              reservation.getTerm().getEndTime());
        }
    }

    /**
     * Recovers a ticketed priming reservation.
     * @param reservation reservation
     * @throws Exception
     */
    protected void revisitTicketedPriming(final IBrokerReservation reservation)
                                   throws Exception
    {
        /*
         * reservation.getResources() may be incorrect here. Use
         * getApprovedResources instead!
         */
        ResourceSet rset = reservation.getApprovedResources();
        Properties p = ((Ticket) rset.getResources()).getProperties();

        /* get identifiers */
        String ids = p.getProperty(Ticket.PropertyIdentifiers);

        /* get shares */
        long[] shares = getShares(rset);

        /* get lease interval */
        //        long start = clock.cycle();
        //        long end = clock.cycle();

        /* get the resource pool */
        LogicalInventory inventory = pools.get(rset.getType().toString());

        /* reserve in the resource pool */
        StringTokenizer st = new StringTokenizer(ids, ",");

        while (st.hasMoreTokens()) {
            ID id = new ID(st.nextToken());
            inventory.reserve(id,
                              shares,
                              reservation.getTerm().getNewStartTime(),
                              reservation.getTerm().getEndTime());
        }
    }

    /**
     * Stores the list of logical identifiers in the specified reservation
     * @param r reservation
     * @param list list of logical identifiers
     */
    protected void storeIDList(final IClientReservation r, final ArrayList<ID> list)
    {
        String str = getIDString(list);
        Properties p = r.getResources().getLocalProperties();

        if (p != null) {
            p.setProperty(PropertyIDList, str);
            r.setDirty();
        }
    }

    /**
     * Issues a new ticket to a request.<br>
     * This is the first request for a ticket We try to satisfy the request for
     * the lower bound of requested resources per dimension. If this succeeds,
     * we find the maximum amount by which we can grow each of the pre-allocated
     * nodes. We compute the final allocation and commit it to the inventory.
     * Finally, we issue a ticket will all required properties.
     *
     * @param r the request
     * @param table the available machines from the logical inventory
     * @param start requested start
     * @param end requested end
     * @param auctionStartTime  time this auction is allocating resources for
     *
     * @return true if ticket successfully completed, false otherwise
     *
     * @throws Exception
     */
    protected boolean ticket(final IBrokerReservation r, final ResourceTable table,
                             final Date start, final Date end, final Date auctionStartTime)
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
            issueTicket(table.getLogicalInventory(),
                        r,
                        allotment,
                        maxResources,
                        start,
                        end,
                        auctionStartTime);

            return true;
        }
    }

    /**
     * Removes irrelevant state from the resource pools.
     * @param cycle cycle
     */
    protected synchronized void tickPools(final long cycle)
    {
        long realTime = clock.cycleEndInMillis(cycle);

        for (LogicalInventory inventory : pools.values()) {
            // inventory.tick(cycle);
            inventory.tick(realTime);
        }
    }
}