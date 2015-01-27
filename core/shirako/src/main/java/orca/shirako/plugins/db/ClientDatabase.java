/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.plugins.db;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.util.Client;
import orca.util.ID;


/**
 * This interface describes the methods necessary to maintain information about
 * the clients of an actor that acts in a server role.
 */
public interface ClientDatabase
{
    /**
     * Adds a new database record representing this client
     * @param client
     * @throws Exception
     */
    public void addClient(Client client) throws Exception;

    /**
     * Updates the database record for the specified client
     * @param client
     * @throws Exception
     */
    public void updateClient(Client client) throws Exception;

    /**
     * Removes the specified client record
     * FIXME: remove once we start indexing using guid
     * @param name
     * @throws Exception
     */
    public void removeClient(String name) throws Exception;
    
    /**
     * Removes the specified client record
     * @param guid client guid
     * @throws Exception
     */
    public void removeClient(ID guid) throws Exception;
    /**
     * Retrieves the specified client record
     * FIXME: remove once we start indexing using guid
     * @param name
     * @return
     * @throws Exception
     */
    public Vector<Properties> getClient(String name) throws Exception;
    /**
     * Retrieves the specified client record
     * @param guid client guid
     * @return
     * @throws Exception
     */
    public Vector<Properties> getClient(ID guid) throws Exception;
    /**
     * Retrieves all client records
     * @return
     * @throws Exception
     */
    public Vector<Properties> getClients() throws Exception;
}