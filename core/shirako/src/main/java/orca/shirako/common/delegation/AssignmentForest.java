package orca.shirako.common.delegation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.time.calendar.AvailableResources;
import orca.util.ID;
import orca.util.ResourceType;

/**
 * An <code>AssignmentForest</code> represents the sequences of assignment
 * choices performed by a broker over its inventory. An assignment forest covers
 * identical resources grouped into one or more resource bins. The forest has
 * one or more root bins, which represent resources delegated to the broker from
 * other brokers/sites. Each non-root node, represents a subdivision of a parent
 * resource bin. The subdivision can represent a delegation to another broker or
 * service manager, or can simply be an internal grouping operation intended to
 * optimize the broker's allocation decisions.
 * @author aydan
 */
public class AssignmentForest
{
    /**
     * All nodes in the forest
     */
    protected HashMap<ID, AssignmentForestNode> nodes;
    /**
     * All root nodes.
     */
    protected HashSet<AssignmentForestNode> roots;
    /**
     * All inventory nodes: nodes from which we can allocate
     */
    protected HashSet<AssignmentForestNode> inventory;
    /**
     * All allocated nodes.
     */
    protected HashSet<AssignmentForestNode> allocated;
    /**
     * Resource type for this forest: all entries in the forest must have the
     * same resource type.
     */
    protected ResourceType resourceType;
    /**
     * Resource properties for resources in this forest: all entries in the
     * forest must have the same resource properties. FIXME: is this too
     * restrictive? Should we allow for different resource properties if we
     * obtain resources from different parent brokers? Probably in this case, it
     * will be best to create a separate forest.
     */
    protected Properties properties;

    /**
     * Source tickets.
     */
    protected HashMap<ID, ResourceTicket> sourceTickets;
    
    /**
     * Authority proxy.
     */
    protected IAuthorityProxy authorityProxy;
    
    /**
     * Creates a new assignment forest.
     */
    public AssignmentForest()
    {
        this.nodes = new HashMap<ID, AssignmentForestNode>();
        this.roots = new HashSet<AssignmentForestNode>();
        this.inventory = new HashSet<AssignmentForestNode>();
        this.allocated = new HashSet<AssignmentForestNode>();
        this.sourceTickets = new HashMap<ID, ResourceTicket>();
    }

    /**
     * Returns the roots of the forest.
     * @return
     */
    public AssignmentForestInnerNode[] getRoots()
    {
        return (AssignmentForestInnerNode[]) roots.toArray();
    }

    /**
     * Returns all inventory nodes.
     * @return
     */
    public AssignmentForestInnerNode[] getInventory()
    {
        return (AssignmentForestInnerNode[]) inventory.toArray();        
    }
    
    /**
     * Adds an allocated node to the forest. Note: the node should have already
     * been added to its parent's list of children.
     * @param node
     */
    public void addAllocatedNode(AssignmentForestNode node)
    {
        nodes.put(node.guid, node);
        allocated.add(node);
    }

    /**
     * Adds an inventory node. Note: the node should have already been added to
     * its parent's list of children.
     * @param node
     */
    public void addInventoryNode(AssignmentForestInnerNode node)
    {
        nodes.put(node.guid, node);
        inventory.add(node);
    }

    protected void addRootNode(AssignmentForestNode node)
    {
        roots.add(node);
    }

    /**
     * Adds a source ticket to the forest.
     * @param ticket
     * @throws Exception
     */
    public void addSourceTicket(ResourceTicket ticket, IAuthorityProxy proxy) throws Exception
    {
        addTicket(ticket, proxy);
    }

    /**
     * Adds an already allocated ticket to the forest.
     * @param ticket
     * @throws Exception
     */
    public void addAllocatedTicket(ResourceTicket ticket) throws Exception
    {
        addTicket(ticket, null);
    }

    protected void addTicket(ResourceTicket ticket, IAuthorityProxy proxy) throws Exception
    {
        assert ticket != null;
        assert ticket.isValid();

        ResourceTicket sourceTicket = null;
        if (proxy != null) {
            sourceTicket = ticket;
            ResourceDelegation root = ticket.getDelegation();
            if (resourceType == null) {
                resourceType = root.getResourceType();
                properties = root.getProperties();
                authorityProxy = proxy;
            }
            if (!resourceType.equals(root.getResourceType())) {
                throw new Exception("Passed in ticket has a different resource type. Forest type: " + resourceType + " ticket type: " + root.getResourceType());
            }
            // FIXME: need a way to check resource properties
        }

        for (int i = ticket.delegations.length - 1; i >= 0; i--) {
            addDelegation(ticket.delegations[i], i == (ticket.delegations.length - 1), i == 0, sourceTicket);
        }
    }

    protected void addDelegation(ResourceDelegation del, boolean root, boolean leaf, ResourceTicket sourceTicket) throws Exception
    {
        // index the bins
        HashMap<ID, ResourceBin> bins = del.getMap();

        // process the bins that are part of this delegation
        for (int i = 0; i < del.sources.length; i++) {
            // make the path of bins from the leaf bin to the root bin
            ResourceBin[] path = getBinPath(del.sources[i], bins);
            addPath(path, root, leaf, sourceTicket);
        }
    }

    protected void addPath(ResourceBin[] path, boolean root, boolean leaf, ResourceTicket sourceTicket) throws Exception
    {
        // we go top-down within the delegation

        for (int i = path.length - 1; i >= 0; i--) {
            // get the current bin
            ResourceBin bin = path[i];
            long start = bin.term.getNewStartTime().getTime();
            long end = bin.term.getEndTime().getTime();
            // do we have a node for it?
            AssignmentForestNode node = nodes.get(bin.guid);
            AssignmentForestInnerNode parent = null;

            if (bin.getParentGuid() != null) {
                parent = (AssignmentForestInnerNode) nodes.get(bin.parentGuid);
                if (parent == null) {
                    throw new Exception("Missing parent bin: " + bin.parentGuid);
                }
            }

            if (node == null) {
                if (sourceTicket != null) {
                    node = new AssignmentForestInnerNode(bin);
                    if (parent != null) {
                        parent.addChild(node);
                    }
                    // add the node to the forest
                    nodes.put(node.guid, node);
                    if (leaf && i == 0) {
                        ResourceTicket ticket = sourceTickets.get(sourceTicket.getGuid());
                        if (ticket == null){
                            ticket = sourceTicket;
                            sourceTickets.put(sourceTicket.getGuid(), sourceTicket);
                        }
                        // we can allocate from this bin
                        // remember the source ticket
                        ((AssignmentForestInnerNode)node).sourceTicket = ticket;
                        // add to the inventory set
                        inventory.add(node);                        
                    }
                    if (root && (i == path.length - 1)) {
                        addRootNode(node);
                    }
                } else {
                    if (leaf && i == 0) {
                        node = new AssignmentForestNode(bin);
                    } else {
                        node = new AssignmentForestInnerNode(bin);
                    }
                    if (parent != null) {
                        parent.addChild(node);
                    }
                    nodes.put(node.guid, node);
                    if (leaf && i == 0) {
                        allocated.add(node);                        
                    }
                }
            }

            if (parent != null) {
                // check for oversubscription
                AvailableResources[] avl = null;
                if (isSplit(bin, parent)) {
                    avl = parent.calendar.reserve(start, end, bin.physicalUnits);
                } else if (isExtracted(bin, parent)) {
                    avl = parent.calendar.reserve(start, end, bin.vector);
                } else {
                    throw new Exception("Bin is neither split nor extracted");
                }

                if (avl != null) {
                    throw new Exception("Detected oversubscription");
                }
            }
        }
    }

    /**
     * Returns the path from the specified bin to the top of the local
     * delegation chain.
     * @param binID starting bin guid
     * @param bins hashmap of bins
     * @return array of <code>ResourceBin</code>-s.
     * @throws Exception
     */
    protected ResourceBin[] getBinPath(ID binID, HashMap<ID, ResourceBin> bins) throws Exception
    {
        ID currentID = binID;
        ArrayList<ResourceBin> path = new ArrayList<ResourceBin>();
        while (currentID != null) {
            ResourceBin cb = bins.get(currentID);
            if (cb == null) {
                // fixme: handle gracefully
                throw new Exception("Missing bin");
            }
            path.add(cb);
            currentID = cb.parentGuid;
        }
        return (ResourceBin[]) path.toArray();
    }

    /**
     * Checks if child was created from parent using a split operation.
     * @param child
     * @param parent
     * @return
     */
    protected boolean isSplit(ResourceBin child, ResourceBin parent)
    {
        // FIXME: should this be <=?
        if (child.physicalUnits < parent.physicalUnits && child.vector.equals(parent.vector)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if child was created from parent using an extract operation.
     * @param child
     * @param parent
     * @return
     */
    protected boolean isExtracted(ResourceBin child, ResourceBin parent)
    {
        if (child.physicalUnits == parent.physicalUnits && parent.vector.containsOrEquals(child.vector)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the resource type of this forest.
     * @return
     */
    public ResourceType getResourceType()
    {
        return resourceType;
    }

    /**
     * Returns the properties for the resources represented by this forest.
     * @return
     */
    public Properties getProperties()
    {
        return properties;
    }
    
    public AssignmentForestNode getNode(ID guid)
    {
        return nodes.get(guid);
    }
    
    public IAuthorityProxy getAuthorityProxy()
    {
        return authorityProxy;
    }
}
