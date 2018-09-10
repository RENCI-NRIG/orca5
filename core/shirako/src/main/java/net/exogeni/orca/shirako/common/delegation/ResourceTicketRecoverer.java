package net.exogeni.orca.shirako.common.delegation;

import net.exogeni.orca.shirako.api.IShirakoPlugin;
import net.exogeni.orca.util.persistence.PersistenceException;
import net.exogeni.orca.util.persistence.RecoverParent;
import net.exogeni.orca.util.persistence.Recoverer;

public class ResourceTicketRecoverer implements Recoverer<ResourceTicket> {
	public ResourceTicket recover(RecoverParent parent, String xml)
			throws PersistenceException {
		IShirakoPlugin plugin = parent.getObject(IShirakoPlugin.class);
		return plugin.getTicketFactory().fromXML(xml);
	}
}
