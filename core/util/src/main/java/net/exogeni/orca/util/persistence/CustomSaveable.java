 package net.exogeni.orca.util.persistence;

import java.util.Properties;

/**
 * Indicates that the object provides custom save logic.
 * @author aydan
 * 
 */
public interface CustomSaveable {
	/**
	 * Finishes saving the object
	 * @param p current saved state
	 * @throws PersistenceException in case of error
	 */
	public void save(Properties p) throws PersistenceException;
}
