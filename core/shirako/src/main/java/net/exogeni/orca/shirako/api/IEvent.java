package net.exogeni.orca.shirako.api;

import java.util.Properties;

import net.exogeni.orca.util.ID;

/**
 * <code>IEvent</code> defines an event interface for events raised by the core.
 */
public interface IEvent{
	/**
	 * Returns the ID of the actor that contains the object that generated
	 * the event. Can be null for container-level events.
	 * @return actor id
	 */
	public ID getActorID();
	/**
	 * An optional properties list describing the event.
	 * @return properties
	 */
	public Properties getProperties();
}
