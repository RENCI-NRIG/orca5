/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.proxies.local;

import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.kernel.BrokerReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;

/**
 * Local proxy for Broker. Allows communication with a Broker in the same
 * container as the caller.
 */
public class LocalBroker extends LocalProxy implements IBrokerProxy {
    public LocalBroker() {
    }

    /**
     * Create a new proxy for the specified actor
     * @param actor
     */
    public LocalBroker(IActor actor) {
        super(actor);
    }

    public IRPCRequestState prepareTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservation((IClientReservation)reservation, caller);
        state.callback = callback;
        return state;
    }


    public IRPCRequestState prepareClaim(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservation((IClientReservation) reservation, caller);
        state.callback = callback;
        return state;
    }
    
    public IRPCRequestState prepareExtendTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller){
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservation((IClientReservation) reservation, caller);
        state.callback = callback;
        return state;
    }
    
    public IRPCRequestState prepareRelinquish(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        LocalProxyRequestState state = new LocalProxyRequestState();
        state.reservation = passReservation((IClientReservation) reservation, caller);
        state.callback = callback;
        return state;
    }

    /**
     * Converts a reservation to be passed from one actor to another
     * @param r The reservation
     * @param auth The auth token of the caller
     * @throws Exception
     * @return
     */
    protected IBrokerReservation passReservation(IClientReservation r, AuthToken auth)  {

        ISlice slice = r.getSlice().cloneRequest();

        // ResourceSet rset = r.resources.abstractCloneAgent();
        ResourceSet rset = abstractCloneBroker(r.getRequestedResources());
        // merge the request properties from the slice. The properties in the
        // reservation have precedence.
        ResourceData.mergePropertiesPriority(r.getSlice().getRequestProperties(), rset.getResourceData().getRequestProperties());

        Term term = (Term) (r.getRequestedTerm().clone());

        IBrokerReservation ar = BrokerReservationFactory.getInstance().create(r.getReservationID(), rset, term, slice);

        ar.setSequenceIn(r.getTicketSequenceOut());

        /*
         * Set reservation owner as identity of destination server, so the
         * server can establish its identity on callbacks.
         */

        ar.setOwner(getIdentity());

        return ar;
    }
}
