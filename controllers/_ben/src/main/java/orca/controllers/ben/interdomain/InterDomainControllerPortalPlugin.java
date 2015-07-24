/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.ben.interdomain;

import orca.manage.extensions.standard.controllers.ControllerPortalPlugin;

/**
 *
 * @author anirban
 */
public class InterDomainControllerPortalPlugin extends ControllerPortalPlugin{

    protected String MainTemplate = "/interdomain/main.vm";

    @Override
    public String getMainTemplate()
    {
        return MainTemplate;
    }

    protected IInterDomainControllerManagementProxy getMyProxy()
    {
        return (IInterDomainControllerManagementProxy) proxy;
    }

}
