/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.plugins.config;

import java.security.SecureRandom;

import org.apache.tools.ant.Project;


public class SliceProject extends Project
{
    Object token;
    Object actorConfiguraLock;
    Object semaphoreMap;
    SecureRandom sr;
    
    public SliceProject()
    {
        this(null);
    }

    public SliceProject(Object token)
    {
        this.token = token;
        this.sr = new SecureRandom();
    }

    public SliceProject(Object token, Object actorConfigurationLock, Object map, SecureRandom sr)
    {
        this.token = token;
        this.actorConfiguraLock = actorConfigurationLock;
        this.semaphoreMap = map;
        this.sr = sr;
    }

    
    public Object getToken()
    {
        return token;
    }

    public void setToken(Object token)
    {
        this.token = token;
    }
    
    public Object getActorConfigurationLock()
    {
        return actorConfiguraLock;
    }
    
    public void setSemaphoreMap(Object map) {
    	this.semaphoreMap = map;
    }
    
    public Object getSemaphoreMap() {
    	return this.semaphoreMap;
    }
    
    public int getRandomInt() {
    	return sr.nextInt();
    }
    
    public int getRandomInt(int n) {
    	return sr.nextInt(n);
    }
    
    public long getRandomLong() {
    	return sr.nextLong();
    }
    
    public void getRandomBytes(byte[] b) {
    	sr.nextBytes(b);
    }
}
