package orca.shirako.proxies.local;

import orca.shirako.api.IActor;
import orca.shirako.container.ProtocolDescriptor;
import orca.shirako.proxies.IServiceFactory;

/**
 * Service factory for the local communication protocol.
 */
public class LocalServiceFactory implements IServiceFactory
{
    /**
     * Creates a new instance of the factory.
     */
    public LocalServiceFactory()
    {
    }
    
    /**
     * {@inheritDoc}
     */
    public void deploy(final ProtocolDescriptor protocol, final IActor actor) throws Exception
    {
        // no-op
    }
    
    /**
     * {@inheritDoc}
     */
    public void undeploy(final ProtocolDescriptor protocol, final IActor actor) throws Exception
    {
        // no-op
    }
}