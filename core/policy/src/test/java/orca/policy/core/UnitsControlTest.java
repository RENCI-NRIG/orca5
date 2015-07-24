package orca.policy.core;

import java.util.Properties;

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
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ClientReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.UpdateData;
import orca.util.PropList;
import orca.util.ResourceType;

public class UnitsControlTest extends AuthorityCalendarPolicyTest implements ResourceProperties, UnitProperties, ConfigurationProperties
{

    protected IResourceControl getControl() throws Exception
    {
        UnitsControl control = new UnitsControl();
        Properties p = new Properties();
        p.setProperty(ResourceControl.PropertyControlResourceTypes, Type.toString());
        control.configure(p);
        return control;
    }

    protected IClientReservation getSource(int units, ResourceType type, Term term, IActor actor, ISlice slice) throws DelegationException, TicketException
    {
        ResourceSet resources = new ResourceSet(units, type);


        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type);
        ResourceTicket ticket = actor.getShirakoPlugin().getTicketFactory().makeTicket(del);
        Ticket cs = new Ticket(ticket, actor.getShirakoPlugin(), null);

        resources.setResources(cs);

        IClientReservation source = ClientReservationFactory.getInstance().create(resources, term, slice);

        return source;
    }
    
    protected void checkBeforeDonate(IAuthority authority)
    {
        super.checkBeforeDonate(authority);
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        UnitsControl control = (UnitsControl) getControl(policy);

        assertEquals(0, control.total);
        assertEquals(0, control.allocated);
    }
    
    protected void checkAfterDonate(IAuthority authority, IClientReservation source)
    {
        super.checkAfterDonate(authority, source);
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        UnitsControl control = (UnitsControl) getControl(policy);


        assertEquals(DonateUnits, control.total);
        assertEquals(0, control.allocated);
        assertEquals(Type, control.type);
    }
    
    protected void checkIncomingLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {
        super.checkIncomingLease(authority, request, incoming, udd);
        
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final UnitsControl control = (UnitsControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertNotNull(u);
        // check the control
        assertEquals(DonateUnits, control.total);
        assertEquals(1, control.allocated);
    }
    
    protected void checkIncomingCloseLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {
        super.checkIncomingCloseLease(authority, request, incoming, udd);
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final UnitsControl control = (UnitsControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(0, uset.getUnits());
        // check the control
        assertEquals(DonateUnits, control.total);
        assertEquals(0, control.allocated);
    }

    protected void checkIncomingExtendLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {        
        super.checkIncomingLease(authority, request, incoming, udd);
        
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final UnitsControl control = (UnitsControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertNotNull(u);
        // check the control
        assertEquals(DonateUnits, control.total);
        assertEquals(1, control.allocated);      
    }
}
