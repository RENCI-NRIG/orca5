/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage.internal;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.beans.CertificateMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationStateMng;
import orca.manage.beans.ResultCertificateMng;
import orca.manage.beans.ResultEventMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultReservationStateMng;
import orca.manage.beans.ResultSliceMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.SliceMng;
import orca.manage.internal.api.IActorManagementObject;
import orca.manage.internal.local.LocalActor;
import orca.manage.proxies.soap.SoapActor;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorRunnable;
import orca.shirako.api.IDatabase;
import orca.shirako.api.IEvent;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaState;
import orca.shirako.kernel.ReservationFactory;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.util.AllActorEventsFilter;
import orca.util.CertificateUtils;
import orca.util.ID;

/**
 * Base class for actor manager objects. This class provides management
 * functions that are common among all actor types. Actor-specific functionality
 * should be placed in classes deriving from this class.
 */
public class ActorManagementObject extends ManagementObject implements IActorManagementObject {
	/**
	 * The actor represented by this wrapper.
	 */
	protected IActor actor;

	/**
	 * The actor database object.
	 */
	protected IDatabase db;

	/**
	 * Create a new default instance.
	 */
	public ActorManagementObject() {
	}

	/**
	 * Create a new instance representing the specified actor
	 * 
	 * @param actor The actor to be represented
	 */
	public ActorManagementObject(IActor actor) {
		setActor(actor);
	}

	@Override
	protected void registerProtocols() {
		OrcaProxyProtocolDescriptor local = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolLocal,
				LocalActor.class.getCanonicalName());
		OrcaProxyProtocolDescriptor soap = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolSoap,
				SoapActor.class.getCanonicalName());
		proxies = new OrcaProxyProtocolDescriptor[] { local, soap };
	}

	/**
	 * Performs recovery for this manager object
	 * 
	 * @throws Exception in case of error
	 */
	@Override
	protected void recover() throws Exception {
		String actorName = serial.getProperty(ManagementObject.PropertyActorName);

		if (actorName == null) {
			throw new RuntimeException("Missing actor name");
		}

		IActor actor = ActorRegistry.getActor(actorName);

		if (actor == null) {
			throw new RuntimeException("The managed actor does not exist");
		}

		setActor(actor);
	}

	/**
	 * Attach the actor to this wrapper. Called at construction time.
	 * 
	 * @param actor actor
	 */
	public void setActor(IActor actor) {
		if (this.actor == null) {
			this.actor = actor;
			this.db = actor.getShirakoPlugin().getDatabase();
			this.logger = actor.getLogger();
			this.id = actor.getGuid();
		}
	}

	/*
	 * =======================================================================
	 * Operations for slices
	 * =======================================================================
	 */

	/**
	 * Retrieves all slices
         * @param caller caller auth token
         * @return return all slices
	 */
	public ResultSliceMng getSlices(AuthToken caller) {
		ResultSliceMng result = new ResultSliceMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getSlices();
				} catch (Exception e) {
					logger.error("getSlices:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillSlice(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getSlices", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves the specified slice
	 * 
	 * @param sliceID sliceName
	 * @param caller auth token for caller 
	 * @return return slice
	 */
	public ResultSliceMng getSlice(SliceID sliceID, AuthToken caller) {
		ResultSliceMng result = new ResultSliceMng();
		result.setStatus(new ResultMng());

		if ((sliceID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getSlice(sliceID);
				} catch (Exception e) {
					logger.error("getSlice:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillSlice(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getSlice", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Creates an inventory slice. Generates a new slice identifier. The
	 * generated identifier will be included in the result object.
	 * 
	 * @param slice slice definition
	 * @param caller caller identity
	 * @return result object
	 */
	public ResultStringMng addSlice(SliceMng slice, AuthToken caller) {
		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((slice == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/* make the slice */
				final ISlice s = SliceFactory.getInstance().create(new SliceID(), slice.getName(),
						Converter.getResourceData(slice));
				s.setDescription(slice.getDescription());
				s.setOwner(actor.getIdentity());
				s.setInventory(true);

				// register the slice: must happen on the actor thread
				actor.executeOnActorThreadAndWait(new IActorRunnable() {
					public Object run() throws Exception {
						try {
							actor.registerSlice(s);
						} catch (Exception e) {
							actor.getShirakoPlugin().releaseSlice(s);
							throw e;
						}
						return null;
					}
				});

				result.setResult(s.getSliceID().toString());
			} catch (Exception e) {
				logger.error("addSlice", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Removes the specified slice.
	 * 
	 * @param sliceID slice identifier.
	 * @param caller caller identity
	 * @return result
	 */
	public ResultMng removeSlice(final SliceID sliceID, final AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((sliceID == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);

			return result;
		}

		try {
			// must happen on the actor main thread
			actor.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					actor.removeSlice(sliceID);
					return null;
				}
			});
		} catch (Exception e) {
			logger.error("removeSlice", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}

		return result;
	}

	/**
	 * Updates the specified slice
	 * 
	 * @param slice Slice description
	 * @param caller caller auth token 
	 * @return result
	 */
	public ResultMng updateSlice(final SliceMng slice, AuthToken caller) {
		final ResultMng result = new ResultMng();
		
		if (slice == null || slice.getSliceID() == null || caller == null){
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		
		try {
			// FIXME: access control
			actor.executeOnActorThreadAndWait(new IActorRunnable() {				
				public Object run() throws Exception {
					ISlice s = actor.getSlice(new SliceID(slice.getSliceID()));
					if (s == null){
						result.setCode(OrcaConstants.ErrorNoSuchSlice);
						return null;
					}
					ManagementUtils.updateSlice(s, slice);
					try {
			           actor.getShirakoPlugin().getDatabase().updateSlice(s);
				    } catch (Exception e) {
				       logger.error("Could not commit slice update", e);
				       result.setCode(OrcaConstants.ErrorDatabaseError);
				    }
					return null;
				}
			});
		}catch (Exception e) {
			logger.error("updateSlice", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}
		
		return result;
	}


	/**
	 * Retrieves all reservations
	 * 
	 * @param caller auth token
	 * @return result
	 */
	public ResultReservationMng getReservations(AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getReservations();
				} catch (Exception e) {
					logger.error("getReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, false);
				}
			} catch (Exception e) {
				logger.error("getReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all reservations
	 * 
	 * @param state state
	 * @param caller caller auth token
	 * @return reservations
	 */
	public ResultReservationMng getReservations(int state, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getReservations(state);
				} catch (Exception e) {
					logger.error("getReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, false);
				}
			} catch (Exception e) {
				logger.error("getReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all reservations in the specified slice
	 * 
	 * @param sliceID slice id
         * @param caller auth token for the caller 
	 * @return returns the reservation
	 */
	public ResultReservationMng getReservations(SliceID sliceID, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if ((sliceID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getReservations(sliceID);
				} catch (Exception e) {
					logger.error("getReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, false);
				}
			} catch (Exception e) {
				logger.error("getReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all reservations in the specified slice
	 * 
	 * @param sliceID Slice id 
	 * @param state slice state
	 * @param caller caller auth token 
	 * @return returns reservation for specified slice
	 */
	public ResultReservationMng getReservations(SliceID sliceID, int state, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (sliceID == null || caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					//v = db.getReservations(sliceID, "%ReservationState=" + state + "%");
					// now that rsv_state is its own column, use a different method /ib 07/2014
					v = db.getReservations(sliceID, state);
				} catch (Exception e) {
					logger.error("getReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, false);
				}
			} catch (Exception e) {
				logger.error("getReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;	}

	/**
	 * Retrieves the specified reservation
	 * 
	 * @param reservationID reservation identifier
	 * @param caller caller auth token 
	 * @return returns specified reservation
	 */
	public ResultReservationMng getReservation(ReservationID reservationID, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if ((reservationID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getReservation(reservationID);
				} catch (Exception e) {
					logger.error("getReservation:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, false);
				}
			} catch (Exception e) {
				logger.error("getReservation", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}


	
	/**
	 * Retrieves the specified reservation from the database
	 * 
	 * @param reservationID reservation identifier
	 * @return returns specific reservation
	 * @throws Exception in case of error
	 */
	protected IReservation getReservationFromDatabase(ReservationID reservationID) throws Exception {
		IReservation r = null;

		/* get the reservation from the database */
		Vector<Properties> v = db.getReservation(reservationID);

		if ((v != null) && (v.size() > 0)) {
			Properties p = v.get(0);
			r = ReservationFactory.createInstance(p);
		}

		return r;
	}

	/**
	 * Removes the specified reservation
	 * 
	 * @param reservationID reservation identifier
	 * @param caller caller auth token 
	 * @return result
	 */
	public ResultMng removeReservation(final ReservationID reservationID, final AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((reservationID == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				// must execute on the actor main thread
				actor.executeOnActorThreadAndWait(new IActorRunnable() {
					public Object run() throws Exception {
						actor.removeReservation(reservationID);
						return null;
					}
				});
			} catch (Exception e) {
				logger.error("removeReservation", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Closes the specified reservation
	 * 
	 * @param reservationID Reservation id
	 * @param caller auth token for the caller
	 * @return result
	 */
	public ResultMng closeReservation(final ReservationID reservationID, final AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((reservationID == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				actor.executeOnActorThreadAndWait(new IActorRunnable() {					
					public Object run() throws Exception {
						actor.close(reservationID);						
						return null;
					}
				});
			} catch (Exception e) {
				logger.error("closeReservation", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	public ResultMng closeSliceReservations(final SliceID sliceID, final AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((sliceID == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				actor.executeOnActorThreadAndWait(new IActorRunnable() {					
					public Object run() throws Exception {
						actor.closeSliceReservations(sliceID);						
						return null;
					}
				});
			} catch (Exception e) {
				logger.error("closeReservation", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}
	public ResultCertificateMng getCertificate() {
		ResultCertificateMng result = new ResultCertificateMng();
		result.setStatus(new ResultMng());

		try {
			// The KeyStoreManager is responsible for its own synchronization, so we 
			// do not need to execute this on the actor main thread.
			Certificate cert = actor.getShirakoPlugin().getKeyStore().getActorCertificate();
			result.getResult().add(Converter.fill(cert));
		} catch (Exception e) {
			logger.error("getCertificate", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}

		return result;
	}

	public ResultCertificateMng getCertificate(String alias, AuthToken auth) {
		ResultCertificateMng result = new ResultCertificateMng();
		result.setStatus(new ResultMng());

		try {
			if (alias == null) {
				result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
				return result;
			}
			// The KeyStoreManager is responsible for its own synchronization, so we 
			// do not need to execute this on the actor main thread.
			Certificate cert = actor.getShirakoPlugin().getKeyStore().getCertificate(alias);
			if (cert != null) {
				result.getResult().add(Converter.fill(cert));
			}
		} catch (Exception e) {
			logger.error("getCertificate", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}

		return result;
	}

	/**
	 * Returns the actor associated with this wrapper
	 * @return returns the actor
	 */
	public IActor getActor() {
		return actor;
	}

	/**
	 * Returns the class of the policy implementation
	 * 
	 * @return policy class
	 */
	public String getPolicyClass() {
		IPolicy policy = actor.getPolicy();

		if (policy != null) {
			return policy.getClass().getCanonicalName();
		}

		return null;
	}

	@Override
	public String getActorName() {
		if (actor != null) {
			return actor.getName();
		}

		return null;
	}

	public ResultMng registerCertificate(CertificateMng certificate, String alias, AuthToken caller) {
		ResultMng result = new ResultMng();
		if (caller == null || certificate == null || alias == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		if (!checkAccess(actor.getName(), caller)) {
			result.setCode(OrcaConstants.ErrorAccessDenied);
			return result;
		}

		// The KeyStoreManager is responsible for its own synchronization, so we 
		// do not need to execute this on the actor main thread.
		try {
			logger.debug("Registering certificate (" + alias + ")");
			Certificate c = CertificateUtils.decode(certificate.getContents());
			actor.getShirakoPlugin().getKeyStore().addTrustedCertificate(alias, c);
		} catch (CertificateException e) {
			logger.error("registerCertificate", e);
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			setExceptionDetails(result, e);
		} catch (Exception e) {
			logger.error("registerCertificate", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}
		return result;
	}

	public ResultMng unregisterCertificate(String alias, AuthToken caller) {
		ResultMng result = new ResultMng();
		if (caller == null || alias == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		if (!checkAccess(actor.getName(), caller)) {
			result.setCode(OrcaConstants.ErrorAccessDenied);
			return result;
		}

		// The KeyStoreManager is responsible for its own synchronization, so we 
		// do not need to execute this on the actor main thread.
		try {
			logger.debug("Unregistering certificate (" + alias + ")");
			//Certificate c = CertificateUtils.decode(certificate.getContents());
			actor.getShirakoPlugin().getKeyStore().removeTrustedCertificate(alias);
		} catch (Exception e) {
			logger.error("unregisterCertificate", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}
		return result;
	}

	public ResultStringMng createEventSubscription(AuthToken caller){
		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());
		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		if (!checkAccess(actor.getName(), caller)) {
			result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
			return result;
		}
		
		try {
			// create an actor subscription with the actor filter on it, so that
			// only events for that actor are 
			ID id = Globals.eventManager.createSubscription(caller, new AllActorEventsFilter(actor.getGuid()));
			result.setResult(id.toString());
		} catch (Exception e){
			logger.error("createEventSubscription", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}
	
	public ResultMng deleteEventSubscription(ID subscriptionID, AuthToken caller){
		ResultMng result = new ResultMng();
		if (caller == null || subscriptionID == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		if (!checkAccess(actor.getName(), caller)) {
			result.setCode(OrcaConstants.ErrorAccessDenied);
			return result;
		}
		
		try {
			Globals.eventManager.deleteSubscription(subscriptionID, caller);
		} catch (Exception e){
			logger.error("deleteEventSubscription", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}
		return result;
	}

	public ResultEventMng drainEvents(ID subscriptionID, int timeout, AuthToken caller) {
		ResultEventMng result = new ResultEventMng();
		result.setStatus(new ResultMng());
		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		if (!checkAccess(actor.getName(), caller)) {
			result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
			return result;
		}
		
		try {
			logger.debug("draining events");
			List<IEvent> events = Globals.eventManager.drainEvents(subscriptionID, caller, timeout);
			Converter.convert(events, result.getResult());			
		} catch (Exception e){
			logger.error("drainEvents", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);			
		}		
		return result;
	}
	
	public ResultMng updateReservation(final ReservationMng reservation, AuthToken caller) {
		final ResultMng result = new ResultMng();

		if ((reservation == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		
		try {
			// FIXME: check access
			actor.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					ReservationID rid = new ReservationID(reservation.getReservationID());
					IReservation r = actor.getReservation(rid);
					if (r == null){
						result.setCode(OrcaConstants.ErrorNoSuchReservation);
						return null;
					}
					
					ManagementUtils.updateReservation(r, reservation);
					r.setDirty();
					try {
			           actor.getShirakoPlugin().getDatabase().updateReservation(r);
				    } catch (Exception e) {
				       logger.error("Could not commit slice update", e);
				       result.setCode(OrcaConstants.ErrorDatabaseError);
				    }
					return null;
				}
			});			
		} catch (Exception e) {
			logger.error("updateReservation", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			ManagementObject.setExceptionDetails(result, e);
		}
		return result;
	}

	/**
	 * Retrieves the state of the specified reservation
	 * @param rid reservation id
	 * @param caller caller auth token
	 * @return reservation state
	 */
	public ResultReservationStateMng getReservationState(ReservationID rid, AuthToken caller) {
		ResultReservationStateMng result = new ResultReservationStateMng();
		result.setStatus(new ResultMng());

		if (rid == null || caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getReservation(rid);
				} catch (Exception e) {
					logger.error("getReservationState:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					return result;
				}
				
				Converter.fillReservationState(result.getResult(), v);
			} catch (Exception e) {
				logger.error("getReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves the state of the specified reservation
	 * 
	 * @param rids list of reservation id
	 * @param caller caller auth token
	 * @return reservation state
	 */
	public ResultReservationStateMng getReservationState(List<ReservationID> rids, AuthToken caller) {
		ResultReservationStateMng result = new ResultReservationStateMng();
		result.setStatus(new ResultMng());

		if (rids == null || rids.size() == 0 || caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		
		for (ReservationID rid : rids){
			if (rid == null){
				result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
				return result;				
			}
		}
		
		try {
			Vector<?> v = null;
			boolean go = true;

			try {
				v = db.getReservations(rids);
			} catch (Exception e) {
				logger.error("getReservationState:db access", e);
				result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
				setExceptionDetails(result.getStatus(), e);
				return result;
			}
			// if the sizes match then there is no missing reservation
			// and the database has returned them entries in the vector in the order we need them
			// so there is nothing to do
			if (rids.size() == v.size()){
				Converter.fillReservationState(result.getResult(), v);
			}else if (v.size() > rids.size()){
				throw new Exception("The database provided too many records");
			}else {
				int i = 0; int j = 0;
				while (i < rids.size()) {
					Properties p = (Properties)v.get(j);
					ReservationID rid = rids.get(i);
					ReservationID other = ReservationFactory.getReservationID(p);
					if (rid.equals(other)){
						result.getResult().add(Converter.fillReservationState(p));
						j++;
					}else {
						ReservationStateMng s = new ReservationStateMng();
						s.setState(OrcaConstants.ReservationStateUnknown);
						s.setPending(OrcaConstants.ReservationPendingStateUnknown);
						result.getResult().add(s);
					}
					i++;
				}
			}
		} catch (Exception e) {
			logger.error("getReservations", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}

		return result;
	}
}
