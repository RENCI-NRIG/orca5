package orca.shirako.common.delegation;

import orca.util.persistence.PersistenceException;
import orca.util.persistence.Saver;

public class ResourceTicketSaver implements Saver<ResourceTicket>{
	public String save(ResourceTicket obj) throws PersistenceException {
		if (obj == null) {
			throw new IllegalArgumentException("obj");
		}
		
		if (obj.getFactory() == null) {
			throw new IllegalStateException("ResourceTicket has no factory");
		}
		return obj.getFactory().toXML(obj);
	}
}