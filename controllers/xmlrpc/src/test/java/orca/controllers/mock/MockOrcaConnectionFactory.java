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
        ServiceManagerManagementObject manager = new ServiceManagerManagementObject();
        manager.initialize();

        // our lives might have been easier if we could get ServiceManager to do part of our work.
        //ServiceManager serviceManager = new ServiceManager();
        //manager.setActor(serviceManager);

        AuthToken authToken = new AuthToken();
        IOrcaServiceManager sm;// = new MockOrcaServiceManager(manager, authToken);

        sm = new MockOrcaServiceManager(manager, authToken, reservationMap, failReservation);

        if (null == sm) {
            throw new Exception("getServiceManager returned null SM");
        }

        return sm;
    }
}