package orca.util;

public class OrcaException extends Exception {
	private static final long serialVersionUID = 7097136969854960335L;

	public OrcaException() {
	    super();
	}
	
	public OrcaException(String message) {
		super(message);
	}
	
	public OrcaException(Throwable t){
		super(t);
	}
	
	public OrcaException(String message, Throwable t){
		super(message + " due to " + t.getMessage(), t);
	}
}