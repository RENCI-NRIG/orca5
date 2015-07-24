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


/**
 * Generates a globally unique identifier (GUID) and prints in on standard
 * out.
 *
 * @author aydan
 */
public class GuidGenerator
{
    /**
     * Main function.
     *
     * @param args arguments
     */
    public static void main(final String[] args)
    {
        new GuidGenerator().generate();
    }

    /**
         * Empty constructor.
         */
    public GuidGenerator()
    {
    }

    /**
     * Generates a GUID and writes it to standard out.
     */
    public void generate()
    {
        System.out.println(new ID().toString());
    }
}