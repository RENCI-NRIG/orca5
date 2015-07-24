/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc;


import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.extensions.api.ManageExtensionsApiConstants;
import orca.manage.extensions.api.proxies.soapaxis2.SoapAxis2ManagerObjectProxy;
import orca.manage.extensions.standard.controllers.ControllerManagerObject;
import orca.security.AuthToken;
import orca.shirako.api.IActor;


/**
 *
 * @author anirban
 */
public class XmlrpcControllerManagerObject extends ControllerManagerObject
{

    protected void registerProtocols()
    {
        OrcaProxyProtocolDescriptor p1 = new OrcaProxyProtocolDescriptor(ManageExtensionsApiConstants.ProtocolLocal, LocalXmlrpcControllerManagementProxy.class.getCanonicalName());
        OrcaProxyProtocolDescriptor p2 = new OrcaProxyProtocolDescriptor("soapaxis2", SoapAxis2ManagerObjectProxy.class.getCanonicalName());
        proxies = new OrcaProxyProtocolDescriptor[] { p1 , p2 };
        //proxies = new ProxyProtocolDescriptor[] { p1 };
    }

    public XmlrpcControllerManagerObject(XmlrpcController controller, IActor actor)
    {
        super(controller, actor);
    }

    public void disableController(AuthToken auth)
    {
        ((XmlrpcController) controller).disableController();
    }

    public void enableController(AuthToken auth)
    {
        ((XmlrpcController) controller).enableController();
    }

    public boolean isControllerEnabled(AuthToken auth)
    {
        return ((XmlrpcController) controller).isControllerEnabled();
    }

}
