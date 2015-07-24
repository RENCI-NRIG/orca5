package orca.handlers.network.tasks;

import org.apache.tools.ant.BuildException;

public class CreateOSPatchTask extends OSBaseTask {
    protected String inputPort;
    protected String outputPort;
    protected String ctag;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (inputPort == null) {
                throw new Exception("Missing input port");
            }

            if (outputPort == null) {
                throw new Exception("Missing output port");
            }

            if (null == ctag) {
                ctag = "1";
            }

            device.createPatch(inputPort, outputPort, ctag);
            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setInputPort(String inputPort) {
        this.inputPort = inputPort;
    }

    public void setOutputPort(String outputPort) {
        this.outputPort = outputPort;
    }

    public void setCTag(String ctag) {
        this.ctag = ctag;
    }
}
