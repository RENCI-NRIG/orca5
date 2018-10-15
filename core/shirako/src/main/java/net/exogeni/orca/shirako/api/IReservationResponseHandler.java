package net.exogeni.orca.shirako.api;

import net.exogeni.orca.shirako.util.RPCException;

public interface IReservationResponseHandler extends IRPCResponseHandler {
	public void handle(RPCException status, IReservation reservation);
}
