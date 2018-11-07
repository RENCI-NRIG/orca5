package orca.shirako.proxies;

import orca.shirako.api.IActor;
import orca.shirako.container.ProtocolDescriptor;

/**
 * <code>IServiceFactor</code> defines the interface for a
 * factory class responsible for deploying/undeploying the services 
 * required to support a given inter-actor communication protocol.
 */
public interface IServiceFactory
{
    /**
     * Deploys the services for the actor.
     * @param actor actor
     * @param protocol protocol
     * @throws Exception in case of error
     */
    public void deploy(ProtocolDescriptor protocol, IActor actor) throws Exception;
    
    /**
     * Undeploys the services for the actor.
     * @param actor actor
     * @param protocol protocol
     * @throws Exception in case of error
     */
    public void undeploy(ProtocolDescriptor protocol, IActor actor) throws Exception;
}
