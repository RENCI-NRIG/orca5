/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import orca.extensions.IPluginFactory;
import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.extensions.internal.PackageManager;
import orca.extensions.internal.Plugin;
import orca.extensions.internal.PluginManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.ResultMng;
import orca.manage.internal.ManagementObject;
import orca.manage.internal.ManagementObjectManager;
import orca.manage.internal.UserSet;
import orca.manage.internal.api.IActorManagementObject;
import orca.manage.internal.api.IManagementObject;
import orca.manage.internal.soap.SoapActorService;
import orca.manage.internal.soap.SoapAuthorityService;
import orca.manage.internal.soap.SoapBrokerService;
import orca.manage.internal.soap.SoapClientActorService;
import orca.manage.internal.soap.SoapContainerService;
import orca.manage.internal.soap.SoapServerActorService;
import orca.manage.internal.soap.SoapServiceManagerService;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorIdentity;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IProxy;
import orca.shirako.api.ITick;
import orca.shirako.common.ConfigurationException;
import orca.shirako.container.api.ContainerState;
import orca.shirako.container.api.IActorContainer;
import orca.shirako.container.api.IConfigurationLoader;
import orca.shirako.container.api.IOrcaAdminConfiguration;
import orca.shirako.container.api.IOrcaConfiguration;
import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.shirako.core.Actor;
import orca.shirako.kernel.OrcaTick;
import orca.shirako.kernel.RPCManager;
import orca.shirako.kernel.interfaces.ITicker;
import orca.shirako.proxies.ActorLocation;
import orca.shirako.proxies.ProxyFactory;
import orca.shirako.proxies.ServiceFactory;
import orca.shirako.proxies.soapaxis2.SoapAxis2ServletContextInitializer;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.util.SpringWSUtils;
import orca.tools.axis2.Axis2ClientSecurityConfigurator;
import orca.util.ExceptionUtils;
import orca.util.ID;
import orca.util.KeystoreManager;
import orca.util.OrcaThreadPool;
import orca.util.PropList;
import orca.util.ReflectionUtils;
import orca.util.db.DatabaseBase;
import orca.util.persistence.PersistenceUtils;

/**
 * <code>ShirakoContainerManager</code> is the "heart" of Shirako-based system.
 * The container manager is responsible for managing the Shirako instance. This
 * is a singleton class.
 */
@SuppressWarnings("unchecked") //It is safe to ignore the warning. See Reflection docs.
public class OrcaContainer <T> implements IActorContainer  {
    public static final String PropertyBeginningOfTime = "BeginningOfTime";
    public static final String PropertyCycleMillis = "CycleMillis";
    public static final String PropertyManualTicks = "ManualTicks";
    
    public static final String PropertyTime = "time";
    public static final String PropertyProtocols = "protocols";
    public static final String PropertyLocationCount = "locations.count";
    public static final String PropertyLocationNamePrefix = "locations.name.";
    public static final String PropertyLocationValuePrefix = "locations.value.";
    public static final String PropertyContainerGuid = "ContainerGuid";

    public static final String PropertyRegistryUrl = "registry.url";
    public static final String PropertyRegistryMethod = "registry.method";
    public static final String PropertyRegistryCertFingerprint = "registry.certfingerprint";
    public static final String PropertyReplicationMode="registry.replication";
    
    /**
     * @author claris
     * Properties for distributed actor registry cache with Couchdb backend
     */    
    public static final String PropertyRegistryUrl_1 = "registry.url.1";
    public static final String PropertyRegistryUrl_2 = "registry.url.2";
    public static final String PropertyRegistryCertFingerprint_1 = "registry.certfingerprint.1";
    public static final String PropertyRegistryCertFingerprint_2 = "registry.certfingerprint.2";
    public static final String PropertyRegistryCouchDBUsername = "registry.couchdb.username";
    public static final String PropertyRegistryCouchDBPassword = "registry.couchdb.password";
    public static final String PropertyRegistryClass = "registry.class";

    /**
     * Registry classes
     */
    protected String defaultRegistryActor = "orca.shirako.container.RemoteRegistryCache";
    protected String selectedRegistryActor = null;
    /**
     * The container lock.
     */
    protected Object containerLock = new Object();
    /**
     * Container state.
     */
    protected ContainerState state = ContainerState.None;
    /**
     * Container GUID. This object is created when processing the configuration
     * file.
     */
    protected ID guid;
    /**
     * The administrative-level container configuration.
     */
    protected IOrcaAdminConfiguration configuration;
    /**
     * The clock: responsible for delivering tick events to subscribed objects.
     * This object is created when processing the configuration file.
     */
    protected ITicker tick;
    /**
     * Supported protocols in this container. Controls the creation/deployment
     * of protocol handlers.
     */
    protected Hashtable<String, ProtocolDescriptor> protocols;
    /**
     * True if this container was created by processing a fresh configuration
     * file. False, if the container was recovered from state stored in a
     * database.
     */
    protected boolean fresh = true;
    /**
     * True if the recovery code has been invoked.
     */
    protected boolean recovered = false;
    /**
     * The container database.
     */
    protected IOrcaContainerDatabase db;

    /**
     * Users
     */
    protected UserSet users;

    /**
     * The package manager
     */
    protected PackageManager packageManager;

    /**
     * The plugin manager
     */
    protected PluginManager pluginManager;

    /**
     * manager for manager objects
     */
    protected ManagementObjectManager managementObjectManager;

    /**
     * Creates a new instance of the container manager.
     */
    public OrcaContainer() {
        protocols = new Hashtable<String, ProtocolDescriptor>();
        users = new UserSet();
        packageManager = new PackageManager();
        pluginManager = new PluginManager();
        managementObjectManager = new ManagementObjectManager();
        
    }

    public static <T> T create(final Class<T> classToCreate) {
        final Constructor<T> constructor;
        
        try {
            constructor = classToCreate.getDeclaredConstructor();
            constructor.setAccessible(true);
            final T result = constructor.newInstance(new Object[0]);
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
       
    }
    /**
     * {@inheritDoc}
     */
    public void initialize(IOrcaAdminConfiguration config) throws ContainerInitializationException {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }

        synchronized (containerLock) {
            if (state != ContainerState.None) {
                throw new ContainerInitializationException("Cannot initialize container in state: " + state);
            }
            state = ContainerState.Starting;
        }

        this.configuration = config;
        boolean failed = false;
        try {
			OrcaThreadPool.start();
        	determineBootMode();
            createDatabase();
         
            selectedRegistryActor=defaultRegistryActor;
            if(config.getConfiguration().getProperty(PropertyRegistryClass)!=null) {
            	selectedRegistryActor = config.getConfiguration().getProperty(PropertyRegistryClass).trim();
            }
            final Class<?> registryCacheClass =  Class.forName(selectedRegistryActor);
           
            T t = (T) create(registryCacheClass);//--->It is ok to ignore cast check warning. See documentation on reflection.
          
            Method getInstance = registryCacheClass.getDeclaredMethod("getInstance");
		    Object result2 = getInstance.invoke(null, new Object[0]);
			T t2 = (T)registryCacheClass.cast(result2);
            
			
			
			Method  configureSSL = registryCacheClass.getDeclaredMethod ("configureSSL");
			configureSSL.invoke(result2, new Object[0]);
		
            boot();
         
            // query  registry to fill in our local cache 
          
			Method singleQuery = registryCacheClass.getDeclaredMethod("singleQuery");
			singleQuery.invoke(result2, new Object[0]);
			
			
            if (isFresh()) {
            	
                try {
                    loadConfiguration();
                   
                } catch (Exception e) {
                    Globals.Log.error("Failed to instantiate actors", e);
                    Globals.Log.error("This container may need to be restored to a clean state");
                }
            } else {
            	// process results of the query to add new edges if necessary
            	// recovery should've re-created existing edges by now
            	
    			Method singleQueryProcess = registryCacheClass.getDeclaredMethod("singleQueryProcess");
    			singleQueryProcess.invoke(result2, new Object[0]);
    			
            }
            // need to wait to start this query thread until configuration is completed
            Method start = registryCacheClass.getDeclaredMethod("start");
            start.invoke(result2, new Object[0]);
          
          
        } catch (ContainerInitializationException e) {
            failed = true;
            throw e;
        } catch (Exception e) {
            failed = true;
            throw new ContainerInitializationException(e);
        } finally {
            synchronized (containerLock) {
                if (failed) {
                    state = ContainerState.Failed;
                } else {
                    state = ContainerState.Started;
                }
            }
        }
    }

    private void determineBootMode() {
        String fileName = Globals.SuperblockLocation;
        File file = new File(fileName);
        Globals.Log.debug("Checking if this container is recovering. Looking for: " + fileName);
        if (file.exists()) {
            Globals.Log.debug("Found superblock file. This container is recovering");
            fresh = false;
        } else {
            Globals.Log.debug("Superblock file does not exist. This is a fresh container");
            fresh = true;
        }
    }

    private void createDatabase() throws ContainerInitializationException {
        Globals.Log.info("Creating container database");
        Properties p = configuration.getContainerDatabaseConfiguration();
        if (p == null) {
            throw new ContainerInitializationException("Missing container database configuration");
        }

        try {
            String className = p.getProperty(DatabaseBase.PropertyDBClassName);
            Class<IOrcaContainerDatabase> objectClass = ReflectionUtils.getClass(className);
            db = PersistenceUtils.restore(objectClass, p);
             if (isFresh()) {
                db.setResetState(true);
            } else {
                db.setResetState(false);
            }
            db.initialize();
            Globals.Log.info("Container database created successfully");
        } catch (Exception e) {
            throw new ContainerInitializationException(e);
        }
    }

    private void createSuperblock() throws Exception {
        Globals.Log.debug("Creating superblock");
        File f = new File(Globals.SuperblockLocation);
        PrintWriter w = new PrintWriter(new FileOutputStream(f));
        try {
        	w.println("This file tells the Orca container to maintain its state on recovery. Hence, removing this file will make the container discard and reset its state.");
            Globals.Log.debug("Superblock created successfully");
        }finally {
        	w.close();
        }
    }

    private void boot() throws Exception {
        Globals.Log.debug("Booting");
    	bootCommon();
        if (isFresh()) {
            Globals.Log.info("Booting a fresh container");
            bootBasic();
            finishFreshBoot();
        } else {
            Globals.Log.info("Recoverying an existing container");
            recoverBasic();
            recoverActors();
            finishRecoveryBoot();
        }
        writeOrcaStateFile();
    }

    protected void registerManagementWebServices() {
		Globals.Log.debug("Registering web service endpoints");
		
    	SpringWSUtils.registerEndpoint(SoapActorService.class);
		SpringWSUtils.registerEndpoint(SoapAuthorityService.class);
		SpringWSUtils.registerEndpoint(SoapBrokerService.class);
		SpringWSUtils.registerEndpoint(SoapClientActorService.class);
		SpringWSUtils.registerEndpoint(SoapContainerService.class);
		SpringWSUtils.registerEndpoint(SoapServerActorService.class);
		SpringWSUtils.registerEndpoint(SoapServiceManagerService.class);  		
    }
    
    protected void bootCommon() throws Exception {
        Globals.Log.debug("Performing common boot tasks");
        
    	fixKeys();
        fixScripts();
        defineProtocols();

        if (Globals.isInsideServletContainer()) {
        	Globals.Log.debug("Running inside a servlet container");
        	Globals.Log.debug("Initializing axis2");
        	
        	SoapAxis2ServletContextInitializer init = new SoapAxis2ServletContextInitializer();
            init.initialize(getConfiguration());
            
            registerManagementWebServices();            
        }

        packageManager.initialize(db);
        pluginManager.initialize(db);
        managementObjectManager.initialize(db);
        RPCManager.start();
    }

    private void defineProtocols() {
    	Globals.Log.debug("Defining container protocols");
        // define the local protocol
        ProtocolDescriptor desc = new ProtocolDescriptor(OrcaConstants.ProtocolLocal, null);
        registerProtocol(desc);
        // check if soapaxis2 is to be enabled
        String temp = getConfiguration().getProperty(IOrcaConfiguration.PropertySoapAxis2Url);
        if (temp != null) {
            desc = new ProtocolDescriptor(OrcaConstants.ProtocolSoapAxis2, temp);
            registerProtocol(desc);
        }
    }

    protected void bootBasic() throws Exception {
        guid = configuration.getConfiguration().getContainerGUID();
        Globals.Log.info("Container guid is: " + guid.toString());
        setTime();
        persistBasic();
        createSuperblock();
    }

    protected void persistBasic() throws Exception {
        persistContainer();
        persistTime();
    }

    /**
     * Persists time configuration
     * @throws Exception in case of error
     */
    protected void persistContainer() throws Exception {
        Properties p = new Properties();
        p.setProperty(PropertyContainerGuid, getGuid().toString());
        db.addContainerProperties(p);
    }

    /**
     * Persists time configuration
     * @throws Exception in case of error
     */
    protected void persistTime() throws Exception {
        Properties p = new Properties();
        p.setProperty(PropertyTime, PropertyTime);
        PropList.setProperty(p, PropertyBeginningOfTime, tick.getBeginningOfTime());
        PropList.setProperty(p, PropertyCycleMillis, tick.getCycleMillis());
        PropList.setProperty(p, PropertyManualTicks, tick.isManual());
        db.addTime(p);
    }


    private void fixKeys() throws Exception {
        Globals.Log.info("Changing keys permissions to 700");

        String command = "chmod -R 700 " + Globals.HomeDirectory + "/keys";
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(command);
        p.waitFor();
    }

    private void fixScripts() throws Exception {
        Globals.Log.info("Making scripts executable");

        String command = "chmod -R u+x " + Globals.HomeDirectory + "/scripts";
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(command);
        p.waitFor();
    }

    protected void setTime() throws Exception {
        long startTime = getConfiguration().getTimeStart();
        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        }
        createAndStartTick(startTime, getConfiguration().getCycleMillis(), getConfiguration().getManualTime());        
    }
    
    private void createAndStartTick(long startTime, long cycleMillis, boolean manual) throws Exception {
        Globals.Log.debug("Creating container ticker");
    	
        tick = new OrcaTick();
        tick.setBeginningOfTime(startTime);
        tick.setCycleMillis(cycleMillis);
        tick.setManual(manual);
        tick.initialize();

        tick.start();    	
        
        if (!tick.isManual()) {
            Globals.Log.info("Using automatic ticks. Tick length=" + tick.getCycleMillis() + "ms");
        } else {
            Globals.Log.info("Using manual ticks. Tick length=" + tick.getCycleMillis() + "ms");
        }
    }

    protected void finishFreshBoot() throws Exception {
        packageManager.installPackages();
        /*
         * Instantiate the container manager object.
         */
        createContainerManagerObject();
        /*
         * instantiate all extensions as specified by the container
         * configuration properties
         */
        loadExtensions();
    }

    protected void finishRecoveryBoot() throws Exception {
    }

    protected void createContainerManagerObject() throws Exception {
        Globals.Log.info("Creating container manager object");

        String name = configuration.getContainerManagerObjectClass();

        if (name == null) {
            Globals.Log.error("Missing container manager object class name...");
        } else {
            Class<?> c = Class.forName(name);
            ManagementObject mo = (ManagementObject) c.newInstance();
            mo.initialize();
            managementObjectManager.registerManagerObject(mo);
        }
    }

    /**
     * Loads all container-level extensions specified in the container
     * configuration file.
     */
    protected void loadExtensions() {
        Globals.Log.info("Instantiating extensions...");

        /*
         * Failure to load extensions will not prevent the container from
         * starting.
         */
        Vector<String> plugins = configuration.getPlugins();

        for (String plugin : plugins) {
            try {
                String[] split = plugin.split(",");

                if (split.length != 2) {
                    Globals.Log.error("Plugin descriptor is invalid: " + plugin);
                } else {
                    int code = instantiatePlugin(new PackageId(split[0]), new PluginId(split[1]));

                    if (code != 0) {
                        Globals.Log.error("An error occurred while instantiating plugin: " + plugin);
                    }
                }
            } catch (Exception e) {
                Globals.Log.error("An error occurred while instantiating plugin: " + plugin);
            }
        }
    }

    protected int instantiatePlugin(PackageId packageId, PluginId pluginId) {
        int result = 0;

        if (Globals.Log.isDebugEnabled()) {
            Globals.Log.debug("Instantiating plugin. PackageId=" + packageId + " pluginId=" + pluginId);
        }

        try {
            Plugin plugin = pluginManager.getPlugin(packageId, pluginId);

            if (plugin == null) {
                throw new Exception("Could not find plugin: (" + packageId + "," + pluginId +")");
            }

            if ((plugin.getPluginType() != Plugin.TypeManagerObject) && (plugin.getPluginType() != Plugin.TypePortalPlugin)) {
                throw new Exception("Invalid plugin type");
            }

            if (!plugin.isFactory()) {
                throw new Exception("The plugin is not a factory");
            }

            if (plugin.getClassName() == null) {
                throw new Exception("Missing plugin class name");
            }

            Class<?> c = Class.forName(plugin.getClassName());
            Object obj = c.newInstance();

            if (!(obj instanceof IPluginFactory)) {
                throw new Exception("The actor plugin does not implement all required interfaces");
            }

            IPluginFactory factory = (IPluginFactory) obj;
            factory.create();

            ManagementObject m = factory.getManager();

            if (m != null) {
                managementObjectManager.registerManagerObject(m);
            }
        } catch (Exception e) {
            Globals.Log.error("Could not instantiate extension", e);
            result = OrcaConstants.ErrorInternalError;
        }

        return result;
    }

    protected void recoverBasic() throws ContainerRecoveryException {
        if (isFresh()) {
            throw new ContainerRecoveryException("A fresh container cannot be recovered");
        }

        synchronized (containerLock) {
            if (state != ContainerState.Starting) {
                throw new ContainerRecoveryException("Invalid state for recovery: " + state);
            }
            state = ContainerState.Recoverying;
        }

        recoverGuid();
        recoverTime();
    }

    private void recoverGuid() throws ContainerRecoveryException {
        try {
        	Globals.Log.debug("Recoverying container GUID");
        	Vector<Properties> v = db.getContainerProperties();

            if ((v != null) && (v.size() != 0)) {
                Properties p = v.get(0);
                if (!p.containsKey(PropertyContainerGuid)) {
                    throw new ContainerRecoveryException("Container GUID missing in database");
                }
                guid = new ID(p.getProperty(PropertyContainerGuid));
                Globals.Log.info("Recovered container guid: " + guid.toString());
            } else {
                throw new ContainerRecoveryException("Could not obtain saved container GUID from database");
            }
        } catch (ContainerRecoveryException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerRecoveryException("GUID recovery failed", e);
        }
    }

    private void recoverTime() throws ContainerRecoveryException {
        try {
        	Globals.Log.debug("Recoverying container time settings");
        	Vector<Properties> v = db.getTime();

            if ((v != null) && (v.size() != 0)) {
                Properties p = (Properties) v.get(0);
                long beginningOfTime = PropList.getRequiredLongProperty(p, PropertyBeginningOfTime);
                long cycleMillis = PropList.getRequiredLongProperty(p, PropertyCycleMillis);
                boolean manual = PropList.getBooleanProperty(p, PropertyManualTicks, false);
                
                createAndStartTick(beginningOfTime, cycleMillis, manual);
            } else {
                throw new ContainerRecoveryException("Could not obtain container saved state from database");
            }
        } catch (ContainerRecoveryException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerRecoveryException("Clock recovery failed", e);
        }
    }

    private void recoverActors() throws ContainerRecoveryException {
        try {
            Globals.Log.info("Recoverying actors");
            Vector<Properties> v = db.getActors();
            int count = (v != null) ? (v.size()) : 0;
            Globals.Log.info("Found " + count + " actors");
            
            if (v != null) {
                for (Properties p : v){
                	try {
                        recoverActor(p);
                    } catch (ContainerRecoveryException e) {
                        Globals.Log.error("Failed to recover actor: " + Actor.getName(p));
                    }
                }
            }
        } catch (Exception e) {
            throw new ContainerRecoveryException("Unexpected error during actor recovery");
        }
    }

    public IActor recoverActor(Properties p) throws ContainerRecoveryException {
        String actorName = Actor.getName(p);
        try {
            if (actorName == null) {
            	throw new ContainerRecoveryException("Cannot recover actor: no name");
            }
            
        	Globals.Log.info("Recoverying actor " + actorName);

        	// first restore the object from the saved state
        	Globals.Log.debug("Restoring actor from saved state");
        	IActor actor = PersistenceUtils.restore(p);            
        	Globals.Log.debug("Initializing the actor object");            
            actor.initialize();
            
            Globals.Log.debug("Recoverying the actor object for actor " + actorName);
            PersistenceUtils.recover(actor, p);

            /*
             * By now we have a valid actor object. We need to register it with
             * the container and call recovery for its reservations.
             */
            registerRecoveredActor(actor);
            
            // Trigger recovery from the saved database state
            Globals.Log.debug("Starting recovery from database for actor "+ actorName);
            actor.recover();
            register((ITick) actor);
//Claris: This is a horrible hack to avoid dealing with actor serialization.
           /*start updating the global actor registry */
  		
            if(selectedRegistryActor.contains("Distributed")) {
            	DistributedRemoteRegistryCache.registerWithRegistry(actor);
            } else {
            	RemoteRegistryCache.registerWithRegistry(actor);
            }
 			
            
            Globals.Log.info("Actor " + actorName + " recovered successfully");
            return actor;
        } catch (Exception e) {
            Globals.Log.error("Actor " + actorName + " failed to recover", e);
            throw new ContainerRecoveryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void loadConfiguration() throws Exception {
    	String conf = System.getProperty("ORCA_ACTOR_XML");    	
    	if (conf != null) {    		
    		File f = new File(conf);
    		if (f.exists()) {
        		loadConfiguration(f.getAbsolutePath());
        		return;    			
    		}
    	}
    	
    	// config file in $ORCA_HOME/config/config.xml
    	String config = System.getProperty("orca.config.xml");
    	if (config == null){
    		config = Globals.HomeDirectory + ContainerConstants.DefaultConfigLocation;
    	}
    	if (!config.startsWith(System.getProperty("file.separator"))){
    		config  = Globals.HomeDirectory + config;
    	}
    	Globals.Log.info("Checking for " + config);
    	File f = new File(config);
    	if (f.exists()) {
    		Globals.Log.info("Succeeded, loading configuration");
    		loadConfiguration(f.getAbsolutePath());
    		return;
    	}

    	Globals.Log.info("Did not find any actor configuration");
    }

    /**
     * {@inheritDoc}
     */
    public void loadConfiguration(byte[] config) throws Exception {
        IConfigurationLoader loader = ((OrcaAdminConfiguration) configuration).getConfigurationLoader();
        loader.setConfiguration(config);
        loader.process();
    }

    /**
     * {@inheritDoc}
     */
    public void loadConfiguration(InputStream config) throws Exception {
        IConfigurationLoader loader = ((OrcaAdminConfiguration) configuration).getConfigurationLoader();
        loader.setConfiguration(config);
        loader.process();
    }

    /**
     * {@inheritDoc}
     */
    public void loadConfiguration(String file) throws Exception {
        IConfigurationLoader loader = ((OrcaAdminConfiguration) configuration).getConfigurationLoader();
        loader.setConfiguration(file);
        loader.process();
    }

    // FIXME: combine add and load into a single method

    /**
     * {@inheritDoc}
     */
    public void register(ITick tickable) {
        tick.addTickable(tickable);
    }

    /**
     * Unregisters the specified object from the container ticker.
     */
    public void unregister(final ITick tickable) {
        tick.removeTickable(tickable);
    }

    /**
     * Registers a new actor: adds the actor to the database, deploys services
     * required by the actor, registers actor proxies and callbacks. Must not
     * register the actor with the clock! Clock registration is a separate
     * phase.
     * @param actor actor
     * @throws Exception in case of error
     */
    public void registerActor(IActor actor) throws Exception {
        ((IOrcaContainerDatabase) db).addActor(actor);
        actor.actorAdded();         
        registerManagementObjects(actor);        
        registerCommon(actor);
        actor.start();
    }
    
    /**
     * Registers the given certificate with the admin key store.
     * @param certificate the certificate
     * @param alias alias for the certificate
     * @return result code
     */
    protected int registerCertificate(Certificate certificate, String alias) {
        int code = 0;
       
        try {
            configuration.getKeyStore().addTrustedCertificate(alias, certificate);
        } catch (Exception e) {
            Globals.Log.error("Could not register actor certificate", e);
            code = OrcaConstants.ErrorInternalError;
        }

        return code;
    }

    /**
     * Unregisters the actor from the container.
     * @param actor actor
     * @throws Exception in case of error
     */
    public void unregisterActor(IActor actor) throws Exception {
        // do not delete from the database!!!

        /* unregister with the management interface */
        managementObjectManager.unloadActorManagerObjects(actor.getName());
        // stop and undeploy all services
        undeployServices(actor);
        // this will unregister the actor and its proxies
        ActorRegistry.unregister(actor);
    }

    /**
     * Performs the common steps required to register an actor with the
     * container.
     * @param actor actor
     * @throws Exception in case of error
     */
    protected void registerCommon(IActor actor) throws Exception {
        // add the actor to the actor registry
        ActorRegistry.registerActor(actor);
        // deploy all services required by this actor to communicate with other
        // actors
        deployServices(actor);
        // registers the required proxies
        registerProxies(actor);
        /* register the actor public key certificate with the container keystore */
        Globals.Log.debug("Obtaining actor certificate");

        Certificate certificate = actor.getShirakoPlugin().getKeyStore().getActorCertificate();
        Globals.Log.debug("Registering actor certificate with the admin keystore");
        registerCertificate(certificate, actor.getGuid().toString());
        Globals.Log.debug("Finished registering actor certificate");
       
        //Claris: This is a horrible hack to avoid dealing with actor serialization
        if(selectedRegistryActor.contains("Distributed")){
        	DistributedRemoteRegistryCache.registerWithRegistry(actor);
        }
        else {
        	RemoteRegistryCache.registerWithRegistry(actor);
        }
    }

    
    protected void registerManagementObjects(IActor actor) throws Exception {
    	String name = actor.getManagementObjectClass();
    	if (name == null) {
    		Globals.Log.warn("Actor " + actor.getName() + " did not specify a management object.");
    		return;
    	}
    	
    	IActorManagementObject mo = (IActorManagementObject)ReflectionUtils.createInstance(name);
    	mo.setActor(actor);
    	mo.initialize();
    	managementObjectManager.registerManagerObject(mo);
    }
    /**
     * Remove actor metadata
     * @param actorName actor name;
     * @throws Exception in case of error
     */
    // FIXME: use guid
    public void removeActor(String actorName) throws Exception {
        db.removeActor(actorName);
        IActor actor = ActorRegistry.getActor(actorName);
        if (actor != null){
        	ActorRegistry.unregister(actor);
        }
        actor.actorRemoved();
    }

    /**
     * Remove actor database
     * @param actorName actor name
     * @throws Exception in case of error
     */
    public void removeActorDatabase(String actorName) throws Exception {
        db.removeActorDatabase(actorName);
    }

    /**
     * {@inheritDoc}
     */
    public void registerProtocol(ProtocolDescriptor protocol) {
        protocols.put(protocol.getProtocol(), protocol);
        Globals.Log.debug("Registered container protocol: " + protocol.getProtocol());
    }

    /**
     * Deploys the required services for this actor.
     * @param actor actor
     * @throws Exception in case of error
     */
    protected void deployServices(IActor actor) throws Exception {
        Globals.Log.debug("Deploying services for actor: " + actor.getName());
    	for (ProtocolDescriptor d : protocols.values()) {
            ServiceFactory.getInstance().deploy(d, actor);
        }
    }

    /**
     * Undeploys services for the actor
     * @param actor actor
     * @throws Exception in case of error
     */
    protected void undeployServices(final IActor actor) throws Exception {
    	Globals.Log.debug("Undeploying services for actor: " + actor.getName());
        for (ProtocolDescriptor d : protocols.values()) {
            ServiceFactory.getInstance().undeploy(d, actor);
        }
    }

    /**
     * Registers all proxies for the specified actor.
     * @param actor actor
     * @throws Exception in case of error
     */
    protected void registerProxies(IActor actor) throws Exception {
    	Globals.Log.debug("Registering proxies for actor: " + actor.getName());
    	
    	for (ProtocolDescriptor d : protocols.values()) {
            ActorLocation location = new ActorLocation();
            location.setDescriptor(d);

            /*
             * NOTE: We don't have or need a type here, so just pass null. Safe
             * because these are all local actors.
             */

            IProxy proxy = ProxyFactory.newProxy(d.getProtocol(), actor, location, null);

            if (proxy != null) {
                ActorRegistry.registerProxy(proxy);
            }

            ICallbackProxy callback = ProxyFactory.newCallback(d.getProtocol(), actor, location);

            if (callback != null) {
                ActorRegistry.registerCallback(callback);
            }
        }
    }

    /**
     * Registers a recovered actor.
     * @param actor recovered actor
     * @throws Exception in case of error
     */
    public void registerRecoveredActor(IActor actor) throws Exception {
        Globals.Log.debug("Regisering a recovered actor");
    	actor.actorAdded();        	
    	registerCommon(actor);    	
        loadActorManagementObjects(actor);
        actor.start();
     }

    protected void loadActorManagementObjects(IActor actor) {
        /* XXX: for now failure to load management objects will not propagate up */
        try {
            managementObjectManager.loadActorManagementObjects(actor.getName());
        } catch (Exception e) {
            Globals.Log.error("Error while loading manager objects for actor: " + actor.getName(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws Exception {
        if (tick != null) {
            tick.stop();
        } else {
            throw new RuntimeException("The container does not have a clock");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void tick() {
        if (tick != null) {
            tick.tick();
        } else {
            throw new RuntimeException("The container does not have a clock");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        Globals.Log.info("Orca container shutting down...");
        try {
        	 final Class<?> c =  Class.forName(selectedRegistryActor);
            
            
            Method getInstance = c.getDeclaredMethod("getInstance");
 			Object result2 = getInstance.invoke(null, new Object[0]);
 			T t2 = (T)c.cast(result2);
 			
 			Method stop = c.getDeclaredMethod("stop");
 			stop.invoke(result2, new Object[0]);
            ActorLiveness.allStop();
            //?
            removeOrcaStateFile();
            // stop ticking
            stop();
            // stop RPCs (incoming/outgoing)
            Globals.Log.info("Stopping RPC manager");
            RPCManager.stop();            
            // stop each actor
            Globals.Log.info("Stopping actors");
            IActor[] actors = ActorRegistry.getActors();
            if (actors != null){
            	for (IActor actor : actors){
            		Globals.Log.info("Stopping actor: " + actor.getName());
            		actor.stop();
            		unregisterActor(actor);
            	}
            }
            // cleanup axis2
            if (Globals.isInsideServletContainer()) {
                SoapAxis2ServletContextInitializer init = new SoapAxis2ServletContextInitializer();
                init.shutdown();
            }
            // clean the actor registry
            ActorRegistry.clear();
            // Globals.eventManager.clearSubscriptions();
            Globals.Log.info("Waiting for threadpool tasks to complete");

            OrcaThreadPool.shutdown();
            if (!OrcaThreadPool.awaitTermination(10000)) {
            	Globals.Log.warn("At least one threadpool task did not complete on time");
            }
            Globals.Log.info("Orca container is no longer active...");
        } catch (Exception e) {
            Globals.Log.error("Error while shutting down", e);
        }
    }

	private void writeOrcaStateFile() throws Exception {
		OrcaState state = OrcaState.getInstance();
		Globals.Log.info("Creating orca state file");
		state.createStateFile();
	}

	private void removeOrcaStateFile() throws Exception {
		// delete orca state file
		OrcaState state = OrcaState.getInstance();
		Globals.Log.info("Deleting orca state file");
		state.deleteStateFile();
	}

	/**
     * {@inheritDoc}
     */
    public ActorClock getActorClock() {
        if (tick == null) {
            throw new RuntimeException("No tick");
        }

        return new ActorClock(this.tick.getBeginningOfTime(), this.tick.getCycleMillis());
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentCycle() {
        if (tick == null) {
            return -1;
        }

        return tick.getCurrentCycle();
    }


    
    public String getAxis2ClientRepository() {
        return getConfiguration().getAxis2ClientRepository();
    }

    public String getAxis2Configuration(String actorID) {
        return Axis2ClientSecurityConfigurator.getInstance().getAxis2ConfigPath(Globals.HomeDirectory, actorID);
    }

    public String getAxis2UnsecureConfiguration(String actorID) {
        return Axis2ClientSecurityConfigurator.getInstance().getAxis2ConfigNoSignPath(Globals.HomeDirectory, actorID);
    }

    public String getAxis2ClientPropertiesRelativePath(String actorID) {
        return Axis2ClientSecurityConfigurator.getInstance().getClientPropertiesRelativePath(Globals.HomeDirectory, actorID);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isManualClock() {
        boolean result = false;

        if (tick != null) {
            result = tick.isManual();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ID getGuid() {
        return guid;
    }


    /**
     * {@inheritDoc}
     */
    public IOrcaContainerDatabase getDatabase() {
        return db;
    }

    /**
     * {@inheritDoc}
     */
    public IOrcaConfiguration getConfiguration() {
        return configuration.getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFresh() {
        return fresh;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRecovered() {
        return recovered;
    }

    /**
     * Returns the manager object manager
     * @return the manager object manager
     */
    public ManagementObjectManager getManagementObjectManager() {
        return managementObjectManager;
    }

    /**
     * Searches in the management object registry for the specified object.
     * @param objectID object GUID
     * @return manager object (if found), null otherwise
     */
    public IManagementObject getManagementObject(ID objectID) {
        assert managementObjectManager != null;

        if (objectID == null) {
            throw new IllegalArgumentException("objectID cannot be null");
        }

        return managementObjectManager.getManagementObject(objectID);
    }

    /*
     * ========================================================================
     * Extension Package Management
     * ========================================================================
     */

    /**
     * Returns the package manager
     * @return the package manager
     */
    public PackageManager getPackageManager() {
        return packageManager;
    }

    /**
     * Returns the plugin manager
     * @return the plugin manager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Returns the root forlder for the specified package
     * @param id id
     * @return the root forlder for the specified package
     */
    public String getPackageRootFolder(PackageId id) {
        return PackageManager.getPackageRootFolder(id);
    }

    public UserSet getUsers() {
        return users;
    }

    public ProtocolDescriptor getProtocolDescriptor(String protocol) {
        return protocols.get(protocol);
    }

    // FIXME: should be moved to a utility class

    /**
     * Attaches exception details
     * @param result Result object
     * @param e Exception
     */
    public void setExceptionDetails(ResultMng result, Exception e) {
        result.setDetails(e.getMessage() + "\n" + ExceptionUtils.getStackTraceString(e.getStackTrace()));
    }

    // FIXME: this method should not be public
    public KeystoreManager getKeyStore() {
        return configuration.getKeyStore();
    }

    public String getAdminIdentifier() {
        return configuration.getAdminIdentifier();
    }
    
    public static IProxy getProxy(String protocol, IActorIdentity identity, ActorLocation location, String type) throws ConfigurationException {
        try {
            IProxy proxy = ActorRegistry.getProxy(protocol, identity.getName());

            if (proxy == null) {
                proxy = ProxyFactory.newProxy(protocol, identity, location, type);
                ActorRegistry.registerProxy(proxy);
            }
            return proxy;
        } catch (Exception e) {
            throw new ConfigurationException("Could not obtain proxy for actor: " + identity.getName() + " protocol: " + protocol, e);
        }
    }
    
    protected static Certificate decodeCertificate(byte[] certificate) throws Exception
    {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certificate);
        Certificate cert = factory.generateCertificate(is);
        is.close();
        return cert;
    }
    
    
    public Certificate getCertificate() {
    	// FIXME: is this the right certificate?
    	return configuration.getKeyStore().getActorCertificate();
    }
    
    public Certificate getCertificate(ID guid){
        try {
            return configuration.getKeyStore().getCertificate(guid.toString());
        } catch (Exception e) {
        	return null;
        }
    }
}
