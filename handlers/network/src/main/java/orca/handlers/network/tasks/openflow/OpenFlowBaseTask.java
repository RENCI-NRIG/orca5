package orca.handlers.network.tasks.openflow;

import orca.handlers.network.openflow.FlowVisorDevice;
import orca.handlers.network.tasks.NetworkBaseTask;

import org.apache.tools.ant.BuildException;

public abstract class OpenFlowBaseTask extends NetworkBaseTask {
	
	public static final String OpenFlow = "openflow";
    protected FlowVisorDevice device;
    protected String dpid = "all";
    protected String priority = "0";
    protected String name;

    @Override
    protected void makeDevice() {
        if (deviceInstance.equalsIgnoreCase(OpenFlow)) {
            device = new FlowVisorDevice(deviceAddress, user, password);
        } else {
            throw new RuntimeException("Unsupported Network Device: " + deviceInstance);
        }        
        configureDevice(device);
    }
    
    @Override
    public void execute() throws BuildException {
        try {
            super.execute();
            makeDevice();
            if ((name == null) || (name.length() == 0))
            	throw new Exception("slice name is null or empty");
            
            if ((dpid == null) || (dpid.length() == 0))
            	throw new Exception ("slice dpid is null or empty");
            
            try {
            	Integer.decode(priority);
            } catch (NumberFormatException e) {
            	throw new Exception ("slice priority is not numeric: " + priority);
            }
            
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("[OpenFlow.OpenFlowBaseTask] An error occurred: " + e.getMessage(), e);
        }
    }

    /**
     * Only set dpid if non zero length (otherwise assume "all")
     * @param dpid
     */
    public void setDpid(String dpid) {
    	if (dpid.length() > 0)
    		this.dpid = dpid;
    }
    
    /**
     * Only set priority if non zero length (otherwise assume "0")
     * @param priority
     */
    public void setPriority(String priority) {
    	if (priority.length() > 0)
    		this.priority=priority;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
