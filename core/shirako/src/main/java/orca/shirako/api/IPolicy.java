/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import java.util.Properties;

import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.time.Term;
import orca.shirako.util.ReservationSet;
import orca.util.ID;
import orca.util.Initializable;
import orca.util.persistence.Recoverable;
import orca.util.persistence.Persistable;

/**
 * <code>IPolicy</code> encapsulates all policy decisions an actor must make to
 * perform its functions. Each actor has one policy instance that controls its
 * policy decisions. The policy instance must implement the <code>IPolicy</code>
 * interface together with one or more of the other policy interfaces (depending
 * on the role the actor performs).
 * <p>
 * Some things to know when implementing a policy:
 * <ol>
 * <li>The policy is always called by a single thread. No synchronization is
 * required, but method implementations should not block for long periods of
 * time, as that would prevent the actor from processing other events.</li>
 * <li>The policy operates on the passed ResourceSets. ResourceSets encapsulate
 * the free resource pools and resource requests. The policy satisfies requests
 * by using ResourceSet methods to shift resources from one ResourceSet to
 * another. Resource attributes etc. flow with these operations, and they drive
 * other resource-specific operations including SHARP ticket delegation.</li>
 * <li>The policy has access to all ResourceData attributes attached to the
 * request and the free resource pools. The client-side methods may decorate the
 * request with attributes (e.g., bids), which are passed through to the
 * server-side policy that handles the request. The client-side also has the
 * option of touching the AuthToken passed with the request...this may be a bit
 * goofy.</li>
 * <li>If it is necessary to defer an allocation request, return false. At some
 * later time, when the policy is ready to satisfy the request (e.g., after an
 * auction on an agent), set bidPending to false and arrange a call to
 * probePending on the requesting reservation. This causes the reservation to
 * retry the pending operation.</li>
 * </ol>
 */
public interface IPolicy extends Initializable, Persistable, Recoverable {
    /**
     * Informs the policy that processing for a new cycle is about to begin. The
     * policy should initialize whatever internal state is necessary to process
     * a new cycle.
     * <p>
     * <b>Note:</b> The cycle number parameter is redundant and is passed for
     * convenience. The policy can always obtain the cycle number by calling
     * <code>IActor.getCurrentCycle()</code>.
     * </p>
     * 
     * @param cycle
     *            the cycle number that is about to be processed
     */
    public void prepare(long cycle);

    /**
     * Informs the policy that all processing for the specified cycle is
     * complete. The policy can safely discard any state associated with the
     * cycle or any previous cycles.
     * <p>
     * <b>Note:</b> The cycle number parameter is redundant and is passed for
     * convenience. The policy can always obtain the cycle number by calling
     * <code>IActor.getCurrentCycle()</code>.
     * </p>
     * 
     * @param cycle
     *            the cycle number that has just passed
     */
    public void finish(long cycle);

    /**
     * Notifies the policy that a reservation is about to be extended. This
     * method will be invoked only for reservations, whose extensions have not
     * been triggered by the policy, e.g, from the management interface. The
     * policy should update its state to reflect the extend request.
     * 
     * @param reservation
     *            reservation to be extended
     * @param resources
     *            resource set used for the extension
     * @param term
     *            term used for the extension
     */
    public void extend(IReservation reservation, ResourceSet resources, Term term);

    /**
     * Notifies the policy that a reservation is about to be closed. This method
     * will be invoked for every reservation that is about to be closed, even if
     * the close was triggered by the policy itself. The policy should update
     * its internal state/cancel pending operations associated with the
     * reservation.
     * 
     * @param reservation
     *            reservation about to be closed
     */
    public void close(IReservation reservation);

    /**
     * Notifies the policy that a reservation has been closed. This method will
     * be invoked for every reservation that closes successfully. The policy
     * must uncommit any resources associated with the reservation, e.g,
     * physical machines, currency, etc.
     * <p>
     * <b>Note:</b> For an authority resources are released using the
     * {@link #IAuthorityPolicy.release(ResourceSet)} method. Authority policy
     * implementations should not consider the resources of the passed
     * reservation as released. The release will take place once all
     * configuration actions complete.
     * </p>
     * 
     * @param reservation
     *            closed reservation
     * 
     * @throws Exception
     * 
     * @see IAuthorityPolicy.release(ResourceSet)
     */
    public void closed(IReservation reservation);

    /**
     * Notifies the policy that a reservation is about to be removed. This
     * method will be invoked for each reservation that is to be removed from
     * the system. The policy should remove any state that it maintains for the
     * reservation.
     * <p>
     * <b>Note:</b> Only failed and closed reservations can be removed. The
     * system will not invoke this method if the reservation is not closed or
     * failed.
     * </p>
     * 
     * @param reservation
     *            reservation to be removed
     */
    public void remove(IReservation reservation);

    /**
     * Answers a query from another actor. This method is intended to be used to
     * obtain policy-specific parameters and information. This method should be
     * used when writing more complex policies requiring additional interaction
     * among actors. Instead of extending the proxies to support
     * passing/obtaining the required information, policy code can use the query
     * interface to request/obtain such information. The implementation should
     * not block for prolonged periods of time. If necessary, future versions
     * will update this interface to allow query responses to be delivered using
     * callbacks.
     * 
     * @param p
     *            a properties list of query parameters. Can be null or empty.
     * 
     * @return a properties list of outgoing values. If the incoming properties
     *         collection is null or empty, should return all possible
     *         properties that can be relevant to the caller.
     */
    public Properties query(Properties p);

    /**
     * Notifies the policy that a configuration action for the object
     * represented by the token parameter has completed.
     * 
     * @param action
     *            configuration action. See Config.Target*
     * @param token
     *            object or a token for the object whose configuration action
     *            has completed
     * @param outProperties
     *            output properties produced by the configuration action
     */
    public void configurationComplete(String action, ConfigToken token, Properties outProperties);

    /**
     * Post recovery entry point. This method will be invoked once all revisit
     * operations are complete and the actor is ready to operate normally.
     * 
     * @throws Exception
     */
    public void reset() throws Exception;

    /**
     * Informs the policy that recovery is about to begin.
     */
    public void recoveryStarting();
    /**
     * Informs the policy about a reservation. Called during recovery/startup.
     * The policy must re-establish any state required for the management of the
     * reservation.
     * 
     * @param reservation
     *            reservation being recovered
     */
    public void revisit(IReservation reservation) throws Exception;

    /**
     * Informs the policy that recovery has completed.
     */
    public void recoveryEnded();
    /**
     * Sets the actor the policy belongs to.
     * 
     * @param actor
     *            the actor the policy belongs to
     */
    public void setActor(IActor actor);

    /**
     * Returns a set of reservations that must be closed.
     * 
     * @param cycle
     *            the current cycle
     * 
     * @return reservations to be closed
     */
    public ReservationSet getClosing(long cycle);

    /**
     * Returns the globally unique identifier of this policy object instance.
     * 
     * @return
     */
    public ID getGuid();
}