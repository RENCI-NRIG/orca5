/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class ScriptExecutor
{
    protected String command;
    protected String[] cmdArray;
    protected Logger logger;

    public ScriptExecutor(String command)
    {
        this.command = command;
    }

    public ScriptExecutor(String[] cmdArray)
    {
        this.cmdArray = cmdArray;
    }

    public ExecutionResult execute() throws Exception
    {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass().getCanonicalName());
        }

        Runtime rt = Runtime.getRuntime();

        if (logger.isDebugEnabled()) {
            if (command != null) {
                logger.debug("Command: " + command);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Command: ");
                sb.append(cmdArray[0]);
                sb.append("\n Arguments: ");

                for (int i = 1; i < cmdArray.length; i++) {
                    sb.append(cmdArray[i]);
                    sb.append(" ");
                }

                logger.debug(sb.toString());
            }
        }

        Process proc = null;

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
        }

        int code = proc.waitFor();
        inbr.close();
        errbr.close();

        ExecutionResult r = new ExecutionResult();
        r.code = code;
        r.stdout = inb.toString();
        r.stderr = eb.toString();

        logger.debug("Exit code: " + r.code);
        logger.debug("Stdout: " + r.stdout);
        logger.debug("Stderr: " + r.stderr);

        return r;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
}
