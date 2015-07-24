/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc;

import orca.manage.extensions.standard.controllers.ControllerPortalPlugin;

/**
 *
 * @author anirban
 */
public class XmlrpcControllerPortalPlugin extends ControllerPortalPlugin
{

    protected String MainTemplate = "/nlr/main.vm";

    public String getMainTemplate()
    {
        return MainTemplate;
    }

    protected IXmlrpcControllerManagementProxy getMyProxy()
    {
        return (IXmlrpcControllerManagementProxy) proxy;
    }

}
