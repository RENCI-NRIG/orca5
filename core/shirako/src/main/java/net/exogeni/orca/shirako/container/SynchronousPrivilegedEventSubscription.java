package net.exogeni.orca.shirako.container;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IEventFilter;
import net.exogeni.orca.shirako.api.IEventHandler;

public class SynchronousPrivilegedEventSubscription extends SynchronousEventSubscription {
	public SynchronousPrivilegedEventSubscription(IEventHandler handler, IEventFilter... optFilters) {
		super(handler, null, optFilters);
	}
	
    public boolean hasAccesss(AuthToken caller){
        return true;
    }
}
