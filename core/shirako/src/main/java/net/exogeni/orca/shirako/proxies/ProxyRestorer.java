package net.exogeni.orca.shirako.proxies;

import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.util.Serializer;
import net.exogeni.orca.util.persistence.PersistenceException;
import net.exogeni.orca.util.persistence.Restorer;

public class ProxyRestorer implements Restorer<IProxy> {
	public IProxy restore(String saved) throws PersistenceException {
		try {
			return Proxy.getProxy(Serializer.toProperties(saved));
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}
}
