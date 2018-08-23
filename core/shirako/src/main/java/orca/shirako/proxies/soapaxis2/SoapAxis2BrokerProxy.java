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
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.proxies.soapaxis2.beans.Plist;
import orca.shirako.proxies.soapaxis2.services.AuthorityServiceStub;
import orca.shirako.proxies.soapaxis2.services.BrokerServiceStub;
import orca.shirako.proxies.soapaxis2.services.Claim;
import orca.shirako.proxies.soapaxis2.services.ExtendTicket;
import orca.shirako.proxies.soapaxis2.services.Relinquish;
import orca.shirako.proxies.soapaxis2.services.Ticket;
import orca.shirako.proxies.soapaxis2.util.Translate;
import orca.shirako.util.RPCError;
import orca.shirako.util.RPCException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Stub;
import org.apache.log4j.Logger;

/**
 * Proxy representing an agent using SOAP for communication.
 */
public class SoapAxis2BrokerProxy extends SoapAxis2Proxy implements IBrokerProxy {
    public SoapAxis2BrokerProxy() {
        stubType = TypeBroker;
    }

    /**
     * {@inheritDoc}
     */
    public SoapAxis2BrokerProxy(String serviceURL, AuthToken identity, Logger logger) {
        super(serviceURL, identity, logger);
        stubType = TypeBroker;
    }

    public void execute(IRPCRequestState state) throws RPCException {
        SoapAxis2ProxyRequestState soap = (SoapAxis2ProxyRequestState) state;
        switch (soap.getType()) {
            case Ticket: {
                try {
                    Ticket request = new Ticket();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).ticket(request, soap.getCaller());
                    } else {
                        ((AuthorityServiceStub) stub).ticket(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;
            case Claim: {
                try {
                    Claim request = new Claim();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).claim(request, soap.getCaller());
                    } else {
                        ((AuthorityServiceStub) stub).claim(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;
            case ExtendTicket: {
                try {
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    ExtendTicket request = new ExtendTicket();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).extendTicket(request, soap.getCaller());
                    } else {
                        ((AuthorityServiceStub) stub).extendTicket(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;
            case Relinquish: {
                try {
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    Relinquish request = new Relinquish();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).relinquish(request, soap.getCaller());
                    } else {
                        ((AuthorityServiceStub) stub).relinquish(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;

            default:
                super.execute(state);
                break;
        }
    }

    public IRPCRequestState prepareTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passBrokerReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    public IRPCRequestState prepareClaim(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passBrokerReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    public IRPCRequestState prepareExtendTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passBrokerReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    public IRPCRequestState prepareRelinquish(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passBrokerReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    /**
     * Converts a slices reservation to a reservation bean that can be sent over
     * SOAP.
     * @param reservation The slices reservation
     * @param auth auth
     * @return orca.shirako.proxies.soapaxis2.beans.Reservation
     */
    protected orca.shirako.proxies.soapaxis2.beans.Reservation passBrokerReservation(IReservation reservation, AuthToken auth) {
        IClientReservation r = (IClientReservation) reservation;
        orca.shirako.proxies.soapaxis2.beans.Reservation rsvn = new orca.shirako.proxies.soapaxis2.beans.Reservation();

        rsvn.setSlice(Translate.translate(r.getSlice()));
        rsvn.setTerm(Translate.translate(r.getRequestedTerm()));
        rsvn.setReservationID(r.getReservationID().toString());
        rsvn.setSequence(r.getTicketSequenceOut());

        orca.shirako.proxies.soapaxis2.beans.ResourceSet rset = Translate.translate(r.getRequestedResources(), Translate.DirectionAgent);
        IConcreteSet cset = r.getRequestedResources().getResources();
        
        if (cset != null) {
            Plist encoded = null;
            try {
                Properties enc = cset.encode(OrcaConstants.ProtocolSoapAxis2);
                encoded = encodePropertiesSoap(enc);
            } catch (Exception e) {
                throw new RuntimeException("Cannot encode concrete set", e);
            }

            if (encoded == null) {
                throw new RuntimeException("Unsupported IConcreteSet: " + cset.getClass().getCanonicalName());
            }

            rset.setConcrete(encoded);
        }

        rsvn.setResourceSet(rset);

        return rsvn;
    }
}
