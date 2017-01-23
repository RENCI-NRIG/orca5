package orca.controllers.mock;

import orca.controllers.OrcaConnectionFactory;
import orca.manage.IOrcaServiceManager;
import orca.manage.beans.TicketReservationMng;
import orca.manage.internal.ServiceManagerManagementObject;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.util.ID;

import java.util.HashMap;
import java.util.Map;

public class MockOrcaConnectionFactory extends OrcaConnectionFactory {

    protected final boolean failReservation;
    protected Map<ReservationID, TicketReservationMng> reservationMap;

    /**
     *
     * @param url
     * @param user
     * @param password
     * @param smGuid
     */
    public MockOrcaConnectionFactory(String url, String user, String password, ID smGuid) {
        // empty reservationMap
        this(url, user, password, smGuid, null, false);
    }

    public MockOrcaConnectionFactory(String url, String user, String password, ID id, Map<ReservationID, TicketReservationMng> reservationMap) {
        this(url, user, password, id, reservationMap, false);
    }

    public MockOrcaConnectionFactory(String url, String user, String password, ID id, Map<ReservationID, TicketReservationMng> reservationMap, boolean failReservation) {
        super(url, user, password, id);
        if (null != reservationMap) {
            this.reservationMap = reservationMap;
        } else {
            this.reservationMap = new HashMap<>();
        }
        this.failReservation = failReservation;
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

        //ServiceManager serviceManager = new ServiceManager();
        //manager.setActor(serviceManager); // TODO: need to be able to call this method.

        AuthToken authToken = new AuthToken();
        IOrcaServiceManager sm;// = new MockOrcaServiceManager(manager, authToken);

        sm = new MockOrcaServiceManager(manager, authToken, reservationMap, failReservation);

        if (null == sm) {
            throw new Exception("getServiceManager returned null SM");
        }

        return sm;
    }
}