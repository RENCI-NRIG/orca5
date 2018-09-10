package net.exogeni.orca.tests.unit.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

public class RedeemPredecessorTest extends ShirakoTest {
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeUnexpectedState = 30;
    public static final int ExitCodeTimeout = 100;

    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyUnits = "units";

    public static final String Site = "site";
    public static final String Broker = "broker";
    public static final String ServiceManager = "service";
    public static final String TestProperty = "test.property";
    public static final int ADVANCE_TIME = 3;
    /**
     * Lease length in cycles.
     */
    protected int leaseLength = 30;
    /**
     * Timeout interval.
     */
    protected long timeout = 2 * leaseLength;
    /**
     * Number of units.
     */
    protected int units = 2;
    /**
     * Number of predecessors.
     */
    protected int numPreds = 2;

    protected IServiceManagerReservation reservation = null;
    protected List<IServiceManagerReservation> preds = new ArrayList<IServiceManagerReservation>();
    protected IServiceManager sm = null;
    protected IBrokerProxy proxy = null;
    protected ActorClock clock = null;
    protected ISlice slice = null;
    protected String noopConfigFile = null;
    protected int extendCount = 0;

    // note: we can find these using query
    protected ResourceType vmType = new ResourceType("vm");

    protected ReservationState state = null;
    protected ReservationState doneState = new ReservationState(ReservationStates.Active, ReservationStates.None,
            ReservationStates.NoJoin);
    protected ReservationState redeemingState = new ReservationState(ReservationStates.Ticketing,
            ReservationStates.Redeeming, ReservationStates.NoJoin);

    public RedeemPredecessorTest(String[] args) {
        super(args);
    }

    @Override
    protected void readParameters() throws Exception {
        super.readParameters();

        String temp = properties.getProperty(PropertyLeaseLength);

        if (temp != null) {
            leaseLength = PropList.getIntegerProperty(properties, PropertyLeaseLength);
        }
        timeout = 2 * leaseLength;
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
        ResourceSet rset = new ResourceSet(units, vmType);
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
                } else if (to.equals(redeemingState)) {
                    Properties config = reservation.getResources().getConfigurationProperties();
                    System.out.println("Configuration properties: " + config);
                    String value = config.getProperty(TestProperty);
                    if (value == null) {
                        System.out.println("Missing property");
                        System.exit(1);
                    }
                    String[] temp = value.split(",");
                    if (temp.length != numPreds * units) {
                        System.out.println("Expected: " + numPreds * units + " entries but found: " + temp.length);
                        System.exit(1);
                    }
                }
            } else {
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

        for (int i = 0; i < numPreds; i++) {
            IServiceManagerReservation r = getVMReservation(cycle);
            Properties filter = new Properties();
            filter.setProperty(UnitProperties.UnitManagementIP, TestProperty);
            reservation.addRedeemPredecessor(r, filter);
            preds.add(r);
        }

        try {
            sm.demand(reservation);
            for (IServiceManagerReservation r : preds) {
                sm.demand(r);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vlan reservation", e);
        }

    }

    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to) {
        if (r == reservation) {
            state = to;
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

    protected synchronized boolean checkDone() {
        if (isActive(state)) {
            if (extendCount > 0) {
                return true;
            }
            System.out.println("wating for extension. extend count=" + extendCount);
        }

        if (isClosed(state)) {
            return true;
        }

        if (isFailed(state)) {
            return true;
        }

        return false;
    }

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

        if (close) {
            if (!(isClosed(state))) {
                return ExitCodeUnexpectedState;
            }
        } else {
            if (!isActive(state)) {
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
            int code = monitor(false);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
            sm.close(reservation);
            code = monitor(true);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed.");
            System.out.println("Test failed: " + e.getMessage());
            System.exit(-1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            RedeemPredecessorTest test = new RedeemPredecessorTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
