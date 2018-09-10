package net.exogeni.orca.util.persistence;


/**
 * Class responsible for recovering instances of another class.
 * Overrides the generic restore method in PersistenceUtils.
 * @author aydan
 *
 * @param <T> Type of the parameter
 */
public interface Recoverer<T> {
	T recover(RecoverParent parent, String p) throws PersistenceException;
}
