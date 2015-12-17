/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

/**
 * AuthorityServiceSkeleton.java This file was auto-generated from WSDL by the
 * Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package orca.shirako.proxies.soapaxis2.services;

import java.rmi.RemoteException;
import java.security.PublicKey;
import java.security.cert.Certificate;

import orca.security.AuthToken;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.SharpCertificate;
import orca.shirako.common.delegation.SharpResourceTicket;
import orca.shirako.core.Ticket;
import orca.shirako.kernel.AuthorityReservationFactory;
import orca.shirako.kernel.IncomingRPC;
import orca.shirako.kernel.IncomingReservationRPC;
import orca.shirako.kernel.RPCRequestType;
import orca.shirako.proxies.soapaxis2.util.ContextTools;
import orca.shirako.proxies.soapaxis2.util.Translate;
import orca.shirako.util.RPCError;

import org.apache.axis2.AxisFault;

/**
 * AuthorityServiceSkeleton java skeleton for the axisService
 */
public class AuthorityServiceSkeleton extends BrokerServiceSkeleton {
    public AuthorityServiceSkeleton() throws Exception {
        super();
    }

    /**
     * Converts a soap.beans.Reservation to a slices.Reservation
     * @param beanReservation
     * @return
     */
    protected IAuthorityReservation passAuthority(orca.shirako.proxies.soapaxis2.beans.Reservation beanReservation) throws Exception {
        // translate all beans to slices objects
        ISlice s = Translate.translate(beanReservation.getSlice());
        orca.shirako.time.Term term = Translate.translate(beanReservation.getTerm());
        orca.shirako.kernel.ResourceSet rset = Translate.translate(beanReservation.getResourceSet());
        IConcreteSet cset = getConcrete(beanReservation);

        if (cset == null) {
            throw new Exception("Unsupported Concrete type");
        }

        rset.setResources(cset);

        IAuthorityReservation ar = AuthorityReservationFactory.getInstance().create(new ReservationID(beanReservation.getReservationID()), rset, term, s);
        // set it to be owned by the authority
        ar.setOwner(actor.getIdentity());
        ar.setSequenceIn((int) beanReservation.getSequence());

        return ar;
    }

    public orca.shirako.proxies.soapaxis2.services.CloseResponse close(orca.shirako.proxies.soapaxis2.services.Close request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            // recreate the reservation
            IReservation pr = passAuthority(request.getReservation());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.Close, pr, authToken);
        } catch (Exception e) {
            String msg = "Invalid close request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new CloseResponse();
    }

    public orca.shirako.proxies.soapaxis2.services.RedeemResponse redeem(orca.shirako.proxies.soapaxis2.services.Redeem request) throws RemoteException {
        // take the auth token as it is (we shall validate it later)
        AuthToken authToken = ContextTools.getClientAuthToken();
        // first we extract the request
        IncomingRPC rpc;
        IAuthorityReservation pr;
        try {
            // recreate the reservation
            pr = passAuthority(request.getReservation());
            IServiceManagerCallbackProxy cb = (IServiceManagerCallbackProxy) getCallback(request.getCallbackURL(), authToken);
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.Redeem, pr, cb, authToken);
        } catch (Exception e) {
            String msg = "Invalid redeem request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        
        try {
            IConcreteSet cs = pr.getRequestedResources().getResources();
            Ticket t = (Ticket)cs;
            ResourceTicket rt = t.getTicket();
            SharpResourceTicket srt = (SharpResourceTicket)rt;
            // obtain the SharpCertificate - represents all delegations from the root of the ticket.
            SharpCertificate cert = srt.getCertificate();
            // validate that this is a valid SharpCertificate
            if (!cert.isValid()) {
                throw new RuntimeException("The SharpCertificate is invalid");
            }
            // Obtain the public key of the actor and the public key at the root of the ticket
            PublicKey myKey = actor.getShirakoPlugin().getKeyStore().getActorCertificate().getPublicKey();
            PublicKey ticketRootKey = cert.getRoot().getPublicKey();
            // both keys must be the same or this ticket did not originate from this actor
            if (!myKey.equals(ticketRootKey)) {
                throw new RuntimeException("The ticket for this redeem request did not originate from this site");
            }
            
            // by now we've established that the SharpCertificate is valid and that we can trust the certificate
            // of the holder of the ticket. Add it to our keystore, if it is not already there.
            
            Certificate callerCert = cert.getHolderCertificate();
            // add the certificate if it is not already present
            actor.getShirakoPlugin().getKeyStore().addTrustedCertificate(authToken.getGuid().toString(), callerCert);
        } catch (Exception e) {
            String msg = "Could not process caller certificate";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.Authorization), e);
        }
            
        // by now the cert should be in the actor keystore, so we can validate the request
        doAuthorize();
        doDispatch(rpc);
        
        return new RedeemResponse();
    }

    public orca.shirako.proxies.soapaxis2.services.ExtendLeaseResponse extendLease(orca.shirako.proxies.soapaxis2.services.ExtendLease request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            // recreate the reservation
            IAuthorityReservation pr = passAuthority(request.getReservation());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.ExtendLease, pr, authToken);
        } catch (Exception e) {
            String msg = "Invalid extendLease request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new ExtendLeaseResponse();
    }
    
    public orca.shirako.proxies.soapaxis2.services.ModifyLeaseResponse modifyLease(orca.shirako.proxies.soapaxis2.services.ModifyLease request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            // recreate the reservation
            IAuthorityReservation pr = passAuthority(request.getReservation());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.ModifyLease, pr, authToken);
        } catch (Exception e) {
            String msg = "Invalid modifyLease request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new ModifyLeaseResponse();
    }
    
}
