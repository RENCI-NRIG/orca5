/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers;

public class ActionOverlapCodes
{
    /**
     * Ignore the previous action.
     */
    public static final int OverlapIgnore = 1;

    /**
     * Wait for the previous action to complete.
     */
    public static final int OverlapWait = 2;

    /**
     * Cancel the previous action.
     */
    public static final int OverlapCancel = 3;

    /**
     * Report an error.
     */
    public static final int OverlapError = 4;
}