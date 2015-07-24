/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.policy.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import orca.policy.core.util.LogicalInventory;
import orca.policy.core.util.MachineState;
import orca.policy.core.util.ResourceProperties;
import orca.shirako.api.IBroker;
import orca.shirako.api.IBrokerPolicy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.delegation.DelegationException;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.BrokerReservationFactory;
import orca.shirako.kernel.ClientReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;

public class BrokerWorstFitPolicyTest extends BrokerPolicyTest {
    public static final int Units = 100;
    public static final int UnitsCPU = 100;
    public static final int UnitsMemory = 1024;
    public static final int UnitsBandwidth = 1000;
    public static final int UnitsStorage = 1000;

    @Override
    public IBrokerPolicy getBrokerPolicy() throws Exception {
        BrokerWorstFitPolicy policy = new BrokerWorstFitPolicy();

        return policy;
    }

    protected IBrokerReservation getRequest(IBrokerReservation request, int units, ResourceType type, Date start, Date end) {
        // resource set
        ResourceSet set = new ResourceSet(units, type);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyCpu, UnitsCPU);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyCpu, UnitsCPU);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyMemory, UnitsMemory);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyMemory, UnitsMemory);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyBandwidth, UnitsBandwidth);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyBandwidth, UnitsBandwidth);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyUnits, units);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyUnits, units);

        // term
        Term term = new Term(request.getTerm().getStartTime(), end, start);

        // reservation
        IBrokerReservation result = BrokerReservationFactory.getInstance().create(request.getReservationID(), set, term, request.getSlice());
        result.setSequenceIn(request.getSequenceIn() + 1);

        return result;
    }

    protected IBrokerReservation getRequest(int units, ResourceType type, Date start, Date end) {
        // create the test slice
        ISlice slice = SliceFactory.getInstance().create("test-slice");

        // resource set
        ResourceSet set = new ResourceSet(units, type);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyCpu, UnitsCPU);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyCpu, UnitsCPU);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyMemory, UnitsMemory);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyMemory, UnitsMemory);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyBandwidth, UnitsBandwidth);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyBandwidth, UnitsBandwidth);
        ResourceProperties.setMax(set.getRequestProperties(), ResourceProperties.PropertyUnits, units);
        ResourceProperties.setMin(set.getRequestProperties(), ResourceProperties.PropertyUnits, units);

        // term
        Term term = new Term(start, end);

        // reservation
        IBrokerReservation request = BrokerReservationFactory.getInstance().create(set, term, slice);

        return request;
    }

    protected IClientReservation getSource(int units, ResourceType type, IBroker broker, ISlice slice) throws DelegationException, TicketException {
        Date start = broker.getActorClock().cycleStartDate(DonateStartCycle);
        Date end = broker.getActorClock().cycleEndDate(DonateEndCycle);
        Term term = new Term(start, end);

        ResourceSet resources = new ResourceSet(units, type);

        Properties p = resources.getResourceProperties();
        PropList.setProperty(p, ResourceProperties.PropertyCpu, UnitsCPU);
        PropList.setProperty(p, ResourceProperties.PropertyMemory, UnitsMemory);
        PropList.setProperty(p, ResourceProperties.PropertyBandwidth, UnitsBandwidth);
        PropList.setProperty(p, ResourceProperties.PropertyStorage, UnitsStorage);

        ResourceDelegation del = broker.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type);
        ResourceTicket ticket = broker.getShirakoPlugin().getTicketFactory().makeTicket(del);
        Ticket cs = new Ticket(ticket, broker.getShirakoPlugin(), getAuthorityProxy());

        resources.setResources(cs);

        IClientReservation source = ClientReservationFactory.getInstance().create(resources, term, slice);

        return source;
    }

    /**
     * Tests donation of inventory resources.
     * @throws Exception
     */
    public void testDonate() throws Exception {
        IBroker broker = getBroker();
        BrokerWorstFitPolicy policy = (BrokerWorstFitPolicy) broker.getPolicy();

        // create an inventory slice
        ISlice slice = SliceFactory.getInstance().create("inventory-slice");
        slice.setInventory(true);

        for (int i = 1; i <= 10; i++) {
            ResourceType type = new ResourceType(i);

            // create a source
            IClientReservation source = getSource(i, type, broker, slice);
            // donate it
            broker.donate(source);

            // check the resource pool
            assertEquals(i, policy.pools.size());

            LogicalInventory pool = policy.getLogicalInventory(type);
            assertNotNull(pool);
            assertEquals(i, pool.getSize());

            // check the ids stored in the reservation
            ArrayList<ID> ids = policy.getIDList(source);
            assertNotNull(ids);
            assertEquals(i, ids.size());

            // go through the inventory and check each machine record
            Iterator<MachineState> iter = pool.iterator();

            while (iter.hasNext()) {
                MachineState m = (MachineState) iter.next();
                // do we have an id?
                assertNotNull(m.getId());
                // same start time?
                assertEquals(source.getTerm().getStartTime(), m.getStart());
                // same end time?
                assertEquals(source.getTerm().getEndTime(), m.getEnd());
                // check total units
                assertEquals(UnitsCPU, m.getTotalCpu());
                assertEquals(UnitsMemory, m.getTotalMemory());
                assertEquals(UnitsBandwidth, m.getTotalBandwidth());
                assertEquals(UnitsStorage, m.getTotalStorage());
            }
        }
    }
}

// IStateChangeListener listener = new IStateChangeListener()
// {
// public void transition(Object obj, IState from, IState to) throws Exception
// {
// System.out.println("Transition: from: " + from + " to: " + to);
// System.out.println("Current cycle: " +
// ((IReservation)obj).getActor().getCurrentCycle());
//        
// }
// };
//
// request.registerListener(listener);
