/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.tests.unit.main;

import java.util.Properties;

import orca.manage.extensions.standard.beans.ActorMng;
import orca.manage.extensions.standard.beans.ResultActorMng;
import orca.manage.extensions.standard.container.StandardContainerManagerObject;
import orca.policy.core.util.PropertiesManager;
import orca.policy.core.util.ResourceProperties;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.api.IState;
import orca.shirako.api.IStateChangeListener;
import orca.shirako.common.ReservationState;
import orca.shirako.common.ResourceData;
import orca.shirako.common.ResourceType;
import orca.shirako.container.Globals;
import orca.shirako.core.Actor;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.tests.core.SimpleLoadSource;

import org.apache.log4j.Logger;

/**
 * Tests a single reservation end-to-end. Assumes all actors this reservation interacts with belong to the same
 * container.
 */
public class ReservationTestTool {
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeUnexpectedStateSM = 30;
    public static final int ExitCodeUnexpectedStateAgent = 30;
    public static final int ExitCodeUnexpectedStateSite = 30;
    public static final int ExitCodeTimeout = 100;

    /**
     * Slice name to use
     */
    public static final String SliceName = "unit";

    /*
     * Default values
     */
    public static final int DefaultLeaseLength = 15;
    public static final int DefaultUnits = 1;
    public static ResourceType DefaultResourceType = new ResourceType(1);
    public static final boolean DefaultElasticTime = true;
    public static final int DefaultCPU = 25;
    public static final int DefaultMemory = 128;
    public static final int DefaultBandwidth = 100;
    public static String DefaultConfigFile = Globals.LocalRootDirectory + "/handlers/common/recover.xml";
    public static final int DefaultExtendCount = 1;

    /**
     * The ant driver file for client-side join/leave operations.
     */
    protected String configFile = DefaultConfigFile;

    /**
     * CPU units to request.
     */
    protected int cpu = DefaultCPU;

    /**
     * Memory to request (MB).
     */
    protected int memory = DefaultMemory;

    /**
     * Bandwidth units to request (Mb/s).
     */
    protected int bandwidth = DefaultBandwidth;

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
     * The resource type to use.
     */
    protected ResourceType rtype = DefaultResourceType;

    /**
     * Are reservations elastic in time?
     */
    protected boolean elasticTime = DefaultElasticTime;

    /**
     * Broker advance time.
     */
    protected long advanceTime;

    /**
     * The service manager.
     */
    protected IServiceManager sm;

    /**
     * The slice object at the service manager.
     */
    protected ISlice slice;

    /**
     * Current state of the sm reservation.
     */
    protected ReservationState currentStateSM;

    /**
     * Current state of the agent reservation.
     */
    protected ReservationState currentStateAgent;

    /**
     * Current state of the site reservation.
     */
    protected ReservationState currentStateSite;

    /**
     * Final reservation states.
     */
    protected ReservationState finalStateSM;

    /**
     * Final reservation states.
     */
    protected ReservationState finalStateAgent;

    /**
     * Final reservation states.
     */
    protected ReservationState finalStateSite;

    /**
     * The sm reservation.
     */
    protected IServiceManagerReservation currentReservationSM;

    /**
     * The agent reservation.
     */
    protected IBrokerReservation currentReservationAgent;

    /**
     * The site reservation.
     */
    protected IAuthorityReservation currentReservationSite;

    /**
     * Number of times the unit check failed.
     */
    protected int mismatchCount = 0;

    /**
     * Number of times the reservation extended
     */
    protected int extendCount = 0;

    /**
     * How many times to extend before the test is considered complete.
     */
    protected int desiredExtendCount = DefaultExtendCount;

    /**
     * Number of extensions
     */
    protected int count = 0;

    /**
     * The logger.
     */
    protected Logger logger;

    /**
     * Listener for state changes at the reservation layer
     */
    protected IStateChangeListener reservationListener = new IStateChangeListener() {
        public void transition(Object obj, IState from, IState to) {
            reservationTransition((IReservation) obj, (ReservationState) from, (ReservationState) to);
        }
    };

    /**
     * Create a new instance
     */
    public ReservationTestTool() {
        logger = Globals.getLogger("reservation.tester");
    }

    /**
     * Checks if the service manager reservation has reached a final state
     * 
     * @return
     */
    protected synchronized boolean checkDone() {
        boolean result = false;

        if (currentStateSM != null) {
            if (currentStateSM.getState() == ReservationStates.Failed) {
                logger.debug("Reservation failed");
                result = true;
            } else if (currentStateSM.getState() == ReservationStates.Closed) {
                logger.debug("Reservation is closed");
                result = true;
            } else if ((currentStateSM.getState() == ReservationStates.Active)
                    && (currentStateSM.getPending() == ReservationStates.None)
                    && (currentStateSM.getJoining() == ReservationStates.NoJoin)
                    && (extendCount == desiredExtendCount)) {
                logger.debug("Reservation is active. We are done");
                result = true;
            } else {
                logger.debug("still waiting...extendCount=" + extendCount + " state: " + currentStateSM.toString());
            }
        }

        if (result) {
            finalStateSM = currentStateSM;
            finalStateAgent = currentStateAgent;
            finalStateSite = currentStateSite;
        }

        return result;
    }

    /**
     * Checks if the reservation has extended
     * 
     * @param r
     * @param from
     * @param to
     * @throws Exception
     */
    protected void checkExtended(IReservation r, ReservationState from, ReservationState to) {
        IActor actor = r.getActor();
        assert actor != null;

        if (actor == sm) {
            if ((to.getState() == ReservationStates.ActiveTicketed) && (to.getPending() == ReservationStates.None)) {
                extendCount++;
            }
        }
    }

    /**
     * Checks the final reservation states for correctness.
     * 
     * @param close
     *            true if closing
     * @return
     */
    protected int checkFinalStates(boolean close) {
        if (!close) {
            if (!(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin)
                    .equals(finalStateSM))) {
                return ExitCodeUnexpectedStateSM;
            }

            if (!(new ReservationState(ReservationStates.Active, ReservationStates.None).equals(finalStateSite))) {
                return ExitCodeUnexpectedStateSite;
            }
        } else {
            if (!(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin)
                    .equals(finalStateSM))) {
                return ExitCodeUnexpectedStateSM;
            }

            if (!(new ReservationState(ReservationStates.Closed, ReservationStates.None).equals(finalStateSite))) {
                return ExitCodeUnexpectedStateSite;
            }
        }

        if (!(new ReservationState(ReservationStates.Ticketed, ReservationStates.None).equals(finalStateAgent))) {
            return ExitCodeUnexpectedStateAgent;
        }

        return ExitCodeOK;
    }

    /**
     * Checks if the reservations hold the requested units.
     * 
     * @param r
     */
    protected void checkUnits(IReservation r) {
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
                    match = (match && (r.getLeasedUnits() == units));
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
            logger.error("units count mismatch: r.getUnits()=" + r.getUnits() + ", units=" + units
                    + ", r.getLeasedUnits()=" + r.getLeasedUnits());
        }
    }

    /**
     * Cleans up after the test is complete.
     */
    protected void clean() {
        Globals.getContainer().unregisterReservationListener(reservationListener);
    }

    /**
     * Creates a reservation object
     * 
     * @param cycle
     * @return
     */
    protected IServiceManagerReservation createReservation(long cycle) {
        ResourceSet rset = new ResourceSet(units, rtype);
        Properties request = rset.getRequestProperties();
        ActorClock clock = Globals.getContainer().getActorClock();

        Term term = new Term(clock.date(cycle + advanceTime), clock.getMillis((long) leaseLength));
        IServiceManagerReservation r = ServiceManagerReservationFactory.getInstance().create(rset, term, slice,
                sm.getDefaultBroker());

        // 90 - 400 - 500
        ResourceProperties.setMax(request, orca.policy.core.util.ResourceProperties.PropertyCpu, cpu);
        orca.policy.core.util.ResourceProperties.setMin(request, orca.policy.core.util.ResourceProperties.PropertyCpu,
                cpu);
        orca.policy.core.util.ResourceProperties.setMax(request,
                orca.policy.core.util.ResourceProperties.PropertyMemory, memory);
        orca.policy.core.util.ResourceProperties.setMin(request,
                orca.policy.core.util.ResourceProperties.PropertyMemory, memory);
        orca.policy.core.util.ResourceProperties.setMax(request,
                orca.policy.core.util.ResourceProperties.PropertyBandwidth, bandwidth);
        orca.policy.core.util.ResourceProperties.setMin(request,
                orca.policy.core.util.ResourceProperties.PropertyBandwidth, bandwidth);
        orca.policy.core.util.ResourceProperties.setMax(request, orca.policy.core.util.ResourceProperties.PropertyUnits,
                units);
        orca.policy.core.util.ResourceProperties.setMin(request, orca.policy.core.util.ResourceProperties.PropertyUnits,
                units);

        // request.setProperty(BrokerWorstFitMultiplePoolsPolicyPlugin.PropertyPoolId,
        // "shirako");
        r.setLocalProperty(AntConfig.PropertyXmlFile, configFile);
        r.setRenewable(true);
        PropertiesManager.setElasticTime(rset, true);

        return r;
    }

    /**
     * Returns the actor this reservation belongs to.
     * 
     * @param r
     * @return
     */
    protected String getActor(IReservation r) {
        String msg;
        IActor actor = r.getActor();

        if (actor != null) {
            msg = "Actor: " + actor.getName();
        } else {
            msg = "Actor: <unknown>";
        }

        msg += (" rid #" + r.getReservationID().toHashString());

        return msg;
    }

    /**
     * Calls up to a default broker to query and set the advance time.
     */
    protected void getAdvanceTime() {
        Properties myProps = new Properties();
        myProps.setProperty("advanceTime", "null");

        try {
            Properties returnProps = sm.query(sm.getDefaultBroker(), myProps);
            advanceTime = Long.parseLong(returnProps.getProperty("advanceTime"));
        } catch (Exception e) {
            throw new RuntimeException("Cannot obtain advance time", e);
        }

        advanceTime = advanceTime + SimpleLoadSource.CLOCK_SKEW;
    }

    /**
     * Converts the states to a string.
     * 
     * @param from
     * @param to
     * @return
     */
    protected String getStates(ReservationState from, ReservationState to) {
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
     * Performs the necessary initialization before running the test
     */
    protected void init() throws Exception {
        timeout = 5 * leaseLength;

        StandardContainerManagerObject man = (StandardContainerManagerObject) Globals.getContainer()
                .getContainerManagerObject();
        ResultActorMng result = man.getServiceManagers();

        if (result.getStatus().getCode() != 0) {
            throw new RuntimeException(
                    "An error occurred while obtaining service managers. Exit code: " + result.getStatus().getCode());
        }

        ActorMng[] sms = result.getResult();

        if ((sms == null) || (sms.length == 0)) {
            throw new RuntimeException("No service manager present in the container");
        }

        sm = (IServiceManager) ActorRegistry.getActor(sms[0].getName());

        if (sm == null) {
            throw new RuntimeException("Cannot obtain sm: " + sms[0].getName());
        }

        // slice = (CodSlice) sm.getSlice(SliceName);
        // if (slice == null) {
        ResourceData rdata = new ResourceData();
        slice = SliceFactory.getInstance().create(SliceName, rdata);

        try {
            sm.registerSlice(slice);
        } catch (Exception e) {
            throw new RuntimeException("Cannot register slice", e);
        }

        // }
        getAdvanceTime();
        Globals.getContainer().enableTracing();
        Globals.getContainer().registerReservationListener(reservationListener);

        // We must wait until the broker is ready to accept requests from us.
        // For now we will take a nap for 10seconds.
        // In the future we should come up with a more elegant solution.
        logger.debug("Sleeping for 10 seconds to give the broker a chance to get ready");
        Thread.sleep(10000);
        logger.debug("Ready to start test");
    }

    /**
     * Issues a reservation request.
     */
    protected void issueRequest() {
        IServiceManagerReservation r = createReservation(sm.getCurrentCycle());
        r.registerListener(reservationListener);

        try {
            sm.demand(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        currentReservationSM = r;
    }

    /**
     * Monitors the state transitions of the reservation.
     * 
     * @param close
     *            true if monitoring reservation closing
     * @return
     */
    protected int monitor(boolean close) {
        boolean done = false;
        long start = Globals.getContainer().getCurrentCycle();

        while (!done) {
            long now = Globals.getContainer().getCurrentCycle();

            if ((now - start) > timeout) {
                logger.debug("Timeout while waiting for test to complete");

                return ExitCodeTimeout;
            }

            done = checkDone();

            if (!done) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    throw new RuntimeException("error while sleeping", e);
                }
            }
        }

        return checkFinalStates(close);
    }

    /**
     * Reservation transition event handler
     * 
     * @param r
     * @param from
     * @param to
     */
    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to) {
        IActor actor = r.getActor();
        assert actor != null;

        switch (actor.getType()) {
        case Actor.TypeServiceManager:
            currentStateSM = to;

            break;

        case Actor.TypeBroker:
            currentStateAgent = to;

            if ((currentReservationAgent == null) && r instanceof IBrokerReservation) {
                currentReservationAgent = (IBrokerReservation) r;
            }

            break;

        case Actor.TypeSiteAuthority:
            currentStateSite = to;

            if ((currentReservationSite == null) && r instanceof IAuthorityReservation) {
                currentReservationSite = (IAuthorityReservation) r;
            }

            break;

        default:
            throw new RuntimeException("Invalid actor type");
        }

        checkUnits(r);
        checkExtended(r, from, to);
        logger.debug("Reservation State Machine transition: " + getStates(from, to) + " " + getActor(r));
    }

    /**
     * Run the test
     * 
     * @return
     */
    public int runTest() throws Exception {
        try {
            init();
            issueRequest();

            int code = monitor(false);

            if (code == ExitCodeOK) {
                if (mismatchCount > 0) {
                    code = ExitCodeUnitsError;
                }

                if (currentReservationSM != null) {
                    sm.close(currentReservationSM);

                    int temp = monitor(true);

                    if (code == 0) {
                        code = temp;
                    }
                }
            }

            return code;
        } finally {
            clean();
        }
    }

    /**
     * Sets the bandwidth share
     * 
     * @param bandwidth
     *            the bandwidth to set
     */
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * Sets the client-side ant XML driver file
     * 
     * @param configFile
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * Sets the cpu share
     * 
     * @param cpu
     *            the cpu to set
     */
    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    /**
     * Sets the elastic time flag
     * 
     * @param elasticTime
     *            the elasticTime to set
     */
    public void setElasticTime(boolean elasticTime) {
        this.elasticTime = elasticTime;
    }

    /**
     * Sets the lease length
     * 
     * @param leaseLength
     *            the leaseLength to set
     */
    public void setLeaseLength(long leaseLength) {
        this.leaseLength = leaseLength;
    }

    /**
     * Sets the memory share
     * 
     * @param memory
     *            the memory to set
     */
    public void setMemory(int memory) {
        this.memory = memory;
    }

    /**
     * Sets the resource type
     * 
     * @param rtype
     *            the rtype to set
     */
    public void setRtype(ResourceType rtype) {
        this.rtype = rtype;
    }

    /**
     * Sets the number of units to request
     * 
     * @param units
     *            the units to set
     */
    public void setUnits(int units) {
        this.units = units;
    }
}
