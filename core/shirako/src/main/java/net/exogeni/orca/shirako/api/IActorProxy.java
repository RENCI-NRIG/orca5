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

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;


/**
 * <code>IActorProxy</code> represents the proxy interface to a generic
 * Shirako actor.
 */
public interface IActorProxy extends IProxy {
    public IRPCRequestState prepareQuery(ICallbackProxy callback, Properties query, AuthToken caller);
}
