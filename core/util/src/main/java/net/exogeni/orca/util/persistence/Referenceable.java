package net.exogeni.orca.util.persistence;

import net.exogeni.orca.util.ID;

/**
 * Marker interface that an object can produce a reference id.
 * The reference id will be persisted when saving the containing object and will be used
 * during recovery to obtain the referenced object.
 * 
 * @author aydan
 *
 */
public interface Referenceable {
	ID getReference();
}
