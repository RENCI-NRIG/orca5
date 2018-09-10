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
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.container.Globals;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Maintains a collection of uniquely named actors residing in a given JVM or
 * known to the JVM (singleton), indexed by type and by owner.
 * getActor(String) now looks up EITHER by name or guid - tries one, then the other
 */
public class ActorRegistry
{
    private static ActorRegistry instance = new ActorRegistry();

    /**
     * Resets the registry: removes all actors, proxies, and callbacks
     * @throws Exception in case of error
     */
    public static void clear() throws Exception
    {
        instance.clearPrivate();
    }

    /**
     * Get local actor entry based on either name or guid
     * @param actorNameOrGuid actorNameOrGuid
     * @return local actor
     */
    public static IActor getActor(String actorNameOrGuid)
    {
        return instance.getActorPrivate(actorNameOrGuid);
    }

    public static IActor[] getActors()
    {
        return instance.getActorsPrivate();
    }

    public static IProxy[] getBrokerProxies(String protocol)
    {
        return instance.getBrokerProxiesPrivate(protocol);
    }

    public static ICallbackProxy getCallback(String protocol, String actorName)
                                      throws Exception
    {
        return instance.getCallbackPrivate(protocol, actorName);
    }

    public static String getEndPoint(String protocol, String actorName) throws Exception
    {
        return instance.getEndPointPrivate(protocol, actorName);
    }

    public static IProxy[] getProxies(String protocol)
    {
        return instance.getProxiesPrivate(protocol);
    }

    public static IProxy getProxy(String protocol, String actorName) throws Exception
    {
        return instance.getProxyPrivate(protocol, actorName);
    }

    public static IProxy[] getSiteProxies(String protocol)
    {
        return instance.getSiteProxiesPrivate(protocol);
    }

    /**
     * Registers an actor
     * @param actor actor
     * @throws Exception in case of error
     */
    public static void registerActor(IActor actor) throws Exception
    {
    	Globals.Log.debug("Registering actor " + actor.getName() + " with the ActorRegistry");
        instance.registerActorPrivate(actor);
    }

    public static void registerCallback(ICallbackProxy callback) throws Exception
    {
    	Globals.Log.debug("Registering callback for actor: " + callback.getIdentity() + " type: " + callback.getType());
        instance.registerCallbackPrivate(callback);
    }

    public static void registerEndPoint(String actorName, String protocol, String endPoint)
    {
        instance.registerEndPointPrivate(actorName, protocol, endPoint);
    }

    /**
     * Register a proxy to an actor
     * @param proxy proxy
     * @throws Exception in case of error
     */
    public static void registerProxy(IProxy proxy) throws Exception
    {
    	Globals.Log.debug("Registering proxy for actor: " + proxy.getIdentity().getName() + " type: " + proxy.getType());
        instance.registerProxyPrivate(proxy);
    }

    public static void unregister(IActor actor)
    {
    	Globals.Log.debug("Unregistering actor: " + actor.getName() + " from the ActorRegistry");
        instance.unregisterPrivate(actor);
    }

    private Hashtable<String, ActorRegistryEntry> actors;
    private Hashtable<String, ActorRegistryEntry> actorsByGuid;
    private CallbackRegistry callbacks;
    private ProxyRegistry proxies;
    private ReentrantReadWriteLock lock;

    private ActorRegistry()
    {
        actors = new Hashtable<String, ActorRegistryEntry>();
        actorsByGuid = new Hashtable<String, ActorRegistryEntry>();
        proxies = new ProxyRegistry();
        callbacks = new CallbackRegistry();
        lock = new ReentrantReadWriteLock();
    }

    public void clearPrivate() throws Exception
    {
        lock.writeLock().lock();

        try {
            // remove all actors
            actors.clear();
            actorsByGuid.clear();
            // remove all proxies
            proxies.clear();
            // remove all callbacks
            callbacks.clear();

            // actorsByOwner.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected IActor getActorPrivate(String actorNameorGuid)
    {
        lock.readLock().lock();

        try {
            IActor result = null;
            ActorRegistryEntry entry = (ActorRegistryEntry) actors.get(actorNameorGuid);

            if (entry != null) {
                result = entry.actor;
            } else {
            	entry = (ActorRegistryEntry) actorsByGuid.get(actorNameorGuid);
            	if (entry != null)
            		result = entry.actor;
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    // XXX: changed method from synchronized to use the read lock /ib 04-08-2011
    private IActor[] getActorsPrivate()
    {
    	lock.readLock().lock();
    	try {
    		IActor[] result = new IActor[actors.size()];

    		int index = 0;
    		Iterator iter = actors.entrySet().iterator();

    		while (iter.hasNext()) {
    			Map.Entry entry = (Map.Entry) iter.next();
    			ActorRegistryEntry e = (ActorRegistryEntry) entry.getValue();
    			result[index++] = e.getActor();
    		}

    		return result;
    	} finally {
    		lock.readLock().unlock();
    	}
    }

    private IProxy[] getBrokerProxiesPrivate(String protocol)
    {
        lock.readLock().lock();

        try {
            return proxies.getBrokerProxies(protocol);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected ICallbackProxy getCallbackPrivate(String protocol, String actorName)
                                         throws Exception
    {
        lock.readLock().lock();

        try {
            return callbacks.getCallback(protocol, actorName);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected String getEndPointPrivate(String protocol, String actorName)
                                 throws Exception
    {
        lock.readLock().lock();

        try {
            String result = null;
            ActorRegistryEntry entry = (ActorRegistryEntry) actors.get(actorName);

            if (entry != null) {
                result = entry.getEndPoint(protocol);
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private IProxy[] getProxiesPrivate(String protocol)
    {
        lock.readLock().lock();

        try {
            return proxies.getProxies(protocol);
        } finally {
            lock.readLock().unlock();
        }
    }

    private IProxy getProxyPrivate(String protocol, String actorName) throws Exception
    {
        lock.readLock().lock();

        try {
            return proxies.getProxy(protocol, actorName);
        } finally {
            lock.readLock().unlock();
        }
    }

    private IProxy[] getSiteProxiesPrivate(String protocol)
    {
        lock.readLock().lock();

        try {
            return proxies.getSiteProxies(protocol);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Registers the specified actor by name and guid
     * @param actor actor
     * @throws Exception in case of error
     */
    private void registerActorPrivate(IActor actor) throws Exception
    {
        lock.writeLock().lock();

        try {
            String name = actor.getName();
            String guid = actor.getGuid().toString();
            

            if (actors.containsKey(name) || actorsByGuid.containsKey(guid))  {
                throw new Exception("An actor named " + name + " already exists");
            }

            ActorRegistryEntry entry = new ActorRegistryEntry(actor);
            actors.put(name, entry);
            actorsByGuid.put(guid, entry);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void registerCallbackPrivate(ICallbackProxy callback) throws Exception
    {
        lock.writeLock().lock();

        try {
            callbacks.registerCallback(callback);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void registerEndPointPrivate(String actorName, String protocol, String endPoint)
    {
        lock.writeLock().lock();

        try {
            ActorRegistryEntry entry = (ActorRegistryEntry) actors.get(actorName);

            if (entry == null) {
                throw new RuntimeException("Missing actor: " + actorName);
            }

            entry.registerEndPoint(protocol, endPoint);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void registerProxyPrivate(IProxy proxy) throws Exception
    {
        lock.writeLock().lock();

        try {
            proxies.registerProxy(proxy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void unregisterPrivate(IActor actor)
    {
        lock.writeLock().lock();

        try {
            String name = actor.getName();
            String guid = actor.getGuid().toString();
            actors.remove(name);
            actorsByGuid.remove(guid);
            callbacks.unregister(name);
            proxies.unregister(name);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
