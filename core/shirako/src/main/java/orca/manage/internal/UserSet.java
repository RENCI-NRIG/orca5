/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.manage.internal;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.container.Globals;
import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.util.LruCache;

import org.apache.log4j.Logger;

/**
 * This class allows access to the users defined for a given container. User
 * information is obtained from the database and cached in an LRU cache.
 */
public class UserSet
{
    /**
     * Cache capacity
     */
    public int cacheCapacity = 100;

    /**
     * User database
     */
    protected IOrcaContainerDatabase db;

    /**
     * LRU cache
     */
    protected LruCache<String, User> cache;
    protected Logger logger;

    /**
     * Creates a new instance
     */
    public UserSet()
    {
        this.logger = Globals.getLogger(this.getClass().getCanonicalName());
        cache = new LruCache<String, User>(cacheCapacity);
    }

    /**
     * Removes the record for the specifed user from the cache (if present)
     * @param userName
     */
    public void flushUser(String userName)
    {
        synchronized (cache) {
            cache.remove(userName);
        }
    }

    /**
     * Obtains the specified user record
     * @param userName
     * @return
     */
    public User getUser(String userName)
    {
        if (userName == null) {
            return null;
        }

        User user = null;

        synchronized (cache) {
            user = cache.get(userName);
        }

        if (user != null) {
            return user;
        }

        // cache miss
        user = getUserDB(userName);

        if (user != null) {
            synchronized (cache) {
                User temp = cache.get(userName);

                if (temp == null) {
                    cache.put(userName, user);
                } else {
                    user = temp;
                }
            }
        }

        return user;
    }

    /**
     * Obtains the specified user record from the database
     * @param userName
     * @return
     */
    protected User getUserDB(String userName)
    {
        User result = null;

        if (userName == null) {
            return null;
        }

        synchronized (this) {
            if (db == null) {
                db = Globals.getContainer().getDatabase();
            }
        }

        try {
            if (db != null) {
                Vector v = db.getUser(userName);

                if ((v != null) && (v.size() > 0)) {
                    Properties p = (Properties) v.get(0);

                    if (p != null) {
                        result = new User();
                        result.reset(p);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("DatabaseUserIdentitySet::getUser", e);
        }

        return result;
    }
    
    public void addInternalAdminUser(User u) {
        synchronized (cache) {
            if (cache.get(u.getLogin()) == null) {
            	cache.put(u.getLogin(), u);
            }
        }   	
    }
}
