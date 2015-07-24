/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import orca.drivers.DriverExitCodes;

import org.apache.log4j.Logger;


public class NumericIDGenerator
{
    protected int idCount = 1;

    /**
     * Map of VM names to a unique number
     */
    protected static Properties idMap;

    /**
     * Location for storing the map file.
     */
    protected String path;

    /**
     * The logger.
     */
    protected Logger logger;

    /**
     * Creates a new instance of the generator.
     * @param path location for storing the map file
     * @param logger logger to use
     */
    public NumericIDGenerator(String path, Logger logger)
    {
        if ((path == null) || (logger == null)) {
            throw new IllegalArgumentException();
        }

        this.path = path;
        this.logger = logger;
        idMap = new Properties();
    }

    public void initialize() throws Exception
    {
        readMapFromDisk();
    }

    /**
     * Generates a new numeric identifier.
     * @param key key
     * @return 0 - success, negative number - error
     */
    public int generateIdentifier(String key)
    {
        int code = 0;

        if (key == null) {
            code = DriverExitCodes.InvalidArguments;
        } else {
            synchronized (idMap) {
                String idString = idMap.getProperty(key);

                if (idString == null) {
                    idCount++;
                    idMap.put(key, Integer.toString(idCount));
                }

                try {
                    writeMapToDisk();
                } catch (Exception e) {
                    logger.error("generateIdentifier", e);
                    code = DriverExitCodes.InternalError;
                }
            }
        }

        return code;
    }

    /**
     * Releases the identifier corresponding to the key.
     * @param key key
     * @return 0 - success, negative number - an error
     */
    public int releaseIdentifier(String key)
    {
        int code = 0;

        if (key == null) {
            code = DriverExitCodes.InvalidArguments;
        } else {
            synchronized (idMap) {
                idMap.remove(key);

                try {
                    writeMapToDisk();
                } catch (Exception e) {
                    logger.error("releaseIdentifier", e);
                    code = DriverExitCodes.InternalError;
                }
            }
        }

        return code;
    }

    /**
     * Returns the numeric identifier that corresponds to the given key or null
     * if no identifier has been generated or some error occurs while retrieving
     * the identifier.
     * @param key key
     * @return numeric identifier, can be null
     */
    public Integer getIdentifier(String key)
    {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        Integer result = null;

        synchronized (idMap) {
            String temp = idMap.getProperty(key);

            if (temp != null) {
                try {
                    result = Integer.parseInt(temp);
                } catch (Exception e) {
                    logger.error("getIdentifier", e);
                    result = null;
                }
            }
        }

        return result;
    }

    /*
     * Called with the idMap lock
     */
    private void writeMapToDisk() throws Exception
    {
        if ((idMap != null) && (idMap.size() > 0)) {
            FileOutputStream stream = new FileOutputStream(path);
            idMap.store(stream, null);
            stream.close();
        }
    }

    private void readMapFromDisk() throws Exception
    {
        File temp = new File(path);

        if (temp.exists()) {
            FileInputStream stream = new FileInputStream(temp);
            idMap.load(stream);
            stream.close();
        }

        for (Object s : idMap.values()) {
            Integer i = Integer.parseInt((String) s);

            if (i > idCount) {
                idCount = i;
            }
        }
    }
}