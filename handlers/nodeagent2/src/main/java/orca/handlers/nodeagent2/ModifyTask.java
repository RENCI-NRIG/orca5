package orca.handlers.nodeagent2;

import org.apache.tools.ant.BuildException;

public class ModifyTask extends RestTask {

    public ModifyTask() {
        rop = RestOperations.MODIFY;
    }

    @Override
    public void execute() throws BuildException {

        _execute();
    }

}
