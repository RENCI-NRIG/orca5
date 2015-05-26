/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Date;
import java.util.Properties;

import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.core.Ticket;
import orca.shirako.time.Term;
import orca.shirako.util.Notice;
import orca.shirako.util.ResourceData;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;
import orca.util.persistence.Recoverable;

/**
 * ResourceSet is an abstract set of resources describing some number of
 * resource units of a given type, e.g., to represent a resource request. A
 * ResourceSet with an attached ConcreteSet is "concrete." The ConcreteSet binds
 * real resources (or a promise) for some or all of the abstract units in the
 * set: for example, a concrete ResourceSet can represent a SHARP ticket or
 * lease. Adding or removing concrete resources does not affect the number of
 * abstract units. If there are fewer concrete units than abstract units, the
 * set has a "deficit".
 * <p>
 * An "elastic" ResourceSet may be filled at less than its full request, and it
 * may change size on extends. An actor may modify an elastic ResourceSet on an
 * active ReservationClient by calling "flex", if there is no pending operation
 * in progress, e.g., in preparation for an elastic extend. This class updates
 * the unit count to match the concrete resources on each reserve or extend (on
 * a server), or update (on a client).
 * <p>
 * Operations on the ConcreteSet through this class may drive probes and state
 * transitions on the underlying resources transferred in and out of the
 * ResourceSet (e.g., node configuration and node reboot for a COD authority, or
 * resource membership changes on a service manager). ConcreteSets are
 * responsible for their own synchronization: calls to ConcreteSet go through
 * pre-op "prepare" or post-op "service" methods in this class, which may block
 * and should not hold any higher-level locks. Most other operations are called
 * through Mapper or the Reservation class with the Manager lock held. <br>
 * <br>
 * <b>Implementation notes</b>
 * <li>The unit count is updated immediately to reflect additions or deletions
 * from the set. Updates to the unit count must occur only in the locked
 * methods. Configuration actions on the ConcreteSet (e.g., as resources join
 * and leave the set) must occur only in unlocked methods (e.g., "service"). A
 * tricky part is flex(), which updates abstract count to reflect a new request:
 * it is unlocked, which could race with an incoming unsolicited lease (which
 * are currently allowed), or with overlapping requests on the same set (which
 * are currently not allowed).
 * <li>ResourceSet was conceived as supporting methods that are independent of
 * context and type of ConcreteSet. That ideal has eroded somewhat, and some key
 * fields and methods are specific to a particular context or role. Someday it
 * may be useful to break this into subclasses.
 * <li>Currently leases are validated only with validateIncoming(). There may be
 * some additional checks to enforce.
 * <li>No changes to ResourceData on merges. Needs thought and documentation. We
 * should remove the properties argument on ConcreteSet.change.
 * <li>The 'null ticket corner case' (see above) is a source of complexity, and
 * should be cleaned up.
 * <li>Calls that "reach around" ResourceSet to the concrete set are
 * discouraged/deprecated.
 */
public class ResourceSet implements Persistable, Recoverable {
    // XXX: not sure if these have to be here!!!
    public static final String ElasticSize = "elasticSize";
    public static final String ElasticTime = "elasticTime";

    public static final String PropertyType = "ResourceSetType";
    public static final String PropertyUnits = "ResourceSetUnits";
    public static final String PropertyProperties = "ResourceSetProperties";
    public static final String PropertyPreviousProperties = "ResourceSetPreviousProperties";
    public static final String PropertyConcreteSet = "ResourceSetConcreteSet";

    /**
     * What type of resources does this set contain. The meaning/assignment of
     * type values is an externally defined convention of interacting actors.
     */
    @Persistent (key = PropertyType)
    protected ResourceType type;

    /**
     * How many units (abstract) the set contains. This count reflects the
     * resources intended or requested for this set. For an active reservation
     * in steady state, the unit count will typically match the number of
     * concrete resources, but it might not match if the resource set is in flux
     * for some reason. For an inventory set, the abstract count reflects the
     * original size of the inventory, independent of any allocations extracted
     * from it.
     */
    @Persistent (key = PropertyUnits)
    protected int units;

    /**
     * Concrete resources.
     */
    @Persistent (key = PropertyConcreteSet)
    protected IConcreteSet resources;

    /**
     * ResourceData is a property list for this ResourceSet. E.g., request
     * attributes, node attributes, etc.
     */
    @Persistent (key = PropertyProperties)
    protected ResourceData properties;

    /**
     * The previous value of the properties list. This is essential for
     * supporting recovery on Authority.
     */
    // XXX: this field does not seem that critical anymore.
    @Persistent (key = PropertyPreviousProperties)
    protected ResourceData previousProperties;

    /**
     * A set of resources recently ejected from the resource set, pending
     * processing, e.g., by a "probe" method.
     */
    @NotPersistent
    protected IConcreteSet released;

    /**
     * A recent update to the concrete resource set, pending processing by a
     * "service" method. Client-side only (i.e., for ticket or lease updates
     * through callback interface).
     */
    @NotPersistent
    protected IConcreteSet updated;

    /**
     * Recent additions of resources to the concrete set, pending processing by
     * a "service" method. Only for authority role only.
     */
    @NotPersistent
    protected IConcreteSet gained;

    /**
     * Recently lost resources pending processing by a "service" method. Only
     * for authority role only.
     */
    @NotPersistent
    protected IConcreteSet lost;

    /**
     * Recently changed resources pending a processing by a "service" method.
     * set.
     */
    @NotPersistent
    protected IConcreteSet modified;

    /**
     * Reservation with which this set is associated.
     */
    @NotPersistent
    protected ReservationID rid;

    @NotPersistent
    private boolean isClosing;
    
    /*
     * =======================================================================
     * Construction and initialization.
     * =======================================================================
     */

    /**
     * Creates a default empty resource set. Used during recovery.
     */
    public ResourceSet() {
        this.units = 0;
        this.type = null;
        resources = null;
        properties = null;
        clean();
    }

    /**
     * Creates a new <code>ResourceSet</code>
     * @param gained a set of gained resources
     * @param lost a set of lost resources
     * @param modified a set of modified resources
     * @param type resource type
     * @param rd resource properties
     */
    public ResourceSet(IConcreteSet gained, IConcreteSet lost, IConcreteSet modified, ResourceType type, ResourceData rdata) {
        this.type = type;
        this.gained = gained;
        this.lost = lost;
        this.modified = modified;
        this.properties = rdata;
    }

    /**
     * Creates an empty resource set with the specified resource type.
     * @param type
     */
    public ResourceSet(ResourceType type) {
        this(null, null, null, type, null);
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
    }

    /**
     * Creates a new <code>ResourceSet</code> using the given concrete
     * resources.
     * @param concrete concrete resources
     * @param type resource type
     */
    public ResourceSet(IConcreteSet concrete, ResourceType type) {
        this(concrete, type, new ResourceData());
    }

    /**
     * Creates a new <code>ResourceSet</code> using the given concrete
     * resources.
     * @param concrete concrete resources
     * @param type resource type
     * @param rdata resource properties
     */
    public ResourceSet(IConcreteSet concrete, ResourceType type, ResourceData rdata) {
        this.units = concrete.getUnits();
        this.type = type;
        resources = concrete;
        properties = rdata;
        clean();
    }

    /**
     * Creates new <code>ResourceSet</code>.
     * @param units number of units
     * @param type resource type
     */
    public ResourceSet(int units, ResourceType type) {
        this(units, type, new ResourceData());
    }

    /**
     * Creates a new <code>ResourceSet</code>.
     * @param units number of units
     * @param type resource type
     * @param rdata resource properties
     */
    public ResourceSet(int units, ResourceType type, ResourceData rdata) {
        this.units = units;
        this.type = type;
        resources = null;
        properties = rdata;
        clean();
    }

    /**
     * Clones the set, but without any of the concrete sets. Used on Authority
     * and Service Manager to create a ResourceSet to hold resources, given a
     * ResourceSet holding a ticket. All properties are preserved (cloned).
     * @return a resources set that is a copy of the current but without any
     *         concrete sets.
     */
    public ResourceSet abstractClone() {
        ResourceData rd = (ResourceData) properties.clone();

        return new ResourceSet(units, type, rd);
    }

    /**
     * Cleans the set.
     */
    private void clean() {
        updated = null;
        released = null;
        gained = null;
        lost = null;
        modified = null;
    }

    /*
     * =======================================================================
     * Cloning
     * =======================================================================
     */
    public ResourceSet clone() {
        ResourceData rd = (ResourceData) properties.clone();
        ResourceSet clone = new ResourceSet(units, type, rd);
        clone.resources = resources.clone();

        return clone;
    }

    /**
     * Any units that fail or are rejected/released asynchronously accumulate
     * within the ConcreteSet until collected. These are cached by a
     * prepareProbe.
     * @return a ResourceSet
     */

    public ResourceSet collectReleased() throws Exception {
        ResourceSet rset = null;
        if (released != null) {
            /*
             * We can safely ignore all properties here. These resources are
             * going to be merged into an inventory pool, which already has
             * all necessary properties.
             */
            rset = new ResourceSet(released, type, new ResourceData());
            released = null;
        }
        return rset;
    }

    protected void deltaUpdate(IReservation r, ResourceSet set) throws Exception {
    	
        if (resources == null) {
            // in case of close for a canceled reservation.
            if (set.gained == null) {
                return;
            }
            /* first time we give concrete resources to this resource set */

            /*
             * Since this set has no concrete resources, we can only gain
             * resources. Lost and modified have no meaning in this case. Assert
             * just in case.
             */
            assert set.gained != null;
            assert set.lost == null;
            assert set.modified == null;

            /* take the units and type */
            this.units = set.gained.getUnits();
            this.type = set.type;
            /* make and initialize the empty concrete set */
            this.resources = set.gained.cloneEmpty();
            this.resources.setup(r);
            /* absorb the properties */
            mergeProperties(r, set);
            /* schedule the gained nodes to be added */
            this.gained = set.gained;
        } else {
            /* the set already has resources */

            /*
             * take the type now, we will calculate the units change and will
             * update it later in the function.
             */
            this.type = set.type;

            int difference = 0;

            /* check for service overrun */
            if ((gained != null) || (lost != null) || (modified != null)) {
                internalError("service overrun in hardChange");
            }

            /* take gained */
            if (set.gained != null) {
                this.gained = set.gained;
                difference = this.gained.getUnits();
            }

            /* take lost */
            if (set.lost != null) {
                this.lost = set.lost;
                difference -= this.lost.getUnits();
            }

            /* take modified */
            if (set.modified != null) {
                this.modified = set.modified;
            }

            
            /* update the units */
            this.units += difference;
            this.previousProperties = properties;
            /* merge properties */
            mergeProperties(r, set);
        }
    }

    protected void error(String s) throws Exception {
        throw new Exception(s);
    }

    /**
     * Sets the number of abstract units to equal the number of concrete units.
     */
    public void fixAbstractUnits() {
        if (resources != null) {
            units = resources.getUnits();
        } else {
            units = 0;
        }
    }

    /*
     * =======================================================================
     * Inventory mode operations
     * =======================================================================
     */
    protected void fullUpdate(IReservation r, ResourceSet rset) throws Exception {
    	
    	
        /* take the units and the type */
        units = rset.units;
        type = rset.type;
        /* take in the properties */
        previousProperties = properties;
        mergeProperties(r, rset);

        /* make a concrete set if the current concrete set is null */
        if (resources == null) {
            resources = rset.resources.cloneEmpty();
            resources.setup(r);
        }

        /* remember the update so that it can be processed later */
        updated = rset.resources;
    }

    /**
     * Returns the number of concrete units contained in this set.
     * @return number of concrete units
     */
    public int getConcreteUnits() {
        if (resources == null) {
            return 0;
        } else {
            return resources.getUnits();
        }
    }

    /**
     * Estimate the concrete resource units the resource set will contain at the
     * specified date.
     * @param then the date
     * @return number of concrete units
     */
    public int getConcreteUnits(Date when) {
        if (resources == null) {
            return 0;
        } else {
            return resources.holding(when);
        }
    }

    /**
     * Returns the configuration properties list.
     * @return configuration properties list. Can be null.
     */
    public Properties getConfigurationProperties() {
        if (properties != null) {
            return properties.getConfigurationProperties();
        }

        return null;
    }

    /**
     * Returns the number of concrete units needed or in excess in this resource
     * set.
     * @return number of units in excess or needed
     */
    public int getDeficit() {
        int result = units;

        if (resources != null) {
            result -= resources.getUnits();
        }

        return result;
    }

    /**
     * Returns the local properties list
     * @return local properties list. Can be null.
     */
    public Properties getLocalProperties() {
        if (properties != null) {
            return properties.getLocalProperties();
        }

        return null;
    }

    /**
     * Returns a string of notices or events pertaining to the underlying
     * resources. The event notices are consumed: subsequent calls return only
     * new information. May return null.
     */
    public Notice getNotices() {
        if (resources == null) {
            return null;
        } else {
            return resources.getNotices();
        }
    }

    /**
     * Returns the request properties.
     * @return request properties list. Can be null.
     */
    public Properties getRequestProperties() {
        if (properties != null) {
            return properties.getRequestProperties();
        }

        return null;
    }

    /**
     * Returns the reservation identifier attached to this resource set.
     * @return reservation identifier
     */
    public ReservationID getReservationID() {
        return rid;
    }

    /**
     * Returns the properties of this resource set.
     * @return set properties
     */
    public ResourceData getResourceData() {
        return this.properties;
    }

    /**
     * Returns the resource properties.
     * @return resource properties list. Can be null.
     */
    public Properties getResourceProperties() {
        if (properties != null) {
            return properties.getResourceProperties();
        }

        return null;
    }

    /**
     * Returns the concrete resources.
     * @return concrete resource set
     */
    public IConcreteSet getResources() {
        return resources;
    }

    /**
     * Returns a proxy to the site authority, which owns the resources
     * represented in the set.
     * @return site authority proxy.
     * @throws Exception
     */
    public IAuthorityProxy getSiteProxy() throws Exception {
        if (resources == null) {
            return null;
        } else {
            return resources.getSiteProxy();
        }
    }

    /**
     * Returns the resource type of the set.
     * @return resource type
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * Returns the number of abstract units in the set.
     * @return number of abstract units
     */
    public int getUnits() {
        return units;
    }

    protected void internalError(String s) throws Exception {
        throw new Exception("internal error: " + s);
    }


    /**
     * Checks if the resource set is active: allocated units are active.
     * @return true if this ResourceSet is active
     */
    public boolean isActive() {
        if (resources == null) {
            return false;
        }

        return resources.isActive();
    }


    /**
     * Checks if the resource set is closed: there are no active units. Do not
     * call this method unless the set had a close in progress: a set with
     * failed units or one that has not yet been activated may register as
     * "closed".
     * @return true if this ResourceSet is active
     */
    public boolean isClosed() {
        if (resources == null) {
            return true;
        }

        if (resources.getUnits() == 0) {
            return true;
        }

        return false;
    }

    public boolean isEmpty() {
        if ((updated != null) && (updated.getUnits() > 0)) {
            return false;
        }

        if ((gained != null) && (gained.getUnits() > 0)) {
            return false;
        }

        if ((lost != null) && (lost.getUnits() > 0)) {
            return false;
        }

        if ((modified != null) && (modified.getUnits() > 0)) {
            return false;
        }

        return true;
    }

    /**
     * Ensures that the set can be merged with another set.
     * @param set to be merged with the current set
     * @throws Exception
     */
    protected void mergeCompatible(ResourceSet in) throws Exception {
        if (in.resources == null) {
            error("merging non-concrete ResourceSet");
        }

        if (in.properties == null) {
            error("merging ResourceSet with unspecified attributes");
        }

        if ((resources != null) && (properties == null)) {
            internalError("merging to ResourceSet with unspecified attributes");
        }

        /*
         * Note that since type definition and interpretation is outside of
         * ResourceSet here we cannot determine if both resource sets contain
         * resources of compatible types. Given how mergeCompatible is used,
         * this restriction does not cause problems: on the client side, before
         * calling mergeCompatible we call the policy to check if a ticket or
         * lease update are acceptable. The policy can check the types and throw
         * an exception if the types are incompatible. On the server side, this
         * predicate is called from reserve() to complete the allocation of
         * resources. The server policy is responsible to ensure that the
         * allocated resource type is compatible with the requested one,
         * otherwise the client will eventually reject the update. The last
         * usage of mergeCompatible is from supply. The caller of supply must
         * make sure that they are grouping resources with compatible types
         * together.
         */
    }

    protected void mergeProperties(IReservation reservation, ResourceSet set) {
        if (properties == null) {
            properties = new ResourceData();
        }

        switch (reservation.getCategory()) {
            case IReservation.CategoryClient:

                /*
                 * On a service manager we only take resource properties from
                 * the broker when we receive the initial ticket update. The
                 * current assumption is that resource properties are constant
                 * over the lifetime of a reservation and are supplied by the
                 * broker.
                 */

                // XXX: change ticket to ITicket once we make Ticket implement
                // ITicket
                if ((resources == null) && (set.getResources() instanceof Ticket)) {
                    ResourceData.mergeProperties(set.getResourceProperties(), properties.getResourceProperties());
                }

                break;

            case IReservation.CategoryBroker:

                /*
                 * On a broker we will store in the resulting resource set only
                 * the resource properties, so that when we generate the update
                 * those can be send back to the client. Since the assumption is
                 * that resource properties are constant over the lifetime of a
                 * reservation, we will do this only for the initial ticket
                 * allocation. Ticket extentions will not result in the addition
                 * of new properties.
                 */
                if (resources == null) {
                    ResourceData.mergeProperties(set.getResourceProperties(), properties.getResourceProperties());
                }

                break;

            case IReservation.CategoryAuthority:

                /*
                 * On an authority we will take the local (provided by the
                 * authority) and configuration, provided by the client,
                 * properties. Local and configuration properties need to be
                 * merged for each update, as they may contain additional
                 * information required by the configuration handlers.
                 */

                ResourceData.mergeProperties(set.getLocalProperties(), properties.getLocalProperties());
                ResourceData.mergeProperties(set.getConfigurationProperties(), properties.getConfigurationProperties());

                break;
        }
    }

    /**
     * Prepares a probe: updates ConcreteSet to reflect underlying resource
     * status.
     * @throws Exception
     */
    public void prepareProbe() throws Exception {
        if (resources != null) {
            resources.probe();
            if (released == null) {
                /*
                 * Collect released and cache it for later processing
                 */
                released = resources.collectReleased();
            }
        }
    }

    /**
     * Probe (no-op)
     * @throws Exception
     */
    public void probe() throws Exception {
    }


    protected void serviceCheck() throws Exception {
        if (resources == null) {
            internalError("WARNING: service post-op call on non-concrete reservation");
        }
    }

    /**
     * Initiate close on the concrete resources
     */
    public void close() {
        if (!isClosing) {
        	isClosing = true;
        	resources.close();
        }
    }

    /*
     * =======================================================================
     * Getters and setters
     * =======================================================================
     */

    /**
     * Complete service for a term extension (server side).
     * @param term the new term
     * @throws Exception
     */

    public void serviceExtend() throws Exception {
    	
    	
        serviceCheck();

        /*
         * An elastic reservation can change concrete resources on extend. The
         * modifications are left in update/gained/lost by *Change() above. On
         * agent the concrete is updated synchronously in SoftChange, so this
         * code segment applies to authority only.
         */

        // XXX: locking and concurrency? Is there a problem here?
        IConcreteSet myGained;

        /*
         * An elastic reservation can change concrete resources on extend. The
         * modifications are left in update/gained/lost by *Change() above. On
         * agent the concrete is updated synchronously in SoftChange, so this
         * code segment applies to authority only.
         */

        // XXX: locking and concurrency? Is there a problem here?
        IConcreteSet myLost;

        /*
         * An elastic reservation can change concrete resources on extend. The
         * modifications are left in update/gained/lost by *Change() above. On
         * agent the concrete is updated synchronously in SoftChange, so this
         * code segment applies to authority only.
         */

        IConcreteSet myModified;
        myGained = gained;
        gained = null;

        myLost = lost;
        lost = null;

        myModified = modified;
        modified = null;

        if (myGained != null) {
            resources.add(myGained, true);
        }

        if (myLost != null) {
            resources.remove(myLost, true);
        }

        if (modified != null) {
            resources.modify(myModified, true);
        }
        
    }

    /**
     * Complete service for a term extension (server side).
     * @param term the new term
     * @throws Exception
     */

    public void serviceModify() throws Exception {
    	
        serviceCheck();
        
        resources.modify(resources, true);
        
    }
    
    
    
    public void serviceReserveSite() throws Exception {
        IConcreteSet cs = null;

        if (gained != null) {
            cs = gained;
            gained = null;
        }

        if (cs != null) {
            resources.add(cs, true);
        }
    }

    /**
     * Service a resource set update (client side). Any changes to existing
     * concrete resources should have been left in "updated" by an update
     * operation.
     * @param slice the slice for this reservation
     * @param rid the reservation ID
     * @param term the term of the reservation
     * @throws Exception
     */
    public void serviceUpdate(IClientReservation reservation) throws Exception {
        IConcreteSet cs = updated;
        updated = null;

        if (cs != null) {
            resources.change(cs, true);
        }
    }

    public void serviceUpdate(IBrokerReservation reservation) throws Exception {
        IConcreteSet cs = updated;
        updated = null;

        if (cs != null) {
            assert resources != null;
            resources.change(cs, true);
        }
    }

    /**
     * Sets the configuration properties.
     * @param p configuration properties list
     */
    public void setConfigurationProperties(Properties p) {
        if (properties == null) {
            properties = new ResourceData();
        }

        ResourceData.mergeProperties(p, properties.getConfigurationProperties());
    }

    /**
     * Sets the local properties.
     * @param p local properties list
     */
    public void setLocalProperties(Properties p) {
        if (properties == null) {
            properties = new ResourceData();
        }

        ResourceData.mergeProperties(p, properties.getLocalProperties());
    }

    /**
     * Sets the request properties.
     * @param p request properties list
     */
    public void setRequestProperties(Properties p) {
        if (properties == null) {
            properties = new ResourceData();
        }

        ResourceData.mergeProperties(p, properties.getRequestProperties());
    }

    /**
     * Attaches the reservation identifier to the set.
     * @param rid reservation identifier
     */
    public void setReservationID(ReservationID rid) {
        this.rid = rid;
    }

    /**
     * Sets the resource properties.
     * @param p resource properties list
     */
    public void setResourceProperties(Properties p) {
        if (properties == null) {
            properties = new ResourceData();
        }
        ResourceData.mergeProperties(p, properties.getResourceProperties());
    }

    /**
     * Set the concrete resources. Used by proxies.
     * @param cset concrete resource set
     */
    public void setResources(IConcreteSet cset) {
        assert resources == null;
        this.resources = cset;
    }

    /**
     * Sets the resource type for the set.
     * @param type resource type
     */
    public void setType(ResourceType type) {
        this.type = type;
    }

    /**
     * Sets the number of abstract units in the set.
     * @param units number of abstract units
     */
    public void setUnits(int units) {
        this.units = units;
    }

    /**
     * Passes information about the containing reservation to the concrete set.
     * @param reservation containing reservation
     */
    public void setup(IReservation reservation) {
        if (resources != null) {
            resources.setup(reservation);
        }
    }

    /**
     * Adds more resources to the resource set. This is an inventory operation:
     * no configuration operations will be triggered for the added units.
     * <p>
     * This method will typically be called with the kernel lock on.
     * </p>
     * @param rset incoming resources
     * @throws Exception
     */
    public void supply(ResourceSet set) throws Exception {
        mergeCompatible(set);
        transfer(set);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("rset: units " + Integer.toString(units) + " ");

        if (resources != null) {
            sb.append(" concrete " + Integer.toString(resources.getUnits()) + " ");
            sb.append(resources.toString());
        }

        return sb.toString();
    }

    /**
     * Transfers resources and type attributes from the passed set into the
     * current set. If the current set already contains a concrete set, all type
     * attributes are already into place and no new transfer of attributes takes
     * place.
     * @param incoming resources to transfer into the current set
     * @throws Exception
     */
    protected void transfer(ResourceSet incoming) throws Exception {
        /*
         * Note: we cannot compare types here. See comment in mergeCompatible.
         */
        if (resources == null) {
            /* this set is obtaining a concrete set */
            resources = incoming.resources;
            units = incoming.units;

            /* transfer type attributes */
            if (properties == null) {
                properties = new ResourceData();
            }

            properties.merge(incoming.getResourceData());
        } else {
            /* this set already has a concrete set */
            resources.add(incoming.resources, false);
            units += incoming.units;

            /*
             * We already have a set of properties. The incoming set cannot have
             * properties we do not have or we care about.
             */
        }
    }

    /*
     * =======================================================================
     * Operations for real resources
     * =======================================================================
     */
    public void update(IReservation r, ResourceSet rset) throws Exception {
        if ((r == null) || (rset == null)) {
            throw new IllegalArgumentException();
        }

        if (rset.resources != null) {
            fullUpdate(r, rset);
        } else {
            deltaUpdate(r, rset);
        }
    }

    public void updateProps(IReservation r, ResourceSet rset) throws Exception {
        if ((r == null) || (rset == null)) {
            throw new IllegalArgumentException();
        }

        mergeProperties(r, rset);
        
    }
    
    /**
     * Validates a fresh <code>ResourceSet</code> passed in from outside. *
     * @throws Exception thrown if the set is determined to be invalid
     */
    protected void validate() throws Exception {
        if (units < 0) {
            throw new Exception("invalid unit count: " + units);
        }
    }

    /**
     * Validates a <code>ResourceSet</code> in an incoming ticket or lease
     * request (server) or in an incoming ticket or lease update (client).
     * Called for each incoming request/update to check validity with no locks
     * held.
     * @throws Exception
     */
    public void validateIncoming() throws Exception {
        validate();

        if (resources != null) {
            resources.validateIncoming();
        }
    }

    /**
     * Validate match between abstract and concrete ResourceSet in a ResourceSet
     * representing an incoming ticket.
     * @param Term optional term associated with ResourceSet
     * @throws Exception if validation fails
     */
    public void validateIncomingTicket(Term t) throws Exception {
        // validateIncoming();

        /*
         * Due to the whacky corner case, an incoming "ticket" can have a null
         * ConcreteSet. Someday we should remove this code segment.
         */
        if (resources == null) {
            if (units != 0) {
                error("no resources to back incoming ticket");
            }

            return;
        }

        if (resources.getUnits() != units) {
            error("size mismatch on incoming ticket");
        }

        resources.validateConcrete(type, units, t);
    }

    /**
     * Validates a <code>ResourceSet</code> that is about to be sent to another
     * actor. Client-side only.
     * @throws Exception
     */
    public void validateOutgoing() throws Exception {
        validate();

        if (resources != null) {
            resources.validateOutgoing();
        }
    }
}
