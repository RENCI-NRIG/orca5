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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.exogeni.orca.drivers.beans.Driver;
import net.exogeni.orca.drivers.beans.Drivers;
import net.exogeni.orca.drivers.beans.ObjectFactory;

import org.apache.log4j.Logger;


class FactorySerializer
{
    static Logger logger = Logger.getLogger(FactorySerializer.class.getCanonicalName());

    /**
     * Saves the installed driver info to the specified file
     * @param factory The driver factory
     * @param file File name
     * @return
     */
    public static int save(DriverFactory factory, String file)
    {
        int result = 0;

        try {
            ObjectFactory f = new ObjectFactory();
            Drivers d = f.createDrivers();

            synchronized (factory.drivers) {
                Iterator<DriverEntry> i = factory.drivers.values().iterator();

                while (i.hasNext()) {
                    DriverEntry entry = i.next();
                    Driver driver = f.createDriver();
                    driver.setId(entry.getDriverId().toString());
                    driver.setClassName(entry.getClassName());
                    driver.setPath(entry.getUrl());
                    d.getDriver().add(driver);
                }
            }

            result = save(d, file, factory.getClass().getClassLoader());
        } catch (Exception e) {
            logger.error("save", e);
            result = -1;
        }

        return result;
    }

    /**
     * Loads the installed drivers list from the specified file
     * @param factory The driver factory
     * @param file File name
     * @return
     */
    public static int load(DriverFactory factory, String file)
    {
        int result = 0;

        try {
            Drivers d = load(file, factory.getClass().getClassLoader());

            if (d == null) {
                result = -1;
            } else {
                List l = d.getDriver();

                if (l != null) {
                    Iterator i = l.iterator();

                    while (i.hasNext()) {
                        Driver driver = (Driver) i.next();
                        int code = factory.install(new DriverId(driver.getId()),
                                                   driver.getClassName());

                        if (code != 0) {
                            result = code;
                        }
                    }
                }
            }
        } catch (Exception e) {
            result = -1;
            logger.error("load", e);
        }

        return result;
    }

    protected static Drivers load(String file, ClassLoader loader)
    {
        try {
            JAXBContext context = JAXBContext.newInstance("net.exogeni.orca.drivers.beans", loader);
            Unmarshaller um = context.createUnmarshaller();
            FileInputStream is = new FileInputStream(file);

            return (Drivers) um.unmarshal(is);
        } catch (Exception e) {
            logger.error("loadfile", e);
            return null;
        }
    }

    protected static int save(Drivers drivers, String file, ClassLoader loader)
    {
        try {
            JAXBContext context = JAXBContext.newInstance("net.exogeni.orca.drivers.beans", loader);
            Marshaller m = context.createMarshaller();

            FileOutputStream os = new FileOutputStream(file);
            m.marshal(drivers, os);

            return 0;
        } catch (Exception e) {
            logger.error("savefile", e);
            return -1;
        }
    }
}