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

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpSession;


/**
 * SessionTool is a session-scoped tool for use in Velocity templates for web
 * applications. Velocity instantiates one instance per session.
 * <p>
 * A SessionTool associates four elements with each session: - a string-valued
 * current role (defaults to "home") - a string-valued current screen (defaults
 * to "home") - a titled {@link ObjectSet} of named objects for the session - an
 * Object-valued "current target"
 * <p>
 * VTL example: #set ( $role = $vsession.role ) #set ( $target =
 * $vsession.target ) #set ( $sobjects = $vsession.objects ) #set (
 * $sobjects.name = "session object set" ) #foreach( $object in
 * $sobjects.allObjects )
 */
public class SessionTool implements ViewTool
{
    protected HttpSession session;
    protected String role;
    protected String screen;
    protected Object target;
    protected ObjectSet objects;

    public SessionTool()
    {
        session = null;
        role = "home";
        screen = "home";
        target = null;
        objects = new ObjectSet();
        objects.setName("Shared objects for the session");
    }

    /**
     * Invoked by the Velocity container to initialize a SessionTool instance
     * for a new session. The container passes a ViewContext, from which we
     * obtain the Web application context and session. Problem: a Velocity tool
     * "cannot fail" to initialize. No exceptions. So we delay initialization to
     * first use, and throw an exception into the template interpreter if it
     * fails.
     * @param o the ViewContext of the first request in this session
     */
    public void init(Object o)
    {
        ViewContext vcontext = (ViewContext) o;
        this.session = (HttpSession) vcontext.getRequest().getSession();
    }

    /**
     * Get the active role for the current session. VTL: $vsession.Role
     * @return active role for the current session
     */
    public String getRole() throws Exception
    {
        return role;
    }

    /**
     * Set the active role for the current session, and reset the screen to the
     * home screen for the role. VTL: $vsession.setRole($role)
     * @param role active role for the current session
     */
    public void setRole(String role) throws Exception
    {
        this.role = role;
        this.screen = "home";
    }

    /**
     * Get the active screen for the current session. VTL: $vsession.Screen
     * @return active screen for the current session
     */
    public String getScreen() throws Exception
    {
        return screen;
    }

    /**
     * Set the active screen for the current session. VTL: #set (
     * $vsession.Screen = $screen )
     * @param screen active screen for the current session
     */
    public void setScreen(String screen) throws Exception
    {
        this.screen = screen;
    }

    /**
     * Get the current target object for the current session. By default the
     * current object is a new Properties object. VTL: $target =
     * $vsession.Target
     * @return current target object
     */
    public Object getTarget() throws Exception
    {
        if (target == null) {
            target = new Properties();
        }

        return target;
    }

    /**
     * Set the active target for the current session. VTL: #set (
     * $vsession.target = $target )
     * @param target current object for the session
     * @return current target object
     */
    public Object setTarget(Object target) throws Exception
    {
        this.target = target;

        return target;
    }

    /**
     * Get the object set handle for the current session. VTL: $vsession.objects
     * @return object set for the current session
     */
    public ObjectSet getObjects()
    {
        return objects;
    }

    /**
     * Get the creation time for this session as a date. VTL:
     * $vsession.creationTime
     * @return creation time of this session
     */
    public Date getCreationTime()
    {
        return new Date(session.getCreationTime());
    }

    public boolean exits(String key)
    {
        return objects.contains(key);
    }

    public void put(String key, Object obj)
    {
        objects.put(key, obj);
    }

    public void remove(String key)
    {
        objects.remove(key);
    }

    public Object get(String key)
    {
        return objects.get(key);
    }

    public String getString(String key)
    {
        return (String) objects.get(key);
    }
}