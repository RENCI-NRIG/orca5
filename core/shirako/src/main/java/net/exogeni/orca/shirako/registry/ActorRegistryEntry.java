/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.registry;

import net.exogeni.orca.shirako.api.IActor;

import java.util.Hashtable;


class ActorRegistryEntry
{
    /**
     * The actor
     */
    public IActor actor;

    /**
     * Protocol specific strings describing the endpoint through which this
     * actor can be used organized by protocol string.
     */
    public Hashtable endPoints;

    public ActorRegistryEntry(IActor actor)
    {
        this.actor = actor;
        this.endPoints = new Hashtable();
    }

    public IActor getActor()
    {
        return actor;
    }

    public String getEndPoint(String protocol)
    {
        return (String) endPoints.get(protocol);
    }

    public void registerEndPoint(String protocol, String endPoint)
    {
        endPoints.put(protocol, endPoint);
    }
}
