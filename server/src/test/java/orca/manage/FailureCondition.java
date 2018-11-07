package orca.manage;

import orca.shirako.api.IEvent;
import orca.util.ID;

public abstract class FailureCondition {
    protected ID actorID;

    public FailureCondition(ID actorID) {
        this.actorID = actorID;
    }

    public abstract boolean matches(IEvent event);
}