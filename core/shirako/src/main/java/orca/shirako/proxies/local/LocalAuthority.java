/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.proxies.local;

import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.kernel.AuthorityReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.proxies.Proxy;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;

/**
 * Local proxy for Authority. Allows communication with an Authority residing in
 * the same container as the caller.
 */
public class LocalAuthority extends LocalBroker implements IAuthorityProxy {
    public LocalAuthority() {
    }

    /**
     * Create a proxy to the specified actor
     * @param actor
     */
    public LocalAuthority(IActor actor) {
        super(actor);
    }

    public IRPCRequestState prepareRedeem(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservationAuthority(reservation, caller);
        state.callback = callback;
        return state;
    }

    public IRPCRequestState prepareExtendLease(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservationAuthority(reservation, caller);
        state.callback = callback;
        return state;
    }

    public IRPCRequestState prepareModifyLease(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservationAuthority(reservation, caller);
        state.callback = callback;
        return state;
    }
    
    public IRPCRequestState prepareClose(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservationAuthority(reservation, caller);
        state.callback = callback;
        return state;
    }

    /**
     * Pass the reservation
     * @param r The reservation
     * @param auth The <code>AuthToken</code> of the caller
     * @throws Exception
     */
    private IReservation passReservationAuthority(IServiceManagerReservation r, AuthToken auth) {
        /*
         * for now each request should have a valid ticket. If this changes,
         * comment out the next statement. For example, we could optimize
         * close() not to pass the ticket.
         */
        if (r.getResources().getResources() == null) {
            throw new IllegalStateException("Missing ticket");
        }

        ISlice s = r.getSlice().cloneRequest();
        Term term = (Term) (r.getTerm().clone());

        // clone the resource set without the concrete resources
        ResourceSet rset = abstractCloneAuthority(r.getResources());
        // merge the configuration properties from the slice. The properties in
        // the reservation have precedence.
        ResourceData.mergePropertiesPriority(r.getSlice().getConfigurationProperties(), rset.getResourceData().getConfigurationProperties());

        /*
         * Decode the concrete set using the destination actor's plugin. This is
         * an optimization. In general, we need to encode the concrete set using
         * the caller's plugin and then decode it with the help of the
         * receiver's plugin. In a local proxy we can avoid the first step and
         * directly call decode.
         */
        IConcreteSet originalTicket = r.getResources().getResources();
        try {
            // encode the concrete set
            Properties enc = originalTicket.encode(OrcaConstants.ProtocolLocal);
            // decode the concrete set
            IConcreteSet encodedTicket = Proxy.decode(enc, getActor().getShirakoPlugin());
            rset.setResources(encodedTicket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // create the reservation
        IAuthorityReservation ar = AuthorityReservationFactory.getInstance().create(r.getReservationID(), rset, term, s);
        // attach the outgoing sequence
        ar.setSequenceIn(r.getLeaseSequenceOut());
        /*
         * Set reservation owner as identity of destination server, so the
         * server can establish its identity on callbacks. CCC: Seems hackish.
         * Do we need to do it here in the proxy?
         */
        ar.setOwner(getIdentity());

        return ar;
    }
}
