package orca.controllers.xmlrpc.statuswatch;

import java.util.List;

import orca.shirako.common.ReservationID;

/**
 * Callbacks on reservation status updates must comply with this interface
 * @author ibaldin
 *
 */
public interface IStatusUpdateCallback<ID> {
	public static class StatusCallbackException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
	}
	
	/**
	 * All reservations in indicated group have gone to Active or have OK modify status
	 * @param cause - reservations that transitioned to Active or OK modify status
	 * @param actOn - reservations that need to be acted on
	 * @throws StatusCallbackException
	 */
	public void success(List<ID> ok, List<ReservationID> actOn) throws StatusCallbackException;
	
	
	/**
	 * Some reservations may have gone into Failed or not OK modify status, so provide an action for those
	 * @param failed - those reservations that failed or went to not OK 
	 * @param ok - reservations in the same group that went Active or OK
	 * @param actOn - reservations to be acted on
	 * @throws StatusCallbackException
	 */
	public void failure(List<ID> failed, List<ID> ok, List<ReservationID> actOn) throws StatusCallbackException;
}
