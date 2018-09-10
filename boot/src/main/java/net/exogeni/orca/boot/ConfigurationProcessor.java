/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.boot;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import net.exogeni.orca.boot.beans.Policy;
import net.exogeni.orca.boot.beans.Actor;
import net.exogeni.orca.boot.beans.Attribute;
import net.exogeni.orca.boot.beans.Attributes;
import net.exogeni.orca.boot.beans.Configuration;
import net.exogeni.orca.boot.beans.Control;
import net.exogeni.orca.boot.beans.Edges;
import net.exogeni.orca.boot.beans.Handler;
import net.exogeni.orca.boot.beans.Pool;
import net.exogeni.orca.boot.beans.Pools;
import net.exogeni.orca.boot.beans.Rset;
import net.exogeni.orca.boot.beans.Vertex;
import net.exogeni.orca.boot.inventory.PoolCreator;
import net.exogeni.orca.extensions.PackageId;
import net.exogeni.orca.extensions.PluginId;
import net.exogeni.orca.manage.IOrcaClientActor;
import net.exogeni.orca.manage.IOrcaContainer;
import net.exogeni.orca.manage.IOrcaServerActor;
import net.exogeni.orca.manage.Orca;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.beans.ClientMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.policy.core.AuthorityCalendarPolicy;
import net.exogeni.orca.policy.core.BrokerSimplerUnitsPolicy;
import net.exogeni.orca.policy.core.IResourceControl;
import net.exogeni.orca.policy.core.ServiceManagerSimplePolicy;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.api.IDatabase;
import net.exogeni.orca.shirako.api.IPolicy;
import net.exogeni.orca.shirako.api.IShirakoPlugin;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.delegation.IResourceTicketFactory;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeType;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolsDescriptor;
import net.exogeni.orca.shirako.container.DistributedRemoteRegistryCache;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.OrcaContainer;
import net.exogeni.orca.shirako.container.RemoteRegistryCache;
import net.exogeni.orca.shirako.core.Authority;
import net.exogeni.orca.shirako.core.Broker;
import net.exogeni.orca.shirako.core.ServiceManager;
import net.exogeni.orca.shirako.kernel.SliceFactory;
import net.exogeni.orca.shirako.plugins.ShirakoPlugin;
import net.exogeni.orca.shirako.plugins.config.AntConfig;
import net.exogeni.orca.shirako.plugins.db.ServerActorDatabase;
import net.exogeni.orca.shirako.plugins.substrate.AuthoritySubstrate;
import net.exogeni.orca.shirako.plugins.substrate.Substrate;
import net.exogeni.orca.shirako.plugins.substrate.db.SubstrateActorDatabase;
import net.exogeni.orca.shirako.proxies.Proxy;
import net.exogeni.orca.shirako.proxies.local.LocalAuthority;
import net.exogeni.orca.shirako.proxies.local.LocalBroker;
import net.exogeni.orca.shirako.proxies.soapaxis2.SoapAxis2AuthorityProxy;
import net.exogeni.orca.shirako.proxies.soapaxis2.SoapAxis2BrokerProxy;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.shirako.util.Misc;
import net.exogeni.orca.tools.axis2.Axis2ClientSecurityConfigurator;
import net.exogeni.orca.util.Base64;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.PersistenceUtils;

import org.apache.log4j.Logger;

public class ConfigurationProcessor {
    protected class ActorPoolsState {
        public ResourcePoolsDescriptor descriptor;
        public String inventory;
    }

    /**
     * The logger.
     */
    protected Logger logger;
    /**
     * The configuration.
     */
    protected Configuration config;
    protected List<IActor> actors;
    protected List<ExportInfo> toExport;
    protected HashMap<ID, ActorPoolsState> pools;
    protected String defaultActorRegistry = "net.exogeni.orca.shirako.container.RemoteRegistryCache";
    protected String selectedActorRegistry = null;

    public ConfigurationProcessor(Configuration config) {
        this.config = config;
        this.logger = Globals.getLogger(this.getClass().getCanonicalName());
        this.toExport = new ArrayList<ExportInfo>();
        this.actors = new ArrayList<IActor>();
        this.pools = new HashMap<ID, ActorPoolsState>();
        selectedActorRegistry = defaultActorRegistry;
        if (Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryClass) != null) {
            selectedActorRegistry = Globals.getContainer().getConfiguration()
                    .getProperty(OrcaContainer.PropertyRegistryClass);
        }
    }

    /**
     * Instantiates the configuration
     * 
     * @throws ConfigurationException in case of error
     */
    public void process() throws ConfigurationException {
        try {
            /* create the actor objects */
            createActors();
            /* create actor security configuration */
            processSecurity();
            /* initialize the actors */
            initializeActors();
            logger.info(" +++++ There are " + net.exogeni.orca.shirako.core.Actor.actorCount + " actors");
            /* register all actors with the container. no ticking yet. */
            registerActors();
            /* create default slice */
            createDefaultSlice();
            /* populate inventory */
            populateInventory();
            /* recover actor state */
            recoverActors();
            /* enable ticking for the actors */
            enableTicking();
            /* link actors */
            processTopology();
            /* process any exports */
            logger.info(" +++++ Processing exports with actor count " + net.exogeni.orca.shirako.core.Actor.actorCount);
            processExports();
            /* claim exported resources */
            processClaims();
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Unexpected error while processing configuration", e);
        }

        logger.info("Finished instantiating actors.");
    }

    /**
     * Instantiates all actors
     * 
     * @throws ConfigurationException in case of error
     */
    protected void createActors() throws ConfigurationException {
        try {
            if (config.getActors() != null) {
                Iterator<?> iter = config.getActors().getActor().iterator();
                while (iter.hasNext()) {
                    createActor((Actor) iter.next());
                }
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Unexpected error while creating actors", e);
        }
    }

    protected void processSecurity() throws ConfigurationException {
        Axis2ClientSecurityConfigurator configurator = Axis2ClientSecurityConfigurator.getInstance();

        for (IActor actor : actors) {
            logger.debug("Creating security configuration for actor: " + actor.getName());
            if (configurator.createActorConfiguration(Globals.HomeDirectory, actor.getGuid().toString()) != 0) {
                throw new ConfigurationException(
                        "Cannot create actor security configuration: actorName=" + actor.getName());
            }
            logger.debug("Created security configuration for actor: " + actor.getName());
            logger.debug("Initializing actor keystore for: " + actor.getName());
            try {
                actor.initializeKeyStore();
            } catch (Exception e) {
                throw new ConfigurationException("Could not initialize keystore for actor: " + actor.getName());
            }
            logger.debug("Initializing actor keystore for: " + actor.getName() + " OK");
        }
    }

    /**
     * Performs final actor initialization
     * 
     * @throws ConfigurationException in case of error
     */
    protected void initializeActors() throws ConfigurationException {
        for (IActor actor : actors) {
            try {
                net.exogeni.orca.shirako.core.Actor.actorCount++;
                actor.initialize();
                actor.getIdentity()
                        .setCertificate((X509Certificate) actor.getShirakoPlugin().getKeyStore().getActorCertificate());
            } catch (Exception e) {
                throw new ConfigurationException("Actor failed to initialize: " + actor.getName(), e);
            }
        }
    }

    protected void registerActors() throws ConfigurationException {
        for (IActor actor : actors) {
            try {
                Globals.getContainer().registerActor(actor);
            } catch (Exception e) {
                throw new ConfigurationException("Could not register actor: " + actor.getName(), e);
            }
        }
    }

    /**
     * Creates a default slice per actor
     * 
     * @throws ConfigurationException in case of error
     */
    protected void createDefaultSlice() throws ConfigurationException {
        for (IActor actor : actors) {
            if (actor.getType() != OrcaConstants.ActorTypeSiteAuthority) {
                ISlice slice = SliceFactory.getInstance().create(actor.getName());
                slice.setInventory(true);
                try {
                    actor.registerSlice(slice);
                } catch (Exception e) {
                    throw new ConfigurationException("Could not create default slice for actor: " + actor.getName(), e);
                }
            }
        }
    }

    protected void populateInventory() throws Exception {
        for (IActor actor : actors) {
            if (actor instanceof IAuthority) {
                if (actor.getShirakoPlugin() instanceof AuthoritySubstrate) {
                    ActorPoolsState state = pools.get(actor.getGuid());
                    PoolCreator creator = new PoolCreator((AuthoritySubstrate) actor.getShirakoPlugin(),
                            state.descriptor, state.inventory);
                    creator.process();
                }
            }
        }
    }

    protected void recoverActors() throws ConfigurationException {
        for (IActor a : actors) {
            try {
                a.recover();
            } catch (Exception e) {
                throw new ConfigurationException("Recovery failed for actor: " + a.getName());
            }
        }
    }

    /**
     * Processes the topology section
     * 
     * @throws ConfigurationException in case of error
     */
    protected void processTopology() throws ConfigurationException {
        if (config.getTopology() != null) {
            if (Globals.Log.isDebugEnabled()) {
                Globals.Log.debug(
                        "Creating proxies for topology with actor count " + config.getActors().getActor().size());
            }
            createProxies(config.getTopology().getEdges());
        } else {
            Globals.Log.warn("Topology was null with actor count " + config.getActors().getActor().size());
        }
        // process edges that are not in config, but that are now known
        // from registry. query thread will not interfere as it hasn't
        // started yet
        if (Globals.Log.isDebugEnabled()) {
            Globals.Log.debug("Processing entries in registry cache after topology");
        }

        if (selectedActorRegistry.contains("Distributed")) {
            DistributedRemoteRegistryCache.getInstance().singleQueryProcess();
        } else {
            RemoteRegistryCache.getInstance().singleQueryProcess();
        }

        //
    }

    /**
     * Exports and claims resources
     * 
     * @throws ConfigurationException in case of error
     */
    protected void processExports() throws ConfigurationException {
        for (ExportInfo ei : toExport) {
            export(ei);
        }
    }

    protected void enableTicking() throws ConfigurationException {
        for (IActor actor : actors) {
            Globals.getContainer().register(actor);
        }
    }

    /**
     * Exports and claims resources
     * 
     * @throws ConfigurationException in case of error
     */
    protected void processClaims() throws ConfigurationException {
        for (ExportInfo ei : toExport) {
            claim(ei);
        }
    }

    /**
     * Creates an actor.
     * 
     * @param actor
     *            description
     * @throws ConfigurationException in case of error
     */
    protected void createActor(Actor actor) throws ConfigurationException {
        Globals.Log.info("Creating an actor: name=" + actor.getName());
        IActor oActor = doCommon(actor);
        doSpecific(oActor, actor);
        actors.add(oActor);
    }

    protected ID makeActorGuid(Actor actor) {
        // actor guid
        ID actorGuid = null;
        // make sure the actor has a guid
        if (actor.getGuid() != null) {
            actorGuid = new ID(actor.getGuid());
        } else {
            actorGuid = new ID();
        }
        return actorGuid;
    }

    protected IActor makeActorInstance(Actor actor) throws ConfigurationException {
        // if type is define and no instance is specified
        // create the instance based on the type.
        // Otherwise, create the instance, based on the instance definition.
        String actorType = actor.getType();
        IActor oActor = null;
        if (actorType != null) {
            if (actorType.equalsIgnoreCase(OrcaConstants.SM) || actorType.equalsIgnoreCase(OrcaConstants.SERVICE)) {
                oActor = new ServiceManager();
            } else if (actorType.equalsIgnoreCase(OrcaConstants.AGENT)
                    || actorType.equalsIgnoreCase(OrcaConstants.BROKER)) {
                oActor = new Broker();
            } else if (actorType.equalsIgnoreCase(OrcaConstants.SITE)
                    || actorType.equalsIgnoreCase(OrcaConstants.AUTHORITY)) {
                oActor = new Authority();
            } else {
                throw new ConfigurationException("Unsupported actor type: " + actorType);
            }
        }

        if (oActor == null) {
            // obtain the class of the actor
            try {
                oActor = (IActor) ConfigurationTools.createInstance(actor.getInstance());
            } catch (Exception e) {
                throw new ConfigurationException("Cannot instantiate actor: " + actor.getName(), e);
            }
        }
        // create the actor identity
        ID actorGuid = makeActorGuid(actor);
        AuthToken authToken = new AuthToken(actor.getName(), actorGuid);
        oActor.setIdentity(authToken);
        // set the description
        if (actor.getDescription() != null) {
            oActor.setDescription(actor.getDescription());
        }
        // set the clock
        oActor.setActorClock(Globals.getContainer().getActorClock());

        return oActor;
    }

    protected IShirakoPlugin makePluginInstance(IActor oActor, Actor actor) throws ConfigurationException {
        IShirakoPlugin shirakoPlugin = null;
        if (actor.getPlugin() == null) {
            if (oActor.getType() == OrcaConstants.ActorTypeSiteAuthority) {
                shirakoPlugin = new AuthoritySubstrate();
                shirakoPlugin.setConfig(new AntConfig());
            } else if (oActor.getType() == OrcaConstants.ActorTypeServiceManager) {
                shirakoPlugin = new Substrate();
                shirakoPlugin.setConfig(new AntConfig());
            } else if (oActor.getType() == OrcaConstants.ActorTypeBroker) {
                shirakoPlugin = new ShirakoPlugin();
            }
        }
        if (shirakoPlugin == null) {
            try {
                shirakoPlugin = (IShirakoPlugin) ConfigurationTools.createInstance(actor.getPlugin());
            } catch (Exception e) {
                throw new ConfigurationException("Cannot instantiate shirako plugin for actor: " + actor.getName(), e);
            }
        }

        // if the plugin does not have a database
        // make and attach one based on type.

        if (shirakoPlugin.getDatabase() == null) {
            IDatabase db = null;
            try {
                // FIXME: somewhat of a hack, but the database settings
                // are not accessible through Globals.getConfiguration()
                Properties p = PersistenceUtils.save(Globals.getContainer().getDatabase());
                if (shirakoPlugin instanceof Substrate) {
                    db = new SubstrateActorDatabase();
                } else {
                    db = new ServerActorDatabase();
                }
                PersistenceUtils.restore(db, p);
            } catch (Exception e) {
                throw new ConfigurationException("Could not create database for actor: " + actor.getName(), e);
            }
            shirakoPlugin.setDatabase(db);
        }

        // attach the ticket factory
        IResourceTicketFactory ticketFactory = getTicketFactory(oActor);
        shirakoPlugin.setTicketFactory(ticketFactory);

        return shirakoPlugin;
    }

    protected IPolicy makeSitePolicy(IActor oActor, Actor actor) throws ConfigurationException {
        if (actor.getControls() == null || actor.getControls().getControl() == null
                || actor.getControls().getControl().size() == 0) {
            throw new ConfigurationException("Missing authority policy but no control has been specified");
        }
        IPolicy policy = new AuthorityCalendarPolicy();
        List<Control> list = actor.getControls().getControl();
        for (Control c : list) {
            try {
                // get the control name
                String cname = c.getClazz();
                if (cname == null) {
                    throw new ConfigurationException("Missing control class name");
                }
                // make the control
                IResourceControl control = (IResourceControl) Misc.createInstance(cname);
                // pass any properties if needed
                Properties cprops = ConfigurationTools.getProperties(c.getProperties());
                if (cprops.size() > 0) {
                    ConfigurationTools.attachConfigurationProperties(control, cprops);
                }

                // map the control types to it
                if (c.getTypes() == null || c.getTypes().getType() == null || c.getTypes().getType().size() == 0) {
                    if (c.getType() == null) {
                        throw new ConfigurationException("No type specified for control");
                    }
                    control.addType(new ResourceType(c.getType()));
                } else {
                    for (String t : c.getTypes().getType()) {
                        control.addType(new ResourceType(t));
                    }
                }

                // register the control with the policy
                ((AuthorityCalendarPolicy) policy).registerControl(control);
            } catch (ConfigurationException e) {
                throw e;
            } catch (Exception e) {
                throw new ConfigurationException("Could not create control", e);
            }
        }
        return policy;
    }

    protected IPolicy makePolicy(Policy policy) throws ConfigurationException {
        IPolicy result = null;
        String className = policy.getClazz();
        if (className == null) {
            throw new ConfigurationException("Policy is missing class name");
        }
        // attempt to instantiate
        try {
            result = (IPolicy) Misc.createInstance(className);
        } catch (Exception e) {
            throw new ConfigurationException("Could not instantiate policy class: " + className, e);
        }
        // attempt to configure
        try {
            Properties p = ConfigurationTools.getProperties(policy.getProperties());
            if (p.size() > 0) {
                ConfigurationTools.attachConfigurationProperties(result, p);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Could not configure policy", e);
        }
        return result;
    }

    protected IPolicy makeActorPolicy(IActor oActor, Actor actor) throws ConfigurationException {
        IPolicy policy = null;
        // We first try the old-style mapper section. If this section
        // is not specified, we try the new policy section. If the new section
        // is also not specified, we create a policy based on the actor type.
        if (actor.getMapper() == null) {
            if (actor.getPolicy() == null) {
                if (oActor.getType() == OrcaConstants.ActorTypeSiteAuthority) {
                    policy = makeSitePolicy(oActor, actor);
                } else if (oActor.getType() == OrcaConstants.ActorTypeBroker) {
                    policy = new BrokerSimplerUnitsPolicy();
                } else if (oActor.getType() == OrcaConstants.ActorTypeServiceManager) {
                    policy = new ServiceManagerSimplePolicy();
                }
            } else {
                policy = makePolicy(actor.getPolicy());
            }
        }

        if (policy == null) {
            try {
                policy = (IPolicy) ConfigurationTools.createInstance(actor.getMapper());
            } catch (Exception e) {
                throw new ConfigurationException("Could not instantiate policy for actor: " + actor.getName(), e);
            }
        }
        return policy;
    }

    /**
     * Performs actor-independent setup.
     * 
     * @param actor actor
     * @return actor
     * @throws ConfigurationException in case of error
     */
    protected IActor doCommon(Actor actor) throws ConfigurationException {
        // make the actor instance
        IActor oActor = makeActorInstance(actor);
        oActor.setShirakoPlugin(makePluginInstance(oActor, actor));
        oActor.setPolicy(makeActorPolicy(oActor, actor));
        return oActor;
    }

    /**
     * Performs actor-dependent initialization
     * 
     * @param oActor oActor
     * @param actor actor
     * @throws ConfigurationException in case of error
     */
    public void doSpecific(IActor oActor, Actor actor) throws ConfigurationException {
        if (oActor instanceof IAuthority) {
            // read and store the resource pools information
            // so that we can process it in populateInventory
            ResourcePoolsDescriptor rd = readResourcePools(actor);
            ActorPoolsState state = new ActorPoolsState();
            state.descriptor = rd;
            state.inventory = actor.getInventory();
            pools.put(oActor.getGuid(), state);
        }
    }

    protected ResourcePoolsDescriptor readResourcePools(Actor actor) throws ConfigurationException {
        ResourcePoolsDescriptor result = new ResourcePoolsDescriptor();
        Pools p = actor.getPools();
        if (p == null) {
            return result;
        }
        List<Pool> list = p.getPool();
        if (list == null) {
            return result;
        }

        for (Pool pool : list) {
            ResourcePoolDescriptor d = new ResourcePoolDescriptor();
            d.setResourceType(new ResourceType(pool.getType()));
            d.setResourceTypeLabel(pool.getLabel());
            d.setUnits(pool.getUnits());
            d.setStart(pool.getStart().toGregorianCalendar().getTime());
            d.setEnd(pool.getEnd().toGregorianCalendar().getTime());
            d.setPoolFactory(pool.getFactory());
            Handler h = pool.getHandler();
            d.setHandlerPath(h.getPath());
            if (h.getPackageId() != null) {
                d.setHandlerPackageId(new PackageId(h.getPackageId()));
            }
            if (h.getPluginId() != null) {
                d.setHandlerPluginId(new PluginId(h.getPluginId()));
            }
            Properties handlerProperties = ConfigurationTools.getProperties(h.getProperties());
            PropList.mergeProperties(handlerProperties, d.getHandlerProperties());
            // get the pool properties
            Properties poolProperties = ConfigurationTools.getProperties(pool.getProperties());
            PropList.mergeProperties(poolProperties, d.getPoolProperties());
            d.setInventory(pool.getInventory());
            Attributes as = pool.getAttributes();
            if (as != null) {
                for (Attribute a : as.getAttribute()) {
                    ResourcePoolAttributeDescriptor att = new ResourcePoolAttributeDescriptor();
                    att.setKey(a.getKey());
                    att.setLabel(a.getLabel());
                    if (a.getMax() != null) {
                        att.setMax(a.getMax().longValue());
                    }
                    if (a.getMin() != null) {
                        att.setMin(a.getMin().longValue());
                    }
                    String type = a.getType();
                    if (type.equalsIgnoreCase("integer")) {
                        att.setType(ResourcePoolAttributeType.INTEGER);
                    } else if (type.equalsIgnoreCase("string")) {
                        att.setType(ResourcePoolAttributeType.STRING);
                    } else if (type.equalsIgnoreCase("ndl")) {
                        att.setType(ResourcePoolAttributeType.NDL);
                    } else if (type.equalsIgnoreCase("class")) {
                        att.setType(ResourcePoolAttributeType.CLASS);
                    } else {
                        throw new ConfigurationException("Unsupported attribute type: " + type);
                    }
                    att.setValue(a.getValue());
                    att.setUnit(a.getUnit());
                    d.addAttribute(att);
                }
            }
            result.add(d);
        }
        return result;
    }

    protected IResourceTicketFactory getTicketFactory(IActor actor) throws ConfigurationException {
        String ticketFactoryClassName = Globals.getConfiguration().getTicketFactoryClassName();
        if (ticketFactoryClassName == null) {
            throw new ConfigurationException("Missing ticket factory class name");
        }
        try {
            IResourceTicketFactory factory = (IResourceTicketFactory) Misc.createInstance(ticketFactoryClassName);
            factory.setActor(actor);
            return factory;
        } catch (Exception e) {
            throw new ConfigurationException("Could not create ticket factory", e);
        }
    }

    /**
     * Creates all proxies.
     * 
     * @param edges edges
     * @throws ConfigurationException in case of error
     */
    protected void createProxies(Edges edges) throws ConfigurationException {
        if (edges != null) {
            for (Edges.Edge e : edges.getEdge()) {
                processEdge(e);
            }
        }
    }

    /**
     * Form partial cache entry based on vertex information in config
     * 
     * @param actor actor
     * @param v v
     * @return guid of the actor
     * @throws ConfigurationException in case of error
     */
    protected String vertexToRegistryCache(IActor actor, Vertex v) throws ConfigurationException {
        HashMap<String, String> res = new HashMap<String, String>();

        Globals.Log.debug("Adding vertex for " + v.getName());
        if (v.getName() == null)
            throw new ConfigurationException("Actor must specify a name");

        res.put(RemoteRegistryCache.ActorName, v.getName());
        if (v.getGuid() != null) {
            res.put(RemoteRegistryCache.ActorGuid, v.getGuid());
        } else {
            // consult the actor for guid
            if (actor.getGuid() == null) {
                throw new ConfigurationException("Cannot find any GUID for actor " + actor.getName());
            }
            Globals.Log.debug("Using guid " + actor.getGuid().toString());
            res.put(RemoteRegistryCache.ActorGuid, actor.getGuid().toString());
        }
        if (v.getLocation() != null) {
            res.put(RemoteRegistryCache.ActorLocation, v.getLocation().getUrl());
            res.put(RemoteRegistryCache.ActorProtocol, v.getLocation().getProtocol());
        } else {
            res.put(RemoteRegistryCache.ActorProtocol, OrcaConstants.ProtocolLocal);
        }
        if (v.getCertificate() != null) {
            res.put(RemoteRegistryCache.ActorCert64, Base64.encodeBytes(v.getCertificate()));
        } // do nothing for local actors - done in registerActor()
        res.put(RemoteRegistryCache.ActorType, v.getType().toLowerCase());

        // add to the cache (local entries will exist and will be merged)
        RemoteRegistryCache.getInstance().addPartialCacheEntry(res.get(RemoteRegistryCache.ActorGuid), res);

        return res.get(RemoteRegistryCache.ActorGuid);
    }

    /**
     * Processes an edge.
     * 
     * @param edge edge
     * @throws ConfigurationException in case of error
     */
    protected void processEdge(Edges.Edge edge) throws ConfigurationException {
        Vertex from = edge.getFrom();
        Vertex to = edge.getTo();

        /*
         * We only like edges broker->site and service->broker
         */

        /*
         * Reverse the edge if it connects site->broker or broker->service
         */
        if ((from.getType().equalsIgnoreCase(OrcaConstants.AUTHORITY)
                || from.getType().equalsIgnoreCase(OrcaConstants.SITE))
                && (to.getType().equalsIgnoreCase(OrcaConstants.BROKER)
                        || to.getType().equalsIgnoreCase(OrcaConstants.AGENT))) {
            Vertex tmp = from;
            from = to;
            to = tmp;
            // the edge may be used for other things, so we'll reverse it as well
            edge.setTo(to);
            edge.setFrom(from);
        }

        if ((from.getType().equalsIgnoreCase(OrcaConstants.BROKER)
                || from.getType().equalsIgnoreCase(OrcaConstants.AGENT))
                && (to.getType().equalsIgnoreCase(OrcaConstants.SERVICE)
                        || to.getType().equalsIgnoreCase(OrcaConstants.SM))) {
            Vertex tmp = from;
            from = to;
            to = tmp;
            // the edge may be used for other things, so we'll reverse it as well
            edge.setTo(to);
            edge.setFrom(from);
        }

        /*
         * Check if this is a valid edge (error if authority -> * or sm -> authority).
         */
        if (from.getType().equalsIgnoreCase(OrcaConstants.AUTHORITY)
                || from.getType().equalsIgnoreCase(OrcaConstants.SITE)) {
            throw new ConfigurationException("Invalid edge type: an edge cannot start at an authority");
        }

        // edges between actors of same type aren't allowed unless the actors are both brokers
        if (from.getType().equalsIgnoreCase(to.getType()) && from.getType().equalsIgnoreCase(OrcaConstants.BROKER)) {
            throw new ConfigurationException("Invalid edge between actors of same type");
        }

        // edges SM->site or vice versa are not allowed
        if ((from.getType().equalsIgnoreCase(OrcaConstants.SERVICE)
                || from.getType().equalsIgnoreCase(OrcaConstants.SM))
                && (to.getType().equalsIgnoreCase(OrcaConstants.AUTHORITY)
                        || to.getType().equalsIgnoreCase(OrcaConstants.SITE)))
            throw new ConfigurationException("Edges between SMs and sites are not allowed");

        if ((to.getType().equalsIgnoreCase(OrcaConstants.SERVICE) || to.getType().equalsIgnoreCase(OrcaConstants.SM))
                && (from.getType().equalsIgnoreCase(OrcaConstants.AUTHORITY)
                        || from.getType().equalsIgnoreCase(OrcaConstants.SITE)))
            throw new ConfigurationException("Edges between SMs and sites are not allowed");

        IOrcaContainer cont = Orca.connect();
        IActor fromActor = ActorRegistry.getActor(from.getName());
        IActor toActor = ActorRegistry.getActor(to.getName());

        /* convert beans to maps for further processing */
        String fromGuid = vertexToRegistryCache(fromActor, from);
        String toGuid = vertexToRegistryCache(toActor, to);

        try {
            if (fromGuid == null || toGuid == null) {
                throw new RuntimeException("Both fromGuid and toGuid must be defined");
            }
            // establish new edge in topology and register a client if necessary
            ClientMng client = null;
            if (selectedActorRegistry.contains("Distributed")) {
                client = DistributedRemoteRegistryCache.getInstance().establishEdge(new ID(fromGuid), new ID(toGuid));
            } else {
                client = RemoteRegistryCache.getInstance().establishEdge(new ID(fromGuid), new ID(toGuid));
            }

            // parse all explicit exports of that edge. the client will not be returned if there is a cert problem
            if (client != null) {
                parseExports(edge, client, (IOrcaServerActor) cont.getActor(new ID(toGuid)));
            }
        } catch (Exception e) {
            throw new ConfigurationException("Could not process exports from: " + toActor.getName() + " to:"
                    + fromActor.getName() + ": " + e.toString());
        }
    }

    // CLAIM/EXPORT

    /**
     * Exports resources
     * 
     * @param info info
     * @throws ConfigurationException in case of error
     */
    protected void export(ExportInfo info) throws ConfigurationException {
        logger.info("Exporting resources from " + info.exporter.getName() + " to " + info.client.getName());

        // get start from the config
        Date start = info.start;
        if (start == null) {
            // no start in config: use now as the start
            long now = Globals.getContainer().getCurrentCycle();
            start = Globals.getContainer().getActorClock().cycleStartDate(now);
        }

        Date end = info.end;
        if (end == null) {
            // no end in config: export for a year from now
            // export for one year
            long length = 1000 * 60 * 60 * 24 * 365;
            end = new Date(start.getTime() + length);
        }

        // export
        if (logger.isTraceEnabled()) {
            logger.trace("Using Server Actor " + info.exporter.getClass().getSimpleName() + " to exportResources");
        }
        info.exported = info.exporter.exportResources(info.rtype, start, end, info.units, null, null, null,
                new AuthToken(info.client.getName(), new ID(info.client.getGuid())));
        if (info.exported == null) {
            throw new ConfigurationException("Could not export resources from actor: " + info.exporter.getName()
                    + " to actor: " + info.client.getName(), info.exporter.getLastError());
        }
    }

    /**
     * Claims resources
     * 
     * @param info info
     * @throws ConfigurationException in case of error
     */
    protected void claim(ExportInfo info) throws ConfigurationException {
        // FIXME: use guid
        IOrcaContainer cont = Orca.connect();
        IOrcaClientActor client = (IOrcaClientActor) cont.getActor(new ID(info.client.getGuid()));
        if (client == null) {
            logger.info(info.client.getName() + " is a remote client. Not performing claim");
            return;
        }

        logger.info("Claiming resources from " + info.exporter.getName() + " to " + info.client.getName());

        ReservationMng r = client.claimResources(info.exporter.getGuid(), info.exported);
        if (r != null) {
            logger.info("Successfully initiated claim for resources from " + info.exporter.getName() + " to "
                    + info.client.getName());
        } else {
            logger.error("Could not initiate claim for resources from " + info.exporter.getName() + " to "
                    + info.client.getName() + ": " + client.getLastError());
        }
    }

    /**
     * Registers the required certificates so that from and to can communicate. This function appears to be deprecated
     * /ib, cert registration is now done in OrcaContainer.establishEdge()
     * 
     * @param from from
     * @param to to
     * @throws ConfigurationException in case of error
     */
    protected void registerCertificates(Vertex from, Vertex to) throws ConfigurationException {
        IActor fromActor = ActorRegistry.getActor(from.getName());
        assert fromActor != null;
        IActor destActor = ActorRegistry.getActor(to.getName());

        // the certificate of the local user
        Certificate fromCertificate = fromActor.getShirakoPlugin().getKeyStore().getActorCertificate();
        Certificate toCertificate = null;

        if (destActor == null) {
            // this is a remote actor
            if (to.getCertificate() == null) {
                throw new ConfigurationException("Missing ceritificate for actor: " + to.getName());
            }
            try {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream is = new ByteArrayInputStream(to.getCertificate());
                toCertificate = factory.generateCertificate(is);
                is.close();
            } catch (Exception e) {
                throw new ConfigurationException("Could not parse certificate of remote actor: " + to.getName());
            }
        } else {
            toCertificate = destActor.getShirakoPlugin().getKeyStore().getActorCertificate();
        }

        // register the certificate of the destination with the from actor
        if (toCertificate != null) {
            fromActor.getShirakoPlugin().getKeyStore().addTrustedCertificate(to.getName(), toCertificate);
        }

        // register the certificate for the from actor with the destination
        // actor
        if (fromCertificate != null) {
            if (destActor != null) {
                // destActor.getShirakoPlugin().getKeyStore().addTrustedCertificate(from.getName(),
                // fromCertificate);
            } else {
                // this a remote actor
                // invoke the management API
            }
        }
    }

    private void parseExports(Edges.Edge edge, ClientMng client, IOrcaServerActor toActor) throws Exception {
        if (toActor == null) {
            throw new Exception("cannot process export for remote actor: " + edge.getTo().getName());
        }

        logger.debug("Parsing configured exports between " + client.getName() + " and " + toActor.getName());
        // mark resources to be exported if present
        List<?> list = edge.getRset();

        if (list != null) {
            Iterator<?> i = list.iterator();

            while (i.hasNext()) {
                Rset rset = (Rset) i.next();

                if (rset != null) {
                    ExportInfo info = new ExportInfo(toActor, client, rset.getUnits(),
                            new ResourceType(rset.getType()));
                    if (rset.getStart() != null) {
                        info.start = rset.getStart().toGregorianCalendar().getTime();
                    }
                    if (rset.getEnd() != null) {
                        info.end = rset.getEnd().toGregorianCalendar().getTime();
                    }

                    Properties ep = ConfigurationTools.getProperties(rset.getProperties());
                    PropList.mergeProperties(ep, info.properties);
                    toExport.add(info);
                }
            }
        }
    }

    // protected IProxy getProxy(String protocol, IActorIdentity identity, ActorLocation location, String type) throws
    // ConfigurationException {
    // try {
    // IProxy proxy = ActorRegistry.getProxy(protocol, identity.getName());
    //
    // if (proxy == null) {
    // proxy = ProxyFactory.newProxy(protocol, identity, location, type);
    // ActorRegistry.registerProxy(proxy);
    // }
    // return proxy;
    // } catch (Exception e) {
    // throw new ConfigurationException("Could not obtain proxy for actor: " + identity.getName() + " protocol: " +
    // protocol, e);
    // }
    // }

    /**
     * Converts an authority proxy to an agent proxy. Used during initial setup for export and claim.
     * 
     * @param authorityProxy authorityProxy
     * @return agent proxy
     * @throws ConfigurationException in case of error
     */
    public static Proxy getAgentProxy(Proxy authorityProxy) throws ConfigurationException {
        if (authorityProxy instanceof LocalAuthority) {
            return (LocalBroker) authorityProxy;
        } else if (authorityProxy instanceof SoapAxis2AuthorityProxy) {
            return (SoapAxis2BrokerProxy) authorityProxy;
        } else {
            throw new ConfigurationException("Unsupported proxy type: " + authorityProxy.getClass().getCanonicalName());
        }
    }

    protected class ExportInfo {
        public IOrcaServerActor exporter;
        public ClientMng client;
        public ReservationID exported;
        public Date start;
        public Date end;
        public Properties properties = new Properties();
        public int units;
        public ResourceType rtype;

        public ExportInfo(IOrcaServerActor exporter, ClientMng client, int units, ResourceType rtype) {
            this.exporter = exporter;
            this.client = client;
            this.units = units;
            this.rtype = rtype;
        }
    }

}
