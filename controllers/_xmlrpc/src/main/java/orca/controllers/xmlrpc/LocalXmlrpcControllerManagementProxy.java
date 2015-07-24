/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc;

import orca.manage.extensions.standard.controllers.proxies.local.LocalControllerManagementProxy;
import orca.security.AuthToken;

/**
 *
 * @author anirban
 */
public class LocalXmlrpcControllerManagementProxy extends LocalControllerManagementProxy implements IXmlrpcControllerManagementProxy
{

    /**
     * The controller manager object.
     */
    protected XmlrpcControllerManagerObject manager;

    public LocalXmlrpcControllerManagementProxy()
    {
    }

    public LocalXmlrpcControllerManagementProxy(XmlrpcControllerManagerObject manager)
    {
        super(manager);
        this.manager = manager;
    }

    public void disableController(AuthToken auth) throws Exception
    {
        manager.disableController(auth);
    }

    public void enableController(AuthToken auth) throws Exception
    {
        manager.enableController(auth);
    }

    public boolean isControllerEnabled(AuthToken auth) throws Exception
    {
        return manager.isControllerEnabled(auth);
    }

}
