package orca.manage;

import orca.manage.proxies.soap.SoapContainer;
import orca.security.AuthToken;
import orca.util.ReflectionUtils;

public class Orca {
	public static final String PROTOCOL_SOAP = "soap://";

	public static IOrcaContainer connect() throws OrcaManagementException {
		return getLocalContainer(null);
	}

	public static IOrcaContainer connect(String user, String password)
			throws OrcaManagementException {
		return connectLocal(user, password);
	}

	public static IOrcaContainer connect(AuthToken auth) throws OrcaManagementException {
		return getLocalContainer(auth);
	}

	public static IOrcaContainer connect(String location) throws OrcaManagementException {
		if (location == null || location.isEmpty()) {
			throw new OrcaManagementException("Invalid location");
		}

		if (location.startsWith("local://")) {
			return getLocalContainer(null);
		} else if (location.startsWith(PROTOCOL_SOAP)) {
			;
			return getSoapContainer(location.substring(PROTOCOL_SOAP.length()));
		} else {
			throw new RuntimeException("Unsupported location: " + location);
		}
	}

	public static IOrcaContainer connect(String location, String user, String password)
			throws OrcaManagementException {
		if (location == null || location.isEmpty()) {
			throw new OrcaManagementException("Invalid location");
		}
		if (user == null || user.isEmpty()) {
			throw new OrcaManagementException("Invalid user");
		}
		if (password == null) {
			throw new OrcaManagementException("Invalid password");
		}

		if (location.startsWith("local://")) {
			return connectLocal(user, password);
		} else if (location.startsWith(PROTOCOL_SOAP)) {
			return connectSoap(location.substring(PROTOCOL_SOAP.length()), user, password);
		} else {
			throw new RuntimeException("Unsupported location: " + location);
		}
	}

	private static IOrcaContainer getLocalContainer(AuthToken caller)
			throws OrcaManagementException {
		// obtain a proxy to the container.
		// NOTE: we must use reflection to avoid dependency on the internal
		// package.
		IOrcaContainer proxy = (IOrcaContainer) ReflectionUtils.invokeStatic(
				"orca.manage.internal.local.LocalConnector", "connect", Thread.currentThread()
						.getContextClassLoader(), new Class<?>[] { AuthToken.class }, caller);

		if (proxy == null) {
			throw new OrcaManagementException("Could not obtain proxy to local container");
		}
		return proxy;
	}

	private static IOrcaContainer connectLocal(String user, String password)
			throws OrcaManagementException {
		// obtain a proxy to the container.
		// NOTE: we must use reflection to avoid dependency on the internal
		// package.
		IOrcaContainer proxy = getLocalContainer(null);
		authenticate(proxy, user, password);
		return proxy;
	}

	private static IOrcaContainer connectSoap(String location, String user, String password)
			throws OrcaManagementException {
		// obtain a proxy to the container.
		// NOTE: we must use reflection to avoid dependency on the internal
		// package.
		IOrcaContainer proxy = getSoapContainer(location);
		authenticate(proxy, user, password);
		return proxy;
	}

	private static void authenticate(IOrcaContainer proxy, String user, String password)
			throws OrcaManagementException {
		// attempt to log in
		try {
			if (!proxy.isLogged() && !proxy.login(user, password)) {
				throw new OrcaManagementException("Could not login", proxy.getLastError());
			}
		} catch (OrcaManagementException e) {
			throw e;
		} catch (Exception e) {
			throw new OrcaManagementException("An error occurred while attempting to log in", e);
		}
	}

	private static IOrcaContainer getSoapContainer(String url) throws OrcaManagementException {
		return new SoapContainer(OrcaConstants.ContainerManagmentObjectID, url, null);
	}
}