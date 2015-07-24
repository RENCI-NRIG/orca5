package orca.shirako.container;

import java.util.List;

import orca.security.AuthToken;
import orca.shirako.api.IEvent;
import orca.shirako.api.IEventFilter;
import orca.shirako.api.IEventHandler;

public class SynchronousEventSubscription extends AEventSubscription {
	private final IEventHandler handler;
	
	public SynchronousEventSubscription(IEventHandler handler, AuthToken token, IEventFilter... optFilters) {
		super(token, optFilters);
		this.handler = handler;
	}
	

	public void deliverEvent(IEvent event){
		if (matches(event)){
			handler.handle(event);
		}
	}

	public List<IEvent> drainEvents(int timeout) {
		throw new RuntimeException("drainEvents is not supported on synchronous subscriptions");
	}

	public boolean isAbandoned() {
		return false;
	}
}