package orca.handlers.network.tasks.Ciena8700;

import org.apache.tools.ant.BuildException;

public class MapVLANSTask extends Ciena8700BaseTask {
    protected String srcVLAN;
    protected String dstVLAN;
    protected String port;

    @Override
    public void synchronizedExecute() throws BuildException {
        try {
            if (null == srcVLAN || null == dstVLAN) {
                throw new Exception("Missing vlan name");
            }

            if (port == null) {
                throw new RuntimeException("Missing port");
            }
            router.mapVLANs(srcVLAN, dstVLAN, port);
            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setSourceTag(String value) {
        this.srcVLAN = value;
    }

    public void setDestinationTag(String value) {
        this.dstVLAN = value;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
