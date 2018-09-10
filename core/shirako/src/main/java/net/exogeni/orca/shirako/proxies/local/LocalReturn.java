/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.proxies.local;

import java.util.Properties;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IAuthorityReservation;
import net.exogeni.orca.shirako.api.IBrokerReservation;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IConcreteSet;
import net.exogeni.orca.shirako.api.IRPCRequestState;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IServerReservation;
import net.exogeni.orca.shirako.api.IServiceManagerCallbackProxy;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;
import net.exogeni.orca.shirako.api.IShirakoPlugin;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.kernel.ClientReservationFactory;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.kernel.ServiceManagerReservationFactory;
import net.exogeni.orca.shirako.proxies.Proxy;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.UpdateData;

/**
 * Callback object to an actor residing in the same container as the caller.
 */
public class LocalReturn extends LocalProxy implements IServiceManagerCallbackProxy {
    public LocalReturn() {
        callback = true;
    }

    /**
     * Create a new callback to the specified actor
     * @param sact The actor
     */
    public LocalReturn(IActor sact) {
        super(sact);
        callback = true;
    }

    public IRPCRequestState prepareUpdateTicket(IBrokerReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservation(reservation);
        state.udd = new UpdateData();
        state.udd.absorb(udd);
        state.callback = callback;
        return state;
    }

    public IRPCRequestState prepareUpdateLease(IAuthorityReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservation(reservation);
        state.udd = new UpdateData();
        state.udd.absorb(udd);
        state.callback = callback;
        return state;
    }

    private IReservation passReservation(IServerReservation r) {
        return passReservation(r, getActor().getShirakoPlugin());
    }
    
    public static IReservation passReservation(IServerReservation r, IShirakoPlugin plugin) {           
        ISlice s = r.getSlice().cloneRequest();

        Term term;

        if (r.getTerm() == null) {
            // we failed before giving this reservation any resources
            term = (Term) r.getRequestedTerm().clone();
        } else {
            term = (Term) r.getTerm().clone();
        }

        ResourceSet rset;

        if (r.getResources() == null) {
            // we did not assign any resources to this reservation
            rset = new ResourceSet(0, r.getRequestedType());
        } else {
            // the reservation already has resources assigned
            rset = abstractCloneReturn(r.getResources());

            // attach the concrete resource (if present)
            IConcreteSet concrete = r.getResources().getResources();

            if (concrete != null) {
                IConcreteSet cset = null;
                try {
                    // encode the concrete set
                    Properties enc = concrete.encode(OrcaConstants.ProtocolLocal);
                    // decode the concrete set
                    cset = Proxy.decode(enc, plugin);
                } catch (Exception e) {
                    throw new RuntimeException("Error while encoding concrete set", e);
                }

                if (cset == null) {
                    throw new IllegalArgumentException("Unsupported ConcreteSet type: " + concrete.getClass().getCanonicalName());
                }

                rset.setResources(cset);
            }
        }

        if (r instanceof IBrokerReservation) {
            IClientReservation rc = ClientReservationFactory.getInstance().create(r.getReservationID(), rset, term, s);
            rc.setTicketSequenceIn(r.getSequenceOut());
            return rc;
        } else {
            IServiceManagerReservation rc = ServiceManagerReservationFactory.getInstance().create(r.getReservationID(), rset, term, s);
            rc.setLeaseSequenceIn(r.getSequenceOut());
            return rc;
        }
    }
}
