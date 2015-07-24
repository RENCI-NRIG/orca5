package orca.shirako.util;

import orca.shirako.api.IEvent;
import orca.shirako.api.IEventFilter;
import orca.util.ID;

public class AllActorEventsFilter implements IEventFilter {
	private final ID id;
	public AllActorEventsFilter(ID actorGUID){
		this.id = actorGUID;
	}
	
	public boolean matches(IEvent event) {
		return id.equals(event.getActorID());
	}	
}