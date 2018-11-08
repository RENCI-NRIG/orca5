package orca.handlers.nodeagent2;

import org.apache.tools.ant.BuildException;

public class JoinTask extends RestTask {

    public JoinTask() {
        rop = RestOperations.JOIN;
    }

    @Override
    public void execute() throws BuildException {

        _execute();
    }

}
