/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.core;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import orca.security.AccessMonitor;
import orca.security.AuthToken;
import orca.security.Guard;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorEvent;
import orca.shirako.api.IActorProxy;
import orca.shirako.api.IActorRunnable;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.api.IReservation;
import orca.shirako.api.IReservationTracker;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.kernel.FailedRPC;
import orca.shirako.kernel.KernelWrapper;
import orca.shirako.kernel.RPCManager;
import orca.shirako.kernel.ReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.proxies.Proxy;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.shirako.util.AllActorEventsFilter;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.TestException;
import orca.util.ID;
import orca.util.IOrcaTimerTask;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.PersistenceUtils;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

/**
 * <code>Actor</code> is the base class for all actor implementations.
 */
public abstract class Actor implements IActor {
    public static final String PropertyAuthToken = "ActorAuthToken";
    public static final String PropertyDescription = "ActorDescription";
    public static final String PropertyMapper = "ActorMapper";
    public static final String PropertyMapperClass = "ActorMapperClass";
    public static final String PropertyPlugin = "ActorPlugin";
    public static final String PropertyPluginClass = "ActorPluginClass";
    public static final String DefaultDescription = "no description";

    public static int actorCount = 0;
    /**
     * Extracts the actor name from the given properties list.
     * 
     * @param properties
     *            properties list
     * @return actor name
     */
    public static String getName(final Properties properties) {
        return properties.getProperty(PropertyName);
    }

    /**
     * Extracts the actor type code from the given properties list.
     * 
     * @param properties
     *            properties list
     * @return actor type code or 0 if an error occurs
     */
    public static int getType(final Properties properties) {
        try {
            return PropList.getIntegerProperty(properties, PropertyType);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Globally unique identifier for this actor.
     */
    @Persistent(key = PropertyGuid, table = "Actors", column = "act_guid")
    protected ID guid;
    /**
     * Actor name.
     */
    @Persistent(key = PropertyName, table = "Actors", column = "act_name")
    protected String name;
    /**
     * Actor type code.
     */
    @Persistent(key = PropertyType, table = "Actors", column = "act_type")
    protected int type;
    /**
     * Actor description.
     */
    @Persistent(key = PropertyDescription, table = "Actors")
    protected String description;
    /**
     * Identity object representing this actor.
     */
    @Persistent(key = PropertyAuthToken, table = "Actors")
    protected AuthToken identity;
    /**
     * Actor policy object.
     */
    @Persistent(key = PropertyMapper, table = "Actors")
    protected IPolicy policy;
    /**
     * The shirako plugin.
     */
    @Persistent(key = PropertyPlugin, table = "Actors")
    protected IShirakoPlugin spi;
    /**
     * True if this actor has completed the recovery phase.
     */
    @NotPersistent
    protected boolean recovered = false;
    /**
     * The kernel wrapper.
     */
    @NotPersistent
    protected KernelWrapper wrapper;
    /**
     * Logger.
     */
    @NotPersistent
    protected Logger logger;
    /**
     * Factory for term.
     */
    @NotPersistent
    protected ActorClock clock;
    /**
     * Access control monitor.
     */
    @NotPersistent
    protected AccessMonitor monitor;
    /**
     * The current cycle.
     */
    @NotPersistent
    protected long currentCycle;
    /**
     * True if the current tick is the first tick this actor has received.
     */
    @NotPersistent
    protected boolean firstTick = true;
    /**
     * Set to true when the actor is stopped.
     */
    @NotPersistent
    protected volatile boolean stopped = false;
    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized = false;
    /**
     * Lock protecting the <code>thread</code> field.
     */
    @NotPersistent
    private Object threadLock = new Object();
    /**
     * Contains a reference to the thread currently executing the timer handler.
     * This field is set at the entry to {@link #externalTick(long)} and clear
     * at the exit. The primary use of the field is to handle correctly stopping
     * the actor.
     */
    @NotPersistent
    private Thread thread;
    /**
     * A queue of timers that have fired and need to be processed.
     */
    @NotPersistent
    protected ArrayList<IOrcaTimerTask> timerQueue = new ArrayList<IOrcaTimerTask>();
    @NotPersistent
    protected ArrayList<IActorEvent> eventQueue = new ArrayList<IActorEvent>();
    @NotPersistent
    protected ReservationTracker reservationTracker;
    @NotPersistent
    protected ID subscriptionID;
    /**
     * Lock used to synchronize the actor event loop
     */
    @NotPersistent
    protected Object actorMainLock = new Object();
    /**
     * Reservations to close once recovery is complete.
     */
    @NotPersistent
    protected ReservationSet closing = new ReservationSet();

    /**
     * Default constructor. This constructor is intended to be used for dynamic
     * instantiation/configuration of an actor. The constructor creates an
     * "empty" actor. Many of the important actor fields must be attached by the
     * creator of the object.
     */
    public Actor() {
        this.guid = new ID();
        this.description = DefaultDescription;
    }

    /**
     * Creates a new actor with the given identity and clock.
     * 
     * @param auth
     *            actor identity
     * @param clock
     *            clock
     */
    public Actor(final AuthToken auth, final ActorClock clock) {
        this.description = DefaultDescription;
        this.identity = auth;
        this.logger = Globals.getLogger(identity.getName());
        this.clock = clock;
        this.guid = new ID();
    }

    /**
     * {@inheritDoc}
     */
    public void actorAdded() throws Exception {
        spi.actorAdded();
        reservationTracker = new ReservationTracker();
        subscriptionID = Globals.eventManager.createSubscription(reservationTracker,
                identity,
                new AllActorEventsFilter(getGuid()));
    }

    public void actorRemoved() {
        if (subscriptionID != null) {
            try {
                Globals.eventManager.deleteSubscription(subscriptionID, identity);
            } catch (Exception e) {
                Globals.Log.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void fail(final ReservationID rid, final String message) throws Exception {
        wrapper.fail(rid, message);
    }

    /**
     * {@inheritDoc}
     */
    public void close(final IReservation reservation) throws Exception {
        if (!this.recovered) {
            closing.add(reservation);
        } else {
            wrapper.close(reservation.getReservationID());
        }
    }

    public void closeSliceReservations(final SliceID sliceID) throws Exception {
        wrapper.closeSliceReservations(sliceID);
    }

    /**
     * {@inheritDoc}
     */
    public void close(final ReservationID rid) throws Exception {
        wrapper.close(rid);
    }

    /**
     * {@inheritDoc}
     */
    public void close(final ReservationSet reservations) {
        for (IReservation r : reservations) {
            try {
                close(r);
            } catch (Exception e) {
                logger.error("Could not close for #" + r.getReservationID().toHashString(), e);
            }
        }
    }

    /**
     * Logs and propagates a general error.
     * 
     * @param string
     *            log/exception message.
     * @throws Exception
     *             always
     */
    protected void error(final String string) throws Exception {
        logger.error(string);
        throw new Exception(string);
    }

    /**
     * {@inheritDoc}
     */
    public void extend(final IReservation reservation, final ResourceSet resources, final Term term)
            throws Exception {
        wrapper.extendReservation(reservation.getReservationID(), resources, term);
    }

    /**
     * {@inheritDoc}
     */
    public void extend(final ReservationID rid, final ResourceSet rset, final Term term)
            throws Exception {
        wrapper.extendReservation(rid, rset, term);
    }
    
    public void externalTick(final long cycle) throws Exception {
        IActorEvent e = new IActorEvent() {
            public void process() throws Exception {
                actorTick(cycle);
            }
        };

        queueEvent(e);
    }

    protected void actorMain() {
        while (true) {
            IActorEvent[] events = null;
            IOrcaTimerTask[] timers = null;

            synchronized (actorMainLock) {
                while (eventQueue.isEmpty() && timerQueue.isEmpty()) {
                    try {
                        actorMainLock.wait();
                    } catch (InterruptedException e) {
                        logger.info("Actor thread interrupted. Exiting");
                        return;
                    }
                }

                if (stopped) {
                    logger.info("Actor exiting");
                    return;
                }

                if (!eventQueue.isEmpty()) {
                    events = eventQueue.toArray(new IActorEvent[eventQueue.size()]);
                    eventQueue.clear();
                }

                if (!timerQueue.isEmpty()) {
                    timers = timerQueue.toArray(new IOrcaTimerTask[timerQueue.size()]);
                    timerQueue.clear();
                }
                // notify any waiters that we've drained events from the queues
                actorMainLock.notifyAll();
            }

            if (events != null) {
                logger.debug("Processing " + events.length + " events");
                for (IActorEvent event : events) {
                    try {
                        event.process();
                    } catch (TestException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error("Error while processing event " + event.getClass().getName(),
                                e);
                    }
                }
            }

            if (timers != null) {
                logger.debug("Processing " + timers.length + " timers");
                for (IOrcaTimerTask timer : timers) {
                    try {
                        processTimer(timer);
                    } catch (TestException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error("Error while processing a timer", e);
                    }
                }
            }
        }
    }

    private void actorTick(long cycle) throws Exception {
        if (!recovered) {
            logger.warn("Tick for an actor that has not completed recovery");
            return;
        }

        long c;
        if (firstTick) {
            c = cycle;
        } else {
            c = currentCycle + 1;
        }

        for (; c <= cycle; c++) {
            if (logger.isDebugEnabled()) {
                logger.debug("actorTick:" + c + " start");
            }

            currentCycle = c;
            policy.prepare(currentCycle);

            // recover the actor if this is the first tick
            if (firstTick) {
                reset();
            }

            tickHandler();
            policy.finish(currentCycle);

            // call the kernel to complete pending operations and purge unneeded
            // state.
            wrapper.tick();

            firstTick = false;

            if (logger.isDebugEnabled()) {
                logger.debug("actorTick:" + c + " end");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ActorClock getActorClock() {
        return clock;
    }

    /**
     * {@inheritDoc}
     */
    public ISlice[] getClientSlices() {
        return wrapper.getClientSlices();
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentCycle() {
        return currentCycle;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    public ID getGuid() {
        if (identity != null) {
            return identity.getGuid();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public AuthToken getIdentity() {
        return identity;
    }

    /**
     * {@inheritDoc}
     */
    public ISlice[] getInventorySlices() {
        return wrapper.getInventorySlices();
    }

    /**
     * {@inheritDoc}
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /*
     * Operations for reservations.
     */

    /**
     * {@inheritDoc}
     */
    public IPolicy getPolicy() {
        return policy;
    }

    /**
     * {@inheritDoc}
     */
    public IReservation getReservation(final ReservationID rid) {
        return wrapper.getReservation(rid);
    }

    /**
     * {@inheritDoc}
     */
    public IReservation[] getReservations(final SliceID sliceID) {
        return wrapper.getReservations(sliceID);
    }

    /**
     * {@inheritDoc}
     */
    public IShirakoPlugin getShirakoPlugin() {
        return spi;
    }

    /**
     * {@inheritDoc}
     */
    public ISlice getSlice(final SliceID sliceID) {
        return wrapper.getSlice(sliceID);
    }

    /**
     * {@inheritDoc}
     */
    public ISlice[] getSlices() {
        return wrapper.getSlices();
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize() throws OrcaException {
        if (!initialized) {
            if (identity == null) {
                throw new OrcaException("The actor is not properly created: no identity");
            }

            if (spi == null) {
                throw new OrcaException("The actor is not properly created: no plugin");
            }

            if (policy == null) {
                throw new OrcaException("The actor is not properly created: no policy");
            }

            if (monitor == null) {
                monitor = new AccessMonitor();
            }

            if (name == null) {
                name = identity.getName();
            }

            if (name == null) {
                throw new OrcaException("The actor is not properly created: no name");
            }

            if (clock == null) {
                clock = Globals.getContainer().getActorClock();
            }

            if (clock == null) {
                throw new OrcaException("The actor is not properly created: no clock");
            }

            if (logger == null) {
                this.logger = Globals.getLogger(identity.getName());
            }

            spi.setActor(this);
            spi.initialize();

            policy.setActor(this);
            policy.initialize();

            wrapper = new KernelWrapper(this, spi, policy, monitor, new Guard());

            /* set cycle to -1 to indicate that no ticks have been received yet */
            currentCycle = -1;
            initialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void initializeKeyStore() throws Exception {
        spi.initializeKeyStore(this);
    }

    /**
     * Logs and propagates an internal error.
     * 
     * @param string
     *            error string
     * @throws Exception
     *             always
     */
    protected void internalError(final String string) throws Exception {
        logger.error("Internal actor error: " + string);
        throw new Exception("Internal error: " + string);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRecovered() {
        return this.recovered;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * {@inheritDoc}
     */
    public Properties query(final Properties properties, final AuthToken caller) {
        return wrapper.query(properties, caller);
    }

    public void query(IActorProxy actorProxy, Properties query, IQueryResponseHandler handler) {
        ICallbackProxy callback = Proxy.getCallback(this, actorProxy.getType());
        RPCManager.query(this, actorProxy, callback, query, handler);
    }

    public void recover() throws Exception {
        logger.info("Starting recovery");

        recoveryStarting();

        logger.debug("Recovering inventory slices");
        Vector<Properties> inventorySlices = spi.getDatabase().getInventorySlices();
        logger.debug("Found " + inventorySlices.size() + " inventory slices");
        recoverSlices(inventorySlices);
        logger.debug("Recovery of inventory slices complete");

        logger.debug("Recovering client slices");
        Vector<Properties> clientSlices = spi.getDatabase().getClientSlices();
        logger.debug("Found " + clientSlices.size() + " client slices");
        recoverSlices(clientSlices);
        logger.debug("Recovery of client slices complete");

        recovered = true;

        recoveryEnded();
        logger.info("Recovery complete");
    }

    protected void recoveryStarting() {
        spi.recoveryStarting();
        policy.recoveryStarting();
    }

    protected void recoveryEnded() {
        spi.recoveryEnded();
        policy.recoveryEnded();
    }

    protected void recoverSlices(Vector<Properties> v) throws Exception {
        for (Properties p : v) {
            try {
                recoverSlice(p);
            } catch (Exception e) {
                String err = "Error in recoverSlice for property list " + p + ": " + e.toString();
                logger.error(err, e);
            }
        }
    }

    protected void recoverSlice(Properties p) throws Exception {
        SliceID sliceID = SliceFactory.getSliceID(p);
        if (sliceID == null) {
            throw new Exception("Missing slice guid");
        }

        ISlice slice = getSlice(sliceID);
        if (slice == null) {
            logger.info("Recovering slice: " + sliceID.toHashString());

            logger.debug("Instantiating slice object");
            slice = SliceFactory.createInstance(p);

            logger.debug("Recoverying slice object");
            PersistenceUtils.recover(slice, this, p);

            logger.debug("Informing the plugin about the slice");
            spi.revisit(slice);

            logger.debug("Registering slice: " + sliceID.toHashString());
            reregisterSlice(slice);

            logger.debug("Recovering reservations in slice: " + sliceID.toHashString());
            recoverReservations(slice);
            logger.info("Recovery of slice " + sliceID.toHashString() + " complete");
        }
    }

    protected void recoverReservations(ISlice slice) throws OrcaException {
        String name = slice.getName() + "(" + slice.getSliceID().toHashString() + ")";
        logger.info("Starting to recover reservations in slice " + name);

        Vector<Properties> reservations = null;

        try {
            reservations = spi.getDatabase().getReservations(slice.getSliceID());
        } catch (Exception e) {
            throw new OrcaException("Could not fetch reservation records for slice " + name
                    + " from the database", e);
        }

        logger.debug("There are " + reservations.size() + " reservation(s) in slice " + name);

        for (Properties p : reservations) {
            try {
                recoverReservation(p);
            } catch (OrcaException e) {
                logger.error("Unexpected error while recoverying reservation", e);
            }
        }

        logger.info("Recovery for reservations in slice " + slice + " complete");
    }

    protected void recoverReservation(Properties p) throws OrcaException {
        IReservation r = null;
        try {
            try {
                r = ReservationFactory.createInstance(p);
            } catch (Exception e) {
                throw new OrcaException("Cannot instantiate reservation object: state=" + p, e);
            }

            logger.info("Found reservation #" + r.getReservationID().toHashString() + " in state: "
                    + r.getReservationState());

            if (r.isClosed()) {
                logger.info("Reservation #" + r.getReservationID().toHashString()
                        + " is closed. Nothing to recover.");
                return;
            }

            logger.info("Recovering reservation #" + r.getReservationID().toHashString());

            logger.debug("Recovering reservation object");
            PersistenceUtils.recover(r, this, p);

            logger.debug("Registering the reservation with the actor");
            reregister(r);

            logger.debug("Revisiting with the ShirakoPlugin");
            spi.revisit(r);

            logger.debug("Revisiting with the actor policy");
            policy.revisit(r);

            logger.info("Recovered reservation #" + r.getReservationID().toHashString());
        } catch (OrcaException e) {
            throw e;
        } catch (Exception e) {
            throw new OrcaException("Could not recover Reservation #"
                    + r.getReservationID().toHashString(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void register(final IReservation reservation) throws Exception {
        wrapper.registerReservation(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public void registerSlice(final ISlice slice) throws Exception {
        wrapper.registerSlice(slice);
    }

    /**
     * {@inheritDoc}
     */
    public void removeReservation(final IReservation reservation) throws Exception {
        wrapper.removeReservation(reservation.getReservationID());
    }

    /**
     * {@inheritDoc}
     */
    public void removeReservation(final ReservationID rid) throws Exception {
        wrapper.removeReservation(rid);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSlice(final ISlice slice) throws Exception {
        removeSlice(slice.getSliceID());
    }

    /**
     * {@inheritDoc}
     */
    public void removeSlice(final SliceID sliceID) throws Exception {
        wrapper.removeSlice(sliceID);
    }

    /**
     * {@inheritDoc}
     */
    public void reregister(final IReservation reservation) throws Exception {
        wrapper.reregisterReservation(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public void reregisterSlice(final ISlice slice) throws OrcaException {
        wrapper.reregisterSlice(slice);
    }

    /**
     * Issues delayed operations
     */
    protected void issueDelayed() {
        assert recovered;
        close(closing);
        closing.clear();
    }

    /**
     * {@inheritDoc}
     */
    protected void reset() throws Exception {
        issueDelayed();
        policy.reset();
    }

    /**
     * {@inheritDoc}
     */
    public void setActorClock(final ActorClock clock) {
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    public void setIdentity(final AuthToken token) {
        this.identity = token;
        this.name = identity.getName();
        this.guid = token.getGuid();
    }

    /**
     * {@inheritDoc}
     */
    public void setPolicy(final IPolicy policy) {
        this.policy = policy;
    }

    /**
     * {@inheritDoc}
     */
    public void setRecovered(final boolean value) {
        this.recovered = value;
    }

    /**
     * {@inheritDoc}
     */
    public void setShirakoPlugin(final IShirakoPlugin spi) {
        this.spi = spi;
    }

    /**
     * {@inheritDoc}
     */
    public void setStopped(final boolean stopped) {
        this.stopped = stopped;
    }

    /**
     * Stores the currently executing thread. The thread is stored at the entry
     * of the timer interrupt and removed once the timer interrupt completes.
     * This information is used in {@link #stop()} to determine if the actor is
     * running and whether the thread must be interrupted.
     * 
     * @param thread
     *            DOCUMENT ME!
     */
    private void setThread(final Thread thread) {
        synchronized (threadLock) {
            this.thread = thread;
        }
    }

    protected boolean isOnActorThread() {
        Thread current = Thread.currentThread();
        boolean result;
        synchronized (threadLock) {
            result = (thread == current);
        }
        return result;
    }

    public void executeOnActorThread(final IActorRunnable r) throws Exception {
        if (isOnActorThread()) {
            r.run();
        } else {
            IActorEvent e = new IActorEvent() {
                public void process() throws Exception {
                    r.run();
                }
            };
        }
    }

    static class ExecutionStatus {
        public Object lock = new Object();
        public boolean done = false;
        public Exception exception = null;
        public Object result = null;
    };

    public Object executeOnActorThreadAndWait(final IActorRunnable r) throws Exception {
        if (isOnActorThread()) {
            return r.run();
        } else {
            final ExecutionStatus st = new ExecutionStatus();
            IActorEvent e = new IActorEvent() {
                public void process() throws Exception {
                    try {
                        st.result = r.run();
                    } catch (Exception e) {
                        st.exception = e;
                    } finally {
                        synchronized (st.lock) {
                            st.done = true;
                            st.lock.notifyAll();
                        }
                    }
                }
            };

            queueEvent(e);
            synchronized (st.lock) {
                while (!st.done) {
                    st.lock.wait();
                }
            }

            if (st.exception != null) {
                throw st.exception;
            }

            return st.result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void start() {
        stopped = false;
        synchronized (threadLock) {
            if (thread != null) {
                throw new IllegalStateException("This actor has already been started");
            }

            thread = new Thread(new Runnable() {
                public void run() {
                    try {
                    	actorCount--;
                        actorMain();
                    } catch (TestException e) {
                        logger.warn("Exiting due to TestException", e);
                    } catch (Exception e) {
                        logger.error("Unexpected error", e);
                    }
                }
            });
            thread.setName(getName());
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        stopped = true;

        synchronized (threadLock) {
            Thread temp = thread;
            thread = null;

            if (temp != null) {
                logger.warn("It seems that the actor thread is running. Interrupting it");
                temp.interrupt();
                try {
                    temp.join();
                } catch (InterruptedException e) {
                    logger.error("Could not join actor thread", e);
                }
            }
        }
    }

    protected void tickHandler() throws Exception {
    }

    protected void processTimer(IOrcaTimerTask timer) throws Exception {
        timer.execute();
    }

    public void handleFailedRPC(ReservationID rid, FailedRPC rpc) {
        wrapper.processFailedRPC(rid, rpc);
    }

    @Override
    public String toString() {
        return "actor";
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(final IReservation reservation) throws Exception {
        wrapper.unregisterReservation(reservation.getReservationID());
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(final ReservationID rid) throws Exception {
        wrapper.unregisterReservation(rid);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterSlice(final ISlice slice) throws Exception {
        unregisterSlice(slice.getSliceID());
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterSlice(final SliceID sliceID) throws Exception {
        wrapper.unregisterSlice(sliceID);
    }

    public void queueTimer(IOrcaTimerTask timer) {
        synchronized (actorMainLock) {
            timerQueue.add(timer);
            actorMainLock.notifyAll();
        }
    }

    public void queueEvent(IActorEvent e) {
        synchronized (actorMainLock) {
            eventQueue.add(e);
            actorMainLock.notifyAll();
        }
    }

    /**
     * Waits until the actor's event queue is drained or the specified timeout
     * expires.
     * 
     * @param ms
     * @return true if the actor's event queue was drained before the timeout
     *         expired, false if the timeout expired before the queue was
     *         drained
     * @throws InterruptedException
     */
    public boolean awaitNoEvents(long ms) throws InterruptedException {
        long now = System.currentTimeMillis();
        synchronized (actorMainLock) {
            while (eventQueue.size() > 0) {
                long remaining = System.currentTimeMillis() - now - ms;
                if (remaining <= 0) {
                    return false;
                }
                actorMainLock.wait(remaining);
            }
            return true;
        }
    }

    public void awaitNoPendingReservations() throws InterruptedException {
        wrapper.awaitNothingPending();
    }

    public IReservationTracker getReservationTracker() {
        return reservationTracker;
    }

    public Actor getRecoveryRoot() {
        return this;
    }

    public <V> V getObject(Class<V> type) throws PersistenceException {
        V result = null;
        if (IShirakoPlugin.class.isAssignableFrom(type)) {
            result = (V) spi;
        } else if (IPolicy.class.isAssignableFrom(type)) {
            result = (V) policy;
        } else if (Logger.class.isAssignableFrom(type)) {
            result = (V) logger;
        } else if (AuthToken.class.isAssignableFrom(type)) {
            result = (V) identity;
        } else if (ActorClock.class.isAssignableFrom(type)) {
            result = (V) clock;
        } else if (type.isAssignableFrom(this.getClass())) {
            return (V)this;
        } else {
            throw new PersistenceException("Do not know how to return an object of type: "
                    + type.getName());
        }

        if (result == null) {
            throw new PersistenceException("Do not know how to return an object of type: "
                    + type.getName());
        }
        return result;
    }

    public <V> V getObject(Class<V> type, ID reference) throws PersistenceException {
        V result = null;
        if (IReservation.class.isAssignableFrom(type)) {
            result = (V) getReservation((ReservationID) reference);
        } else if (ISlice.class.isAssignableFrom(type)) {
            result = (V) getSlice((SliceID) reference);
        } else if (IActor.class.isAssignableFrom(type)) {
            if (getReference().equals(reference)) {
                result = (V) this;
            } else {
                result = (V) ActorRegistry.getActor(reference.toString());
            }
        } else {
            throw new PersistenceException("Do not know how to return an object of type: "
                    + type.getName() + " and id " + reference);
        }

        // FIXME: During recovery, some objects may not be available when they
        // are needed.
        // For now we allow reference objects to be null, but we might need to
        // tighten that to avoid runtime errors
        // if (result == null) {
        // throw new PersistenceException("Could not obtain object of type: " +
        // type.getName() + " and id " + reference + " (" +
        // reference.toHashString() + ")");
        // }

        return result;
    }

    public ID getReference() {
        return guid;
    }
}
