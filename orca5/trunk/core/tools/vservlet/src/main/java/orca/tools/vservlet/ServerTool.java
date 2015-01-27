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

import org.apache.velocity.tools.view.tools.ViewTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;


/**
 * ServerTool is an application-scoped tool for use in Velocity web
 * applications: it initializes the Java, establishes root data structures, and
 * makes them available to Velocity templates (VTL).
 * <p>
 * ServerTool instantiates a singleton {@link ServerRoot} object, an instance of
 * a ServerRoot subclass specified for the web application in
 * vservlet.properties. The root object is available to the VTL as $server.Root,
 * assigned to $root in the standard prologue.vm.
 */
public class ServerTool implements ViewTool
{
    /**
     * The key vservlet.properties for a Web application deployment
     * initialization parameter (specified in web.xml) that names the properties
     * file to initialize vservlets. Due to a current Velocity bug, the
     * parameter is ignored and the default is always used: put your
     * initialization properties in /WEB-INF/vservlet.properties.
     */
    protected static final String INIT_PROPS_KEY = "vservlet.properties";

    /**
     * Property serverRootClass: the class name for the Web application's
     * {@link ServerRoot} object. Default is "vservlet.SampleServerRoot".
     */
    protected static final String SERVER_ROOT_KEY = "serverRootClass";
    protected static final String DEFAULT_SERVER_ROOT = "vservlet.SampleServerRoot";

    /**
     * Property serverRootInitParam: the name of the properties file for the
     * {@link ServerRoot} object (optional).
     */
    protected static final String SERVER_ROOT_INIT_KEY = "serverRootInitParam";
    protected ServletContext context;
    protected boolean debug;
    protected String propsFile;
    protected Properties properties;
    protected Properties rproperties; // debug
    protected ServerRoot root;
    protected boolean initialized;

    public ServerTool()
    {
        context = null;
        debug = false;

        propsFile = null;
        properties = new Properties();
        rproperties = null;

        root = null;
        initialized = false;
    }

    /**
     * Initializes this tool. The Velocity container creates one instance of
     * ServerTool per Web application, and passes the ServletContext to init.
     * <p>
     * Problem: a Velocity tool "cannot fail" to initialize. No exceptions. So
     * we delay initialization of the Web application to first use, and throw an
     * exception into the template interpreter if it fails. In this way the
     * error is reported to the Web application user.
     * <p>
     * Problem: the current version of Velocity/View/Layout does not seem to
     * pass init parameters correctly in the context. All we need is the name of
     * the vservlet.properties file...so we hardwire it.
     * @param o allegedly the ServletContext of the first request.
     */
    public void init(Object o)
    {
        context = (ServletContext) o;
    }

    /**
     * Initializes this tool and Web application. Called when the Web
     * application VTL first accesses its root object.
     * @throws Exception if the application fails to initialize.
     */
    protected void initialize() throws Exception
    {
        if (!initialized) {
            loadProperties();
            loadRoot();
            initialized = true;
        }
    }

    /**
     * Returns the root object for this Web service, initializing the Web
     * service if necessary. VTL: $server.Root
     * @return the root object
     */
    public ServerRoot getRoot() throws Exception
    {
        if (!initialized) {
            initialize();
        }

        return root;
    }

    /**
     * Returns the ServletContext passed to this tool by Velocity. It is also
     * available to the VTL as $application.
     * @return the ServletContext for this Web service
     */
    public ServletContext getContext()
    {
        return context;
    }

    /**
     * Sets the debug flag for this Web service. If it is true, the VTL should
     * output diagnostic info.
     * @param flag debug flag
     */
    public void setDebug(boolean flag)
    {
        debug = flag;
    }

    /**
     * Gets the debug flag for this Web service. If it is true, the VTL should
     * output diagnostic info. VTL: $server.debug
     * @return debug flag
     */
    public boolean getDebug()
    {
        return debug;
    }

    /**
     * Returns the properties of this ServerTool. There's not much to see here,
     * but it's useful for debug.
     * @return ServerTool initialization properties
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * Loads the properties for this ServerTool from the property resource path
     * (e.g., vservlet.properties) specified in the web.xml deployment
     * descriptor.
     * @throws Exception if file is missing, unreadable, or malformed
     */
    protected void loadProperties() throws Exception
    {
        propsFile = context.getInitParameter(INIT_PROPS_KEY);

        /*
         * Workaround for Velocity bug: see comment at init() above.
         */
        if (propsFile == null) {
            propsFile = "/WEB-INF/vservlet.properties";
        }

        properties = loadPropertyResource(propsFile, "vservlet");
    }

    /**
     * Instantiates the {@link ServerRoot} object for this Web application, and
     * initializes it from its properties file.
     * @throws ClassNotFoundException if ServerRoot class does not load
     * @throws InstantiationException if ServerRoot class does not instantiate
     * @throws Exception if properties file is missing, unreadable, or malformed
     * @throws Exception if root object fails to initialize
     */
    protected void loadRoot() throws Exception
    {
        String rclass = DEFAULT_SERVER_ROOT;
        String rpath = null;
        String serr;

        /*
         * The properties should include the class name and properties file path
         * for the Web application's root object.
         */
        rpath = properties.getProperty(SERVER_ROOT_INIT_KEY, rpath);
        rclass = properties.getProperty(SERVER_ROOT_KEY, rclass);

        if (rclass == null) {
            serr = "No vservlet root class specified in application properties ";

            if (propsFile != null) {
                serr = serr + propsFile;
            }

            throw new Exception(serr);
        }

        /*
         * Instantiate the ServerRoot object using the specified class (or the
         * default).
         */
        Class c = Class.forName(rclass);
        root = (ServerRoot) c.newInstance();

        /*
         * Initialize the ServerRoot object with its properties, if any.
         */
        Properties rprop = loadPropertyResource(rpath, "vservlet Web application");
        rproperties = rprop;
        root.init(rprop, this);
    }

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
        String serr;
        InputStream stream = context.getResourceAsStream(path);

        if (stream == null) {
            serr = "Cannot find " + descr + " properties file: ";
            throw new Exception(serr + path);
        }

        return stream;
    }

    /**
     * Loads a properties file named as a Web application resource path, and
     * throws generic, descriptive exceptions for error handling.
     * @param relative pathname of the resource
     * @param brief description of resource for error messages
     * @throws Exception if file is missing, unreadable, or malformed
     */
    protected Properties loadPropertyResource(String path, String descr) throws Exception
    {
        Properties p = new Properties();
        String serr;

        if (path == null) {
            return p;
        }

        InputStream stream = getResourceAsStream(path, descr);

        try {
            p.load(stream);
        } catch (IOException ioe) {
            serr = "The " + descr + " properties file is unreadable: ";
            throw new Exception(serr + path);
        }

        return p;
    }

    /**
     * Returns a string of useful information about this ServerTool properties
     * and context for debugging.
     * @return string with some diagnostics
     */
    public String showProperties()
    {
        String s = "Server properties: ";

        s = s + "But first the context properties: ";

        Enumeration e = context.getInitParameterNames();

        while (e.hasMoreElements()) {
            String param = (String) e.nextElement();
            s = s + param;
        }

        s = s + "\n\n" + "And now to our regular programming: ";

        if (propsFile == null) {
            s = s + "no properties file specified!\n";
        } else {
            s = s + "properties file " + propsFile + "\n";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        properties.list(pw);

        s = s + sw.toString();

        return s;
    }
}