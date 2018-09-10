/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.proxies.soapaxis2;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IActorIdentity;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.api.IBroker;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.ProtocolDescriptor;
import net.exogeni.orca.shirako.proxies.ActorLocation;
import net.exogeni.orca.shirako.proxies.IProxyFactory;
import net.exogeni.orca.shirako.proxies.Proxy;
import net.exogeni.orca.shirako.registry.ActorRegistry;

/**
 * A factory class for SOAP proxies and callbacks.
 */
public class SoapAxis2ProxyFactory implements IProxyFactory
{
    /**
     * {@inheritDoc}
     */
    public IProxy newProxy(final IActorIdentity identity, final ActorLocation location, String type)
    {
        Proxy result = null;
        IActor actor = ActorRegistry.getActor(identity.getName());

        if (actor != null) {
            ProtocolDescriptor d = location.getDescriptor();
            if (d != null && d.getLocation() != null) {
                // XXX: service name is linked to actor name
                String url = d.getLocation() + "/services/" + actor.getName();
                if (actor instanceof IAuthority) {
                    result = new SoapAxis2AuthorityProxy(url, actor.getIdentity(), actor.getLogger());
                } else if (actor instanceof IBroker) {
                    result = new SoapAxis2BrokerProxy(url, actor.getIdentity(), actor.getLogger());
                }
            }
        }
        else {
        	String url = location.getLocation();
        	if (type != null) {
        		if (type.equals("authority") || type.equals("site")) {
        			result = new SoapAxis2AuthorityProxy(url, identity.getIdentity(), Globals.getLogger(identity.getName()));
        		} else if (type.equals("agent") || type.equals("broker")) {
        			result = new SoapAxis2BrokerProxy(url, identity.getIdentity(), Globals.getLogger(identity.getName()));
        		} else {
        		    throw new RuntimeException("Unsupported proxy type: " + type);
        		}
        	} else {
        		throw new RuntimeException("Missing proxy type");
        	}
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ICallbackProxy newCallback(final IActorIdentity identity, final ActorLocation location)
    {
        ICallbackProxy result = null;
        IActor actor = ActorRegistry.getActor(identity.getName());

        if (actor != null) {
            ProtocolDescriptor d = location.getDescriptor();
            if (d != null && d.getLocation() != null) {
                // XXX: service name is linked to actor name
                String url = d.getLocation() + "/services/" + actor.getName();
                ActorRegistry.registerEndPoint(actor.getName(), IProxy.ProxyTypeSoapAxis2, url);
                result = new SoapAxis2Return(url, actor.getIdentity(), actor.getLogger());
            }
        }

        return result;
    }
}
