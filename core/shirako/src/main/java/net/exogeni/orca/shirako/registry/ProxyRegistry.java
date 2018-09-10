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

import net.exogeni.orca.shirako.api.IAuthorityProxy;
import net.exogeni.orca.shirako.api.IBrokerProxy;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.proxies.Proxy;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;


class ProxyRegistry
{
    class ProtocolEntry
    {
        /**
         * All proxies
         */
        public Hashtable<String, IProxy> proxies;

        /**
         * All proxies to brokers
         */
        public Vector<IProxy> brokerProxies;

        /**
         * All proxies to sites
         */
        public Vector<IProxy> siteProxies;

        public ProtocolEntry()
        {
            proxies = new Hashtable<String, IProxy>();
            brokerProxies = new Vector<IProxy>();
            siteProxies = new Vector<IProxy>();
        }

        public void clear()
        {
            proxies.clear();
            brokerProxies.clear();
            siteProxies.clear();
        }
    }

    protected Hashtable<String, ProtocolEntry> protocols;

    public ProxyRegistry()
    {
        protocols = new Hashtable<String, ProtocolEntry>();
    }

    public void clear()
    {
        Iterator<ProtocolEntry> iter = protocols.values().iterator();

        while (iter.hasNext()) {
            ProtocolEntry table = iter.next();
            table.clear();
        }

        protocols.clear();
    }

    public Proxy[] getBrokerProxies(String protocol)
    {
        ProtocolEntry entry = protocols.get(protocol);

        if (entry == null) {
            return null;
        }

        Proxy[] result = new Proxy[entry.brokerProxies.size()];
        entry.brokerProxies.copyInto(result);

        return result;
    }

    public Proxy[] getProxies(String protocol)
    {
        ProtocolEntry entry = protocols.get(protocol);

        if (entry == null) {
            return null;
        }

        Proxy[] result = new Proxy[entry.proxies.size()];
        entry.proxies.values().toArray(result);

        return result;
    }

    public IProxy getProxy(String protocol, String actorName)
    {
        ProtocolEntry protocolTable = protocols.get(protocol);

        if (protocolTable == null) {
            return null;
        }

        return protocolTable.proxies.get(actorName);
    }

    public Proxy[] getSiteProxies(String protocol)
    {
        ProtocolEntry entry = protocols.get(protocol);

        if (entry == null) {
            return null;
        }

        Proxy[] result = new Proxy[entry.siteProxies.size()];
        entry.siteProxies.copyInto(result);

        return result;
    }

    /**
     * Registers the given callback
     * @param proxy
     */
    public void registerProxy(IProxy proxy)
    {
        String protocol = proxy.getType();
        ProtocolEntry protocolTable = protocols.get(protocol);

        if (protocolTable == null) {
            protocolTable = new ProtocolEntry();
            protocols.put(protocol, protocolTable);
        }

        String name = proxy.getIdentity().getName();

        if (!protocolTable.proxies.containsKey(name)) {
            protocolTable.proxies.put(name, proxy);

            if (proxy instanceof IAuthorityProxy) {
                protocolTable.siteProxies.add(proxy);
            }

            if (proxy instanceof IBrokerProxy) {
                protocolTable.brokerProxies.add(proxy);
            }
        }
    }

    /**
     * Unregisters any proxies for the specified actor
     * @param actorName Actor name
     */
    public void unregister(String actorName)
    {
        Iterator<ProtocolEntry> iter = protocols.values().iterator();

        while (iter.hasNext()) {
            ProtocolEntry entry = iter.next();
            IProxy proxy = entry.proxies.get(actorName);

            if (proxy != null) {
                entry.proxies.remove(actorName);

                if (proxy instanceof IAuthorityProxy) {
                    entry.siteProxies.remove(proxy);
                }

                if (proxy instanceof IBrokerProxy) {
                    entry.brokerProxies.remove(proxy);
                }
            }
        }
    }
}
