package orca.controllers.xmlrpc;

import java.util.List;

import orca.controllers.xmlrpc.statuswatch.IStatusUpdateCallback;
import orca.shirako.common.ReservationID;

public class ReservationDependencyStatusUpdate implements IStatusUpdateCallback<ReservationID> {

	@Override
	public void success(List<ReservationID> ok, List<ReservationID> actOn)
			throws StatusCallbackException {
		
		
		
		System.out.println("SUCCESS ON MODIFY WATCH OF " + ok);
		//ok-parents
		//actOn-to be modified reservation
		for(ReservationID r_id:ok){
			
		}

	}

	@Override
	public void failure(List<ReservationID> failed, List<ReservationID> ok,
			List<ReservationID> actOn) throws StatusCallbackException {
		System.out.println("FAILURE ON MODIFY WATCH OF " + failed);

	}

}
