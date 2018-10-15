/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.container;


import java.util.Properties;

import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.PersistenceException;
import net.exogeni.orca.util.persistence.PersistenceUtils;
import net.exogeni.orca.util.persistence.Persistent;


public class ProtocolDescriptor implements Persistable
{
    public static final String PropertyProtocol = "protocol";
    public static final String PropertyLocation = "location";

    public static ProtocolDescriptor newInstace(Properties p) throws PersistenceException
    {
    	return PersistenceUtils.restore(p);
    }

    @Persistent (key = PropertyProtocol)
    protected String protocol;
    @Persistent (key = PropertyLocation)
    protected String location;

    public ProtocolDescriptor()
    { 
    }
    
    public ProtocolDescriptor(String protocol, String location)
    {
        this.protocol = protocol;
        this.location = location;
    }

    /**
     * @return the location
     */
    public String getLocation()
    {
        return this.location;
    }

    /**
     * @return the protocol
     */
    public String getProtocol()
    {
        return this.protocol;
    }
}
