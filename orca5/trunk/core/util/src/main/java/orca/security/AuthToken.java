/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.security;

import java.security.cert.X509Certificate;
import java.util.Properties;

import orca.util.ID;
import orca.util.PropList;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;

public class AuthToken implements Persistable
{
	public static final String PropertyAuthTokenName = "name";
    public static final String PropertyAuthTokenGuid = "guid";
	public static final String PropertyAuthTokenCred = "cred";
	public static final String PropertyAuthTokenCert = "cert";

	/**
	 * The human-readable name.
	 */
	@Persistent(key = PropertyAuthTokenName)
	protected String name;
	/**
	 * Unique identity represented by this token.
	 */
	@Persistent(key = PropertyAuthTokenGuid)
	protected ID guid;
	/**
	 * The authentication credential.
	 */
	@Persistent(key = PropertyAuthTokenCred)	
	protected Credentials cred;	
	/**
	 * Public Key Certificate.
	 */
	@Persistent(key = PropertyAuthTokenCert)		
	protected X509Certificate cert;

	@NotPersistent
	protected String loginToken;

	/**
	 * Default constructor. Used when rebuilding state from the database
	 */
	public AuthToken() 
	{
	}

	public AuthToken(String name){
		this(name, null, null);
	}
	
	/**
	 * Creates a new <code>AuthToken</code>.
	 * @param name identity name
	 * @param guid identity guid
	 */
	public AuthToken(String name, ID guid) 
	{
		this(name, guid, null);
	}


	/**
	 * Constructs a new <code>AuthToken</code>
	 * @param name identity name 
	 * @param guid identity guid 
	 * @param credentials authentication credentials
	 */
	public AuthToken(String name, ID guid, Credentials credentials)
	{
		this.name = name;
		this.guid = guid;
		this.cred = credentials;
	}

	/**
	 * Gets the name of the identity represented by this token.
	 * @return identity name
	 */
	public String getName() 
	{
		return name;
	}
	
	public ID getGuid()
	{
	    return guid;
	}
	
	/**
	 * Gets the identity credentials
	 * @return identity credentials. Can be null.
	 */
	public Credentials getCredentials() 
	{
		return cred;
	}

	/**
	 * Sets the identity's credentials
	 * @param cred credentials
	 */
	public void setCredentials(Credentials cred) 
	{
		this.cred = cred;
	}

	public void setCertificate(X509Certificate cert){
		this.cert = cert;
	}
	
	public X509Certificate getCertificate(){
		return this.cert;
	}
		
	@Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof AuthToken))return false;
        AuthToken otherAuthToken = (AuthToken)other;
        return ((guid.equals(otherAuthToken.guid)) &&
        		(name.equals(otherAuthToken.name)));
    }
	
	@Override
	public int hashCode() { 
		int hash = 1;
	    hash = hash * 31 + guid.hashCode();
	    hash = hash * 31 + name.hashCode();
	    return hash;
	}
	
	@Override
    public String toString()
    {
        return "(" + guid.toString() + ":" + name + ")";
    }

	public void setLoginToken(String token) {
		this.loginToken = token;
	}
	
	public String getLoginToken() {
		return loginToken;
	}
}