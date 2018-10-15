/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.proxies;

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IConcreteSet;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.api.IShirakoPlugin;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.shirako.util.Misc;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.PersistenceUtils;
import net.exogeni.orca.util.persistence.Persistent;
import net.exogeni.orca.util.persistence.Restore;

import org.apache.log4j.Logger;

/**
 * The <code>Proxy</code> class represents a stub to a shirako actor. In shirako
 * all inter-actor communication happens with the help of proxies. Proxies
 * define a general interface, which is implementation independent and enables
 * easy implementation of new communication protocols.
 */
@Restore(ProxyRestorer.class)
public abstract class Proxy implements IProxy {
    public static final String PropertyProxyType = "ProxyType";
    public static final String PropertyProxyActorAuth = "ProxyActorAuth";
    public static final String PropertyProxyActorName = "ProxyActorName";
    public static final String PropertyProxyActorGuid = "ProxyActorGuid";
    public static final String PropertyProxyCallback = "ProxyCallback";

    /**
     * Obtains a callback for the specified actor and protocol
     * @param actor actor
     * @param protocol protocol
     * @return ICallbackProxy
     */
    public static ICallbackProxy getCallback(IActor actor, String protocol) {
        if (actor == null) {
            throw new IllegalArgumentException("actor cannot be null");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("protocol cannot be null");
        }
        ICallbackProxy callback = null;
        try {
            callback = ActorRegistry.getCallback(protocol, actor.getName());
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain callback proxy: protocol=" + protocol, e);
        }
        if (callback == null){
            throw new RuntimeException("Could not obtain callback proxy: protocol=" + protocol);
        }
        return callback;
    }

    /**
     * Obtains a proxy object from the specified properties list. If a suitable
     * proxy object has already been created and registered with the
     * <code>ActorRegistry</code>, the already existing object is returned and
     * no new object is created. Otherwise, the method creates the proxy object
     * and registers it with the <code>ActorRegistry</code>
     * @param p Properties list representing the proxy
     * @return IProxy
     * @throws Exception in case of error
     */
    public static IProxy getProxy(Properties p) throws Exception {
        String type = PropList.getRequiredProperty(p, PropertyProxyType);
        String name = PropList.getRequiredProperty(p, PropertyProxyActorName);
        boolean isCallback = PropList.getRequiredBooleanProperty(p, PropertyProxyCallback);

        IProxy proxy = null;

        if (isCallback) {
            proxy = (Proxy) ActorRegistry.getCallback(type, name);
        } else {
            proxy = ActorRegistry.getProxy(type, name);
        }

        if (proxy == null) {
            proxy = recoverProxy(p, true);
        } else {
            /*
             * in the ActorRegistry we do not register agent proxies to
             * authorities. however, the PeerRegistry for a broker may contain
             * an agent proxy to an authority. Here we check for this case to
             * make sure we return the correct proxy
             */
            String className = proxy.getClass().getName();
            String expectedClassName = PropList.getRequiredProperty(p, PersistenceUtils.PropertyClassName);

            if (!className.equals(expectedClassName)) {
                proxy = recoverProxy(p, false);
            }
        }

        return proxy;
    }

    /**
     * Creates a proxy list from a properties list representing the
     * serialization of the proxy. Optionally, the resulting object may be
     * registered with the <code>ActorRegistry</code> so that it becomes visible
     * to the rest of the system.
     * @param p Properties list representing the proxy
     * @param register If true, the resulting proxy is registered with the
     *            container's <code>ActorRegistry</code>
     * @return Proxy
     * @throws Exception in case of error
     */
    private static Proxy recoverProxy(Properties p, boolean register) throws Exception {
    	Proxy proxy = PersistenceUtils.restore(p, true);

    	String name = PropList.getRequiredProperty(p, PropertyProxyActorName);
        if (name == null) {
            name = "unknown-actor";
        }
        proxy.setLogger(Globals.getLogger(name));
        
        if (register) {
            if (proxy.callback) {
                ActorRegistry.registerCallback((ICallbackProxy) proxy);
            } else {
                ActorRegistry.registerProxy(proxy);
            }
        }

        return proxy;
    }

    public static IConcreteSet decode(Properties enc, IShirakoPlugin plugin) throws Exception {
        IConcreteSet cs = null;
        String name = enc.getProperty(PersistenceUtils.PropertyClassName);
        if (name == null) {
            throw new RuntimeException("cannot decode: missing conreteset class name");
        }

        Globals.Log.debug("decoding concrete set: class=" + name);

        try {
            cs = (IConcreteSet) Misc.createInstance(name);
        } catch (Exception e) {
            Globals.Log.error("Cannot instantiate concrete set", e);
            throw e;
        }

        cs.decode(enc, plugin);

        return cs;
    }

    /**
     * Type of the proxy: local, soap, xmlrpc, etc...
     */
    @Persistent(key = PropertyProxyType)
    protected String proxyType;

    /**
     * True if this proxy is used as a callback, false otherwise.
     */
    @Persistent(key = PropertyProxyCallback)
    protected boolean callback;

    @Persistent(key = PropertyProxyActorName)
    protected String actorName;
    
    @Persistent(key = PropertyProxyActorGuid)
    protected ID actorGuid;
    
    @Persistent(key = PropertyProxyActorAuth)
    protected AuthToken auth;
    /**
     * The logger
     */
    @NotPersistent
    protected Logger logger;

    public Proxy() {
    }

    public Proxy(AuthToken auth) {
    	this.auth = auth;
    	this.actorName = auth.getName();
    	this.actorGuid = auth.getGuid();
    }
    /**
     * Clones the resource set, but without any of the concrete sets. Preserves
     * only the configuration properties. This method should be used when
     * sending a redeem/extend/close request to an authority.
     * @param set resource set
     * @return a resources set that is a copy of the current but without any
     *         concrete sets.
     */
    protected ResourceSet abstractCloneAuthority(ResourceSet set) {
        // Create the resource data carrying only configuration properties
        // Be defensive and check for nulls
        ResourceData newResourceData = new ResourceData();
        Properties p = set.getConfigurationProperties();

        if (p == null) {
            p = new Properties();
        } else {
            p = (Properties) p.clone();
        }

        ResourceData.mergeProperties(p, newResourceData.getConfigurationProperties());

        return new ResourceSet(set.getUnits(), set.getType(), newResourceData);
    }

    /*
     * ========================================================================
     * Cloning resource sets
     * ========================================================================
     */

    /**
     * Clones the resource set, but without any of the concrete sets. Preserves
     * only the request properties. This method should be used when sending a
     * request to a broker.
     * @param set resource set
     * @return a resources set that is a copy of the current but without any
     *         concrete sets.
     */
    protected ResourceSet abstractCloneBroker(ResourceSet set) {
        // Create the resource data carrying only request properties
        // Be defensive and check for nulls
        ResourceData newResourceData = new ResourceData();
        Properties p = set.getRequestProperties();

        if (p == null) {
            p = new Properties();
        } else {
            p = (Properties) p.clone();
        }

        ResourceData.mergeProperties(p, newResourceData.getRequestProperties());

        return new ResourceSet(set.getUnits(), set.getType(), newResourceData);
    }

    /**
     * Clones the resource set, but without any of the concrete sets. Preserves
     * only the resource properties. This method should be used when sending an
     * update ticket/update lease.
     * @param set resource set
     * @return a resources set that is a copy of the current but without any
     *         concrete sets.
     */
    public static ResourceSet abstractCloneReturn(ResourceSet set) {
        // Create the resource data carrying only resource properties
        // Be defensive and check for nulls
        ResourceData newResourceData = new ResourceData();
        Properties p = set.getResourceProperties();

        if (p == null) {
            p = new Properties();
        } else {
            p = (Properties) p.clone();
        }

        ResourceData.mergeProperties(p, newResourceData.getResourceProperties());
        return new ResourceSet(set.getUnits(), set.getType(), newResourceData);
    }

    public ID getGuid() {
        return actorGuid;
    }

    public AuthToken getIdentity() {
        return auth;
    }

    /*
     * ========================================================================
     * Persistence
     * ========================================================================
     * @return returns actor name
     */
    public String getName() {
        return actorName;
    }

    /**
     * Returns the type of this proxy
     * @return proxy type
     */
    public String getType() {
        return proxyType;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * {@inheritDoc}
     * @return Logger
     */
    public Logger getLogger() {
        return logger;
    }
}
