/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc;

import java.util.Properties;

import orca.extensions.IControllerFactory;
import orca.extensions.PortalPluginDescriptor;
import orca.manage.extensions.standard.Converter;
import orca.manage.internal.ManagementObject;
import orca.shirako.api.IActor;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.kernel.SliceFactory;

/**
 *
 * @author anirban
 */
public class XmlrpcControllerFactory implements IControllerFactory
{

    public static final String PropertySliceName = "sliceName";
    protected IActor actor;
    protected XmlrpcController cont;
    protected XmlrpcControllerManagerObject xmlrpcControllerManagerObject;
    protected PortalPluginDescriptor desc;
    protected String sliceName = "xmlrpc";

    public void configure(Properties p) throws Exception
    {
        process(p);
    }

    public void configure(String p) throws Exception
    {
        Properties pp = Converter.decodeProperties(p);
        process(pp);
    }

    public void create() throws Exception
    {

        if (!(actor instanceof IServiceManager)) {
            throw new Exception("Invalid actor type");
        }

        if (sliceName == null) {
            throw new Exception("Missing slice name");
        }

        ISlice slice = (ISlice) SliceFactory.getInstance().create(sliceName);
        actor.registerSlice(slice);

        cont = new XmlrpcController();
        cont.setActor(actor);
        cont.setSlice(slice);
        cont.initialize();
        // attach the controller to the slice
        slice.setController(cont);

        xmlrpcControllerManagerObject = new XmlrpcControllerManagerObject(cont, actor);
        xmlrpcControllerManagerObject.initialize();
        

        desc = new PortalPluginDescriptor();
        desc.setRootClassName(XmlrpcControllerPortalPlugin.class.getCanonicalName());
        desc.setManagerKey(xmlrpcControllerManagerObject.getID().toString());
        desc.setKey(xmlrpcControllerManagerObject.getID().toString());
        desc.setActorName(actor.getName());
        desc.setSliceName(sliceName);
        desc.setPackageId(XmlrpcControllerConstants.PackageId);


    }

    public ManagementObject getManager()
    {
        return xmlrpcControllerManagerObject;
    }

    public Object getObject()
    {
        return cont;
    }

    public PortalPluginDescriptor getRoot()
    {
        return desc;
    }

    protected void process(Properties p) throws Exception
    {
        if (p.getProperty(PropertySliceName) != null) {
            sliceName = p.getProperty(PropertySliceName);
        }
    }

    public void setActor(IActor actor)
    {
        this.actor = actor;
    }

}
