/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage;

import java.util.List;
import java.util.Properties;

import orca.manage.beans.UnitMng;
import orca.shirako.common.ReservationID;

public interface IOrcaServiceManager extends IOrcaActor, IOrcaClientActor {
	public List<UnitMng> getUnits(ReservationID reservationID) throws Exception;
	public boolean modifyReservation(ReservationID reservation, Properties modifyProperties);
}