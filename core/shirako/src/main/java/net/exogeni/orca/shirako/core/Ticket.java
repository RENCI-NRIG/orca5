/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.core;

import java.util.Date;
import java.util.Properties;

import net.exogeni.orca.shirako.api.IAuthorityProxy;
import net.exogeni.orca.shirako.api.IConcreteSet;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IShirakoPlugin;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.proxies.Proxy;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.Notice;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.PersistenceUtils;
import net.exogeni.orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

/**
 * <code>Ticket</code> is an <code>IConcreteSet</code> implementation that wraps
 * a <code>ResourceTicket</code> for use inside of a <code>ResourceSet</code>.
 * @author aydan
 */
public class Ticket implements IConcreteSet {
    public static final String PropertyOldUnits = "TicketStubOldUnits";
    public static final String PropertyAuthority = "TicketAuthority";
    public static final String PropertyResourceTicket = "TicketResourceTicket";

    // FIXME: should these be here?
    public static final String PropertyIdentifiers = "Identifiers";
    public static final String PropertyDivisible = "Divisible";

    /*
     * Encoding/decoding properties. FIXME: probably need to be unified with the
     * serialization/deserialization properties.
     */

    public static final String PropertyTicketAuthorityProxy = "ticket.authority.proxy";
    public static final String PropertyTicketResourceTicket = "ticket.resourceTicket";

    /**
     * The plugin object
     */
    @Persistent (reference=true)
    protected IShirakoPlugin plugin;

    /**
     * The logger
     */
    @Persistent (reference=true)
    protected Logger logger;

    /**
     * The reservation this ticket belongs to
     */
    @Persistent (reference=true)
    protected IReservation reservation;

    /**
     * The authority who owns the resources described in this concrete set
     */
    @Persistent(key = PropertyAuthority)
    protected IAuthorityProxy authority;

    /**
     * The encapsulated resource ticket.
     */
    @Persistent(key = PropertyResourceTicket, restore=false)
    protected ResourceTicket resourceTicket;

    /**
     * Units we used to have before the current extend
     */
    @Persistent (key = PropertyOldUnits)
    protected int oldUnits = 0;

    /**
     * Default constructor.
     */
    public Ticket() {
    }

    /**
     * Creates a new <code>Ticket</code>. This method is called when an actor
     * receives a new ticket from another actor.
     * @param ticket the resource ticket
     * @param plugin the shirako plugin
     * @param authority proxy
     */
    public Ticket(ResourceTicket ticket, IShirakoPlugin plugin, IAuthorityProxy authority) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.authority = authority;
        this.resourceTicket = ticket;
    }

    /**
     * Copy constructor.
     * @param ticket ticket to copy
     */
    public Ticket(Ticket ticket) {
        this(ticket, ticket.plugin);
    }

    /**
     * Makes a copy of the passed ticket using the specified plugin.
     * @param ticket ticket
     * @param plugin plugin
     */
    public Ticket(Ticket ticket, IShirakoPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        // FIXME: we are not making a clone of the
        // authority proxy. This assumes that actor proxies are stateless!
        this.authority = ticket.authority;
        // make a clone of the resource ticket
        this.resourceTicket = plugin.getTicketFactory().clone(ticket.resourceTicket);
        this.oldUnits = ticket.oldUnits;
    }

    /**
     * Creates a new ticket from the specified parent ticket.
     * @param parent parent ticket
     * @param ticket resource ticket
     */
    public Ticket(Ticket parent, ResourceTicket ticket) {
        this.plugin = parent.plugin;
        this.logger = plugin.getLogger();
        this.authority = parent.authority;
        this.resourceTicket = ticket;
    }

    public Properties encode(String protocol) throws Exception {
        Properties enc = new Properties();

        enc.setProperty(PersistenceUtils.PropertyClassName, this.getClass().getCanonicalName());
        if (getType() != null) {
            enc.setProperty(PropertyResourceType, getType().toString());
        }
        PropList.setProperty(enc, PropertyUnits, getUnits());

        IProxy proxyToUse = ActorRegistry.getProxy(protocol, authority.getName());        
        // the proxy to the site
        PropList.setProperty(enc, PropertyTicketAuthorityProxy, PersistenceUtils.save(proxyToUse));
        // the resource ticket XML
        enc.setProperty(PropertyTicketResourceTicket, plugin.getTicketFactory().toXML(getTicket()));
        return enc;
    }

    public void decode(Properties enc, IShirakoPlugin plugin) throws Exception {
        // decode the proxy
        Properties p = PropList.getPropertiesProperty(enc, PropertyTicketAuthorityProxy);
        IAuthorityProxy proxy = (IAuthorityProxy) Proxy.getProxy(p);
        String temp = enc.getProperty(PropertyTicketResourceTicket);
        if (temp == null) {
            throw new RuntimeException("Missing resource ticket");
        }
        ResourceTicket rt = plugin.getTicketFactory().fromXML(temp);
        
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.authority = proxy;
        this.resourceTicket = rt;        
    }

    /**
     * {@inheritDoc}
     */
    public void add(IConcreteSet cs, boolean configure) throws Exception {
        throw new Exception("add() is not supported by Ticket");
    }

    /**
     * {@inheritDoc}
     */
    public void change(IConcreteSet cs, boolean configure) throws Exception {
        assert cs != null;

        // remember the old units
        this.oldUnits = getUnits();

        // the concrete set must be a ticket
        Ticket t = (Ticket) cs;
        assert t.resourceTicket != null;
        // make a clone of the resource ticket
        this.resourceTicket = plugin.getTicketFactory().clone(t.resourceTicket);
    }

    /**
     * {@inheritDoc}
     */
    public IConcreteSet clone() {
        return new Ticket(this);
    }

    /**
     * {@inheritDoc}
     */
    public IConcreteSet cloneEmpty() {
        return new Ticket(this);
    }

    /**
     * {@inheritDoc}
     */
    public void close(){
    }

    /**
     * {@inheritDoc}
     */
    public IConcreteSet collectReleased() throws Exception {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Notice getNotices() {
        return null;
    }

    /**
     * Returns the ticket properties.
     * @return the ticket properties
     */
    public Properties getProperties() {
        if (resourceTicket == null) {
            return null;
        }
        return resourceTicket.getProperties();
    }

    /**
     * Returns the <code>IShirakoPlugin</code> associated with this ticket.
     * @return IShirakoPlugin associated with this ticket
     */
    public IShirakoPlugin getShirakoPlugin() {
        return plugin;
    }

    /**
     * {@inheritDoc}
     */
    public IAuthorityProxy getSiteProxy() {
        return authority;
    }

    /**
     * Returns the ticket term.
     * @return the ticket term
     */
    public Term getTerm() {
        if (resourceTicket == null) {
            return null;
        }

        return resourceTicket.getTerm();
    }

    /**
     * Returns the <code>ResourceTicket</code> wrapped by this concrete set.
     * @return resource ticket
     */
    public ResourceTicket getTicket() {
        return resourceTicket;
    }

    /**
     * Returns the resource type of the associated ticket.
     * @return resource type
     */
    public ResourceType getType() {
        if (resourceTicket == null) {
            return null;
        }
        return resourceTicket.getResourceType();
    }

    /**
     * {@inheritDoc}
     */
    public int getUnits() {
        if (resourceTicket == null) {
            return 0;
        }
        return resourceTicket.getUnits();
    }

    /**
     * {@inheritDoc}
     */
    public int holding(final Date date) {
        if (date == null) {
            throw new IllegalArgumentException();
        }

        Term term = getTerm();
        if (term == null) {
            return 0;
        }

        if (date.before(term.getNewStartTime())) {
            if (date.before(term.getStartTime())) {
                // date is before start time
                return 0;
            } else {
                // date is in [start, newStart)
                return oldUnits;
            }
        } else {
            if (date.after(term.getEndTime())) {
                // date is after end time
                return 0;
            } else {
                // date is in [newStart,end]
                return getUnits();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        // valid tickets are always active, if anyone asks
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void modify(IConcreteSet cs, boolean configure) throws Exception {
        throw new Exception("Not supported by TicketSet");
    }

    /**
     * {@inheritDoc}
     */
    public void probe() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void remove(IConcreteSet cs, boolean configure) throws Exception {
        throw new Exception("Not supported by TicketSet");
    }

    /**
     * {@inheritDoc}
     */
    public IConcreteSet selectExtract(int count, Properties p) throws Exception {
        throw new Exception("Cannot selectExtract from a Ticket");
    }

    /**
     * Indicates that we're committing resources to a client (on an an agent).
     * May need to touch TicketSet database since we're committing it. On a
     * client (service manager) this indicates that we have successfully scored
     * a ticket. The ticket has already been validated with validate().
     * @param reservation the slice for the reservation
     */
    public void setup(IReservation reservation) {
        this.reservation = reservation;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Ticket [units = ");
        sb.append(Integer.toString(getUnits()));
        sb.append(" oldUnits = " + Integer.toString(oldUnits));

        if (reservation != null) {
            ISlice slice = reservation.getSlice();

            if (slice == null) {
                logger.error("reservation inside ticket has no slice");
            } else {
                sb.append(" Slice=" + reservation.getSlice().getName());
            }
        }

        sb.append("] ");

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void validateConcrete(ResourceType type, int units, Term t) throws Exception {
        if (getUnits() < units) {
            throw new Exception("Ticket not valid for requested units");
        }

        // [aydan: 04/01/06] Commented while transitioning to ResourceType
        // if (this.type != type) {
        // throw new Exception("Ticket not valid for requested type");
        // }
    }

    /**
     * {@inheritDoc}
     */
    public void validateIncoming() throws Exception {
        // TODO: validate incoming ticket, add to local TicketSet.
    }

    /**
     * {@inheritDoc}
     */
    public void validateOutgoing() throws Exception {
        // no-op
    }

    public void restartActions() throws Exception {
        // no-op for Ticket
    }
}
