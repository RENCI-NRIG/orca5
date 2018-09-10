/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

/**
 * ActorServiceSkeleton.java This file was auto-generated from WSDL by the
 * Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package net.exogeni.orca.shirako.proxies.soapaxis2.services;

import java.rmi.RemoteException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Properties;

import javax.xml.namespace.QName;

import net.exogeni.orca.security.AccessDeniedException;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IConcreteSet;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.common.delegation.SharpCertificate;
import net.exogeni.orca.shirako.common.delegation.SharpResourceTicket;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.kernel.ClientReservationFactory;
import net.exogeni.orca.shirako.kernel.IncomingFailedRPC;
import net.exogeni.orca.shirako.kernel.IncomingQueryRPC;
import net.exogeni.orca.shirako.kernel.IncomingRPC;
import net.exogeni.orca.shirako.kernel.IncomingReservationRPC;
import net.exogeni.orca.shirako.kernel.RPCManager;
import net.exogeni.orca.shirako.kernel.RPCRequestType;
import net.exogeni.orca.shirako.proxies.Proxy;
import net.exogeni.orca.shirako.proxies.soapaxis2.SoapAxis2Proxy;
import net.exogeni.orca.shirako.proxies.soapaxis2.SoapAxis2Return;
import net.exogeni.orca.shirako.proxies.soapaxis2.beans.Plist;
import net.exogeni.orca.shirako.proxies.soapaxis2.beans.Reservation;
import net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet;
import net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools;
import net.exogeni.orca.shirako.proxies.soapaxis2.util.Translate;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.shirako.util.RPCError;
import net.exogeni.orca.shirako.util.RemoteActorException;
import net.exogeni.orca.shirako.util.UpdateData;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.log4j.Logger;

/**
 * ActorServiceSkeleton java skeleton for the axisService
 */
public class ActorServiceSkeleton {
    public static final String OrcaSoapFaultNamespace = "http://geni-net.exogeni.orca.renci.org/";
    public static final String OrcaSoapFaultPrefix = "net.exogeni.orcafault";

    /**
     * The local actor. All web-service calls are translated to objects that the
     * actor understands and forwarded to the actor
     */
    protected IActor actor;

    /**
     * Cached logger
     */
    protected Logger logger;

    /**
     * Actor setup. This code interacts with the ActorRegistry within this
     * container to retrieve the necessary objects needed to initialize the
     * service
     * @throws Exception in case of error
     */
    protected void initialize() throws Exception {
        /*
         * Obtain the current message context and retrieve the service name. By
         * convention, the service name is set to equal the name of the actor it
         * represents.
         */
        MessageContext context = MessageContext.getCurrentMessageContext();
        AxisService svc = context.getAxisService();
        String actorName = svc.getName();
        // call the ActorRegistry to retrieve the actor;
        actor = ActorRegistry.getActor(actorName);

        if (actor == null) {
            throw new Exception("Cannot retrieve actor: " + actorName);
        }

        logger = actor.getLogger();
    }

    /**
     * Construct a SOAP callback object to a service listening on the specified
     * endpoint and identified by the given auth token.
     * @param url callback endpoint
     * @param authToken auth token of the service
     * @return ICallbackProxy
     * @throws Exception in case of error
     */
    protected ICallbackProxy getCallback(String url, AuthToken authToken) throws Exception {
        /*
         * Create a new callback object: attach the actor logger to it so that
         * all events on this callback are linked to the actor.
         */
        SoapAxis2Return callback = new SoapAxis2Return(url, authToken, actor.getLogger());
        return callback;
    }

    /**
     * Extracts the concrete set from a SOAP reservation
     * @param r The SOAP reservation
     * @return IConcreteSet
     * @throws Exception in case of error
     */
    protected IConcreteSet getConcrete(Reservation r) throws Exception {
        ResourceSet soapSet = r.getResourceSet();
        Plist set = soapSet.getConcrete();

        if (set == null) {
            return null;
        }

        // decode the the Plist to Properties
        Properties enc = SoapAxis2Proxy.decodePropertiesSoap(set);
        // convert the Properties to a concreteset
        IConcreteSet cs = Proxy.decode(enc, actor.getShirakoPlugin());
        return cs;
    }

    /**
     * Converts a SOAP reservation to a ReservationClient
     * @param reservation The SOAP reservation
     * @return IClientReservation
     * @throws Exception in case of error
     */
    protected IClientReservation passClient(Reservation reservation) throws Exception {
        /*
         * Convert the SOAP reservation to a slices.ReservationClient
         */
        ISlice slice = Translate.translate(reservation.getSlice());
        net.exogeni.orca.shirako.kernel.ResourceSet rs = Translate.translate(reservation.getResourceSet());
        rs.setResources(getConcrete(reservation));

        return ClientReservationFactory.getInstance().create(new net.exogeni.orca.shirako.common.ReservationID(reservation.getReservationID()), rs, Translate.translate(reservation.getTerm()), slice);
    }

    protected AuthToken _authorize() throws AccessDeniedException {
        // very simple for now:
        // the incoming soap message must be signed by someone whose
        // certificate is in the actor's keystore.
        boolean authorized = false;
        try {
            authorized = ContextTools.verifySignedBy(actor);
        } catch (Exception e) {
            throw new AccessDeniedException("An error occurred while validating request signature", e);
        }
        // obtain the auth token
        AuthToken authToken = ContextTools.getClientAuthToken();
        if (!authorized) {
            String remoteActor = authToken.getName();
            String remoteGuid = "unknown";
            if (authToken.getGuid() != null) {
                remoteGuid = authToken.getGuid().toString();
            }
            if (remoteActor == null) {
                remoteActor = "unknown";
            }
            if (remoteGuid == null) {
                remoteGuid = "unknown";
            }
            throw new AccessDeniedException("Could not validate request signature: local actor=" + actor.getName() + " (" + actor.getGuid() + "), remoteActor=" + remoteActor + " (" + remoteGuid + ")");
        }
        return ContextTools.getClientAuthToken();
    }

    protected QName getFaultCode(RPCError t) {
        return new QName(OrcaSoapFaultNamespace, t.toString(), OrcaSoapFaultPrefix);
    }

    protected AuthToken doAuthorize() throws AxisFault {
        AuthToken authToken;
        try {
            authToken = _authorize();
        } catch (AccessDeniedException e) {
            String msg = "Unauthorized request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.Authorization), e);
        }
        return authToken;
    }

    protected void doDispatch(IncomingRPC rpc) throws AxisFault {
        // enqueue the incoming RPC
        try {
            RPCManager.dispatchIncoming(actor, rpc);
        } catch (RemoteActorException e) {
            String msg = "An error occurred while dispatching request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.RemoteError), e);
        }
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse query(net.exogeni.orca.shirako.proxies.soapaxis2.services.Query request) throws RemoteException {
        // authorize
        AuthToken authToken = doAuthorize();        
        // extract the request
        IncomingRPC rpc;
        try {
            Properties query = Translate.translate(request.getProperties());
            ICallbackProxy callback = getCallback(request.getCallbackURL(), authToken);
            rpc = new IncomingQueryRPC(request.getMessageID(), query, callback, authToken);
        } catch (Exception e) {
            String msg = "Invalid query request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        // dispatch
        doDispatch(rpc);
        return new QueryResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse queryResult(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult request) throws RemoteException {
        // authorize
        AuthToken authToken = doAuthorize();
        // extract the request
        IncomingRPC rpc;
        try {
            Properties query = Translate.translate(request.getProperties());
            rpc = new IncomingQueryRPC(request.getMessageID(), request.getRequestID(), query, authToken);
        } catch (Exception e) {
            String msg = "Invalid queryResult request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        // dispatch
        doDispatch(rpc);
        return new QueryResultResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse updateLease(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease request) throws RemoteException {
        // authorize
        AuthToken authToken = doAuthorize();
        // extract the request
        IncomingRPC rpc;
        try {
            // convert the reservation
            IReservation rsvn = passClient(request.getReservation());
            UpdateData udd = Translate.translate(request.getUpdateData());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.UpdateLease, rsvn, udd, authToken);
        } catch (Exception e) {
            String msg = "Invalid updateLease request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        // dispatch
        doDispatch(rpc);
        return new UpdateLeaseResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse updateTicket(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket request) throws RemoteException {
        // authorize
        AuthToken authToken = doAuthorize();
        // extract the request
        IncomingReservationRPC rpc;
        try {
            // convert the reservation
            IReservation rsvn = passClient(request.getReservation());
            UpdateData udd = Translate.translate(request.getUpdateData());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.UpdateTicket, rsvn, udd, authToken);
        } catch (Exception e) {
            String msg = "Invalid updateTicket request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        
        // Before we dispatch the ticket we must extract the certificate of the site authority
        // and make sure it is present in our keystore. We should also validate that
        // the certificate we've been issued has been issued to us by the broker.
        
        try {
            if (((IServiceManagerReservation)rpc.getReservation()).getResources() != null) {
                IConcreteSet cs = ((IServiceManagerReservation)rpc.getReservation()).getResources().getResources();
                if (cs != null) {
                    Ticket t = (Ticket)cs;
                    ResourceTicket rt = t.getTicket();
                    SharpResourceTicket srt = (SharpResourceTicket)rt;
                    // obtain the SharpCertificate - represents all delegations from the root of the ticket.
                    SharpCertificate cert = srt.getCertificate();
                    // validate that this is a valid SharpCertificate
                    if (!cert.isValid()) {
                        throw new RuntimeException("The SharpCertificate is invalid");
                    }
                    
                    // get the broker's public key from the SharpCertificate
                    PublicKey brokerKey = cert.getIssuerCertificate().getHolderCertificate().getPublicKey();
                    // get the broker's public key from our keystore
                    PublicKey brokerKeyFromMyKeyStore = actor.getShirakoPlugin().getKeyStore().getCertificate(authToken.getGuid().toString()).getPublicKey();
                    if (!brokerKey.equals(brokerKeyFromMyKeyStore)) {
                        throw new RuntimeException("The SharpCert was not issued by the broker we expected");
                    }
                    
                    // make sure that the SharpCertificate was issued for us
                    PublicKey myKey = cert.getHolderCertificate().getPublicKey();
                    PublicKey myKeyFromMyKeystore = actor.getShirakoPlugin().getKeyStore().getActorCertificate().getPublicKey();
                    if (!myKey.equals(myKeyFromMyKeystore)){
                        throw new RuntimeException("The ShaprCert was not issued to me");
                    }
                    
                    // register the site's certificate in our keystore
                    Certificate rootCert = cert.getRoot();
                    String alias = t.getSiteProxy().getIdentity().getGuid().toString();
                    actor.getShirakoPlugin().getKeyStore().addTrustedCertificate(alias, rootCert);
                }
            }
        } catch (Exception e) {
            String msg = "Could not process the incoming certificate(s)";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }

        // dispatch it
        doDispatch(rpc);

//        // FIXME: get rid of this. We need to modify the way the site
//        // processes redeem, instead of having to do this hack here.
//        try {
//            logger.info("Enqueued Incoming updateTicket from <" + authToken.getName() + "> for #" + rpc.getReservation().getReservationID().toString() + ": r=" + rpc.getReservation().toString());
//            if (actor.getType() == IActor.TypeServiceManager) {
//                // FIXME: need a better way to handle this case
//                // The problem is the following:
//                // If a service maanger learns about a new site that it should
//                // talk to,
//                // the sm will have the site's certificate, but the site may not
//                // have the sm's certificate.
//                // when the sm attempts to talk to the site, the site will
//                // reject the attempt.
//                // For now we have a hack that would register the SM's
//                // certificate with the site.
//                // This is ugly and wrong on many levels: we are making an RPC
//                // in the context of another
//                // RPC and should be fixed.
//                ICertificatePolicy policy = actor.getShirakoPlugin().getCertificatePolicy();
//                if (policy != null && !request.getUpdateData().getFailed()) {
//                    policy.onUpdateTicket((net.exogeni.orca.shirako.core.Ticket) rpc.getReservation().getResources().getResources());
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Could not register certificate with site", e);
//            // throw new RemoteException(e.getMessage(), e);
//        }
        return new UpdateTicketResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse failedRPC(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC request) throws RemoteException {
        // authorize
        AuthToken authToken = doAuthorize();
        // extract the request
        IncomingRPC rpc;
        try {
            // extract the requestType
            RPCRequestType failedRequestType = RPCRequestType.convert(request.getRequestType());
            if (request.getReservationID() != null && !request.getReservationID().equals("")) {
                rpc = new IncomingFailedRPC(request.getMessageID(), failedRequestType, request.getRequestID(), new ReservationID(request.getReservationID()), request.getErrorDetails(), authToken);
            }else{
                rpc = new IncomingFailedRPC(request.getMessageID(), failedRequestType, request.getRequestID(), request.getErrorDetails(), authToken);
            }          
        } catch (Exception e) {
            String msg = "Invalid failedRequest";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        // dispatch
        doDispatch(rpc);
        return new FailedRPCResponse();
    }
}
