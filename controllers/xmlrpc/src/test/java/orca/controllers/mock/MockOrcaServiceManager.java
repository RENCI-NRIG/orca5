package orca.controllers.mock;

import orca.embed.EmbedTestHelper;
import orca.embed.workflow.Domain;
import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.internal.Converter;
import orca.manage.internal.ManagementObject;
import orca.manage.internal.ServiceManagerManagementObject;
import orca.manage.internal.local.LocalServiceManager;
import orca.ndl.NdlException;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.util.ID;

import java.io.IOException;
import java.util.*;

public class MockOrcaServiceManager extends LocalServiceManager {

    protected Map<ReservationID, TicketReservationMng> reservationMap;

    /**
     * stolen from CloudHandlerTest.java
     */
    protected static Map<Domain, Map<String, Integer>> resourceMap;
    static {
        resourceMap = new HashMap<>();
        Domain domain;

        try {
            //domain = new Domain("orca/ndl/substrate/mass.rdf");
            domain = new Domain("orca/ndl/substrate/uvanlvmsite.rdf");

            HashMap<String, Integer> resource = new HashMap<>();
            resource.put("site.vm", 8); // this must be smaller than the test we expect to fail (4 XO Xlarge)
            resource.put("site.vlan", 2);
            //resource.put("site.lun", 2);

            resourceMap.put(domain, resource);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NdlException e) {
            e.printStackTrace();
        }
    }

    public MockOrcaServiceManager(ManagementObject manager, AuthToken auth) {
        super(manager, auth);
        reservationMap = new HashMap<>();
    }

    public MockOrcaServiceManager(ServiceManagerManagementObject manager, AuthToken authToken, Map<ReservationID, TicketReservationMng> reservationMap) {
        super(manager, authToken);
        this.reservationMap = reservationMap;
    }

    /**
     *
     * @param slice is ignored
     * @return
     */
    @Override
    public SliceID addSlice(SliceMng slice) {
        SliceID sliceID = new SliceID();
        slice.setSliceID(sliceID.toString());
        return sliceID;
        //ResultStringMng resultStringMng = manager.addSlice(slice, null);
        //resultStringMng.
    }

    /**
     *
     * @param broker is ignored
     * @return
     */
    @Override
    public List<PoolInfoMng> getPoolInfo(ID broker) {
        List<String> abstractModels = new ArrayList<>();
        ResourcePoolsDescriptor pools = new ResourcePoolsDescriptor();
        List<PoolInfoMng> poolInfoMngs = new ArrayList<>();

        try {
            EmbedTestHelper.populateModelsAndPools(abstractModels, pools, resourceMap);
            //cloudHandler.addSubstrateModel(abstractModels);


            // from ClientActorManagementObjectHelper.java
            for (ResourcePoolDescriptor resourcePoolDescriptor : pools) {
                Properties temp = new Properties();
                resourcePoolDescriptor.save(temp, null);
                PoolInfoMng poolInfoMng = new PoolInfoMng();
                poolInfoMng.setType(resourcePoolDescriptor.getResourceType().toString());
                poolInfoMng.setName(resourcePoolDescriptor.getResourceTypeLabel());
                poolInfoMng.setProperties(Converter.fill(temp));
                poolInfoMngs.add(poolInfoMng);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NdlException e) {
            e.printStackTrace();
        }

        return poolInfoMngs;
    }

    /**
     *
     * @param reservation
     * @return
     */
    @Override
    public ReservationID addReservation(TicketReservationMng reservation) {
        ReservationID reservationID = new ReservationID();
        reservation.setReservationID(reservationID.toString());
        reservationMap.put(reservationID, reservation);
        return reservationID;
    }

    /**
     *
     * @param reservationID
     * @return
     */
    @Override
    public ReservationMng getReservation(ReservationID reservationID) {
        return reservationMap.get(reservationID);
    }

    /**
     *
     * @param sliceID is ignored
     * @return
     */
    @Override
    public List<ReservationMng> getReservations(SliceID sliceID) {
        return new ArrayList<ReservationMng>(reservationMap.values());
    }

    /**
     *
     * @param reservation
     * @return
     */
    @Override
    public boolean demand(ReservationMng reservation) {
        return true;
    }
}
