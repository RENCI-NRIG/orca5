package net.exogeni.orca.manage;

import java.util.ArrayList;
import java.util.Properties;

import net.exogeni.orca.manage.beans.LeaseReservationMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.server.OrcaTestServer;
import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.shirako.api.IEventHandler;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.meta.ConfigurationProperties;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.kernel.InboundRPCEvent;
import net.exogeni.orca.shirako.kernel.IncomingRPC;
import net.exogeni.orca.shirako.kernel.OutboundRPCEvent;
import net.exogeni.orca.shirako.kernel.RPCRequest;
import net.exogeni.orca.shirako.kernel.ReservationDatabaseUpdateEvent;
import net.exogeni.orca.shirako.kernel.ReservationStateTransitionEvent;
import net.exogeni.orca.shirako.kernel.ReservationStates;
import net.exogeni.orca.shirako.util.ReservationState;
import net.exogeni.orca.shirako.util.TestException;
import net.exogeni.orca.util.OrcaException;

import org.junit.Assert;

public class ReservationChecker extends RecoveryTest {
    public static boolean PrintEventStack = true;

    private boolean conditionReached = false;
    private Object conditionLock = new Object();

    private IEventHandler handler = new IEventHandler() {
        public void handle(IEvent event) {
            if (event instanceof ReservationStateTransitionEvent) {
                handleStateTransition((ReservationStateTransitionEvent) event);
            } else if (event instanceof OutboundRPCEvent) {
                handleRPC((OutboundRPCEvent) event);
            } else if (event instanceof InboundRPCEvent) {
                handleRPC((InboundRPCEvent) event);
            } else if (event instanceof ReservationDatabaseUpdateEvent) {
                handleDbUpdate((ReservationDatabaseUpdateEvent) event);
            } else {
                System.out.println("Received event: " + event.getClass().getName());
            }

            if (PrintEventStack) {
                try {
                    throw new OrcaException();
                } catch (OrcaException e) {
                    e.printStackTrace(System.out);
                }
            }
            if (!conditionReached && condition.matches(event)) {
                System.out.println("***** Reached desired failure condition ****");
                synchronized (conditionLock) {
                    conditionReached = true;
                    conditionLock.notify();
                }
                throw new TestException("Forcing error");
            }
        }
    };

    private FailureCondition condition;

    public ReservationChecker(FailureCondition condition) {
        this.condition = condition;
    }

    protected void handleStateTransition(ReservationStateTransitionEvent event) {
        IReservation r = event.getReservation();
        System.out.println("(transition) actor: " + r.getActor().getName() + " " + r.getClass().getSimpleName() + "  #"
                + r.getReservationID().toHashString() + " in slice " + r.getSlice().getName() + " state: "
                + r.getReservationState());

    }

    protected void handleDbUpdate(ReservationDatabaseUpdateEvent event) {
        IReservation r = event.getReservation();
        System.out.println("(db: " + (event.IsBefore() ? "before" : "after") + ") actor: " + r.getActor().getName()
                + " " + r.getClass().getSimpleName() + "  #" + r.getReservationID().toHashString() + " in slice "
                + r.getSlice().getName() + " state: " + r.getReservationState());
    }

    protected void handleRPC(OutboundRPCEvent event) {
        RPCRequest request = event.getRequest();
        System.out.println("(out) actor: " + request.getActor().getName() + " type " + request.getRequestType());
    }

    protected void handleRPC(InboundRPCEvent event) {
        IncomingRPC request = event.GetRequest();
        System.out.println("(in) actor: " + event.getActor().getName() + " type " + request.getRequestType());
    }

    protected void awaitCondition() throws InterruptedException {
        synchronized (conditionLock) {
            while (!conditionReached) {
                conditionLock.wait();
            }
        }
    }

    public void doIt() throws Exception {
        Globals.eventManager.createPrivilegedSubscription(handler);
        OrcaTestServer.startServer();
        awaitNoPendingReservations();
        System.out.println("Container ready");

        // create our test slice
        IOrcaContainer cont = connect();
        IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);
        createTestSlice(sm);

        SliceMng slice = getTestSlice(sm);

        TicketReservationMng r = new LeaseReservationMng();
        r.setStart(System.currentTimeMillis());
        r.setEnd(System.currentTimeMillis() + 1000 * 20);
        r.setUnits(1);
        r.setResourceType("foo");
        r.setSliceID(slice.getSliceID());
        Properties local = new Properties();
        local.setProperty(ConfigurationProperties.ConfigHandler, "common/noopd.xml");
        r.setLocalProperties(OrcaConverter.fill(local));

        ReservationID rid = sm.addReservation(r);
        Assert.assertNotNull(rid);
        System.out.println("Created reservation: rid=" + rid.toHashString());

        if (condition instanceof ReservationTransitionFailureCondition) {
            ((ReservationTransitionFailureCondition) condition).setReservationID(rid);
        }

        Assert.assertTrue(sm.demand(rid));

        awaitCondition();

        OrcaTestServer.stopServer();
        System.out.println("Container stopped");

        System.out.println("Recoverying container");
        OrcaTestServer.startOrRecoverServer();

        Assert.assertTrue(OrcaConverter.awaitActive(rid, sm));
        System.out.println("The reservation became active");

        LeaseReservationMng lease = (LeaseReservationMng) sm.getReservation(rid);
        Assert.assertEquals(1, lease.getLeasedUnits());
        Assert.assertEquals(ReservationStates.Active, lease.getState());

        // close the reservation
        System.out.println("Closing the reservation");
        sm.closeReservation(rid);
        System.out.println("Waiting for the reservation to close");
        Assert.assertTrue(OrcaConverter.awaitClosed(rid, sm));
        System.out.println("The reservation is now closed");

        OrcaTestServer.stopServer();
        System.out.println("Container stopped");
    }

    private static void checkCondition(FailureCondition c) throws Exception {
        ReservationChecker checker = new ReservationChecker(c);
        checker.doIt();
    }

    private static void checkSM() throws Exception {
        ArrayList<FailureCondition> list = new ArrayList<FailureCondition>();
        // list.add(new ReservationTransitionFailureCondition(SM_GUID,
        // new ReservationState(ReservationStates.Nascent,
        // ReservationStates.Ticketing,
        // ReservationStates.NoJoin)));

        // list.add(new ReservationTransitionFailureCondition(SM_GUID,
        // new ReservationState(ReservationStates.Ticketed,
        // ReservationStates.None,
        // ReservationStates.NoJoin)));

        // list.add(new ReservationTransitionFailureCondition(SM_GUID,
        // new ReservationState(ReservationStates.Ticketed,
        // ReservationStates.None,
        // ReservationStates.BlockedRedeem)));

        // list.add(new ReservationTransitionFailureCondition(SM_GUID,
        // new ReservationState(ReservationStates.Ticketed,
        // ReservationStates.Redeeming,
        // ReservationStates.NoJoin)));

        // list.add(new ReservationTransitionFailureCondition(SM_GUID,
        // new ReservationState(ReservationStates.Active,
        // ReservationStates.None,
        // ReservationStates.BlockedJoin)));

        list.add(new ReservationTransitionFailureCondition(SM_GUID,
                new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.Joining)));

        for (int i = 0; i < list.size(); ++i) {
            checkCondition(list.get(i));
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("ORCA_HOME", "net.exogeni.orca");
        checkSM();
    }
}
