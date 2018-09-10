package net.exogeni.orca.manage;

import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.util.ID;

public abstract class FailureCondition {
    protected ID actorID;

    public FailureCondition(ID actorID) {
        this.actorID = actorID;
    }

    public abstract boolean matches(IEvent event);
}
