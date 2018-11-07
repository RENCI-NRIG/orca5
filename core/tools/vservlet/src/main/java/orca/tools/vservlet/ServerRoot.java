/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.tools.vservlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;


/**
 * ServerRoot is a singleton that initializes the Web application and supplies
 * methods for use in the Velocity VTL code. The ServerTool instantiates a
 * singleton instance of this class (or a subclass) for each vservlet Web
 * application, passing the property list from its initialization properties
 * file. This version includes rudimentary support for identity management and
 * sending e-mail from the service.
 * <p>
 * Web applications should extend this class. The ServerTool subclass to be
 * instantiated, and a property file to pass to it, are specified in the
 * vservlet.properties. The ServerRoot object is accessible from the VTL as
 * $server.root (set to $root in standard prologue.vm).
 */
public class ServerRoot
{
    protected Properties properties;
    protected IdentitySet idset;
    protected ServerTool stool;
    protected Logger logger;

    /**
     * Property serviceName: service title, for title bars etc.
     */
    public static final String SERVER_TITLE_KEY = "serviceName";
    protected String name;

    /**
     * Property emailAdmin: service admin e-mail address (optional).
     */
    public static final String SERVER_EMAILADMIN_KEY = "emailAdmin";
    protected String emailAdmin;

    /**
     * Property SMTPserver: SMTP server host or IP, to send e-mail (optional).
     */
    public static final String SMTP_SERVER_KEY = "SMTPserver";
    protected String SMTPserver;

    /**
     * Property SMTPport: SMTP server port to send e-mail (optional).
     */
    public static final String SMTP_PORT_KEY = "SMTPport";
    protected String SMTPport;

    /**
     * Property emailReplyTo: return address for outgoing e-mail (optional).
     */
    public static final String SERVER_EMAIL_KEY = "emailReplyTo";
    protected String replyto;
    protected IPortalDatabase db;

    public ServerRoot()
    {
        logger = Logger.getLogger(this.getClass().getCanonicalName());
        properties = null;
        stool = null;
        name = "Sample Velocity servlet application";
        emailAdmin = null;

        SMTPserver = null;
        SMTPport = null;
        replyto = "nobody@nowhere.com";
        idset = null;
    }

    /**
     * The {@link ServerTool} calls this once to initialize the application.
     * @param p configuration properties
     * @throws Exception if the object fails to initialize (e.g., bad
     *             properties)
     */
    public void init(Properties p, ServerTool stool) throws Exception
    {
        this.properties = p;
        this.stool = stool;

        String title = p.getProperty(SERVER_TITLE_KEY);

        if (title != null) {
            name = title;
        }

        emailAdmin = p.getProperty(SERVER_EMAILADMIN_KEY);

        /*
         * Look for the SMTP properties, and enable e-mail transmission from
         * this application if they are defined.
         */
        SMTPserver = p.getProperty(SMTP_SERVER_KEY);
        SMTPport = p.getProperty(SMTP_PORT_KEY);
        replyto = p.getProperty(SERVER_EMAIL_KEY);
    }

    /**
     * Returns the name/title for the service, suitable for the VTL to place on
     * title bars, etc. VTL: $root.serviceName
     * @return service name suitable for printing
     */
    public String getServiceName()
    {
        return name;
    }

    /**
     * Returns the name/title for the service (just getServiceName).
     * @return service name suitable for printing
     */
    public String toString()
    {
        return name;
    }

    /**
     * Returns ServerRoot configuration property list. It is easy to look up
     * properties from VTL.
     * @return ServerRoot configuration properties
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * Get a date object for right now. To print it from VTL, toString works
     * pretty well (just say $root.now), but there's also a generic date tool
     * for Velocity if you want to change the formatting.
     * @return date representing current time
     */
    public Date getNow()
    {
        return new Date(System.currentTimeMillis());
    }

    /*
     * =======================================================================
     * Configuration management
     * =======================================================================
     */

    /**
     * Returns an open InputStream for a resource (file) specified with a
     * context-relative name, e.g., a path string appearing as a property value
     * in a properties file or deployment descriptor.
     * @param relative pathname of the resource
     * @param brief description of resource for error messages
     * @return a stream suitable for reading the resource
     * @throws Exception if file is missing or unreadable
     */
    public InputStream getResourceAsStream(String path, String descr) throws Exception
    {
        return stool.getResourceAsStream(path, descr);
    }

    /**
     * Utility method to convert a string value (e.g., from property file) into
     * an integer, with descriptive exception if problem.
     */
    public int stringToInteger(String str, String descr) throws Exception
    {
        try {
            Integer value = new Integer(str);

            return value.intValue();
        } catch (Exception e) {
            throw new Exception("String for " + descr + "(" + str + ") is not a valid integer");
        }
    }

    /**
     * Loads a properties file named as a Web application resource path, and
     * throws generic, descriptive exceptions for error handling.
     * @param relative pathname of the resource
     * @param brief description of resource for error messages
     * @throws Exception if file is missing, unreadable, or malformed
     */
    public Properties loadPropertyResource(String path, String descr) throws Exception
    {
        Properties p = new Properties();
        String serr;

        if (path == null) {
            return p;
        }

        InputStream stream = stool.getResourceAsStream(path, descr);

        try {
            p.load(stream);
        } catch (IOException ioe) {
            serr = "The " + descr + " properties file is unreadable: ";
            throw new Exception(serr + path);
        }

        return p;
    }

    /*
     * =======================================================================
     * Identity management
     * =======================================================================
     */

    /**
     * Returns the user {@link Identity} for the given request/session. The
     * identity defaults to {"nobody", "Anonymous User"}). VTL:
     * $root.identity($request)
     * @return the Identity object for request submitter
     * @throws Exception
     */
    public synchronized Identity getIdentity(HttpServletRequest request) throws Exception
    {
        Identity id = null;
        String username = null;

        if (idset == null) {
            // idset = new IdentitySet(this);
            throw new Exception("Missing idset");
        }

        /*
         * For container-managed security. This seems to be platform-dependent:
         * try to be robust. If we don't get it, default to nobody. secure =
         * request.isSecure();
         */
        if (id == null) {
            username = request.getRemoteUser();

            if (username != null) {
                id = idset.getIdentity(username);
            }
        }

        if (id == null) {
            /*
             * Doesn't resolve for some reason. username =
             * request.getUserPrincipal.getName(); if (username != null) id =
             * idset.getIdentity(username);
             */
        }

        if (id == null) {
            if (username != null) {
                id = idset.makeUnrecognized(username);
            } else {
                id = idset.makeAnonymous();
            }
        }

        return id;
    }

    /*
     * =======================================================================
     * Sending email from the service
     * =======================================================================
     */

    /**
     * Return email for admin for this service, if configured, else null. VTL:
     * $root.adminMail
     * @return e-mail for service administrator, or null
     */
    public String getAdminMail()
    {
        return emailAdmin;
    }

    /**
     * Rudimentary e-mail transmission. It would be nice if we knew how to
     * invoke Velocity (recursively) to create the message body from a template.
     * <p>
     * VTL: $root.sendMessage($recipient, $subject, $body)
     */
    public void sendMessage(String to, String subject, String message) throws Exception
    {
        Socket socket;
        InputStream in;
        OutputStream out;
        BufferedReader bin;
        PrintStream prout;

        int sport;
        String serr;

        serr = "The " + name + " cannot send mail: ";

        if ((SMTPserver == null) || (SMTPport == null)) {
            throw new Exception(serr + "no mail server is known");
        }

        /*
         * Could use propertyToInteger above. XXX
         */
        try {
            sport = Integer.parseInt(SMTPport);
        } catch (NumberFormatException nfe) {
            throw new Exception(
                serr +
                "the mail server was identified incorrectly: the SMTP server port property is not a valid integer");
        }

        String myserver = SMTPserver + ":" + sport;

        /*
         * This code is from Sara Sprenkle, modified to make it self-contained,
         * to parameterize it from the service properties, and to throw
         * exceptions into the template for detected error cases. Not all errors
         * are detected, e.g., she doesn't appear to be checking return codes
         * from the SMTP server.
         */
        String incoming = new String();
        String RCPTTO = "RCPT TO:<" + to + ">";
        String SUBJECT = "Subject: " + subject;
        String MAILFROM = "MAIL FROM:<" + replyto + ">";
        String DATA = "DATA";
        String CONTENT_ENCODING = "Content-Transfer-Encoding: 7bit";
        String CONTENT_TYPE = "Content-Type: text/html; charset=US-ASCII";
        String CONTENT_LENGTH = "Content-Length: ";
        String END = ".\r\n.\r\n";

        /*
         * SMTP returns either "220" or "250" to indicate everything went OK
         */
        final String OKCmd = "220|250";

        /* connect to the mail server */
        try {
            socket = new Socket(SMTPserver, sport);
        } catch (IOException e) {
            throw new Exception(serr + "cannot connect to " + myserver);
        }

        try {
            in = socket.getInputStream();
            bin = new BufferedReader(new InputStreamReader(in));
            out = socket.getOutputStream();
            prout = new PrintStream(out);
        } catch (IOException e) {
            throw new Exception(serr + "error connecting to " + myserver);
        }

        /*
         * OK, we're connected, let's be friendly and say hello to the mail
         * server...
         */
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String IPAdd = localHost.toString();
            int slashLoc = IPAdd.indexOf('/');
            IPAdd = IPAdd.substring(slashLoc + 1);
            prout.println("HELO " + IPAdd);
            prout.flush();
        } catch (Exception e) {
            /*
             * System.err.println("Problem with HELO protocol");
             * e.printStackTrace(); System.err.println("Keep going anyway.");
             */
        }

        try {
            incoming = bin.readLine();
        } catch (IOException e) {
            throw new Exception(serr + "error on socket read from " + myserver);
        }

        /* let server know YOU wanna send mail... */
        prout.println(MAILFROM);
        prout.flush();

        try {
            incoming = bin.readLine();
        } catch (IOException e) {
            throw new Exception(serr + "error on socket read from " + myserver);
        }

        // let server know WHOM you're gonna send mail to...
        prout.println(RCPTTO);
        prout.flush();

        try {
            incoming = bin.readLine();
        } catch (IOException e) {
            throw new Exception(serr + "error on socket read from " + myserver);
        }

        // let server know you're now gonna send the message contents...
        prout.println(DATA);
        prout.flush();

        try {
            incoming = bin.readLine();
        } catch (IOException e) {
            throw new Exception(serr + "error on socket read from " + myserver);
        }

        /* finally, send the message... */
        prout.println(SUBJECT);
        prout.println(CONTENT_ENCODING);
        prout.println(CONTENT_TYPE);
        prout.println(CONTENT_LENGTH + message.length());
        prout.println(message);
        prout.println(END);
        prout.flush();

        try {
            incoming = bin.readLine();
        } catch (IOException e) {
            throw new Exception(serr + "error on socket read from " + myserver);
        }

        /* we're done, disconnect from server */
        try {
            socket.close();
        } catch (IOException e) {
            // System.out.println("Error closing socket.");
        }
    }

    public Logger getLogger()
    {
        return logger;
    }

    public IPortalDatabase getDatabase()
    {
        return db;
    }
}