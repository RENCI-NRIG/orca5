/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * A simple LRU cache
 *
 * @param <K> Class for keys
 * @param <V> Class for objects
 */
public class LruCache<K, V> extends LinkedHashMap<K, V>
{
    /**
     *
     */
    private static final long serialVersionUID = 7296385564816740480L;
    public static final float loadFactor = 0.75F;
    protected int cacheCapacity;

    public LruCache(int cacheCapacity)
    {
        super(cacheCapacity + 1, loadFactor, true);
        this.cacheCapacity = cacheCapacity;
    }

    @Override
    public boolean removeEldestEntry(Map.Entry<K, V> eldest)
    {
        return size() > cacheCapacity;
    }
}
