package net.exogeni.orca.shirako.api;

public interface IEventFilter {
	boolean matches(IEvent event);
}
