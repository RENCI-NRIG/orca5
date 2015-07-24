package orca.manage;

import orca.util.ID;

public abstract class ServiceManagerTest extends ActorTest {
	public ServiceManagerTest(ID actorGuid, String actorName) {
		super(actorGuid, actorName);
	}
	
	protected IOrcaServiceManager getServiceManager() {
		return (IOrcaServiceManager)getActor();
	}
}