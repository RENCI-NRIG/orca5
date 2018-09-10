/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.drivers.util;

import org.apache.log4j.Logger;


public class EventLogger
{
    protected String event;
    protected String operand;
    protected static final String none = "None";
    protected Logger logger;

    public EventLogger(String event, String operand)
    {
        this.event = event;
        this.operand = operand;
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    public void logEvent(String name, Long startTime, Long stopTime)
    {
        String format = event + "<>" + operand + "<>" + name + "<>" + startTime + "<>" + stopTime +
                        "<>" + (stopTime - startTime);
        logger.info(format);
    }

    public void logEvent(String name, Long startTime)
    {
        Long stopTime = System.currentTimeMillis();

        String format = event + "<>" + operand + "<>" + name + "<>" + startTime + "<>" + stopTime +
                        "<>" + (stopTime - startTime);
        logger.info(format);
    }

    public void logEvent(DriverScriptExecutionResult r)
    {
        String formatStdOut;

        if ((r.stdout != null) && (r.stdout.length() != 0)) {
            formatStdOut = r.stdout.replace("\n", " ");
        } else {
            formatStdOut = none;
        }

        String formatStdErr;

        if ((r.stderr != null) && (r.stderr.length() != 0)) {
            formatStdErr = r.stderr.replace("\n", " ");
        } else {
            formatStdErr = none;
        }

        String formatCommand;

        if ((r.command != null) && (r.command.length() != 0)) {
            formatCommand = r.command.replace("\n", " ");
        } else {
            formatCommand = none;
        }

        String format = event + "<>" + operand + "<>" + formatCommand + "<>" + formatStdOut + "<>" +
                        formatStdErr + "<>" + r.code + "<>" + r.startTime + "<>" + r.stopTime;

        logger.info(format);
    }
}