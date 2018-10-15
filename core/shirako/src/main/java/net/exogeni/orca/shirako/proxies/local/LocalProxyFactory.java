/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.proxies.local;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IActorIdentity;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.api.IBroker;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.proxies.ActorLocation;
import net.exogeni.orca.shirako.proxies.IProxyFactory;
import net.exogeni.orca.shirako.proxies.Proxy;
import net.exogeni.orca.shirako.registry.ActorRegistry;


public class LocalProxyFactory implements IProxyFactory
{
    public IProxy newProxy(IActorIdentity identity, ActorLocation location, String type)
    {
        Proxy result = null;
        IActor actor = ActorRegistry.getActor(identity.getName());

        if (actor != null) {
            if (actor instanceof IAuthority) {
                result = new LocalAuthority(actor);
            } else if (actor instanceof IBroker) {
                result = new LocalBroker(actor);
            }
        }

        return result;
    }

    public ICallbackProxy newCallback(IActorIdentity identity, ActorLocation location)
    {
        ICallbackProxy result = null;
        IActor actor = ActorRegistry.getActor(identity.getName());

        if (actor != null) {
            result = new LocalReturn(actor);
        }

        return result;
    }
}
