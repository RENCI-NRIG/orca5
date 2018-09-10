/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.boot;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.exogeni.orca.boot.beans.Configuration;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.api.IConfigurationLoader;

import org.apache.log4j.Logger;

public class ConfigurationLoader implements IConfigurationLoader {
    /**
     * Path to configuration file.
     */
    protected String path;
    /**
     * Configuration file contents.
     */
    protected byte[] bytes;
    /**
     * Input stream to configuration file.
     */
    protected InputStream stream;
    /**
     * The configuration.
     */
    protected Configuration config;
    /**
     * The logger.
     */
    protected Logger logger = Globals.getLogger(this.getClass().getCanonicalName());

    public ConfigurationLoader() {
    }

    public ConfigurationLoader(byte[] bytes) {
        this.bytes = bytes;
    }

    public ConfigurationLoader(InputStream stream) {
        this.stream = stream;
    }

    public ConfigurationLoader(String path) {
        this.path = path;
    }

    public void process() throws ConfigurationException {
        readConfiguration();

        // process the configuration file
        ConfigurationProcessor init = new ConfigurationProcessor(config);
        init.process();

        // if (!add) {
        // ContainerManager.getInstance().setInitialized(true);
        // ContainerManager.getInstance().persistConfiguration();
        // } else {
        // // we may need to to an updateActor for each newly added actor
        // }
        //
        // // actor defaults should be added by the container when doing
        // addActor
    }

    protected void readConfiguration() throws ConfigurationException {
        try {
            if (path != null) {
                config = readConfiguration(path);
            } else if (bytes != null) {
                config = readConfiguration(bytes);
            } else if (stream != null) {
                config = readConfiguration(stream);
            } else {
                throw new ConfigurationException("No data source has been specified");
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Configuration file could not be found", e);
        } catch (JAXBException e) {
            throw new ConfigurationException("An error occurred while reading configuration", e);
        } catch (ConfigurationException e) {
            throw e;
        }
    }

    /**
     * Reads the configuration from the given byte stream
     * 
     * @param configBytes
     * @throws Exception
     */
    private Configuration readConfiguration(byte[] configBytes) throws JAXBException {
        logger.info("Reading configuration. Source: bytearray");
        InputStream is = new ByteArrayInputStream(configBytes);
        JAXBContext context = JAXBContext.newInstance("orca.boot.beans");
        Unmarshaller um = context.createUnmarshaller();
        return (Configuration) um.unmarshal(is);
    }

    /**
     * Reads the specified configuration file
     * 
     * @throws Exception
     */
    private Configuration readConfiguration(InputStream is) throws JAXBException {
        logger.info("Reading configuration: Source: stream");
        JAXBContext context = JAXBContext.newInstance("orca.boot.beans");
        Unmarshaller um = context.createUnmarshaller();
        return (Configuration) um.unmarshal(is);
    }

    /**
     * Reads the specified configuration file
     * 
     * @throws Exception
     */
    private Configuration readConfiguration(String configFile) throws JAXBException, FileNotFoundException {
        logger.info("Reading configuration: file = " + configFile);
        InputStream is = new FileInputStream(configFile);
        JAXBContext context = JAXBContext.newInstance("orca.boot.beans");
        Unmarshaller um = context.createUnmarshaller();
        return (Configuration) um.unmarshal(is);
    }

    public void setConfiguration(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setConfiguration(InputStream is) {
        this.stream = is;
    }

    public void setConfiguration(String path) {
        this.path = path;
    }
}
