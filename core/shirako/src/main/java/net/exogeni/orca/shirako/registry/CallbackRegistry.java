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

import net.exogeni.orca.shirako.api.ICallbackProxy;

import java.util.Hashtable;
import java.util.Iterator;


class CallbackRegistry
{
    protected Hashtable<String, Hashtable<String, ICallbackProxy>> protocols;

    public CallbackRegistry()
    {
        protocols = new Hashtable<String, Hashtable<String, ICallbackProxy>>();
    }

    public void clear()
    {
        Iterator iter = protocols.values().iterator();

        while (iter.hasNext()) {
            Hashtable table = (Hashtable) iter.next();
            table.clear();
        }

        protocols.clear();
    }

    public ICallbackProxy getCallback(String protocol, String actorName)
    {
        Hashtable<String, ICallbackProxy> protocolTable = protocols.get(protocol);

        if (protocolTable == null) {
            return null;
        }

        return protocolTable.get(actorName);
    }

    /**
     * Registers the given callback
     * @param callback
     */
    public void registerCallback(ICallbackProxy callback)
    {
        String protocol = callback.getType();
        Hashtable<String, ICallbackProxy> protocolTable = protocols.get(protocol);

        if (protocolTable == null) {
            protocolTable = new Hashtable<String, ICallbackProxy>();
            protocols.put(protocol, protocolTable);
        }

        protocolTable.put(callback.getIdentity().getName(), callback);
    }

    public void unregister(String actorName)
    {
        Iterator<Hashtable<String, ICallbackProxy>> tables = protocols.values().iterator();

        while (tables.hasNext()) {
            Hashtable<String, ICallbackProxy> table = tables.next();
            table.remove(actorName);
        }
    }
}
