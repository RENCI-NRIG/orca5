/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.manage.internal;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultPoolInfoMng;
import orca.manage.beans.ResultProxyMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.ResultStringsMng;
import orca.manage.beans.ResultUnitMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.internal.local.LocalServiceManager;
import orca.manage.proxies.soap.SoapServiceManager;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorRunnable;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.substrate.ISubstrateDatabase;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;

public class ServiceManagerManagementObject extends ActorManagementObject implements IClientActorManagementObject {
	/**
	 * The service manager represented by this wrapper
	 */
	protected IServiceManager sm;
	/**
	 * Helper object implementing the IClientActorManagementObject functionality, so
	 * that it can be shared with the broker.
	 */
	protected ClientActorManagementObjectHelper clientHelper;
	
	/**
	 * Create a new instance
	 */
	public ServiceManagerManagementObject() {
	}

	/**
	 * Create a new instance
	 * 
	 * @param sm
	 *            Service manager
	 */
	public ServiceManagerManagementObject(IServiceManager sm) {
		super(sm);
		clientHelper = new ClientActorManagementObjectHelper(sm);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerProtocols() {
		OrcaProxyProtocolDescriptor local = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolLocal,
				LocalServiceManager.class.getName());
		OrcaProxyProtocolDescriptor soap = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolSoap,
				SoapServiceManager.class.getName());
		proxies = new OrcaProxyProtocolDescriptor[] { local, soap };
	}

	@Override
	public synchronized void setActor(IActor actor) {
		if (sm == null) {
			super.setActor(actor);
			sm = (IServiceManager) actor;
			clientHelper = new ClientActorManagementObjectHelper(sm);
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
	
	public ResultMng modifyReservation(final ReservationID reservation,
			   final Properties modifyProperties, 
			   AuthToken caller) {
		return clientHelper.modifyReservation(reservation, modifyProperties, caller);
	}

	public ResultUnitMng getReservationUnits(ReservationID reservationID, AuthToken caller) {
		ResultUnitMng result = new ResultUnitMng();
		result.setStatus(new ResultMng());

		if ((reservationID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<Properties> v = null;
				boolean go = true;

				try {
					v = getSubstrateDatabase().getUnits(reservationID);
				} catch (Exception e) {
					logger.error("getReservationUnits:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillUnits(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getReservationUnits", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	protected ISubstrateDatabase getSubstrateDatabase() {
		return (ISubstrateDatabase) actor.getShirakoPlugin().getDatabase();
	}
	

}
