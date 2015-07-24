package orca.handlers.network.tasks.openflow;

import org.apache.tools.ant.BuildException;

public class AddIPFlowSpaceTask extends OpenFlowBaseTask {

	protected String srcIP;
	protected String dstIP;

	@Override
	public void execute() throws BuildException {
		super.execute();
		
		try {
			
			if (srcIP == null) {
				throw new Exception("Missing source ip address");
			}
			
			if (dstIP == null) {
				throw new Exception("Missing destination ip address");
			}

			device.addIPFlowSpace(name, dpid, priority, srcIP, dstIP);
			setResult(0);
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException(
					"[OpenFlow.AddIpFlowSpaceTask] An error occurred: "
							+ e.getMessage(), e);
		}
	}

	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}

	public void setDstIP(String dstIP) {
		this.dstIP = dstIP;
	}
}
