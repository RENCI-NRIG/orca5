/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityPolicy;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

/**
 * Base class for all authority policy implementations.
 */
public class AuthorityPolicy extends Policy implements IAuthorityPolicy {
    public static final String PropertySourceTicket = "sourceTicket";

    /**
     * Map of inventory tickets, indexed by resource type.
     */
    @NotPersistent
    protected HashMap<ResourceType, HashSet<ResourceSet>> tickets;

    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    public AuthorityPolicy() {
    }

    /**
     * Creates a new instance.
     * 
     * @param actor
     *            actor the policy belongs to
     */
    public AuthorityPolicy(final IAuthority actor) {
        super(actor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();
            tickets = new HashMap<ResourceType, HashSet<ResourceSet>>();
            initialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revisit(final IReservation reservation) throws Exception {
        if (reservation instanceof IClientReservation) {
            donate((IClientReservation)reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void donate(final IClientReservation r) throws Exception {
        ResourceSet rset = r.getResources();
        rset.setReservationID(r.getReservationID());

        if (logger.isInfoEnabled()) {
            logger.info("AuthorityPolicy donateTicket " + rset.toString());
        }

        HashSet<ResourceSet> ticketSet = tickets.get(rset.getType());

        if (ticketSet == null) {
            ticketSet = new HashSet<ResourceSet>();
            tickets.put(rset.getType(), ticketSet);
        }

        ticketSet.add(rset);
    }

    /**
     * {@inheritDoc}
     */
    public void donate(final ResourceSet resources) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void eject(final ResourceSet resources) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void failed(final ResourceSet rset) {
    }

    /**
     * {@inheritDoc}
     */
    public int unavailable(final ResourceSet resources) throws Exception {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void available(final ResourceSet resources) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void freed(final ResourceSet resources) {
    }

    /**
     * {@inheritDoc}
     */
    public void recovered(final ResourceSet rset) {
    }

    /**
     * {@inheritDoc}
     */
    public void release(final ResourceSet resources) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean bind(final IBrokerReservation reservation) throws Exception {
        ResourceSet requested = reservation.getRequestedResources();

        HashSet<ResourceSet> set = tickets.get(requested.getType());

        if ((set == null) || (set.size() == 0)) {
            error("bindTicket: no tickets available for the requested resource type "
                    + requested.getType());
        }

        // We need a calendar to tell us what we have exported from each source
        // ticket. For now, just take the first element from the set and export
        // from it
        // Also: it should be possible to specify a source ticket to use for a
        // given export. This can be a property on the AgentReservation (say:
        // slice and
        // reservation id to use). Have to make sure that the resource set has
        // the slice and the
        // reservation id.
        ReservationID rid = null;
        if (reservation.getRequestedResources().getRequestProperties() != null) {
            String temp = reservation.getRequestedResources()
                    .getRequestProperties()
                    .getProperty(PropertySourceTicket);

            if (temp != null) {
                rid = new ReservationID(temp);
            }
        }

        ResourceSet ticket = null;
        if (rid != null) {
            // FIXME: linear search for now
            Iterator<ResourceSet> iter = set.iterator();
            while (iter.hasNext()) {
                ResourceSet t = iter.next();
                if (t.getReservationID() != null) {
                    if (t.getReservationID().equals(rid)) {
                        ticket = t;
                        break;
                    }
                }
            }
        } else {
            Iterator<ResourceSet> iter = set.iterator();
            ticket = iter.next();
        }

        if (ticket == null) {
            error("bindticket: no tickets available");
        }

        /*
         * If this is an elastic export, whittle it down to size if necessary.
         * Signal obvious export errors. This code is fragile/temporary: it
         * assumes that the request is for "now", and that there is just one
         * export per resource type. Other errors involving multiple exports per
         * type will be caught in the extract below, but we won't have a chance
         * to adjust if the request is elastic.
         */
        int units = ticket.getResources().getUnits();
        int needed = requested.getUnits();

        if (needed > units) {
            logger.error("insufficient units available to export to agent");
            needed = units;
        }

        // make the new delegation
        ResourceDelegation del = actor.getShirakoPlugin()
                .getTicketFactory()
                .makeDelegation(needed,
                        reservation.getRequestedTerm(),
                        requested.getType(),
                        reservation.getClientAuthToken().getGuid());
        // make the resulting resource set
        ResourceSet mine = extract(ticket, del);
        // set the approved term to be equal to the requested term
        reservation.setApproved(new Term(reservation.getRequestedTerm()), mine);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean bind(final IAuthorityReservation reservation) throws Exception {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void allocate(final long cycle) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void assign(final long cycle) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void correctDeficit(final IAuthorityReservation reservation) throws Exception {
        if (reservation.getResources() == null) {
            // no resources yet;
            return;
        }

        finishCorrectDeficit(null, reservation);
    }

    /**
     * Finishes correcting a deficit.
     * 
     * @param rset
     *            correction
     * @param reservation
     *            reservation
     * 
     * @throws Exception in case of error
     */
    protected void finishCorrectDeficit(final ResourceSet rset,
            final IAuthorityReservation reservation) throws Exception {
        /*
         * We could have a partial set if there's a shortage. Go ahead and
         * install it: we'll come back later for the rest if we return a null
         * term. Alternatively, we could release them and throw an error.
         */
        if (rset == null) {
            logWarn("we either do not have resources to satisfy the request or the reservation has/will have a pending operation");

            return;
        }

        if (rset.isEmpty()) {
            reservation.setPendingRecover(false);
        } else {
            reservation.getResources().update(reservation, rset);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean extend(final IAuthorityReservation reservation) throws Exception {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean extend(final IBrokerReservation reservation) throws Exception {
        /* extends are not supported */
        return false;
    }

    /**
     * Creates a new resource set using the source and the specified delegation.
     * 
     * @param source source
     * @param delegation delegation
     * @return created resource set
     * @throws TicketException in case of error
     */
    protected ResourceSet extract(ResourceSet source, ResourceDelegation delegation)
            throws TicketException {
        assert source != null;
        assert delegation != null;

        logger.debug("extracting delegation: " + delegation.toString());
        // make a resource set to wrap around
        ResourceData rd = new ResourceData();
        PropList.mergeProperties(source.getResourceProperties(), rd.getResourceProperties());
        ResourceSet extracted = new ResourceSet(delegation.getUnits(),
                delegation.getResourceType(),
                rd);

        // obtain the source resource ticket
        ResourceTicket sourceTicket = ((Ticket) source.getResources()).getTicket();

        // make a new ResourceTicket using the delegation and the source ticket
        ResourceTicket newTicket = actor.getShirakoPlugin()
                .getTicketFactory()
                .makeTicket(sourceTicket, delegation);

        // make a concrete set to wrap the resource ticket
        Ticket cset = new Ticket((Ticket) source.getResources(), newTicket);
        // store the resource ticket
        extracted.setResources(cset);

        return extracted;
    }

}
