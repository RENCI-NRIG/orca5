/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.ben.simple;

import orca.controllers.ben.LocalBenControllerManagementProxy;
import orca.controllers.ben.simple.BenSimpleControllerManagerObject.ResultBenRequestMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.internal.ManagementObject;
import orca.security.AuthToken;

public abstract class LocalBenSimpleControllerManagementProxy extends LocalBenControllerManagementProxy implements BenSimpleControllerManagementProxy {
    /**
     * The controller manager object.
     */
    private BenSimpleControllerManagerObject manager;

    public LocalBenSimpleControllerManagementProxy() {
    }

    public LocalBenSimpleControllerManagementProxy(BenSimpleControllerManagerObject manager) {
        super(manager);
        this.manager = manager;
    }

    @Override
    public void setManagerObject(ManagementObject manager) {
        super.setManagerObject(manager);
        this.manager = (BenSimpleControllerManagerObject) manager;
    }

    public ResultStringMng addRequest(String start, String end, int vmsDuke, int vmsRenci, AuthToken caller) throws Exception {
        return manager.addRequest(start, end, vmsDuke, vmsRenci, caller);
    }

    public ResultBenRequestMng getRequests(AuthToken caller) throws Exception {
        return manager.getRequests(caller);
    }

    public ResultBenRequestMng getRequests(String id, AuthToken caller) throws Exception {
        return manager.getRequests(id, caller);
    }
}
