/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.tools.vservlet;

import java.util.Properties;


/**
 * This sample application maintains a collection of named objects with
 * descriptions. The sample menus to operate on the objects direct to
 * propedit.vm VTL, which assumes the objects are property lists.
 */
public class SampleServerRoot extends ServerRoot
{
    private ObjectSet objects;

    public SampleServerRoot()
    {
        // default name, overridden by service properties to init().
        name = "Sample vservlet service";
        objects = new ObjectSet();
        objects.setName("Shared objects for the " + name);
    }

    /**
     * Initializes this root object (called on first access).
     * @throws Exception if the object fails to initialize (e.g., bad
     *             properties)
     */
    public void init(Properties p, ServerTool s) throws Exception
    {
        super.init(p, s);
    }

    /**
     * Get the object set handle for the application. VTL: $root.objects
     * @return object set
     */
    public ObjectSet getObjects()
    {
        return objects;
    }

    public String toString()
    {
        return name;
    }
}