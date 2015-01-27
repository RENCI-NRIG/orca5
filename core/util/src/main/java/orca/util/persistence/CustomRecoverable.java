package orca.util.persistence;

import java.util.Properties;

import orca.util.OrcaException;

/**
 * A recoverable object that requires custom recovery logic.
 * The custom recovery logic is invoked after the generic recover algorithm.
 * @author aydan
 *
 */
public interface CustomRecoverable extends Recoverable {
	void recover(RecoverParent parent, Properties savedState) throws OrcaException;
}