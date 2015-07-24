package orca.shirako.common.delegation;

import orca.shirako.api.IShirakoPlugin;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.RecoverParent;
import orca.util.persistence.Recoverer;

public class ResourceTicketRecoverer implements Recoverer<ResourceTicket> {
	public ResourceTicket recover(RecoverParent parent, String xml)
			throws PersistenceException {
		IShirakoPlugin plugin = parent.getObject(IShirakoPlugin.class);
		return plugin.getTicketFactory().fromXML(xml);
	}
}