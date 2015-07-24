/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.ben.interdomain;

import orca.controllers.ben.interdomain.InterDomainControllerManagerObject.ResultInterDomainRequestMng;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.extensions.standard.controllers.proxies.local.LocalControllerManagementProxy;
import orca.manage.internal.ManagementObject;
import orca.security.AuthToken;

/**
 *
 * @author anirban
 */
public class LocalInterDomainControllerManagementProxy extends LocalControllerManagementProxy implements IInterDomainControllerManagementProxy{

    protected InterDomainControllerManagerObject manager;

    public LocalInterDomainControllerManagementProxy()
    {
    }

    public LocalInterDomainControllerManagementProxy(InterDomainControllerManagerObject manager)
    {
        this.manager = manager;
    }

    @Override
    public void setManagerObject(ManagementObject manager)
    {
        this.manager = (InterDomainControllerManagerObject) manager;
    }

    public ResultStringMng addRequest(String ndl, String start, String end, AuthToken caller) throws Exception
    {
        return manager.addRequest(ndl, start, end, caller);
    }

    public ResultInterDomainRequestMng getRequests(AuthToken caller) throws Exception
    {
        return manager.getRequests(caller);
    }

    public ResultInterDomainRequestMng getRequests(String id, AuthToken caller) throws Exception
    {
        return manager.getRequests(id, caller);
    }

    public ResultMng closeRequest(String id, AuthToken caller) throws Exception {
        return manager.closeRequest(id, caller);
    }


}
