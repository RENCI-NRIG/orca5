package net.exogeni.orca.shirako.common.delegation;

import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.util.persistence.PersistenceException;
import net.exogeni.orca.util.persistence.Saver;

public class ResourceTicketSaver implements Saver<ResourceTicket>{
	public String save(ResourceTicket obj) throws PersistenceException {
		if (obj == null) {
			throw new IllegalArgumentException("obj");
		}
		
		if (obj.getFactory() == null) {
			throw new IllegalStateException("ResourceTicket has no factory");
		}

		if (Globals.Log.isDebugEnabled()){
			Globals.Log.debug("Object of " + obj.getClass().getSimpleName() + " calling toXML()");
		}
		return obj.getFactory().toXML(obj);
	}
}
