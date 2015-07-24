package orca.handlers.network.tasks.openflow;

import org.apache.tools.ant.BuildException;


public class RemoveFlowSpaceTask extends OpenFlowBaseTask {
	protected String match;

	@Override
	public void execute() throws BuildException {
		super.execute();

		try {
			if (name == null) {
				throw new Exception("Missing slice name");
			}

			if (match.equals("") || match.equals("any") || match.equals("all"))
				match = "OFMatch[]";

			device.execute("api.changeFlowSpace", new Object[] { name });
			setResult(0);
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException(
					"[OpenFlow.RemoveFlowSpaceTask] An error occurred: "
							+ e.getMessage(), e);
		}
	}

	public void setMatch(String match) {
		this.match = match;
	}

}
