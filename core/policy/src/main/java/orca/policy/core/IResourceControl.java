/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.policy.core;

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.ConfigToken;
import orca.util.ID;
import orca.util.Initializable;
import orca.util.ResourceType;
import orca.util.persistence.Recoverable;
import orca.util.persistence.Persistable;

/**
 * Interface for authority policy resource control implementations. An authority
 * policy organizes the authorities inventory into a number of resource pools.
 * Each resource pool is associated with a resource control object responsible
 * for servicing requests for resources for the resource pool.
 */
public interface IResourceControl extends Initializable, Persistable, Recoverable
{
    /**
     * Informs the control about a source ticket. Depending on the control
     * implementation, the control may choose to treat each source ticket as
     * inventory.
     * @param r
     * @throws Exception
     */
    public void donate(IClientReservation r) throws Exception;

    /**
     * Informs the control about physical inventory.
     * @param set set of inventory resources (inventory units)
     * @throws Exception
     */
    public void donate(ResourceSet set) throws Exception;

    /**
     * Forcefully removes the specified resources from the control's inventory.
     * @param rset set of resources to eject from the inventory (inventory
     *            units)
     * @throws Exception
     */
    public void eject(ResourceSet rset) throws Exception;

    /**
     * Releases previously allocated resources. Note that some of those
     * resources may represent failed resource units. Failed units should not be
     * released. Once the failure is corrected (by an entity external to the
     * control), the control will be notified through
     * {@link #freed(ResourceSet)}.
     * @param resources set of released resources (allocated units)
     * @throws Exception
     */
    public void release(ResourceSet resources) throws Exception;

    /**
     * Notifies the control that inventory resources are now available for use.
     * @param rset inventory resources that have become available (inventory units)
     * @throws Exception
     */
    public void available(ResourceSet rset) throws Exception;

    /**
     * Indicates that some inventory resources should be marked as unavailable.
     * @param rset resources (inventory units)
     * @return -1 if at least one resource unit is currently in use; 0 otherwise
     * @throws Exception
     */
    public int unavailable(ResourceSet rset) throws Exception;

    /**
     * Indicates that previously committed resources have been freed. Most
     * likely these resources represent failed allocations.
     * @param set set of freed resources (allocated units)
     * @throws Exception
     */
    public void freed(ResourceSet set) throws Exception;

    /**
     * Notifies the control that some inventory resources have failed and cannot
     * be used to satisfy client requests.
     * @param set set of failed resources (inventory units)
     * @throws Exception
     */
    public void failed(ResourceSet set) throws Exception;

    /**
     * Notifies the policy that previously failed resources have been recovered
     * and can be safely used to satisfy client requests.
     * @param set set of recovered resources
     * @throws Exception
     */
    public void recovered(ResourceSet set) throws Exception;

    /**
     * Assigns resources to the reservation.
     * @param reservation reservation
     * @return resources assigned to the reservation (allocated units)
     * @throws Exception
     */
    public ResourceSet assign(IAuthorityReservation reservation) throws Exception;

    /**
     * Informs the control that a reservation with previously allocated
     * resources has a deficit. The deficit may be positive or negative. The
     * control must decide how to handle the deficit. For example, the control
     * may try allocating new resources.
     * <p>
     * This method may be invoked multiple times. The core will invoke either
     * until the deficit is corrected or the control indicates that the
     * reservation must be sent back to the client even though it has a deficit.
     * @param reservation reservation with a deficit
     * @return resources allocated/removed so that the deficit is corrected (allocated units)
     * @throws Exception
     */
    public ResourceSet correctDeficit(IAuthorityReservation reservation) throws Exception;

    /**
     * Notifies the control that a reservation is about to be closed.
     * This method will be invoked for every reservation that is about to be
     * closed, even if the close was triggered by the control itself. The
     * policy should update its internal state/cancel pending operations
     * associated with the reservation. This method is invoked with the kernel
     * lock on.
     *
     * @param reservation reservation about to close
     */
    public void close(IReservation reservation);

    /**
     * Notifies the control that recovery is starting.
     */
    public void recoveryStarting();
    
    /**
     * Recovers state for a reservation.
     * @param r reservation
     * @throws Exception
     */
    public void revisit(IReservation r) throws Exception;

    /**
     * Notifies the control that recovery has completed.
     */
    public void recoveryEnded();
    
    /**
     * Notifies the control that a configuration action for the object
     * represented by the token parameter has completed.
     * 
     * @param action configuration action. See Config.Target*
     * @param token object or a token for the object whose configuration action has completed
     * @param outProperties output properties produced by the configuration action
     */
    public void configurationComplete(String action, ConfigToken token, Properties outProperties);

    /**
     * Returns the control guid. Every control must have a unique guid.
     * @return resource control guid
     */
    public ID getGuid();

    // FIXME: revisit the various Type-related methods. Their semantics seem unclear.
    
    /**
     * Returns the resource types this resource control handles. A resource
     * control can be responsible for more than one resource type, although
     * usually the mapping will be one-to-one.
     * @return list of resource types
     */
    public ResourceType[] getTypes();

    /**
     * Instructs the control the handle the specified type.
     * @param type
     */
    public void addType(ResourceType type);
    
    /**
     * Informs the control that it no longer handles the specified type.
     * @param type
     */
    public void removeType(ResourceType type);
    
    /**
     * Registers the specified resource type with the resource control.
     * FIXME: what is the relationship with addType. Should there be unregister
     * @param type
     */
    public void registerType(ResourceType type);

    /**
     * Sets the actor the control is associated with.
     * @param actor actor
     */
    public void setActor(IActor actor);
}
