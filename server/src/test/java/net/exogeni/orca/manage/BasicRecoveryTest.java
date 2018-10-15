package net.exogeni.orca.manage;

import java.util.Properties;

import net.exogeni.orca.manage.beans.EventMng;
import net.exogeni.orca.manage.beans.LeaseReservationMng;
import net.exogeni.orca.manage.beans.ReservationStateTransitionEventMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.server.OrcaTestServer;
import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.shirako.api.IEventHandler;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.meta.ConfigurationProperties;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.kernel.RPCManager;

import org.junit.Assert;
import org.junit.Test;

public class BasicRecoveryTest extends RecoveryTest {

    @Test
    // Tests boot with all claims completing before the shutdown
    public void test_Basic() throws Exception {
        // start fresh
        OrcaTestServer.startServer();
        awaitNoPendingReservations();

        // all claims should be complete now
        validateState();
        OrcaTestServer.stopServer();

        // recovery
        OrcaTestServer.startOrRecoverServer();
        awaitNoPendingReservations();
        validateState();
        // cleanup
        OrcaTestServer.stopServer();
    }

    @Test
    // Tests boot with shutdown before the site sends the claim response
    public void test_PendingClaim() throws Exception {

        // start fresh
        OrcaTestServer.startServer();
        RPCManager.awaitNothingPending();
        OrcaTestServer.stopServer();

        OrcaTestServer.startOrRecoverServer();
        awaitNoPendingReservations();
        validateState();
        OrcaTestServer.stopServer();
    }

    @Test
    // Tests recovery and issuing a reservation after recovery
    public void test_ReservationAfterRecover() throws Exception {
        IEventHandler handler = new IEventHandler() {
            public void handle(IEvent event) {
                System.out.println("Received event: " + event.getClass().getName());
            }
        };

        Globals.eventManager.createPrivilegedSubscription(handler);

        // start fresh
        OrcaTestServer.startServer();
        awaitNoPendingReservations();
        // all claims should be complete now

        // create our test slice
        IOrcaContainer cont = connect();
        IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);
        createTestSlice(sm);

        // shut the server down
        OrcaTestServer.stopServer();

        // recover
        OrcaTestServer.startOrRecoverServer();
        awaitNoPendingReservations();

        cont = connect();
        sm = cont.getServiceManager(SM_GUID);
        oneReservation(sm);

        OrcaTestServer.stopServer();
    }

    private void validateState() throws Exception {
    }

    public void oneReservation(IOrcaServiceManager sm) throws Exception {
        final Object testDone = new Object();

        IOrcaEventHandler handler = new IOrcaEventHandler() {
            public void handle(EventMng e) {
                System.out.println("Received an event: " + e.getClass().getName());
                if (e instanceof ReservationStateTransitionEventMng) {
                    ReservationStateTransitionEventMng ste = (ReservationStateTransitionEventMng) e;
                    System.out.println("Reservation #" + ste.getReservationId() + " transitioned into: "
                            + OrcaConverter.getState(ste.getState()));
                    if (OrcaConverter.hasNothingPending(ste.getState()) && OrcaConverter.isActive(ste.getState())
                            && !OrcaConverter.isActiveTicketed(ste.getState())) {
                        synchronized (testDone) {
                            System.out.println("Reservation is active.");
                            testDone.notify();
                        }
                    }
                }
            }

            public void error(OrcaError error) {
                System.err.println("An error occurred: " + error);
            }
        };

        LocalEventManager m = new LocalEventManager(sm, handler);
        m.start();

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

        Assert.assertTrue(sm.demand(rid));

        System.out.println("Waiting for the reservations to become active");
        Assert.assertTrue(OrcaConverter.awaitActive(rid, sm));
        System.out.println("The reservation became active");

        LeaseReservationMng lease = (LeaseReservationMng) sm.getReservation(rid);
        Assert.assertEquals(1, lease.getLeasedUnits());
        // close the reservation
        System.out.println("Closing the reservation");
        sm.closeReservation(rid);
        System.out.println("Waiting for the reservation to close");
        Assert.assertTrue(OrcaConverter.awaitClosed(rid, sm));
        System.out.println("The reservation is now closed");

        m.stop();
    }
}
