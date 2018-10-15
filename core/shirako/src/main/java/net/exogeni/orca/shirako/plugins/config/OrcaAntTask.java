/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.plugins.config;

import java.io.File;
import java.util.Properties;

import net.exogeni.orca.util.ChangeClasspath;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

public class OrcaAntTask extends Task {
    public static final int InternalTaskError = -123456;
    public static final String PropertyRootPath = "root.dir";
    public static final String PropertyTestMode = "test.mode";

    /**
     * Name of the property under which to store the exit code.
     */
    protected String exitCodeProperty;

    /**
     * Name of the property under which to store the exit message.
     */
    protected String exitCodeMessageProperty;

    /**
     * Classpath to use
     */
    protected Path classpath;

    /**
     * Timeout for operations
     */
    protected int timeout;

    /**
     * Name of the loader to use
     */
    protected String loaderref;
    protected Logger logger;

    public OrcaAntTask() {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    @Override
    public void execute() throws BuildException {
        if (getProject().getProperty(PropertyTestMode) != null) {
            try {
                File f = new File("config/log4j.properties");
                if (f.exists()){
                    PropertyConfigurator.configure("config/log4j.properties");
                }
            } catch (Exception e) {
                System.err.println("Could not load config/log4.properties");
            }
        }

        super.execute();
        try {
            fixClassPath();
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            logger.error("", e);
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a path to the classpath.
     * @return a class path to be configured
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }

        return classpath.createPath();
    }

    protected void fixClassPath() throws Exception {
        if (loaderref != null) {
            Object obj = getProject().getReference(loaderref);

            if (obj != null) {
                System.out.println(obj.getClass().getCanonicalName());

                if (obj instanceof ClassLoader) {
                    Thread.currentThread().setContextClassLoader((ClassLoader) obj);
                }
            }
        }

        if (classpath != null) {
            String[] list = classpath.list();

            for (int i = 0; i < list.length; i++) {
                File f = new File(list[i]);

                if (f.exists()) {
                    ChangeClasspath.addFile(getProject().getClass().getClassLoader(), f);
                }
            }
        }
    }

    protected void setResult(int code) {
        if (exitCodeProperty != null) {
            getProject().setProperty(exitCodeProperty, Integer.toString(code));
            getProject().setProperty(exitCodeMessageProperty, getErrorMessage(code));
        } else {
            if (code != 0) {
                throw new RuntimeException("An error has occurred. Error code: " + code);
            }
        }
    }

    protected String getErrorMessage(int code) {
        return "";
    }

    protected void setExitCode(int code) throws Exception {
        if (exitCodeProperty != null) {
            getProject().setProperty(exitCodeProperty, Integer.toString(code));
        } else {
            if (code != 0) {
                throw new Exception("An error has occurred. Error code: " + code);
            }
        }
    }

    protected Properties getProperties() throws Exception {
        return null;
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @param r a reference to a classpath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Gets the classpath to be used for this compilation.
     * @return the class path
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * @param exitCodeProperty the exitCodePropert to set
     */
    public void setExitCodeProperty(String exitCodeProperty) {
        this.exitCodeProperty = exitCodeProperty;
        this.exitCodeMessageProperty = exitCodeProperty + ".message";
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @param loaderref the loaderref to set
     */
    public void setLoaderref(String loaderref) {
        this.loaderref = loaderref;
    }

    /**
     * Set the classpath to be used for this compilation.
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }
}
