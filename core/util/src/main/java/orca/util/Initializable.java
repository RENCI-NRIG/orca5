/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.util;


/**
 * <code>Initializable</code> defines a method to perform
 * class initialization. Each implementation should ensure that initialization
 * is performed at most once.
 */
public interface Initializable
{
    /**
     * Initializes the object.
     *
     * @throws Exception if initialization fails
     */
    public void initialize() throws OrcaException;
}