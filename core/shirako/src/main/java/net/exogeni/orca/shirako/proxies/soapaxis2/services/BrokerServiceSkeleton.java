/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

/**
 * BrokerServiceSkeleton.java This file was auto-generated from WSDL by the
 * Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package net.exogeni.orca.shirako.proxies.soapaxis2.services;

import java.rmi.RemoteException;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IBrokerReservation;
import net.exogeni.orca.shirako.api.IClientCallbackProxy;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.kernel.BrokerReservationFactory;
import net.exogeni.orca.shirako.kernel.IncomingRPC;
import net.exogeni.orca.shirako.kernel.IncomingReservationRPC;
import net.exogeni.orca.shirako.kernel.RPCRequestType;
import net.exogeni.orca.shirako.proxies.soapaxis2.util.Translate;
import net.exogeni.orca.shirako.util.RPCError;

import org.apache.axis2.AxisFault;

/**
 * BrokerServiceSkeleton java skeleton for the axisService
 */
public class BrokerServiceSkeleton extends ActorServiceSkeleton {
    public BrokerServiceSkeleton() throws Exception {
        initialize();
    }

    /**
     * Convert a SOAP reservation to an AgentReservation
     * @param reservation The SOAP reservation
     * @return IBrokerReservation
     * @throws Exception in case of error
     */
    protected IBrokerReservation passAgent(net.exogeni.orca.shirako.proxies.soapaxis2.beans.Reservation reservation) throws Exception {
        ISlice slice = Translate.translate(reservation.getSlice());
        net.exogeni.orca.shirako.kernel.ResourceSet rset = Translate.translate(reservation.getResourceSet());
        net.exogeni.orca.shirako.time.Term term = Translate.translate(reservation.getTerm());
        ReservationID rid = new ReservationID(reservation.getReservationID());

        // recreate the reservation
        IBrokerReservation ar = BrokerReservationFactory.getInstance().create(rid, rset, term, slice);
        // set the owner of this reservation to be local actor
        ar.setOwner(actor.getIdentity());
        ar.setSequenceIn((int) reservation.getSequence());

        return ar;
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse ticket(net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            // convert the reservation
            IBrokerReservation ar = passAgent(request.getReservation());
            IClientCallbackProxy callback = (IClientCallbackProxy) getCallback(request.getCallbackURL(), authToken);
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.Ticket, ar, callback, authToken);
        } catch (Exception e) {
            String msg = "Invalid ticket request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new TicketResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse claim(net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            IBrokerReservation ar = passAgent(request.getReservation());
            IClientCallbackProxy callback = (IClientCallbackProxy) getCallback(request.getCallbackURL(), authToken);
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.Claim, ar, callback, authToken);
        } catch (Exception e) {
            String msg = "Invalid claim request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new ClaimResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse extendTicket(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            // convert the reservation
            IBrokerReservation ar = passAgent(request.getReservation());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.ExtendTicket, ar, authToken);
        } catch (Exception e) {
            String msg = "Invalid extendTicket request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new ExtendTicketResponse();
    }

    public net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse relinquish(net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish request) throws RemoteException {
        AuthToken authToken = doAuthorize();
        IncomingRPC rpc;
        try {
            // convert the reservation
            IBrokerReservation ar = passAgent(request.getReservation());
            rpc = new IncomingReservationRPC(request.getMessageID(), RPCRequestType.Relinquish, ar, authToken);
        } catch (Exception e) {
            String msg = "Invalid relinquish request";
            logger.error(msg, e);
            throw new AxisFault(msg, getFaultCode(RPCError.InvalidRequest), e);
        }
        doDispatch(rpc);
        return new RelinquishResponse();
    }
}
