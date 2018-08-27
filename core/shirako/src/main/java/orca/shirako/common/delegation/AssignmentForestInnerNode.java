package orca.shirako.common.delegation;

import java.util.Vector;
import java.util.logging.Logger;

import orca.shirako.common.ResourceVector;
import orca.shirako.time.Term;
import orca.shirako.time.calendar.AvailableResources;
import orca.shirako.time.calendar.ResourceDelegationCalendar;
import orca.util.ID;

// Nodes:
// - extract: subtract vector from all units in the bin
// - split: subtract x units from the bin. cannot be performed directly if the
// bin has
// already been used as the target of an extract operation. In this case the
// desired vector must be extracted into a new bin, which can
// then be split into the desired number of physical machines

public class AssignmentForestInnerNode extends AssignmentForestNode
{
    /**
     * List of children nodes: bin pools created from this bin pool.
     */
    protected Vector<AssignmentForestNode> children;
    /**
     * Resource availability calendar.
     */
    protected ResourceDelegationCalendar calendar;
    
    /**
     * The source ticket for this node (can be null).
     */
    protected ResourceTicket sourceTicket;
    
    public AssignmentForestInnerNode(ID guid, ID parentGuid, int physicalUnits, ResourceVector vector, Term term)
    {
        super(guid, parentGuid, physicalUnits, vector, term);
        this.children = new Vector<AssignmentForestNode>();
        this.calendar = new ResourceDelegationCalendar(term.getNewStartTime().getTime(), term.getNewStartTime().getTime(), physicalUnits, vector);
    }

    public AssignmentForestInnerNode(ID parentGuid, int physicalUnits, ResourceVector vector, Term term)
    {
        this(new ID(), parentGuid, physicalUnits, vector, term);
    }

    public AssignmentForestInnerNode(ResourceBin bin)
    {
        super(bin);
    }
    
    protected void addChild(AssignmentForestNode child)
    {
        child.parent = this;
        child.parentGuid = this.guid;
        this.children.add(child);
    }

    /**
     * Splits the specified number of physical units from the current bin into a
     * new inventory node.
     * @param term term for the new node
     * @param physicalUnits number of physical units to split
     * @return resulting inventory node
     * @throws Exception in case of error
     */
    public AssignmentForestInnerNode split(Term term, int physicalUnits) throws Exception
    {
        return (AssignmentForestInnerNode) split(term, physicalUnits, false);
    }

    public AssignmentForestNode allocate(Term term, int physicalUnits) throws Exception
    {
        return split(term, physicalUnits, true);
    }

    // Note: does not add the node to the tree
    private AssignmentForestNode split(Term term, int physicalUnits, boolean allocate) throws Exception
    {
        assert term != null;
        assert physicalUnits > 0;
        
        AssignmentForestNode node = null;
        
        long start = term.getNewStartTime().getTime();
        long end = term.getEndTime().getTime();
     
        // check if an extraction operation has already taken place
        // over the specified interval
        if (calendar.hasBeenExtracted(start, end)){
            throw new Exception("Cannot split: an extraction operation has taken place over a portion of the selected interval");
        }
        
        // check the feasibility
        long availableUnits = calendar.getMinUnits(start, end);
        if (availableUnits < physicalUnits){
            throw new Exception("Cannot split: insufficient units. Need " + physicalUnits + "but only " + availableUnits + " are available");
        }
       
        // update the calendar: subtract only physicalUnits
        AvailableResources[] over = calendar.reserve(start, end, physicalUnits);
        assert over == null;
        
        if (allocate){
            node = new AssignmentForestNode(this.guid, physicalUnits, vector, term);
        } else {
            // make an inventory node
            node = new AssignmentForestInnerNode(this.guid, physicalUnits, vector, term);
        }
        
        addChild(node);
        return node;
    }

    /**
     * Extracts resources from the current bin. The specified virtual units,
     * each with the given resource vector, are extracted from each physical
     * unit of the resource bin. The total number of extracted units is thus:
     * X*virtualUnits, where X is the number of physical units inside the
     * current resource bin.
     * @param term term
     * @param virtualUnits virtual units to extract from each physical unit
     * @param vector resource vector for each virtual unit
     * @return resulting node
     * @throws Exception in case of error
     */
    public AssignmentForestInnerNode extract(Term term, int virtualUnits, ResourceVector vector) throws Exception
    {
        return (AssignmentForestInnerNode) extract(term, virtualUnits, vector, false);
    }

    public AssignmentForestInnerNode allocate(Term term, int virtualUnits, ResourceVector vector) throws Exception
    {
        return (AssignmentForestInnerNode) extract(term, virtualUnits, vector, true);
    }

    public AssignmentForestNode extract(Term term, int virtualUnits, ResourceVector vector, boolean allocate) throws Exception
    {
        assert term != null;
        assert virtualUnits > 0;
        assert vector != null;

        AssignmentForestNode node = null;

        long start = term.getNewStartTime().getTime();
        long end = term.getEndTime().getTime();

        // check if a split operation has already taken place
        // over the specified interval
        if (calendar.hasBeenSplit(start, end)) {
            throw new Exception("Cannot extract: a slpit operation has taken place over a portion of the selected interval");
        }

        // check the feasibility

        // make the new resource vector:
        // virtualUnits * vector
        ResourceVector newVector = new ResourceVector(vector);
        newVector.multiply(virtualUnits);
        ResourceVector availableVector = calendar.getMinVector(start, end);
        if (!availableVector.containsOrEquals(newVector)) {
            throw new Exception("Cannot extract: insufficient units. Need " + newVector + " but only " + availableVector + " is available");
        }
        // subtract newVector from EACH physical unit
        AvailableResources[] over = calendar.reserve(start, end, newVector);
        assert over == null;

        if (allocate){
            node = new AssignmentForestNode(this.guid, physicalUnits, newVector, term);
        } else {
            // make an inventory node
            node = new AssignmentForestInnerNode(this.guid, physicalUnits, newVector, term);
        }
        
        addChild(node);
        return node;
    }
    
    public ResourceTicket getSourceTicket()
    {
        return sourceTicket;
    }     
    
    public int getMinAvailableUnits(long start, long end)
    {
        return (int)calendar.getMinUnits(start, end);
    }
}
