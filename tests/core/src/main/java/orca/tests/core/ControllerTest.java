package orca.tests.core;

import java.util.Properties;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.manage.OrcaManagementException;
import orca.manage.extensions.standard.actors.ServiceManagerManagerObject;
import orca.shirako.api.IActor;
import orca.shirako.api.IController;
import orca.shirako.api.IServiceManager;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.container.api.IActorContainer;
import orca.shirako.registry.ActorRegistry;

public class ControllerTest extends ShirakoTest {
    /**
     * Slice we created for the controller.
     */
    protected SliceID sliceId;
    /**
     * Actor owning the controller.
     */
    protected IServiceManager actor;

    public ControllerTest(String[] args) {
        super(args);
    }

    protected IController getController() {
        assert actor != null;
        assert sliceId != null;
        return actor.getSlice(sliceId).getController();
    }

    protected void startController(String actorName, PackageId packageId, PluginId controllerId, Properties cp)
            throws Exception {
        if (actorName == null || packageId == null || controllerId == null) {
            throw new IllegalArgumentException();
        }

        // resolve the actor
        IActor temp = ActorRegistry.getActor(actorName);
        if (temp == null) {
            throw new RuntimeException("Actor: " + actorName + " does not exist in this container");
        }
        if (!(temp instanceof IServiceManager)) {
            throw new RuntimeException("Actor: " + actorName + " is not a service maneger");
        }
        actor = (IServiceManager) temp;

        // the manager object gives us access to the management interface
        IActorContainer manager = Globals.getContainer();
        // get the management object for the actor
        ServiceManagerManagerObject man = (ServiceManagerManagerObject) manager.getManagementObjectManager()
                .getManagementObject(actor.getGuid());
        if (man == null) {
            throw new RuntimeException("Could not obtain management object for actor " + actorName);
        }

        try {
            sliceId = man.addApplication(packageId, controllerId, cp);
        } catch (OrcaManagementException e) {
            throw new RuntimeException("An error occurred while creating controller. Error code=" + e.getErrorCode(),
                    e);
        }
    }
}
