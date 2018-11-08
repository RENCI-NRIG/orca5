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
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.proxies.soapaxis2.beans.Plist;
import orca.shirako.proxies.soapaxis2.services.AuthorityServiceStub;
import orca.shirako.proxies.soapaxis2.services.Close;
import orca.shirako.proxies.soapaxis2.services.ExtendLease;
import orca.shirako.proxies.soapaxis2.services.ModifyLease;
import orca.shirako.proxies.soapaxis2.services.Redeem;
import orca.shirako.proxies.soapaxis2.util.Translate;
import orca.shirako.util.RPCError;
import orca.shirako.util.RPCException;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

/**
 * Proxy representing an Authority using SOAP for communication
 */
public class SoapAxis2AuthorityProxy extends SoapAxis2BrokerProxy implements IAuthorityProxy {
    public SoapAxis2AuthorityProxy() {
        stubType = TypeSite;
    }

    /**
     * {@inheritDoc}
     */
    public SoapAxis2AuthorityProxy(String serviceURL, AuthToken identity, Logger logger) {
        super(serviceURL, identity, logger);
        stubType = TypeSite;
    }

    public void execute(IRPCRequestState state) throws RPCException {
        SoapAxis2ProxyRequestState soap = (SoapAxis2ProxyRequestState) state;
        switch (soap.getType()) {
            case Redeem: {
                try {
                    AuthorityServiceStub stub = (AuthorityServiceStub) getServiceStub(soap.getCaller());
                    Redeem request = new Redeem();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    stub.redeem(request, soap.getCaller());
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;

            case ExtendLease: {
                try {
                    AuthorityServiceStub stub = (AuthorityServiceStub) getServiceStub(soap.getCaller());
                    ExtendLease request = new ExtendLease();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    stub.extendLease(request, soap.getCaller());
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;

            case ModifyLease: {
                try {
                    AuthorityServiceStub stub = (AuthorityServiceStub) getServiceStub(soap.getCaller());
                    ModifyLease request = new ModifyLease();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    stub.modifyLease(request, soap.getCaller());
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;    
                
            case Close: {
                try {
                    AuthorityServiceStub stub = (AuthorityServiceStub) getServiceStub(soap.getCaller());
                    Close request = new Close();
                    request.setMessageID(soap.getMessageID());
                    request.setReservation(soap.reservation);
                    request.setCallbackURL(soap.callbackUrl);
                    stub.close(request, soap.getCaller());
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

    public IRPCRequestState prepareRedeem(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passAuthorityReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    public IRPCRequestState prepareExtendLease(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passAuthorityReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    public IRPCRequestState prepareModifyLease(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passAuthorityReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }
    
    public IRPCRequestState prepareClose(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.reservation = passAuthorityReservation(reservation, caller);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    protected orca.shirako.proxies.soapaxis2.beans.Reservation passAuthorityReservation(IReservation reservation, AuthToken caller) {
        /*
         * Every communication with an authority through this proxy requires a
         * valid ticket.
         */
        IConcreteSet cs = reservation.getResources().getResources();

        if (cs == null) {
            throw new IllegalArgumentException("Missing ticket");
        }

        /*
         * Create the SOAP bean
         */
        orca.shirako.proxies.soapaxis2.beans.Reservation rsvn = new orca.shirako.proxies.soapaxis2.beans.Reservation();

        rsvn.setSlice(Translate.translate(reservation.getSlice()));
        rsvn.setTerm(Translate.translate(reservation.getTerm()));

        orca.shirako.proxies.soapaxis2.beans.ResourceSet rset = Translate.translate(reservation.getResources(), Translate.DirectionAuthority);
        Plist encoded = null;
        try {
            Properties enc = cs.encode(OrcaConstants.ProtocolSoapAxis2);
            encoded = encodePropertiesSoap(enc);
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode concrete set");
        }

        if (encoded == null) {
            throw new RuntimeException("Unsupported IConcreteSet: " + cs.getClass().getCanonicalName());
        }

        rset.setConcrete(encoded);
        rsvn.setResourceSet(rset);

        rsvn.setReservationID(reservation.getReservationID().toString());
        rsvn.setSequence(((IServiceManagerReservation) reservation).getLeaseSequenceOut());

        return rsvn;
    }
}
