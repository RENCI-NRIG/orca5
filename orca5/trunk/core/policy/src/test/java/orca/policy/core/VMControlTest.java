package orca.policy.core;

import java.util.Collection;
import java.util.Properties;

import orca.policy.core.util.Vmm;
import orca.policy.core.util.VmmPool;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.UnitID;
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
import orca.util.PropList;
import orca.util.ResourceType;


public class VMControlTest extends AuthorityCalendarPolicyTest implements ResourceProperties, ConfigurationProperties, UnitProperties
{
    public static final int StartVlan = 50;
    public static final int EndVlan = StartVlan + DonateUnits - 1;
    public static final int VmmCapacity = 3;
    public static final int VMMemory = 300;
    
    protected IResourceControl getControl() throws Exception
    {
        VMControl control = new VMControl();
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
        ad.setValue(Integer.toString(VMMemory));
        rd.addAttribute(ad);
        return rd;
    }
    
    protected IClientReservation getSource(int units, ResourceType type, Term term, IActor actor, ISlice slice) throws DelegationException, TicketException
    {
        ResourceSet resources = new ResourceSet(units, type);

        Properties p = resources.getResourceProperties();    
        ResourcePoolDescriptor rd = getPoolDescriptor();
        rd.save(p, null);
        
        Properties local = resources.getLocalProperties();
        local.setProperty(VMControl.PropertyIPSubnet, "255.255.255.0");
        local.setProperty(VMControl.PropertyIPList, "192.168.1.2/24");
        PropList.setProperty(local, VMControl.PropertyCapacity, VmmCapacity);
        
        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type);
        ResourceTicket ticket = actor.getShirakoPlugin().getTicketFactory().makeTicket(del);
        Ticket cs = new Ticket(ticket, actor.getShirakoPlugin(), null);

        resources.setResources(cs);

        IClientReservation source = ClientReservationFactory.getInstance().create(resources, term, slice);

        return source;
    }

    @Override
    protected ResourceSet getDonateSet(IAuthority authority)
    {
        ResourceSet set = new ResourceSet(DonateUnits, Type);
        Properties p = set.getResourceProperties();    
        ResourcePoolDescriptor rd = getPoolDescriptor();
        rd.save(p, null);
        Properties local = set.getLocalProperties();
        local.setProperty(VMControl.PropertyIPSubnet, "255.255.255.0");
        local.setProperty(VMControl.PropertyIPList, "192.168.1.2/24");
        PropList.setProperty(local, VMControl.PropertyCapacity, VmmCapacity);
        
        UnitSet uset = new UnitSet(authority.getShirakoPlugin());
        set.setResources(uset);
        
        // populate the node group
        for (int i = 0; i < DonateUnits; i++){
            Unit host = new Unit();      
            host.setProperty(UnitHostName, "Host " + i);
            host.setProperty(UnitManagementIP, "192.168.123." + (i+1));
            host.setProperty(UnitControl, "192.168.123." + (i+1));
            uset.add(host);
        }
        
        return set;
    }

    protected void checkBeforeDonateSet(IAuthority authority)
    {
        super.checkBeforeDonateSet(authority);
        
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        VMControl control = (VMControl) getControl(policy);
        
        assertNotNull(control.inventory);
        assertEquals(0, control.inventory.size());
    }
    
    protected void checkAfterDonateSet(IAuthority authority, ResourceSet set)
    {
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        VMControl control = (VMControl) getControl(policy);
        
        assertEquals(1, control.inventory.size());
        VmmPool pool = control.inventory.values().iterator().next();
        assertEquals(DonateUnits, pool.getVmmsCount());
        assertEquals(VmmCapacity, pool.getCapacity());
        assertEquals(Type, pool.getType());
        
        Collection<Vmm> vmms = pool.getVmmSet();
        assertNotNull(vmms);
        
        for (Vmm vmm : vmms){
            assertNotNull(vmm.getHost());
            assertEquals(VmmCapacity, vmm.getCapacity());
            assertEquals(VmmCapacity, vmm.getAvailable());            
            assertEquals(0, vmm.getHostedCount());            
        }
    }

    protected Unit myUnit = null;
    
    protected void checkIncomingLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {
        super.checkIncomingLease(authority, request, incoming, udd);
        
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final VMControl control = (VMControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertEquals(VMMemory, Integer.valueOf(u.getProperty(UnitMemory)).intValue());
        assertNotNull(u.getProperty(UnitManagementIP));
        assertNotNull(u.getProperty(UnitManageSubnet));

        UnitID vmmHostID = u.getParentID();
        assertNotNull(vmmHostID);
        
        VmmPool pool = control.inventory.get(Type);
        assertNotNull(pool);
        Vmm vmm = pool.getVmm(vmmHostID);
        assertNotNull(vmm);
        assertEquals(TicketUnits, vmm.getHostedCount());
        assertEquals(VmmCapacity - TicketUnits, vmm.getAvailable());
        assertTrue(vmm.getHostedVMs().contains(u));
        System.out.println("Unit=" + u.toString());
        // remember the unit so that we can check that the unit is preserved in checkIncomingExtendLease
        myUnit = u; 
    }
    
    protected void checkIncomingCloseLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {
        super.checkIncomingCloseLease(authority, request, incoming, udd);
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final VMControl control = (VMControl) getControl(policy);

        assertNotNull(myUnit);
        
        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(0, uset.getUnits());
        // check the control
        UnitID vmmHostID = myUnit.getParentID();
        assertNotNull(vmmHostID);
        VmmPool pool = control.inventory.get(Type);
        assertNotNull(pool);
        Vmm vmm = pool.getVmm(vmmHostID);
        assertNotNull(vmm);
        assertEquals(0, vmm.getHostedCount());
        assertEquals(VmmCapacity, vmm.getAvailable());
        assertFalse(vmm.getHostedVMs().contains(myUnit));
    }

    protected void checkIncomingExtendLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd)
    {        
        super.checkIncomingLease(authority, request, incoming, udd);
        
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final VMControl control = (VMControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertEquals(VMMemory, Integer.valueOf(u.getProperty(UnitMemory)).intValue());
        assertNotNull(u.getProperty(UnitManagementIP));
        assertNotNull(u.getProperty(UnitManageSubnet));

        UnitID vmmHostID = myUnit.getParentID();
        assertNotNull(vmmHostID);
        
        VmmPool pool = control.inventory.get(Type);
        assertNotNull(pool);
        Vmm vmm = pool.getVmm(vmmHostID);
        assertNotNull(vmm);
        assertEquals(TicketUnits, vmm.getHostedCount());
        assertEquals(VmmCapacity - TicketUnits, vmm.getAvailable());
        assertTrue(vmm.getHostedVMs().contains(u));
        System.out.println("(extend) Unit=" + u.toString());
        assertEquals(myUnit, u);
    }
    
    // TODO: - need a test that extends and grows and another one that extends and shrinks
}
