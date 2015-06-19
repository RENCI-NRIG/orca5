package orca.manage.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.LeaseReservationMng;
import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationPredecessorMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultPoolInfoMng;
import orca.manage.beans.ResultProxyMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.ResultStringsMng;
import orca.manage.beans.TicketReservationMng;
import orca.security.AuthToken;
import orca.shirako.api.IActorRunnable;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IClientActor;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.shirako.container.Globals;
import orca.shirako.core.BrokerPolicy;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.proxies.Proxy;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;

import org.apache.log4j.Logger;

public class ClientActorManagementObjectHelper implements IClientActorManagementObject {
	protected IClientActor client;
	protected Logger logger;

	public ClientActorManagementObjectHelper(IClientActor client) {
		this.client = client;
		this.logger = Globals.getLogger(this.getClass().getCanonicalName());
	}

	public ResultProxyMng getBrokers(AuthToken caller) {

		ResultProxyMng result = new ResultProxyMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				IBrokerProxy[] brokers = client.getBrokers();
				Converter.fillProxy(result.getResult(), brokers);
			} catch (Exception e) {
				logger.error("getBrokers", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result.getStatus(), e);
			}
		}
		return result;
	}

	public ResultProxyMng getBroker(ID brokerID, AuthToken caller) {
		ResultProxyMng result = new ResultProxyMng();
		result.setStatus(new ResultMng());

		if (caller == null || brokerID == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				IBrokerProxy b = client.getBroker(brokerID);
				if (b == null) {
					result.getStatus().setCode(OrcaConstants.ErrorNoSuchBroker);
				} else {
					result.getResult().add(Converter.fill((Proxy) b));
				}
			} catch (Exception e) {
				logger.error("getBroker", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultMng addBroker(ProxyMng broker, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((broker == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				IBrokerProxy proxy = Converter.getAgentProxy(broker);

				if (proxy == null) {
					result.setCode(OrcaConstants.ErrorInvalidArguments);
				} else {
					client.addBroker(proxy);
				}
			} catch (Exception e) {
				logger.error("addBroker", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result, e);
			}
		}

		return result;
	}

	public ResultPoolInfoMng getPoolInfo(ID broker, AuthToken caller) {
		ResultPoolInfoMng result = new ResultPoolInfoMng();
		result.setStatus(new ResultMng());

		if ((broker == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				IBrokerProxy b = client.getBroker(broker);
				if (b != null) {
					Properties request = BrokerPolicy.getResourcePoolsQuery();
					Properties response = ManagementUtils.query(client, b, request);
					ResourcePoolsDescriptor rdp = BrokerPolicy.getResourcePools(response);
					for (ResourcePoolDescriptor rd : rdp) {
						Properties temp = new Properties();
						rd.save(temp, null);
						PoolInfoMng pi = new PoolInfoMng();
						pi.setType(rd.getResourceType().toString());
						pi.setName(rd.getResourceTypeLabel());
						pi.setProperties(Converter.fill(temp));
						result.getResult().add(pi);
					}
				} else {
					result.getStatus().setCode(OrcaConstants.ErrorNoSuchBroker);
				}
			} catch (Exception e) {
				logger.error("getPoolInfo", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result.getStatus(), e);
			}
		}
		return result;
	}

	protected ReservationID addReservation(TicketReservationMng reservation, ResultMng result)
			throws Exception {
		// check access
		SliceID slice = new SliceID(reservation.getSliceID());
		ResourceSet rset = Converter.getResourceSet(reservation);

		Term term = new Term(new Date(reservation.getStart()), new Date(reservation.getEnd()));
		ID broker;
		if (reservation.getBroker() != null) {
			broker = new ID(reservation.getBroker());
		} else {
			broker = null;
		}

		final IServiceManagerReservation rc = ServiceManagerReservationFactory.getInstance()
				.create(rset, term);
		rc.setRenewable(reservation.isRenewable());
		if (rc.getState() != ReservationStates.None
				|| rc.getPendingState() != ReservationStates.None) {
			result.setCode(OrcaConstants.ErrorInvalidReservation);
			result.setMessage("Only reservations in Nascent.None can be added");
			return null;
		}

		ISlice s = client.getSlice(slice);
		if (s == null) {
			result.setCode(OrcaConstants.ErrorNoSuchSlice);
			return null;
		}
		rc.setSlice(s);

		IBrokerProxy proxy = null;
		if (broker == null) {
			proxy = client.getDefaultBroker();
		} else {
			proxy = client.getBroker(broker);
		}
		if (proxy == null) {
			result.setCode(OrcaConstants.ErrorNoSuchBroker);
			return null;
		}
		rc.setBroker(proxy);
		client.register(rc);
		return rc.getReservationID();
	}

	public ResultStringMng addReservation(final TicketReservationMng reservation, AuthToken caller) {
		final ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());

		if ((reservation == null) || (reservation.getSliceID() == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				ReservationID rid = (ReservationID) client
						.executeOnActorThreadAndWait(new IActorRunnable() {
							public Object run() throws Exception {
								return addReservation(reservation, result.getStatus());
							}
						});

				if (rid != null) {
					result.setResult(rid.toString());
				}
			} catch (Exception e) {
				logger.error("addReservation", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result.getStatus(), e);
			}
		}
		return result;
	}

	public ResultStringsMng addReservations(final List<TicketReservationMng> reservations,
			AuthToken caller) {
		final ResultStringsMng result = new ResultStringsMng();
		result.setStatus(new ResultMng());

		if ((reservations == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		for (ReservationMng r : reservations) {
			if (r.getSliceID() == null) {
				result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
				return result;
			}
		}

		try {
			// check access
			List<ReservationID> rr = (List<ReservationID>) client
					.executeOnActorThreadAndWait(new IActorRunnable() {
						public Object run() throws Exception {
							List<ReservationID> rids = new ArrayList<ReservationID>(reservations
									.size());
							try {
								for (TicketReservationMng reservation : reservations) {
									ReservationID id = addReservation(reservation,
											result.getStatus());
									if (id != null) {
										rids.add(id);
									} else {
										throw new Exception("Could not add reservation");
									}
								}
							} catch (Exception e) {
								for (ReservationID rid : rids) {
									client.unregister(rid);
								}
								rids.clear();
							}
							return rids;
						}
					});
			if (result.getStatus().getCode() == 0) {
				for (ReservationID rid : rr) {
					result.getResult().add(rid.toString());
				}
			}
		} catch (Exception e) {
			logger.error("addReservation", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			ManagementObject.setExceptionDetails(result.getStatus(), e);
		}

		return result;
	}

	public ResultMng demandReservation(final ReservationID reservation, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((reservation == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		try {
			// FIXME: check access
			client.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					client.demand(reservation);
					return null;
				}
			});
		} catch (Exception e) {
			logger.error("demandReservation", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			ManagementObject.setExceptionDetails(result, e);
		}
		return result;
	}

	public ResultMng demandReservation(final ReservationMng reservation, AuthToken caller) {
		final ResultMng result = new ResultMng();

		if ((reservation == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}

		try {
			// FIXME: check access
			client.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					ReservationID rid = new ReservationID(reservation.getReservationID());
					IReservation r = client.getReservation(rid);
					if (r == null) {
						result.setCode(OrcaConstants.ErrorNoSuchReservation);
						return null;
					}
							
					ManagementUtils.updateReservation(r, reservation);
					// process redeem predecessors
					if (reservation instanceof LeaseReservationMng) {
						LeaseReservationMng lease = (LeaseReservationMng)reservation;
						List<ReservationPredecessorMng> predecessors = lease.getRedeemPredecessors();
						for (ReservationPredecessorMng pred : predecessors) {
							// need at leat reservation id
							if (pred.getReservationID() == null){
								logger.warn("Redeem predecessor specified for rid=" + rid.toHashString() + " but missing reservation id of predecessor");
								continue;
							}
							// the predecessor should have been added
							ReservationID predid = new ReservationID(pred.getReservationID());
							IReservation pr = client.getReservation(predid);
							if (pr == null){
								logger.warn("Redeem predecessor for rid=" + rid.toHashString() + " with rid=" + predid + " does not exist. Ignoring it");
								continue;
							}
							if (!(pr instanceof IServiceManagerReservation)){
								logger.warn("Redeem predecessor with rid=" + predid.toHashString() + " is not an IServiceManagerReservation: class=" + pr.getClass().getName());
								continue;
							}
							
							// extract the filter
							// note: there is an important difference between no filter and empty filter
							//   - no filter means pass all properties, empty filter means do not pass any.
							if (pred.getFilter() != null){
								Properties filter = OrcaConverter.fill(pred.getFilter());	
								if (logger.isDebugEnabled()){
									logger.debug(
										"Setting redeem predecessor on reservation #" + r.getReservationID().toHashString() + 
										": pred=" + pr.getReservationID().toHashString() + " filter=" + filter);
								}
								((IServiceManagerReservation)r).addRedeemPredecessor((IServiceManagerReservation)pr, filter);
							} else {
								if (logger.isDebugEnabled()){
									logger.debug(
										"Setting redeem predecessor on reservation #" + r.getReservationID().toHashString() + 
										": pred=" + pr.getReservationID().toHashString() + " filter=none");
								}
								((IServiceManagerReservation)r).addRedeemPredecessor((IServiceManagerReservation)pr);								
							}
						}
					}
					
					try {
						client.getShirakoPlugin().getDatabase().updateReservation(r);
					} catch (Exception e) {
						logger.error("Could not commit slice update", e);
						result.setCode(OrcaConstants.ErrorDatabaseError);
					}
					client.demand(rid);
					return null;
				}
			});
		} catch (Exception e) {
			logger.error("demandReservation", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			ManagementObject.setExceptionDetails(result, e);
		}
		return result;
	}

	/**
	 * Claim previously exported resources
	 * 
	 * @param brokerName
	 *            Broker to claim the resources from
	 * @param sliceName
	 *            Slice to store the claimed resources in (must be the same as
	 *            the remote slice in which the resources have been exported to)
	 * @param reservationId
	 *            Reservation identifier of the reservation representing the
	 *            exported resources
	 * @return
	 */
	public ResultReservationMng claimResources(ID brokerID, SliceID sliceID,
			final ReservationID reservationID, AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if ((caller == null) || (brokerID == null) || (sliceID == null) || (reservationID == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * make up a resource type. the system will replace it with the
				 * correct one
				 */
				ResourceType rtype = new ResourceType(new ID().toString());
				ResourceData rdata = new ResourceData();
				final ResourceSet rset = new ResourceSet(0, rtype, rdata);

				final IBrokerProxy mybroker = client.getBroker(brokerID);

				if (mybroker == null) {
					result.getStatus().setCode(OrcaConstants.ErrorNoSuchBroker);

					return result;
				}

				final ISlice slice = client.getSlice(sliceID);

				if (slice == null) {
					result.getStatus().setCode(OrcaConstants.ErrorNoSuchSlice);

					return result;
				}

				if (!slice.isInventory()) {
					result.getStatus().setCode(OrcaConstants.ErrorInvalidSlice);

					return result;
				}

				IClientReservation rc = (IClientReservation) client
						.executeOnActorThreadAndWait(new IActorRunnable() {
							public Object run() throws Exception {
								return client.claim(reservationID, rset, slice, mybroker);
							}
						});

				if (rc != null) {
					ReservationMng reservation = Converter.fill(rc, true);
					result.getResult().add(reservation);
				} else {
					throw new Exception("Internal Error");
				}
			} catch (Exception e) {
				logger.error("claimResources", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultReservationMng claimResources(ID brokerID, final ReservationID reservationID,
			AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if ((caller == null) || (brokerID == null) || reservationID == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * make up a resource type. the system will replace it with the
				 * correct one
				 */
				ResourceType rtype = new ResourceType(new ID().toString());
				ResourceData rdata = new ResourceData();
				final ResourceSet rset = new ResourceSet(0, rtype, rdata);

				final IBrokerProxy mybroker = client.getBroker(brokerID);

				if (mybroker == null) {
					result.getStatus().setCode(OrcaConstants.ErrorNoSuchBroker);
					return result;
				}

				IClientReservation rc = (IClientReservation) client
						.executeOnActorThreadAndWait(new IActorRunnable() {
							public Object run() throws Exception {
								return client.claim(reservationID, rset, mybroker);
							}
						});

				if (rc != null) {
					ReservationMng reservation = Converter.fill(rc, true);
					result.getResult().add(reservation);
				} else {
					throw new Exception("Internal Error");
				}
			} catch (Exception e) {
				logger.error("claimResources", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				ManagementObject.setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultMng extendReservation(final ReservationID reservation, final Date newEndTime,
			final int newUnits, final ResourceType newResourceType,
			final Properties requestProperties, final Properties configProperties, AuthToken caller) {
		final ResultMng result = new ResultMng();
		if (reservation == null || newEndTime == null || caller == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		try {
			client.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					IReservation r = client.getReservation(reservation);
					if (r == null) {
						result.setCode(OrcaConstants.ErrorNoSuchReservation);
						return null;
					}
					PropList.mergePropertiesPriority(r.getResources().getConfigurationProperties(),
							configProperties);
					PropList.mergePropertiesPriority(r.getResources().getRequestProperties(),
							requestProperties);
					ResourceSet rset = new ResourceSet();
					if (newUnits == OrcaConstants.ExtendSameUnits) {
						rset.setUnits(r.getResources().getUnits());
					} else {
						rset.setUnits(newUnits);
					}
					if (newResourceType == null) {
						rset.setType(r.getResources().getType());
					}
					
					//configProperties.setProperty("shirako.modify.key1", "value1");
					
					rset.setConfigurationProperties(configProperties);
					rset.setRequestProperties(requestProperties);
					// FIXME: not setting local/resource properties, but should
					// we?
					
					Date tmpStartTime = r.getTerm().getStartTime();
                    Term newTerm = r.getTerm().extend();

                    //newTerm.setNewStartTime(r.getTerm().getStartTime());
                    newTerm.setEndTime(newEndTime);
                    newTerm.setNewStartTime(tmpStartTime);
                    newTerm.setStartTime(tmpStartTime);
					
					/*
					Properties modifyProps = new Properties();
					modifyProps.setProperty("new-modify-property1", "value1");
					System.out.println("Before calling client.modify()");
					client.modify(reservation, modifyProps);
					System.out.println("After calling client.modify()");
					*/
					
					
					client.extend(reservation, rset, newTerm);
					
					return null;
				}
			});
		} catch (Exception e) {
			logger.error("extendReservation", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			ManagementObject.setExceptionDetails(result, e);
		}

		return result;
	}
	
	public ResultMng modifyReservation(final ReservationID reservation, final Properties modifyProperties, AuthToken caller) {
		final ResultMng result = new ResultMng();
		if (reservation == null || modifyProperties == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
			return result;
		}
		logger.debug("ClientActorManagementObjectHelper: modifyReservation(): reservation:" + reservation + " | modifyProperties = " + modifyProperties);
		try {
			client.executeOnActorThreadAndWait(new IActorRunnable() {
				public Object run() throws Exception {
					IReservation r = client.getReservation(reservation);
					if (r == null) {
						result.setCode(OrcaConstants.ErrorNoSuchReservation);
						return null;
					}
					
					// Shipping the modifyProperties to the core and have ServiceManager.modify()
					// handle the properties
					
					client.modify(reservation, modifyProperties);
					
					return null;
				}
			});			
		} catch (Exception e) {
			logger.error("modifyReservation", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			ManagementObject.setExceptionDetails(result, e);
		}

		return result;
	}
	
	

}
