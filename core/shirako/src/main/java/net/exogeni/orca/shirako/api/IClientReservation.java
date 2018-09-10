/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ResourceType;


/**
 * <code>IClientReservation</code> defines the reservation interface for
 * actors acting as clients of other actors.
 */
public interface IClientReservation extends IReservation
{
    /**
     * Returns the authority who issued the lease for this reservation.
     *
     * @return site authority or null
     */
    public IAuthorityProxy getAuthority();

    /**
     * Returns a proxy to the broker linked to this reservation. Can be
     * null.
     *
     * @return broker linked to this reservation. Can be null.
     */
    public IBrokerProxy getBroker();

    /**
     * Returns the previous ticket term.
     *
     * @return previous ticket term
     */
    public Term getPreviousTicketTerm();

    /**
     * Returns the cached reservation renewal time. Used during
     * recovery.
     *
     * @return cached reservation renewal time
     */
    public long getRenewTime();

    /**
     * Returns the resources suggested to the policy for a new/extend
     * request for the reservation.
     *
     * @return suggested resources
     */
    public ResourceSet getSuggestedResources();

    /**
     * Returns the term suggested to the policy for a new/extend
     * request for the reservation.
     *
     * @return suggested term
     */
    public Term getSuggestedTerm();

    /**
     * Returns the most recently suggest resource type.
     *
     * @return suggested resource type.
     */
    public ResourceType getSuggestedType();

    /**
     * Returns the reservation sequence number for incoming
     * ticket/extend ticket messages.
     *
     * @return reservation sequence number for incoming ticket/extend ticket
     *         messages
     */
    public int getTicketSequenceIn();

    /**
     * Returns the reservation sequence number for outgoing
     * ticket/extend ticket messages.
     *
     * @return reservation sequence number for outgoing ticket/extend ticket
     *         messages
     */
    public int getTicketSequenceOut();

    /**
     * Returns the current ticket term. Note that <code>getTerm</code>
     * will return the currently active term. This can be either the ticket
     * term or the lease term.
     *
     * @return current ticket term
     */
    public Term getTicketTerm();

    /**
     * Checks if the reservation represents exported resources.
     *
     * @return true if the reservation represents exported resources
     */
    public boolean isExported();

    /**
     * Checks if the reservation is renewable.
     *
     * @return true if the reservation is renewable
     */
    public boolean isRenewable();

    /**
     * Sets the broker who will issue tickets for the reservation. This
     * method can be called only for reservations in the Nascent state.
     *
     * @param broker broker request tickets from
     *
     * @throws Exception if the reservation is in the wrong state
     */
    public void setBroker(IBrokerProxy broker) throws Exception;

    /**
     * Sets the exported flag.
     *
     * @param exported flag value
     */
    public void setExported(boolean exported);

    /**
     * Controls the renewable flag.
     *
     * @param renewable value for the renewable flag
     */
    public void setRenewable(boolean renewable);

    
    /**
     * Get the value of renewable flag
     * @return value of renewable flag
     */
    public boolean getRenewable();
    
    /**
     * Caches the reservation renewal time. This information is used to
     * simplify recovery.
     *
     * @param time reservation renewal time
     */
    public void setRenewTime(long time);

    /**
     * Indicates that a suggestion to the policy for resources and/or
     * term has been made.
     */
    public void setSuggested();

    /**
     * Sets the term and resources suggested to the policy for a
     * new/extend request for the reservation.
     *
     * @param term suggested term
     * @param resources suggested resources
     */
    public void setSuggested(Term term, ResourceSet resources);

    /**
     * Sets the resources suggested to the policy for a new/extend
     * request for the reservation.
     *
     * @param resources suggested resources
     */
    public void setSuggestedResources(ResourceSet resources);

    /**
     * Sets the term suggested to the policy for a new/extend request
     * for the reservation.
     *
     * @param term suggested term
     */
    public void setSuggestedTerm(Term term);

    /**
     * Sets the reservation sequence number for incoming ticket
     * messages.
     *
     * @param sequence sequence number
     */
    public void setTicketSequenceIn(int sequence);

    /**
     * Sets the reservation sequence number for outgoing ticket/extend
     * ticket messages.
     *
     * @param sequence sequence number
     */
    public void setTicketSequenceOut(int sequence);
    
    /**
     * Returns a string describing the reservation status.
     * 
     * @return status string
     */
    public String getUpdateNotices();
    
    /**
     * Sets the policy associated with this resservation.
     * @param policy policy
     */
    public void setPolicy(IClientPolicy policy);
    
    /**
     * Returns the client callback proxy for this reservation.
     * @return IClientCallbackProxy
     */
    public IClientCallbackProxy getClientCallbackProxy();
}
