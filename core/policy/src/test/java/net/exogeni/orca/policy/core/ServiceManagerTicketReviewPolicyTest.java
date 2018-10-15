package net.exogeni.orca.policy.core;

import net.exogeni.orca.shirako.api.IServiceManager;
import net.exogeni.orca.shirako.api.IServiceManagerPolicy;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.kernel.*;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ResourceType;

import static java.lang.Thread.sleep;

public class ServiceManagerTicketReviewPolicyTest extends ServiceManagerPolicyTest {

    @Override
    public IServiceManagerPolicy getSMPolicy() throws Exception {
        return new ServiceManagerTicketReviewPolicyTestWrapper();
    }

    /**
     * Test where two reservations are requested.
     * One is failed by the broker, and the other is subsequently failed by the SM
     *
     * @throws Exception
     */
    public void testFail() throws Exception {
        IServiceManager sm = getSM();

        ActorClock clock = sm.getActorClock();
        Term.clock = clock;

        ResourceSet resources = new ResourceSet(1, new ResourceType(1));

        // slice name must start with 'fail' for test to run correctly.
        // see ServiceManagerTestWrapper.bid()
        ISlice slice = SliceFactory.getInstance().create("fail");
        sm.registerSlice(slice);

        long start = 5;
        long end = 10;

        Term term = new Term(clock.cycleStartDate(start), clock.cycleEndDate(end));

        // one reservation
        IServiceManagerReservation r1 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r1.setRenewable(false);
        sm.register(r1);
        sm.demand(r1.getReservationID());

        // second reservation
        IServiceManagerReservation r2 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r2.setRenewable(false);
        sm.register(r2);
        sm.demand(r2.getReservationID());

        for (int i = 1; i <= end+2; i++) {
            sm.externalTick((long) i);
            //System.out.println("i: " + i + " r.getState() = " + r.getState());
            while (sm.getCurrentCycle() != i){
                sleep(1);
            }

            if ((i >= start) && (i < (end - 1))) {
                assertTrue(r1.isClosed());
                assertTrue(r2.isClosed());
            }

        }

    }

    /**
     * Test where two reservations are requested.
     * One of the reservations is not demanded, and thus remains Nascent.
     * The ticketed reservation is not redeemed and made active until the second reservation is no longer Nascent.
     *
     * @throws Exception
     */
    public void testNascent() throws Exception {
        IServiceManager sm = getSM();

        ActorClock clock = sm.getActorClock();
        Term.clock = clock;

        ResourceSet resources = new ResourceSet(1, new ResourceType(1));
        ISlice slice = SliceFactory.getInstance().create("nascent");
        sm.registerSlice(slice);

        long start = 5;
        long end = 10;

        Term term = new Term(clock.cycleStartDate(start), clock.cycleEndDate(end));

        // one reservation
        IServiceManagerReservation r1 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r1.setRenewable(false);
        sm.register(r1);
        sm.demand(r1.getReservationID());

        // second reservation
        IServiceManagerReservation r2 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r2.setRenewable(false);
        sm.register(r2);
        //sm.demand(r2.getReservationID()); // don't ticket this until later
        boolean r2demanded = false;


        for (int i = 1; i <= end+2; i++) {
            sm.externalTick((long) i);
            //System.out.println("i: " + i + " r.getState() = " + r.getState());
            while (sm.getCurrentCycle() != i){
                sleep(1);
            }

            if ( (i == start-3) && !r2demanded ){
                assertTrue(r1.isTicketed());
                assertTrue(r2.isNascent());
                sm.demand(r2.getReservationID());
                r2demanded = true;
            }

            if ((i >= start) && (i < (end - 1))) {
                assertTrue(r1.isActive());
                assertTrue(r2.isActive());
                //System.out.println("i: " + i + " r1.getState() = " + r1.getState() + " r2.getState() = " + r2.getState());
            }

            if (i > end) {
                assertTrue(r1.isClosed());
                assertTrue(r2.isClosed());
            }
        }

    }

    /**
     * Test where three reservations are requested.
     * One of the tickets is failed.
     * One of the reservations is not demanded, and thus remains Nascent.
     * The failed and ticketed reservations will not be Closed until the final reservation is no longer Nascent.
     *
     * @throws Exception
     */
    public void testFailAndNascent() throws Exception {
        IServiceManager sm = getSM();

        ActorClock clock = sm.getActorClock();
        Term.clock = clock;

        ResourceSet resources = new ResourceSet(1, new ResourceType(1));

        // slice name must start with 'fail' for test to run correctly.
        // see ServiceManagerTestWrapper.bid()
        ISlice slice = SliceFactory.getInstance().create("fail_nascent");
        sm.registerSlice(slice);

        long start = 10;
        long end = 15;

        Term term = new Term(clock.cycleStartDate(start), clock.cycleEndDate(end));

        // one reservation
        IServiceManagerReservation r1 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r1.setRenewable(false);
        sm.register(r1);
        sm.demand(r1.getReservationID());

        // second reservation
        IServiceManagerReservation r2 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r2.setRenewable(false);
        sm.register(r2);
        //sm.demand(r2.getReservationID()); // don't ticket this until later
        boolean r2demanded = false;

        // third reservation
        IServiceManagerReservation r3 = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);
        r3.setRenewable(false);
        sm.register(r3);
        sm.demand(r3.getReservationID());


        for (int i = 1; i <= end+2; i++) {
            sm.externalTick((long) i);
            //System.out.println("i: " + i + " r.getState() = " + r.getState());
            while (sm.getCurrentCycle() != i){
                sleep(1);
            }

            // give plenty of time to make sure the Failed ticket is not closed until Nascent is resolved
            if ( i>2 && !r2demanded ){
                assertTrue(r1.isFailed());
                assertTrue(r2.isNascent());
                assertTrue(r3.isTicketed());

                if (i>6) {
                    sm.demand(r2.getReservationID());
                    r2demanded = true;
                }
            }

            if ((i >= start) && (i < (end - 1))) {
                //System.out.println("i: " + i + " r1.getState() = " + r1.getState() + " r2.getState() = " + r2.getState() + " r3.getState() = " + r3.getState());
                assertTrue(r1.isClosed());
                assertTrue(r2.isClosed());
                assertTrue(r3.isClosed());
            }

            if (i > end) {
                assertTrue(r1.isClosed());
                assertTrue(r2.isClosed());
                assertTrue(r3.isClosed());
            }
        }

    }

}
