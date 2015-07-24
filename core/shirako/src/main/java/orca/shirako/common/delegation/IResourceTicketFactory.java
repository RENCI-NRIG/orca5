package orca.shirako.common.delegation;

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.common.ResourceVector;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.ResourceType;

public interface IResourceTicketFactory
{
	/**
	 * Converts the resource ticket to XML.
	 * @param ticket ticket to convert
	 * @return XML string representation
	 */
	String toXML(ResourceTicket ticket);
	
	/**
	 * Obtains a resource ticket from XML.
	 * @param xml XML string representation
	 * @return resource ticket
	 */
	ResourceTicket fromXML(String xml);
	
	/**
	 * Sets the actor this factory belongs to.
	 * @param actor
	 */
	void setActor(IActor actor);
	
	/**
	 * Returns the actor represented by this factory.
	 * @return
	 */
	IActor getActor();
	
	/**
	 * Initializes the factory.
	 * @throws Exception
	 */
	void initialize() throws Exception;
	
	/**
	 * Creates a new <code>ResourceDelegation</code> 
	 * @param units number of units
	 * @param vector resource vector
	 * @param term term
	 * @param type resource type
	 * @param sources source bins used for the delegation
	 * @param bins bins references by the delegation
	 * @param properties properties list
	 * @param holder identifier of the holder of the delegation
	 * @return
	 */
	ResourceDelegation makeDelegation(int units, ResourceVector vector, Term term, ResourceType type, 
			 ID[] sources, ResourceBin[] bins, Properties properties,
			 ID holder) throws DelegationException;

	
	/**
	 * Makes a root delegation.
	 * @param units
	 * @param term
	 * @param type
	 * @return
	 * @throws DelegationException
	 */
	ResourceDelegation makeDelegation(int units, Term term, ResourceType type) throws DelegationException;

    /**
     * Makes a root delegation.
     * @param units
     * @param term
     * @param type
     * @param properties
     * @return
     * @throws DelegationException
     */
    ResourceDelegation makeDelegation(int units, Term term, ResourceType type, Properties properties) throws DelegationException;

    /**
	 * Makes a delegation to the specified holder.
	 * @param units
	 * @param term
	 * @param type
	 * @param holder
	 * @return
	 * @throws DelegationException
	 */
	ResourceDelegation makeDelegation(int units, Term term, ResourceType type, ID holder) throws DelegationException;

    /**
     * Makes a delegation to the specified holder.
     * @param units
     * @param term
     * @param type
     * @param properties
     * @param holder
     * @return
     * @throws DelegationException
     */
    ResourceDelegation makeDelegation(int units, Term term, ResourceType type, Properties properties, ID holder) throws DelegationException;

    /**
	 * Creates a new root ticket.
	 * @param delegation root delegation
	 * @return
	 * @throws TicketException
	 */
	ResourceTicket makeTicket(ResourceDelegation delegation) throws TicketException;

	/**
	 * Creates a new ticket from the specified source ticket.
	 * @param source
	 * @param delegation
	 * @return
	 * @throws TicketException
	 */
	ResourceTicket makeTicket(ResourceTicket source, ResourceDelegation delegation) throws TicketException;
	
	/**
	 * Creates a new ticket from the specified source tickets.
	 * Note: all sources must share the same root delegation.
	 * @param sources source tickets
	 * @param delegation delegation
	 * @return
	 * @throws TicketException
	 */
	ResourceTicket makeTicket(ResourceTicket[] sources, ResourceDelegation delegation) throws TicketException;
	
	/**
	 * Makes a deep clone of the specified resource ticket.
	 * @param original
	 * @return
	 */
	ResourceTicket clone(ResourceTicket original);
	
}