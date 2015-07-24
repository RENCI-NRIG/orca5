package orca.handlers.network.tasks;

import org.apache.tools.ant.BuildException;

public class CreateDTNCRSTask extends DTNBaseTask {
    protected String srcPort;
    protected String dstPort;
    protected String payloadType;
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

            if (payloadType == null) {
                throw new Exception("Missing payload type");
            }

            if (null == ctag) {
                ctag = "1";
            }

            device.createCrossConnect(srcPort, dstPort, payloadType, ctag);
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

    public void setPayloadtype(String payloadType) {
        this.payloadType = payloadType;
    }

    public void setCTag(String ctag) {
        this.ctag = ctag;
    }
}
