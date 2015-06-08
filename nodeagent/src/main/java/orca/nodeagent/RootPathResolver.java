/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.nodeagent;

import java.io.File;

/**
 * Resolves the Root path for code running inside the node agent service. Do not
 * use this class to resolve root paths for code running inside shirako.
 */
public class RootPathResolver {
    /**
     * Environment variable declaring the home directory for the node agent
     * service.
     */
    public static final String NAHOME = "NA_HOME";

    /**
     * Returns the root path. The root path will equal to the contents of the
     * environment variable NA_HOME (when defined), or to the current working
     * directory.
     * @return root path
     */
    public static String getRoot() {
        String temp = System.getenv(NAHOME);
        if (temp == null) {
            temp = getDefaultRoot();
        }
        return temp;
    }

    /**
     * Returns the current working directory.
     * @return current working directory
     */
    public static String getDefaultRoot() {
        File f = new File(".");
        return f.getAbsolutePath() + "/";
    }
}
