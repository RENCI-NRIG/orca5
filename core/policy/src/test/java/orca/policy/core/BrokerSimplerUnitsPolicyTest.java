/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.policy.core;

import java.util.Date;
import java.util.Properties;

import orca.policy.core.util.InventoryForType;
import orca.shirako.api.IBroker;
import orca.shirako.api.IBrokerPolicy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.common.delegation.DelegationException;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.common.meta.QueryProperties;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.common.meta.ResourcePoolAttributeType;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.core.Broker;
import orca.shirako.core.BrokerPolicy;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.BrokerReservationFactory;
import orca.shirako.kernel.ClientReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.util.ResourceType;

public class BrokerSimplerUnitsPolicyTest extends BrokerPolicyTest {
    @Override
    public IBrokerPolicy getBrokerPolicy() throws Exception {
        BrokerSimplerUnitsPolicy policy = new BrokerSimplerUnitsPolicy();
        return policy;
    }

    protected IBrokerReservation getRequest(IBrokerReservation request, int units, ResourceType type, Date start, Date end) {
        // resource set
        ResourceSet set = new ResourceSet(units, type);
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
        // term
        Term term = new Term(start, end);
        // reservation
        IBrokerReservation request = BrokerReservationFactory.getInstance().create(set, term, slice);
        return request;
    }

    protected ResourcePoolDescriptor getPoolDescriptor(ResourceType rtype) {
        ResourcePoolDescriptor rd = new ResourcePoolDescriptor();
        rd.setResourceType(rtype);
        rd.setResourceTypeLabel("Pool label: " + rtype);
        ResourcePoolAttributeDescriptor ad = new ResourcePoolAttributeDescriptor();
        ad.setKey(ResourceProperties.ResourceMemory);
        ad.setLabel("Memory");
        ad.setUnit("MB");
        ad.setType(ResourcePoolAttributeType.INTEGER);
        ad.setValue(Integer.toString(1024));
        rd.addAttribute(ad);
        return rd;
    }

    protected IClientReservation getSource(int units, ResourceType type, IBroker broker, ISlice slice) throws DelegationException, TicketException {
        Date start = broker.getActorClock().cycleStartDate(DonateStartCycle);
        Date end = broker.getActorClock().cycleEndDate(DonateEndCycle);
        Term term = new Term(start, end);

        ResourceSet resources = new ResourceSet(units, type);

        ResourceDelegation del = broker.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type);
        ResourceTicket ticket = broker.getShirakoPlugin().getTicketFactory().makeTicket(del);
        Ticket cs = new Ticket(ticket, broker.getShirakoPlugin(), getAuthorityProxy());

        resources.setResources(cs);
        // attach the resource pool descriptor
        Properties resourceProperties = resources.getResourceProperties();
        ResourcePoolDescriptor rd = getPoolDescriptor(type);
        rd.save(resourceProperties, null);

        IClientReservation source = ClientReservationFactory.getInstance().create(resources, term, slice);

        return source;
    }

    /**
     * Tests donation of inventory resources.
     * @throws Exception
     */
    public void testDonate() throws Exception {
        IBroker broker = getBroker();
        BrokerSimplerUnitsPolicy policy = (BrokerSimplerUnitsPolicy) broker.getPolicy();

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
            assertEquals(i, policy.inventory.getInventory().size());

            InventoryForType pool = policy.inventory.get(type.toString());
            assertNotNull(pool);
            assertEquals(i, pool.getFree());
            assertEquals(0, pool.getAllocated());
        }
    }

    protected ResourcePoolsDescriptor checkQueryResponse(Properties response, int count) throws Exception {
        String temp = response.getProperty(QueryProperties.QueryResponse);
        assertNotNull(temp);
        assertEquals(temp, QueryProperties.QueryActionDisctoverPools);
        temp = response.getProperty(QueryProperties.PoolsCount);
        assertNotNull(temp);
        assertEquals(count, Integer.parseInt(temp), count);
        ResourcePoolsDescriptor result = BrokerPolicy.getResourcePools(response);
        assertEquals(count, result.size());
        return result;
    }

    public void testQuery() throws Exception {
        IBroker broker = getBroker();
        BrokerSimplerUnitsPolicy policy = (BrokerSimplerUnitsPolicy) broker.getPolicy();

        Properties request = new Properties();
        request.setProperty(QueryProperties.QueryAction, QueryProperties.QueryActionDisctoverPools);

        Properties response = policy.query(request);
        System.out.println(response);

        ResourcePoolsDescriptor rd = checkQueryResponse(response, 0);

        ISlice slice = SliceFactory.getInstance().create("inventory-slice");
        slice.setInventory(true);

        for (int i = 1; i <= 10; i++) {
            ResourceType type = new ResourceType(i);

            // create a source
            IClientReservation source = getSource(i, type, broker, slice);
            // donate it
            broker.donate(source);

            response = policy.query(request);
            rd = checkQueryResponse(response, i);
        }
    }
    
    public void testAdvancedRequest() throws Exception {
        IBroker broker = getBroker();
        IServiceManager sm = getSM();
        BrokerSimplerUnitsPolicy policy = (BrokerSimplerUnitsPolicy) broker.getPolicy();
 
        // set the allocation horizon 
        policy.allocationHorizon = 10; // allocate up to 10 cycles in the future 
        
        ActorClock clock = broker.getActorClock();
        ResourceType type = new ResourceType(1);
        ClientCallbackHelper proxy = new ClientCallbackHelper(sm.getName(), sm.getGuid());
        ClientCallbackHelper brokerCallback = new ClientCallbackHelper(broker.getName(), broker.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(brokerCallback);
        
        // create the inventory slice
        ISlice inventorySlice = SliceFactory.getInstance().create("inventory-slice");
        inventorySlice.setInventory(true);

        // create a source
        IClientReservation source = getSource(1, type, broker, inventorySlice);
        // donate it
        broker.donate(source);

        long cycle = 1;
        // tick the broker once so that it is ready to accept requests
        broker.externalTick(cycle++);

        int units = 1;
        Date start = clock.cycleStartDate(DonateStartCycle);
        Date end = clock.cycleEndDate(DonateEndCycle - 1);

        // make the request
        IBrokerReservation request = getRequest(units, type, start, end);
        // request the ticket
        ((Broker)broker).ticket(request, proxy, proxy.getIdentity());
        
        assertEquals(proxy.prepared, 0);
        assertEquals(proxy.called, 0);
        
        for (; cycle < DonateEndCycle; cycle++) {
            // the first tick should trigger the allocation and should enqueue the report RPC (proxy.prepared should become 1);
            // since the RPC is performed on a separate thread, the exact cycle when the SM is going to process it
            // is not deterministic.
            broker.externalTick(cycle);
            // the reservation on the broker side should have transitioned to ticketed
            assertTicketed(request, units, type, start, end);
            // a proxy callback should have been prepared
            assertEquals(proxy.prepared, 1);
            
            if (proxy.called > 0){
                assertTicketed(proxy.getReservation(), units, type, start, end);
            }
        }

        assertEquals(1, proxy.getCalled());
        assertTrue(request.isClosed());
    }

}
