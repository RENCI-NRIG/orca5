/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.proxies.soapaxis2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.api.IBroker;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.ProtocolDescriptor;
import net.exogeni.orca.shirako.proxies.IServiceFactory;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.description.AxisService;

/**
 * Service factory for the SOAP communication protocol.
 */
public class SoapAxis2ServiceFactory implements IServiceFactory
{
    /**
     * Service scope: application.
     */    
    public static final String ScopeApplication = "application";
    /**
     * Location of the broker service descriptor.
     */
    public static final String AxisBrokerServiceDescriptor = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/broker.xml";

    /**
     * Location of the broker service descriptor (ws-security enabled).
     */
    public static final String AxisBrokerServiceDescriptorSecure = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/broker.secure.xml";

    /**
     * Location of the authority service descriptor.
     */
    public static final String AxisAuthorityServiceDescrptor = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/authority.xml";

    /**
     * Location of the authority service descriptor (ws-security enabled).
     */
    public static final String AxisAuthorityServiceDescriptorSecure = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/authority.secure.xml";

    /**
     * Location of the service manager service descriptor.
     */
    public static final String AxisServiceManagerServiceDescrptor = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/servicemanager.xml";

    /**
     * Location of the service manager service descriptor (ws-security enabled).
     */
    public static final String AxisServiceManagerServiceDescrptorSecure = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/servicemanager.secure.xml";

    /**
     * Location of the XSL transformation stylesheet. Used to replace security parameters in the service descriptor.
     */
    public static final String TransformTemplate = "net/exogeni/orca/shirako/proxies/soapaxis2/deployment/transform.xsl";

    /**
     * Axis2 configuration context used by the Axis2 servlet.
     */
    public static ConfigurationContext context = null;


    /**
     * Creates a new instance of the factory.
     */
    public SoapAxis2ServiceFactory()
    {
    }

    /**
     * Creates a deployment descriptor for the actor service.
     * 
     * @param actor actor
     * 
     * @throws Exception in case of error
     * 
     * @return File object pointing to the service descriptor
     */
    public File getDescriptor(IActor actor) throws Exception
    {
        assert actor != null;
        
        String path = null;        
        URL url = null;

        // resolve the correct descriptor depending on the secure-communication flag
        if (Globals.getConfiguration().isSecureActorCommunication()){
            Globals.Log.debug("Using secure communication");
            if (actor instanceof IBroker) {
                path = AxisBrokerServiceDescriptorSecure;
            } else {
                if (actor instanceof IAuthority) {
                    path = AxisAuthorityServiceDescriptorSecure;
                } else {
                    path = AxisServiceManagerServiceDescrptorSecure;
                }
            }
        } else {
            Globals.Log.debug("Not using secure communication");
            if (actor instanceof IBroker) {
                path = AxisBrokerServiceDescriptor;
            } else {
                if (actor instanceof IAuthority) {
                    path = AxisAuthorityServiceDescrptor;
                } else {
                    path = AxisServiceManagerServiceDescrptor;
                }
            }
        }
        
        assert path != null;
        
        // attempt to resolve the descriptor template
        Globals.Log.debug("Resolving: " + path);
        url = this.getClass().getClassLoader().getResource(path);
        if (url == null) {
            throw new RuntimeException("Cannot locate service descriptor: " + path);
        }
        
        // create a new file where we will prepare the descriptor for the service
        File file = File. createTempFile("net.exogeni.orca", "", new File("/tmp"));        
        // to prepare the descriptor we need to resolve the xsl template
        URL transURL = this.getClass().getClassLoader().getResource(TransformTemplate);        
        if (transURL == null){
            throw new RuntimeException("Cannot locate transformer stylesheet: " + TransformTemplate);
        }
        
        // make the transformation
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer trans = factory.newTransformer(new StreamSource(transURL.openStream()));

        // resolve the actor properties file
        String propFile = Globals.getContainer().
                            getAxis2ClientPropertiesRelativePath(actor.getGuid().toString());
        // set the parameter to the transformer
        trans.setParameter("propFile", propFile);
        // perform the transformation
        trans.transform(new StreamSource(url.openStream()), new StreamResult(file.toURI().getPath()));
        
        return file;
    }

    /**
     * {@inheritDoc}
     */
    public void deploy(final ProtocolDescriptor protocol, final IActor actor) throws Exception
    {
        Globals.Log.info("Deploying service for actor " + actor.getName());

        if (Globals.ServletContext == null) {
            Globals.Log.warn("The SOAP protocol is configured but Orca is not running inside a servlet container");
            return;
        }
        

        File file = getDescriptor(actor);
        deployService(actor.getName(), ScopeApplication, new FileInputStream(file));
        //file.delete();
        Globals.Log.info("Deployed service for actor " + actor.getName());
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(final ProtocolDescriptor protocol, final IActor actor) throws Exception
    {
        Globals.Log.info("Undeploying service for actor " + actor.getName());
        undeployService(actor.getName());
        Globals.Log.info("Removed service for actor " + actor.getName());
    }
    
    public static void deployService(String serviceName, String scope, InputStream config) throws Exception
    {
        if (context == null) {
            throw new RuntimeException("Missing context");
        }

        // Load the service
        AxisService service = DeploymentEngine.buildService(config, Thread.currentThread().getContextClassLoader(), context);
        // Service name = actor name
        service.setName(serviceName);
        // application scope
        service.setScope(scope);
        // Register the service
        context.getAxisConfiguration().addService(service);
        // FIXME: it seems that it takes a bit longer for a service to become available
    }
    
    public static void undeployService(String serviceName) throws Exception
    {
        if (context == null) {
            throw new RuntimeException("Missing context");
        }
        context.getAxisConfiguration().removeService(serviceName);
    }
}
