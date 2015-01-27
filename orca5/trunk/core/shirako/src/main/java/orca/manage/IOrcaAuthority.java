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

import orca.manage.beans.ReservationMng;
import orca.manage.beans.UnitMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.UnitID;

public interface IOrcaAuthority extends IOrcaServerActor {
	/**
	 * Retrieves all leases the site has issued to service managers. 
	 * @return
	 */
	public List<ReservationMng> getAuthorityReservations();
	/**
	 * Retrieves all units in the specified reservation
	 * @param reservationID
	 * @return
	 */
	public List<UnitMng> getUnits(ReservationID reservationID);
	/**
	 * Returns the inventory of the actor
	 * @return
	 */
	public List<UnitMng> getInventory();
	/**
	 * Returns the inventory of the actor in the specified slice.
	 * @param sliceId
	 * @return
	 */
	public List<UnitMng> getInventory(SliceID sliceId);
	/**
	 * Returns the specified inventory item
	 * @param unit
	 * @return
	 */
	public UnitMng getUnit(UnitID unit);
	
	/**
	 * Transfers inventory to the specified slice
	 * @param sliceId
	 * @param unit
	 * @return
	 */
	public boolean transferInventory(SliceID sliceId, UnitID unit);
	/**
	 * Transfers inventory back from the specified slice
	 * @param unit
	 * @return
	 */
	public boolean untransferInventory(UnitID unit);
	
	// FIXME: resource pools?
}