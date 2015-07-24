/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage.internal.db;

import java.util.Properties;
import java.util.Vector;

import orca.manage.internal.api.IManagementObject;
import orca.util.ID;


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
     * @throws Exception
     */
    public void addManagerObject(IManagementObject manager) throws Exception;

    /**
     * Removes the specified manager object
     * @param id Unique identifier of the manager object
     * @throws Exception
     */
    public void removeManagerObject(ID id) throws Exception;

    /**
     * Removes all manager objects associated with the specified actor
     * @param actorName
     * @throws Exception
     */
    public void removeManagerObjects(String actorName) throws Exception;

    /**
     * Returns the specified manager object
     * @param key Key identifying the portal plugin
     * @return
     * @throws Exception
     */
    public Vector<Properties> getManagerObject(ID id) throws Exception;

    /**
     * Retrieves all manager object
     * @return
     * @throws Exception
     */
    public Vector<Properties> getManagerObjects() throws Exception;

    /**
     * Retrieves all manager objects associated with the given actor
     * @param actorName Name of the actor
     * @return
     * @throws Exception
     */
    public Vector<Properties> getManagerObjects(String actorName) throws Exception;

    /**
     * Retrieves all manager objects that are not associated with actors
     * @return
     * @throws Exception
     */
    public Vector<Properties> getManagerObjectsContainer() throws Exception;
}