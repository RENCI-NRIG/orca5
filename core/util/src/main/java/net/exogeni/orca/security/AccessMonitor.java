/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.security;

import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;


/**
 * AccessMonitor encapsulates access control policy and any means of proving or
 * gaining access to resources, e.g., payment. This should be an abstract
 * pluggable class.
 * <p>
 * Each operation on a slices actor includes an AuthToken to represent identity
 * and any ancillary information. AccessMonitor determines whether any given
 * access is permitted, given the AuthToken and a per-object auth.Guard, which
 * encapsulates an access control list.
 */
public class AccessMonitor
{
    /*
     * Access types
     */
    public static final int AccessUpdate = 1;
    public static final int AccessCreateSlice = 2;
    public static final int AccessReserve = 3;
    public static final int AccessOps = 4;

    public static final Logger logger = Logger.getLogger(AbacUtil.class);
    
    public AccessMonitor()
    {
    }

    /**
     * Gets the unique identity object associated with an AuthToken.
     * @param auth auth token
     * @return identity object (credential)
     * @throws Exception in case of error
     */
    public Credentials getCredentials(AuthToken auth) throws Exception
    {
        return auth.getCredentials();
    }

    /**
     * Gets the unique identity object associated with an identity reference
     * (whose interpretation is implementation-specific).
     * @param name name of the object
     * @return identity object (credential)
     * @throws Exception in case of error
     */
    public Object getCredential(String name) throws Exception
    {
        return null;
    }

    public void checkReserve(AuthToken auth, Guard guard) throws Exception
    {
        return;
    }
    
    public void checkReserve(Guard guard, AuthToken requester, X509Certificate authorityCert, PrivateKey authorityKey) throws Exception
    {
    	if(!AbacUtil.verifyCredentials)
    		return;
    	
        guard.checkReserve(requester, authorityCert, authorityKey);
    }

    public void checkUpdate(Guard guard, AuthToken requester, X509Certificate authorityCert, PrivateKey authorityKey) throws Exception
    {
    	if(!AbacUtil.verifyCredentials)
    		return;
    	
        guard.checkReserve(requester, authorityCert, authorityKey);
    }
    
    public AuthToken checkProxy(AuthToken proxy, AuthToken requester) throws CertificateException
    {
    	if(requester == null)
    		return proxy;
    	
    	if(!AbacUtil.verifyCredentials)
    		return requester;

    	try{
			if(AbacUtil.checkPrivilege(proxy.getCertificate(), requester.getCertificate(), null, new String[]{AbacUtil.AbacRoleSpeaksFor})){
				return requester;
			}
			throw new CertificateException("Proxy verification failed");
		} catch (Exception exception){
			logger.error(exception.getMessage());
			throw new CertificateException("Proxy verification failed");
		}
	}

}
