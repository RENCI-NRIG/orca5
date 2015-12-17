package orca.handlers.xcat.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import orca.shirako.common.meta.UnitProperties;

import org.apache.tools.ant.BuildException;

public class XCatGenerateBashFileRemIfaceTask extends XCatGenerateBashFileModTask {

	protected void generateInterface(PrintWriter out) throws Exception {        

		out.println("#");
		out.println("# interface configuration section");
		out.println("#");

		// see what physical interface on the host we need to attach to (eth0 if unspecified)
		String hostNet = getProject().getProperty(modifyPrefix() + UnitProperties.UnitHostEthSuffix);
		if (hostNet == null) {
			System.out.println("Modify remove interface " + modifyIndex + " is missing hosteth/network name. Ignoring");
			return;
		}
		String hosteth = networkMap.get(hostNet.trim());
		if (hosteth == null) {
			System.out.println("Unable to find an interface mapping for host interface network " + hostNet);
			return;
		}
		hosteth = hosteth.trim();

		// disable tagged interface
		String tag = getProject().getProperty(modifyPrefix() + UnitProperties.UnitEthVlanSuffix);
		if (tag == null) {
			System.out.println("Modify add interface " + modifyIndex + " is missing vlan tag. Ignoring.");
			return;
		}

		bashRemoveInterfaceCommands(out, hosteth, tag);
		out.println(); 
	}
	
	public void execute() throws BuildException {
		try {
			super.execute();
			if (file == null) {
				throw new Exception("Missing file parameter");
			}
			PrintWriter out = new PrintWriter(new FileWriter(new File(file)));
			
			generateInterface(out); 
			// currently we do nothing to remove storage 11/04/2015 /ib
			// generateStorage(out);
			out.close();
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException("An error occurred: " + e.getMessage(), e);
		}
	}

}
