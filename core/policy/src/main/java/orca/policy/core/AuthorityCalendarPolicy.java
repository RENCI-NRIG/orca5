/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.ConfigurationException;
import orca.shirako.core.AuthorityPolicy;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.time.Term;
import orca.shirako.time.calendar.AuthorityCalendar;
import orca.shirako.util.ReservationSet;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.OrcaRuntimeException;
import orca.util.ResourceType;
import orca.util.persistence.CustomRestorable;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.Persistent;

/**
 * The base for authority policy implementations
 * 
 * @author aydan
 */
public class AuthorityCalendarPolicy extends AuthorityPolicy implements CustomRestorable {
    /**
     * If true, we will use lazy revocation.
     */
    @Persistent(key = "LazyClose")
    protected boolean lazyClose = false;

    /**
     * Resource control objects indexed by guid.
     */
    @Persistent(key = "ResourceControls")
    protected HashMap<ID, IResourceControl> controlsByGuid;

    /**
     * ResourceControl objects indexed by resource type.
     */
    @NotPersistent
    protected HashMap<ResourceType, IResourceControl> controlsByResourceType;

    /**
     * The authority's calendar. A calendar of all requests
     */
    @NotPersistent
    protected AuthorityCalendar calendar;

    /**
     * Says if the actor has been initialized
     */
    @NotPersistent
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    public AuthorityCalendarPolicy() {
        controlsByResourceType = new HashMap<ResourceType, IResourceControl>();
        controlsByGuid = new HashMap<ID, IResourceControl>();
    }


    /**
     * Custom restore function. Invoked during recovering the policy object.
     */
    public void restore(Properties p) throws PersistenceException {
        for (IResourceControl c : controlsByGuid.values()) {
            try {
                registerControlTypes(c);
            } catch (ConfigurationException e) {
                throw new PersistenceException("Cannot restore resource control", e);
            }
        }
    }

    @Override
    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();
            this.calendar = new AuthorityCalendar(clock);
            initializeControls();
            initialized = true;
        }
    }

    /**
     * Initializes all registered controls.
     * 
     * @throws OrcaException
     */
    protected void initializeControls() throws OrcaException {
        for (IResourceControl c : controlsByGuid.values()) {
            c.setActor(actor);
            c.initialize();
        }
    }

    @Override
    public void donate(ResourceSet rset) throws Exception {
        super.donate(rset);
        IResourceControl rc = getControl(rset.getType());
        if (rc != null) {
            rc.donate(rset);
        } else {
            throw new Exception("Unsupported resource type " + rset.getType());
        }
    }

    @Override
    public void eject(ResourceSet rset) throws Exception {
        super.eject(rset);
        IResourceControl rc = getControl(rset.getType());
        if (rc != null) {
            rc.eject(rset);
        } else {
            throw new Exception("Unsupported resource type");
        }
    }

    @Override
    public int unavailable(ResourceSet rset) throws Exception {
        int code = super.unavailable(rset);
        if (code == 0) {
            IResourceControl rc = getControl(rset.getType());
            if (rc != null) {
                code = rc.unavailable(rset);
            } else {
                throw new Exception("Unsupported resource type");
            }
        }
        return code;
    }

    @Override
    public void available(ResourceSet rset) throws Exception {
        super.available(rset);
        IResourceControl rc = getControl(rset.getType());
        if (rc != null) {
            rc.available(rset);
        } else {
            throw new Exception("Unsupported resource type");
        }
    }

    @Override
    public void freed(ResourceSet rset) {
        IResourceControl rc = getControl(rset.getType());
        if (rc != null) {
            try {
                rc.freed(rset);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported resource type");
        }
    }

    @Override
    public void release(ResourceSet rset) {
        IResourceControl rc = getControl(rset.getType());
        if (rc != null) {
            try {
                rc.release(rset);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported resource type");
        }
    }

    public void recoveryStarting() {
        super.recoveryStarting();
        for (IResourceControl c : controlsByGuid.values()) {
            c.recoveryStarting();
        }
    }
    
    public void revisit(final IReservation reservation) throws Exception {
        super.revisit(reservation);
        
        if (reservation instanceof IAuthorityReservation) {
            IAuthorityReservation r = (IAuthorityReservation)reservation;
            // add it to the closing calendar
            calendar.addClosing(r, getClose(r.getTerm()));
            ResourceSet approved = r.getApprovedResources();
            if (approved == null) {
                logger.debug("Reservation has no approved resources. Nothing is allocated to it.");
                return;
            }
            
            // FIXME: the mapping to resource control can be better, e.g., we can use a local property
            // to remember the resource control we used for the allocation, rather than assuming that 
            // mapping was done based on type.
            
            ResourceType rtype = approved.getType();
            logger.debug("Resource type for recovered reservation: " + rtype);
            IResourceControl control = getControl(rtype);
            if (control == null) {
                throw new OrcaException("Missing resource control");
            }
            control.revisit(r);
        }
    }

    public void recoveryEnded() {
        super.recoveryEnded();
        for (IResourceControl c : controlsByGuid.values()) {
            c.recoveryEnded();
        }    
    }
    
    @Override
    public void donate(IClientReservation r) throws Exception {
        super.donate(r);
        IResourceControl rc = getControl(r.getType());
        if (rc != null) {
            rc.donate(r);
        } else {
            throw new Exception("Unsupported resource type");
        }
    }

    @Override
    public boolean bind(IAuthorityReservation r) throws Exception {
        /*
         * Simple for now: make sure that this is a valid term and do not modify
         * its start/end time and add it to the calendar. If the request came
         * after its start time, but before its end cycle, add it for allocation
         * to lastAllocatedCycle + 1. If it came after its end cycle, throw.
         */
        long currentCycle = actor.getCurrentCycle();
        Term approved = new Term(r.getRequestedTerm());
        long start = clock.cycle(approved.getNewStartTime());

        if (start <= currentCycle) {
            // this request is late
            long end = clock.cycle(approved.getEndTime());
            if (end <= currentCycle) {
                // this request is too late
                error("The request cannot be redeemed: its term has expired");
            }

            start = currentCycle + 1;
        }
        calendar.addRequest(r, start);

        long close = getClose(r.getRequestedTerm());
        calendar.addClosing(r, close);
        return false;
    }

    @Override
    public boolean extend(IAuthorityReservation reservation) throws Exception {
        /*
         * Simple for now: make sure that this is a valid term and do not modify
         * its start/end time and add it to the calendar. If the request came
         * after its start time, but before its end cycle, add it for allocation
         * to lastAllocatedCycle + 1. If it came after its end cycle, throw an
         * exception.
         */
        long currentCycle = actor.getCurrentCycle();
        Term approved = new Term(reservation.getRequestedTerm());
        long start = clock.cycle(approved.getNewStartTime());

        if (start <= currentCycle) {
            // this request is late
            long end = clock.cycle(approved.getEndTime());
            if (end <= currentCycle) {
                // this request is too late
                error("The request cannot be redeemed: its term has expired");
            }
            start = currentCycle + 1;
        }

        calendar.removeClosing(reservation);
        calendar.addRequest(reservation, start);
        long close = getClose(reservation.getRequestedTerm());
        calendar.addClosing(reservation, close);
        return false;
    }

    @Override
    public void extend(IReservation reservation, ResourceSet resources, Term term) {
        throw new OrcaRuntimeException("Not implemented");
    }

    @Override
    public void correctDeficit(final IAuthorityReservation reservation) throws Exception {
        if (reservation.getResources() != null) {
            IResourceControl rc = getControl(reservation.getResources().getType());
            if (rc != null) {
                finishCorrectDeficit(rc.correctDeficit(reservation), reservation);
            } else {
                throw new RuntimeException("Unsupported resource type");
            }
        }
    }

    @Override
    public void close(final IReservation reservation) {
        calendar.removeScheduledOrInProgress(reservation);
        if (reservation.getType() != null) {
            IResourceControl rc = getControl(reservation.getType());
            if (rc != null) {
                try {
                    rc.close(reservation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void closed(final IReservation reservation) {
        if (reservation instanceof IAuthorityReservation) {
            calendar.removeOutlay((IAuthorityReservation) reservation);
        }
    }

    @Override
    public void remove(IReservation reservation) {
        throw new OrcaRuntimeException("Not implemented");
    }

    @Override
    public void finish(long cycle) {
        super.finish(cycle);
        calendar.tick(cycle);
    }

    @Override
    public void assign(long cycle) throws Exception {
        try {
            ReservationSet requests = getRequests(cycle);
            mapForCycle(requests, cycle);
        } catch (Exception e) {
            logger.error("error in assign: ", e);
        }
    }

    /**
     * Orders mapper request processing for this cycle.
     * 
     * @param requests
     *            The requests for this cycle
     * @param cycle
     *            The cycle
     * @throws Exception
     */
    protected void mapForCycle(ReservationSet requests, long cycle) throws Exception {
        if ((requests == null) || (requests.size() == 0)) {
            logger.debug("Authority requests for cycle " + cycle + " = [none]");
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Authority requests for cycle " + cycle + " = " + requests);
        }

        /*
         * Walk through everything we need to do in this cycle. First map any
         * reservations that are releasing resources in this cycle (or staying
         * the same), then map reservations that are allocating.
         */
        mapShrinking(requests);
        mapGrowing(requests);

        logger.debug("Completed authority mapForCycle " + cycle);
    }

    /**
     * Maps reservations that are shrinking or staying the same (extending with
     * no flex) in this cycle, and removes them from the bid set.
     * 
     * @param bids
     *            set of deferred operations for this cycle (non-null)
     * @throws Exception
     */
    protected void mapShrinking(ReservationSet bids) throws Exception {
        logger.debug("Processing shrinking requests");
        Iterator<IReservation> i = bids.iterator();
        while (i.hasNext()) {
            IAuthorityReservation r = (IAuthorityReservation) i.next();
            long adjust = r.getDeficit();
            if (adjust > 0) {
                continue;
            }
            if (!r.isTerminal() && r.isExtendingLease()) {
                if (logger.isDebugEnabled()) {
                    if (adjust < 0) {
                        logger.debug("**Shrinking reservation by " + adjust + ": " + r);
                    } else {
                        if (adjust == 0) {
                            logger.debug("**Extending reservation (no flex): " + r);
                        }
                    }
                }
                map(r);
                i.remove();
            }
        }
    }

    /**
     * Maps reservations that are growing in this cycle (redeems or expanding
     * extends), and removes them from the bid set.
     * 
     * @param bids
     *            set of deferred operations for this cycle (non-null)
     * @throws Exception
     */
    protected void mapGrowing(ReservationSet bids) throws Exception {
        logger.debug("Processing growing requests");
        Iterator<IReservation> i = bids.iterator();
        while (i.hasNext()) {
            IAuthorityReservation r = (IAuthorityReservation) i.next();
            if (r.isTerminal()) {
                continue;
            }
            long adjust = r.getDeficit();
            assert adjust > 0;
            if (logger.isDebugEnabled()) {
                if (r.isExtendingLease()) {
                    logger.debug("**Growing reservation by " + adjust + ": " + r.toLogString());
                } else {
                    logger.debug("**Redeeming reservation for " + adjust + ": " + r.toLogString());
                }
            }
            map(r);
            i.remove();
        }
    }

    /**
     * Maps a reservation. Indicates we will approve the request: update its
     * expire time in the calendar, and issue a map probe. The map probe will
     * result in a retry of the mapper request through <code>bind</code> or
     * <code>extend</code> above, which will release the request to the
     * associated mapper.
     * 
     * @param r
     *            the reservation
     * @throws Exception
     */
    protected void map(IAuthorityReservation r) throws Exception {
        ResourceSet assigned = assign(r);
        if (assigned != null) {
            Term approved = new Term(r.getRequestedTerm());
            r.setApproved(approved, assigned);
            r.setBidPending(false);
        } else {
            // reschedule only if the reservation has not been failed/closed
            if (!r.isTerminal()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Deferring reservation " + r + " for the next cycle: "
                            + (actor.getCurrentCycle() + 1));
                }
                reschedule(r);
            }
        }
    }

    /**
     * Assign resources for the given reservation
     * 
     * @param reservation
     *            the request
     * @return a set of resources for the request
     * @throws Exception
     */
    protected ResourceSet assign(IAuthorityReservation reservation) throws Exception {
        IResourceControl rc = getControl(reservation.getRequestedResources().getType());

        if (rc != null) {
            try {
                return rc.assign(reservation);
            } catch (Exception e) {
                /*
                 * FIXME: For now we will assume that if the control raised an
                 * exception it is because it did not have resources. We should
                 * go back to the control implementations and make sure that we
                 * do not use exceptions to indicate lack of resources.
                 * Exceptions should be used only for unrecoverable error
                 * conditions.
                 */
                logger.error("Could not assign", e);
                return null;
            }
        } else {
            throw new Exception("Unsupported resource type");
        }
    }

    @Override
    public void configurationComplete(String action, ConfigToken token, Properties outProperties) {
        super.configurationComplete(action, token, outProperties);

        IResourceControl rc = getControl(token.getResourceType());

        if (rc != null) {
            try {
                rc.configurationComplete(action, token, outProperties);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported resource type");
        }

    }

    /**
     * See if a reservation has expired
     * 
     * @param r
     *            reservation
     * @return <code>true</code> if the reservation expired; otherwise, return
     *         <code>false</code>
     */
    protected boolean isExpired(IReservation r) throws Exception {
        Date now = new Date(System.currentTimeMillis());
        Date end = r.getTerm().getEndTime();

        return !(end.after(now));
    }

    /**
     * Reschedule a reservation into the calendar
     * 
     * @param r
     *            the reservation
     */
    protected void reschedule(IAuthorityReservation r) {
        calendar.removeRequest(r);
        calendar.addRequest(r, actor.getCurrentCycle() + 1);
    }

    /**
     * Return the cycle when a term closes
     * 
     * @param term
     *            the term
     * @return the cycle of the end of a term
     */
    protected long getClose(Term term) {
        if (lazyClose) {
            return -1;
        } else {
            return clock.cycle(term.getEndTime()) + 1;
        }
    }

    @Override
    public ReservationSet getClosing(long cycle) {
        return calendar.getClosing(cycle);
    }

    protected ReservationSet getRequests(long cycle) {
        return calendar.getRequests(cycle);
    }

    public IResourceControl getControl(ID guid) {
        return controlsByGuid.get(guid.toString());
    }

    /**
     * Obtains the control object for the given resource type
     * 
     * @param type
     *            resource type
     * @return a control
     * @throws Exception
     */
    protected IResourceControl getControl(ResourceType type) {
        return (IResourceControl) controlsByResourceType.get(type);
    }

    /**
     * Returns a reverse map of resource control to resource types. The table is
     * indexed by the resource control object and each entry is a linked list of
     * resource types.
     * 
     * @return a table of all of the different control types
     */
    public Hashtable<IResourceControl, List<String>> getControlTypes() {
        Hashtable<IResourceControl, List<String>> types = new Hashtable<IResourceControl, List<String>>();
        Iterator<Map.Entry<ResourceType, IResourceControl>> iter = controlsByResourceType.entrySet()
                .iterator();

        while (iter.hasNext()) {
            Map.Entry<ResourceType, IResourceControl> entry = iter.next();
            String t = (String) entry.getKey().toString();
            IResourceControl c = (IResourceControl) entry.getValue();
            List<String> list = types.get(c);

            if (list == null) {
                list = new ArrayList<String>();
                types.put(c, list);
            }

            list.add(t);
        }

        return types;
    }

    /**
     * Returns a comma-separated strings of all resource types the given
     * resource control is registered to serve.
     * 
     * @param types
     *            the control types
     * @param c
     *            a control
     * @return a string representation of the control types
     * @throws Exception
     */
    protected String getTypes(Hashtable<IResourceControl, List<String>> types, IResourceControl c)
            throws Exception {
        List<String> list = types.get(c);

        if (list == null) {
            throw new Exception("no types");
        }

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(list.get(i));
        }

        return sb.toString();
    }

    /**
     * Registers the given control for the specified resource type. If the
     * policy plugin has already been initialized, the control should be
     * initialized.
     * 
     * @param type
     *            resource type
     * @param control
     *            the control
     */
    public void registerControl(IResourceControl control) throws ConfigurationException {
        registerControlTypes(control);
        controlsByGuid.put(control.getGuid(), control);
    }

    private void registerControlTypes(IResourceControl control) throws ConfigurationException {
        ResourceType[] types = control.getTypes();
        if (types == null || types.length == 0) {
            throw new ConfigurationException("Resource control does not specify any types");
        }

        for (int i = 0; i < types.length; ++i) {
            if (types[i] == null) {
                throw new ConfigurationException("Invalid resource type specified");
            }
        }
        int i = 0;
        try {
            for (; i < types.length; ++i) {
                if (controlsByResourceType.containsKey(types[i])) {
                    throw new ConfigurationException("There is already a control associated with resource type "
                            + types[i].toString());
                }
                controlsByResourceType.put(types[i], control);
            }
        } catch (ConfigurationException e) {
            for (int j = 0; j < i; ++j) {
                controlsByResourceType.remove(types[i]);
            }
            throw e;
        }
    }
}