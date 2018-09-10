/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.tools.vservlet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;


/**
 * ObjectSet maintains a titled collection of named objects with String
 * descriptions, synchronized for concurrent access from VTL as shared state for
 * a Web application. ObjectSet creates/stores Property objects by default: a
 * subclass may override.
 */
public class ObjectSet
{
    private HashMap objects;
    private HashMap descriptions;
    String name;

    public ObjectSet()
    {
        name = "ObjectSet";
        objects = new HashMap();
        descriptions = new HashMap();
    }

    /**
     * Set the name/title of the ObjectSet, e.g., for table titles.
     * @param name the name/title of the ObjectSet
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the name/title of the ObjectSet, e.g., for table titles.
     * @return the name/title of the ObjectSet
     * @see toString
     */
    public String getName()
    {
        return name;
    }

    /**
     * Create and return a new object of the type maintained by this collection.
     * This version makes Properties: override in subclass.
     * @return new property list
     */
    public Object create()
    {
        Properties p = new Properties();

        return p;
    }

    /**
     * Store an object in the collection.
     * @param key name/title of this object
     * @param descr description of this object
     * @param o the object to add to the collection
     * @return null if success, else an error string.
     */
    public synchronized void put(String key, String descr, Object o)
    {
        //if (objects.get(key) != null)
        //    return "Object " + key + " already exists!";
        objects.put(key, o);
        descriptions.put(key, descr);

        // return ("added successfully");
        //return null;
    }

    public synchronized void put(String key, Object o)
    {
        objects.put(key, o);
    }

    public synchronized String getString(String key)
    {
        return (String) objects.get(key);
    }

    public synchronized boolean contains(String key)
    {
        return objects.containsKey(key);
    }

    /**
     * Retrieve an object from the collection.
     * @param key name/title of the object
     */
    public synchronized Object get(String key)
    {
        return objects.get(key);
    }

    /**
     * Retrieve an object description from the collection.
     * @param key name/title of the object
     * @return the object description or null
     */
    public synchronized String getDescription(String key)
    {
        return (String) descriptions.get(key);
    }

    /**
     * Remove an object from the collection.
     * @param key name/title of the object
     * @return null for success, else an error message
     */
    public synchronized String remove(String key)
    {
        if (objects.remove(key) == null) {
            return "Object " + key + " does not exist!";
        }

        descriptions.remove(key);

        // return "removed successfully";
        return null;
    }

    /**
     * Return an iterator for the objects in the collection, suitable for use by
     * VTL #foreach. Warning: templates iterating through the objects may throw
     * an exception if there is a concurrent modification to the collection. I'd
     * like to know how to mask this so the iterator is just a "hint"...short of
     * cloning it.
     * @return iterator
     */
    public Iterator allObjects()
    {
        return objects.keySet().iterator();
    }

    /**
     * Render ObjectSet as a string: return its name/title.
     * @return name/title of this ObjectSet
     */
    public String toString()
    {
        return name;
    }
}