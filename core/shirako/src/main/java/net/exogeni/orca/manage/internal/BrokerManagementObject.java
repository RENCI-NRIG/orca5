/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.manage.internal;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaProxyProtocolDescriptor;
import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultPoolInfoMng;
import net.exogeni.orca.manage.beans.ResultProxyMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultStringMng;
import net.exogeni.orca.manage.beans.ResultStringsMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.manage.internal.local.LocalBroker;
import net.exogeni.orca.manage.proxies.soap.SoapBroker;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IBroker;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public class BrokerManagementObject extends ServerActorManagementObject implements IClientActorManagementObject {
	/**
	 * The agent represented by this wrapper
	 */
	protected IBroker broker;
	
	protected ClientActorManagementObjectHelper clientHelper;

	/**
	 * Create a new instance
	 */
	public BrokerManagementObject() {
	}

	/**
	 * Create a new instance
	 * 
	 * @param broker
	 *            Broker
	 */
	public BrokerManagementObject(IBroker broker) {
		super(broker);
		clientHelper = new ClientActorManagementObjectHelper(broker);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerProtocols() {
		OrcaProxyProtocolDescriptor local = new OrcaProxyProtocolDescriptor(
				OrcaConstants.ProtocolLocal, LocalBroker.class.getName());
		OrcaProxyProtocolDescriptor soap = new OrcaProxyProtocolDescriptor(
				OrcaConstants.ProtocolSoap, SoapBroker.class.getName());
		proxies = new OrcaProxyProtocolDescriptor[] { local, soap };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setActor(IActor actor) {
		if (broker == null) {
			super.setActor(actor);
			this.broker = (IBroker) actor;
			clientHelper = new ClientActorManagementObjectHelper(broker);
		}
	}

	public ResultProxyMng getBrokers(AuthToken caller) {
		return clientHelper.getBrokers(caller);
	}
	
	public ResultProxyMng getBroker(ID brokerID, AuthToken caller) {
		return clientHelper.getBroker(brokerID, caller);
	}

	public ResultMng addBroker(ProxyMng broker, AuthToken caller) {
		return clientHelper.addBroker(broker, caller);
	}

	public ResultPoolInfoMng getPoolInfo(ID broker, AuthToken caller) {
		return clientHelper.getPoolInfo(broker, caller);
	}

	public ResultStringMng addReservation(final TicketReservationMng reservation, AuthToken caller) {
		return clientHelper.addReservation(reservation, caller);
	}

	public ResultStringsMng addReservations(final List<TicketReservationMng> reservations, AuthToken caller) {
		return clientHelper.addReservations(reservations, caller);
	}

	public ResultMng demandReservation(final ReservationID reservation, AuthToken caller) {
		return clientHelper.demandReservation(reservation, caller);
	}

	public ResultMng demandReservation(final ReservationMng reservation, AuthToken caller){
		return clientHelper.demandReservation(reservation, caller);
	}

	public ResultReservationMng claimResources(ID brokerID, SliceID sliceID, final ReservationID reservationID, AuthToken caller) {
		return clientHelper.claimResources(brokerID, sliceID, reservationID, caller);
	}
	
	public ResultReservationMng claimResources(ID brokerID,	final ReservationID reservationID, AuthToken caller) {
		return clientHelper.claimResources(brokerID, reservationID, caller);
	}
	
	public ResultMng extendReservation(final ReservationID reservation, 
			   final Date newEndTime, 
			   final int newUnits,
			   final ResourceType newResourceType,
			   final Properties requestProperties, 
			   final Properties configProperties, 
			   AuthToken caller) {
		return clientHelper.extendReservation(reservation, newEndTime, newUnits, newResourceType, requestProperties, configProperties, caller);
	}
}
