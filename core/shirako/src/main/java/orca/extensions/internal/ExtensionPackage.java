/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.extensions.internal;


import orca.extensions.PackageId;
import orca.util.PropList;

import java.util.Properties;


/**
 * The <code>ExtensionPackage</code> class represents the metadata for an
 * extension package. Each extension package consists of a unique identifier, a
 * name, and an optional description.
 */
public class ExtensionPackage
{
    /*
     * Serialization/deserialization constants.
     */
    public static final String PropertyId = "ExtensionPackageId";
    public static final String PropertyName = "ExtensionPackageName";
    public static final String PropertyDescription = "ExtensionPackageDescription";

    /**
     * Package identifier. Each extension package must have a unique identifier.
     */
    protected PackageId id;

    /**
     * Package name.
     */
    protected String name;

    /**
     * Package description.
     */
    protected String description;

    /**
     * Creates a new instance
     */
    public ExtensionPackage()
    {
    }

    /**
     * Returns the package description.
     * @return the description
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Returns the package identifier.
     * @return the package identifier
     */
    public PackageId getId()
    {
        return this.id;
    }

    /**
     * Returns the package name
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Deserializes the object from the given properties list.
     * @param p properties list
     * @throws Exception
     */
    public void reset(Properties p) throws Exception
    {
        id = new PackageId(p.getProperty(PropertyId));
        name = p.getProperty(PropertyName);
        description = p.getProperty(PropertyDescription);
    }

    /**
     * Serializes the object into a properties list.
     * @return a properties list
     * @throws Exception
     */
    public Properties save() throws Exception
    {
        Properties p = new Properties();
        save(p);

        return p;
    }

    /**
     * Serializes the object into the given properties list.
     * @param p properties list to serialize into
     * @throws Exception
     */
    public void save(Properties p) throws Exception
    {
        PropList.setProperty(p, PropertyId, id.toString());
        PropList.setProperty(p, PropertyName, name);
        PropList.setProperty(p, PropertyDescription, description);
    }

    /**
     * Sets the package description string.
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Sets the package identifier.
     * @param id the package identifier to set
     */
    public void setId(PackageId id)
    {
        this.id = id;
    }

    /**
     * Sets the package name.
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }
}