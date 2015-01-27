/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.ben;

import orca.manage.extensions.standard.beans.ResultProxyMng;
import orca.manage.extensions.standard.controllers.proxies.local.LocalControllerManagementProxy;
import orca.manage.internal.ManagementObject;
import orca.security.AuthToken;

public class LocalBenControllerManagementProxy extends LocalControllerManagementProxy implements IBenControllerManagementProxy
{
    /**
     * The controller manager object.
     */
    private BenControllerManagerObject manager;

    public LocalBenControllerManagementProxy()
    {
    }

    public LocalBenControllerManagementProxy(BenControllerManagerObject manager)
    {
        super(manager);
        this.manager = manager;
    }

    @Override
    public void setManagerObject(ManagementObject manager)
    {
        super.setManagerObject(manager);
        this.manager = (BenControllerManagerObject) manager;
    }


    public ResultProxyMng getVMBroker(AuthToken caller) throws Exception
    {
        return manager.getVMBroker(caller);
    }

    public ResultProxyMng getVlanBroker(AuthToken caller) throws Exception
    {
        return manager.getVlanBroker(caller);
    }    
}
