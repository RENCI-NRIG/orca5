package net.exogeni.orca.shirako.common.delegation;

import net.exogeni.orca.shirako.common.ResourceVector;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ID;

/**
 * <code>ResourceBin</code> represents a grouping of resources with identical properties. 
 * Each resource bin consists of one or more physical units with a given resource vector.
 * Each resource bin has a unique identifier and a unique parent resource bin.
 * @author aydan
 */
public class ResourceBin
{
    /**
     * The bin's unique identifier.
     */
    protected ID guid;
    /**
     * Parent bin guid.
     */
    protected ID parentGuid;
    /**
     * Number of physical units available in this bin.
     */
    protected int physicalUnits;
    /**
     * Resource vector for each physical unit.
     */
    protected ResourceVector vector;
    
    protected Term term;
    
    public ResourceBin(ResourceBin bin)
    {
        this.guid = bin.guid;
        this.parentGuid = bin.parentGuid;
        this.physicalUnits = bin.physicalUnits;
        this.vector = bin.vector;
        this.term = bin.term;
    }
    
    public ResourceBin(ID parentGuid, int physicalUnits, ResourceVector vector, Term term)
    {
        this(new ID(), parentGuid, physicalUnits, vector, term);
    }
    
    public ResourceBin(ID parentGuid, int physicalUnits,Term term)
    {
        this(new ID(), parentGuid, physicalUnits, null, term);
    }

    public ResourceBin(int physicalUnits,Term term)
    {
        this(new ID(), null, physicalUnits, null, term);
    }

    /**
     * Makes a new resource bin.
     * @param guid unique identifier
     * @param parentGuid parentGuid
     * @param physicalUnits number of physical units
     * @param vector resource vector
     * @param term term
     */
    public ResourceBin(ID guid, ID parentGuid, int physicalUnits, ResourceVector vector, Term term)
    {
        this.guid = guid;
        this.parentGuid = parentGuid;
        this.physicalUnits = physicalUnits;
        this.vector = vector;
        this.term = term;
    }
    
    /**
     * Returns the unique identifier of the bin.
     * @return guid
     */
    public ID getGuid()
    {
        return guid;
    }
    
    /**
     * Returns the bin's parent identifier.
     * @return parent guid
     */
    public ID getParentGuid()
    {
        return parentGuid;
    }
    
    /**
     * Returns the number of physical units in the bin.
     * @return physical units
     */
    public int getPhysicalUnits()
    {
        return physicalUnits;
    }

    /**
     * Returns the resource vector of the bin.
     * @return resource vector
     */
    public ResourceVector getVector()
    {
        return vector;
    }
    
    public Term getTerm()
    {
        return term;
    }
}
