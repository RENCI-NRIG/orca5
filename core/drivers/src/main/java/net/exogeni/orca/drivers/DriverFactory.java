/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.drivers;

import java.util.Hashtable;

import com.mysql.jdbc.StringUtils;
import org.apache.log4j.Logger;


public class DriverFactory
{
    public static int CodeInvalidArguments = -1001;
    public static int CodeCannotResolveDriverClass = -1002;
    public static int CodeDriverIsAlreadyInstalled = -1003;
    public static int CodeErrorCleaningUp = -1004;
    public static int CodeDriverIsNotInstalled = -1005;
    public static int CodeCannotCreateDirectory = -1006;
    public static int CodeCannotExpandPackage = -1007;

    /**
     * Name of the folder containing jars and files that must be added to a
     * driver's class loader.
     */
    public static final String LibFolderName = "lib";

    /**
     * Property specifying the driver identifier
     */
    public static final String PropertyDriverId = "driver.id";

    /**
     * Property specifying the action identifier
     */
    public static final String ProeprtyActionId = "driver.action.id";

    /**
     * Map of installed drivers
     */
    protected Hashtable<DriverId, DriverEntry> drivers;
    protected String driversRoot = ".";
    protected Logger logger;

    public DriverFactory()
    {
        this.drivers = new Hashtable<DriverId, DriverEntry>();
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    public Logger getLogger()
    {
        return logger;
    }

    public void setDriversRoot(String driversRoot)
    {
        this.driversRoot = driversRoot;
    }

    public String getDriversRoot()
    {
        return driversRoot;
    }

    public String getDriverRoot(IDriver driver)
    {
        return getDriverRoot(driver.getId());
    }

    public String getDriverRoot(DriverId id)
    {
        return driversRoot + "/" + id.toString();
    }

    /**
     * Installs the specified driver
     * @param id Unique driver identifier
     * @param className Name of the class implementing the driver. The class
     *            should be resolvable by the class loader
     * @return int
     */
    public int install(DriverId id, String className)
    {
        int result = 0;

        if ((id == null) || StringUtils.isNullOrEmpty(className)) {
            logger.warn("Invalid arguments to install driver");

            return CodeInvalidArguments;
        }

        logger.debug("Trying to install driver. Id=" + id + " class name = " + className);

        String path = getDriverRoot(id) + "/" + LibFolderName;

        DriverEntry entry = new DriverEntry(id, path, className);

        if (!entry.isResolvable()) {
            logger.warn("Cannot resolve driver class");

            return CodeCannotResolveDriverClass;
        }

        synchronized (drivers) {
            if (drivers.containsKey(id)) {
                logger.warn("This driver is already installed");
                result = CodeDriverIsAlreadyInstalled;
            } else {
                drivers.put(id, entry);
            }
        }

        return result;
    }

    /**
     * Upgrades the specified driver
     * @param id Driver identifier
     * @param className Class name for the driver class
     * @return int
     */
    public int upgrade(DriverId id, String className)
    {
        int result = 0;

        if ((id == null) || StringUtils.isNullOrEmpty(className)) {
            return CodeInvalidArguments;
        }

        String path = getDriverRoot(id) + "/" + LibFolderName;
        DriverEntry entry = new DriverEntry(id, path, className);

        if (!entry.isResolvable()) {
            return CodeCannotResolveDriverClass;
        }

        synchronized (drivers) {
            if (!drivers.containsKey(id)) {
                result = CodeDriverIsNotInstalled;
            } else {
                drivers.put(id, entry);
            }
        }

        return result;
    }

    /**
     * Uninstalls the specified driver
     * @param id Driver identifier
     * @return int
     */
    public int uninstall(DriverId id)
    {
        int result = 0;

        if (id == null) {
            return CodeInvalidArguments;
        }

        DriverEntry entry = null;

        synchronized (drivers) {
            entry = drivers.remove(id);
        }

        if (entry != null) {
            synchronized (entry) {
                if (entry.refCounter == 0) {
                    if (entry.driver != null) {
                        try {
                            entry.driver.cleanup();
                        } catch (Exception e) {
                            result = CodeErrorCleaningUp;
                        }
                    }
                } else {
                    // we will call cleanup when the client returns the driver
                }
            }
        } else {
            result = CodeDriverIsNotInstalled;
        }

        return result;
    }

    /**
     * Obtains a driver
     * @param id Driver identifier
     * @return IDriver
     */
    public IDriver getDriver(DriverId id)
    {
        DriverEntry entry = getDriverEntry(id);
        IDriver result = null;

        if (entry != null) {
            result = entry.getDriver(this);

            if ((result != null) && result.isStateful()) {
                synchronized (entry) {
                    entry.refCounter++;
                }
            }
        }

        return result;
    }

    /**
     * Releases a driver after use
     * @param driver The driver
     * @return int
     */
    public int releaseDriver(IDriver driver)
    {
        int result = 0;

        if (driver == null) {
            result = CodeInvalidArguments;
        } else {
            if (driver.isStateful()) {
                DriverEntry entry = getDriverEntry(driver.getId());

                if (entry != null) {
                    synchronized (entry) {
                        entry.refCounter--;
                    }
                } else {
                    try {
                        driver.cleanup();
                    } catch (Exception e) {
                        result = CodeErrorCleaningUp;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Loads the installed drivers
     * @param file File name
     * @return int
     */
    public int load(String file)
    {
        return FactorySerializer.load(this, file);
    }

    /**
     * Saves the installed drivers
     * @param file File name
     * @return int
     */
    public int save(String file)
    {
        return FactorySerializer.save(this, file);
    }

    /**
     * Obtains the specified Driver entry
     * @param id
     * @return DriverEntry
     */
    protected DriverEntry getDriverEntry(DriverId id)
    {
        if (id == null) {
            return null;
        }

        synchronized (drivers) {
            return drivers.get(id);
        }
    }
}
