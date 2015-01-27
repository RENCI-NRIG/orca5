package orca.handlers.network.tasks;

import orca.handlers.network.core.CommandException;
import orca.handlers.network.core.INetworkDevice;
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;

public abstract class NetworkBaseTask extends OrcaAntTask {
    public static final String PropertyEmulation = "emulation";

    protected String user;
    protected String password;
    protected String deviceAddress;
    protected String deviceInstance;

    protected abstract void makeDevice() throws CommandException;

    protected void configureDevice(INetworkDevice device) {
        String temp =  (getProject().getProperty(PropertyEmulation));
        if (temp != null) {
            temp = temp.trim();
            if (temp.equalsIgnoreCase("true")) {
                device.enableEmulation();
            }
        }
    }
    
    @Override
    public void execute() throws BuildException {
        try {
            super.execute();
            if (user == null) {
                throw new RuntimeException("Missing device user ID");
            }

            if (password == null) {
                throw new RuntimeException("Missing device password");
            }

            if (deviceAddress == null) {
                throw new RuntimeException("Missing device address");
            }
            
            if (deviceInstance == null) {
                throw new RuntimeException("Missing device instance name");
            }
            makeDevice();
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public void setDeviceInstance(String deviceInstance) {
        this.deviceInstance = deviceInstance;
    }
}
