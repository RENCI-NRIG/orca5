package net.exogeni.orca.policy.core.util;

import java.util.Properties;

import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.util.ResourceType;

public abstract class InventoryForType {
    protected ResourceType type;
    protected Properties properties;
    protected IClientReservation source;
    protected ResourcePoolDescriptor rpd;

    public InventoryForType() {
        properties = new Properties();
    }

    public ResourceType getType() {
        return type;
    }

    protected void setType(ResourceType type) {
        this.type = type;
    }

    public void donate(IClientReservation source) {
        if (this.source != null) {
            throw new IllegalStateException("This inventory pool already has a source.");
        }
        this.source = source;
    }

    public IClientReservation getSource() {
        return source;
    }

    protected void setDescriptor(ResourcePoolDescriptor rpd) {
        this.rpd = rpd;
        rpd.save(properties, null);
    }

    public ResourcePoolDescriptor getDescriptor() {
        return rpd;
    }

    public Properties getProperties() {
        return properties;
    }

    /**
     * Allocates the specified number of units given the client request
     * properties. This method is called for new ticketing reservations.
     * @param count how many units to allocate
     * @param request request properties
     * @return the resource properties to be passed back to the client
     */
    public abstract Properties allocate(int count, Properties request);

    /**
     * Allocates the specified number of units given the client request
     * properties and the current resource allocation. This method is called for
     * extending reservations.
     * @param count how many new units to allocate
     * @param request what the client wants
     * @param resource what is currently allocated
     * @return the new resource properties to be associated with the reservation
     *         and passed back to the client
     */
    public abstract Properties allocate(int count, Properties request, Properties resource);

    /**
     * Called during revisit to indicate that a ticketed reservation is being
     * recovered.
     * @param count number of units
     * @param resource resource properties
     */
    public abstract void allocateRevisit(int count, Properties resource);

    /**
     * Frees the specified number of resource units.
     * @param count number of units
     * @param request request properties
     * @param resource resource properties
     * @return new resource properties
     */
    public abstract Properties free(int count, Properties request, Properties resource);

    /**
     * Frees the specified number of units.
     * @param count count
     * @param resources resource properties
     */
    public abstract void free(int count, Properties resources);

    /**
     * Returns the number of free units in the inventory pool.
     * @return number of free units
     */
    public abstract int getFree();

    /**
     * Returns the number of allocated units from this inventory pool.
     * @return number of allocated units
     */
    public abstract int getAllocated();
}
