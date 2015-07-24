package orca.util.persistence;


/**
 * Class responsible for recovering instances of another class.
 * Overrides the generic restore method in PersistenceUtils.
 * @author aydan
 *
 * @param <T>
 */
public interface Recoverer<T> {
	T recover(RecoverParent parent, String p) throws PersistenceException;
}