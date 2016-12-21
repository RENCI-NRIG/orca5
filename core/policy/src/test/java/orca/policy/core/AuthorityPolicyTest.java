package orca.policy.core;

import java.util.Date;

import orca.policy.core.ServiceManagerCallbackHelper.IUpdateLeaseHandler;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorIdentity;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityPolicy;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IDatabase;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.common.delegation.DelegationException;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaTestCase;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.AuthorityReservationFactory;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.plugins.config.Config;
import orca.shirako.plugins.substrate.SubstrateTestWrapper;
import orca.shirako.plugins.substrate.db.SubstrateActorDatabase;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;
import orca.shirako.util.UpdateData;
import orca.util.ID;
import orca.util.ResourceType;
import org.apache.log4j.Logger;

import static java.lang.Thread.sleep;

/**
 * <code>AuthorityPolicyTest</code> is the base class for authority policy unit
 * tests.
 * @author aydan
 */
public abstract class AuthorityPolicyTest extends OrcaTestCase {
    public static final ResourceType Type = new ResourceType(1);
    public static final String Label = "test resource";
    public static final long DonateStartCycle = 10;
    public static final long DonateEndCycle = 100;
    public static final int DonateUnits = 123;
    public static final long TicketStartCycle = DonateStartCycle + 1;
    public static final long TicketEndCycle = TicketStartCycle + 10;
    public static final long TicketNewEndCycle = TicketEndCycle + 10;
    public static final int TicketUnits = 1;
    protected static final Logger logger = Globals.getLogger(AuthorityPolicyTest.class.getCanonicalName());

    @Override
    protected IDatabase makeActorDatabase() {
        return new SubstrateActorDatabase();
    }

    @Override
    protected IShirakoPlugin makeShirakoPlugin() {
        // use the wrapper, so that we create the
        // keystore for the actor, instead of relying on it having being created
        // by the system
        SubstrateTestWrapper plugin = new SubstrateTestWrapper();
        Config config = new Config();
        plugin.setConfig(config);
        return plugin;
    }

    /**
     * Returns a new authority actor to use.
     * @return
     * @throws Exception
     */
    public IAuthority getAuthority() throws Exception {
        IAuthority authority = super.getAuthority();
        authority.setRecovered(true);
        Term.setClock(authority.getActorClock());
        return authority;
    }

    /**
     * Creates a source reservation.
     * @param units
     * @param type
     * @param start
     * @param end
     * @param actor
     * @param slice
     * @return
     * @throws DelegationException
     * @throws TicketException
     */
    protected abstract IClientReservation getSource(int units, ResourceType type, Term term, IActor actor, ISlice slice) throws DelegationException, TicketException;

    /**
     * Returns a new ticket for the specified resources
     * @param units number of units
     * @param type resource type
     * @param term term
     * @param source source ticket
     * @param actor actor to issue the ticket
     * @param holder recepient of the ticket
     * @return
     * @throws Exception
     */
    protected Ticket getTicket(int units, ResourceType type, Term term, IClientReservation source, IActor actor, ID holder) throws Exception {
        ResourceTicket srcTicket = ((Ticket) source.getResources().getResources()).getTicket();

        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type, srcTicket.getDelegation().getProperties(), holder);
        ResourceTicket ticket = actor.getShirakoPlugin().getTicketFactory().makeTicket(srcTicket, del);
        Ticket cs = new Ticket(ticket, actor.getShirakoPlugin(), null);
        return cs;
    }

    /**
     * Creates a slice for requests.
     * @return
     */
    protected ISlice getRequestSlice() {
        // create the test slice
        ISlice slice = SliceFactory.getInstance().create(new SliceID(), "test-slice", new ResourceData());
        return slice;
    }

    /**
     * Makes a new redeem request
     * @param units
     * @param type
     * @param start
     * @param end
     * @param ticket
     * @return
     */
    protected IAuthorityReservation getRequest(int units, ResourceType type, Term term, Ticket ticket) {
        // resource set
        ResourceSet set = new ResourceSet(units, type);
        // attach the ticket
        set.setResources(ticket);

        ISlice slice = getRequestSlice();
        // reservation
        IAuthorityReservation request = AuthorityReservationFactory.getInstance().create(set, term, slice);
        return request;
    }

    /**
     * Makes a new extend lease request.
     * @param request
     * @param units
     * @param type
     * @param start
     * @param end
     * @param ticket
     * @return
     */
    protected IAuthorityReservation getRequest(IAuthorityReservation request, int units, ResourceType type, Term term, Ticket ticket) {
        // resource set
        ResourceSet set = new ResourceSet(units, type);
        // set the ticket
        set.setResources(ticket);
        // reservation
        IAuthorityReservation result = AuthorityReservationFactory.getInstance().create(request.getReservationID(), set, term, request.getSlice());

        result.setSequenceIn(request.getSequenceIn() + 1);

        return result;
    }

    public void testCreate() throws Exception {
        getAuthority();
    }

    /**
     * Checks the actor policy state before any resources have been donated to
     * it.
     * @param authority actor
     */
    protected void checkBeforeDonate(IAuthority authority) {

    }

    /**
     * Checks the actor policy state after the specified resources have been
     * donated to it.
     * @param authority actor
     * @param source source reservation
     */
    protected void checkAfterDonate(IAuthority authority, IClientReservation source) {
    }

    protected IClientReservation getDonateSource(IActor actor) throws Exception {
        // create an inventory slice
        ISlice slice = SliceFactory.getInstance().create("inventory-slice");
        slice.setInventory(true);
        actor.registerSlice(slice);

        // create a source reservation
        Date start = actor.getActorClock().cycleStartDate(DonateStartCycle);
        Date end = actor.getActorClock().cycleEndDate(DonateEndCycle);
        Term term = new Term(start, end);
        IClientReservation source = getSource(DonateUnits, Type, term, actor, slice);
        actor.register(source);
        return source;
    }

    public void testDonate() throws Exception {
        // create the actor
        IAuthority site = getAuthority();
        IAuthorityPolicy policy = (IAuthorityPolicy) site.getPolicy();
        // check the policy state before donating
        checkBeforeDonate(site);
        // get the donation reservation
        IClientReservation source = getDonateSource(site);
        // donate the reservation to the policy
        policy.donate(source);
        // check the policy
        checkAfterDonate(site, source);
    }

    protected void checkBeforeDonateSet(IAuthority authority) {
    }

    protected void checkAfterDonateSet(IAuthority authority, ResourceSet set) {
    }

    protected ResourceSet getDonateSet(IAuthority authority) {
        return new ResourceSet(DonateUnits, Type);
    }

    public void testDonateSet() throws Exception {
        // create the actor
        IAuthority site = getAuthority();
        IAuthorityPolicy policy = (IAuthorityPolicy) site.getPolicy();
        // check the policy state before donating
        checkBeforeDonateSet(site);
        // get the donation reservation
        ResourceSet set = getDonateSet(site);
        // donate the reservation to the policy
        policy.donate(set);
        // check the policy
        checkAfterDonateSet(site, set);
    }

    protected IAuthorityReservation getRedeemRequest(IAuthority authority, IClientReservation source, IActorIdentity identity) throws Exception {
        Date reqStart = authority.getActorClock().cycleStartDate(TicketStartCycle);
        Date reqEnd = authority.getActorClock().cycleStartDate(TicketEndCycle);
        Term reqTerm = new Term(reqStart, reqEnd);
        Ticket ticket = getTicket(TicketUnits, Type, reqTerm, source, authority, identity.getGuid());
        IAuthorityReservation request = getRequest(TicketUnits, Type, reqTerm, ticket);
        //authority.registerSlice(request.getSlice());
        //authority.register(request);
        return request;
    }

    protected IAuthorityReservation getExtendLeaseRequest(IAuthority authority, IClientReservation source, IActorIdentity identity, IAuthorityReservation request) throws Exception {
        Date reqStart = authority.getActorClock().cycleStartDate(TicketStartCycle);
        Date reqNewStart = authority.getActorClock().cycleStartDate(TicketEndCycle + 1);
        Date reqEnd = authority.getActorClock().cycleStartDate(TicketNewEndCycle);
        Term reqTerm = new Term(reqStart, reqEnd, reqNewStart);
        Ticket ticket = getTicket(TicketUnits, Type, reqTerm, source, authority, identity.getGuid());
        IAuthorityReservation newRequest = getRequest(request, TicketUnits, Type, reqTerm, ticket);
        //authority.register(newRequest);
        return newRequest;
    }

    protected void checkIncomingLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        assertNotNull(incoming);
        // get the resources
        ResourceSet set = incoming.getResources();
        assertNotNull(set);
        // check the resource
        assertEquals(request.getRequestedUnits(), set.getUnits());
        // check the term
        assertEquals(incoming.getTerm(), request.getRequestedTerm());
    }

    protected void checkIncomingCloseLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        assertNotNull(incoming);
        // get the resources
        ResourceSet set = incoming.getResources();
        assertNotNull(set);
        // FIXME: abstract units does not change on close?
        assertEquals(request.getRequestedUnits(), set.getUnits());
    }

    public void testRedeem() throws Exception {
        // note: variables to be used in the update lease handler must be
        // marked as final
        final IAuthority site = getAuthority();
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) site.getPolicy();
        IServiceManager sm = getSM();
        // create the callback helper: this is how we check the returned lease
        ServiceManagerCallbackHelper proxy = new ServiceManagerCallbackHelper(sm.getName(), sm.getGuid());
        ServiceManagerCallbackHelper authorityProxy = new ServiceManagerCallbackHelper(site.getName(), site.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(authorityProxy);

        // create a source reservation
        IClientReservation source = getDonateSource(site);
        // donate the reservation to the policy
        policy.donate(source);
        // create the source set
        ResourceSet sourceSet = getDonateSet(site);
        // donate the source set
        policy.donate(sourceSet);

        // get a redeem request
        final IAuthorityReservation request = getRedeemRequest(site, source, proxy);

        // attach the handler
        IUpdateLeaseHandler handler = new IUpdateLeaseHandler() {
            boolean waitingForLease = true;
            boolean waitingForClose = false;

            public void handleUpdateLease(IReservation reservation, UpdateData udd, AuthToken caller) {
                if (waitingForLease) {
                    checkIncomingLease(site, request, reservation, udd);
                    waitingForLease = false;
                    waitingForClose = true;
                } else if (waitingForClose) {
                    assertTrue(site.getCurrentCycle() >= TicketEndCycle);
                    checkIncomingCloseLease(site, request, reservation, udd);
                    waitingForClose = false;
                } else {
                    throw new RuntimeException("Invalid state");
                }
            }

            public void checkTermination() {
                assertFalse(waitingForLease);
                assertFalse(waitingForClose);
            }
        };

        // attach the update lease handler
        proxy.setUpdateLeaseHandler(handler);
        // tick the actor once so that it becomes active
        externalTick(site, 0);
        // redeem the request
        logger.info("Redeeming request...");
        site.redeem(request, proxy, proxy.getIdentity());
        // keep ticking
        for (long cycle = 1; cycle < DonateEndCycle; cycle++) {
            externalTick(site, cycle);
        }

        handler.checkTermination();
    }

    protected void checkIncomingExtendLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        assertNotNull(incoming);
        // check the reservation state
        assertEquals(ReservationStates.Active, incoming.getState());
        assertEquals(ReservationStates.None, incoming.getPendingState());
        // get the resources
        ResourceSet set = incoming.getResources();
        assertNotNull(set);
        // check the resource
        assertEquals(request.getRequestedUnits(), set.getUnits());
        // check the term
        assertEquals(incoming.getTerm(), request.getRequestedTerm());
    }

    public void testExtendLease() throws Exception {
        // note: variables to be used in the update lease handler must be
        // marked as final
        final IAuthority site = getAuthority();
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) site.getPolicy();
        IServiceManager sm = getSM();
        // create the callback helper: this is how we check the returned lease
        ServiceManagerCallbackHelper proxy = new ServiceManagerCallbackHelper(sm.getName(), sm.getGuid());
        ServiceManagerCallbackHelper authorityProxy = new ServiceManagerCallbackHelper(site.getName(), site.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(authorityProxy);

        // create a source reservation
        IClientReservation source = getDonateSource(site);
        // donate the reservation to the policy
        policy.donate(source);
        // create the source set
        ResourceSet sourceSet = getDonateSet(site);
        // donate the source set
        policy.donate(sourceSet);
        // get a redeem request
        final IAuthorityReservation request = getRedeemRequest(site, source, proxy);
        final IAuthorityReservation extendRequest = getExtendLeaseRequest(site, source, proxy, request);

        // attach the handler
        IUpdateLeaseHandler handler = new IUpdateLeaseHandler() {
            boolean waitingForExtendLease = false;
            boolean waitingForLease = true;
            boolean waitingForClose = false;

            public void handleUpdateLease(IReservation reservation, UpdateData udd, AuthToken caller) {
                if (waitingForLease) {
                    checkIncomingLease(site, request, reservation, udd);
                    waitingForLease = false;
                    waitingForExtendLease = true;
                } else if (waitingForExtendLease) {
                    checkIncomingExtendLease(site, request, reservation, udd);
                    waitingForExtendLease = false;
                    waitingForClose = true;
                } else if (waitingForClose) {
                    assertTrue(site.getCurrentCycle() >= TicketNewEndCycle);
                    checkIncomingCloseLease(site, request, reservation, udd);
                    waitingForClose = false;
                } else {
                    throw new RuntimeException("Invalid state");
                }
            }

            public void checkTermination() {
                assertFalse(waitingForLease);
                assertFalse(waitingForExtendLease);
                assertFalse(waitingForClose);
            }
        };

        // attach the update lease handler
        proxy.setUpdateLeaseHandler(handler);

        // tick the actor once so that becomes active
        externalTick(site, 0);
        // redeem the request
        logger.info("Redeeming request...");
        site.redeem(request, proxy, proxy.getIdentity());
        // keep ticking
        for (long cycle = 1; cycle < DonateEndCycle; cycle++) {
            if (cycle == TicketEndCycle - 3) {
                logger.info("Extending lease...");
                site.extendLease(extendRequest, proxy.getIdentity());
            }
            externalTick(site, cycle);
        }

        handler.checkTermination();
    }

    /**
     *
     * @param site
     * @param cycle
     * @throws Exception
     */
    protected void externalTick(IAuthority site, long cycle) throws Exception {
        site.externalTick(cycle);

        while (site.getCurrentCycle() != cycle){
            sleep(1);
        }
    }

    public void testClose() throws Exception {
        // note: variables to be used in the update lease handler must be
        // marked as final
        final IAuthority site = getAuthority();
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) site.getPolicy();
        IServiceManager sm = getSM();
        // create the callback helper: this is how we check the returned lease
        ServiceManagerCallbackHelper proxy = new ServiceManagerCallbackHelper(sm.getName(), sm.getGuid());
        ServiceManagerCallbackHelper authorityProxy = new ServiceManagerCallbackHelper(site.getName(), site.getGuid());
        ActorRegistry.registerCallback(proxy);
        ActorRegistry.registerCallback(authorityProxy);

        // create a source reservation
        IClientReservation source = getDonateSource(site);
        // donate the reservation to the policy
        policy.donate(source);
        // create the source set
        ResourceSet sourceSet = getDonateSet(site);
        // donate the source set
        policy.donate(sourceSet);
        // get a redeem request
        final IAuthorityReservation request = getRedeemRequest(site, source, proxy);

        // attach the handler
        IUpdateLeaseHandler handler = new IUpdateLeaseHandler() {
            boolean waitingForLease = true;
            boolean waitingForClose = false;

            public void handleUpdateLease(IReservation reservation, UpdateData udd, AuthToken caller) {
                if (waitingForLease) {
                    checkIncomingLease(site, request, reservation, udd);
                    waitingForLease = false;
                    waitingForClose = true;
                } else if (waitingForClose) {
                    assertTrue(site.getCurrentCycle() == TicketEndCycle - 3);
                    checkIncomingCloseLease(site, request, reservation, udd);
                    waitingForClose = false;
                } else {
                    throw new RuntimeException("Invalid state");
                }
            }

            public void checkTermination() {
                assertFalse(waitingForLease);
                assertFalse(waitingForClose);
            }
        };

        // attach the update lease handler
        proxy.setUpdateLeaseHandler(handler);
        // tick the actor once so that becomes active
        externalTick(site, 0);
        // redeem the request
        site.redeem(request, proxy, proxy.getIdentity());
        // keep ticking
        for (long cycle = 1; cycle < DonateEndCycle; cycle++) {
            if (cycle == TicketEndCycle - 3) {
                site.close(request);
            }
            externalTick(site, cycle);
        }

        handler.checkTermination();
    }

}
