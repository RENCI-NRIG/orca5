package orca.manage.internal.local;

import orca.manage.IOrcaContainer;
import orca.manage.OrcaConstants;
import orca.manage.internal.ContainerManagementObject;
import orca.security.AuthToken;
import orca.shirako.container.Globals;

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