package orca.shirako.common.delegation;

import java.util.HashMap;
import java.util.Properties;

import orca.shirako.common.ResourceVector;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.ResourceType;

/**
 * <code>ResourceDelegation</code> expresses a delegation of a number of resources with
 * identical properties from one actor to another. Each delegation is backed by resources
 * stored in one or more resource bins and covers a fixed time interval (term). Individual units
 * in a delegation are indistinguishable from each other.
 * @author aydan
 *
 */
public class ResourceDelegation
{
    /**
     * The delegation's unique identifier.
     */
    protected ID guid;
    /**
     * Lease interval.
     */
    protected Term term;
    /**
     * Number of units delegated. This is the total number of
     * units represented by the delegation and can be different
     * from the number of physical units
     */
    protected int units;    
    /**
     * Resource vector for each delegated unit.
     */
    protected ResourceVector vector;    
    /**
     * Resource type.
     */
    protected ResourceType type;
    /**
     * Resource properties (optional).
     */
    protected Properties properties;
    /**
     * Issuer identifier.
     */
    protected ID issuer;
    /**
     * Holder identifier.
     */
    protected ID holder;
    /**
     * Source bins used for this delegation.
     */
    protected ID[] sources;
    /**
     * Actor-local resource bins referenced by the delegation.
     */
    protected ResourceBin[] bins;

    @Override
    public String toString()
    {
    	StringBuffer sb = new StringBuffer();
    	sb.append("delegation=[");
    	sb.append("guid=");
    	sb.append(guid);
    	sb.append(",units=");
    	sb.append(units);
    	sb.append(",vector=");
    	sb.append(vector);
    	sb.append(",type=");
    	sb.append(type);
    	sb.append(",issuer=");
    	sb.append(issuer);
    	sb.append(",holder=");
    	sb.append(holder);
    	sb.append("]");
    	return sb.toString();
    }

    public ResourceDelegation(int units, ResourceVector vector, Term term, ResourceType type, 
                              ID[] sources, ResourceBin[] bins, Properties properties,
                              ID issuer, ID holder)
    {
        this.guid = new ID();
        this.units = units;
        this.vector = vector;
        this.term = term;
        this.type = type;
        this.properties = properties;
        this.issuer = issuer;
        this.holder = holder;
        this.sources = sources;
        this.bins = bins;
    }

    public boolean isValid()
    {
        if (guid == null || term == null || units <=0 || 
                vector == null || !vector.isPositive() || type == null
                || sources == null || sources.length == 0 || 
                bins == null || bins.length == 0){
            return false;
        }
        return true;
        
        
        // fixme: currently not checking issuer and holder
    }
    
    public ID getGuid()
    {
        return guid;
    }
    
    public ResourceVector getResourceVector()
    {
        return vector;
    }
    
    public ResourceType getResourceType()
    {
        return type;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public Term getTerm()
    {
        return term;
    }

    public int getUnits()
    {
        return units;
    }

    public ID getIssuer()
    {
        return issuer;
    }

    public ID getHolder()
    {
        return holder;
    }
    
    public void setIssuer(ID issuer)
    {
    	this.issuer = issuer;
    }
    
    public void setHolder(ID holder)
    {
    	this.holder = holder;
    }
    
    public void setUnits(int units) {
		this.units = units;
	}

	/**
     * Returns a hash map of all actor-local bins referenced by this delegation.
     * The map indexes bins by guid.
     * @return hash map
     */
    public HashMap<ID, ResourceBin> getMap()
    {
        // index the bins
        HashMap<ID, ResourceBin> result = new HashMap<ID, ResourceBin>();
        for (int i = 0; i < bins.length; i++){
            result.put(bins[i].guid, bins[i]);
        }
        
        return result;
    }

}
