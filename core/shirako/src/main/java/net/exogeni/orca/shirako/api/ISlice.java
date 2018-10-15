/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.security.Guard;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.Recoverable;
import net.exogeni.orca.util.persistence.Referenceable;


/**
 * <code>ISlice</code> describes the programming interface to a slice object.
 * Each slice has a name (not necessarily unique) and a globally unique
 * identifier.
 * <p>
 * Slices are used to organize groups of reservations. Each reservation belongs
 * to exactly one slice. Slices information is passed to upstream actors as part
 * of ticket and lease requests. There are several slice types:
 * <ul>
 * <li> inventory slices are used to organized reservations that represent an
 * inventory. For example, allocated resources, or resources to be used to
 * satisfy client requests</li>
 * <li> client slices are used on server actors to group reservations
 * representing client requests (allocated/assigned resources).</li>
 * <li> broker client slices - are client slices that represent the requests
 * from a broker that acts as a client of the containing actor</li>
 * </ul>
 * <p>
 * Each slice contains a number of properties lists, which can be used to store
 * properties applicable to all reservations associated with the slice.
 * Properties defined in the slice are automatically inherited by reservations.
 * Each reservation can also override a property inherited by the slice, but
 * defining it in its appropriate properties list.
 * <p>
 * Slices within a service manager can be associated with an optional
 * controller. The controller can represent a specific application and the slice
 * will contain the reservations that belong to the application.
 */
public interface ISlice extends Persistable, Recoverable, Referenceable
{
    /**
     * Serialization property name: slice GUID.
     */
    public static final String PropertyGuid = "SliceGuid";

    /**
     * Serialization property name: slice type.
     */
    public static final String PropertyType = "SliceType";

    /**
     * Serialization property name: slice name.
     */
    public static final String PropertyName = "SliceName";

    /**
     * Serialization property name: slice resource type.
     */
    public static final String PropertyResourceType = "SliceResourceType";

    /**
     * Makes a minimal clone of the slice object sufficient for
     * cross-actor calls.
     *
     * @return a slice object to use when making cross-actor calls.
     */
    public ISlice cloneRequest();

    /**
     * Returns the slice configuration properties list (by reference).
     *
     * @return configuration properties list
     */
    public Properties getConfigurationProperties();

    /**
     * Returns the slice description.
     *
     * @return slice description
     */
    public String getDescription();

    /**
     * Returns the slice guard.
     *
     * @return the guard
     */
    public Guard getGuard();

    /**
     * Returns the slice local properties list (by reference).
     *
     * @return local properties list
     */
    public Properties getLocalProperties();

    /**
     * Returns the slice name.
     *
     * @return slice name
     */
    public String getName();

    /**
     * Returns the slice owner.
     *
     * @return slice owner
     */
    public AuthToken getOwner();

    /**
     * Returns the slice properties (by reference).
     *
     * @return slice properties
     */
    public ResourceData getProperties();

    /**
     * Returns the slice request properties list (by reference).
     *
     * @return request properties list
     */
    public Properties getRequestProperties();

    /**
     * Returns the slice resource properties list (by reference).
     *
     * @return resource properties list
     */
    public Properties getResourceProperties();

    /**
     * Returns the resource type of the slice (if any).
     *
     * @return slice resource type
     */
    public ResourceType getResourceType();

    /**
     * Returns the slice identifier.
     *
     * @return slice identifier
     */
    public SliceID getSliceID();

    /**
     * Checks if the slice is a broker client slice (a client slice
     * within an authority that represents a broker).
     *
     * @return true if the slice is a broker client slice
     */
    public boolean isBrokerClient();

    /**
     * Checks if the slice is a client slice.
     *
     * @return true if the slice is a client slice
     */
    public boolean isClient();

    /**
     * Checks if the slice is an inventory slice.
     *
     * @return true if the slice is an inventory slice
     */
    public boolean isInventory();

    /**
     * Marks the slice as a broker client slice (a client slice within
     * an authority that represents a broker).
     */
    public void setBrokerClient();

    /**
     * Marks the slice as a client slice.
     */
    public void setClient();

    /**
     * Sets the slice description.
     *
     * @param description the description
     */
    public void setDescription(String description);

    /**
     * Sets the slice guard.
     *
     * @param g the guard
     */
    public void setGuard(Guard g);

    /**
     * Sets the inventory flag.
     *
     * @param value inventory status: true, inventory slice, false, client
     *        slice
     */
    public void setInventory(boolean value);

    /**
     * Sets the slice name.
     *
     * @param name slice name to set
     */
    public void setName(String name);

    /**
     * Sets the slice owner.
     *
     * @param auth the slice owner
     */
    public void setOwner(AuthToken auth);

    /**
     * Sets the slice properties.
     *
     * @param properties slice properties
     */
    public void setProperties(ResourceData properties);

    /**
     * Sets the slice resource type.
     *
     * @param resourceType resource type
     */
    public void setResourceType(ResourceType resourceType);
}
