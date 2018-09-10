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

import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.Persistent;


public class Guard implements Persistable
{
	public static final String PropertyOwner = "owner";
	public static final String PropertyObjectId = "objectId";
	
	/**
     * The owner of the object this guard is associated with.
     */
	@Persistent(key = PropertyOwner)
    protected AuthToken owner;

    @Persistent (key = PropertyObjectId)
    protected ID objectId;
    
	public Guard()
    {
    }

	public void checkReserve(AuthToken requester, X509Certificate authorityCert, PrivateKey authorityKey) throws CertificateException {
    	checkPrivilege(requester, authorityCert, authorityKey, new String[]{AbacUtil.AbacRoleOwner});
    }

    public void checkUpdate(AuthToken requester, X509Certificate authorityCert, PrivateKey authorityKey) throws CertificateException {
    	checkPrivilege(requester, authorityCert, authorityKey, new String[]{AbacUtil.AbacRoleOwner});
    }
    
    public void checkPrivilege(AuthToken requester, X509Certificate authorityCert, PrivateKey authorityKey, String[] requiredPrivileges) throws CertificateException
    {
    	try{
    		AbacUtil.createObjectPolicy(authorityCert, authorityKey, AbacUtil.ActorTrustSliceAuthority, objectId, requiredPrivileges);
    		boolean result = AbacUtil.checkPrivilege(requester.getCertificate(), authorityCert, objectId, requiredPrivileges);
        	if(!result){
        		throw new CertificateException("User does not have the required privilege.");
        	}
    	}catch(Exception exception){
        	throw new CertificateException("User does not have the required privilege.");
    	}
    }
    
    public void checkOwner(AuthToken auth) throws Exception
    {
    	if(owner != null){
    		if(!owner.equals(auth)){
    			throw new Exception("Authorization Exception");
    		}
    	}
    }
    
    public void setOwner(AuthToken owner) {
    	//this.checkOwner(caller);
		this.owner = owner;
	}
    
    public void setObjectId(ID objectId){
    	this.objectId = objectId;
    }
}
