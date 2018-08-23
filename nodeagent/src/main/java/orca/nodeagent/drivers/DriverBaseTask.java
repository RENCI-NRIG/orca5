/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.drivers;

import java.util.Properties;

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.client.AntBaseTask;
import orca.tools.axis2.Axis2ClientConfigurationManager;
import orca.tools.axis2.Axis2ClientSecurityConfigurator;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.tools.ant.BuildException;

public abstract class DriverBaseTask extends AntBaseTask {
    /**
     * Controls whether client stubs should be created with security turned on or off. Possible values: yes|no. If axis2
     * security is set then the task will read the "root.dir" and "actor.id" properties. If these properties are
     * defined, the task will try to create a configuration context with the security settings for the given actor.
     */
    public static final String PropertyAxis2Security = "axis2.security";
    public static final String PropertyActorID = "actor.id";

    /**
     * Location of the service
     */
    protected String location;

    /**
     * Path to repository (local/optional)
     */
    protected String repository;

    /**
     * Path to axis2.xml (local/optional)
     */
    protected String config;
    protected String driverId;

    // protected String serviceIP;

    /**
     * @param driverId
     *            the driverId to set
     */
    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    /**
     * Sets the service location
     * 
     * @param location location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets the path to the local repository
     * 
     * @param repository repository
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * Sets the path to the local axis2.xml
     * 
     * @param config config
     */
    public void setConfig(String config) {
        this.config = config;
    }

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {
            // resolveSecurity();
        } catch (Exception e) {
            logger.error("", e);
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    protected void resolveSecurity() throws Exception {
        String actorID = null;
        String rootDir = null;

        if (config == null) {
            String sec = getProject().getProperty(PropertyAxis2Security);

            if ((sec != null) && sec.equalsIgnoreCase("yes")) {
                System.out.println("fixing security");
                /* we want security. check if root.dir and actor.id are defined */
                actorID = getProject().getProperty(PropertyActorID);
                rootDir = getProject().getProperty(PropertyRootPath);

                if ((actorID != null) && (rootDir != null)) {
                    System.out.println("trying to set security");

                    Axis2ClientSecurityConfigurator configurator = Axis2ClientSecurityConfigurator.getInstance();
                    config = configurator.getAxis2ConfigPath(rootDir, actorID);
                    System.out.println("config: " + config);
                }
            }
        }
    }

    /**
     * Returns a service stub
     * 
     * @return NodeAgentServiceStub
     * @throws Exception in case of error
     */
    protected NodeAgentServiceStub getStub() throws Exception {
        // if (repository != null || config != null) {
        // ConfigurationContext cc =
        // ConfigurationContextFactory.createConfigurationContextFromFileSystem(repository,
        // config);
        // return new NodeAgentServiceStub(cc, location);
        // } else {
        // return new NodeAgentServiceStub(location);
        // }
        message("Creating axis2 configuration context: repository=" + repository + " config=" + config);

        ConfigurationContext context = Axis2ClientConfigurationManager.getInstance().getContext(repository, config);

        message("Creating service stub: location=" + location);
        return new NodeAgentServiceStub(context, location);
    }

    public Properties getProperties() throws Exception {
        return new Properties();
    }

    protected void message(String message) {
        logger.debug(message);
        System.out.println(message);
    }

}
