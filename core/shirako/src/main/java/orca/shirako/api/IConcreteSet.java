/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.api;

import java.util.Date;
import java.util.Properties;

import orca.shirako.time.Term;
import orca.shirako.util.Notice;
import orca.util.ResourceType;
import orca.util.persistence.Recoverable;
import orca.util.persistence.Persistable;

/**
 * <code>IConcreteSet</code> defines the interface for concrete set resources. A
 * concrete set is intended to represent a set of resources. For example,
 * compute servers, storage servers, network paths, etc.
 * <b>Note:</b> each concrete set type should implement a default constructor.
 * <p>
 * Concrete sets are single threaded: their methods are invoked by the actor 
 * main thread and there is no need for internal synchronization. When a concrete set 
 * method needs to trigger a configuration action, the configuration action should
 * be executed on a separate thread. Once the configuration action completes, an event 
 * should be queued to the actor to be processed on the actor main thread.
 */
public interface IConcreteSet extends Persistable, Recoverable {
    public static final String PropertyResourceType = "cs.resourceType";
    public static final String PropertyUnits = "cs.units";
        
    /**
     * Adds the passed set to the current set. Optionally triggers configuration
     * actions on all added units.
     * @param set set to add
     * @param configure if true, configuration actions will be triggered for all
     *            added units
     * @throws Exception
     */
    public void add(IConcreteSet set, boolean configure) throws Exception;

    /**
     * Makes changes to the resources in the concrete set. The incoming concrete
     * set represents the state that the current set has to be updated to. The
     * implementation must determine what units have been added/removed/modified
     * and perform the appropriate actions.
     * @param set concrete resources representing the new state of the current
     *            set
     * @param configure if true, configuration actions will be triggered for all
     *            added, removed, or modified units
     * @throws Exception thrown if something is wrong
     */
    public void change(IConcreteSet set, boolean configure) throws Exception;

    /**
     * Makes a clone of the concrete set. Unlike {@link #cloneEmpty()}, this
     * method preserves the set: the set elements are the same objects as the
     * original IConcreteSet, but the indexing structures are different. That
     * is, adding/removing units to the original should not affect the clone.
     * But modifications to an individual unit should be visible form the
     * original and the clone.
     * @return a clone of the concrete set
     */
    public IConcreteSet clone();

    /**
     * Makes an "empty" clone of this concrete set. An "empty" clone is a copy
     * of a concrete set with the "set" removed from it.
     * @return an "empty" clone of this concrete set
     */
    public IConcreteSet cloneEmpty();

    /**
     * Initiates close operations on all resources contained in the set.
     */
    public void close();
    /**
     * Encodes the concrete set into a properties list so that it can
     * be passed to another actor.
     * @param protocol protocol
     * @return a {@link Properties} list representing this concrete set
     * @throws Exception
     */
    public Properties encode(String protocol) throws Exception;    
    /**
     * Initializes the concrete set with information derived from the
     * passed in properties list.
     * @param enc encoded {@link Properties} list describing this concrete set
     * @param plugin {@link IShirakoPlugin} of containing actor
     * @throws Exception
     */
    public void decode(Properties enc, IShirakoPlugin plugin) throws Exception;    
    /**
     * Collects any released (closed) and/or failed resources.
     * @return a concrete set containing released and or/failed resources
     * @throws Exception
     */
    public IConcreteSet collectReleased() throws Exception;

    /**
     * Gets a a collection of notices or events pertaining to the underlying
     * resources. The event notices are consumed: subsequent calls return only
     * new information. May return null.
     * @return DOCUMENT ME!
     */
    public Notice getNotices();

    /**
     * Return a proxy or reference for the unique site that owns these
     * resources.
     * @return the authority that owns the resources
     * @throws Exception
     */
    public IAuthorityProxy getSiteProxy() throws Exception;

    /**
     * Returns how many units are contained in the set.
     * @return number of units contained in the set
     */

    /**
     * Returns the current number of units in the concrete set.
     */
    public int getUnits();

    /**
     * Returns how many units are in the set at the given time instance.
     * @param date time instance
     * @return how many units will be in the set for at the given time instance.
     */
    public int holding(Date date);

    /**
     * Checks if the concrete set is active. A concrete set is active if all
     * units contained in the set are active.
     * @return true if the set is active
     */
    public boolean isActive();

    /**
     * Updates the units in the current set with information contained in the
     * passed set. Note that the passed set may contain only a subset of the
     * units contained in the current set. Optionally triggers configuration
     * actions for all removed/modified units.
     * @param set set containing the update data
     * @param configure if true, configuration actions will be triggered for all
     *            modified units
     * @throws Exception
     */
    public void modify(IConcreteSet set, boolean configure) throws Exception;

    /**
     * Checks the status of pending operations.
     * @throws Exception
     */
    public void probe() throws Exception;

    /**
     * Removes the passed set from the current set. Optionally triggers
     * configuration actions for all removed units. If the lease term for the
     * concrete set has changed, this call must be followed by a call to
     * {@link #extend(Term)}.
     * @param set set to remove
     * @param configure if true, configuration actions will be triggered for all
     *            removed units
     * @throws Exception
     */
    public void remove(IConcreteSet set, boolean configure) throws Exception;

    /**
     * Initializes the concrete set with information about the containing
     * reservation. This method is called with the manager lock on and hence
     * should not block for long periods of time.
     * @param reservation reservation this concrete set belongs to
     */
    public void setup(IReservation reservation);

    /**
     * Validate that the concrete set matches the abstract resource set
     * parameters.
     * @param type abstract resources resource type
     * @param units abstract resources units
     * @param term abstract resources term
     */
    public void validateConcrete(ResourceType type, int units, Term term) throws Exception;

    /**
     * Validates a concrete set as it is received by an actor from another
     * actor. This method should examine the contents of the concrete set and
     * determine whether it is well-formed. Well-formed is an implementation
     * specific notion. For example, a Sharp ticket is well-formed if all claims
     * the ticket is composed of nest properly. This method is also the place to
     * perform any additional verification required to ascertain that the
     * resources represented by the concrete set are valid. For example, a Sharp
     * ticket is valid if it does not result in oversubscription.
     * <p>
     * This method is called from <code>ResourceSet</code> with no locks on.
     * </p>
     * @throws Exception if validation fails
     */
    public void validateIncoming() throws Exception;

    /**
     * Validates a concrete set as it is about to be sent from the actor to
     * another actor. This method should examine the contents of the concrete
     * set and determine whether it is well-formed. Any other validation
     * required before sending the concrete set should go in this function.
     * <p>
     * This method is called from <code>ResourceSet</code> with no locks on.
     * </p>
     * @throws Exception if validation fails
     */
    public void validateOutgoing() throws Exception;

    /**
     * This method will be called during recovery to ensure that all pending
     * actions are restarted. If a unit has an outstanding action that has not
     * completed yet, that action would have to be restarted during this call.
     * @throws Exception
     */
    public void restartActions() throws Exception;
}
