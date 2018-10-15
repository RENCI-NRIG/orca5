package net.exogeni.orca.manage.internal.local;

import net.exogeni.orca.manage.IOrcaContainer;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.internal.ContainerManagementObject;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.container.Globals;

public class LocalConnector {
	public static IOrcaContainer connect(AuthToken token){
		// obtain the container manager object
		ContainerManagementObject man = (ContainerManagementObject)Globals.getContainer().getManagementObjectManager().getManagementObject(OrcaConstants.ContainerManagmentObjectID);
		// create the local proxy
		LocalContainer proxy = new LocalContainer(man, token);
		// "Login" to enable access.
		proxy.login();		
		return proxy;				
	}
}
