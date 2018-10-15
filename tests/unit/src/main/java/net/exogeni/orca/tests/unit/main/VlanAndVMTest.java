package net.exogeni.orca.tests.unit.main;

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

public class VlanAndVMTest extends ShirakoTest {
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeUnexpectedState = 30;
    public static final int ExitCodeTimeout = 100;

    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyUnits = "units";

    public static final String VlanSite = "ben-vlan-site";
    public static final String VlanBroker = "ben-vlan-broker";
    public static final String RenciVMSite = "renci-vm-site";
    public static final String DukeVMSite = "duke-vm-site";
    public static final String VMBroker = "vm-broker";
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
    protected IServiceManagerReservation vmReservationRenci = null;
    protected IServiceManagerReservation vmReservationDuke = null;
    protected IServiceManager sm = null;
    protected IBrokerProxy vmBrokerProxy = null;
    protected IBrokerProxy vlanBrokerProxy = null;
    protected ActorClock clock = null;
    protected ISlice slice = null;
    protected String noopConfigFile = null;
    protected int extendCountVlan = 0;
    protected int extendCountRenci = 0;
    protected int extendCountDuke = 0;

    // note: we can find these using query
    protected ResourceType vlanType = new ResourceType("ben.vlan");
    protected ResourceType vmRenciType = new ResourceType("renci.vm");
    protected ResourceType vmDukeType = new ResourceType("duke.vm");

    protected ReservationState stateVlan = null;
    protected ReservationState stateRenci = null;
    protected ReservationState stateDuke = null;
    protected ReservationState doneState = new ReservationState(ReservationStates.Active, ReservationStates.None,
            ReservationStates.NoJoin);

    public VlanAndVMTest(String[] args) {
        super(args);
    }

    @Override
    protected void readParameters() throws Exception {
        super.readParameters();

        String temp = properties.getProperty(PropertyLeaseLength);

        if (temp != null) {
            leaseLength = PropList.getIntegerProperty(properties, PropertyLeaseLength);
        }

        temp = properties.getProperty(PropertyUnits);

        if (temp != null) {
            units = PropList.getIntegerProperty(properties, PropertyUnits);
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
        vmBrokerProxy = sm.getBroker(VMBroker);
        if (vmBrokerProxy == null) {
            throw new RuntimeException("missing vm broker proxy");
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

    protected IServiceManagerReservation getVMReservation(long cycle, ResourceType type) {
        ResourceSet rset = new ResourceSet(units, type);
        Term term = new Term(clock.date(cycle + ADVANCE_TIME), clock.getMillis((long) leaseLength));
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance()
                .create(rset, term, slice, vmBrokerProxy);

        AntConfig.setServiceXml(r, noopConfigFile);
        r.setRenewable(true);
        return r;
    }

    protected IStateChangeListener reservationListener = new IStateChangeListener() {
        public void transition(Object obj, IState from, IState to) {
            if (obj == vlanReservation) {
                System.out.println("Vlan reservation transition: from " + from + " to " + to);
            } else if (obj == vmReservationRenci) {
                System.out.println("VM reservation (RENCI) transition: from " + from + " to " + to);
            } else if (obj == vmReservationDuke) {
                System.out.println("VM reservation (DUKE) transition: from " + from + " to " + to);
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

        vmReservationRenci = getVMReservation(cycle, vmRenciType);
        vmReservationRenci.registerListener(reservationListener);

        vmReservationDuke = getVMReservation(cycle, vmDukeType);
        vmReservationDuke.registerListener(reservationListener);
        // set the predecessor relationship and the filter
        Properties filter = new Properties();
        filter.setProperty(UnitProperties.UnitVlanTag, UnitProperties.UnitVlanTag);
        vmReservationRenci.addRedeemPredecessor(vlanReservation, filter);
        vmReservationDuke.addRedeemPredecessor(vlanReservation, filter);

        try {
            sm.demand(vlanReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vlan reservation", e);
        }

        try {
            sm.demand(vmReservationRenci);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vm reservation (RENCI)", e);
        }

        try {
            sm.demand(vmReservationDuke);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vm reservation (DUKE)", e);
        }
    }

    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to) {
        if (r == vlanReservation) {
            stateVlan = to;
            if (isExtended(to)) {
                extendCountVlan++;
            }
        } else if (r == vmReservationRenci) {
            stateRenci = to;
            if (isExtended(to)) {
                extendCountRenci++;
            }
        } else if (r == vmReservationDuke) {
            stateDuke = to;
            if (isExtended(to)) {
                extendCountDuke++;
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
        if (isActive(stateVlan) && isActive(stateRenci) && isActive(stateDuke)) {
            if (extendCountRenci > 0 && extendCountVlan > 0 && extendCountDuke > 0) {
                return true;
            }
            System.out.println("wating for extension. vlan=" + extendCountVlan + " renci=" + extendCountRenci + " duke="
                    + extendCountDuke);
        }

        if (isClosed(stateVlan) && isClosed(stateRenci) && isClosed(stateDuke)) {
            return true;
        }

        if (isFailed(stateVlan) || isFailed(stateRenci) || isFailed(stateDuke)) {
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
            if (!(isClosed(stateVlan) && isClosed(stateRenci) && isClosed(stateDuke))) {
                return ExitCodeUnexpectedState;
            }
        } else {
            if (!(isActive(stateVlan) && isActive(stateRenci) && isActive(stateDuke))) {
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
            sm.close(vmReservationDuke);
            sm.close(vmReservationRenci);
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
            VlanAndVMTest test = new VlanAndVMTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
