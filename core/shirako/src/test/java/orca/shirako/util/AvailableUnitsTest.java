/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orca.util.AvailableUnits;


public class AvailableUnitsTest extends TestCase
{
    public void testCompareTo()
    {
        AvailableUnits a1 = new AvailableUnits();

        a1.start = 1;
        a1.end = 10;
        a1.units = 100;

        assertEquals(0, a1.compareTo(a1));

        AvailableUnits a2 = new AvailableUnits();

        a2.start = 5;
        a2.end = 10;
        a2.units = 1000;

        assertEquals(0, a1.compareTo(a2));
        assertEquals(0, a2.compareTo(a1));

        a2.end = 100;

        assertEquals(-1, a1.compareTo(a2));
        assertEquals(1, a2.compareTo(a1));
    }

    public static Test suite()
    {
        return new TestSuite(AvailableUnitsTest.class);
    }
}