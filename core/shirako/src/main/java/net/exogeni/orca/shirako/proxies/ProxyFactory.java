/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.proxies;

import java.util.Hashtable;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.shirako.api.IActorIdentity;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.proxies.local.LocalProxyFactory;
import net.exogeni.orca.shirako.proxies.soapaxis2.SoapAxis2ProxyFactory;


public class ProxyFactory
{
    static ProxyFactory instance = new ProxyFactory();

    public static ICallbackProxy newCallback(String protocol, IActorIdentity identity,
                                             ActorLocation location)
    {
        ICallbackProxy result = null;
        IProxyFactory factory = instance.factories.get(protocol);

        if (factory != null) {
            result = factory.newCallback(identity, location);
        }

        return result;
    }

    public static IProxy newProxy(String protocol, IActorIdentity identity, ActorLocation location, String type)
    {
        IProxy result = null;
        IProxyFactory factory = instance.factories.get(protocol);

        if (factory != null) {
            result = factory.newProxy(identity, location,type);
        }

        return result;
    }

    /**
     * Hashtable of supported proxy factories.
     * Maps a protocol name to a factory.
     */
    private Hashtable<String, IProxyFactory> factories;

    /**
     * Creates a new instance.
     */
    private ProxyFactory()
    {
        factories = new Hashtable<String, IProxyFactory>();
        loadFactories();
    }

    /**
     * Populates the factories table.
     */
    private void loadFactories()
    {
        factories.put(OrcaConstants.ProtocolLocal, new LocalProxyFactory());
        factories.put(OrcaConstants.ProtocolSoapAxis2, new SoapAxis2ProxyFactory());
    }
}
