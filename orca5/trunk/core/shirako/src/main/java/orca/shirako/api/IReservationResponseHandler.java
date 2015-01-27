package orca.shirako.api;

import orca.shirako.util.RPCException;

public interface IReservationResponseHandler extends IRPCResponseHandler {
	public void handle(RPCException status, IReservation reservation);
}