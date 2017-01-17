package orca.controllers.mock;

import orca.controllers.OrcaConnectionFactory;
import orca.manage.*;
import orca.manage.internal.ActorManagementObject;
import orca.manage.internal.ServiceManagerManagementObject;
import orca.manage.proxies.soap.SoapContainer;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.container.Globals;
import orca.util.ID;
import orca.util.ReflectionUtils;

import static orca.manage.Orca.PROTOCOL_SOAP;

public class MockOrcaConnectionFactory extends OrcaConnectionFactory {

    /**
     *
     * @param url
     * @param user
     * @param password
     * @param smGuid
     */
    public MockOrcaConnectionFactory(String url, String user, String password, ID smGuid) {
        super(url, user, password, smGuid);
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public synchronized IOrcaServiceManager getServiceManager() throws Exception {
        // obtain a proxy to the container.
        // NOTE: we must use reflection to avoid dependency on the internal
        // package.
        //container = (IOrcaContainer) ReflectionUtils.invokeStatic(
        //        "orca.manage.internal.local.LocalConnector", "connect", Thread.currentThread()
        //                .getContextClassLoader(), new Class<?>[] { AuthToken.class }, (Object) null);

        //String location = url.substring(PROTOCOL_SOAP.length());
        //container = new SoapContainer(OrcaConstants.ContainerManagmentObjectID, location, null);

        //container = Orca.connect();
        //IOrcaServiceManager sm = container.getServiceManager(smGuid);

        ServiceManagerManagementObject manager = new ServiceManagerManagementObject();
        manager.initialize();
        AuthToken authToken = new AuthToken();
        IOrcaServiceManager sm = new MockOrcaServiceManager(manager, authToken);
        //manager.setActor((IActor) sm); // TODO: need to be able to call this method.

        if (null == sm) {
            throw new Exception("getServiceManager returned null SM");
        }

        return sm;
    }
}