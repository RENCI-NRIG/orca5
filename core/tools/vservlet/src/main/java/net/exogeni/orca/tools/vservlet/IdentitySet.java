/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.tools.vservlet;

import java.util.*;


/**
 * IdentitySet is a singleton with access to a set of user identities enabled
 * for the Web application.
 */
public class IdentitySet
{
    protected ServerRoot root;
    protected HashMap identities;

    public IdentitySet(ServerRoot root)
    {
        this.root = root;
        identities = null;
    }

    /**
     * Returns the user identity corresponding to a login name (e.g., for
     * container-managed security, a name as returned by
     * HttpServletRequest.getRemoteUser() or Principal.getName()
     * @param username pre-authenticated user name
     * @return user identity object or null
     */
    public Identity getIdentity(String username)
    {
        /*
         * Look it up in users, make the identity, cache it. There should be a
         * database behind us, and someone will have to extend this class to get
         * to it. If Identity is authenticated, be sure to
         * id.setAuthenticated(true).
         */
        return null;
    }

    /**
     * Remove any cached state associated with the specified user
     * @param userName pre-authenticated user name
     */
    public void flushIdentity(String userName)
    {
    }

    /**
     * Returns a default identity for an anonymous user, with default roles
     * "home" and "session".
     * @return default identity {nobody, Anonymous User}
     */
    public Identity makeAnonymous()
    {
        Identity id = new Identity("nobody", "Anonymous User");
        id.addRole("home");
        id.addRole("session");

        return id;
    }

    /**
     * Returns a default identity for an unrecognized user, with default roles
     * "home" and "session".
     * @param username authenticated user name
     * @return default identity {username, "Unrecognized User"}
     */
    public Identity makeUnrecognized(String username)
    {
        Identity id = new Identity("somebody", "Unrecognized User");
        id.addRole("home");
        id.addRole("session");

        return id;
    }
}