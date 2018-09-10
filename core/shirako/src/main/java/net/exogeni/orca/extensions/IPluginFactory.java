/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.extensions;

import java.util.Properties;

import net.exogeni.orca.manage.internal.ManagementObject;


/**
 * Factory for extension objects (plugins). Any plugin factory must implement
 * this interface.
 */
public interface IPluginFactory
{
    /**
     * Creates all necessary objects that this factory produces. The environment
     * will call this method before calling any of the factory's accessor
     * methods.
     * @throws Exception in case of error
     */
    public void create() throws Exception;

    /**
     * Returns the object created by the factory. This method applies to
     * factories that create a single object. When calling this method the
     * environment knows the type of the object and what to do with it.
     * @return object
     */
    public Object getObject();

    /**
     * Returns the associated manager object
     * @return manager object. Can be null.
     */
    public ManagementObject getManager();

    /**
     * Passes configuration properties to be used by the factory.
     * @param p properties list
     * @throws Exception in case of error
     */
    public void configure(Properties p) throws Exception;

    /**
     * Passes configuration properties to be used by the factory.
     * @param p properties serialized as a string.
     * @throws Exception in case of error
     */
    public void configure(String p) throws Exception;
}
