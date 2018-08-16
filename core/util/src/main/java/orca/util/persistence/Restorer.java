package orca.util.persistence;


/**
 * Class responsible for restoring instances of another class.
 * Overrides the generic restore method in PersistenceUtils.
 * @author aydan
 *
 * @param <T> Type of the parameter
 */
public interface Restorer<T> {
	T restore(String p) throws PersistenceException;
}
