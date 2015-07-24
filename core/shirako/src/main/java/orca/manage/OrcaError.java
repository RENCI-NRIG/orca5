package orca.manage;

import orca.manage.beans.ResultMng;
import orca.util.ErrorUtils;

public class OrcaError {
	private ResultMng status;
	private Exception e;
	
	public OrcaError(ResultMng status, Exception e){
		this.status = status;
		this.e = e;
	}
	
	public ResultMng getStatus() {
		return status;
	}
	
	public Exception getException() {
		return e;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (status != null) {
			sb.append("Status: code=");
			sb.append(status.getCode());
			sb.append(" (");
			sb.append(OrcaConstants.getErrorMessage(status.getCode()));
			sb.append(")");
			if (status.getMessage() != null){
				sb.append(" message=");			
				sb.append(status.getMessage());
			}
			if (status.getDetails() != null){
				sb.append(" details=");
				sb.append(status.getDetails());
			}
		} else {
			sb.append("Status: no status indicated");
		}
		if (e != null){
			sb.append("\n");
			sb.append("Exception: ");
			sb.append(e.getMessage());
			sb.append("\n");
			sb.append(ErrorUtils.getStackTrace(e));
		}
		return sb.toString();
	}
}
