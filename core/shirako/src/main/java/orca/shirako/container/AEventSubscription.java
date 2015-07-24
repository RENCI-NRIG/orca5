package orca.shirako.container;

import java.util.HashMap;
import java.util.List;

import orca.security.AuthToken;
import orca.shirako.api.IEvent;
import orca.shirako.api.IEventFilter;
import orca.util.ID;

public abstract class AEventSubscription {
	protected final ID subscriptionID;
	private final AuthToken token;
	private final HashMap<ID, IEventFilter> filters;
	
	public AEventSubscription(AuthToken token, IEventFilter... optFilters) {
		this.token = token;
		this.subscriptionID = new ID();		
		this.filters = new HashMap<ID, IEventFilter>();
		if (optFilters != null) {
			for (IEventFilter f : optFilters){
				filters.put(new ID(), f);
			}
		}
	}
	
	public ID getSubscriptionID() {
		return subscriptionID;
	}
	
	public boolean hasAccesss(AuthToken caller){
		if (caller == null) {return false;}
		if (caller.getName() == null) {return false;}
		if (token == null) {return false;}
		return (caller.getName().equals(token.getName()));
	}
	
	public ID addEventFilter(IEventFilter filter) {
		ID fid = new ID();
		filters.put(fid, filter);
		return fid;
	}
	
	public void deleteEventFilter(ID fid){
		filters.remove(fid);
	}
	
	protected boolean matches(IEvent event){
		// if at least one filter rejects the event,
		// then reject the event
		if (filters.size() == 0){return true;}
		for (IEventFilter f : filters.values()) {
			if (!f.matches(event)) {
				return false;
			}
		}
		return true;
	}
	
	public abstract void deliverEvent(IEvent event);
	public abstract List<IEvent> drainEvents(int timeout);
	public abstract boolean isAbandoned();

}