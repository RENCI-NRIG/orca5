package net.exogeni.orca.handlers.network.tasks;

import org.apache.tools.ant.BuildException;

public class DeleteDTNCRSTask extends DTNBaseTask {
    protected String srcPort;
    protected String dstPort;
    protected String ctag;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (srcPort == null) {
                throw new Exception("Missing src port");
            }

            if (dstPort == null) {
                throw new Exception("Missing dest port");
            }

            if (null == ctag) {
                ctag = "1";
            }

            device.deleteCrossConnect(srcPort, dstPort, ctag);
            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setSrcPort(String srcPort) {
        this.srcPort = srcPort;
    }

    public void setDstPort(String dstPort) {
        this.dstPort = dstPort;
    }

    public void setCTag(String ctag) {
        this.ctag = ctag;
    }
}
