package net.exogeni.orca.shirako.common.delegation;

import net.exogeni.orca.shirako.common.ResourceVector;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ID;

public class AssignmentForestNode extends ResourceBin
{
    /**
     * Pointer to the parent.
     */
    protected AssignmentForestNode parent;

    public AssignmentForestNode(ID guid, ID parentGuid, int physicalUnits, ResourceVector vector, Term term)
    {
        super(guid, parentGuid, physicalUnits, vector, term);        
    }
    
    public AssignmentForestNode(ID parentGuid, int physicalUnits, ResourceVector vector, Term term)
    {
        this(new ID(), parentGuid, physicalUnits, vector, term);        
    }

    public AssignmentForestNode(ResourceBin bin)
    {
        super(bin);
    }
    
    public AssignmentForestNode getParent()
    {
        return parent;
    }
}
