package orca.shirako.common.delegation;

import java.util.Properties;

import orca.shirako.common.ResourceVector;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.ResourceType;
import orca.util.persistence.Persistable;
import orca.util.persistence.Recover;
import orca.util.persistence.Recoverable;
import orca.util.persistence.Save;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * <code>ResourceTicket</code> represents a sequence of one or more delegations of resources
 * from one party to another. 
 * @author aydan
 *
 */
@Save(ResourceTicketSaver.class)
@Recover(ResourceTicketRecoverer.class)
public class ResourceTicket implements Persistable, Recoverable
{   
	@XStreamOmitField
	private IResourceTicketFactory factory;
	
    /**
     * All delegation records represented by this ticket.
     */
    protected ResourceDelegation[] delegations;
    
    // the delegation at 0 is the most recent delegation: the one the ticket refers to
 
    protected ResourceTicket() {}
    
    /**
     * Creates a root ticket from the specified delegation.
     * @param delegation root delegation
     */
    public ResourceTicket(IResourceTicketFactory factory, ResourceDelegation delegation)
    {
    	if (factory == null) {
    		throw new IllegalArgumentException("factory");
    	}
    	
    	this.factory = factory;
        this.delegations = new ResourceDelegation[1];
        this.delegations[0] = delegation;
    }

    /**
     * Creates a new ticket from the given source ticket and delegation. 
     * @param source source ticket
     * @param delegation delegation
     */
    public ResourceTicket(IResourceTicketFactory factory, ResourceTicket source, ResourceDelegation delegation)
    {    	
    	if (factory == null) {
    		throw new IllegalArgumentException("factory");
    	}
    	
    	this.factory = factory;
    	this.delegations = new ResourceDelegation[source.delegations.length + 1];
    	this.delegations[0] = delegation;
    	for (int i = 0; i < source.delegations.length; i++){
    		this.delegations[i+1] = source.delegations[i];
    	}
    }

    public ResourceTicket(IResourceTicketFactory factory, ResourceTicket[] sources, ResourceDelegation delegation)
    {
    	if (factory == null) {
    		throw new IllegalArgumentException("factory");
    	}
    	
    	this.factory = factory;
        int size = 1;
        for (int i = 0; i < sources.length; i++){
            size += sources[i].delegations.length;
        }
        this.delegations = new ResourceDelegation[size];
        this.delegations[0] = delegation;
        int index = 1;
        for (int i = 0; i < sources.length; i++){
            for (int j = 0; j < sources[i].delegations.length; i++){
                delegations[index++] = sources[i].delegations[j];
            }
        }
    }
    
    public boolean isValid()
    {
        if (delegations == null || delegations.length == 0){
            return false;
        }
        
        // check the validity of each delegation record
        for (int i = 0; i < delegations.length; i++){
            if (!delegations[i].isValid()){
                return false;
            }
        }

        // check parent guids;
//        ResourceDelegation current = getDelegation();        
//        for (int i = 1; i < delegations.length; i++){
//            ResourceDelegation parent = delegations[i];
//            if (parent.guid != current.parentGuid){
//                return false;
//            }
//            current = parent;
//        }
        
        // fixme: for sharp tickets we need to verify the hierarchical keys
        return true;
    }
    
    public ResourceDelegation getDelegation()
    {
        return delegations[0];
    }

    /**
     * Returns the resource type of the resource represented by this ticket.
     * @return
     */
    public ResourceType getResourceType()
    {
        return getDelegation().getResourceType();
    }

    /**
     * Returns a properties list with additional information about the resources
     * represented by this ticket.
     * @return
     */
    public Properties getProperties()
    {
        return getDelegation().getProperties();
    }

    public ID getIssuer()
    {
        return getDelegation().getIssuer();
    }

    public ID getHolder()
    {
        return getDelegation().getHolder();
    }

    /**
     * Returns the ticket unique identifier.
     * @return
     */
    public ID getGuid()
    {
        return getDelegation().guid;
    }

    /**
     * Returns the term represented by this ticket.
     * @return
     */
    public Term getTerm()
    {
        return getDelegation().getTerm();
    }

    /**
     * Returns the number of units represented by this ticket.
     * @return
     */
    public int getUnits()
    {
        return getDelegation().getUnits();
    }

    /**
     * Returns the resource vector of each unit represented by this ticket.
     * @return
     */
    public ResourceVector getResourceVector()
    {
        return getDelegation().vector;
    }
    
    public IResourceTicketFactory getFactory() {
    	return factory;
    }
    
    public void setFactory(IResourceTicketFactory factory) {
    	this.factory = factory;
    }
}
