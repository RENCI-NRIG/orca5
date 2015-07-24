/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.plugins.substrate;

import orca.shirako.api.IActor;
import orca.shirako.container.Globals;
import orca.tools.axis2.Axis2ClientSecurityConfigurator;


public class SubstrateTestWrapper extends Substrate
{
    @Override
    public void initializeKeyStore(IActor actor) throws Exception
    {
        Axis2ClientSecurityConfigurator configurator = Axis2ClientSecurityConfigurator.getInstance();
        
        if (configurator.createActorConfiguration(Globals.HomeDirectory,
                actor.getGuid().toString())!= 0){
            throw new Exception("Cannot create security configuration for actor");
        }
        
        super.initializeKeyStore(actor);
    }
}