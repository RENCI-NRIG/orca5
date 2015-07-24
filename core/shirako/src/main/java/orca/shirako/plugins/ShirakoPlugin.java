/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.plugins;

import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorEvent;
import orca.shirako.api.IDatabase;
import orca.shirako.api.IReservation;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.common.delegation.IResourceTicketFactory;
import orca.shirako.container.Globals;
import orca.shirako.core.Actor;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.plugins.config.Config;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.util.Misc;
import orca.shirako.util.ResourceData;
import orca.tools.axis2.Axis2ClientSecurityConfigurator;
import orca.util.KeystoreManager;
import orca.util.OrcaException;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

/**
 * The base implementation for actor-specific extensions.
 * 
 * @author aydan
 */
public class ShirakoPlugin implements IShirakoPlugin {
    public static final String PropertyConfig = "PluginConfig";
    public static final String PropertyConfigClass = "PluginConfigClass";
    public static final String PropertyDatabase = "PluginDatabase";
    public static final String PropertyDatabaseClass = "PluginDatabaseClass";

    /**
     * The configuration provider.
     */
    @Persistent(key = PropertyConfig)
    protected Config config;

    /**
     * The database provider
     */
    @Persistent(key = PropertyDatabase)
    protected IDatabase db;

    /**
     * Configuration properties passed by the configuration engine.
     */
    @Persistent
    protected Properties configurationProperties;

    /**
     * The actor the plugin is associated with
     */
    @Persistent(reference = true)
    protected IActor actor;

    /**
     * The logger
     */
    @Persistent(reference = true)
    protected Logger logger;

    /**
     * The keystore manager.
     */
    @NotPersistent
    protected KeystoreManager keystoreManager;

    /**
     * True if this plugin was configured from a config file.
     */
    @NotPersistent
    protected boolean fromConfig = false;

    /**
     * The resource ticket factory.
     */
    @NotPersistent
    protected IResourceTicketFactory ticketFactory;

    /**
     * Initialization status
     */
    @NotPersistent
    private boolean initialized = false;

    public ShirakoPlugin() {
    }

    /**
     * Create a new <code>ShirakoPlugin</code>
     * 
     * @param actor
     *            The actor this plugin is related to
     * @param db
     *            Database to use
     * @param config
     *            Configuration manager to use
     */
    public ShirakoPlugin(Actor actor, IDatabase db, Config config) {
        this.actor = actor;
        this.db = db;
        this.config = config;
    }

    /**
     * Initialization entry point
     */
    public void initialize() throws OrcaException {
        if (!initialized) {
            try {
                if (actor == null) {
                    throw new OrcaException("Missing actor");
                }
                if (ticketFactory == null) {
                    ticketFactory = makeTicketFactory();
                }
                if (db != null) {
                    db.setLogger(logger);
                    db.setActorName(actor.getName());
                    // prevent the db from resetting if we are recovering !!!
                    db.setResetState(Globals.getContainer().isFresh());
                    db.initialize();
                }

                // Note: config is initialized in actorAdded()
                initializeKeyStore(actor);
                ticketFactory.initialize();
                initialized = true;
            } catch (OrcaException e) {
                throw e;
            } catch (Exception e) {
                throw new OrcaException("Cannot initialize", e);
            }
        }
    }

    public void configure(Properties p) throws Exception {
        configurationProperties = p;
        fromConfig = true;
    }

    public void actorAdded() throws Exception {
        if (db != null) {
            db.actorAdded();
        }

        if (config != null) {
            config.setSlicesPlugin(this);
            config.initialize();
        }
    }

    public void initializeKeyStore(IActor actor) throws Exception {
        if (keystoreManager == null) {
            // do we really need this dependency?
            String path = Axis2ClientSecurityConfigurator.getInstance()
                    .getKeyStorePath(Globals.HomeDirectory, actor.getGuid().toString());

            // FIXME: where should this one come from
            // FIXME: hardcoded
            String password = "clientkeystorepass";
            String keyPassword = "clientkeypass";
            keystoreManager = new KeystoreManager(path, password, keyPassword);
            keystoreManager.initialize();
        }
    }

    public void recoveryStarting() {
        // noop
    }
    
    public void restartConfigurationActions(IReservation r) throws Exception {
    }

    public void revisit(IReservation r) throws OrcaException {
    }

    public void revisit(ISlice s) throws OrcaException {
    }

    public void recoveryEnded() {
        // noop
    }
    
    public ISlice createSlice(SliceID id, String name, ResourceData properties, Object other)
            throws Exception {
        return SliceFactory.getInstance().create(id, name, properties);
    }

    public void releaseSlice(ISlice slice) throws Exception {
    }

    public boolean validateIncoming(IReservation reservation, AuthToken auth) {
        return true;
    }

    /**
     * Configuration callback function. Called when a configuration action
     * completes.
     * 
     * @param token
     *            Unique opaque token to identify the completed operation
     * @param properties
     *            Output properties list produced by the configuration action.
     */
    public void configurationComplete(ConfigToken token, Properties properties) {
        // this callback can happen on a thread other than the actor main thread
        // create an event and enqueue it for the actor to be processed on its
        // main thread
        actor.queueEvent(new ConfigurationCompleteEvent(token, properties));
    }

    protected void processConfigurationComplete(ConfigToken token, Properties properties) {
        String target = properties.getProperty(Config.PropertyTargetName);
        assert target != null;
        boolean unsupported = false;

        if (target.equals(Config.TargetJoin)) {
            processJoinComplete(token, properties);
        } else if (target.equals(Config.TargetLeave)) {
            processLeaveComplete(token, properties);
        } else if (target.startsWith(Config.TargetModify)) { // changed from target.equals to target.startsWith because modify targets will have subcommands (e.g. modify.restart)
            processModifyComplete(token, properties);
        } else {
            // FIXME: should be an exception?
            unsupported = true;
            logger.warn("Unsupported target in configurationComplete(): " + target);
        }

        if (!unsupported) {
            actor.getPolicy().configurationComplete(target, token, properties);
        }
    }

    public IActor getActor() {
        return actor;
    }

    public Config getConfig() {
        return config;
    }

    public IDatabase getDatabase() {
        return db;
    }

    public KeystoreManager getKeyStore() {
        return keystoreManager;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the status code contained in the properties list. To be use in
     * configuration handlers.
     * 
     * @param properties
     * @return
     */
    protected int getResultCode(Properties properties) {
        int result = 0;
        String temp = properties.getProperty(Config.PropertyTargetResultCode);

        if (temp != null) {
            result = Integer.parseInt(temp);
        }

        return result;
    }

    private IResourceTicketFactory makeTicketFactory() throws Exception {
        String ticketFactoryClassName = Globals.getConfiguration().getTicketFactoryClassName();
        if (ticketFactoryClassName == null) {
            throw new RuntimeException("Missing ticket factory class name");
        }

        IResourceTicketFactory factory = (IResourceTicketFactory) Misc.createInstance(ticketFactoryClassName);
        factory.setActor(actor);
        return factory;
    }

    /**
     * Callback handler for a completing join request
     * 
     * @param token
     * @param properties
     */
    protected void processJoinComplete(Object token, Properties properties) {
    }

    /**
     * Callback handler for a completing leave request.
     * 
     * @param token
     * @param properties
     */
    protected void processLeaveComplete(Object token, Properties properties) {
    }

    protected void processModifyComplete(Object token, Properties properties) {
    }

    /**
     * Sets the actor. Obtains and caches the ral.
     */
    public void setActor(IActor actor) {
        this.actor = actor;
        this.logger = actor.getLogger();
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setDatabase(IDatabase db) {
        this.db = db;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public IResourceTicketFactory getTicketFactory() {
        return ticketFactory;
    }

    public void setTicketFactory(IResourceTicketFactory ticketFactory) {
        this.ticketFactory = ticketFactory;
    }

    /**
     * {@inheritDoc}
     */
    /**
     * Returns the configuration properties list passed at instantiation time to
     * this plugin.
     * 
     * @return configuration properties list
     */
    public Properties getConfigurationProperties() {
        return configurationProperties;
    }

    public boolean isSiteAuthority() {
        return (actor.getType() == OrcaConstants.ActorTypeSiteAuthority);
    }

    public class ConfigurationCompleteEvent implements IActorEvent {
        private ConfigToken token;
        private Properties properties;

        public ConfigurationCompleteEvent(ConfigToken token, Properties properties) {
            this.token = token;
            this.properties = properties;
        }

        public void process() throws Exception {
            processConfigurationComplete(token, properties);
        }
    }
}
