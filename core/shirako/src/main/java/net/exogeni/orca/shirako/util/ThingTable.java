/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.util;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;


/**
 * ThingTable maintains a table of objects indexed by names, which it verifies
 * to be non-null, non-empty, and unique. It may be used by agent and/or VCM
 * since it does not touch the thing state.
 * <p>
 * ThingTable is unsynchronized: the caller is responsible for synchronization.
 */
public class ThingTable
{
    private HashMap things;
    private Logger logger;
    private String thingType;

    public ThingTable(Logger logger)
    {
        things = new HashMap();
        this.logger = logger;
        thingType = new String("thing");
    }

    /**
     * Associate a thing record with a thing name. Verify that the name and
     * record are minimally valid.
     * @param thingname the name of the thing
     * @param thing the thing
     * @throws Exception thrown if thing name is null
     * @throws Exception thrown if there is no descriptor
     * @throws Exception thrown if the thing is already active
     */
    public void install(String thingname, Object thing) throws Exception
    {
        if ((thingname == null) || thingname.equals("")) {
            throw new Exception("Missing " + thingType + " name");
        }

        if (thing == null) {
            throw new Exception("Missing " + thingType + " descriptor");
        }

        // logger.info("Install "+thingType+" "+ thingname);
        Object o = things.put(thingname, thing);

        if (o != null) {
            logger.error("Duplicate " + thingType + " " + thingname);
            things.put(thingname, o);
            throw new Exception("Thing " + thingType + " is already active");
        }
    }

    /**
     * Return a Thing iterator.
     * @return a thing iterator
     */
    public Iterator iterator()
    {
        return things.values().iterator();
    }

    /**
     * Do a lookup on a thing name and return thing
     * @param thingname the name of the thing
     * @return the thing associated with that thing name
     * @throws Exception thrown if no thing associated with that name
     */
    public Object lookup(String thingname) throws Exception
    {
        Object t = softLookup(thingname);

        if (t == null) {
            logger.error("No " + thingType + " with name " + thingname);
            throw new Exception("No such " + thingType + " " + thingname);
        }

        return t;
    }

    /**
     * Remove a thing
     * @param thingname the name of the thing being removed
     * @return the thing
     * @throws Exception thrown if thing name is null
     */
    public Object remove(String thingname) throws Exception
    {
        if ((thingname == null) || thingname.equals("")) {
            throw new Exception("Missing " + thingType + " name");
        }

        Object t = things.remove(thingname);

        if (t != null) {
            return t;
        }

        return null;
    }

    /**
     * Set logger
     * @param logger logger
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    public void setType(String type)
    {
        thingType = type;
    }

    /**
     * Do a soft lookup on a thing name and return thing
     * @param thingname the name of the thing
     * @return the thing associated with that thing name
     */
    public Object softLookup(String thingname)
    {
        if ((thingname == null) || thingname.equals("")) {
            return null;

            //            Exception e = new Exception("Missing " + thingType + " name " + thingname);
            //            e.printStackTrace();
            //            logger.error("Lookup failed on " + thingType + " " + thingname);
            //            throw e;
        }

        // logger.info("Lookup thing " + thingname);
        Object t = (Object) things.get(thingname);

        return t;
    }
}
