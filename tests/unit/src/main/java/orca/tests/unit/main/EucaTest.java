package orca.tests.unit.main;

import java.io.File;
import java.io.FileInputStream;

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

public class EucaTest extends ShirakoTest {
    public static final int ExitCodeOK = 0;
    public static final int ExitCodeUnitsError = 20;
    public static final int ExitCodeUnexpectedState = 30;
    public static final int ExitCodeTimeout = 100;

    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyUnits = "units";
    public static final String PropertySshKey = "ssh.key";

    public static final String Site = "site";
    public static final String Broker = "broker";
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

    protected IServiceManagerReservation reservation = null;
    protected IServiceManager sm = null;
    protected IBrokerProxy proxy = null;
    protected ActorClock clock = null;
    protected ISlice slice = null;
    protected String noopConfigFile = null;
    protected int extendCount = 0;
    protected String sshKey = null;

    // note: we can find these using query
    protected ResourceType vmType = new ResourceType("vm");

    protected ReservationState state = null;
    protected ReservationState doneState = new ReservationState(ReservationStates.Active, ReservationStates.None,
            ReservationStates.NoJoin);

    public EucaTest(String[] args) {
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

        sshKey = properties.getProperty(PropertySshKey);
        if (sshKey != null) {
            sshKey = readFileAsString(sshKey);
        }
    }

    private static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        FileInputStream f = new FileInputStream(filePath);
        f.read(buffer);
        return new String(buffer);
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
        Term term = new Term(clock.date(cycle + ADVANCE_TIME), clock.getMillis((long) leaseLength));
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance()
                .create(rset, term, slice, proxy);

        if (sshKey != null) {
            r.setConfigurationProperty(ConfigurationProperties.ConfigSSHKey, sshKey);
        }

        r.setConfigurationProperty(UnitProperties.UnitVlanTag, "25");
        r.setConfigurationProperty(UnitProperties.UnitVlanHostEth, "eth0");
        // r.setConfigurationProperty("unit.eth1.vlan.tag", "20");
        // For physical
        // r.setConfigurationProperty("unit.eth1.mode", "phys");
        // r.setConfigurationProperty("unit.eth1.hosteth", "eth0");
        // r.setConfigurationProperty("unit.eth1.ip", "2.2.2.2/24");

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
                        System.out.println(
                                "unit id=" + u.getID() + " ec2instace: " + u.getProperty(UnitProperties.UnitEC2Instance)
                                        + " has management ip: " + u.getProperty(UnitProperties.UnitManagementIP));
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
        try {
            sm.demand(reservation);
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
            System.out.println("Closing reservation");
            sm.close(reservation);
            code = monitor(true);
            if (code != ExitCodeOK) {
                throw new Exception("monitor returned non-zero code: " + code);
            }
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed.");
            System.out.println("Test failed: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            EucaTest test = new EucaTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
