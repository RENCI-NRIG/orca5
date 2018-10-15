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

import net.exogeni.orca.security.AuthToken;

import net.exogeni.orca.util.ID;


/**
 * <code>IActorIdentity</code> defines the interface required to represent the
 * identity of an actor. Each actor in Shirako is represented by a globally
 * unique identifier and an <code>AuthToken</code>.
 */
public interface IActorIdentity
{
    /**
     * Returns the globally unique identifier of this actor.
     *
     * @return actor guid
     */
    public ID getGuid();

    /**
     * Returns the identity of the actor.
     *
     * @return the actor's identity
     */
    public AuthToken getIdentity();

    /**
     * Returns the actor name.
     *
     * @return the actor name. Note that actor names are expected to be unique.
     */
    public String getName();
}
