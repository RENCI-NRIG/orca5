package orca.shirako.kernel;

import java.util.Properties;

import orca.shirako.api.IEvent;
import orca.shirako.api.IReservation;
import orca.util.ID;

public class ReservationDatabaseUpdateEvent implements IEvent {
    private IReservation reservation;
    private boolean before;
    
    public ReservationDatabaseUpdateEvent(IReservation reservation, boolean before) {
        this.reservation = reservation;
        this.before = before;
    }
    
    public ID getActorID() {
        return reservation.getActor().getGuid();
    }

    public Properties getProperties() {
        return null;
    }
    
    public IReservation getReservation() {
        return reservation;
    }
    
    public boolean IsBefore() {
        return before;
    }
}