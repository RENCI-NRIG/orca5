/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.util;

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActorIdentity;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.Persistent;


public class Client implements Persistable
{
    public static final String PropertyName = "name";
    public static final String PropertyGuid = "guid";

    @Persistent (key = PropertyName)
    protected String name;
    @Persistent (key = PropertyGuid)
    protected ID guid;

    public Client()
    {
    }

    public void revisit(IActorIdentity actor, Properties p) throws Exception
    {
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public void setGuid(ID guid)
    {
        this.guid = guid;
    }        

    public String getName()
    {
        return name;
    }
    
    public ID getGuid()
    {
        return guid;
    }
    
    public AuthToken getAuthToken()
    {
        return new AuthToken(name, guid);
    }
}
