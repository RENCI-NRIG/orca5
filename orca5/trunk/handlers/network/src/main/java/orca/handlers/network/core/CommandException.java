package orca.handlers.network.core;

public class CommandException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CommandException(String msg) {
		super(msg);
	}
	
	public CommandException(String msg, Exception inner) {
	    super(msg, inner);
	}
	
	public CommandException(Exception inner) {
	    super(inner);
	}
}
