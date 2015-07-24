package orca.policy.core;

import java.util.Properties;

import orca.policy.core.SimpleVMControl.PoolData;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.delegation.DelegationException;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.common.meta.ResourcePoolAttributeType;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ClientReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.UpdateData;
import orca.util.ResourceType;

public class SimpleVMControlTest extends AuthorityCalendarPolicyTest implements ResourceProperties, ConfigurationProperties, UnitProperties {
    public static final String AttributeValueMemory = "128";

    protected IResourceControl getControl() throws Exception {
        SimpleVMControl control = new SimpleVMControl();
        Properties p = new Properties();
        p.setProperty(ResourceControl.PropertyControlResourceTypes, Type.toString());
        control.configure(p);
        return control;
    }

    protected ResourcePoolDescriptor getPoolDescriptor() {
        ResourcePoolDescriptor rd = new ResourcePoolDescriptor();
        rd.setResourceType(Type);
        rd.setResourceTypeLabel(Label);
        ResourcePoolAttributeDescriptor ad = new ResourcePoolAttributeDescriptor();
        ad.setKey(ResourceMemory);
        ad.setLabel("Memory");
        ad.setUnit("MB");
        ad.setType(ResourcePoolAttributeType.INTEGER);
        ad.setValue(AttributeValueMemory);
        rd.addAttribute(ad);
        return rd;
    }

    protected IClientReservation getSource(int units, ResourceType type, Term term, IActor actor, ISlice slice) throws DelegationException, TicketException {
        ResourceSet resources = new ResourceSet(units, type);

        Properties p = resources.getResourceProperties();
        ResourcePoolDescriptor rd = getPoolDescriptor();
        rd.save(p, null);

        Properties local = resources.getLocalProperties();
        local.setProperty(VMControl.PropertyIPSubnet, "255.255.255.0");
        local.setProperty(VMControl.PropertyIPList, "192.168.1.2/24");

        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type);
        ResourceTicket ticket = actor.getShirakoPlugin().getTicketFactory().makeTicket(del);
        Ticket cs = new Ticket(ticket, actor.getShirakoPlugin(), null);

        resources.setResources(cs);

        IClientReservation source = ClientReservationFactory.getInstance().create(resources, term, slice);

        return source;
    }

    @Override
    protected void checkBeforeDonate(IAuthority authority) {
        super.checkBeforeDonateSet(authority);

        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        SimpleVMControl control = (SimpleVMControl) getControl(policy);

        assertNotNull(control.inventory);
        assertEquals(0, control.inventory.size());
    }

    @Override
    protected void checkAfterDonate(IAuthority authority, IClientReservation source) {
        super.checkAfterDonate(authority, source);

        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        SimpleVMControl control = (SimpleVMControl) getControl(policy);

        assertEquals(253, control.ipset.getFreeCount());
        assertEquals(1, control.inventory.size());
        PoolData pool = control.inventory.values().iterator().next();
        assertEquals(DonateUnits, pool.getFree());
        assertEquals(Type, pool.getType());
    }

    protected Unit myUnit = null;

    protected void checkIncomingLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        super.checkIncomingLease(authority, request, incoming, udd);

        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final SimpleVMControl control = (SimpleVMControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertEquals(AttributeValueMemory, u.getProperty(UnitMemory));
        assertNotNull(u.getProperty(UnitManagementIP));
        assertNotNull(u.getProperty(UnitManageSubnet));
        PoolData pool = control.inventory.get(Type);
        assertNotNull(pool);
        assertEquals(1, pool.getAllocated());
        assertEquals(DonateUnits - 1, pool.getFree());

        System.out.println("Unit=" + u.toString());
        // remember the unit so that we can check that the unit is preserved in
        // checkIncomingExtendLease
        myUnit = u;
    }

    protected void checkIncomingCloseLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        super.checkIncomingCloseLease(authority, request, incoming, udd);
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final SimpleVMControl control = (SimpleVMControl) getControl(policy);

        assertNotNull(myUnit);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(0, uset.getUnits());
        PoolData pool = control.inventory.get(Type);
        assertNotNull(pool);
        assertEquals(0, pool.getAllocated());
        assertEquals(DonateUnits, pool.getFree());
    }

    protected void checkIncomingExtendLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        super.checkIncomingLease(authority, request, incoming, udd);

        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final SimpleVMControl control = (SimpleVMControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertEquals(AttributeValueMemory, u.getProperty(UnitMemory));
        assertNotNull(u.getProperty(UnitManagementIP));
        assertNotNull(u.getProperty(UnitManageSubnet));
        PoolData pool = control.inventory.get(Type);
        assertNotNull(pool);
        assertEquals(1, pool.getAllocated());
        assertEquals(DonateUnits - 1, pool.getFree());
        System.out.println("(extend) Unit=" + u.toString());
        assertEquals(myUnit, u);
    }

    // TODO: - need a test that extends and grows and another one that extends
    // and shrinks
}
