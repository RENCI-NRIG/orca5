/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.manage.internal.db;

import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.manage.internal.api.IManagementObject;
import net.exogeni.orca.util.ID;


/**
 * Specifies the database interface required to store and organize
 * @author aydan
 *
 */
public interface IManagementObjectDatabase
{
    /**
     * Registers a new manager object
     * @param manager The manager object
     * @throws Exception in case of error
     */
    public void addManagerObject(IManagementObject manager) throws Exception;

    /**
     * Removes the specified manager object
     * @param id Unique identifier of the manager object
     * @throws Exception in case of error
     */
    public void removeManagerObject(ID id) throws Exception;

    /**
     * Removes all manager objects associated with the specified actor
     * @param actorName actor name
     * @throws Exception in case of error
     */
    public void removeManagerObjects(String actorName) throws Exception;

    /**
     * Returns the specified manager object
     * @param id Key identifying the portal plugin
     * @return returns vector of the properties of the specified object
     * @throws Exception in case of error
     */
    public Vector<Properties> getManagerObject(ID id) throws Exception;

    /**
     * Retrieves all manager object
     * @return returns vector of the properties of the specified object
     * @throws Exception in case of error
     */
    public Vector<Properties> getManagerObjects() throws Exception;

    /**
     * Retrieves all manager objects associated with the given actor
     * @param actorName Name of the actor
     * @return returns vector of the properties of the specified object
     * @throws Exception in case of error
     */
    public Vector<Properties> getManagerObjects(String actorName) throws Exception;

    /**
     * Retrieves all manager objects that are not associated with actors
     * @return returns vector of the properties of the specified object
     * @throws Exception in case of error
     */
    public Vector<Properties> getManagerObjectsContainer() throws Exception;
}
