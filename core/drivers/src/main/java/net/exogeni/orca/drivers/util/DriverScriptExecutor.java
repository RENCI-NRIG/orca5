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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;


public class DriverScriptExecutor
{
    protected String command;
    protected String[] cmdArray;
    protected boolean log = false;
    protected long timeout = 0;
    protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    public DriverScriptExecutor(String[] cmdArray)
    {
        this.cmdArray = cmdArray;
    }

    public DriverScriptExecutor(String command)
    {
        this.command = command;
    }

    public DriverScriptExecutor(String[] cmdArray, long timeout, boolean log)
    {
        this.cmdArray = cmdArray;
        this.timeout = timeout;
        this.log = log;
    }

    public DriverScriptExecutor(String command, long timeout)
    {
        this.command = command;
        this.timeout = timeout;
    }

    public DriverScriptExecutionResult execute() throws Exception
    {
        String executedCommand;
        Runtime rt = Runtime.getRuntime();

        if (command != null) {
            if (log && (logger.isDebugEnabled())) {
                logger.debug("Command: " + command);
            }

            executedCommand = command;
        } else {
            StringBuilder sb = new StringBuilder();
            StringBuilder commandString = new StringBuilder();
            sb.append("Command: ");
            sb.append(cmdArray[0]);
            commandString.append(cmdArray[0] + " ");
            sb.append("\n Arguments: ");

            for (int i = 1; i < cmdArray.length; i++) {
                sb.append(cmdArray[i]);
                commandString.append(cmdArray[i]);
                sb.append(" ");
                commandString.append(" ");
            }

            if (log && (logger.isDebugEnabled())) {
                logger.debug(sb.toString());
            }

            executedCommand = commandString.toString();
        }

        Process proc = null;

        Long end;
        Long start = System.currentTimeMillis();

        if (cmdArray == null) {
            proc = rt.exec(command);
        } else {
            proc = rt.exec(cmdArray);
        }

        InputStream in = proc.getInputStream();
        InputStreamReader inr = new InputStreamReader(in);
        BufferedReader inbr = new BufferedReader(inr);

        InputStream err = proc.getErrorStream();
        InputStreamReader errr = new InputStreamReader(err);
        BufferedReader errbr = new BufferedReader(errr);

        String line;
        boolean moreIn = true;
        boolean moreErr = true;
        StringBuffer inb = new StringBuffer();
        StringBuffer eb = new StringBuffer();

        while (moreIn || moreErr) {
            if (moreIn) {
                line = inbr.readLine();

                if (line == null) {
                    moreIn = false;
                } else {
                    if (inb.length() > 0) {
                        inb.append("\n");
                    }

                    inb.append(line);
                }
            }

            if (moreErr) {
                line = errbr.readLine();

                if (line == null) {
                    moreErr = false;
                } else {
                    if (eb.length() > 0) {
                        eb.append("\n");
                    }

                    eb.append(line);
                }
            }

            /*if (timeout != 0) {
                    if ((System.currentTimeMillis() - start) > timeout) {
                            try {
                                    proc.exitValue();
                            } catch (Exception e) {
                                    proc.destroy();
                                    ExecutionResult r = new ExecutionResult();
                                    r.code = -1;
                                    r.stdout = inb.toString();
                                    r.stderr = eb.toString();

                                    return r;
                            }
                    }
            }*/
        }

        int code = 0;

        if (timeout == 0) {
            code = proc.waitFor();
        } else {
            code = proc.waitFor();

            /*while ((System.currentTimeMillis() - start) < timeout) {
                    try {
                            proc.exitValue();
                            Thread.sleep(2000);
                    } catch (Exception e) {

                    }
            }

            proc.destroy();
                    code = -1;              */
        }

        end = System.currentTimeMillis();

        inbr.close();
        errbr.close();

        DriverScriptExecutionResult r = new DriverScriptExecutionResult();
        r.code = code;
        r.command = executedCommand;
        r.stdout = inb.toString();
        r.stderr = eb.toString();
        r.startTime = start;
        r.stopTime = end;

        if (log && logger.isDebugEnabled()) {
            logger.debug("Exit code: " + r.code);
            logger.debug("Stdout: " + r.stdout);
            logger.debug("Stderr: " + r.stderr);
            logger.debug("ScriptName#" + executedCommand + "#Time#" + (end - start));
        }

        return r;
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public void setLog(boolean log)
    {
        this.log = log;
    }
}