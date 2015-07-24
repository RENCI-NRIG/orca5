package orca.handlers.network.tasks.openflow;

import org.apache.tools.ant.BuildException;

public class CreateSliceTask extends OpenFlowBaseTask{
    protected String passwd;
    protected String ctrlUrl;
    protected String email;

    @Override
    public void execute() throws BuildException {
    	super.execute();

    	// to guarantee idempotence, failure in creating
    	// a slice triggers an attempt to delete a slice
    	// of same name and then trying again. Only then
    	// do we fail.
    	try {

    		if (passwd == null) {
    			throw new Exception("Missing slice password");
    		}

    		if (ctrlUrl == null) {
    			throw new Exception("Missing slice controller url");
    		}

    		if (email == null) {
    			throw new Exception("Missing slice user email");
    		}

    		try {
    			//device.execute("api.createSlice", new Object[] { name, passwd, ctrlUrl, email });
    			device.createSlice(name, passwd, ctrlUrl, email);
    		} catch (BuildException ee) {
    			// try to delete it and then create it;
    			// if that throws an exception, then we're done
    			
    			// WARNING: This may leave an orphan controller attached to previous slice! /ib
    			device.deleteSlice(name);
    			device.createSlice(name, passwd, ctrlUrl, email);
    		}

    		setResult(0);
    	} catch (BuildException e) {
    		throw e;
    	} catch (Exception e) {
    		throw new BuildException("[OpenFlow.CreateSliceTask] An error occurred: " + e.getMessage(), e);
    	}
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public void setCtrlUrl(String ctrlUrl) {
        this.ctrlUrl = ctrlUrl;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }

}
