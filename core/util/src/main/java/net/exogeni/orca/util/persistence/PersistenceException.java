package net.exogeni.orca.util.persistence;

import net.exogeni.orca.util.OrcaException;

/**
 * Exception resulting from saving/restoring an object.
 * @author aydan
 *
 */
public class PersistenceException extends OrcaException {
	private static final long serialVersionUID = 3594353438883108514L;

	public PersistenceException(String message) {
		super(message);
	}
	
	public PersistenceException(Throwable t){
		super(t);
	}
	
	public PersistenceException(String message, Throwable t){
		super(message, t);
	}
}
