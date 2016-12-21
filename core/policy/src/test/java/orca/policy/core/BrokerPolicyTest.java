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

import orca.policy.core.util.DummyAuthorityProxy;
import orca.security.AuthToken;
import orca.shirako.api.*;
import orca.shirako.common.delegation.DelegationException;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaTestCase;
import orca.shirako.core.Broker;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.proxies.soapaxis2.SoapAxis2AuthorityProxy;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.ResourceType;
import org.apache.log4j.Logger;

import static java.lang.Thread.sleep;

/**
 * Base class for broker policy unit tests.
 */
public abstract class BrokerPolicyTest extends OrcaTestCase {

    public static final long DonateStartCycle = 10;
    public static final long DonateEndCycle = 100;
    protected static final Logger logger = Globals.getLogger(BrokerPolicyTest.class.getCanonicalName());

    @Override
    public IBroker getBroker() throws Exception {
        IBroker broker = super.getBroker();
        broker.setRecovered(true);
        Term.setClock(broker.getActorClock());
        return broker;
    }

    protected abstract IClientReservation getSource(int units, ResourceType type, IBroker broker, ISlice slice) throws DelegationException, TicketException;

    protected abstract IBrokerReservation getRequest(int units, ResourceType type, Date start, Date end);

    protected abstract IBrokerReservation getRequest(IBrokerReservation request, int units, ResourceType type, Date start, Date end);

    /**
     * Tests if the actor and the policy can be instantiated.
     * @throws Exception
     */
    public void testCreate() throws Exception {
        getBroker();
    }

    protected IAuthorityProxy getAuthorityProxy() {
        AuthToken auth = new AuthToken("mysite", new ID());
        // NOTE: we use a SOAP proxy at the broker, but the SM makes the call using a local proxy.
        // In this way we make sure that the code correctly detects that it must translate the proxy to the correct protocol.
        IAuthorityProxy proxy = new SoapAxis2AuthorityProxy("http://my.container/orca:8080/services/mysite", auth, Globals.Log);
        // NOTE: passing the ticket  back to the SM involves encoding/decoding the proxy to the authority
        // encoding depends on the existence of a proxy in the actor registry.
        try {
            // make sure that we have a local proxy to the authority in the actor registry
            if (ActorRegistry.getProxy(IProxy.ProxyTypeLocal, "mysite") == null) {
                DummyAuthorityProxy dummy = new DummyAuthorityProxy(auth);
                ActorRegistry.registerProxy(dummy);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return proxy;
    }

    public void testAllocateTicket() throws Exception {
        /*
         * Requests a ticket for all resources. Checks if the ticket is
         * allocated for what was asked. Checks the term. Checks whether the
         * reservation is closed when it expires.
         */
        IBroker broker = getBroker();
        IServiceManager sm = getSM();

        ActorClock clock = broker.getActorClock();
        ResourceType type = new ResourceType(1);
        ClientCallbackHelper proxy = new ClientCallbackHelper(sm.getName(), sm.getGuid());
        ClientCallbackHelper brokerCallback = new ClientCallbackHelper(broker.getName(), broker.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(brokerCallback);
        
        int lastCalled = proxy.getCalled();

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

        // keep ticking
        for (; cycle < DonateEndCycle; cycle++) {
            broker.externalTick(cycle);
            while (broker.getCurrentCycle() != cycle){
                sleep(1);
            }

            if (lastCalled < proxy.getCalled()) {
                assertTicketed(proxy.getReservation(), units, type, start, end);
                lastCalled = proxy.getCalled();
            }
        }

        broker.awaitNoPendingReservations(); // without this, the test would sometime pass, and sometime fail

        assertEquals(1, proxy.getCalled());
        assertTrue(request.isClosed());
    }

    public void testAllocateTicket2() throws Exception {
        /*
         * Requests a ticket for all resources. Checks if the ticket is
         * allocated for what was asked. Checks the term. Checks whether the
         * reservation is closed when it expires. Repeat one more time.
         */
        IBroker broker = getBroker();
        IServiceManager sm = getSM();
        ActorClock clock = broker.getActorClock();
        ResourceType type = new ResourceType(1);
        ClientCallbackHelper proxy = new ClientCallbackHelper(sm.getName(), sm.getGuid());
        ClientCallbackHelper brokerCallback = new ClientCallbackHelper(broker.getName(), broker.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(brokerCallback);

        int lastCalled = proxy.getCalled();

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
        long cycleEnd = DonateEndCycle - 50;
        Date end = clock.cycleEndDate(cycleEnd);

        // make the request
        IBrokerReservation request = getRequest(units, type, start, end);
        // request the ticket
        ((Broker)broker).ticket(request, proxy, proxy.getIdentity());

        // keep ticking
        for (; cycle < DonateEndCycle; cycle++) {
            broker.externalTick(cycle);
            while (broker.getCurrentCycle() != cycle){
                sleep(1);
            }

            if (lastCalled < proxy.getCalled()) {
                assertTicketed(proxy.getReservation(), units, type, start, end);
                lastCalled = proxy.getCalled();
            }

            if (cycle == cycleEnd) {
                broker.awaitNoPendingReservations();

                assertTrue(request.isClosed());
                assertEquals(1, proxy.getCalled());

                start = clock.cycleStartDate(cycleEnd + 1);
                end = clock.cycleEndDate(DonateEndCycle - 1);
                request = getRequest(units, type, start, end);
                ((Broker)broker).ticket(request, proxy, proxy.getIdentity());
            }
        }

        broker.awaitNoPendingReservations();
        assertTrue(request.isClosed());
        assertEquals(2, proxy.getCalled());
    }

    public void testExtendTicket() throws Exception {
        /*
         * Requests a ticket for all resources. Checks if the ticket is
         * allocated for what was asked. Checks the term. Checks whether the
         * reservation is closed when it expires. Repeat one more time.
         */
        IBroker broker = getBroker();
        IServiceManager sm = getSM();
        ActorClock clock = broker.getActorClock();
        ResourceType type = new ResourceType(1);
        ClientCallbackHelper proxy = new ClientCallbackHelper(sm.getName(), sm.getGuid());
        ClientCallbackHelper brokerCallback = new ClientCallbackHelper(broker.getName(), broker.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(brokerCallback);
        
        int lastCalled = proxy.getCalled();

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
        long cycleEnd = DonateEndCycle - 50;
        Date end = clock.cycleEndDate(cycleEnd);

        // make the request
        IBrokerReservation request = getRequest(units, type, start, end);
        IBrokerReservation reservation = request;
        // request the ticket
        ((Broker)broker).ticket(request, proxy, proxy.getIdentity());

        // keep ticking
        for (; cycle <= DonateEndCycle; cycle++) {
            broker.externalTick(cycle);
            while (broker.getCurrentCycle() != cycle){
                sleep(1);
            }

            if (lastCalled < proxy.getCalled()) {
                assertTicketed(proxy.getReservation(), units, type, start, end);
                lastCalled = proxy.getCalled();
            }

            // renew 10 cycles before the end
            if (cycle == (cycleEnd - 10)) {
                assertEquals(1, proxy.getCalled());
                start = new Date(end.getTime() + 1);
                end = clock.cycleEndDate(DonateEndCycle - 1);
                request = getRequest(request, units, type, start, end);
                ((Broker)broker).extendTicket(request, proxy.getIdentity());
            }
        }

        assertTrue(reservation.isClosed());
        assertEquals(2, proxy.getCalled());
    }

    protected void assertTicketed(IReservation r, int units, ResourceType type, Date start, Date end) {
        assertNotNull(r);
        assertFalse(r.isFailed());
        // assertTrue((r.getState() == ReservationStates.Ticketed) &&
        // (r.getPendingState() == ReservationStates.None));
        assertEquals(units, r.getResources().getUnits());
        assertEquals(type, r.getResources().getType());
        assertNotNull(r.getTerm());
        // assertEquals(start, r.getTerm().getStartTime());
        //System.out.println(start.getTime() + ":" + r.getTerm().getNewStartTime().getTime());
        assertEquals(start, r.getTerm().getNewStartTime());
        assertEquals(end, r.getTerm().getEndTime());
    }

}
