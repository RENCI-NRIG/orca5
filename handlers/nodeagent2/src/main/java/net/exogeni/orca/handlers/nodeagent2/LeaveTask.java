package net.exogeni.orca.handlers.nodeagent2;

import org.apache.tools.ant.BuildException;

public class LeaveTask extends RestTask {

    public LeaveTask() {
        rop = RestOperations.LEAVE;
    }

    @Override
    public void execute() throws BuildException {

        _execute();
    }

}
