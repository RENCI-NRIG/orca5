/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import orca.security.AuthToken;

import orca.shirako.time.Term;
import orca.util.ResourceType;

import java.util.Properties;


/**
 * <code>ITicket</code> represent the interface for ticket implementations. A
 * ticket is a promise for resources. Each ticket covers a number of resource
 * units for a single resource type over a specified period of time. A ticket also contains
 * a properties list, which can be used to specify additional information.
 */
public interface ITicket extends Cloneable
{
    /**
     * Creates a clone of the ticket.
     *
     * @return ticket clone
     */
    public Object clone();

    /**
     * Creates a new ticket from the ticket.
     *
     * @param issuer entity issuing the ticket
     * @param holder entity the ticket is issued to
     * @param units number of units to extract
     * @param term term for the new ticket
     * @param properties properties list for the new ticket.
     *
     * @return a new ticket
     *
     * @throws Exception if ticket extraction fails
     */
    public ITicket extract(AuthToken issuer, AuthToken holder, int units, Term term,
                           Properties properties) throws Exception;

    /**
     * Returns the ticket properties.
     *
     * @return ticket properties
     */
    public Properties getProperties();

    /**
     * Returns the term of the ticket.
     *
     * @return ticket term
     */
    public Term getTerm();

    /**
     * Returns the resource type of the resources bound by the ticket.
     *
     * @return ticket resource type
     */
    public ResourceType getType();

    /**
     * Returns the number of units bound to the ticket.
     *
     * @return ticketed units
     */
    public int getUnits();
}