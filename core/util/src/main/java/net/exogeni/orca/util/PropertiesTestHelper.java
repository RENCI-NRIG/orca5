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
import java.io.FileInputStream;

import java.util.Enumeration;
import java.util.Properties;


/**
 * PropertiesTestHelper makes it easier to process test configuration
 * properties.
 */
public class PropertiesTestHelper
{
    public static final String PathTestProperties = "ant/tests.properties";
    public static final String PathTestUserProperties = "ant/user.tests.properties";
    
    // private properties are intended to store e.g. substrate logins
    // they are optional.
    private String pathPrivateProperties = null;

    /**
     * Property name: location of the node agent service.
     */
    public static final String PropertyServiceLocation = "service.location";

    /**
     * Property name: path to axis2 repository.
     */
    public static final String PropertyAxis2Repository = "axis2.repository";

    /**
     * Property name: path to axis2.xml.
     */
    public static final String PropertyAxis2Xml = "axis2.xml";
    public static final String PropertyAxis2SecureXml = "axis2.secure.xml";
    public static final String PropertyAxis2NotSecureXml = "axis2.notsecure.xml";
    public static final String PropertyAxis2Dir = "axis2.dir";
    protected String axis2Dir;
    protected String location;
    protected String repository;
    protected String config;
    protected String secureConfig;
    protected String notSecureConfig;
    protected Properties properties, privateProperties;
    protected boolean done = false;

    public PropertiesTestHelper()
    {
    }

    public PropertiesTestHelper(String privatePropertiesPath) {
    	pathPrivateProperties = privatePropertiesPath;
    }
    
    protected synchronized void checkDone()
    {
        if (!done) {
            throw new IllegalStateException("you must call process() first");
        }
    }

    /**
     * Performs some replacements and expansions.
     *
     * @param p properties list
     */
    protected void expand(Properties p)
    {
        Enumeration<?> e = p.propertyNames();

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            p.setProperty(name, p.getProperty(name).replace("${basedir}", "."));
        }

        e = p.propertyNames();

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            p.setProperty(name, expand(p, name));
        }
    }

    /**
     * Expands recursively the given key. Note: does not handle
     * infinite recursion.
     *
     * @param p properties list
     * @param key key
     *
     * @return expanded key.
     */
    protected String expand(Properties p, String key)
    {
        String result = p.getProperty(key);

        if (result == null) {
            return "${" + key + "}";
        }

        int index = 0;

        while (true) {
            int start = result.indexOf("${", index);

            if (start == -1) {
                break;
            }

            int end = result.indexOf("}", index);

            if (end == -1) {
                break;
            }

            String exp = expand(p, result.substring(start + 2, end));
            index = start + exp.length();

            String tmp = result;
            result = result.substring(0, start) + exp;

            if (tmp.length() > (end + 1)) {
                result = result + tmp.substring(end + 1, tmp.length());
            }
        }

        return result;
    }

    public String getConfig()
    {
        checkDone();

        return config;
    }

    public String getLocation()
    {
        checkDone();

        return location;
    }

    public String getNotSecureConfig()
    {
        return notSecureConfig;
    }

    public Properties getProperties()
    {
        checkDone();

        return properties;
    }

    public Properties getPrivateProperties()
    {
        checkDone();

        return privateProperties;
    }
    
    public String getRepository()
    {
        checkDone();

        return repository;
    }

    public String getSecureConfig()
    {
        return secureConfig;
    }

    public synchronized void process()
    {
        if (!done) {
            properties = readTestProperties();
            expand(properties);
            if (privateProperties != null)
            	expand(privateProperties);
            location = properties.getProperty(PropertyServiceLocation);
            repository = properties.getProperty(PropertyAxis2Repository);
            config = properties.getProperty(PropertyAxis2Xml);

            secureConfig = properties.getProperty(PropertyAxis2SecureXml);
            notSecureConfig = properties.getProperty(PropertyAxis2NotSecureXml);

            try {
                axis2Dir = properties.getProperty(PropertyAxis2Dir);

                if (axis2Dir != null) {
                    ChangeClasspath.addFile(new File(axis2Dir));
                }
            } catch (Exception e) {
                System.out.println("Failed to change the classpath");
            }

            done = true;
        }
    }

    protected Properties readTestProperties()
    {
        try {
            File f = new File(PathTestProperties);

            if (!f.exists()) {
                throw new Exception(PathTestProperties + " does not exist");
            }

            FileInputStream is = new FileInputStream(f);
            Properties p = new Properties();
            p.load(is);
            is.close();
            f = new File(PathTestUserProperties);

            if (f.exists()) {
                is = new FileInputStream(f);

                Properties puser = new Properties();
                puser.load(is);
                is.close();
                PropList.mergeProperties(puser, p);
            }

            // read private properties if available
            if (pathPrivateProperties != null) {
            	f = new File(pathPrivateProperties);
            	if (f.exists()) {
            		is = new FileInputStream(f);
            		privateProperties = new Properties();
            		privateProperties.load(is);
            		is.close();
            	}
            }
            
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
