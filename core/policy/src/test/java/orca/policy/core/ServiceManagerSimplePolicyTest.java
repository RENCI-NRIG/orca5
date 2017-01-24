/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.policy.core;

import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerPolicy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.container.Globals;
import orca.shirako.kernel.*;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.util.ResourceType;

import static java.lang.Thread.sleep;

/**
 * Unit tests for <code>ServiceManagerSimplePolicy</code>
 */
public class ServiceManagerSimplePolicyTest extends ServiceManagerPolicyTest {

    @Override
    public IServiceManagerPolicy getSMPolicy() throws Exception {
        IServiceManagerPolicy policy = new ServiceManagerSimplePolicyTestWrapper();

        return policy;
    }

    public void testCreate() throws Exception {
        IServiceManager sm = (IServiceManager) getSM();
        for (int i = 1; i <= 100; i++) {
            sm.externalTick((long) i);
        }
    }

    public void testDemand() throws Exception {
        IServiceManager sm = (IServiceManager) getSM();

        ActorClock clock = sm.getActorClock();
        Term.clock = clock;

        ResourceSet resources = new ResourceSet(1, new ResourceType(1));
        ISlice slice = SliceFactory.getInstance().create("myslice");
        sm.registerSlice(slice);

        long start = 5;
        long end = 10;

        Term term = new Term(clock.cycleStartDate(start), clock.cycleEndDate(end));

        IServiceManagerReservation r = ServiceManagerReservationFactory.getInstance().create(resources, term, slice);

        r.setRenewable(false);

        sm.register(r);
        sm.demand(r.getReservationID());

        for (int i = 1; i <= end+2; i++) {
            sm.externalTick((long) i);
            //System.out.println("i: " + i + " r.getState() = " + r.getState());
            while (sm.getCurrentCycle() != i){
                sleep(1);
            }

            if ((i >= start) && (i < (end - 1))) {
                assertTrue(r.getState() == ReservationStates.Active);
            }

            if (i > end) {
                assertTrue(r.getState() == ReservationStates.Closed);
            }
        }
    }

    public void testFail() throws Exception {
        IServiceManager sm = (IServiceManager) getSM();

        ActorClock clock = sm.getActorClock();
        Term.clock = clock;

        ResourceSet resources = new ResourceSet(1, new ResourceType(1));
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
                assertTrue(r1.getState() == ReservationStates.Failed);
                assertTrue(r2.getState() == ReservationStates.Failed);
            }

            if (i > end) {
                //assertTrue(r2.getState() == ReservationStates.Closed);
            }
        }

        IKernelSlice kernelSlice = (IKernelSlice) r1.getSlice();
        //kernelSlice.
    }

    public void testNascent() throws Exception {
        IServiceManager sm = (IServiceManager) getSM();

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

            if ( (i == start-2) && !r2demanded ){
                assertTrue(r1.getState() == ReservationStates.Ticketed);
                assertTrue(r2.getState() == ReservationStates.Nascent);
                sm.demand(r2.getReservationID());
                r2demanded = true;
            }

            if ((i >= start) && (i < (end - 1))) {
                assertTrue(r1.getState() == ReservationStates.Active);
                assertTrue(r2.getState() == ReservationStates.Active);
            }

            if (i > end) {
                assertTrue(r1.getState() == ReservationStates.Closed);
                assertTrue(r2.getState() == ReservationStates.Closed);
            }
        }

    }

}
