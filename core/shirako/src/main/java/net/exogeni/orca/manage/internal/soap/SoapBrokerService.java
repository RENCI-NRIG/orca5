package net.exogeni.orca.manage.internal.soap;

import net.exogeni.orca.manage.internal.BrokerManagementObject;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.util.ID;

public class SoapBrokerService extends SoapService {
	public static final String BROKER_NS = "http://www.nicl.duke.edu/orca/manage/services/broker";

	public SoapBrokerService(){
	}
	
	protected BrokerManagementObject getActorMO(ID guid) {
		try {
		return (BrokerManagementObject)Globals.getContainer().getManagementObjectManager().getManagementObject(guid);
		} catch (Exception e){
			throw new RuntimeException("Invalid actor guid: " + guid);
		}
	}	
}
