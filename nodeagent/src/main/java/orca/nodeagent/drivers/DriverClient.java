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

import orca.drivers.DriverId;
import orca.nodeagent.NodeAgentServiceStub;
import orca.tools.axis2.Axis2ClientConfigurationManager;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.log4j.Logger;


/**
 * <code>DriverClient</code> is the base class for all driver client
 * implementations.
 */
public class DriverClient
{
    /**
     * Location of node agent service.
     */
    protected String location = null;

    /**
     * Path to local axis2 repository.
     */
    protected String repository = null;

    /**
     * Path to local axis2.xml.
     */
    protected String config = null;

    /**
     * Driver identifier.
     */
    protected DriverId id;

    /**
     * Node Agent service stub.
     */
    private NodeAgentServiceStub stub;
    protected Logger logger;

    /**
     * Creates a new driver client.
     * @param id driver identifier
     * @param location node agent service location
     * @param repository axis2 repository location
     * @param config axis2 configuration file
     */
    public DriverClient(DriverId id, String location, String repository, String config)
    {
        if ((id == null) || (location == null)) {
            throw new IllegalArgumentException();
        }

        this.id = id;
        this.location = location;
        this.repository = repository;
        this.config = config;
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    /**
     * Creates a new driver client.
     * @param id driver identifier
     * @param stub node agent service client stub
     */
    public DriverClient(DriverId id, NodeAgentServiceStub stub)
    {
        if ((id == null) || (stub == null)) {
            throw new IllegalArgumentException();
        }

        this.id = id;
        this.stub = stub;
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    /**
     * Returns a stub for communicating with the node agent service.
     * @return node agent service stub
     * @throws Exception
     */
    protected synchronized NodeAgentServiceStub getStub() throws Exception
    {
        if (stub == null) {
            // if (repository != null || config != null) {
            // ConfigurationContext cc =
            // ConfigurationContextFactory.createConfigurationContextFromFileSystem(repository,
            // config);
            // stub = new NodeAgentServiceStub(cc, location);
            // } else {
            // stub = new NodeAgentServiceStub(location);
            // }
            
            message("Creating axis2 configuration context: repository=" + repository + " config=" + config);
            
            ConfigurationContext context = Axis2ClientConfigurationManager.getInstance().getContext(
                repository,
                config);
            message("Creating service stub: location=" + location);
            stub = new NodeAgentServiceStub(context, location);
        }

        return stub;
    }
    
    protected void reportError(Exception e){
        // print it first
        System.err.println("An error occurred: " + e.getMessage());
        e.printStackTrace();
        logger.error(e);
    }
    
    protected void message(String message) {
        logger.debug(message);
        System.out.println(message);
    }
}