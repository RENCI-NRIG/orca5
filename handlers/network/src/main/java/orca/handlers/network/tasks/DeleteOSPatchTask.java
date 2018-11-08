package orca.handlers.network.tasks;

import org.apache.tools.ant.BuildException;

public class DeleteOSPatchTask extends OSBaseTask {
    protected String port;
    protected String ctag;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (port == null) {
                throw new Exception("Missing port");
            }

            if (null == ctag) {
                ctag = "1";
            }

            device.deletePatch(port, ctag);
            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setCTag(String ctag) {
        this.ctag = ctag;
    }
}
