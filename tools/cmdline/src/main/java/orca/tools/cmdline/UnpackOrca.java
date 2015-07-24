package orca.tools.cmdline;

import orca.extensions.internal.PackageManager;
import orca.shirako.container.Globals;

public class UnpackOrca {
	
	public static void main(String[] argv) {
		System.out.println("Unpacking ORCA in " + Globals.HomeDirectory);
		//System.setProperty("ORCA_HOME", "orca/");
		PackageManager pm = new PackageManager();
		try {
			pm.expandPackages();
		} catch (Exception e) {
			System.err.println("Unable to initialize package manager: " + e);
			e.printStackTrace();
		}
	}
}
