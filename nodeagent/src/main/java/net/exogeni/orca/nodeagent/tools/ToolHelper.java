/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tools;

public class ToolHelper {
    protected String[] args;
    protected String location;
    protected String repository = null;
    protected String config = null;
    protected int index = 0;

    public ToolHelper(String[] args) {
        this.args = args;
        parse();
    }

    protected void parse() {
        for (; index < args.length; index++) {
            if (args[index].startsWith("-")) {
                if (args[index].equals("-l")) {
                    index++;

                    if (index < args.length) {
                        location = args[index];
                    } else {
                        break;
                    }
                } else if (args[index].equals("-r")) {
                    index++;

                    if (index < args.length) {
                        repository = args[index];
                    } else {
                        break;
                    }
                } else if (args[index].equals("-c")) {
                    index++;

                    if (index < args.length) {
                        config = args[index];
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    /**
     * @return the config
     */
    public String getConfig() {
        return this.config;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * @return the repository
     */
    public String getRepository() {
        return this.repository;
    }
}