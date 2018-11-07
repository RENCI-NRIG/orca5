package orca.util.persistence;

import java.util.Properties;

/**
 * Indicates that the object provides custom restore logic
 * @author aydan
 */
public interface CustomRestorable{
	/**
	 * Finishes restoring the object
	 * @param saved the object's current saved state to be augmented by this method.
	 * @throws PersistenceException in case of error
	 */
	public void restore(Properties saved) throws PersistenceException;
}
