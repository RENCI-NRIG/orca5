package orca.manage.internal;

import java.util.Properties;

import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.util.RPCError;
import orca.shirako.util.RPCException;

public class ManagementUtils {
   static class MyQueryResponseHandler implements IQueryResponseHandler {
        Properties result;
        RPCException error;

        public void handle(RPCException t, Properties response) {
            synchronized (this) {
                error = t;
                result = response;
                this.notify();
            }
        }
    };

    public static Properties query(IActor actor, IActorProxy actorProxy, Properties query) throws RPCException {
        MyQueryResponseHandler handler = new MyQueryResponseHandler();
        // issue the query
        actor.query(actorProxy, query, handler);
        // wait fot the query to complete
        synchronized (handler) {
            try {
                handler.wait();
            } catch (InterruptedException e) {
                throw new RPCException(RPCError.LocalError, e);
            }
        }
        if (handler.error != null) {
            throw new RPCException(handler.error);
        }
        return handler.result;
    }

	public static void updateReservation(IReservation r, ReservationMng mng){
		if (r instanceof IClientReservation){
			IClientReservation rc = (IClientReservation)r;
			rc.setRenewable(((TicketReservationMng)mng).isRenewable());
		}
		
		Converter.absorbProperties(mng, r);
	}

	public static void updateSlice(ISlice s, SliceMng mng){
		Converter.absorbProperties(mng, s);
	}

}