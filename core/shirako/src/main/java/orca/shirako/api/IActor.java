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

import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.FailedRPC;
import orca.shirako.time.ActorClock;
import orca.util.IOrcaTimerQueue;
import orca.util.Initializable;
import orca.util.persistence.Persistable;
import orca.util.persistence.RecoverParent;
import orca.util.persistence.Recoverable;
import orca.util.persistence.Referenceable;

import org.apache.log4j.Logger;

/**
 * <code>IActor</code> defines the common functionality of all actors. An actor
 * offers a collection of management operations for slices and reservations and
 * implements the public methods necessary to serve calls from other actors,
 * e.g., requests for tickets and leases.
 * <p>
 * Every actor has a globally unique identifier and a name. The current
 * implementation assumes that names are globally unique. In addition, each
 * actor can have an optional description (used for display purposes, e.g., the
 * web portal).
 * <p>
 * The actions of each actor, e.g, how to request new resources, how to
 * arbitrate among multiple ticket requests, etc, are driven by policy modules.
 * </p>
 * <p>
 * Shirako defines three types of actors:
 * <ul>
 * <li>Service Manager - the manager of an application/service. Service managers
 * request and consume resources</li>
 * <li>Broker - arbiter among requests for resources. Brokers determine who gets
 * what and for how long.</li>
 * <li>Site Authority - owner of resources.</li>
 * </ul>
 * <p>
 * Each of the aforementioned roles is defined in a corresponding interface. An
 * actor instance must implement at least one of these interfaces.
 * 
 * @see IClientActor
 * @see IServerActor
 * @see IServiceManager
 * @see IBroker
 * @see IAuthority
 * @see IPolicy
 */
public interface IActor extends Initializable, Persistable, Recoverable, RecoverParent,
        Referenceable, IActorIdentity, ISliceOperations, IReservationOperations, ITick,
        IOrcaTimerQueue {
    public static final String PropertyGuid = "ActorGuid";
    public static final String PropertyName = "ActorName";
    public static final String PropertyType = "ActorType";

    /**
     * Informs the actor that it has been integrated in the container. This
     * method should finish the initialization of the actor: some initialization
     * steps may not be able to execute until the actor is part of the running
     * container.
     * 
     * @throws Exception
     *             if a critical error occurs while processing the event
     */
    public void actorAdded() throws Exception;

    public void actorRemoved();

    /**
     * Returns the actor clock used by the actor.
     * 
     * @return actor clock
     */
    public ActorClock getActorClock();

    /**
     * Returns the cycle this actor is processing.
     * 
     * @return the current clock cycle
     */
    public long getCurrentCycle();

    /**
     * Returns the description for the actor.
     * 
     * @return description
     */
    public String getDescription();

    /**
     * Returns the logger used by the actor.
     * 
     * @return logger
     */
    public Logger getLogger();

    /**
     * Returns the policy used by the actor.
     * 
     * @return the policy used by the actor
     */
    public IPolicy getPolicy();

    /**
     * Returns the <code>ShirakoPlugin</code> used by the actor.
     * 
     * @return shirako plugin
     */
    public IShirakoPlugin getShirakoPlugin();

    /**
     * Returns the actor type code.
     * 
     * @return actor type code
     */
    public int getType();

    /**
     * Initializes the actor key store. Called early in the initialization
     * process.
     * 
     * @throws Exception
     *             if the key store cannot be initialized
     */
    public void initializeKeyStore() throws Exception;

    /**
     * Checks if the actor has completed recovery.
     * 
     * @return true if this actor has completed recovery
     */
    public boolean isRecovered();

    /**
     * Returns the value of the stopped flag.
     * 
     * @return true if the actor has been stopped
     */
    public boolean isStopped();

    /**
     * Recovers the actor from saved database state.
     * 
     * @throws Exception
     *             if an error occurs during recovery
     */
    public void recover() throws Exception;

    /**
     * Sets the actor clock to be used by the actor.
     * 
     * @param clock
     *            actor clock
     */
    public void setActorClock(ActorClock clock);

    /**
     * Sets the description for the actor.
     * 
     * @param description
     *            actor description
     */
    public void setDescription(String description);

    /**
     * Sets the identity of this actor. Must be called before
     * <code>initialize</code>.
     * 
     * @param token
     *            actor's identity token
     */
    public void setIdentity(AuthToken token);

    /**
     * Sets the policy for this actor. Must be called before
     * <code>initialize</code>
     * 
     * @param policy
     *            policy implementation to use
     */
    public void setPolicy(IPolicy policy);

    /**
     * Sets the recovered flag.
     * 
     * @param value
     *            flag value
     */
    public void setRecovered(boolean value);

    /**
     * Sets the plugin to be used by the actor. Must be called before
     * <code>initialize</code>.
     * 
     * @param spi
     *            shirako plugin to use.
     */
    public void setShirakoPlugin(IShirakoPlugin spi);

    /**
     * Performs all required actions when starting an actor. Note: this method
     * is for internal use. An actor cannot be started by invoking the method
     * directly. Use the management interface to start actors, instead.
     */
    public void start();

    /**
     * Performs all required actions when stopping an actor. Note: this method
     * is for internal use. An actor cannot be stopped by invoking the method
     * directly. Use the management interface to stop actors, instead.
     */
    public void stop();

    /**
     * Adds an event
     * 
     * @param incoming incoming event
     */
    public void queueEvent(IActorEvent incoming);

    /**
     * Issues a query request to the specified actor. The call is non-blocking.
     * When the response from the remote actor is received, handler is invoked.
     * 
     * @param actorProxy actor proxy
     * @param query query
     * @param handler handler
     */
    public void query(IActorProxy actorProxy, Properties query, IQueryResponseHandler handler);

    /**
     * Processes a query request from the specified caller.
     * 
     * @param properties
     *            the request
     * @param caller
     *            the caller
     * @return query response
     */
    public Properties query(final Properties properties, final AuthToken caller);

    // FIXME: move to actor private interface(kernel)
    public void handleFailedRPC(ReservationID rid, FailedRPC rpc);

    // public void probePending(ReservationID rid);

    public void executeOnActorThread(final IActorRunnable r) throws Exception;

    public Object executeOnActorThreadAndWait(final IActorRunnable r) throws Exception;

    public String getManagementObjectClass();

    public IReservationTracker getReservationTracker();

    public void awaitNoPendingReservations() throws InterruptedException;
}
