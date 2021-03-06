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
	public static class ReadStream implements Runnable {
	    String name;
	    InputStream is;
	    Thread thread;     
	    Logger log;
	    StringBuffer output;
	    
	    public ReadStream(String name, InputStream is, Logger log) {
	        this.name = name;
	        this.is = is;
	        this.log = log;
	        output = new StringBuffer();
	    }       
	    public void start () {
	        thread = new Thread (this);
	        thread.start ();
	    }       
	    public void run () {
	        try {
	            InputStreamReader isr = new InputStreamReader (is);
	            BufferedReader br = new BufferedReader (isr);   
	            while (true) {
	                String s = br.readLine();
	                if (s == null) break;
	                else 
	                	output.append(s + "\n");
	            }
	            is.close ();    
	        } catch (Exception ex) {
	        	log.error("Unable to read process stream in ScriptExecutor");
	        }
	    }
	    public String getOutput() {
	    	return output.toString();
	    }
	}
	
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

        ReadStream stdinRstr = new ReadStream("stdin", proc.getInputStream(), logger);
        ReadStream errRstr = new ReadStream("stderr", proc.getErrorStream(), logger);
        
        stdinRstr.start();
        errRstr.start();

        int code = proc.waitFor();
        //inbr.close();
        //errbr.close();

        ExecutionResult r = new ExecutionResult();
        r.code = code;
        r.stdout = stdinRstr.getOutput();
        r.stderr = errRstr.getOutput();

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
