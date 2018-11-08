/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.openflow;

import java.util.Properties;

import orca.manage.extensions.api.ManagerObject;
import orca.manage.extensions.api.PortalPluginDescriptor;
import orca.manage.extensions.standard.Converter;
import orca.manage.extensions.standard.controllers.ControllerFactory;
import orca.shirako.api.IActor;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.kernel.SliceFactory;

public class OpenFlowControllerFactory implements ControllerFactory, OpenFlowControllerConstants {
    public static final String PropertySliceName = "sliceName";

    protected IActor actor;
    protected OpenFlowController cont;
    protected OpenFlowControllerManagerObject managerObject;
    protected PortalPluginDescriptor desc;

    protected String sliceName = "openflow";

    protected OpenFlowController makeController() {
        return new OpenFlowController();
    }

    protected OpenFlowControllerManagerObject makeManagerObject() {
        return new OpenFlowControllerManagerObject(cont, actor);
    }

    public String getPortalPluginClassName() {
        return OpenFlowControllerPortalPlugin.class.getCanonicalName();
    }

    public void create() throws Exception {
        if (!(actor instanceof IServiceManager)) {
            throw new Exception("Invalid actor type");
        }

        if (sliceName == null) {
            throw new Exception("Missing slice name");
        }

        ISlice slice = (ISlice) SliceFactory.getInstance().create(sliceName);

        actor.registerSlice(slice);

        cont = makeController();
        cont.setActor(actor);
        cont.setSlice(slice);
        cont.initialize();

        // attach the controller to the slice
        slice.setController(cont);

        managerObject = makeManagerObject();
        managerObject.initialize();

        desc = new PortalPluginDescriptor();
        desc.setRootClassName(getPortalPluginClassName());
        desc.setManagerKey(managerObject.getID().toString());
        desc.setKey(managerObject.getID().toString());
        desc.setActorName(actor.getName());
        desc.setSliceName(sliceName);
        desc.setPackageId(MyPackageId);

        System.out.println("=== OpenFlowControllerFactory.ends...");
    }

    public void configure(Properties p) throws Exception {
        process(p);
    }

    public void configure(String p) throws Exception {
        Properties pp = Converter.decodeProperties(p);
        process(pp);
    }

    public ManagerObject getManager() {
        return managerObject;
    }

    public Object getObject() {
        return cont;
    }

    public PortalPluginDescriptor getRoot() {
        return desc;
    }

    protected void process(Properties p) throws Exception {
        if (p.getProperty(PropertySliceName) != null) {
            sliceName = p.getProperty(PropertySliceName);
        }
    }

    public void setActor(IActor actor) {
        this.actor = actor;
    }
}
