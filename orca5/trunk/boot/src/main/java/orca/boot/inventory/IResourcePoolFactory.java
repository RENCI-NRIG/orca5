package orca.boot.inventory;

import orca.shirako.api.IClientReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.plugins.substrate.ISubstrate;

public interface IResourcePoolFactory {
    /**
     * Sets the substrate.
     * @param substrate
     */
    void setSubstrate(ISubstrate substrate) throws ConfigurationException;
    /**
     * Sets the initial resource pools descriptor.
     * The factory can modify the descriptor as needed.
     * @param desc
     */
    void setDescriptor(ResourcePoolDescriptor desc) throws ConfigurationException;
    
    /**
     * Returns the final pool descriptor.
     * @return
     */
    ResourcePoolDescriptor getDescriptor() throws ConfigurationException;
    
    /**
     * Returns the source reservation for this resource pool.
     * @param slice
     * @return
     * @throws ConfigurationException
     */
    IClientReservation createSourceReservation(ISlice slice) throws ConfigurationException;    
}