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


import net.exogeni.orca.shirako.api.IActor;


/**
 * Base interface of actor factory classes.
 */
public interface IActorFactory extends IPluginFactory
{
    /**
     * Sets the actor name.
     * @param name actor name string
     */
    public void setName(String name);

    /**
     * Sets the actor description.
     * @param description actor description string
     */
    public void setDescription(String description);

    /**
     * Returns the actor object created by this factory
     * @return actor object
     */
    public IActor getActor();
}
