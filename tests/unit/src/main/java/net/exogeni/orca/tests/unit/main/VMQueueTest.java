package net.exogeni.orca.tests.unit.main;

import net.exogeni.orca.policy.core.util.PropertiesManager;
import net.exogeni.orca.shirako.api.IBrokerProxy;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IServiceManager;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.api.IState;
import net.exogeni.orca.shirako.api.IStateChangeListener;
import net.exogeni.orca.shirako.common.ReservationState;
import net.exogeni.orca.shirako.common.ResourceType;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitSet;
import net.exogeni.orca.shirako.kernel.ReservationStates;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.kernel.ServiceManagerReservationFactory;
import net.exogeni.orca.shirako.meta.UnitProperties;
import net.exogeni.orca.shirako.plugins.config.AntConfig;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.tests.core.ShirakoTest;
import net.exogeni.orca.util.PropList;

public class VMQueueTest extends ShirakoTest {
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeUnexpectedState = 30;
    public static final int ExitCodeTimeout = 100;

    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyUnits = "units";

    public static final String Site = "site";
    public static final String Broker = "broker";
    public static final String ServiceManager = "service";

    public static final int ADVANCE_TIME = 3;

    /**
     * Lease length in cycles.
     */
    protected int leaseLength = 30;
    /**
     * Timeout interval.
     */
    protected long timeout;
    /**
     * Number of units.
     */
    protected int units = 1;

    protected IServiceManagerReservation reservation = null;
    protected IServiceManagerReservation qreservation = null;
    protected IServiceManager sm = null;
    protected IBrokerProxy proxy = null;
    protected ActorClock clock = null;
    protected ISlice slice = null;
    protected String noopConfigFile = null;
    protected int extendCount = 0;

    // note: we can find these using query
    protected ResourceType vmType = new ResourceType("vm");

    protected ReservationState state = null;
    protected ReservationState qstate = null;
    protected ReservationState doneState = new ReservationState(ReservationStates.Active, ReservationStates.None,
            ReservationStates.NoJoin);

    public VMQueueTest(String[] args) {
        super(args);
    }

    @Override
    protected void readParameters() throws Exception {
        super.readParameters();

        String temp = properties.getProperty(PropertyLeaseLength);

        if (temp != null) {
            leaseLength = PropList.getIntegerProperty(properties, PropertyLeaseLength);
        }
        timeout = 5 * leaseLength;
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
    }

    protected IServiceManagerReservation getVMReservation(long cycle) {
        ResourceSet rset = new ResourceSet(1, vmType);
        PropertiesManager.setElasticTime(rset, true);
        Term term = new Term(clock.date(cycle + ADVANCE_TIME), clock.getMillis((long) leaseLength));
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance()
                .create(rset, term, slice, proxy);

        AntConfig.setServiceXml(r, noopConfigFile);
        r.setRenewable(true);
        return r;
    }

    protected IStateChangeListener reservationListener = new IStateChangeListener() {
        public void transition(Object obj, IState from, IState to) {
            if (obj == reservation) {
                System.out.println("VM reservation transition: from " + from + " to " + to);
                if (to.equals(doneState)) {
                    ResourceSet leased = reservation.getLeasedResources();
                    UnitSet uset = (UnitSet) leased.getResources();
                    for (Unit u : uset.getSet()) {
                        System.out.println("unit id=" + u.getID() + " hosted on "
                                + u.getProperty(UnitProperties.UnitParentHostName) + " has ip: "
                                + u.getProperty(UnitProperties.UnitManagementIP));
                    }
                }
            } else if (obj == qreservation) {
                System.out.println("queued VM reservation transition: from " + from + " to " + to);
                if (to.equals(doneState)) {
                    ResourceSet leased = qreservation.getLeasedResources();
                    UnitSet uset = (UnitSet) leased.getResources();
                    for (Unit u : uset.getSet()) {
                        System.out.println("unit id=" + u.getID() + " hosted on "
                                + u.getProperty(UnitProperties.UnitParentHostName) + " has ip: "
                                + u.getProperty(UnitProperties.UnitManagementIP));
                    }
                }
            }

            else {
                System.out.println("Unknown reservation object: " + obj);
            }
            reservationTransition((IReservation) obj, (ReservationState) from, (ReservationState) to);
        }
    };

    protected boolean isExtended(ReservationState state) {
        if (state != null) {
            return (state.getState() == ReservationStates.ActiveTicketed)
                    && (state.getPending() == ReservationStates.None);
        }
        return false;
    }

    protected void issueRequests() {
        long cycle = sm.getCurrentCycle();
        reservation = getVMReservation(cycle);
        reservation.registerListener(reservationListener);
        try {
            sm.demand(reservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand first reservation", e);
        }
        qreservation = getVMReservation(cycle);
        qreservation.registerListener(reservationListener);
        try {
            sm.demand(qreservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand second reservation", e);
        }
    }

    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to) {
        if (r == reservation) {
            state = to;
            if (isExtended(to)) {
                extendCount++;
            }
        } else if (r == qreservation) {
            qstate = to;
            if (isExtended(to)) {
                extendCount++;
            }
        }
    }

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

    protected synchronized boolean checkDone(boolean firstPass) {
        ReservationState toCheck;
        if (firstPass) {
            toCheck = state;
        } else {
            toCheck = qstate;
        }

        if (isActive(toCheck)) {
            if (extendCount > 0) {
                return true;
            }
            System.out.println("wating for extension. extend count=" + extendCount);
        }

        if (isClosed(toCheck)) {
            return true;
        }

        if (isFailed(toCheck)) {
            return true;
        }

        return false;
    }

    protected int monitor(boolean firstPass, boolean close) {
        boolean done = false;
        long start = Globals.getContainer().getCurrentCycle();

        while (!done) {
            long now = Globals.getContainer().getCurrentCycle();

            if ((now - start) > timeout) {
                logger.debug("Timeout while waiting for test to complete");

                return ExitCodeTimeout;
            }

            done = checkDone(firstPass);

            if (!done) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    throw new RuntimeException("error while sleeping", e);
                }
            }
        }

        ReservationState toCheck;
        if (firstPass) {
            toCheck = state;
        } else {
            toCheck = qstate;
        }

        if (close) {
            if (!(isClosed(toCheck))) {
                return ExitCodeUnexpectedState;
            }
        } else {
            if (!isActive(toCheck)) {
                return ExitCodeUnexpectedState;
            }
        }
        return ExitCodeOK;
    }

    @Override
    protected void runTest() {
        try {
            setupTest();
            issueRequests();
            // wait until the first extend
            int code = monitor(true, false);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
            System.out.println("First reservation extended. Closing first reservation");
            // close the first reservation: this should unblock the second
            extendCount = 0;
            sm.close(reservation);
            // wait until the first closes
            code = monitor(true, true);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
            System.out.println("First reservation is closed");
            System.out.println("Waiting for second reservation");
            // wait until the second extends
            code = monitor(false, false);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
            System.out.println("Second reservation extended. Closing second reservation.");
            // close the second
            sm.close(qreservation);
            // wait until the second closes
            code = monitor(true, true);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
            System.out.println("Second reservation closed.");
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed.");
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            VMQueueTest test = new VMQueueTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
