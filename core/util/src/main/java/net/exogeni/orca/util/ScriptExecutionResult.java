/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;

import java.util.Properties;


public class ScriptExecutionResult
{
    public int code;
    public String command;
    public String stdout;
    public String stderr;
    public Long startTime;
    public Long stopTime;
    public Properties p;
}
