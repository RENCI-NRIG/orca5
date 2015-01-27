package orca.util.persistence;


/**
 * Class responsible for saving instances of another class.
 * Overrides the generic restore method in PersistenceUtils.
 * @author aydan
 *
 * @param <T>
 */
public interface Saver<T> {
	String save(T obj) throws PersistenceException;
}