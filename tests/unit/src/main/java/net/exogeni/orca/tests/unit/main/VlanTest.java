package net.exogeni.orca.tests.unit.main;

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

public class VlanTest extends ShirakoTest {
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeUnexpectedState = 30;
    public static final int ExitCodeTimeout = 100;

    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyUnits = "units";

    public static final String VlanSite = "ben-vlan-site";
    public static final String VlanBroker = "ben-vlan-broker";
    public static final String ServiceManager = "service";

    public static final int ADVANCE_TIME = 3;

    /**
     * Lease length in cycles.
     */
    protected int leaseLength = 300;
    /**
     * Timeout interval.
     */
    protected long timeout = 2 * leaseLength;
    /**
     * Number of units.
     */
    protected int units = 1;

    protected IServiceManagerReservation vlanReservation = null;
    protected IServiceManager sm = null;
    protected IBrokerProxy vlanBrokerProxy = null;
    protected ActorClock clock = null;
    protected ISlice slice = null;
    protected String noopConfigFile = null;
    protected int extendCountVlan = 0;

    // note: we can find these using query
    protected ResourceType vlanType = new ResourceType("ben.vlan");

    protected ReservationState stateVlan = null;
    protected ReservationState doneState = new ReservationState(ReservationStates.Active, ReservationStates.None,
            ReservationStates.NoJoin);

    public VlanTest(String[] args) {
        super(args);
    }

    @Override
    protected void readParameters() throws Exception {
        super.readParameters();

        String temp = properties.getProperty(PropertyLeaseLength);

        if (temp != null) {
            leaseLength = PropList.getIntegerProperty(properties, PropertyLeaseLength);
        }
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
        vlanBrokerProxy = sm.getBroker(VlanBroker);
        if (vlanBrokerProxy == null) {
            throw new RuntimeException("missing vlan broker proxy");
        }
        clock = Globals.getContainer().getActorClock();
        noopConfigFile = Globals.LocalRootDirectory + "/handlers/common/noop.xml";
        timeout = 2 * leaseLength;
    }

    protected IServiceManagerReservation getVlanReservation(long cycle) {
        ResourceSet rset = new ResourceSet(1, vlanType);
        Term term = new Term(clock.date(cycle + ADVANCE_TIME), clock.getMillis((long) leaseLength));
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance()
                .create(rset, term, slice, vlanBrokerProxy);

        AntConfig.setServiceXml(r, noopConfigFile);
        r.setRenewable(true);
        return r;
    }

    protected IStateChangeListener reservationListener = new IStateChangeListener() {
        public void transition(Object obj, IState from, IState to) {
            if (obj == vlanReservation) {
                System.out.println("Vlan reservation transition: from " + from + " to " + to);
                if (to.equals(doneState)) {
                    ResourceSet leased = vlanReservation.getLeasedResources();
                    UnitSet uset = (UnitSet) leased.getResources();
                    for (Unit u : uset.getSet()) {
                        System.out.println("Vlan Tag: " + u.getProperty(UnitProperties.UnitVlanTag));
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
        vlanReservation = getVlanReservation(cycle);
        vlanReservation.registerListener(reservationListener);

        try {
            sm.demand(vlanReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vlan reservation", e);
        }

    }

    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to) {
        if (r == vlanReservation) {
            stateVlan = to;
            if (isExtended(to)) {
                extendCountVlan++;
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
        if (isActive(stateVlan)) {
            if (extendCountVlan > 0) {
                return true;
            }
            System.out.println("wating for extension. count=" + extendCountVlan);
        }

        if (isClosed(stateVlan)) {
            return true;
        }

        if (isFailed(stateVlan)) {
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
            if (!(isClosed(stateVlan))) {
                return ExitCodeUnexpectedState;
            }
        } else {
            if (!isActive(stateVlan)) {
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
            code = monitor(true);
            sm.close(vlanReservation);
            code = monitor(true);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed.");
            System.out.println("Test failed");
            System.exit(-1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            VlanTest test = new VlanTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
