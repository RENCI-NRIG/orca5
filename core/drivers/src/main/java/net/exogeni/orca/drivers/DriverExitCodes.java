/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.drivers;

public class DriverExitCodes
{
    public static int OK = 0;
    public static int InternalError = -1;
    public static int UnknownAction = -10;
    public static int InvalidArguments = -20;
    public static int RemoteError = -30;
    public static int ClientInternalError = -40;
    public static int ErrorInvalidActionOverlap = -50;
    public static int ErrorInterrupted = -60;
    public static int ErrorNotImplemented = -70;

    // anything below -10000 is driver specific
}