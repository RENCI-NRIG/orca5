package net.exogeni.orca.shirako.proxies;

import java.util.Hashtable;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.ProtocolDescriptor;
import net.exogeni.orca.shirako.proxies.local.LocalServiceFactory;
import net.exogeni.orca.shirako.proxies.soapaxis2.SoapAxis2ServiceFactory;

/**
 * Factory class responsible for deploying/undeploying actor services.
 */
public class ServiceFactory
{
    private static final ServiceFactory instance = new ServiceFactory();
    
    /**
     * Maps protocol name to factory class.
     */
    private Hashtable<String, IServiceFactory> map;
    
    /**
     * Creates a new instance.
     */
    private ServiceFactory()
    {
        map = new Hashtable<String, IServiceFactory>();
        loadFactories();
    }
    
    private void loadFactories()
    {
        map.put(OrcaConstants.ProtocolLocal, new LocalServiceFactory());
        map.put(OrcaConstants.ProtocolSoapAxis2, new SoapAxis2ServiceFactory());
    }
    
    /**
     * Deploys the actor services for the specified protocol.
     * @param protocol protocol
     * @param actor actor
     * @throws Exception in case of error
     */
    public void deploy(final ProtocolDescriptor protocol, final IActor actor) throws Exception
    {
        IServiceFactory factory = map.get(protocol.getProtocol());
        if (factory != null){
        	Globals.Log.debug("Deploying services for actor " + actor.getName() + " using protocol " + protocol.getProtocol());
            factory.deploy(protocol, actor);
        }else {
        	Globals.Log.debug("No services are available using protocol " + protocol.getProtocol());
        }
    }
    
    /**
     * Undeploys the actor services for the specified protocol.
     * @param protocol protocol
     * @param actor actor
     * @throws Exception in case of error
     */
    public void undeploy(final ProtocolDescriptor protocol, final IActor actor) throws Exception
    {
        IServiceFactory factory = map.get(protocol.getProtocol());
        if (factory != null){
            factory.undeploy(protocol, actor);
        }
    }
    /**
     * Returns the factory instance.
     * @return factory instance
     */
    public static ServiceFactory getInstance()
    {
        return instance;
    }
    
}
