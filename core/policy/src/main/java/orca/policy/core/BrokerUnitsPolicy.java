package orca.policy.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.AssignmentForest;
import orca.shirako.common.delegation.AssignmentForestInnerNode;
import orca.shirako.common.delegation.AssignmentForestNode;
import orca.shirako.common.delegation.ResourceBin;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.core.PropertiesManager;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceData;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

public class BrokerUnitsPolicy extends BrokerPriorityPolicy
{
	@NotPersistent
    protected HashMap<String, AssignmentForest> inventory;

    public BrokerUnitsPolicy()
    {
        inventory = new HashMap<String, AssignmentForest>();
    }

    @Override
    public void donate(final IClientReservation r) throws Exception
    {
        super.donate(r);

        ResourceType rtype = r.getType();
        ResourceSet rset = r.getResources();
        Ticket cset = (Ticket) rset.getResources();
        ResourceTicket ticket = cset.getTicket();

        // get the assignment forest for this type
        AssignmentForest forest = inventory.get(rtype.toString());
        if (forest == null) {
            forest = new AssignmentForest();
            inventory.put(rtype.toString(), forest);
        }

        // add the source ticket to the forest
        forest.addSourceTicket(ticket, cset.getSiteProxy());

        AssignmentForestInnerNode[] roots = forest.getRoots();
        assert roots != null && roots.length > 0;

    }

    @Override
    public void allocate(final long cycle) throws Exception
    {
        // should we allocate?

        if (getNextAllocation(cycle) != cycle) {
            return;
        }

        // update the last allocation cycle
        lastAllocation = cycle;

        // get the allocation start time
        long startTime = getStartForAllocation(cycle);
        // get all pending requests
        ReservationSet requests = calendar.getRequests(startTime);
        // do we have any work to do?
        if ((requests == null) || (requests.size() == 0)) {
            logger.debug("No requests for auction start cycle " + startTime);
            return;
        }

        logger.debug("Allocating resources for cycle " + startTime);

        if (requests.size() > 0) {
            allocateExtending(requests, startTime);
            allocateTicketing(requests, startTime);
        }
    }

    @Override
    public Properties query(final Properties properties)
    {
        if ((properties != null) && properties.containsKey(PropertyDiscoverTypes)) {
            Properties p = new Properties();
            int count = 0;

            for (Map.Entry<String, AssignmentForest> entry : inventory.entrySet()) {
                AssignmentForest forest = entry.getValue();
                String type = entry.getKey();
                p.setProperty(PropertyTypeNamePrefix + count, type);
                PropList.setProperty(p, PropertyTypeDescriptionPrefix + count, forest.getProperties());
                count++;
            }

            PropList.setProperty(p, PropertyTypeCount, count);

            return p;
        } else {
            return super.query(properties);
        }
    }

    /**
     * Returns the default pool id.
     * @return default pool id
     */
    protected String getDefaultPoolID()
    {
        String result = null;
        Iterator<String> iterator = inventory.keySet().iterator();

        if (iterator.hasNext()) {
            result = iterator.next();
        }

        return result;
    }

    protected void allocateExtending(final ReservationSet requests, final long startCycle) throws Exception
    {
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
                    AssignmentForest forest = inventory.get(poolID);

                    if (forest != null) {
                        // FIXME: for now we simply allocate a fresh ticket                        
                        ticket(r, forest, new Term(r.getTerm().getStartTime(), end, start));
                        // extend the ticket
                    } else {
                        // request from an invalid pool. fail the reservation
                        r.fail("No such pool.");
                    }
                }
            }
        }
    }

    protected void allocateTicketing(final ReservationSet requests, final long startCycle) throws Exception
    {
        Date startTime = clock.date(startCycle);

        if (requests != null) {
            for (IReservation res : requests) {
                IBrokerReservation r = (IBrokerReservation) res;

                // is this a new request?
                if (!r.isExtendingTicket() && !r.isClosed()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("cycle: " + actor.getCurrentCycle() + " new ticket request: " + r);
                    }

                    // get the cycle-aligned start/end time
                    Date start = alignStart(r.getRequestedTerm().getNewStartTime());
                    Date end = alignEnd(r.getRequestedTerm().getEndTime());

                    /*
                     * if start is before the start time for this auction and
                     * the request is elastic in time, we shift it to start at
                     * startTime
                     */
                    if (start.before(startTime) && PropertiesManager.isElasticTime(r.getRequestedResources())) {
                        // calculate the term length (milliseconds)
                        long length = end.getTime() - start.getTime();
                        // shift the start
                        start = new Date(clock.cycleStartInMillis(startCycle));
                        // shift the end
                        end = new Date(start.getTime() + length);
                    }

                    // which pool
                    String poolID = getRequestedPoolID(r);

                    // if no pool, map to the default one
                    if (poolID == null) {
                        poolID = getDefaultPoolID();
                    }

                    if (poolID != null) {
                        // get the inventory for the pool
                        AssignmentForest forest = inventory.get(poolID);
                        if (forest != null) {
                            // allocate the ticket                            
                            ticket(r, forest, new Term(start, end));
                        } else {
                            // cannot determine a pool for this request. Fail
                            // the reservation
                            r.fail("Invalid pool");
                        }
                    } else {
                        // cannot determine a pool for this request. Fail
                        // the reservation
                        r.fail("Invalid pool");
                    }
                }
            }
        }
    }

    protected void ticket(IBrokerReservation r, AssignmentForest forest, Term term)
    {
        try {
            AssignmentForestInnerNode[] nodes = forest.getInventory();
            if (nodes == null || nodes.length == 0) {
                throw new Exception("No resources");
            }
            ResourceSet rset = r.getRequestedResources();
            int needed = rset.getUnits();
            int allocatedNodes = 0;
            ArrayList<AssignmentForestNode> allocation = new ArrayList<AssignmentForestNode>();

            // scan through the inventory resource bins and allocate
            // resources until we satisfy the request or run out of resources

            for (int i = 0; i < nodes.length; i++) {
                if (needed == 0) {
                    break;
                }
                AssignmentForestInnerNode node = nodes[i];
                int available = node.getMinAvailableUnits(term.getNewStartTime().getTime(), term.getEndTime().getTime());
                if (available > 0) {
                    int toAllocate = Math.min(available, needed);
                    AssignmentForestNode allocated = node.allocate(term, toAllocate);
                    forest.addAllocatedNode(allocated);
                    allocation.add(node);
                    needed -= toAllocate;
                    allocatedNodes += toAllocate;
                }
            }
            if (needed > 0) {
                logger.error("partially satisfied request: allocated=" + allocatedNodes + " needed=" + needed);
            }
            Ticket ticket = issueTicket(r, allocation, term, allocatedNodes, forest);
            attachResources(r, ticket);
        } catch (Exception e) {
            logger.error(e);
            r.fail(e.toString());
        }
    }

    protected void attachResources(IBrokerReservation r, Ticket ticket)
    {
        // make a resource set
        ResourceData rd = new ResourceData();
        // fixme: pass additional properties further down
        ResourceSet rset = new ResourceSet(ticket.getUnits(), ticket.getType(), rd);
        rset.setResources(ticket);

        // attach the current request properties so that we can look at
        // them in the future
        Properties p = r.getRequestedResources().getRequestProperties();
        rset.setRequestProperties(p);

        r.setApproved(ticket.getTerm(), rset);

        if (requireApproval) {
            /*
             * Allocations require administrative approval. Add it to the set.
             */
            addForApproval(r);
        } else {
            addToCalendar(r);
            /*
             * Whatever happened up there, this bid is no longer pending. It
             * either succeeded or is now marked failed. (Could/should assert.)
             */
            r.setBidPending(false);
        }
    }

    protected Ticket issueTicket(IBrokerReservation r, ArrayList<AssignmentForestNode> allocation, Term term, int units, AssignmentForest forest) throws Exception
    {
        ID[] sources = new ID[allocation.size()];
        ResourceType resourceType = forest.getResourceType();
        Properties properties = forest.getProperties();

        int i = 0;
        for (AssignmentForestNode bin : allocation) {
            sources[i++] = bin.getGuid();
        }

        HashMap<ID, ResourceBin> map = new HashMap<ID, ResourceBin>();
        HashMap<ID, ResourceTicket> sourceTickets = new HashMap<ID, ResourceTicket>();

        // scan through the allocation and record the bins and source tickets
        // that have been used
        for (AssignmentForestNode node : allocation) {
            ResourceBin bin = new ResourceBin(node);
            map.put(bin.getGuid(), bin);
            AssignmentForestInnerNode current = (AssignmentForestInnerNode) forest.getNode(bin.getParentGuid());
            while (current != null && current.getSourceTicket() == null) {
                bin = map.get(current.getGuid());
                if (bin == null) {
                    bin = new ResourceBin(current);
                    map.put(bin.getGuid(), bin);
                }
                current = (AssignmentForestInnerNode) forest.getNode(bin.getParentGuid());
            }
            if (current == null) {
                throw new Exception("did not find a source ticket");
            }
            ResourceTicket source = current.getSourceTicket();
            sourceTickets.put(source.getGuid(), source);
        }
        ResourceBin[] bins = (ResourceBin[]) map.values().toArray();
        ResourceTicket[] tickets = (ResourceTicket[]) sourceTickets.values().toArray();

        ResourceDelegation delegation = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, null, term, resourceType, sources, bins, properties, getClientID(r));
        // make a new ResourceTicket using the delegation and the source ticket
        ResourceTicket newResourceTicket = actor.getShirakoPlugin().getTicketFactory().makeTicket(tickets, delegation);
        // make the concrete set
        Ticket ticket = new Ticket(newResourceTicket, actor.getShirakoPlugin(), forest.getAuthorityProxy());
        return ticket;
    }
}
