/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.core;

import orca.security.AuthToken;

import orca.shirako.api.IActorIdentity;

import orca.util.ID;


/**
 * Represents an actor's identity.
 */
public class ActorIdentity implements IActorIdentity
{
    /**
     * Auth token.
     */
    protected AuthToken auth;

    /**
         * Creates a new instance.
         * @param name actor name
         */
    public ActorIdentity(final String name, final ID guid)
    {
        this.auth = new AuthToken(name, guid);
    }

    /**
     * {@inheritDoc}
     */
    public ID getGuid()
    {
        return auth.getGuid();
    }

    /**
     * {@inheritDoc}
     */
    public AuthToken getIdentity()
    {
        return auth;
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return auth.getName();
    }
}