/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.container.api;


import java.util.Properties;
import java.util.Vector;

import orca.extensions.internal.db.IExtensionsDatabase;
import orca.manage.internal.User;
import orca.shirako.api.IActor;
import orca.shirako.common.UnitID;
import orca.shirako.core.Unit;
import orca.util.ID;
import orca.util.Initializable;
import orca.util.persistence.Persistable;


/**
 * Interface for container-level databases. Defines the functions necessary for
 * bootstrapping and recovering a shirako container.
 */
public interface IOrcaContainerDatabase extends IExtensionsDatabase, Initializable, Persistable
{
    /**
     * Adds the container properties list to the database.
     *
     * @param p container properties list.
     *
     * @throws Exception if an error occurs while accessing the database
     */
    void addContainerProperties(Properties p) throws Exception;

    /**
     * Updates the container properties list.
     *
     * @param p container properties list.
     *
     * @throws Exception if an error occurs while accessing the database
     */
    void updateContainerProperties(Properties p) throws Exception;

    /**
         * Retrieves the container properties list.
         * @return container properties list
         * @throws Exception if an error occurs while accessing the database
         */
    Vector<Properties> getContainerProperties() throws Exception;

    /**
     * Controls whether the database should reset its state.
     *
     * @param value TRUE if reset is required, FALSE otherwise
     */
    void setResetState(boolean value);

    /**
     * Retrieves the actors defined in this container
     */
    public Vector<Properties> getActors() throws Exception;

    /**
     * Retrieves the actors defined in this container
     * @param name actor name query string
     * @param type actor type (seed AbstractActor.Type*)
     * @return
     * @throws Exception
     */
    public Vector<Properties> getActors(String name, int type) throws Exception;

    /**
     * Retrieves the specified actor record
     * @param name actor name
     * @throws Exception
     */
    public Vector<Properties> getActor(String name) throws Exception;

    /**
     * Adds a new actor record to the database
     * @param actor actor to be added
     */
    public void addActor(IActor actor) throws Exception;

    /**
     * Removes the specified actor record
     * @param actorName actor name
     * @throws Exception
     */
    public void removeActor(String actorName) throws Exception;

    /**
     * Destroy the database for this actor. Applies to actors storing their
     * database on the same database server as the container database.
     * @param actorName actor name
     * @throws Exception
     */
    public void removeActorDatabase(String actorName) throws Exception;

    /**
     * Updates the actor's database record
     * @param actor
     */
    public void updateActor(IActor actor) throws Exception;

    /**
     * Adds the time record to the database
     * @param p
     * @throws Exception
     */
    public void addTime(Properties p) throws Exception;

    /**
     * Retrieves the time record from the database
     * @return
     * @throws Exception
     */
    public Vector<Properties> getTime() throws Exception;
    
    /**
     * Retrieves all user records
     * @return
     * @throws Exception
     */
    public Vector<Properties> getUsers() throws Exception;

    /**
     * Retrieves the specified user record
     * @param userName user name
     * @return
     * @throws Exception
     */
    public Vector<Properties> getUser(String userName) throws Exception;

    public Vector<Properties> getUser(String userName, String password) throws Exception;

    /**
     * Creates a new user record
     * @param user user
     * @throws Exception
     */
    public void addUser(User user) throws Exception;

    /**
     * Sets the password for the specified user.
     * @param login
     * @param password
     * @throws Exception
     */
    public void setUserPassword(String login, String password) throws Exception;
    /**
     * Updates the specified user record
     * @param user
     * @throws Exception
     */
    public void updateUser(User user) throws Exception;

    /**
     * Removes the specified user record
     * @param user
     * @throws Exception
     */
    public void removeUser(String user) throws Exception;

    public void addInventory(Unit u) throws Exception;
    public void removeInventory(UnitID uid) throws Exception;
    public void updateInventory(Unit u) throws Exception;
    public Properties getInventory(String name) throws Exception;
    public Vector<Properties> getInventory() throws Exception;
    public void transferInventory(String inventoryName, ID actorGuid) throws Exception;
    public void untransferInventory(String inventoryName, ID actorGuid) throws Exception;
}