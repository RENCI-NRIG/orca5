package net.exogeni.orca.manage.internal;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.beans.AuthTokenMng;
import net.exogeni.orca.manage.beans.CertificateMng;
import net.exogeni.orca.manage.beans.ClientMng;
import net.exogeni.orca.manage.beans.ResultCertificateMng;
import net.exogeni.orca.manage.beans.ResultClientMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultSliceMng;
import net.exogeni.orca.manage.beans.ResultStringMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IActorRunnable;
import net.exogeni.orca.shirako.api.IBrokerReservation;
import net.exogeni.orca.shirako.api.IServerActor;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.core.AuthorityPolicy;
import net.exogeni.orca.shirako.kernel.BrokerReservationFactory;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.kernel.SliceFactory;
import net.exogeni.orca.shirako.plugins.db.ClientDatabase;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.Client;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.CertificateUtils;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public class ServerActorManagementObject extends ActorManagementObject {
	protected IServerActor sa;

	public ServerActorManagementObject() {
	}

	public ServerActorManagementObject(IServerActor sa) {
		super(sa);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setActor(IActor actor) {
		if (sa == null) {
			super.setActor(actor);
			this.sa = (IServerActor) actor;
		}
	}

	/*
	 * =======================================================================
	 * Operations for reservations
	 * =======================================================================
	 */

	/**
	 * Retrieves all reservations for which this actor is the broker
	 * 
	 * @param caller caller auth token
	 * @return all reservation
	 */
	public ResultReservationMng getBrokerReservations(AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getBrokerReservations();
				} catch (Exception e) {
					logger.error("getBrokerReservations access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getBrokerReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultReservationMng getInventoryReservations(AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getHoldings();
				} catch (Exception e) {
					logger.error("getHoldings:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getHoldings", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultReservationMng getInventoryReservations(SliceID sliceID, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null || sliceID == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getHoldings(sliceID);
				} catch (Exception e) {
					logger.error("getHoldings:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getHoldings", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all inventory slices
	 * @param caller caller auth token
	 * @return returns slice
	 */
	public ResultSliceMng getInventorySlices(AuthToken caller) {
		ResultSliceMng result = new ResultSliceMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getInventorySlices();
				} catch (Exception e) {
					logger.error("getInventorySlices:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillSlice(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getInventorySlices", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all client slices
	 * @param caller caller auth token
	 * @return returns slice
	 */
	public ResultSliceMng getClientSlices(AuthToken caller) {
		ResultSliceMng result = new ResultSliceMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getClientSlices();
				} catch (Exception e) {
					logger.error("getClientSlices:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillSlice(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getClientSlices", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Create a client slice
	 * 
	 * @param slice
	 *            Slice Description
	 * @param caller caller auth token
	 * @return returns result 
	 */
	public ResultStringMng addClientSlice(SliceMng slice, AuthToken caller) {
		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((slice == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				// get the owner auth token
				AuthTokenMng mngOwner = slice.getOwner();
				AuthToken owner = null;

				boolean ownerIsOK = false;

				if (mngOwner != null) {
					owner = Converter.fill(mngOwner);
					if (owner != null) {
						if (owner.getName() != null && owner.getGuid() != null) {
							ownerIsOK = true;

							// FIXME: check that this token represents an actor,
							// known to this actor
						}
					}
				}

				if (!ownerIsOK) {
					result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
				} else {
					/* make the slice */
					final ISlice s = SliceFactory.getInstance().create(new SliceID(),
							slice.getName(), Converter.getResourceData(slice));
					s.setDescription(slice.getDescription());
					/* mark as client slice */
					s.setInventory(false);

					/* set the owner */
					assert (owner != null);
					s.setOwner(owner);

					actor.executeOnActorThreadAndWait(new IActorRunnable() {
						public Object run() throws Exception {
							try {
								/*
								 * register the slice (adds the slice to the
								 * database)
								 */
								((IServerActor) actor).registerClientSlice(s);
							} catch (Exception e) {
								actor.getShirakoPlugin().releaseSlice(s);
								throw e;
							}
							return null;
						}
					});

					result.setResult(s.getSliceID().toString());
				}
			} catch (Exception e) {
				logger.error("addClientSlice", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Registers a new client with the actor.
	 * 
	 * @param client
	 *            client
	 * @param certificate certificate
	 * @param caller caller auth token
	 * @return result
	 */
	public ResultMng registerClient(ClientMng client, CertificateMng certificate, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((client == null) || (certificate == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				final Client c = Converter.fill(client);
				final Certificate cert = CertificateUtils.decode(certificate.getContents());
				actor.executeOnActorThreadAndWait(new IActorRunnable() {
					public Object run() throws Exception {
						((IServerActor) actor).registerClient(c, cert);
						return null;
					}
				});
			} catch (Exception e) {
				logger.error("registerClient", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	public ResultClientMng getClients(AuthToken caller) {
		ResultClientMng result = new ResultClientMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<Properties> v = ((ClientDatabase) actor.getShirakoPlugin().getDatabase())
						.getClients();
				Converter.fillClient(result.getResult(), v);
			} catch (Exception e) {
				logger.error("getClients", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultClientMng getClient(ID guid, AuthToken caller) throws Exception {
		ResultClientMng result = new ResultClientMng();
		result.setStatus(new ResultMng());

		if ((guid == null) || caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<Properties> v = ((ClientDatabase) actor.getShirakoPlugin().getDatabase())
						.getClient(guid);
				Converter.fillClient(result.getResult(), v);
			} catch (Exception e) {
				logger.error("getClient", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultMng unregisterClient(final ID guid, final AuthToken caller) throws Exception {
		ResultMng result = new ResultMng();

		if ((guid == null) || caller == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				actor.executeOnActorThreadAndWait(new IActorRunnable() {
					public Object run() throws Exception {
						((IServerActor) actor).unregisterClient(guid);
						return null;
					}
				});
			} catch (Exception e) {
				logger.error("getClient", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	public ResultCertificateMng getClientCertificate(ID guid, AuthToken caller) throws Exception {
		ResultCertificateMng result = new ResultCertificateMng();
		result.setStatus(new ResultMng());

		if ((guid == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Certificate cert = actor.getShirakoPlugin().getKeyStore()
						.getCertificate(guid.toString());
				result.getResult().add(Converter.fill(cert));
			} catch (Exception e) {
				logger.error("getClientCertificate", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all client reservations
	 * 
	 * @param caller caller
	 * @return all reservations
	 */
	public ResultReservationMng getClientReservations(AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getClientReservations();
				} catch (Exception e) {
					logger.error("getReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultReservationMng getClientReservations(SliceID slice, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null || slice == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getClientReservations(slice);
				} catch (Exception e) {
					logger.error("getReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, true);
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
	 * export the resources for a slice 
	 * @param clientSliceID client Slice Id
	 * @param poolID pool slice id
	 * @param start start date
	 * @param end end date
	 * @param units units
	 * @param ticketProperties ticket properties  
	 * @param resourceProperties resource properties
	 * @param sourceTicketID source ticket id
	 * @param caller auth token for the caller
	 * @return result string
	 */
	public ResultStringMng exportResources(SliceID clientSliceID, SliceID poolID, Date start,
			Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken caller) {

		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((clientSliceID == null) || (poolID == null) || (start == null) || (end == null)
				|| (units < 1) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		try {
			final ISlice clientSlice = sa.getSlice(clientSliceID);
			if (clientSlice == null) {
				result.getStatus().setCode(OrcaConstants.ErrorNoSuchSlice);
				return result;
			}

			ISlice pool = sa.getSlice(poolID);
			if (pool == null) {
				result.getStatus().setCode(OrcaConstants.ErrorNoSuchResourcePool);
				return result;
			}

			Term term = new Term(start, end);
			ResourceData rdata = new ResourceData();
			if (sourceTicketID != null) {
				rdata.getRequestProperties().setProperty(AuthorityPolicy.PropertySourceTicket,
						sourceTicketID.toString());
			}

			ResourceSet rset = new ResourceSet(units, pool.getResourceType(), rdata);
			final IBrokerReservation r = BrokerReservationFactory.getInstance().create(rset, term,
					clientSlice);
			r.setOwner(clientSlice.getOwner());
			// TODO: use ticketProperties and resourceProperties

			// do the actual export
			actor.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					sa.export(r, clientSlice.getOwner());
					return null;
				}
			});
			result.setResult(r.getReservationID().toString());
		} catch (Exception e) {
			logger.error("exportResources", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}

	public ResultStringMng exportResources(SliceID poolID, Date start, Date end, int units,
			Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, final AuthToken client, AuthToken caller) {

		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((client == null) || (poolID == null) || (start == null) || (end == null) || (units < 1)
				|| (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		try {
			ISlice pool = sa.getSlice(poolID);
			if (pool == null) {
				result.getStatus().setCode(OrcaConstants.ErrorNoSuchResourcePool);
				return result;
			}

			final Term term = new Term(start, end);
			ResourceData rdata = new ResourceData();
			if (sourceTicketID != null) {
				rdata.getRequestProperties().setProperty(AuthorityPolicy.PropertySourceTicket,
						sourceTicketID.toString());
			}

			final ResourceSet rset = new ResourceSet(units, pool.getResourceType(), rdata);
			// TODO: use ticketProperties and resourceProperties

			// do the actual export
			ReservationID exported = (ReservationID) actor
					.executeOnActorThreadAndWait(new IActorRunnable() {
						public Object run() throws Exception {
							return sa.export(rset, term, client);
						}
					});
			result.setResult(exported.toString());
		} catch (Exception e) {
			logger.error("exportResources", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}

	/**
	 * @param clientSliceID client slice ID
	 * @param resourceType resource type
	 * @param start start date
	 * @param end end date
	 * @param units units
	 * @param ticketProperties ticketProperties
	 * @param resourceProperties resourceProperties
	 * @param sourceTicketID sourceTicketID
	 * @param caller caller
	 * @return result
	 */
	public ResultStringMng exportResources(SliceID clientSliceID, ResourceType resourceType,
			Date start, Date end, int units, Properties ticketProperties,
			Properties resourceProperties, ReservationID sourceTicketID, AuthToken caller) {

		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((clientSliceID == null) || (resourceType == null) || (start == null) || (end == null)
				|| (units < 1) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		try {
			final ISlice clientSlice = sa.getSlice(clientSliceID);
			if (clientSlice == null) {
				result.getStatus().setCode(OrcaConstants.ErrorNoSuchSlice);
				return result;
			}
			
			Term term = new Term(start, end);
			ResourceData rdata = new ResourceData();
			if (sourceTicketID != null) {
				rdata.getRequestProperties().setProperty(AuthorityPolicy.PropertySourceTicket,
						sourceTicketID.toString());
			}

			ResourceSet rset = new ResourceSet(units, resourceType, rdata);
			final IBrokerReservation r = BrokerReservationFactory.getInstance().create(rset, term, clientSlice);
			r.setOwner(clientSlice.getOwner());
			// TODO: use ticketProperties and resourceProperties

			// do the actual export
			actor.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					sa.export(r, clientSlice.getOwner());
					return null;
				}
			});
			result.setResult(r.getReservationID().toString());
		} catch (Exception e) {
			logger.error("exportResources", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}

	/**
	 * @param resourceType resource type
	 * @param start start date
	 * @param end end date
	 * @param units units
	 * @param ticketProperties ticketProperties
	 * @param resourceProperties resourceProperties
	 * @param sourceTicketID sourceTicketID
	 * @param client client 
	 * @param caller caller
	 * @return result
	 */
	public ResultStringMng exportResources(ResourceType resourceType, Date start, Date end,
			int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, final AuthToken client, AuthToken caller) {

		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((resourceType == null) || (start == null) || (end == null) || (units < 1)
				|| (client == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		
		try {
			final Term term = new Term(start, end);
			ResourceData rdata = new ResourceData();
			if (sourceTicketID != null) {
				rdata.getRequestProperties().setProperty(AuthorityPolicy.PropertySourceTicket,
						sourceTicketID.toString());
			}

			// TODO: use ticketProperties and resourceProperties
			final ResourceSet rset = new ResourceSet(units, resourceType, rdata);
			if (logger.isDebugEnabled()){
				logger.debug("Executing export on actor " + actor.getName() + " " + sa.getName() + "(" + sa.getClass().getSimpleName() + ") " + rset.getReservationID());
			}
			// do the actual export
			ReservationID exported = (ReservationID) actor
					.executeOnActorThreadAndWait(new IActorRunnable() {
						public Object run() throws Exception {
							return sa.export(rset, term, client);
						}
					});

			result.setResult(exported.toString());
		} catch (Exception e) {
			logger.error("exportResources", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}
}
