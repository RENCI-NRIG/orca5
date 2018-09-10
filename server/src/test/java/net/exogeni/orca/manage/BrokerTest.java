package net.exogeni.orca.manage;

import net.exogeni.orca.util.ID;

public abstract class BrokerTest extends ServerActorTest {
    public BrokerTest(ID actorGuid, String actorName) {
        super(actorGuid, actorName);
    }

    protected IOrcaBroker getBroker() {
        return (IOrcaBroker) getActor();
    }
}
