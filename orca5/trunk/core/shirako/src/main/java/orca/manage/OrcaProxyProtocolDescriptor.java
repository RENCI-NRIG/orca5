/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage;

/**
 * <code>ProxyProtocolDescriptor</code> describes a proxy class that can be used to
 * communicate with a manager object.
 * @author aydan
 *
 */
public class OrcaProxyProtocolDescriptor
{
	/**
	 * Communication protocol.
	 */
    protected String protocol;
    /**
     * Name of the class implementing this proxy.
     */
    protected String proxyClass;

    /**
     * Creates a new instance.
     */
    public OrcaProxyProtocolDescriptor()
    {
    }

    /**
     * Creates a new instance.
     * @param protocol protocol name
     * @param proxyClass class name
     */
    public OrcaProxyProtocolDescriptor(String protocol, String proxyClass)
    {
        this.protocol = protocol;
        this.proxyClass = proxyClass;
    }

    /**
     * Returns the protocol name.
     * @return
     */
    public String getProtocol()
    {
        return protocol;
    }

    /**
     * Sets the protocol name.
     * @param protocol protocol name
     */
    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Gets the proxy class name.
     * @return class name
     */
    public String getProxyClass()
    {
        return proxyClass;
    }

    /**
     * Sets the proxy class name.
     * @param proxyClass class name.
     */
    public void setProxyClass(String proxyClass)
    {
        this.proxyClass = proxyClass;
    }
}