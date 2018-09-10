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

import java.io.File;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;


/**
 * Utility class to elements to the classpath at runtime.
 */
public class ChangeClasspath
{
    /**
     * Adds a path to the specified class loader.
     *
     * @param loader class loader to add the path to
     * @param file file representing the path
     *
     * @throws Exception in case of error
     */
    public static void addFile(final ClassLoader loader, final File file) throws Exception
    {
        addURL(loader, file.toURL());
    }

    /**
     * Adds a path the current class loader.
     *
     * @param file file representing the path
     *
     * @throws Exception in case of error
     */
    public static void addFile(final File file) throws Exception
    {
        addURL(ClassLoader.getSystemClassLoader(), file.toURL());
    }

    /**
     * Adds a path to the current class loader.
     *
     * @param path path to add
     *
     * @throws Exception in case of error
     */
    public static void addFile(final String path) throws Exception
    {
        File f = new File(path);
        addFile(f);
    }

    /**
     * Adds a URL to the given class loader.
     *
     * @param loader class loader
     * @param url url to add
     *
     * @throws RuntimeException in case of error
     */
    public static void addURL(final ClassLoader loader, final URL url) throws RuntimeException
    {
        Class<?> sysclass = URLClassLoader.class;
        Class<?>[] parameters = new Class[] { URL.class };

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(loader, new Object[] { url });
        } catch (Exception e) {
            throw new RuntimeException("Could not add the URL to the classloader", e);
        }
    }

    /**
         * Empty constructor.
         */
    protected ChangeClasspath()
    {
    }
}
