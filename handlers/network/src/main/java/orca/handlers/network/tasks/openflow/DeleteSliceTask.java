package orca.handlers.network.tasks.openflow;

import org.apache.tools.ant.BuildException;

public class DeleteSliceTask extends OpenFlowBaseTask {

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {

            device.deleteSlice(name);
            setResult(0);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("[OpenFlow.DeleteSliceTask] An error occurred: " + e.getMessage(), e);
        }
    }

}
