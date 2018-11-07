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

import static orca.shirako.common.meta.UnitProperties.UnitVlanTag;

public class VlanControlTest extends AuthorityCalendarPolicyTest //implements ResourceProperties, UnitProperties, ConfigurationProperties
{
    public static final int StartVlan = 50;
    public static final int EndVlan = StartVlan + DonateUnits - 1;
    public static final int VlanBW = 100;

    protected IResourceControl getControl() throws Exception
    {
        VlanControl control = new VlanControl();
        Properties p = new Properties();
        p.setProperty(ResourceControl.PropertyControlResourceTypes, Type.toString());
        control.configure(p);
        return control;
    }

    protected IClientReservation getSource(int units, ResourceType type, Term term, IActor actor, ISlice slice) throws DelegationException, TicketException
    {
        ResourceSet resources = new ResourceSet(units, type);

        // set the vlan properties
        PropList.setProperty(resources.getLocalProperties(), VlanControl.PropertyVlanRangeNum, 1);
        PropList.setProperty(resources.getLocalProperties(), VlanControl.PropertyStartVlan + 1, StartVlan);
        PropList.setProperty(resources.getLocalProperties(), VlanControl.PropertyEndVlan + 1, EndVlan);

        Properties properties = new Properties();
        PropList.setProperty(properties, ResourceProperties.ResourceBandwidth, VlanBW);
        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type, properties);
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
        VlanControl control = (VlanControl) getControl(policy);

        assertNotNull(control.tags);
        assertEquals(0, control.tags.size());
    }
    
    protected void checkAfterDonate(IAuthority authority, IClientReservation source)
    {
        super.checkAfterDonate(authority, source);
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        VlanControl control = (VlanControl) getControl(policy);

        // check the invariants
        assertEquals(DonateUnits, control.tags.size());
        assertEquals(DonateUnits, control.tags.getFree());
        assertEquals(0, control.tags.getAllocated());
        assertEquals(Type, control.type);
    }

    protected int vlantag = 0;
    
    protected void checkIncomingLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {
        super.checkIncomingLease(authority, request, incoming, udd);
        
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final VlanControl control = (VlanControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        String stag = u.getProperty(UnitVlanTag);
        assertNotNull(stag);
        // extract the tag
        int tag = Integer.parseInt(stag);
        System.out.println("VLAN tag = " + tag);
        // remember the tag so that we can check that the tag is preserved in checkIncomingExtendLease
        vlantag = tag;
        // check the control
        assertEquals(DonateUnits - 1, control.tags.getFree());
        assertEquals(1, control.tags.getAllocated());
    }
    
    protected void checkIncomingCloseLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {
        super.checkIncomingCloseLease(authority, request, incoming, udd);
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final VlanControl control = (VlanControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(0, uset.getUnits());
        // check the control
        assertEquals(DonateUnits, control.tags.getFree());
        assertEquals(0, control.tags.getAllocated());
    }

    protected void checkIncomingExtendLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {        
        super.checkIncomingLease(authority, request, incoming, udd);
        
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final VlanControl control = (VlanControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        String stag = u.getProperty(UnitVlanTag);
        assertNotNull(stag);
        // extract the tag
        int tag = Integer.parseInt(stag);
        System.out.println("(extend) VLAN tag = " + tag);
        assertEquals(vlantag, tag);
        // check the control
        assertEquals(DonateUnits - 1, control.tags.getFree());
        assertEquals(1, control.tags.getAllocated());          
    }
}
