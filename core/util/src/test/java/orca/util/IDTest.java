/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import orca.util.ID;


public class IDTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite(IDTest.class);
    }

    public void testClone()
    {
        String[] ids = new String[] { "one", "two", "three", "four" };

        for (int i = 0; i < ids.length; i++) {
            ID id1 = new ID(ids[i]);
            ID id2 = (ID) id1.clone();
            assertNotSame(id1, id2);
            assertEquals(id1, id2);
        }
    }

    public void testCreate()
    {
        String[] ids = new String[] { "one", "two", "three", "four" };

        for (int i = 0; i < ids.length; i++) {
            ID id = new ID(ids[i]);
            assertEquals(ids[i], id.toString());
        }
    }

    public void testEquals()
    {
        String[] ids = new String[] { "one", "two", "three", "four" };

        ID previousID = null;

        for (int i = 0; i < ids.length; i++) {
            ID id1 = new ID(ids[i]);
            ID id2 = new ID(ids[i]);

            assertEquals(id1, id1);
            assertEquals(id1, id2);
            assertFalse(id1.equals(previousID));
            assertFalse(id2.equals(previousID));
            assertFalse(id1.equals(ids[i]));
        }
    }
}