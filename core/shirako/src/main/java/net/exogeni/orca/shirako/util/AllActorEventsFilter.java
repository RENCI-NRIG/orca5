package net.exogeni.orca.shirako.util;

import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.shirako.api.IEventFilter;
import net.exogeni.orca.util.ID;

public class AllActorEventsFilter implements IEventFilter {
	private final ID id;
	public AllActorEventsFilter(ID actorGUID){
		this.id = actorGUID;
	}
	
	public boolean matches(IEvent event) {
		return id.equals(event.getActorID());
	}	
}
