package orca.handlers.network.tasks;

import orca.shirako.plugins.config.OrcaAntTask;
import orca.shirako.plugins.config.SliceProject;
import orca.shirako.util.SemaphoreMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Use file locking (so it works with Ant) to
 * indicate the end of an atomic sequence of operations
 * @author ibaldin
 *
 */
public class StopAtomicSequenceTask extends OrcaAntTask {
	protected String exclusiveDevice;
	
	@Override
    public void execute() throws BuildException {
		
    	super.execute();
		
		if (exclusiveDevice == null) {
			throw new BuildException("StopAtomicSequenceTask: Missing exclusive device property");
		}
    	
		Project opr = getProject();
		
		// when testing handlers, it really is just Project, cant get the map
		if (!(opr instanceof SliceProject))
			return;
		
		SliceProject pr = (SliceProject)getProject();
		
		SemaphoreMap sems = (SemaphoreMap)pr.getSemaphoreMap();
		
    	try {
    		sems.release(exclusiveDevice);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered releasing sequence lock " + exclusiveDevice + ": " + e);
    	}
	}
	
	public void setDevice(String s) {
		exclusiveDevice = s;
	}
}
