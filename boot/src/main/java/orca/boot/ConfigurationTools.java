/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.boot;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Iterator;
import java.util.Properties;

import orca.boot.beans.CryptoKey;
import orca.boot.beans.Instance;
import orca.boot.beans.Parameter;
import orca.boot.beans.Rdata;
import orca.boot.beans.Rset;
import orca.boot.beans.SimpleParameter;
import orca.boot.beans.SimpleParameters;
import orca.shirako.api.IObjectFactory;
import orca.shirako.container.Globals;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.util.ResourceData;
import orca.util.ResourceType;

/**
 * A class with utility functions for processing configuration files
 * 
 * @author aydan
 */
public class ConfigurationTools {
    public static final String URLSeparator = ":";

    public static void attachConfigurationProperties(Object obj, Properties p) throws Exception {
        callMethod(obj, "configure", p.getClass(), p);
    }

    /**
     * Sets the specified property of the given object.
     * 
     * @param object
     *            The object
     * @param param
     *            Parameter desciption
     * @throws Exception in case of error
     */
    public static void attachParameter(Object object, Parameter param) throws Exception {
        Class<?> cl = null;

        Object obj = getObject(param);

        if (param.getBase() != null) {
            cl = Class.forName(param.getBase());
        } else {
            cl = obj.getClass();
        }

        callMethod(object, "set" + param.getName(), cl, obj);
    }

    /**
     * Calls a method with no parameters
     * 
     * @param object
     *            The object to make the call on
     * @param methodName
     *            The method name
     * @return Object
     * @throws Exception in case of error
     */
    public static Object callMethod(Object object, String methodName) throws Exception {
        // create the argument array
        Class<?>[] arguments = new Class[] {};

        // get the method
        Method method = object.getClass().getMethod(methodName, arguments);

        // call it
        return method.invoke(object, (Object[]) arguments);
    }

    /**
     * Calls a method on the specified object
     * 
     * @param object
     *            The object
     * @param methodName
     *            The name of the method to be called
     * @param argumentClass
     *            The class of the argument
     * @param argument
     *            The argument
     * @throws Exception in case of error
     * @return Object
     */
    public static Object callMethod(Object object, String methodName, Class<?> argumentClass, Object argument)
            throws Exception {
        Globals.Log.debug("Calling method: " + object.getClass().getCanonicalName() + "." + methodName);
        // create the argument array
        Class<?>[] arguments = new Class[] { argumentClass };

        // get the method
        Method method = object.getClass().getMethod(methodName, arguments);

        // call it
        return method.invoke(object, new Object[] { argument });
    }

    /**
     * Calls a method on the specified object that takes a single parameter
     * 
     * @param object
     *            The object
     * @param methodName
     *            The name of the method to be called
     * @param argumentClass
     *            The name of the class of the argument
     * @param argument
     *            The argument
     * @throws Exception in case of error
     * @return Object
     */
    public static Object callMethod(Object object, String methodName, String argumentClass, Object argument)
            throws Exception {
        return callMethod(object, methodName, Class.forName(argumentClass), argument);
    }

    /**
     * Creates an object instance and sets up its custom parameters
     * 
     * @param inst inst
     * @return object
     * @throws Exception in case of error
     */
    public static Object createInstance(Instance inst) throws Exception {
        Class<?> objectClass = Class.forName(inst.getClassName());
        Object result = objectClass.newInstance();

        if (inst.getType() != null) {
            // this is an object factory
            IObjectFactory fac = (IObjectFactory) result;
            Properties p = null;

            if (inst.getProperties() != null) {
                p = getProperties(inst.getProperties());
            } else {
                String path = getURLPath(inst.getInput());
                p = loadProperties(path);
            }

            result = fac.newInstance(p);
        } else {
            if (inst.getProperties() != null) {
                // this is an object taking a properties list with configuration
                // parameters
                attachConfigurationProperties(result, getProperties(inst.getProperties()));
            }
        }

        // call the specified properties methods
        if (inst.getParameters() != null) {
            Iterator<?> iter = inst.getParameters().getParameter().iterator();

            while (iter.hasNext()) {
                attachParameter(result, (Parameter) iter.next());
            }
        }

        return result;
    }

    public static Object createInstance(String className) throws Exception {
        Class<?> objectClass = Class.forName(className);

        return objectClass.newInstance();
    }

    /**
     * Decodes a private key
     * 
     * @param data
     *            The CryptoKey bean
     * @return PrivateKey
     * @throws Exception in case of error
     */
    public static PrivateKey decodePrivateKey(CryptoKey data) throws Exception {
        if (data == null) {
            return null;
        }

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(data.getValue());
        KeyFactory keyFactory = KeyFactory.getInstance(data.getAlgorithm());

        return keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * Decodes a public key
     * 
     * @param data
     *            The CryptoKey bean
     * @return PublicKey
     * @throws Exception in case of error
     */
    public static PublicKey decodePublicKey(CryptoKey data) throws Exception {
        if (data == null) {
            return null;
        }

        X509EncodedKeySpec spec = new X509EncodedKeySpec(data.getValue());

        KeyFactory kf = KeyFactory.getInstance(data.getAlgorithm());

        return kf.generatePublic(spec);
    }

    /**
     * Extracts an object from a Parameter bean
     * 
     * @param param
     *            The parameter bean
     * @return The object
     * @throws Exception in case of error
     */
    public static Object getObject(Parameter param) throws Exception {
        String type = param.getType();

        if (type.equals("string")) {
            return param.getValue();
        }

        if (type.equals("integer")) {
            return Integer.valueOf(param.getValue());
        }

        if (type.equals("long")) {
            return Long.valueOf(param.getValue());
        }

        if (type.equals("boolean")) {
            return Boolean.valueOf(param.getValue());
        }

        if (type.equals("instance")) {
            return createInstance(param.getInstance());
        }

        throw new Exception("Unsupported parameter type!");
    }

    public static java.util.Properties getProperties(orca.boot.beans.Properties beanProperties) {
        java.util.Properties properties = new java.util.Properties();

        if (beanProperties != null) {
            Object[] beanPropertyArray = beanProperties.getProperty().toArray();

            if (beanPropertyArray != null) {
                for (int i = 0; i < beanPropertyArray.length; i++) {
                    orca.boot.beans.Property prop = (orca.boot.beans.Property) beanPropertyArray[i];
                    properties.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        return properties;
    }

    /**
     * Converts a boot.beans.SimpleParameters to java.util.Properties
     * 
     * @param param
     *            The SimpleParameters bean
     * @return Properties
     */
    public static Properties getProperties(SimpleParameters param) {
        Properties properties = new Properties();

        if (param != null) {
            if (param.getParameter() != null) {
                Iterator<?> iter = param.getParameter().iterator();

                while (iter.hasNext()) {
                    SimpleParameter p = (SimpleParameter) iter.next();
                    properties.setProperty(p.getName(), p.getValue());
                }
            }
        }

        return properties;
    }

    /**
     * Converts a boot.beans.Rdata to slices.ResourceData
     * 
     * @param beanRData beanRData
     * @return ResourceData
     */
    public static ResourceData getResourceData(Rdata beanRData) {
        ResourceData rdata = new ResourceData();

        if (beanRData != null) {
            // local
            SimpleParameters p = beanRData.getLocalProperties();

            if (p != null) {
                ResourceData.mergeProperties(getProperties(p), rdata.getLocalProperties());
            }

            // request
            p = beanRData.getRequestProperties();

            if (p != null) {
                ResourceData.mergeProperties(getProperties(p), rdata.getRequestProperties());
            }

            // resource
            p = beanRData.getResourceProperties();

            if (p != null) {
                ResourceData.mergeProperties(getProperties(p), rdata.getResourceProperties());

            }

            // configuration
            p = beanRData.getConfigurationProperties();

            if (p != null) {
                ResourceData.mergeProperties(getProperties(p), rdata.getConfigurationProperties());
            }
        }

        return rdata;
    }

    /**
     * Converts a boot.beans.Rset to slices.ResourceSet
     * 
     * @param rset
     *            The Rset bean
     * @return ResourceSet
     */
    public static ResourceSet getResourceSet(Rset rset) {
        return new ResourceSet(rset.getUnits(), new ResourceType(rset.getType()),
                getResourceData(rset.getResourceData()));
    }

    public static String getURLPath(String path) throws Exception {
        String result = path;
        int index = path.indexOf(URLSeparator);

        if (path.charAt(index + 1) != '/') {
            result = path.substring(0, index + 1) + Globals.HomeDirectory + path.substring(index + 1);
        }

        return result;
    }

    /**
     * Loads data from a properties file
     * 
     * @param location
     *            The file path
     * @return Properties
     * @throws Exception in case of error
     */
    public static Properties loadProperties(String location) throws Exception {
        URL url = new URL(location);
        InputStream is = url.openStream();

        Properties p = new Properties();
        p.load(is);
        is.close();

        return p;
    }

    // public static Certificate decodeCertificate(byte[] certificate) throws Exception
    // {
    // CertificateFactory factory = CertificateFactory.getInstance("X.509");
    // ByteArrayInputStream is = new ByteArrayInputStream(certificate);
    // Certificate cert = factory.generateCertificate(is);
    // is.close();
    // return cert;
    // }
}
