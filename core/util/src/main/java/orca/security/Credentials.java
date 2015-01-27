/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.security;

import java.util.Properties;

import orca.util.persistence.Persistable;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.PersistenceUtils;

/**
 * <code>Credentials</code> represents an authentication/authorization credentials. 
 * @author aydan
 *
 */
public class Credentials implements Persistable
{
    public Credentials()
    {
    }
 
    public static Credentials getCredentials(Properties p) throws PersistenceException
    {
    	return PersistenceUtils.restore(p);
    }
}