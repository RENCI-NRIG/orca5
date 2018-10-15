package net.exogeni.orca.embed.cloudembed;

import net.exogeni.orca.embed.policyhelpers.SystemNativeError;
import net.exogeni.orca.ndl.elements.NetworkConnection;

public interface IConnectionManager {

    public SystemNativeError createConnection(NetworkConnection requestConnection, boolean interDomainRequest,
            boolean needExchange, String nc_of_version);

    public NetworkConnection releaseConnection(NetworkConnection conn, String requestURI);

    public NetworkConnection releaseInModel(NetworkConnection conn, String requestURI);

}
