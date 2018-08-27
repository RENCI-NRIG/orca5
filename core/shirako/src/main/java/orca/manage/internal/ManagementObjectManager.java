/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.manage.internal;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.manage.internal.api.IManagementObject;
import orca.manage.internal.db.IManagementObjectDatabase;
import orca.shirako.container.Globals;
import orca.shirako.container.ProtocolDescriptor;
import orca.shirako.proxies.soapaxis2.SoapAxis2ServiceFactory;
import orca.util.ID;

import org.apache.log4j.Logger;

/**
 * This class makes it possible to
 * register, index, and persist information about <code>ManagementObject</code>s.
 * <p>
 * A <code>ManagementObject</code> can be registered only if no other object with
 * the same identifier has been registered. Registering the same object more than
 * once is not permitted.
 * </p>
 * <p>
 * Each successfully registered management object is serialized and stored in the
 * database. Note that once stored, this object cannot be updated. The
 * stored information is sufficient to recreate an instance of the
 * <code>ManagementObject</code>. Objects deriving from
 * <code>ManagementObject</code> are responsible for their own persistence.
 * </p>
 */
public class ManagementObjectManager
{
    /**
     * Name for the container service.
     */
    public static final String ContainerServiceName = "container";
    /**
     * "Live" manager objects. These are all manager objects that have been
     * instantiated inside this container.
     */
    protected Hashtable<ID, IManagementObject> objects;

    /**
     * The database.
     */
    protected IManagementObjectDatabase database;

    /**
     * The logger.
     */
    protected Logger logger;

    /**
     * Initialization status.
     */
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    public ManagementObjectManager()
    {
        objects = new Hashtable<ID, IManagementObject>();
    }

    /**
     * Instantiates a manager object from a properties list.
     * @param p Serialized version of the manager object
     * @return a <code>ManagerObject</code> on success
     * @throws Exception if instantiation fails
     */
    protected ManagementObject createInstance(Properties p) throws Exception
    {
        String className = p.getProperty(ManagementObject.PropertyClassName);

        if (className == null) {
            throw new RuntimeException("Missing class name");
        }

        Globals.Log.debug("Creating management object: " + className);
        
        Class<?> c = Class.forName(className);
        Object obj = c.newInstance();

        // the object must be derive from ManagerObject
        if (!(obj instanceof ManagementObject)) {
            throw new RuntimeException("Object does not implement ManagementObject interface");
        }

        // reset the manager object from the properties list
        ManagementObject man = (ManagementObject) obj;
        man.reset(p);
        man.initialize();

        return man;
    }

    /**
     * Retrieves the specified manager object.
     * @param id object guid
     * @return returns Management object
     */
    public IManagementObject getManagementObject(ID id)
    {
        synchronized (objects) {
           return objects.get(id);
        }
    }

    /**
     * Performs initialization. If the system is recovering after a
     * shutdown/crash, loads all manager objects that pertain to the container.
     * @param database database 
     * @throws Exception in case of error
     */
    public void initialize(IManagementObjectDatabase database) throws Exception
    {
        if (database == null){
            throw new IllegalArgumentException("database cannot be null");
        }
        
        if (!initialized) {
            logger = Globals.getLogger(this.getClass().getCanonicalName());
            this.database = database;
            if (!Globals.getContainer().isFresh()) {
                /*
                 * We are recovering. Load all manager objects that are not
                 * associated with an actor. Manager objects associated with a
                 * given actor will be instantiated when we recover that actor.
                 */
                loadContainerManagerObjects();
            }

            initialized = true;
        }
    }

    /**
     * Loads all manager objects associated with the specific actor.
     * @param actorName actor name
     * @throws Exception in case of error
     */
    public void loadActorManagementObjects(String actorName) throws Exception
    {
        Globals.Log.info("Loading container-level management objects for actor: " + actorName);
        Vector<Properties> v = database.getManagerObjects(actorName);
        loadObjects(v);
        Globals.Log.info("Finished loading container-level management objects for actor: " + actorName);
    }

    /**
     * Loads all manager objects not associated with specific actors
     * @throws Exception in case of error
     */
    protected void loadContainerManagerObjects() throws Exception
    {
        Globals.Log.info("Loading container-level management objects");
        Vector<Properties> v = database.getManagerObjectsContainer();
        loadObjects(v);
        Globals.Log.info("Finished loading container-level management objects");
    }

    /**
     * Loads the specified management objects
     * @param v vector of properties
     * @throws Exception in case of error
     */
    protected void loadObjects(Vector<Properties> v) throws Exception
    {
        if (v != null) {
            for (int i = 0; i < v.size(); i++) {
                ManagementObject man = createInstance(v.get(i));

                synchronized (objects) {
                    if (objects.contains(man.getID())) {
                        throw new Exception(
                            "there is already a management object in memory with the specified id");
                    }
                    objects.put(man.getID(), man);
                }
                
                Globals.Log.debug("Loaded management object with id: " + man.getID());
                // attempt to deploy the object services, if any
                try {
                    deployObjectServices(man);
                } catch (Exception e) {
                    logger.error("Could not deploy services for management object: " + man.getID(), e);
                }
            }
        }
    }

    /**
     * Registers a new management object
     * @param object management object
     * @throws Exception in case of error
     */
    public void registerManagerObject(IManagementObject object) throws Exception
    {
        // add to the database
        database.addManagerObject(object);
        
        // add to the hash table
        synchronized (objects) {
            objects.put(object.getID(), object);
        }
        // check if any services must be deployed
        deployObjectServices(object);
    }
        
    protected void deployObjectServices(IManagementObject object) throws Exception
    {
    	// WRITEME: update ManagementObjects so that they can specify Spring web services to deploy.
    	// But first figure out if we even need this capability.
    	
    	
//    	// NOTE: for now we only support services based on SoapAxis2
//        ProtocolDescriptor desc = Globals.getContainer().getProtocolDescriptor(ProtocolNames.SoapAxis2);
//        // note: we must be running inside a servlet container to use soap for now.
//        if (desc != null && Globals.ServletContext != null) {
//            logger.debug("container supports soapaxis2. checking if object supplies a service");
//            String serviceDesc = object.getAxis2ServiceDescriptor();
//            if (serviceDesc != null){
//                logger.debug("object supplies a soap service: " + serviceDesc);
//                URL url =  this.getClass().getClassLoader().getResource(serviceDesc);
//                if (url != null){
//                    String serviceName = getServiceName(object);
//                    logger.debug("Deploying service: " + desc.getLocation() + "/services/" + serviceName);
//                    SoapAxis2ServiceFactory.deployService(serviceName, SoapAxis2ServiceFactory.ScopeApplication, url.openStream());
//                    logger.debug("Deployed service: " + desc.getLocation() + "/services/" + serviceName);
//                }
//            }
//        }
    }

    /**
     * Obtains the service name for the specified management object.
     * @param object management object
     * @return service name
     */
    protected String getServiceName(IManagementObject object)
    {
        String serviceName = object.getID().toString();
        if (object instanceof ContainerManagementObject){
            // this is the container management object
            // service name is ContainerServiceName
            serviceName = ContainerServiceName;
        }
        return serviceName;
    }
    
    /**
     * Unloads all management objects associated with the specific actor.
     * @param actorName actor name
     * @throws Exception in case of error
     */
    public void unloadActorManagerObjects(String actorName) throws Exception
    {
        Vector<Properties> v = database.getManagerObjects(actorName);
        unloadObjects(v);
    }

    /**
     * Unloads the specified management objects
     * @param v vector of properties
     * @throws Exception in case of error
     */
    protected void unloadObjects(Vector<Properties> v) throws Exception
    {
        if (v != null) {
            for (int i = 0; i < v.size(); i++) {
                Properties p = v.get(i);
                String key = p.getProperty(ManagementObject.PropertyID);

                if (key != null) {
                    IManagementObject object;
                    synchronized (objects) {
                        object = objects.remove(key);
                    }
                    if (object != null) {
                        try {
                            ProtocolDescriptor desc = Globals.getContainer().getProtocolDescriptor(OrcaConstants.ProtocolSoapAxis2);
                            if (desc != null){                        
                                SoapAxis2ServiceFactory.undeployService(getServiceName(object));
                            }
                        } catch (Exception e){
                        }
                    }
                } else {
                    logger.error("Management object has no id");
                }
            }
        }
    }

    /**
     * Unregisters the specified management object
     * @param id management object id
     * @throws Exception in case of error
     */
    public void unregisterManager(ID id) throws Exception
    {
        IManagementObject object;
        
        synchronized (objects) {
            object = objects.remove(id);
        }

        database.removeManagerObject(id);
        
        if (object != null) {
            try {
                ProtocolDescriptor desc = Globals.getContainer().getProtocolDescriptor(OrcaConstants.ProtocolSoapAxis2);
                if (desc != null){                        
                    SoapAxis2ServiceFactory.undeployService(getServiceName(object));
                }
            } catch (Exception e){
            }
        }
    }
}
