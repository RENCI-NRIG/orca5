/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.policy.core;

import net.exogeni.orca.shirako.api.IServiceManager;
import net.exogeni.orca.shirako.api.IServiceManagerPolicy;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.kernel.ReservationStates;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.kernel.ServiceManagerReservationFactory;
import net.exogeni.orca.shirako.kernel.SliceFactory;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ResourceType;

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
}
