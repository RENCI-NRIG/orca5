/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.manage.internal;

import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaProxyProtocolDescriptor;
import net.exogeni.orca.manage.beans.ConfigMappingMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultUnitMng;
import net.exogeni.orca.manage.internal.local.LocalAuthority;
import net.exogeni.orca.manage.proxies.soap.SoapAuthority;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.common.UnitID;
import net.exogeni.orca.shirako.core.PropertiesManager;
import net.exogeni.orca.shirako.plugins.config.ConfigurationMapping;
import net.exogeni.orca.shirako.plugins.substrate.ISubstrateDatabase;

public class AuthorityManagementObject extends ServerActorManagementObject {
	/**
	 * The authority represented by this wrapper
	 */
	protected IAuthority authority;

	/**
	 * Create a new instance
	 */
	public AuthorityManagementObject() {
	}

	/**
	 * Create a new instance
	 * 
	 * @param authority Authority to represent
	 */
	public AuthorityManagementObject(IAuthority authority) {
		super(authority);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerProtocols() {
		OrcaProxyProtocolDescriptor local = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolLocal,
				LocalAuthority.class.getName());
		OrcaProxyProtocolDescriptor soap = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolSoap,
				SoapAuthority.class.getName());
		proxies = new OrcaProxyProtocolDescriptor[] { local, soap };
	}

	@Override
	public void setActor(IActor actor) {
		if (authority == null) {
			super.setActor(actor);
			this.authority = (IAuthority) actor;
		}
	}

	/*
	 * =======================================================================
	 * Operations for reservations
	 * =======================================================================
	 */

	/**
	 * Retrieves all reservations for which this actor is the authority
	 * 
	 * @param caller caller auth token
	 * @return all reservations
	 */
	public ResultReservationMng getAuthorityReservations(AuthToken caller) {
		ResultReservationMng result = new ResultReservationMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<?> v = null;
				boolean go = true;

				try {
					v = db.getAuthorityReservations();
				} catch (Exception e) {
					logger.error("getAuthorityReservations:db access", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillReservation(result.getResult(), v, true);
				}
			} catch (Exception e) {
				logger.error("getAuthorityReservations", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}


	/*
	 * =======================================================================
	 * Operations for configuration mapping files
	 * =======================================================================
	 */

	// / XXX: need to be revised
	public ConfigMappingMng[] getConfigMappings() {
		try {
			Vector v = db.getConfigurationMappings();

			return Converter.fillConfigMapping(v);
		} catch (Exception e) {
			return null;
		}
	}

	public ConfigMappingMng getConfigMapping(String key) {
		try {
			Vector v = db.getConfigurationMapping(key);

			if ((v != null) && (v.size() > 0)) {
				return Converter.fillConfigMapping((Properties) v.get(0));
			}

			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public int addConfigMapping(String key, ConfigMappingMng map) {
		try {
			ConfigurationMapping m = Converter.fill(map);
			db.addConfigurationMapping(key, m);

			return 0;
		} catch (Exception e) {
			return -1;
		}
	}

	public int removeConfigMapping(String key) {
		try {
			db.removeConfigurationMapping(key);

			return 0;
		} catch (Exception e) {
			return -1;
		}
	}

	public int updateConfigMapping(String key, ConfigMappingMng map) {
		try {
			ConfigurationMapping m = Converter.fill(map);
			db.updateConfigurationMapping(key, m);

			return 0;
		} catch (Exception e) {
			return -1;
		}
	}

	public ResultUnitMng getInventory(AuthToken caller) {
		ResultUnitMng result = new ResultUnitMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<Properties> v = null;
				boolean go = true;

				try {
					v = getSubstrateDatabase().getInventory();
				} catch (Exception e) {
					logger.error("getInventory:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillUnits(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getInventory", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultUnitMng getInventory(SliceID sliceId, AuthToken caller) {
		ResultUnitMng result = new ResultUnitMng();
		result.setStatus(new ResultMng());

		if (caller == null || sliceId == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<Properties> v = null;
				boolean go = true;

				try {
					v = getSubstrateDatabase().getInventory(sliceId);
				} catch (Exception e) {
					logger.error("getInventory:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillUnits(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getInventory", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultMng transferInventory(SliceID sliceId, UnitID unit, AuthToken caller) {
		ResultMng result = new ResultMng();

		if (caller == null || sliceId == null || unit == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				getSubstrateDatabase().transfer(unit, sliceId);
			} catch (Exception e) {
				logger.error("transferInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	public ResultMng untransferInventory(UnitID unit, AuthToken caller) {
		ResultMng result = new ResultMng();

		if (caller == null || unit == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				getSubstrateDatabase().untransfer(unit);
			} catch (Exception e) {
				logger.error("transferInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	protected ISubstrateDatabase getSubstrateDatabase() {
		return (ISubstrateDatabase) actor.getShirakoPlugin().getDatabase();
	}

	/**
	 * Returns all units that belong to this reservation.
	 * 
	 * @param reservationID Reservation id
	 * @param caller AuthToken of the caller
         * @return return the result unit
	 */
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

	
	/**
	 * Returns all units that belong to this reservation.
	 * 
	 * @param unitID unit id
	 * @param caller AuthToken of the caller
         * @return return the result unit
	 */
	public ResultUnitMng getUnit(UnitID unitID, AuthToken caller) {
		ResultUnitMng result = new ResultUnitMng();
		result.setStatus(new ResultMng());

		if ((unitID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				Vector<Properties> v = null;
				boolean go = true;

				try {
					v = getSubstrateDatabase().getUnit(unitID);
				} catch (Exception e) {
					logger.error("getUnit", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillUnits(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getUnit", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}
}
