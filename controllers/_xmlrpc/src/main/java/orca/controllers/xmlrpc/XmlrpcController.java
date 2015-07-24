/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.controllers.xmlrpc;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import orca.controllers.xmlrpc.geni.GeniAmV1Handler;
import orca.policy.core.ServiceManagerCalendarPolicy;
import orca.shirako.api.IActor;
import orca.shirako.api.IController;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationState;
import orca.shirako.container.ContainerConstants;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaXmlrpcServlet;
import orca.shirako.core.EventHandler;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.webserver.WebServer;

public class XmlrpcController implements IController
{

    public static long ClockSkew = 1;
    public static final String ServiceName = "xmlrpcService";
    public static final String XmlrpcControllerPropertiesFile="xmlrpc.controller.properties";
    
    protected IServiceManager sm;
    protected Logger logger = null;
    protected ISlice slice;
    protected String root;
    private boolean initialized = false;
    protected boolean controllerEnabled = true;

    protected WebServer webserver;
    protected OrcaXmlrpcHandler handler;
    protected static Properties controllerProperties = null;
    
    public XmlrpcController()
    {
        super();
    }

    public synchronized void initialize() throws Exception
    {
        if (!initialized) {
            if (sm == null) {
                throw new Exception("Missing actor");
            }

            if (slice == null) {
                throw new Exception("Missing slice");
            }

            logger = sm.getLogger();
            
            root = (Globals.getContainer()).getPackageRootFolder(XmlrpcControllerConstants.PackageId);

            // Register an EventHandler to track lease state changes
            ((ServiceManagerCalendarPolicy) sm.getPolicy()).register(new XmlrpcEventHandler(logger));

            // Store controller state so that xml-rpc handler has access to it
            XmlrpcOrcaState instance = XmlrpcOrcaState.getInstance();
            instance.setSM(sm);
            instance.setController(this);
            instance.setSlice(slice);

            // load properties file configuring controller behavior
            controllerProperties = new Properties();
        	logger.info("Checking for " + Globals.RootDirectory + ContainerConstants.ConfigDir + XmlrpcControllerPropertiesFile);
        	
        	File f = new File(Globals.RootDirectory + ContainerConstants.ConfigDir + XmlrpcControllerPropertiesFile);
        	if (f.exists()) {
        		logger.info("Succeeded, loading configuration");
        		controllerProperties.load(new FileInputStream(f));
        	} else 
        		logger.info("No XMLRPC controller configuration found, defaults will be used");
            
            // register XMLRPC handler(s) with ORCA
            logger.info("Adding XMLRPC Orca handler to global list (namespace 'orca')");
            OrcaXmlrpcServlet.addXmlrpcHandler("orca", OrcaXmlrpcHandler.class, false);
            logger.info("Adding XMLRPC GENI AM v1 handler to global list (default namespace 'geni')");
            OrcaXmlrpcServlet.addXmlrpcHandler("geni", GeniAmV1Handler.class, true);
            
            initialized = true;
        }
    }

    // to be sure this is read-only
    public static String getProperty(String p) {
    	if (controllerProperties != null)
    		return controllerProperties.getProperty(p);
    	return null;
    }
    
    public synchronized void disableController()
    {
        this.controllerEnabled = false;
    }

    public synchronized void enableController()
    {
        this.controllerEnabled = true;
    }

    public synchronized boolean isControllerEnabled()
    {
        return this.controllerEnabled;
    }

    public ISlice getSlice()
    {
        return slice;
    }

    public void setSlice(ISlice slice)
    {
        this.slice = slice;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public void setActor(IActor actor)
    {
        this.sm = (IServiceManager) actor;
    }
    
    public void reset(Properties properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Properties save() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void save(Properties properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void tick(long cycle)
    {
        
    }


    protected class XmlrpcEventHandler extends EventHandler
    {
    	Logger logger;
    	
    	public XmlrpcEventHandler(Logger l) {
    		logger = l;
    	}
    	
        @Override
        public void initialize(IServiceManager s) {
            super.initialize(s);
        }

        @Override
        public void onBeforeExtendTicket(IReservation r) {

            //System.out.println("***** Inside onBeforeExtendTicket");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onBeforeExtendTicket *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onBeforeExtendTicket(r);

        }

        @Override
        public void onClose(IReservation r) {

            //System.out.println("***** Inside onClose");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onClose *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onClose(r);

        }

        @Override
        public void onCloseComplete(IReservation r) {

            //System.out.println("***** Inside onCloseComplete");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onCloseComplete *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onCloseComplete(r);

        }

        @Override
        public void onExtendLease(IReservation r) {

            //System.out.println("***** Inside onExtendLease");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onExtendLease *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onExtendLease(r);

        }

        @Override
        public void onExtendLeaseComplete(IReservation r) {

            //System.out.println("***** Inside onExtendLeaseComplete");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onExtendLeaseComplete *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onExtendLeaseComplete(r);

        }

        @Override
        public void onExtendTicket(IReservation r) {

            //System.out.println("***** Inside onExtendTicket");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onExtendTicket *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onExtendTicket(r);

        }

        @Override
        public void onExtendTicketComplete(IReservation r) {

            //System.out.println("***** Inside onExtendTicketComplete");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onExtendTicketComplete *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onExtendTicketComplete(r);

        }

        @Override
        public void onLease(IReservation r) {

            //System.out.println("***** Inside onLease");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onLease *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());


            super.onLease(r);
        }

        @Override
        public void onLeaseComplete(IReservation r) {

            //System.out.println("***** Inside onLeaseComplete");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onLeaseComplete *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onLeaseComplete(r);
        }

        @Override
        public void onTicket(IReservation r) {

            //System.out.println("***** Inside onTicket");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onTicket *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onTicket(r);
        }

        @Override
        public void onTicketComplete(IReservation r) {

            //System.out.println("***** Inside onTicketComplete");

            IServiceManagerReservation cr = (IServiceManagerReservation) r;
            ReservationState resState = cr.getReservationState();

            logger.debug("*** onTicketComplete *** | resGUID: " + cr.getReservationID() + " | State: " + resState.getStateName() + " | Resource Type: " + cr.getApprovedType() + " | Units: " + cr.getApprovedUnits());
            //System.out.println("Current overall reservation state is " + resState.getStateName());
            //System.out.println("Current joining reservation state is " + resState.getJoiningName());
            //System.out.println("Current pending reservation state is " + resState.getPendingName());

            super.onTicketComplete(r);
        }

    }

}
