/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.proxies;

import net.exogeni.orca.shirako.container.ProtocolDescriptor;


public class ActorLocation
{
    /*
     * When location is null, the proxy code should derive the location using
     * the protocol descriptor and the actor name/guid.
     */
    protected String location;
    protected ProtocolDescriptor descriptor;

    public ActorLocation()
    {
    }

    public ActorLocation(String loc){
    	this.location = loc;
    }
    
    /**
     * @return the descriptor
     */
    public ProtocolDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    /**
     * @return the location
     */
    public String getLocation()
    {
        return this.location;
    }

    /**
     * @param descriptor the descriptor to set
     */
    public void setDescriptor(ProtocolDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location)
    {
        this.location = location;
    }
}
