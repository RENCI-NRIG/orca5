package orca.util.persistence;


// FIXME: are we using this one any more?

public interface Restorable{
	public void restore(String saved) throws PersistenceException;
}