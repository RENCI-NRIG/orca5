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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

import org.apache.log4j.Logger;


class DriverEntry
{
    protected DriverId id;
    protected String url;
    protected String className;
    protected Class driverClass;
    protected ClassLoader loader;
    protected IDriver driver;
    protected int refCounter = 0;
    protected Logger logger;

    /**
     * Creates a new <code>DriverEntry</code>
     * @param id Driver identifier
     * @param url path to the jar implementing the driver (optional)
     * @param className name of the driver class
     */
    public DriverEntry(DriverId id, String url, String className)
    {
        this.id = id;
        this.url = url;
        this.className = className;
        logger = Logger.getLogger(id.toString());
        resolve();
    }

    /**
     * Returns true if we managed to resolve the class implementing the driver
     * @return
     */
    public boolean isResolvable()
    {
        return (driverClass != null);
    }

    public DriverId getDriverId()
    {
        return id;
    }

    public String getUrl()
    {
        return url;
    }

    public String getClassName()
    {
        return className;
    }

    public synchronized IDriver getDriver(DriverFactory factory)
    {
        IDriver result = null;

        try {
            if (driver == null) {
                if (driverClass != null) {
                    result = (IDriver) driverClass.newInstance();
                    result.setFactory(factory);

                    int code = result.initialize();

                    if (code != 0) {
                        result = null;
                        logger.error("Driver " + id + " failed to initialize. Code: " + code);
                    }

                    if (result.isStateful()) {
                        driver = result;
                    }
                }
            } else {
                result = driver;
            }
        } catch (Exception e) {
            result = null;
            logger.error("getDriver", e);
        }

        if (result != null) {
            if (loader != null) {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }

        return result;
    }

    /**
     * Resolves the driver class
     */
    protected void resolve()
    {
        URL[] urls = null;

        /**
         * Add the driver jar if present
         */
        try {
            if (url != null) {
                File f = new File(url);

                if (f.exists() && f.isDirectory()) {
                    File[] files = f.listFiles();
                    Vector<URL> myurls = new Vector<URL>();

                    if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                            if (files[i].isFile()) {
                                URL url = files[i].toURL();
                                myurls.add(url);
                            }
                        }
                    }

                    urls = new URL[myurls.size()];
                    myurls.copyInto(urls);
                } else {
                    File jar = new File(url);
                    URL url = jar.toURL();
                    urls = new URL[] { url };
                }
            }
        } catch (Exception e) {
            logger.error("resolve first phase", e);
        }

        /**
         * Try to resolve the class
         */
        try {
            // Create a new class loader with the directory
            ClassLoader cl = null;

            if (urls != null) {
                cl = new URLClassLoader(urls, this.getClass().getClassLoader());
                loader = cl;
            } else {
                cl = this.getClass().getClassLoader();
            }

            driverClass = cl.loadClass(className);
        } catch (Exception e) {
            logger.error("resolve: second phase", e);
            System.err.println(e.getMessage());
        }
    }
}