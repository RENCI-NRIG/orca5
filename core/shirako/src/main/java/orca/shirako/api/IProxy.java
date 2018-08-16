/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import orca.shirako.util.RPCException;
import orca.util.persistence.Persistable;

import org.apache.log4j.Logger;


/**
 * <code>IProxy</code> defines the base interface each actor proxy must
 * implement.
 */
public interface IProxy extends IActorIdentity, Persistable
{
    /**
     * Type code for proxies using local communication.
     */
    public static final String ProxyTypeLocal = "local";

    /**
     * Type code for proxies using SOAP communication and axis2.
     */
    public static final String ProxyTypeSoapAxis2 = "soapaxis2";
 
    /**
     * Returns the type of the proxy, e.g., local, soap, xmlrpc, etc.
     *
     * @return proxy type
     */
    public String getType();
    
    /**
     * Executes the specified request.
     * @param request rpc request
     */
    public void execute(IRPCRequestState request) throws RPCException;
    
    /**
     * Returns the logger used by the proxy.
     * @return logger
     */
    public Logger getLogger();
}
