package net.exogeni.orca.boot.inventory;

import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.plugins.substrate.ISubstrate;

public interface IResourcePoolFactory {
    /**
     * Sets the substrate.
     * 
     * @param substrate substrate
     * @throws ConfigurationException in case of error
     */
    void setSubstrate(ISubstrate substrate) throws ConfigurationException;

    /**
     * Sets the initial resource pools descriptor. The factory can modify the descriptor as needed.
     * 
     * @param desc desc
     * @throws ConfigurationException in case of error
     */
    void setDescriptor(ResourcePoolDescriptor desc) throws ConfigurationException;

    /**
     * Returns the final pool descriptor.
     * 
     * @return ResourcePoolDescriptor
     * @throws ConfigurationException in case of error
     */
    ResourcePoolDescriptor getDescriptor() throws ConfigurationException;

    /**
     * Returns the source reservation for this resource pool.
     * 
     * @param slice slice
     * @return IClientReservation
     * @throws ConfigurationException in case of error
     */
    IClientReservation createSourceReservation(ISlice slice) throws ConfigurationException;
}
