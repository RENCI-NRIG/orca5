/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.tools.axis2;

import java.util.HashMap;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class Axis2ClientConfigurationManager {
    public static final String DefaultIndex = "default";
    /**
     * Singleton instance.
     */
    private static Axis2ClientConfigurationManager instance;
    /**
     * Hashmap if configuration contexts.
     */
    private HashMap<String, HashMap<String, ConfigurationContext>> map;

    static {
        instance = new Axis2ClientConfigurationManager();
    }

    private Axis2ClientConfigurationManager() {
        map = new HashMap<String, HashMap<String, ConfigurationContext>>();
    }

    /**
     * Returns the configuration context for a given (repository, axis
     * configuration) pair.
     * @param repository path to axis2 repository (can be null)
     * @param config path to axis2 configuration file (can be null)
     * @return axis2 ConfigurationContext
     * @throws Exception
     */
    public ConfigurationContext getContext(String repository, String config) throws Exception {
        String repIndex = repository;
        if (repIndex == null) {
            repIndex = DefaultIndex;
        }

        String configIndex = config;
        if (configIndex == null) {
            configIndex = DefaultIndex;
        }

        ConfigurationContext context = null;

        synchronized (map) {
            HashMap<String, ConfigurationContext> repMap = map.get(repIndex);
            if (repMap == null) {
                repMap = new HashMap<String, ConfigurationContext>();
                map.put(repIndex, repMap);
            }

            context = repMap.get(configIndex);
            if (context == null) {
                context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repository, config);
                repMap.put(configIndex, context);
            }
        }

        return context;
    }

    public static Axis2ClientConfigurationManager getInstance() {
        return instance;
    }
}
