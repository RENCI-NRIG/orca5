package orca.shirako.proxies;

import orca.shirako.api.IProxy;
import orca.util.Serializer;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.Restorer;

public class ProxyRestorer implements Restorer<IProxy> {
	public IProxy restore(String saved) throws PersistenceException {
		try {
			return Proxy.getProxy(Serializer.toProperties(saved));
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}
}