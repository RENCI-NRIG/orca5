package orca.controllers.ben;

import java.util.Properties;

import orca.extensions.IControllerFactory;
import orca.extensions.PortalPluginDescriptor;
import orca.manage.extensions.standard.Converter;
import orca.manage.internal.ManagementObject;
import orca.shirako.api.IActor;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.kernel.SliceFactory;

public abstract class BenControllerFactory implements IControllerFactory, BenConstants {
    public static final String PropertySliceName = "sliceName";
    protected IActor actor;
    protected BenController cont;
    protected BenControllerManagerObject managerObject;
    protected PortalPluginDescriptor desc;
    protected String sliceName = "ben";

    protected abstract BenController makeController();

    protected abstract BenControllerManagerObject makeManagerObject();

    protected abstract String getPortalPluginClassName();

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
    }

    public void configure(Properties p) throws Exception {
        process(p);
    }

    public void configure(String p) throws Exception {
        Properties pp = Converter.decodeProperties(p);
        process(pp);
    }

    public ManagementObject getManager() {
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