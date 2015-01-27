/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.nodeagenthost;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;

import java.io.File;

/**
 * A simple HTTP server that can host axis2 web services.
 * @author aydan
 */
public class Server {
    public static void printUsage() {
        System.err.println("Usage: Server <repository> <port>");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            printUsage();
            return;
        }

        try {
            ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(args[0], args[0] + "/conf/axis2.xml");
            SimpleHTTPServer server = new SimpleHTTPServer(context, Integer.parseInt(args[1]));
            server.start();

            while (!server.isRunning()) {
                Thread.sleep(500);
                System.out.println("Waiting for server to start:" + server.isRunning());
            }

            System.out.println("HTTP server is running.  Daemonizing.");
            daemonize();
        } catch (Exception e) {
            System.err.println("Starting the Shirako node manager failed.");
            e.printStackTrace();
        }
    }

    static private void daemonize() throws Exception {
        /* Create a file and set the file to be deleted on exit */
        File f = getPidFile();
        if (f != null) {
            f.deleteOnExit();
        }
        /* close all input and output */
        System.out.close();
        System.err.close();
        System.in.close();
    }

    static private File getPidFile() {
        String path = System.getProperty("daemon.pidfile");
        if (path != null) {
            File pidFile = new File(path);
            return pidFile;
        }
        return null;
    }
}
