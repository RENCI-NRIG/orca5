package orca.manage;

import orca.shirako.api.IEvent;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ReservationStateTransitionEvent;
import orca.shirako.util.ReservationState;
import orca.util.ID;

public class ReservationTransitionFailureCondition extends FailureCondition {
    private ReservationID rid;
    private ReservationState state;

    public ReservationTransitionFailureCondition(ID actorID, ReservationState state,
            ReservationID rid) {
        super(actorID);
        this.state = state;
        this.rid = rid;
    }

    public ReservationTransitionFailureCondition(ID actorID, ReservationState state) {
        super(actorID);
        this.state = state;
    }

    public void setReservationID(ReservationID rid) {
        this.rid = rid;
    }

    public boolean matches(IEvent event) {
        if (event instanceof ReservationStateTransitionEvent) {
            ReservationStateTransitionEvent e = (ReservationStateTransitionEvent) event;

            return e.getActorID().equals(this.actorID) && e.getReservationID().equals(this.rid)
                    && e.getState().equals(state);
        }
        return false;
    }
}