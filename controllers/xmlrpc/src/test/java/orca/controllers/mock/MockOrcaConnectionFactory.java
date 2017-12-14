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

/**
 * A messy attempt at an OrcaConnectionFactory that doesn't need to talk to 'Live' servers. The main thing is that it
 * returns a MockOrcaServiceManager.
 */
public class MockOrcaConnectionFactory extends OrcaConnectionFactory {

    protected final boolean failReservation;
    protected Map<ReservationID, TicketReservationMng> reservationMap;

    /**
     * Jump-start a new SM with some fake reservations already in place, as well as an indicator to the (Mock) SM
     * whether it should fail any reservations.
     *
     * @param url
     *            passed to super
     * @param user
     *            passed to super
     * @param password
     *            passed to super
     * @param id
     *            passed to super
     * @param reservationMap
     *            a Map of fake reservations that any new SM will have. Useful for testing modifySlice()
     * @param failReservation
     *            an indicator to any SM created whether it should fail any reservations
     */
    public MockOrcaConnectionFactory(String url, String user, String password, ID id,
            Map<ReservationID, TicketReservationMng> reservationMap, boolean failReservation) {
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
     * @return a MockOrcaServiceManager
     * @throws Exception
     */
    public synchronized IOrcaServiceManager getServiceManager() throws Exception {
        ServiceManagerManagementObject manager = new ServiceManagerManagementObject();
        manager.initialize();

        // our lives might have been easier if we could get ServiceManager to do part of our work.
        // ServiceManager serviceManager = new ServiceManager();
        // manager.setActor(serviceManager);

        AuthToken authToken = new AuthToken();
        IOrcaServiceManager sm;// = new MockOrcaServiceManager(manager, authToken);

        sm = new MockOrcaServiceManager(manager, authToken, reservationMap, failReservation);

        if (null == sm) {
            throw new Exception("getServiceManager returned null SM");
        }

        return sm;
    }
}