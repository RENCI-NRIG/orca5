/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.plugins.config;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.shirako.api.IActorIdentity;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.container.Globals;
import orca.shirako.container.api.IOrcaConfiguration;
import orca.shirako.core.Authority;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class AntConfig extends Config {
    public static final String PropertyAntFile = "ant.file";
    public static final String PropertyAntProjectHelper = "ant.projectHelper";
    public static final String DefaultXml = "common/noop.xml";
    public static final String PropertyConfigCount = "config.count";
    public static final String PropertyConfigTypePrefix = "config.type.";
    public static final String PropertyConfigFilePrefix = "config.file.";
    public static final String PropertyRootDir = "root.dir";
    public static final String PropertyOrcaHome = "orca.home";
    public static final String PropertyActorID = "actor.id";
    public static final String PropertyContainerGuid = "container.guid";
    public static final String PropertyAxis2Repository = "axis2.repository";
    public static final String PropertySecureCommunication = "secure.communication";
    public static final String PropertyEmulation = "emulation";
    public static final String PropertyNodeAgentPort = "na.port";
    public static final String PropertyNodeAgentProtocol = "na.protocol";
    public static final String PropertyNodeAgentUri = "na.uri";
    public static final String PropertyStartTime = "start.time";
    public static final String PropertyEndTime = "stop.time";
	
    public static final int DefaultCapacity = 1000;
    public static final int DefaultThreads = 10;
    public static final int DefaultPollInterval = 1000;
    
    @Persistent
    protected int capacity;
    @Persistent
    protected int maxThreads;
    
    @NotPersistent
    protected Thread[] threads;
    @NotPersistent
    protected Object lock = new Object();
    @NotPersistent
    protected LinkedList<RunConfig> tasks;
    @NotPersistent
    protected int size;
    @NotPersistent
    protected boolean go = true;
    @NotPersistent
    protected Hashtable<String, ConfigurationMapping> files;
    @NotPersistent
    protected String defaultXml = DefaultXml;
    @NotPersistent
    private boolean initialized;
    @NotPersistent
    protected String siteGuid;

    public AntConfig() {
        files = new Hashtable<String, ConfigurationMapping>();
        tasks = new LinkedList<RunConfig>();
        capacity = DefaultCapacity;
        maxThreads = DefaultThreads;
    }

    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();
            try {
	            storeMapping();
	            loadMapping();
	            threads = new Thread[maxThreads];
	
	            for (int i = 0; i < threads.length; i++) {
	                threads[i] = new ExecuteTask();
	                threads[i].start();
	            }
	            getSiteGuid();
	            initialized = true;
            } catch (OrcaException e) {
            	throw e;
            } catch (Exception e) {
            	throw new OrcaException("Cannot initialize", e);
            }
        }
    }

    protected void getSiteGuid() {
        IActorIdentity actor = plugin.getActor();

        if (actor instanceof Authority) {
            siteGuid = ((Authority) actor).getGuid().toString();
        }
    }

    protected void storeMapping() throws Exception {
        Iterator<?> iter = files.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            String key = (String) entry.getKey();
            ConfigurationMapping config = (ConfigurationMapping) entry.getValue();
            plugin.getDatabase().addConfigurationMapping(key, config);
        }
    }

    protected void loadMapping() throws Exception {
        Vector<Properties> v = plugin.getDatabase().getConfigurationMappings();

        if (v != null) {
            for (int i = 0; i < v.size(); i++) {
                Properties p = v.get(i);
                ConfigurationMapping mapping = ConfigurationMapping.newInstance(p);
                addConfigMapping(mapping);
            }
        }
    }

    protected void storeMapping(ConfigurationMapping mapping) {
        if (initialized) {
            try {
                plugin.getDatabase().addConfigurationMapping(mapping.getKey(), mapping);
            } catch (Exception e) {
                logger.error("Error while adding configuration mapping to the database", e);
            }
        }
    }

    public void addConfigMapping(ConfigurationMapping mapping) {
        synchronized (files) {
            files.put(mapping.getKey(), mapping);
        }

        storeMapping(mapping);
    }

    public void addConfigFile(ResourceType resourceType, String file) {
        ConfigurationMapping entry = new ConfigurationMapping();
        entry.setKey(resourceType.getType());
        entry.setConfigFile(file);

        synchronized (files) {
            files.put(entry.getKey(), entry);
        }

        storeMapping(entry);
    }

    public void setConfigFile(String config) {
        String[] tmp = config.split(",");
        ConfigurationMapping entry = new ConfigurationMapping();
        entry.setKey(tmp[0]);
        entry.setConfigFile(tmp[1]);

        synchronized (files) {
            files.put(entry.getKey(), entry);
        }

        storeMapping(entry);
    }

    public void configure(Properties p) throws Exception {
        int count = PropList.getIntegerProperty(p, PropertyConfigCount);

        for (int i = 0; i < count; i++) {
            try {
                ConfigurationMapping entry = new ConfigurationMapping();
                entry.setKey(new ResourceType(p.getProperty(PropertyConfigTypePrefix + i)).getType());
                entry.setConfigFile(p.getProperty(PropertyConfigFilePrefix + i));

                synchronized (files) {
                    files.put(entry.getKey(), entry);
                }
            } catch (Exception e) {
                System.out.println("[AntConfig]Error while processing configuration files list");
            }
        }
    }

    protected ResourceType getResourceType(Properties p) throws Exception {
        String r = p.getProperty(PropertyResourceType);

        if (r == null) {
            throw new Exception("Missing resource type property");
        }

        return new ResourceType(r);
    }

    // protected void attachConfigFile(Properties properties) throws Exception
    // {
    // String file = properties.getProperty(PropertyXmlFile);
    // if (file == null) {
    // ConfigurationMapping entry = null;
    // synchronized (files) {
    // entry = files.get(getResourceType(properties).getType());
    // }
    //
    // file = defaultXml;
    // if (entry != null) {
    // file = entry.getConfigFile();
    // }
    // }
    //
    // if ((!file.startsWith("/")) && (!(file.indexOf(":") == 1))) {
    // file = ContainerManager.getInstance().getRootDirectory() + "/" + file;
    // }
    // properties.setProperty(PropertyConfig, file);
    // }
    protected void attachConfigFile(Properties properties) throws Exception {
        String file = properties.getProperty(ConfigurationProperties.ConfigHandler);
        if (file == null) {
            ConfigurationMapping entry = null;

            synchronized (files) {
                entry = files.get(getResourceType(properties).getType());
            }

            file = defaultXml;

            if (entry != null) {
                file = entry.getConfigFile();
                /*
                 * Take the handler properties and merge them with the passed
                 * properties list. XXX: handler properties have priority. This
                 * is good if we want to prevent clients from overriding
                 * important properties, but also reduces flexibility.
                 */
                Properties handlerProps = entry.getProperties();
                if (handlerProps != null) {
                    PropList.mergeProperties(handlerProps, properties);
                }
            }
        }

        // if not an absolute path, resolve relative to our HomeDirectory
        if ((!file.startsWith("/")) && (!(file.indexOf(":") == 1))) {
            file = Globals.HomeDirectory + "/handlers/" + file;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Using handler: " + file);
        }
        properties.setProperty(PropertyConfig, file);
    }

    protected void preprocess(Properties p) {
        if (siteGuid != null) {
            p.setProperty(UnitProperties.UnitSite, siteGuid);
        }
    }

    public void enqueueBlocking(RunConfig task) throws Exception {
        synchronized (lock) {
            while (size >= capacity) {
                lock.wait();
            }

            tasks.add(task);
            size++;
            lock.notify();
        }
    }

    public void join(ConfigToken token, Properties p) throws Exception {
        preprocess(p);

        RunConfig task = new RunConfig(TargetJoin, p, token);
        enqueueBlocking(task);
        
        if (isSynchronous) {
            while (!task.isFinished()) {
                Thread.sleep(DefaultPollInterval);
            }
        }
    }

    public void leave(ConfigToken token, Properties p) throws Exception {
        preprocess(p);

        RunConfig task = new RunConfig(TargetLeave, p, token);
        enqueueBlocking(task);
        
        if (isSynchronous) {
            // FIXME: replace with wait / notify
            while (!task.isFinished()) {
                Thread.sleep(DefaultPollInterval);
            }
        }
    }

    public void modify(ConfigToken token, Properties p) throws Exception {
        preprocess(p);

        // Get the modify subcommand from the property "modify.subcommand.<index>" and pass the one with the highest index as target
        // For modify to work, the modify.subcommand.<index> property needs to be present, and needs to be a string that starts with "modify."
        // Else it will default to a target called "modify"
        // If there is no "modify.*" or "modify" target in the handler, it will fail in the same way as it will fail when a specified ant target does not exist
        
        int highestIndex = PropList.highestModifyIndex(p, OrcaConstants.MODIFY_SUBCOMMAND_PROPERTY);
        String modifyTarget = p.getProperty(OrcaConstants.MODIFY_SUBCOMMAND_PROPERTY + highestIndex);
        
        if(modifyTarget == null){
        	modifyTarget = TargetModify;
        }
        
        p.setProperty(Config.PropertyModifySequenceNumber, Integer.toString(highestIndex));
        
        //RunConfig task = new RunConfig(TargetModify, p, token);
        RunConfig task = new RunConfig(modifyTarget, p, token);
        enqueueBlocking(task);

        if (isSynchronous) {
            while (!task.isFinished()) {
                Thread.sleep(DefaultPollInterval);
            }
        }
    }

    public void setDefaultXml(String value) {
        this.defaultXml = value;
    }

    protected void setHandlerProperties(Properties properties) {
        IOrcaConfiguration config = Globals.getConfiguration();

        PropList.setProperty(properties, PropertySecureCommunication, config.isSecureCommunication());
        PropList.setProperty(properties, PropertyEmulation, config.isEmulation());
        PropList.setProperty(properties, PropertyAxis2Repository, config.getAxis2ClientRepository());
        PropList.setProperty(properties, PropertyNodeAgentPort, config.getNodeAgentPortNumber());
        PropList.setProperty(properties, PropertyNodeAgentProtocol, config.getNodeAgentProtocol());
        PropList.setProperty(properties, PropertyNodeAgentUri, config.getNodeAgentUri());
    }

    /**
     * This function is called right before executing the handler.
     * @param action name of the action the handler represents
     * @param token object the handler is associated with
     * @param properties handler input properties
     */
    protected void beforeExecute(String action, Object token, Properties properties) {
    }

    class RunConfig {
        private boolean finished = false;
        private String target;
        private Properties properties;
        private ConfigToken token;

        public RunConfig(String target, Properties properties, ConfigToken token) {
            this.target = target;
            this.properties = properties;
            this.token = token;

            setProperties();
        }

        public void setFinished() {
            finished = true;
        }

        public boolean isFinished() {
            return finished;
        }

        /**
         * Constructs a string from the given stack trace
         * @param trace
         * @return
         */
        private String getStackTraceString(StackTraceElement[] trace) {
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < trace.length; i++) {
                sb.append(trace[i].toString());
                sb.append("\n");
            }

            return sb.toString();
        }

        private void setRootDirectory() {
            properties.setProperty(PropertyRootDir, Globals.HomeDirectory);
            properties.setProperty(PropertyOrcaHome, Globals.HomeDirectory);
        }

        private void setActorID() {
            String actorID = plugin.getActor().getGuid().toString();
            properties.setProperty(PropertyActorID, actorID);
        }

        private void setContainerGuid() {
            String prefix = Globals.getContainer().getGuid().toString();
            properties.setProperty(PropertyContainerGuid, prefix);
        }

        private void setProperties() {
            setRootDirectory();
            setContainerGuid();
            setActorID();

            setHandlerProperties(properties);
        }

        private String getFileName() throws Exception {
            String name = properties.getProperty(PropertyConfig);

            if (name == null) {
                throw new Exception("Configuration file is missing");
            }

            return name;
        }

        private void attachProperties(Project project) {
            Iterator<?> i = properties.entrySet().iterator();

            while (i.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                project.setUserProperty(key, value);

                //System.out.println("Setting property: " + key + "=" + value);
            }
        }

        private Properties getProperties(Project project) {
            Properties p = new Properties();
            Hashtable<?, ?> h = project.getProperties();

            Iterator<?> i = h.entrySet().iterator();

            while (i.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
                String key = (String) entry.getKey();
                // XXX: This filtering seems like a premature optimization. We
                // already have
                // those properties in memory. It will make no difference if we
                // return them all.
                // if (key.startsWith(PropertyOutputPrefix) ||
                // key.startsWith("iscsi.") || key.startsWith("join.*")) {
                // p.setProperty(key, (String) entry.getValue());
                // }
                p.setProperty(key, (String) entry.getValue());
            }

            return p;
        }

        private Properties executeTarget() throws Exception {
        	
            File buildFile = new File(getFileName());
            SliceProject project = new SliceProject(token, actorConfigurationLock, handlerSemaphoreMap, secureRandom);

            // ClassLoader current = project.getCoreLoader();
            // ClassLoader mine = this.getClass().getClassLoader();
            // ClassLoader other = project.getClass().getClassLoader();

            // project.setCoreLoader(mine);
            project.setUserProperty(PropertyAntFile, buildFile.getAbsolutePath());
            attachProperties(project);

            DefaultLogger consoleLogger = new DefaultLogger();
            consoleLogger.setErrorPrintStream(System.err);
            consoleLogger.setOutputPrintStream(System.out);
            consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
            project.addBuildListener(consoleLogger);

            try {
                project.fireBuildStarted();
                project.init();

                ProjectHelper helper = ProjectHelper.getProjectHelper();
                project.addReference(PropertyAntProjectHelper, helper);
                helper.parse(project, buildFile);
                project.executeTarget(target);
                project.fireBuildFinished(null);

                return getProperties(project);
            } catch (BuildException e) {
                project.fireBuildFinished(e);
                throw e;
            } catch (Exception ex){
            	// TODO: Fix catchall exception
            	project.fireBuildFinished(ex);
                throw ex;
            }
            
        }

        public void execute() {
            Properties result = null;
            long start;
            long end;

            start = System.currentTimeMillis();

            try {
                attachConfigFile(properties);
                beforeExecute(target, token, properties);
                result = executeTarget();
            } catch (Exception e) {
                if (result == null) {
                    result = new Properties();
                }

                result.setProperty(PropertyTargetResultCode, Integer.toString(ResultCodeException));
                result.setProperty(PropertyExceptionMessage, e.getMessage());
                result.setProperty(PropertyExceptionStack, getStackTraceString(e.getStackTrace()));
            }

            String exitCode = result.getProperty(PropertyTargetResultCode);
            String exitCodeMessage = result.getProperty(PropertyTargetResultCodeMessage);

            if (exitCode == null) {
                result.setProperty(PropertyTargetResultCode, Integer.toString(ResultCodeOK));
            }

            if (exitCodeMessage != null) {
                result.setProperty(PropertyTargetResultCodeMessage, exitCodeMessage);
            } else {
                result.setProperty(PropertyTargetResultCodeMessage, "Unknown:  handler did not report message");
            }

            result.setProperty(PropertyConfigurationProperties, properties.toString());
            result.setProperty(PropertyTargetName, target);

            // if (properties.getProperty("host.net.ip") != null) {
            // result.setProperty("host.net.ip",
            // properties.getProperty("host.net.ip"));
            // }

            end = System.currentTimeMillis();
            logger.debug("target: " + target + " dur(ms)=" + (end - start) + 
            		" exit code: " + result.getProperty(PropertyTargetResultCode) + 
            		" message: " + result.getProperty(PropertyTargetResultCodeMessage));

            result.setProperty(PropertyStartTime, Long.toString(start));
            result.setProperty(PropertyEndTime, Long.toString(end));
            // pass the configuration sequence number back
            PropList.setProperty(result, Config.PropertyActionSequenceNumber, properties.getProperty(Config.PropertyActionSequenceNumber));

            // pass the resource type if present
            String rtype = properties.getProperty(PropertyResourceType);
            if (rtype != null) {
                result.setProperty(Config.PropertyResourceType, rtype);
            }
            
            // required for passing in the modify sequence number so that Substrate:processModifyComplete() 
            // can populate the index for code/message for the modify actions
            String modifySeqNum = properties.getProperty(Config.PropertyModifySequenceNumber);
            if(modifySeqNum != null){
            	result.setProperty(Config.PropertyModifySequenceNumber, modifySeqNum);
            }
            
            plugin.configurationComplete(token, result);
        }
    }

    class ExecuteTask extends Thread {
        public ExecuteTask() {
            this.setContextClassLoader(this.getClass().getClassLoader());
            this.setDaemon(true);
        }

        private void execute(RunConfig task) {
            task.execute();
        }

        public void run() {
            RunConfig task = null;

            try {
                while (go) {
                    synchronized (lock) {
                        while (size == 0) {
                            lock.wait();
                        }

                        task = tasks.removeFirst();
                        size--;
                        lock.notify();
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Executing ant driver: " + task.getClass().toString());
                    }

                    execute(task);
                    task.setFinished();
                }
            } catch (Exception e) {
                logger.error("run", e);
            }
        }
    }

}
