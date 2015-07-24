/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.proxies.soapaxis2;

import java.rmi.RemoteException;
import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.security.AuthToken;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IServerReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.proxies.soapaxis2.beans.Plist;
import orca.shirako.proxies.soapaxis2.services.ActorServiceStub;
import orca.shirako.proxies.soapaxis2.services.UpdateLease;
import orca.shirako.proxies.soapaxis2.services.UpdateTicket;
import orca.shirako.proxies.soapaxis2.util.Translate;
import orca.shirako.time.Term;
import orca.shirako.util.RPCError;
import orca.shirako.util.RPCException;
import orca.shirako.util.UpdateData;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

/**
 * Implements an actor callback using SOAP.
 * @author aydan
 */
public class SoapAxis2Return extends SoapAxis2Proxy implements IServiceManagerCallbackProxy {
    /**
     * Construct a new SOAP callback.
     * @param serviceEndpoint Endpoint for the callback
     * @param identity Identity of the actor waiting for the callback
     * @param logger Logger
     */
    public SoapAxis2Return(String serviceEndpoint, AuthToken identity, Logger logger) {
        super(serviceEndpoint, identity, logger);
        stubType = TypeReturn;
        callback = true;
    }

    public SoapAxis2Return() {
        stubType = TypeReturn;
        callback = true;
    }

    public void execute(IRPCRequestState state) throws RPCException {
        SoapAxis2ProxyRequestState soap = (SoapAxis2ProxyRequestState) state;
        switch (state.getType()) {
            case UpdateTicket: {
                try {
                    ActorServiceStub stub = (ActorServiceStub) getServiceStub(soap.getCaller());
                    UpdateTicket request = new UpdateTicket();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setUpdateData(soap.udd);
                    request.setCallbackURL(soap.callbackUrl);
                    // invoke the stub
                    stub.updateTicket(request, soap.getCaller());
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            } break;

            case UpdateLease: {
                try {
                    ActorServiceStub stub = (ActorServiceStub) getServiceStub(soap.getCaller());
                    UpdateLease request = new UpdateLease();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setUpdateData(soap.udd);
                    request.setCallbackURL(soap.callbackUrl);
                    // invoke the stub
                    stub.updateLease(request, soap.getCaller());
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            } break;

            default:
                super.execute(state);
                break;
        }
    }

    public IRPCRequestState prepareUpdateTicket(IBrokerReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passReservation(reservation, caller);
        state.udd = Translate.translate(udd);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();        
        return state;
    }

    public IRPCRequestState prepareUpdateLease(IAuthorityReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passReservation(reservation, caller);
        state.udd = Translate.translate(udd);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    private orca.shirako.proxies.soapaxis2.beans.Reservation passReservation(IServerReservation r, AuthToken auth) {
        orca.shirako.proxies.soapaxis2.beans.Reservation res = new orca.shirako.proxies.soapaxis2.beans.Reservation();
        res.setSlice(Translate.translate(r.getSlice()));
        // attach the reservation id of the destination
        res.setReservationID(r.getReservationID().toString());

        Term term = null;

        if (r.getTerm() == null) {
            // we failed before giving this reservation any resources
            term = (Term) r.getRequestedTerm().clone();
        } else {
            term = (Term) r.getTerm().clone();
        }

        // attach the term
        res.setTerm(Translate.translate(term));

        // copy the resource set
        orca.shirako.proxies.soapaxis2.beans.ResourceSet rset = null;

        if (r.getResources() == null) {
            rset = Translate.translate(new orca.shirako.kernel.ResourceSet(0, r.getRequestedType()), Translate.DirectionReturn);
        } else {
            rset = Translate.translate(r.getResources(), Translate.DirectionReturn);

            // check if there is a concrete set and pass it
            IConcreteSet cset = r.getResources().getResources();

            if (cset != null) {
                Plist encoded = null;
                try {
                    Properties enc = cset.encode(OrcaConstants.ProtocolSoapAxis2);
                    encoded = encodePropertiesSoap(enc);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot encode IConcreteSet", e);
                }

                if (encoded == null) {
                    throw new RuntimeException("Unsupported IConcreteSet: " + cset.getClass().getCanonicalName());
                }

                rset.setConcrete(encoded);
            }
        }

        // attach the resource set
        res.setResourceSet(rset);

        return res;
    }
}
