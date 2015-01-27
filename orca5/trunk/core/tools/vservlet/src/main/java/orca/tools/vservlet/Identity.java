/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.tools.vservlet;

import java.util.Iterator;
import java.util.LinkedHashSet;


/**
 * Identity encapsulates all notion of user identity for a vservlet Web
 * application, including the set of roles a user can play.
 * <p>
 * If a Web application has a notion of identity, it should extend this class,
 * and implement a class supporting the IdentitySet interface to look up
 * instances of Identity by username, e.g., as defined by
 * HttpServletRequest.getRemoteUser or Principal.getName.
 * <p>
 * Each identity has two default roles: "home", and "session".
 * <p>
 * VTL: $identity.login: user's login $identity.username: user's name, suitable
 * for printing
 */
public class Identity
{
    protected String login;
    protected String username;
    protected LinkedHashSet roles;
    protected boolean authenticated;
    protected String first;
    protected String last;

    public Identity(String login, String name)
    {
        this.login = login;
        this.username = name;
        roles = new LinkedHashSet();
        authenticated = false;
    }

    /**
     * Sets this Identity as authenticated, or not. (default false)
     */
    public void setAuthenticated(boolean authenticated)
    {
        this.authenticated = authenticated;
    }

    /**
     * Returns true if this Identity is marked as authenticated, else false.
     * @return authenticated flag
     */
    public boolean getAuthenticated()
    {
        return authenticated;
    }

    /**
     * Returns an iterator of Strings representing the operable roles for this
     * session's user identity. VTL #foreach( $role in $identity.roles )
     */
    public synchronized Iterator getRoles()
    {
        return roles.iterator();
    }

    /**
     * Adds an operable role for the current identity. The iterator preserves
     * insertion order.
     * @param role name of new role.
     */
    public synchronized void addRole(String role)
    {
        roles.add(role);
    }

    public synchronized boolean hasRole(String role)
    {
        return roles.contains(role);
    }

    /**
     * Returns the username for the current session.
     * @return username name suitable for display
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Returns the user's login name for the current session. VTL:
     * $identity.Login
     * @return login user login name
     */
    public String getLogin()
    {
        return login;
    }

    public String toString()
    {
        return username;
    }

    /**
     * @return the first
     */
    public String getFirst()
    {
        return this.first;
    }

    /**
     * @param first the first to set
     */
    public void setFirst(String first)
    {
        this.first = first;
    }

    /**
     * @return the last
     */
    public String getLast()
    {
        return this.last;
    }

    /**
     * @param last the last to set
     */
    public void setLast(String last)
    {
        this.last = last;
    }
}