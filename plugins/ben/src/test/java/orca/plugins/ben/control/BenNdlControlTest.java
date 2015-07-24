package orca.plugins.ben.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import orca.plugins.ben.control.BenNdlControl;
import orca.policy.core.AuthorityCalendarPolicy;
import orca.policy.core.AuthorityCalendarPolicyTest;
import orca.policy.core.IResourceControl;
import orca.policy.core.ResourceControl;
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

public class BenNdlControlTest extends AuthorityCalendarPolicyTest implements ResourceProperties, UnitProperties, ConfigurationProperties {
    public static final String SubstrateFile = "orca/network/ben-dell.rdf";

    public static final String RequestFile = "orca/network/request-6509.rdf";

    public String getNdl(String file) {
        try {
            InputStream s = getClass().getClassLoader().getResource(RequestFile).openStream();
            StringBuffer sb = new StringBuffer();

            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            String line = null;
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected IAuthorityReservation getRequest(int units, ResourceType type, Term term, Ticket ticket) {
        IAuthorityReservation r = super.getRequest(units, type, term, ticket);
        Properties reqp = r.getRequestedResources().getConfigurationProperties();
        String ndl = getNdl(RequestFile);
        System.out.println("NDL" + ndl);
        reqp.setProperty(BenNdlControl.PropertyRequestNdl, ndl);
        return r;
    }

    protected IResourceControl getControl() throws Exception {
        BenNdlControl control = new BenNdlControl();
        Properties p = new Properties();
        p.setProperty(ResourceControl.PropertyControlResourceTypes, Type.toString());
        control.configure(p);
        return control;
    }

    protected IClientReservation getSource(int units, ResourceType type, Term term, IActor actor, ISlice slice) throws DelegationException, TicketException {
        ResourceSet resources = new ResourceSet(units, type);

        // set the vlan properties
        PropList.setProperty(resources.getResourceProperties(), BenNdlControl.PropertySubstrateFile, SubstrateFile);

        ResourceDelegation del = actor.getShirakoPlugin().getTicketFactory().makeDelegation(units, term, type);
        ResourceTicket ticket = actor.getShirakoPlugin().getTicketFactory().makeTicket(del);
        Ticket cs = new Ticket(ticket, actor.getShirakoPlugin(), null);

        resources.setResources(cs);

        IClientReservation source = ClientReservationFactory.getInstance().create(resources, term, slice);

        return source;
    }

    protected void checkBeforeDonate(IAuthority authority) {
        super.checkBeforeDonate(authority);
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        BenNdlControl control = (BenNdlControl) getControl(policy);

        assertNull(control.handler);
        assertFalse(control.inprogress);
    }

    protected void checkAfterDonate(IAuthority authority, IClientReservation source) {
        super.checkAfterDonate(authority, source);
        AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        BenNdlControl control = (BenNdlControl) getControl(policy);

        // check the invariants
        assertNotNull(control.handler);
        assertFalse(control.inprogress);
        assertEquals(Type, control.type);
    }

    protected int vlantag = 0;

    protected void checkIncomingLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        super.checkIncomingLease(authority, request, incoming, udd);

        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final BenNdlControl control = (BenNdlControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the node (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertTrue(u.getProperty(UnitVlanTag) != null);
        // extract the tag
        int tag = Integer.parseInt(u.getProperty(UnitVlanTag));
        System.out.println("VLAN tag = " + tag);
        // remember the tag so that we can check that the tag is preserved in
        // checkIncomingExtendLease
        vlantag = tag;
        // check the control
        assertFalse(control.inprogress);
    }

    protected void checkIncomingCloseLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        super.checkIncomingCloseLease(authority, request, incoming, udd);
        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final BenNdlControl control = (BenNdlControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(0, uset.getUnits());
        // check the control
        assertFalse(control.inprogress);
    }

    protected void checkIncomingExtendLease(IAuthority authority, IAuthorityReservation request, IReservation incoming, UpdateData udd) {
        super.checkIncomingLease(authority, request, incoming, udd);

        final AuthorityCalendarPolicy policy = (AuthorityCalendarPolicy) authority.getPolicy();
        final BenNdlControl control = (BenNdlControl) getControl(policy);

        ResourceSet set = incoming.getResources();
        UnitSet uset = (UnitSet) set.getResources();
        assertNotNull(uset);
        assertEquals(TicketUnits, uset.getUnits());
        // get the unit (it contains the vlan tag)
        Unit u = (Unit) uset.getSet().iterator().next();
        assertTrue(u.getProperty(UnitVlanTag) != null);
        // extract the tag
        int tag = Integer.parseInt(u.getProperty(UnitVlanTag));
        System.out.println("(extend) VLAN tag = " + tag);
        assertEquals(vlantag, tag);
        // check the control
        assertFalse(control.inprogress);
    }
}
