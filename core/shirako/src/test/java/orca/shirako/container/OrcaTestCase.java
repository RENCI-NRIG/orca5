/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container;

import orca.security.AuthToken;
import orca.shirako.api.*;
import orca.shirako.common.delegation.IResourceTicketFactory;
import orca.shirako.common.delegation.SimpleResourceTicketFactory;
import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.shirako.core.Authority;
import orca.shirako.core.AuthorityPolicy;
import orca.shirako.core.Broker;
import orca.shirako.core.BrokerPolicy;
import orca.shirako.core.Policy;
import orca.shirako.core.ServiceManager;
import orca.shirako.core.ServiceManagerPolicy;
import orca.shirako.core.TestActor;
import orca.shirako.plugins.ShirakoPluginTestWrapper;
import orca.shirako.plugins.db.ActorDatabase;
import orca.shirako.proxies.soapaxis2.SoapAxis2AuthorityProxy;
import orca.shirako.registry.ActorRegistry;
import orca.util.ID;

public abstract class OrcaTestCase extends OrcaTestCaseBase {
    public static final String ActorName = "testActor";
    public static ID ActorGuid = new ID();
    
    public static final String SMName = "sm";
    public static final String AuthorityName = "Authority";
    public static final String BrokerName = "broker";
    
    public static final ID SMGuid = new ID("test-sm-guid");
    public static final ID AuthorityGuid = new ID("test-authority-guid");
    public static final ID BrokerGuid = new ID("test-broker-guid");

    /**
     * Instantiates the actor database.
     * @return
     */
    protected IDatabase makeActorDatabase() {
        ActorDatabase db = new ActorDatabase();
        return db;
    }

    protected IDatabase makeSMDatabase() {
        return makeActorDatabase();
    }
    
    protected IDatabase makeBrokerDatabase() {
        return makeActorDatabase();
    }
    
    protected IDatabase makeAuthorityDatabase() {
        return makeActorDatabase();
    }
    
    /**
     * Instantiates and initializes the database for the default actor.
     * @return
     * @throws Exception
     */
    public IDatabase getActorDatabase() throws Exception {
        return getActorDatabase(ActorName);
    }
    
        
    protected void initializeDatabase(ActorDatabase db, String name) throws Exception {
        db.setDb(MySqlDatabaseName);
        db.setMySqlServer(MySqlDatabaseHost);
        db.setMySqlUser(MySqlDatabaseUser);

        db.setResetState(false);
        db.setActorName(name);
        db.initialize();        
    }
    
    /**
     * Instantiates and initializes the database for the specified actor.
     * @param name
     * @return
     * @throws Exception
     */
    public IDatabase getActorDatabase(String name) throws Exception {
        ActorDatabase db = (ActorDatabase) makeActorDatabase();
        initializeDatabase(db, name);
        return db;
    }

    public IDatabase getSMDatabase(String name) throws Exception {
        ActorDatabase db = (ActorDatabase) makeSMDatabase();
        initializeDatabase(db, name);
        return db;
    }

    public IDatabase getAuthorityDatabase(String name) throws Exception {
        ActorDatabase db = (ActorDatabase) makeAuthorityDatabase();
        initializeDatabase(db, name);
        return db;
    }
    
    public IDatabase getBrokerDatabase(String name) throws Exception {
        ActorDatabase db = (ActorDatabase) makeBrokerDatabase();
        initializeDatabase(db, name);
        return db;
    }
    /**
     * Instantiates and initializes the shirako plugin for the default actor.
     * @return
     * @throws Exception
     */
    public IShirakoPlugin getShirakoPlugin() throws Exception {
        return getShirakoPlugin(ActorName);
    }

    protected IShirakoPlugin makeShirakoPlugin() {
        return new ShirakoPluginTestWrapper();
    }

    protected IShirakoPlugin makeSMShirakoPlugin() {
        return makeShirakoPlugin();
    }

    protected IShirakoPlugin makeAuthorityShirakoPlugin() {
        return makeShirakoPlugin();
    }

    protected IShirakoPlugin makeBrokerShirakoPlugin() {
        return makeShirakoPlugin();
    }

    
    /**
     * Instantiates and initializes the shirako plugi for the specified actor.
     * @param name
     * @return
     * @throws Exception
     */
    public IShirakoPlugin getShirakoPlugin(String name) throws Exception {
        IShirakoPlugin plugin = makeShirakoPlugin();
        plugin.setDatabase(getActorDatabase(name));
        return plugin;
    }

    public IShirakoPlugin getSMShirakoPlugin(String name) throws Exception {
        IShirakoPlugin plugin = makeSMShirakoPlugin();
        plugin.setDatabase(getSMDatabase(name));
        return plugin;
    }
    
    public IShirakoPlugin getBrokerShirakoPlugin(String name) throws Exception {
        IShirakoPlugin plugin = makeBrokerShirakoPlugin();
        plugin.setDatabase(getBrokerDatabase(name));
        return plugin;
    }
    
    public IShirakoPlugin getAuthorityShirakoPlugin(String name) throws Exception {
        IShirakoPlugin plugin = makeAuthorityShirakoPlugin();
        plugin.setDatabase(getAuthorityDatabase(name));
        return plugin;
    }

        
    /**
     * Instantiates the policy class to use.
     * @return
     * @throws Exception
     */
    public IPolicy getPolicy() throws Exception {
        IPolicy policy = new Policy();
        return policy;
    }
    
    public IAuthorityPolicy getAuthorityPolicy() throws Exception {
        return new AuthorityPolicy();
    }
    
    public IBrokerPolicy getBrokerPolicy() throws Exception {
        return new BrokerPolicy();
    }

    public IServiceManagerPolicy getSMPolicy() throws Exception {
        return new ServiceManagerPolicy();
    }

    protected IActor getActorInstance() {
        IActor actor = new TestActor();
        return actor;
    }
    
    protected IAuthority getAuthorityInstance() {
        return new Authority();
    }

    protected IBroker getBrokerInstance() {
        return new Broker();
    }
    
    protected IServiceManager getSMInstance() {
        return new ServiceManager();
    }
    
    public IActor getUninitializedActor(String name, ID guid) throws Exception {
        IActor actor = getActorInstance();
        AuthToken token = new AuthToken(name, guid);
        actor.setIdentity(token);
        actor.setActorClock(getActorClock());
        actor.setPolicy(getPolicy());
        actor.setShirakoPlugin(getShirakoPlugin(name));
        IResourceTicketFactory tf = new SimpleResourceTicketFactory();
        tf.setActor(actor);
        actor.getShirakoPlugin().setTicketFactory(tf);
        return actor;
    }

    public IActor getUninitializedSM(String name, ID guid) throws Exception {
        IActor actor = getSMInstance();
        AuthToken token = new AuthToken(name, guid);
        actor.setIdentity(token);
        actor.setActorClock(getActorClock());
        actor.setPolicy(getSMPolicy());
        actor.setShirakoPlugin(getSMShirakoPlugin(name));
        IResourceTicketFactory tf = new SimpleResourceTicketFactory();
        tf.setActor(actor);
        actor.getShirakoPlugin().setTicketFactory(tf);
        return actor;
    }
    
    public IActor getUninitializedBroker(String name, ID guid) throws Exception {
        IActor actor = getBrokerInstance();
        AuthToken token = new AuthToken(name, guid);
        actor.setIdentity(token);
        actor.setActorClock(getActorClock());
        actor.setPolicy(getBrokerPolicy());
        actor.setShirakoPlugin(getBrokerShirakoPlugin(name));
        IResourceTicketFactory tf = new SimpleResourceTicketFactory();
        tf.setActor(actor);
        actor.getShirakoPlugin().setTicketFactory(tf);
        return actor;
    }

    public IActor getUninitializedAuthority(String name, ID guid) throws Exception {
        IActor actor = getAuthorityInstance();
        AuthToken token = new AuthToken(name, guid);
        actor.setIdentity(token);
        actor.setActorClock(getActorClock());
        actor.setPolicy(getAuthorityPolicy());
        actor.setShirakoPlugin(getAuthorityShirakoPlugin(name));
        IResourceTicketFactory tf = new SimpleResourceTicketFactory();
        tf.setActor(actor);
        actor.getShirakoPlugin().setTicketFactory(tf);
        return actor;
    }
    
    public IActor getActor(String name, ID guid) throws Exception {
        IActor actor = getUninitializedActor(name, guid);
        actor.initialize();
        registerNewActor(actor);
        return actor;
    }

    public IServiceManager getSM(String name, ID guid) throws Exception {
        IActor actor = getUninitializedSM(name, guid);
        actor.initialize();
        registerNewActor(actor);
        return (IServiceManager)actor;
    }
    
    public IBroker getBroker(String name, ID guid) throws Exception {
        IActor actor = getUninitializedBroker(name, guid);
        actor.initialize();
        registerNewActor(actor);
        return (IBroker)actor;
    }
    
    public IAuthority getAuthority(String name, ID guid) throws Exception {
        IActor actor = getUninitializedAuthority(name, guid);
        actor.initialize();
        registerNewActor(actor);
        return (IAuthority)actor;
    }
    
    public IActor getActor() throws Exception {
        return getActor(ActorName, ActorGuid);
    }

    public IServiceManager getSM() throws Exception {
        return getSM(SMName, SMGuid);
    }
    
    public IBroker getBroker() throws Exception {
        return getBroker(BrokerName, BrokerGuid);
    }
    
    public IAuthority getAuthority() throws Exception {
        return getAuthority(AuthorityName, AuthorityGuid);
    }
  
    protected void registerNewActor(IActor actor) throws Exception {
        IOrcaContainerDatabase db = getContainerDatabase();
        db.removeActor(actor.getName());
        db.addActor(actor);
        ActorRegistry.unregister(actor);
        ActorRegistry.registerActor(actor);
        actor.actorAdded();
        actor.start();  // not sure if there is a better way, but this seems to work.
    }
    
    /**
     * Makes and registers a new default actor.
     * @return
     * @throws Exception
     */
    public IActor getRegisteredNewActor() throws Exception {
        IActor actor = getActor();
        registerNewActor(actor);
        return actor;
    }
    
    
    /**
     * Makes and registers a new default actor.
     * @return
     * @throws Exception
     */
    public IActor getRegisteredNewSM() throws Exception {
        IActor actor = getSM();
        registerNewActor(actor);
        return actor;
    }
    
    
    /**
     * Makes and registers a new default actor.
     * @return
     * @throws Exception
     */
    public IActor getRegisteredNewBroker() throws Exception {
        IActor actor = getBroker();
        registerNewActor(actor);
        return actor;
    }
    
    /**
     * Makes and registers a new default actor.
     * @return
     * @throws Exception
     */
    public IActor getRegisteredNewAuthority() throws Exception {
        IActor actor = getAuthority();
        registerNewActor(actor);
        return actor;
    }
}

