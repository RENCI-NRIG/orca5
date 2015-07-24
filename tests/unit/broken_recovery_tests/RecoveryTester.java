package orca.tests.unit.recovery;

import java.io.PrintWriter;
import java.util.Properties;

import orca.cod.NodeGroup;
import orca.cod.api.ICodClientReservation;
import orca.cod.api.ICodSlice;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.standard.container.StandardContainerManagerObject;
import orca.policy.core.util.PropertiesManager;
import orca.policy.core.util.ResourceProperties;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IEvent;
import orca.shirako.api.IEventListener;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.api.IState;
import orca.shirako.api.IStateChangeListener;
import orca.shirako.common.ReservationID;
import orca.shirako.common.ReservationState;
import orca.shirako.common.ResourceData;
import orca.shirako.common.ResourceType;
import orca.shirako.common.SliceID;
import orca.shirako.common.TestException;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaContainer;
import orca.shirako.container.api.IOrcaContainer;
import orca.shirako.core.Event;
import orca.shirako.kernel.CodClientSliceFactory;
import orca.shirako.kernel.CodServiceManagerReservationFactory;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.tests.core.SimpleLoadSource;

import org.apache.log4j.Logger;

/**
 * Tests recovery from a specific condition
 */
public class RecoveryTester
{
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeErrorStartActor = 5;
    public static final int ExitCodeInternalError = 10;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeTimeout = 100;

    public static final String SliceID = "recovery";

    public static final String NameSM = "service";
    public static final String NameAgent = "broker";
    public static final String NameAuthority = "site";

    public static final int TestModeServiceManager = 1;
    public static final int TestModeAgent = 2;
    public static final int TestModeAuthority = 4;

    public static final int DefaultLeaseLength = 15;
    public static final int DefaultUnits = 1;
    public static ResourceType DefaultResourceType = new ResourceType(1);
    public static final boolean DefaultElasticTime = true;
    public static final long DefaultCyclesToWait = 0;
    public static String DefaultConfigFile = Globals.LocalRootDirectory + "/handlers/common/recover.xml";

    /**
     * The ant driver file for client-side join/leave operations.
     */
    protected String configFile = DefaultConfigFile;    
    /**
     * Lease length (in cycles).
     */
    protected long leaseLength = DefaultLeaseLength;
    /**
     * Timeout for reservations (in cycles).
     */
    protected long timeout;
    /**
     * Number of resource units to request
     */
    protected int units = DefaultUnits;
    /**
     * The resource type to use;
     */
    protected ResourceType rtype = DefaultResourceType;
    /**
     * The number of cycles to wait before starting recovery
     */
    protected long cyclesToWait = DefaultCyclesToWait;
    /**
     * Are reservations elastic in time?
     */
    protected boolean elasticTime = DefaultElasticTime;
    /**
     * Broker advance time
     */
    protected long advanceTime;
    /**
     * The service manager
     */
    protected IServiceManager sm;
    /**
     * The slice object at the service manager
     */
    protected ISlice slice;
    /**
     * Final reservation state
     */
    protected ReservationState finalState;
    /**
     * The reservation state to check
     */
    protected ReservationState stateToCheck;
    /**
     * Current state of the sm reservation
     */
    protected ReservationState currentStateSM;
    /**
     * Current state of the agent reservation
     */
    protected ReservationState currentStateAgent;
    /**
     * Current state of the site reservation;
     */
    protected ReservationState currentStateSite;
    /**
     * The sm reservation
     */
    protected IServiceManagerReservation currentReservationSM;
    /**
     * The agent reservation
     */
    protected IBrokerReservation currentReservationAgent;
    /**
     * The site reservation
     */
    protected IAuthorityReservation currentReservationSite;

    /**
     * Number of time the unit check failed
     */
    protected int mismatchCount = 0;

    /**
     * The logger.
     */
    protected Logger logger;

    /**
     * Listener for state changes at the reservation layer
     */
    protected IStateChangeListener reservationListener = new IStateChangeListener()
    {
        public void transition(Object obj, IState from, IState to)
        {
            reservationTransition((IReservation) obj, (ReservationState) from, (ReservationState) to);
        }
    };

    /**
     * Listener for state changes at the database layer.
     */
    protected IStateChangeListener databaseListener = new IStateChangeListener()
    {
        public void transition(Object obj, IState from, IState to) throws Exception
        {
            databaseTransition((IReservation) obj, (ReservationState) from, (ReservationState) to);
        }
    };

    protected IEventListener eventLisener = new IEventListener()
    {
        public void notify(IEvent event)
        {
            notifyEvent(event);
        }
    };

    /**
     * Set to true when recovery starts
     */
    protected boolean recoverying = false;
    /**
     * True if the reservation has gone through one extension after recovery
     */
    protected boolean hasExtended = false;
    /**
     * Number of extensions
     */
    protected int count = 0;
    /**
     * Logger for events
     */
    protected PrintWriter writer;
    /**
     * Test mode
     */
    protected int testMode;

    public RecoveryTester(int testMode, ReservationState state)
    {
        this.testMode = testMode;
        this.stateToCheck = state;
        logger = Globals.getLogger("recovery.tester");
    }

    public RecoveryResult runTest()
    {
        try {
            init();
            issueRequest();
            int code = recover(getActorName());
            if (code == ExitCodeOK) {
                if (mismatchCount > 0) {
                    code = ExitCodeUnitsError;
                }
            }
            return new RecoveryResult(code, finalState);
        } catch (Exception e){
            throw new RuntimeException(e);            
        } finally {
            clean();
        }
    }

    protected void checkUnits(IReservation r)
    {
        boolean match = true;

        switch (r.getState()) {
            case ReservationStates.Ticketed:
                if (!(r instanceof IAuthorityReservation)) {
                    match = (r.getUnits() == units);
                }
                break;
            case ReservationStates.Active:
                if (r.getPendingState() == ReservationStates.None) {
                    if (r instanceof IAuthorityReservation) {
                        match = (r.getUnits() == units);
                        // match = (match && (r.getLeasedUnits() == units));
                    } else {
                        IServiceManagerReservation rc = (IServiceManagerReservation) r;
                        synchronized (rc) {
                            if (rc.getJoinState() == ReservationStates.NoJoin) {
                                if (!rc.isPendingRecover()) {
                                    match = (r.getUnits() == units);
                                    match = (match && (r.getLeasedUnits() == units));
                                }
                            }
                        }
                    }
                }
                break;
        }

        if (!match) {
            mismatchCount++;
            // throw new RuntimeException("Units mismatched");
        }
    }

    /**
     * Reservation transition event handler
     * @param r
     * @param from
     * @param to
     */
    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to)
    {
        boolean print = false;
        IActor actor = r.getActor();
        assert actor != null;

        if (actor.getName().equals(NameSM)) {
            currentStateSM = to;
            print = true;
        } else if (actor.getName().equals(NameAgent)) {
            currentStateAgent = to;
            if (currentReservationAgent == null && r instanceof IBrokerReservation) {
                currentReservationAgent = (IBrokerReservation) r;
                print = true;
            } else if (currentReservationAgent != null && currentReservationAgent.getReservationID().equals(r.getReservationID())) {
                print = true;
            }
        } else {
            currentStateSite = to;
            if (currentReservationSite == null && r instanceof IAuthorityReservation) {
                currentReservationSite = (IAuthorityReservation) r;
                print = true;
            } else if (currentReservationSite != null && currentReservationSite.getReservationID().equals(r.getReservationID())) {
                print = true;
            }
        }

        if (print) {
            checkUnits(r);
            String msg = "Reservation state-machine transition: " + getStates(from, to) + " " + getActor(r);
            System.out.println(msg);
            if (writer != null) {
                writer.println(msg);
                writer.flush();
            }
        }
    }

    protected boolean pleaseStop = false;

    protected void notifyEvent(IEvent e)
    {
        switch (e.getComponent()) {
            case Event.ComponentKernel:
                switch (e.getId()) {
                    case orca.shirako.kernel.KernelLocation.LocationServiceUpdateLease:
                        if (testMode == TestModeServiceManager && !recoverying) {
                            if (e.getActor().getName().equals(NameSM)) {
                                if (stateToCheck.equals(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.Joining))) {
                                    IReservation r = (IReservation) e.getSubject();
                                    if (currentReservationSM != null && r.getReservationID().equals(currentReservationSM.getReservationID())) {
                                        // pleaseStop = true;
                                        // stopActor(NameSM);
                                    }
                                }
                            }
                        }
                        break;
                }
                break;

            case Event.ComponentNodeGroup:
                switch (e.getId()) {
                    case NodeGroup.LocationTransferIn:
                        if (testMode == TestModeServiceManager && pleaseStop) {
                            if (e.getActor().getName().equals(NameSM)) {
                                pleaseStop = false;
                                stopActor(NameSM);
                            }
                        } else if (testMode == TestModeAuthority && pleaseStop) {
                            if (e.getActor().getName().equals(NameAuthority)) {
                                pleaseStop = false;
                                stopActor(NameAuthority);
                            }
                        }
                        break;
                }
                break;
        }
    }

    /**
     * Database state transition event handler
     * @param r
     * @param from
     * @param to
     * @throws Exception
     */
    protected void databaseTransition(IReservation r, ReservationState from, ReservationState to) throws Exception
    {
        boolean print = true;

        switch (testMode) {
            case TestModeServiceManager:
                dbTransitionSM(r, from, to);
                break;
            case TestModeAgent:
                dbTransitionAgent(r, from, to);
                break;
            case TestModeAuthority:
                dbTransitionAuthority(r, from, to);
                break;
        }

        if (print) {
            String msg = "Reservation database transition: " + getStates(from, to) + " " + getActor(r);
            System.out.println(msg);
            if (writer != null) {
                writer.println(msg);
                writer.flush();
            }
        }

        checkExtended(r, from, to);
    }

    /**
     * Checks if the reservation has extended
     * @param r
     * @param from
     * @param to
     * @throws Exception
     */
    protected void checkExtended(IReservation r, ReservationState from, ReservationState to) throws Exception
    {
        IActor actor = r.getActor();
        assert actor != null;

        if (actor.getName().equals(NameSM)) {
            if (to.getState() == ReservationStates.ActiveTicketed && to.getPending() == ReservationStates.None) {
                if (stateToCheck != null) {
                    if (stateToCheck.getState() == ReservationStates.ActiveTicketed || stateToCheck.getState() == ReservationStates.Active) {
                        if (count == 1) {
                            hasExtended = true;
                        } else {
                            count++;
                        }
                    } else {
                        hasExtended = true;
                    }
                }
            }
        }
    }

    /**
     * Database event handler for a service manager
     * @param r
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    protected boolean dbTransitionSM(IReservation r, ReservationState from, ReservationState to) throws Exception
    {
        boolean print = true;

        IActor actor = r.getActor();
        assert actor != null;

        if (actor.getName().equals(NameSM)) {
            if (currentReservationSM != null && r.getReservationID().equals(currentReservationSM.getReservationID())) {
                if (stateToCheck != null && stateToCheck.equals(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.Joining))) {
                    if (!recoverying) {
                        pleaseStop = true;
                    }
                    // print = true;
                } else {
                    // print = true;
                    if (!recoverying) {
                        if (stateToCheck != null && to.equals(stateToCheck)) {
                            stopActor(NameSM);
                        }
                    }
                }
            }
        }
        return print;
    }

    protected boolean dbTransitionAuthority(IReservation r, ReservationState from, ReservationState to) throws Exception
    {
        boolean print = true;

        IActor actor = r.getActor();
        assert actor != null;

        if (actor.getName().equals(NameAuthority)) {
            if (currentReservationSite != null && r.getReservationID().equals(currentReservationSite.getReservationID())) {
                if (stateToCheck != null && !recoverying && (stateToCheck.equals(new ReservationState(ReservationStates.Ticketed, ReservationStates.Priming)))) {
                    if (stateToCheck.equals(to) && !recoverying) {
                        pleaseStop = true;
                    }
                } else if (stateToCheck != null & stateToCheck.equals(new ReservationState(ReservationStates.Active, ReservationStates.Priming))) {
                    if (stateToCheck.equals(to) && !recoverying) {
                        stopActor(NameAuthority);
                    }
                } else {
                    // print = true;
                    if (!recoverying) {
                        if (stateToCheck != null && to.equals(stateToCheck)) {
                            stopActor(NameAuthority);
                        }
                    }
                }
            }
        }
        return print;
    }

    /**
     * Database event handler for an agent
     * @param r
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    protected boolean dbTransitionAgent(IReservation r, ReservationState from, ReservationState to) throws Exception
    {
        boolean print = false;
        IActor actor = r.getActor();
        assert actor != null;

        if (actor.getName().equals(NameAgent)) {
            if (currentReservationAgent != null && r.getReservationID().equals(currentReservationAgent.getReservationID())) {
                print = true;
                if (!recoverying) {
                    if (stateToCheck != null && to.equals(stateToCheck)) {
                        stopActor(NameAgent);
                    }
                }
            }
        }
        return print;
    }

    /**
     * Recovers the specified actor
     * @param actorName actor name
     * @return
     */
    protected int recover(String actorName)
    {
        boolean done = false;
        boolean waitingForCompletion = false;
        long start = 0;

        while (!done) {
            if (waitingForCompletion) {
                done = checkDone();
            } else if (getRecoverying()) {
                if (cyclesToWait != 0) {
                    try {
                        long ss = Globals.getContainer().getCurrentCycle();
                        while (Globals.getContainer().getCurrentCycle() - ss < cyclesToWait) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("error while waiting", e);
                    }
                }

                System.out.println("Recovering actor");
                if (writer != null) {
                    writer.println("Recovering actor");
                    writer.flush();
                }

                hasExtended = false;
                count = 0;
                int code = 0;
                try {
                    StandardContainerManagerObject mo = (StandardContainerManagerObject)Globals.getContainer().getContainerManagerObject();
                    ResultMng result = mo.startActor(actorName);
                    code = result.getCode();
                } catch (Exception e) {
                    code = -666;
                }
                if (code == 0) {
                    System.out.println("Actor recovered");
                    waitingForCompletion = true;
                } else {
                    System.out.println("Actor recovery failed");
                    return ExitCodeErrorStartActor;
                }
                start = Globals.getContainer().getCurrentCycle();
            }

            if (start > 0) {
                long now = Globals.getContainer().getCurrentCycle();
                if (now - start > timeout) {
                    String msg = "Timeout while waiting for reservation recovery to complete";
                    System.out.println(msg);
                    if (writer != null) {
                        writer.println(msg);
                        writer.flush();
                    }
                    return ExitCodeTimeout;
                }
            }

            if (!done) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    throw new RuntimeException("error while sleeping", e);
                }
            }
        }

        /*
         * Always remove the sm reservation
         */

        if (removeReservation(NameSM, currentReservationSM) != 0) {
            throw new RuntimeException("Cannot remove the sm reservation");
        }

        /*
         * When testing site of broker also remove site/broker reservation
         */

        if (testMode == TestModeAgent) {
            if (removeReservation(NameAgent, currentReservationAgent) != 0) {
                throw new RuntimeException("Cannot remove the agent reservation");
            }
        } else if (testMode == TestModeAuthority) {
            if (removeReservation(NameAuthority, currentReservationSite) != 0) {
                throw new RuntimeException("Cannot remove the site reservation");
            }
        }

        return ExitCodeOK;
    }

    /**
     * Checks if the service manager reservation has reached a final state
     * @return
     */
    protected synchronized boolean checkDone()
    {
        boolean result = false;

        if (currentStateSM != null) {
            if (currentStateSM.getState() == ReservationStates.Failed) {
                System.out.println("Reservation failed");
                if (writer != null) {
                    writer.println("Reservation failed");
                    writer.flush();
                }
                result = true;
            } else if (currentStateSM.getState() == ReservationStates.Closed) {
                System.out.println("Reservation is closed");
                if (writer != null) {
                    writer.println("Reservation is closed");
                    writer.flush();
                }
                result = true;
            } else if (currentStateSM.getState() == ReservationStates.Active && currentStateSM.getPending() == ReservationStates.None && currentStateSM.getJoining() == ReservationStates.NoJoin && hasExtended) {
                System.out.println("Reservation is active. We are done");
                if (writer != null) {
                    writer.println("Reservation is active");
                    writer.flush();
                }
                result = true;
            } else {
                System.out.println("still waiting...extended = " + hasExtended);
            }
        }

        if (result) {
            finalState = currentStateSM;
        }
        return result;
    }

    protected int removeReservation(String actorName, IReservation r)
    {
        int code = 0;
        try {
            IActor actor = ActorRegistry.getActor(actorName);
            assert actor != null;

            System.out.println("Closing/removing reservation for actor " + actor.getName());
            writer.println("Closing/removing reservation for actor " + actor.getName());
            writer.flush();

            if (r != null) {
                ReservationID rid = r.getReservationID();
                IReservation rc;
                rc = (IReservation) actor.getReservation(rid);

                if (rc != null) {
                    if (!rc.isClosed() && !rc.isFailed()) {
                        actor.close(rc);
                        rc.awaitClosed();
                    }
                    actor.removeReservation(rid);
                    System.out.println("Reservation removed. Done!");
                    writer.println("Reservation removed. Done!");
                    writer.flush();
                } else {
                    System.out.println("Reservation does not exist");
                    writer.println("Reservation does not exist");
                    writer.flush();
                }
            }
        } catch (Exception e) {
            System.out.println("an error occurred while trying to close reservation");
            code = -1;
        }
        return code;
    }

    protected synchronized boolean getRecoverying()
    {
        return recoverying;
    }

    /**
     * Stops the actor
     * @param name actor name
     * @throws Exception
     */
    protected void stopActor(String name)
    {
        try {
            StandardContainerManagerObject mo = (StandardContainerManagerObject)Globals.getContainer().getContainerManagerObject();
            mo.stopActor(name);
            System.out.println("Reached desired state. Actor stopped");
            if (writer != null) {
                writer.println("Reached desired state. Actor stopped");
                writer.flush();
            }
            synchronized (this) {
                recoverying = true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /*
         * We need to throw the exception to terminate the current operation.
         * The actor thread would be stopped by the stopActor thread but
         * requests that come from other actors come on separate threads. The
         * exception here will stop such a request.
         */
        throw new TestException("actor is stopped forcefully");
    }

    /**
     * Performs the necessary initialization before running the test
     */
    protected void init() throws Exception
    {
        timeout = 5 * leaseLength;
        sm = (IServiceManager) ActorRegistry.getActor(NameSM);
        if (sm == null) {
            throw new RuntimeException("Missing sm: " + NameSM);
        }
        slice = sm.getSlice(new SliceID(SliceID));
        if (slice == null) {
            ResourceData rdata = new ResourceData();
            slice = CodClientSliceFactory.getInstance().create(SliceID, rdata);
            ((ICodSlice) slice).setHostPrefix(SliceID);
            try {
                sm.registerSlice(slice);
            } catch (Exception e) {
                throw new RuntimeException("Cannot register slice", e);
            }
        }
        getAdvanceTime();
        Globals.getContainer().registerDatabaseListener(databaseListener);
        Globals.getContainer().registerReservationListener(reservationListener);
        Globals.getContainer().registerListener(eventLisener);
        // We must wait until the broker is ready to accept requests from us.
        // For now we will take a nap for 10seconds.
        // In the future we should come up with a more elegant solution.
        logger.debug("Sleeping for 10 seconds to give the broker a chance to get ready");
        Thread.sleep(10000);
        logger.debug("Ready to start test");
    }

    /**
     * Cleans up after the test is complete
     */
    protected void clean()
    {
        Globals.getContainer().unregisterDatabaseListener(databaseListener);
        Globals.getContainer().unregisterReservationListener(reservationListener);
        sm.getShirakoPlugin().getDatabase().unregisterReservationListener(databaseListener);
    }

    /**
     * Issues a reservation request
     */
    protected void issueRequest()
    {
        IServiceManagerReservation r = createReservation(sm.getCurrentCycle());
        r.registerListener(reservationListener);
        sm.getShirakoPlugin().getDatabase().registerReservationListener(databaseListener);
        try {
            sm.demand(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        currentReservationSM = r;
    }

    protected IServiceManagerReservation createReservation(long cycle)
    {
        ResourceSet rset = new ResourceSet(units, rtype);
        Properties request = rset.getRequestProperties();
        ActorClock clock = Globals.getContainer().getActorClock();

        Term term = new Term(clock.date(cycle + advanceTime), clock.getMillis((long) leaseLength));
        ICodClientReservation r = (ICodClientReservation) CodServiceManagerReservationFactory.getInstance()
        .create(
rset,
term,
slice,
sm.getDefaultBroker());

        // 90 - 400 - 500
        ResourceProperties.setMax(request, ResourceProperties.PropertyCpu, 1);
        ResourceProperties.setMin(request, ResourceProperties.PropertyCpu, 1);
        ResourceProperties.setMax(request, ResourceProperties.PropertyMemory, 1);
        ResourceProperties.setMin(request, ResourceProperties.PropertyMemory, 1);
        ResourceProperties.setMax(request, ResourceProperties.PropertyBandwidth, 1);
        ResourceProperties.setMin(request, ResourceProperties.PropertyBandwidth, 1);
        ResourceProperties.setMax(request, ResourceProperties.PropertyUnits, units);
        ResourceProperties.setMin(request, ResourceProperties.PropertyUnits, units);

        r.setServiceConfig(configFile);
        r.setRenewable(true);
        
        PropertiesManager.setElasticTime(rset, true);

        return r;
    }

    /**
     * Calls up to a default broker to query and set the advance time.
     */
    protected void getAdvanceTime()
    {
        Properties myProps = new Properties();
        myProps.setProperty("advanceTime", "null");
        try {
            Properties returnProps = sm.getDefaultBroker().query(myProps, sm.getIdentity());
            advanceTime = Long.parseLong(returnProps.getProperty("advanceTime"));
        } catch (Exception e) {
            throw new RuntimeException("Cannot obtain advance time", e);
        }
        advanceTime = advanceTime + SimpleLoadSource.CLOCK_SKEW;
    }

    /**
     * Converts the states to a string
     * @param from
     * @param to
     * @return
     */
    protected String getStates(ReservationState from, ReservationState to)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("from: ");
        if (from != null) {
            sb.append(from.toString());
            sb.append(" ");
        } else {
            sb.append(" * ");
        }
        sb.append("to: ");
        assert to != null;
        sb.append(to.toString());
        sb.append(" ");
        return sb.toString();
    }

    /**
     * Returns the actor this reservation belongs to
     * @param r
     * @return
     */
    protected String getActor(IReservation r)
    {
        String msg;
        IActor actor = r.getActor();
        if (actor != null) {
            msg = "Actor: " + actor.getName();
        } else {
            msg = "Actor: <unknown>";
        }
        msg += " rid #" + r.getReservationID().toHashString();
        return msg;
    }

    /**
     * Returns the name of the tested actor
     * @return
     */
    protected String getActorName()
    {
        String actorName = null;

        switch (testMode) {
            case TestModeServiceManager:
                actorName = NameSM;
                break;
            case TestModeAgent:
                actorName = NameAgent;
                break;
            case TestModeAuthority:
                actorName = NameAuthority;
                break;
        }
        return actorName;
    }

    /**
     * @param cyclesToWait the cyclesToWait to set
     */
    public void setCyclesToWait(long cyclesToWait)
    {
        this.cyclesToWait = cyclesToWait;
    }

    /**
     * @param elasticTime the elasticTime to set
     */
    public void setElasticTime(boolean elasticTime)
    {
        this.elasticTime = elasticTime;
    }

    /**
     * @param leaseLength the leaseLength to set
     */
    public void setLeaseLength(long leaseLength)
    {
        this.leaseLength = leaseLength;
    }

    /**
     * @param rtype the rtype to set
     */
    public void setRtype(ResourceType rtype)
    {
        this.rtype = rtype;
    }

    /**
     * @param stateToCheck the stateToCheck to set
     */
    public void setStateToCheck(ReservationState stateToCheck)
    {
        this.stateToCheck = stateToCheck;
    }

    /**
     * @param testMode the testMode to set
     */
    public void setTestMode(int testMode)
    {
        this.testMode = testMode;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(int units)
    {
        this.units = units;
    }

    /**
     * @param writer the writer to set
     */
    public void setWriter(PrintWriter writer)
    {
        this.writer = writer;
    }
}
