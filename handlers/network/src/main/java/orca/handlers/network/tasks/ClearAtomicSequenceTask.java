package orca.handlers.network.tasks;

import orca.shirako.plugins.config.OrcaAntTask;
import orca.shirako.plugins.config.SliceProject;
import orca.shirako.util.SemaphoreMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Remove a sempahore from the map to help GC
 * @author ibaldin
 *
 */
public class ClearAtomicSequenceTask extends OrcaAntTask {
	// get semaphore map from config
	protected String exclusiveDevice;
	protected int timeout;
	
	@Override
    public void execute() throws BuildException {
    	super.execute();
		
		if (exclusiveDevice == null) {
			throw new BuildException("ClearAtomicSequenceTask: Missing exclusive device property");
		}
		
		Project opr = getProject();
		
		// when testing handlers, it really is just Project, cant get the map
		if (!(opr instanceof SliceProject))
			return;
		
		SliceProject pr = (SliceProject)getProject();
		
		SemaphoreMap sems = (SemaphoreMap)pr.getSemaphoreMap();
		
		try {
			sems.delete(exclusiveDevice);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered deleting semaphore for " + exclusiveDevice + ": " + e);
    	}
	}
	
	public void setDevice(String s) {
		exclusiveDevice = s;
	}
}
