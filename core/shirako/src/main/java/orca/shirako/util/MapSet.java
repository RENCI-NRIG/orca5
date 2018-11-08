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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * MapSet is a general utility collection class that maintains a map of maps,
 * i.e., a doubly-indexed collection or a tree of depth three (to track
 * grandchildren by their parents and names). It can return iterators for the
 * map keys, object keys within a map, or objects within a map. Insertion order
 * is preserved in the object iterators.
 * <p>
 * ActorRegistry uses this class is to track the actors owned by each identity,
 * but there could be other uses as well.
 */
public class MapSet
{
    protected HashMap maps;

    public MapSet()
    {
        maps = new HashMap();
    }

    /**
     * Removes all entries from the MapSet
     */
    public void clear()
    {
        Iterator iter = maps.values().iterator();

        while (iter.hasNext()) {
            Map map = (Map) iter.next();
            map.clear();
        }

        maps.clear();
    }

    /**
     * Returns true iff the map contains the vale
     * @param mapkey mapkey
     * @param ovalue ovalue
     * @return if the map contains the value
     */
    public boolean containsValue(Object mapkey, Object ovalue)
    {
        Map map = getMap(mapkey);

        return map.containsValue(ovalue);
    }

    /**
     * Gets the object indexed by (mapKey, oKey).
     * @param mapKey a key for a map in this MapSet
     * @param oKey a key for an object in the selected map
     * @return the indexed object, or null
     */
    public Object get(Object mapKey, Object oKey)
    {
        Map map = getMap(mapKey);

        return map.get(oKey);
    }

    /**
     * Gets the map associated with a map key, and creates one if it does not
     * already exist.
     * @param mapkey a key for the map in this MapSet
     * @return the map indexed by mapKey
     */
    private Map getMap(Object mapKey)
    {
        assert (mapKey != null);

        Map map = (Map) maps.get(mapKey);

        if (map == null) {
            map = new LinkedHashMap();
            maps.put(mapKey, map);
        }

        return map;
    }

    /**
     * Gets an iterator of mapKeys for the maps in this MapSet.
     * @return iterator of map keys
     */
    public Iterator getMapKeys()
    {
        return maps.keySet().iterator();
    }

    /**
     * Gets an iterator of object keys for a specific map.
     * @param mapkey a key for a map in this MapSet
     * @return iterator of object keys in the selected map
     */
    public Iterator getObjectKeys(Object mapkey)
    {
        Map map = getMap(mapkey);

        return map.keySet().iterator();
    }

    /**
     * Gets an iterator of objects in a specific map.
     * @param mapkey a key for a map in this MapSet
     * @return iterator of objects in the selected map
     */
    public Iterator getObjects(Object mapkey)
    {
        Map map = getMap(mapkey);

        return map.values().iterator();
    }

    /**
     * Puts an object in this MapSet, indexed by keys for the map and for the
     * object within the map.
     * @param mapKey a key for the map in this MapSet
     * @param oKey a key for the object in the selected map
     * @param obj the object to put in the selected map
     * @return object formerly indexed at (mapKey, oKey), or null
     */
    public Object put(Object mapKey, Object oKey, Object obj)
    {
        Map map = getMap(mapKey);
        Object former = map.put(oKey, obj);

        return former;
    }
}
