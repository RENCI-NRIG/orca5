package net.exogeni.orca.shirako.api;

import java.util.Properties;

import net.exogeni.orca.shirako.util.RPCException;

public interface IQueryResponseHandler extends IRPCResponseHandler {
    public void handle(RPCException status, Properties result);
}
