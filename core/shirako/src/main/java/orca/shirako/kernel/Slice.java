/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Iterator;
import java.util.Properties;

import orca.security.AuthToken;
import orca.security.Guard;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceData;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

/**
 * Slice implementation. A slice has a globally unique identifier, name,
 * description, property list, an owning identity, an access control list, and a
 * set of reservations.
 * <p>
 * This class is used within the Service Manager, which may hold reservations on
 * many sites; on the Broker, which may have provided tickets to the slice for
 * reservations at many sites; and on the site Authority, where each slice may
 * hold multiple reservations for resources at that site.
 * </p>
 */
class Slice implements IKernelSlice, Cloneable {
    /*
     * Serialization constants.
     */
    public static final String PropertyDescription = "SliceDescription";
    public static final String PropertyOwner = "SliceOwner";
    public static final String PropertyGuard = "SliceGuard";
    public static final String PropertyProperties = "SliceProperties";

    /**
     * Globally unique identifier.
     */
    @Persistent(key = PropertyGuid)
    protected SliceID guid;

    /**
     * Slice name. Not required to be globally or locally unique.
     */
    @Persistent(key = PropertyName)
    protected String slicename;

    /**
     * Description string. Has only local meaning.
     */
    @Persistent(key = PropertyDescription)
    protected String description;

    /**
     * A collection of property lists inherited by each reservation in this
     * slice. Properties defined on the reservation level override properties
     * defined here.
     */
    @Persistent(key = PropertyProperties)
    protected ResourceData rsrcdata;

    /**
     * The slice type: inventory or client.
     */
    @Persistent(key = PropertyType)
    protected String type = SliceTypes.ClientSlice;

    /**
     * The owner of the slice.
     */
    @Persistent(key = PropertyOwner)
    protected AuthToken owner;

    /**
     * Access control monitor.
     */
    @Persistent(key = PropertyGuard)
    protected Guard guard;

    /**
     * Resource type associated with this slice. Used when the slice is used to
     * represent an inventory pool.
     */
    @Persistent(key = PropertyResourceType)
    protected ResourceType resourceType;

    /**
     * The reservations in this slice.
     */
    @NotPersistent
    protected ReservationSet reservations;

    /*
     * =======================================================================
     * Construction and initialization
     * =======================================================================
     */

    /**
     * Creates a new "blank" instance. Should be used primarily for recovery
     * purposes.
     */

    public Slice() {
        guard = new Guard();
        reservations = new ReservationSet();
    }

    /**
     * Creates a new slice with the given identifier.
     * 
     * @param id
     *            slice identifier
     * @param name
     *            slice name
     */
    public Slice(SliceID id, String name) {
        clear();
        guid = id;
        slicename = name;
        rsrcdata = new ResourceData();
        guard = new Guard();
        reservations = new ReservationSet();
    }

    /**
     * Create a new slice with the given name and properties.
     * 
     * @param id
     *            slice identifier
     * @param name
     *            slice name
     * @param rdata
     *            properties
     */
    public Slice(SliceID id, String name, ResourceData rdata) {
        clear();
        guid = id;
        slicename = name;
        rsrcdata = rdata;
        guard = new Guard();
        reservations = new ReservationSet();
    }

    /**
     * Creates a new slice with the given name. Generates a new slice
     * identifier.
     * 
     * @param name
     *            slice name
     */
    public Slice(String name) {
        this(new SliceID(), name);
    }

    /**
     * Create a new slice with the given name and properties. Generates a new
     * slice identifier.
     * 
     * @param name
     *            slice name
     * @param rdata
     *            properties
     */
    public Slice(String name, ResourceData rdata) {
        this(new SliceID(), name, rdata);
    }

    /**
     * Clear the slice
     */
    private void clear() {
        slicename = "unspecified";
        description = "no description";
        reservations = new ReservationSet();
        rsrcdata = null;
        owner = null;
    }

    /*
     * =======================================================================
     * Miscellaneous
     * =======================================================================
     */
    public ISlice cloneRequest() {
        Slice result = new Slice();
        result.slicename = this.slicename;
        result.guid = this.guid;

        return result;
    }

    public Properties getConfigurationProperties() {
        return rsrcdata.getConfigurationProperties();
    }

    public String getDescription() {
        return this.description;
    }

    public Guard getGuard() {
        return this.guard;
    }

    /*
     * =======================================================================
     * Slice properties. Inherited by all reservations in the slice.
     * Reservations can override the slice properties.
     * =======================================================================
     */
    public Properties getLocalProperties() {
        return this.rsrcdata.getLocalProperties();
    }

    /*
     * =======================================================================
     * Kernel functions
     * =======================================================================
     */
    public String getName() {
        return slicename;
    }

    public AuthToken getOwner() {
        return this.owner;
    }

    public ResourceData getProperties() {
        return rsrcdata;
    }

    public Properties getRequestProperties() {
        return this.rsrcdata.getRequestProperties();
    }

    /**
     * {@inheritDoc}
     */
    public ReservationSet getReservations() {
        return reservations;
    }

    /**
     * {@inheritDoc}
     */
    public IKernelReservation[] getReservationsArray() {
        IKernelReservation[] result = new IKernelReservation[reservations.size()];
        Iterator<IReservation> iter = reservations.iterator();
        int index = 0;

        while (iter.hasNext()) {
            result[index++] = (IKernelReservation) iter.next();
        }

        return result;
    }

    public Properties getResourceProperties() {
        return this.rsrcdata.getResourceProperties();
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public SliceID getSliceID() {
        return guid;
    }

    public boolean isBrokerClient() {
        return type.equals(SliceTypes.BrokerClientSlice);
    }

    public boolean isClient() {
        return !isInventory();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return reservations.isEmpty();
    }

    public boolean isInventory() {
        return (type.equals(SliceTypes.InventorySlice));
    }

    public void myMethod() {
    }

    /**
     * {@inheritDoc}
     */
    public void prepare() throws OrcaException {
        /*
         * when registering a new slice we must make sure the slice does not
         * contain stale state.
         */
        reservations.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void register(final IKernelReservation r) throws Exception {
        if (reservations.contains(r.getReservationID())) {
            throw new Exception("Reservation " + r.getReservationID().toHashString()
                    + " already added in slice");
        }

        reservations.add(r);
    }

    public void setBrokerClient() {
        this.type = SliceTypes.BrokerClientSlice;
    }

    public void setClient() {
        this.type = SliceTypes.ClientSlice;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGuard(Guard g) {
        this.guard = g;
    }

    /*
     * =======================================================================
     * Getter and Setters
     * =======================================================================
     */
    public void setInventory(boolean value) {
        if (value) {
            this.type = SliceTypes.InventorySlice;
        } else {
            this.type = SliceTypes.ClientSlice;
        }
    }

    public void setName(String name) {
        this.slicename = name;
    }

    public void setOwner(AuthToken auth) {
        this.owner = auth;
        this.getGuard().setOwner(auth);
        this.getGuard().setObjectId(this.guid);
    }

    public void setProperties(ResourceData rsrcdata) {
        this.rsrcdata = rsrcdata;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * {@inheritDoc}
     */
    public IKernelReservation softLookup(final ReservationID rid) {
        return (IKernelReservation) reservations.get(rid);
    }

    @Override
    public String toString() {
        assert slicename != null;

        String s = slicename + "(" + guid.toHashString() + ")";

        return s;
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(final IKernelReservation r) {
        reservations.remove(r);
    }

    public ID getReference() {
        return guid;
    }
}