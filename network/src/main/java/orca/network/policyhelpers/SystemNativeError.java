package orca.network.policyhelpers;

public class SystemNativeError {
	private int       error;
	private String    message;
	private String 	  additional;
	
	public String getAdditional() {
		return additional;
	}
	public void setAdditional(String additional) {
		this.additional = additional;
	}
	public final int    getErrno()   { return this.error;   }
	public final String getMessage() { return this.message; }
	
	public void setErrno(int error) {
		this.error = error;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public final String toString()
	{
		return this.error + " (" + this.message + ")";
	}
}
