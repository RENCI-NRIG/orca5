package orca.shirako.container;

import orca.security.AuthToken;
import orca.shirako.api.IEventFilter;
import orca.shirako.api.IEventHandler;

public class SynchronousPrivilegedEventSubscription extends SynchronousEventSubscription {
	public SynchronousPrivilegedEventSubscription(IEventHandler handler, IEventFilter... optFilters) {
		super(handler, null, optFilters);
	}
	
    public boolean hasAccesss(AuthToken caller){
        return true;
    }
}