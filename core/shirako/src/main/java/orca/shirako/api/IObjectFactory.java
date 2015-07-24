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

import java.util.Properties;


/**
 * <code>IObjectFactory</code> defines the interface for a generic object
 * factory.
 */
public interface IObjectFactory
{
    /**
     * Creates a new instance of the specified object using the given
     * properties list.
     *
     * @param properties properties list containing information about created
     *        object
     *
     * @return DOCUMENT ME!
     */
    public Object newInstance(Properties properties) throws Exception;
}