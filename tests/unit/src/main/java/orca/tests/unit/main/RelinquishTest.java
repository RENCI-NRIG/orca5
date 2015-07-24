package orca.tests.unit.main;

import org.apache.tools.ant.taskdefs.Sleep;

import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.api.IState;
import orca.shirako.api.IStateChangeListener;
import orca.shirako.common.ReservationState;
import orca.shirako.common.ResourceType;
import orca.shirako.container.Globals;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.meta.ConfigurationProperties;
import orca.shirako.meta.UnitProperties;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.tests.core.ShirakoTest;
import orca.util.PropList;

public class RelinquishTest extends ShirakoTest {
    public static final String Site = "site";
    public static final String Broker = "broker";
    public static final String ServiceManager = "service";

    public static final int ADVANCE_TIME = 3;

    /**
     * Lease length in cycles.
     */
    protected int leaseLength = 100;
    /**
     * Timeout interval.
     */
    protected long timeout = 2 * leaseLength;
    /**
     * Number of units.
     */
    protected int units = 1;

    protected IServiceManagerReservation reservation = null;
    protected IServiceManager sm = null;
    protected IBrokerProxy proxy = null;
    protected ActorClock clock = null;
    protected ISlice slice = null;
    protected String noopConfigFile = null;
    protected ReservationState cancelState = null;    
    protected Object mutex = new Object();
    protected boolean done = false;
    protected ResourceType resourceType = new ResourceType("vm");

    protected ReservationState state = null;
    protected ReservationState doneState = new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin);

    public RelinquishTest(String[] args) {
        super(args);
    }

    protected void setupTest() {
        sm = (IServiceManager) ActorRegistry.getActor(ServiceManager);
        if (sm == null) {
            throw new RuntimeException("missing service manager actor");
        }
        slice = sm.getSlices()[0];
        if (slice == null) {
            throw new RuntimeException("missing slice");
        }
        proxy = sm.getBroker(Broker);
        if (proxy == null) {
            throw new RuntimeException("missing broker proxy");
        }
        clock = Globals.getContainer().getActorClock();
        noopConfigFile = Globals.LocalRootDirectory + "/handlers/common/noop.xml";
        timeout = 2 * leaseLength;
    }

    protected IServiceManagerReservation getReservation(long cycle) {
        ResourceSet rset = new ResourceSet(1, resourceType);
        Term term = new Term(clock.date(cycle + ADVANCE_TIME), clock.getMillis((long) leaseLength));
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance().create(rset, term, slice, proxy);
        AntConfig.setServiceXml(r, noopConfigFile);
        r.setRenewable(true);
        return r;
    }

    protected IStateChangeListener reservationListener = new IStateChangeListener() {
        public void transition(Object obj, IState from, IState to) {
            if (obj == reservation) {
                System.out.println("VM reservation transition: from " + from + " to " + to);
                state = (ReservationState) to;   
                boolean cancelled = false;
                if (cancelState != null && cancelState.equals(state)) {
                    cancelState = null;
                    cancelled = true;
                    try {
                        Thread.sleep(1000);
                        System.out.println("Cancelling reservation in state: " + to);
                        sm.close(reservation);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                if (!cancelled && (isClosed(state) || isActive(state))) {
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            } else {
                System.out.println("Unknown reservation object: " + obj);
            }
        }
    };

    protected boolean isClosed(ReservationState state) {
        if (state == null) {
            return false;
        }
        return state.getState() == ReservationStates.Closed;
    }

    protected boolean isFailed(ReservationState state) {
        if (state == null) {
            return false;
        }
        return state.getState() == ReservationStates.Failed;
    }

    protected boolean isActive(ReservationState state) {
        if (state == null) {
            return false;
        }
        return doneState.equals(state);
    }

    protected void testCancel(ReservationState instate) throws Exception{
        System.out.println("Testing cancel in state: " + instate);       
        // isue the reservation to be cancelled
        cancelState = instate;
        long cycle = sm.getCurrentCycle();
        reservation = getReservation(cycle);
        reservation.registerListener(reservationListener);
        state = new ReservationState(ReservationStates.Nascent, ReservationStates.None, ReservationStates.NoJoin);
        try {
            sm.demand(reservation);
            if (state.equals(cancelState)) {
                System.out.println("Cancelling reservation in state: " + state);
                sm.close(reservation);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand reservation", e);
        }
        synchronized (mutex) {
            while (!(isActive(state) || isClosed(state) || isFailed(state))) {
                mutex.wait();
            }
        }
        if (!isClosed(state)) {
            throw new RuntimeException("First reservation did not close properly");
        }
        System.out.println("First reservation cancelled");
        
        // issue the second reservation: it must become active
        cycle = sm.getCurrentCycle();
        reservation = getReservation(cycle);
        reservation.registerListener(reservationListener);
        try {
            sm.demand(reservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vlan reservation", e);
        }
        state = new ReservationState(ReservationStates.Nascent, ReservationStates.None, ReservationStates.NoJoin);
        synchronized (mutex) {
            while (!(isActive(state) || isClosed(state) || isFailed(state))) {
                mutex.wait();
            }
        }
        if (!isActive(state)) {
            throw new RuntimeException("Second reservation did not become active");
        }
        System.out.println("Second reservation became active");
        // close the second reservation
        sm.close(reservation);
        synchronized (mutex) {
            while (!(isClosed(state) || isFailed(state))) {
                mutex.wait();
            }
        }
        if (!isClosed(state)) {
            throw new RuntimeException("Second reservation did not close properly");
        }
    }
    
    @Override
    protected void runTest() {
        try {
            setupTest();
            testCancel(new ReservationState(ReservationStates.Nascent, ReservationStates.None, ReservationStates.NoJoin));
            testCancel(new ReservationState(ReservationStates.Nascent, ReservationStates.Ticketing, ReservationStates.NoJoin));
            testCancel(new ReservationState(ReservationStates.Ticketed, ReservationStates.None, ReservationStates.NoJoin));
            testCancel(new ReservationState(ReservationStates.Ticketed, ReservationStates.None, ReservationStates.BlockedRedeem));
            testCancel(new ReservationState(ReservationStates.Ticketed, ReservationStates.Redeeming, ReservationStates.NoJoin));
            testCancel(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed.");
            System.out.println("Test failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            RelinquishTest test = new RelinquishTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
