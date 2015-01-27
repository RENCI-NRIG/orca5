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

import java.util.Properties;

import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.beans.ResultMng;
import orca.manage.internal.api.IManagementObject;
import orca.security.AuthToken;
import orca.shirako.container.Globals;
import orca.util.ErrorUtils;
import orca.util.ID;
import orca.util.PropList;

import org.apache.log4j.Logger;


/**
 * Base class for all manager objects. A manager object is part of the
 * management layer. It provides a set of management operations for a given
 * component of shirako, for example, actor or slice.
 * <p>
 * Each manager object is registered with the management layer under a unique
 * identifier. The creator of the object is responsible for assigning the
 * correct identifier. The default constructor of <code>ManagerObject</code>
 * generates a unique identifier for each new instance. This identifier can be
 * modified and replaced with the desired identifier. Once a manager object has
 * been registered with the management layer, its identifier cannot change.
 * </p>
 * <p>
 * The {@link #typeID} field of each <code>ManagerObject</code> can be used to
 * assign the same identifier to all instances of a given manager object class.
 * The type identifier field can then be used to construct an appropriate proxy
 * to the <code>ManagerObject</code>.
 * </p>
 * <p>
 * Each <code>ManagerObject</code> can be accessed using a number of
 * protocols. By default, each object can be accessed using local communication.
 * In addition to local communication, the object may support remote
 * communication protocols such as SOAP or XMLRPC. Each
 * <code>ManagerObject</code> maintains an array of protocol descriptors for
 * each supported protocol.
 * </p>
 * <p>
 * Each <code>ManagerObject</code> is responsible for its own persistence. The
 * <code>save()</code> method is going to be invoked only once (when the
 * object is registered). The <code>reset</code> method is going to be invoked
 * every time the system has to reinstantiate the <code>ManagerObject</code>.
 * </p>
 * <p>
 * A <code>ManagerObject</code> can be associated with a given actor. A
 * <code>ManagerObject</code> is associated with an actor if
 * {@link #getActorName()) returns a valid actor name. All manager objects not
 * associated with an actor are considered "container-level", even though they
 * may interact with one or more actors.
 * </p>
 */
public class ManagementObject implements IManagementObject
{
    /**
     * Property for storing a <code>ManagerObject</code>'s class name under
     * during serialization.
     */
    public static final String PropertyClassName = "ObjectClassName";

    /*
     * Serialization/deserialization constants
     */
    public static final String PropertyTypeID = "MOTYPEID";
    public static final String PropertyProxiesLength = "MOProxiesLength";
    public static final String PropertyProxiesPrefix = "MOProxiesPrefix.";
    public static final String PropertyProxiesProtocol = ".protocol";
    public static final String PropertyProxiesClass = ".class";
    public static final String PropertyID = "MOID";
    public static final String PropertyActorName = "MOActorName";

    /**
     * Unique identifier for this proxy type. All instances of this class should
     * have the same.
     * <code>typeID<code>, but they may each have a different <code>id</code>.
     */
    protected ID typeID;

    /**
     * Descriptors of supported proxies.
     */
    protected OrcaProxyProtocolDescriptor[] proxies;

    /**
     * Identifier of the manager object
     */
    protected ID id;

    /**
     * The logger
     */
    protected Logger logger;

    /**
     * The serialized properties this object was recovered from.
     */
    protected Properties serial;

    /**
     * Path to the axis2 service descriptor for this object.
     */
    protected String axis2ServiceDescriptor;
    /**
     * Initialization flag.
     */
    private boolean initialized = false;

    /**
     * Crate a new instance. Assigns a unique ID
     */
    public ManagementObject()
    {
        id = new ID();
        logger = Globals.getLogger(this.getClass().getCanonicalName());
    }

    /**
     * Register all communication protocols this object supports
     */
    protected void registerProtocols()
    {
    }

    /**
     * Performs initialization of the manager object
     * @param manager
     * @throws Exception
     */
    public void initialize() throws Exception
    {
        if (!initialized) {
            registerProtocols();
            if (serial != null) {
                // we are recovering
                recover();
            }

            initialized = true;
        }
    }

    /**
     * Performs recovery actions for this manager object.
     * @throws Exception
     */
    protected void recover() throws Exception
    {
    }

    /**
     * Serialize the object to a properties list.
     * @return a properties list
     */
    public Properties save()
    {
        Properties p = new Properties();
        save(p);

        return p;
    }

    /**
     * Serializes the object into the given properties list.
     * @param p properties list to serialize into
     */
    public void save(Properties p)
    {
        p.setProperty(PropertyClassName, this.getClass().getCanonicalName());
        p.setProperty(PropertyID, id.toString());

        if (typeID != null) {
            p.setProperty(PropertyTypeID, typeID.toString());
        }

        saveProtocols(p);
        PropList.setProperty(p, PropertyActorName, getActorName());
    }

    protected void saveProtocols(Properties p)
    {
        if (proxies == null) {
            return;
        }

        PropList.setProperty(p, PropertyProxiesLength, proxies.length);

        for (int i = 0; i < proxies.length; i++) {
            p.setProperty(PropertyProxiesPrefix + i + PropertyProxiesProtocol,
                          proxies[i].getProtocol());
            p.setProperty(PropertyProxiesPrefix + i + PropertyProxiesClass,
                          proxies[i].getProxyClass());
        }
    }

    protected void loadProtocols(Properties p)
    {
        if (p.containsKey(PropertyProxiesLength)) {
            int count = PropList.getIntegerProperty(p, PropertyProxiesLength);
            proxies = new OrcaProxyProtocolDescriptor[count];

            for (int i = 0; i < proxies.length; i++) {
                proxies[i] = new OrcaProxyProtocolDescriptor();
                proxies[i].setProtocol(
                    p.getProperty(PropertyProxiesPrefix + i + PropertyProxiesProtocol));
                proxies[i].setProxyClass(
                    p.getProperty(PropertyProxiesPrefix + i + PropertyProxiesClass));
            }
        }
    }

    /**
     * Restore this object from a properties list
     * @param p
     */
    public void reset(Properties p)
    {
        id = new ID(p.getProperty(PropertyID));

        if (p.getProperty(PropertyTypeID) != null) {
            typeID = new ID(p.getProperty(PropertyTypeID));
        }

        loadProtocols(p);
        serial = p;
    }

    /**
     * Returns the unique identifier of this object. Each
     * <code>ManagerObject</code> object must have a unique identifier.
     * @return the unique identifier. Always non-null.
     */
    public ID getID()
    {
        return id;
    }

    /**
     * Returns the name of the actor the object is associated with, if any.
     * @return actor name. Null if this management object
     * should not be associated with an actor.
     */
    public String getActorName()
    {
        return null;
    }

    /**
     * Returns the unique type identifier for the object, if any.
     * @return type identifier. Can be null.
     */
    public ID getTypeID()
    {
        return typeID;
    }

    /**
     * Returns the descriptors for all communication protocols supported by the
     * object.
     * @return an array of protocol descriptors. Must be non-null.
     */
    public OrcaProxyProtocolDescriptor[] getProxies()
    {
        return proxies;
    }

    /**
     * Converts the given stack trace into a string
     * @param trace Stack trace
     */
    public String getStackTraceString(StackTraceElement[] trace)
    {
        return ErrorUtils.getStackTrace(trace);
    }

    /**
     * Attaches exception details
     * @param result Result object
     * @param e Exception
     */
    public static void setExceptionDetails(ResultMng result, Exception e)
    {
    	result.setMessage(e.getMessage());
        result.setDetails(e.getMessage() + "\n\n" + orca.util.ExceptionUtils.getStackTraceString(e.getStackTrace())); 
    }
    
    public String getAxis2ServiceDescriptor()
    {
        return axis2ServiceDescriptor;
    }
    
	/*
	 * ========================================================================
	 * Access control
	 * ========================================================================
	 */

	/**
	 * Checks if the given user is an administrator
	 * 
	 * @param token
	 * @return
	 */
	public boolean isAdmin(AuthToken token) {
		if (token == null) {
			return false;
		}

		User user = Globals.getContainer().getUsers().getUser(token.getName());

		if (user != null) {
			if (!user.isLoggedIn(token.getLoginToken())) {return false;}
			return user.isAdmin();
		}

		return false;
	}

	/**
	 * Checks if the user identified by the {@link AuthToken} can operate on the
	 * specified actor. A user can operate an actor if either of the two
	 * conditions holds:
	 * <ul>
	 * <li>The user holds the "admin" role</li>
	 * <li>The user has been granted the right to operate the actor</li>
	 * </ul>
	 * 
	 * @param wrapper Name of the actor
	 * @param userToken User credentials
	 * @return True if the user can operate on the actor, false otherwise
	 */
	public boolean checkAccess(String actorName, AuthToken userToken) {
		if ((userToken == null) || (actorName == null)) {
			return false;
		}

		User user = Globals.getContainer().getUsers().getUser(userToken.getName());

		if (user != null) {
			if (!user.isLoggedIn(userToken.getLoginToken())) {return false;}
			/*
			 * First check if the user is an admin. If not, then check if an
			 * admin has delegated the user the right to operate the actor
			 */
			if (user.isAdmin()) {
				return true;
			} else {
				if (user.canOperate(actorName)) {
					return true;
				}
			}
		}

		return false;
	}
	
	public boolean isLoggedIn(AuthToken auth) {
		if (auth.getLoginToken() == null) {return false;}
		
		User user = Globals.getContainer().getUsers().getUser(auth.getName());
		if (user == null) {return false;}
		
		return user.isLoggedIn(auth.getLoginToken());
	}
}