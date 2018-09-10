package net.exogeni.orca.util.persistence;

import net.exogeni.orca.util.ID;

/**
 * Parent object with state used to recover other objects.
 * @author aydan
 */
public interface RecoverParent {
	RecoverParent getRecoveryRoot();
	<V> V getObject(Class<V> type) throws PersistenceException;
	<V> V getObject(Class<V> type, ID reference) throws PersistenceException; 	
}
