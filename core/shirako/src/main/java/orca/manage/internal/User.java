/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage.internal;


import java.util.List;
import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.util.PropList;


/**
 * Represents a user record. The management layer uses user records for
 * performing access control decisions.
 */
public class User
{
    public static final String PropertyLogin = "UserLogin";
    public static final String PropertyFirst = "UserFirst";
    public static final String PropertyLast = "UserLast";
    public static final String PropertyRoles = "UserRoles";
    public static final String PropertyActors = "UserActors";

    /**
     * Loging name
     */
    protected String login;

    /**
     * First name
     */
    protected String first;

    /**
     * last name
     */
    protected String last;

    /**
     * Roles granted to this user
     */
    protected String[] roles;

    /**
     * Actors this user can operate
     */
    protected String[] actors;

    /**
     * Token issued to the user during login.
     */
    protected String loginToken;
    
    /**
     * Crete a new instance
     */
    public User()
    {
    }

    /**
     * Checks if this user can operate on the specified actor
     * @param actorName Name of the actor
     * @return true for success; false otherwise
     */
    public boolean canOperate(String actorName)
    {
        // XXX; this is a sub-optimal implementation and results in cost linear
        // in the number of actors. However, given that the operable actors will
        // be a few (most of the times just one), it may work just fine.
        if ((actors != null) && (actorName != null)) {
            for (int i = 0; i < actors.length; i++) {
                if (actors[i].equals(actorName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the actors this user can operate
     * @return the actors this user can operate
     */
    public String[] getActors()
    {
        return actors;
    }

    /**
     * Returns the first name
     * @return the first name
     */
    public String getFirst()
    {
        return first;
    }

    /**
     * Returns the last name
     * @return the last name
     */
    public String getLast()
    {
        return last;
    }

    /**
     * Returns the login name
     * @return login name
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * Returns the roles granted to this user
     * @return roles granted to user
     */
    public String[] getRoles()
    {
        return roles;
    }

    /**
     * Checks if this user holds the specified role
     * @param role role name.
     * @return true for success; false otherwise
     */
    public boolean hasRole(String role)
    {
        // XXX; this is a sub-optimal implementation and results in cost linear
        // in the number of roles. However, given that the roles will
        // be a few (most of the times just one), it may work just fine.
        if ((roles != null) && (role != null)) {
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equals(role)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if this user is an admin
     * @return true for success; false otherwise
     */
    public boolean isAdmin()
    {
        return hasRole(OrcaConstants.RoleAdmin);
    }

    /**
     * Deserializes the object from the given properties list
     * @param p properties
     * @throws Exception in case of error
     */
    public void reset(Properties p) throws Exception
    {
        login = p.getProperty(PropertyLogin);
        first = p.getProperty(PropertyFirst);
        last = p.getProperty(PropertyLast);

        roles = PropList.getStringArrayProperty(p, PropertyRoles);
        actors = PropList.getStringArrayProperty(p, PropertyActors);
    }

    /**
     * Serializes the object into a properties list
     * @return properties
     * @throws Exception in case of error
     */
    public Properties save() throws Exception
    {
        Properties p = new Properties();
        save(p);

        return p;
    }

    /**
     * Serializes the object into the given properties list
     * @param p properties
     * @throws Exception in case of error
     */
    public void save(Properties p) throws Exception
    {
        PropList.setProperty(p, PropertyLogin, login);
        PropList.setProperty(p, PropertyFirst, first);
        PropList.setProperty(p, PropertyLast, last);
        PropList.setProperty(p, PropertyRoles, roles);
        PropList.setProperty(p, PropertyActors, actors);
    }

    /**
     * Sets the actors this user can operate
     * @param actors array of actors
     */
    public void setActors(String[] actors)
    {
        this.actors = actors;
    }

    public void setActors(List<String> actors) {
    	this.actors = (String[])actors.toArray();
    }
    /**
     * Sets the first name
     * @param first first name
     */
    public void setFirst(String first)
    {
        this.first = first;
    }

    /**
     * Sets the last name
     * @param last last name
     */
    public void setLast(String last)
    {
        this.last = last;
    }

    /**
     * Sets the login name
     * @param login login name
     */
    public void setLogin(String login)
    {
        this.login = login;
    }

    /**
     * Sets the roles granted to this user
     * @param roles roles
     */
    public void setRoles(String[] roles)
    {
        this.roles = roles;
    }
    
    public void setRoles(List<String> roles) {
    	this.roles = (String[])roles.toArray();
    }
    
    public void setLoginToken(String token) {
    	this.loginToken = token;
    }
    
    public boolean isLoggedIn(String token) {
    	return (loginToken != null && loginToken.equals(token));
    }
}
