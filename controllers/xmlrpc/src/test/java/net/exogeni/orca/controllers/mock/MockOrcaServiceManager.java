package net.exogeni.orca.controllers.mock;

import net.exogeni.orca.embed.EmbedTestHelper;
import net.exogeni.orca.embed.workflow.Domain;
import net.exogeni.orca.manage.OrcaConverter;
import net.exogeni.orca.manage.beans.*;
import net.exogeni.orca.manage.internal.Converter;
import net.exogeni.orca.manage.internal.ManagementObject;
import net.exogeni.orca.manage.internal.local.LocalServiceManager;
import net.exogeni.orca.ndl.NdlException;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolsDescriptor;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.PropList;

import java.io.IOException;
import java.util.*;

import static net.exogeni.orca.manage.OrcaConstants.ReservationStateFailed;

/**
 * A messy attempt at an OrcaServiceManager that doesn't need to talk to 'Live' servers.
 */
public class MockOrcaServiceManager extends LocalServiceManager {

    protected final boolean failReservation;
    protected Map<ReservationID, TicketReservationMng> reservationMap;

    /**
     * stolen from CloudHandlerTest.java
     */
    protected Map<Domain, Map<String, Integer>> resourceMap;

    /*
     * the tests go a lot quicker with this as a static assignment, but TS3-3 and others fail when not run
     * independently, somehow, because the 'ben' domain gets messed up, resulting in a NPE at
     * orca.ndl.elements.NetworkElement.getRank(NetworkElement.java:268)
     */
    private void populateResourceMap() {
        resourceMap = new HashMap<>();
        Domain domain;
        HashMap<String, Integer> resource;

        try {
            /*
             * VLAN only domains, will all use the same copy of the resource map
             */
            resource = new HashMap<>();
            resource.put("site.vlan", 9);

            domain = new Domain("net/exogeni/orca/ndl/substrate/bbnNet.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/ben-6509.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/fiuNet.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/gwuNet.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/ion.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/nlr.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/pscNet.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/rciNet.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/uhNet.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/wvnNet.rdf");
            resourceMap.put(domain, resource);

            /*
             * Add VMs to resources
             */
            resource = new HashMap<>();
            resource.put("site.vlan", 9);
            resource.put("site.vm", 5);

            domain = new Domain("net/exogeni/orca/ndl/substrate/bbnvmsite.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/gwuvmsite.rdf");
            resourceMap.put(domain, resource);

            // domain = new Domain("net/exogeni/orca/ndl/substrate/pscvmsite.rdf");
            // resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/uhvmsite.rdf");
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/wvnvmsite.rdf");
            resourceMap.put(domain, resource);

            /*
             * These need to be more custom
             */
            domain = new Domain("net/exogeni/orca/ndl/substrate/fiuvmsite.rdf");
            resource = new HashMap<>();
            resource.put("site.vm", 36);
            resource.put("site.vlan", 9);
            resource.put("site.lun", 100);
            resource.put("site.baremetalce", 2);
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/pscvmsite.rdf");
            resource = new HashMap<>();
            resource.put("site.vm", 150);
            resource.put("site.vlan", 8);
            resource.put("site.baremetalce", 2);
            resourceMap.put(domain, resource);

            domain = new Domain("net/exogeni/orca/ndl/substrate/rcivmsite.rdf");
            resource = new HashMap<>();
            resource.put("site.vm", 101);
            resource.put("site.vlan", 8);
            resourceMap.put(domain, resource);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NdlException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param manager
     *            passed to super
     * @param authToken
     *            passed to super
     * @param reservationMap
     *            a Map of fake reservations that any new SM will have. Useful for testing modifySlice()
     * @param failReservation
     *            an indicator to any SM created whether it should fail any reservations
     */
    public MockOrcaServiceManager(ManagementObject manager, AuthToken authToken,
            Map<ReservationID, TicketReservationMng> reservationMap, boolean failReservation) {
        super(manager, authToken);
        this.reservationMap = reservationMap;
        this.failReservation = failReservation;
        if (null == resourceMap) {
            populateResourceMap();
        }
    }

    /**
     * Just adds new SliceID to slice, does not save any details.
     *
     * @param slice
     *            new ID is added to slice, but not saved in SM
     * @return
     */
    @Override
    public SliceID addSlice(SliceMng slice) {
        SliceID sliceID = new SliceID();
        slice.setSliceID(sliceID.toString());
        return sliceID;
        // ResultStringMng resultStringMng = manager.addSlice(slice, null); // can the manager be made to track this for
        // us?
        // resultStringMng.
    }

    /**
     * Provides a static list of pool resources
     *
     * @param broker
     *            is ignored
     * @return
     */
    @Override
    public List<PoolInfoMng> getPoolInfo(ID broker) {
        List<String> abstractModels = new ArrayList<>();
        ResourcePoolsDescriptor pools = new ResourcePoolsDescriptor();
        List<PoolInfoMng> poolInfoMngs = new ArrayList<>();

        try {
            EmbedTestHelper.populateModelsAndPools(abstractModels, pools, resourceMap);
            // cloudHandler.addSubstrateModel(abstractModels);

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
     * Add reservation to our maintained reservationMap
     *
     * @param reservation
     *            new ReservationID is added to reservation
     * @return new ReservationID
     */
    @Override
    public ReservationID addReservation(TicketReservationMng reservation) {
        ReservationID reservationID = new ReservationID();

        // check for reservationID from request
        String guid = OrcaConverter.getConfigurationProperty(reservation, "elment.GUID");
        if (null != guid) {
            reservationID = new ReservationID(guid);
        }

        reservation.setReservationID(reservationID.toString());
        reservationMap.put(reservationID, reservation);
        return reservationID;
    }

    /**
     *
     * @param reservationID
     *            used to find reservation
     * @return the reservation matching ID
     */
    @Override
    public ReservationMng getReservation(ReservationID reservationID) {
        return reservationMap.get(reservationID);
    }

    /**
     *
     * @param sliceID
     *            is ignored
     * @return all reservations from this (Mock) SM
     */
    @Override
    public List<ReservationMng> getReservations(SliceID sliceID) {
        // fail one of the reservations in the Test
        if (failReservation) {
            for (ReservationMng reservation : reservationMap.values()) {
                reservation.setState(ReservationStateFailed);
                break;
            }
        }

        return new ArrayList<ReservationMng>(reservationMap.values());
    }

    /**
     * Always returns true
     *
     * @param reservation
     *            ignored
     * @return true
     */
    @Override
    public boolean demand(ReservationMng reservation) {
        return true;
    }

    /**
     * This is currently only called in Test from testModifySliceWithModifyRemove, and that code path only seems to be
     * looking for two properties.
     *
     * @param reservationID
     * @return
     */
    @Override
    public List<UnitMng> getUnits(ReservationID reservationID) {
        List<UnitMng> unitMngList = new ArrayList<>();
        UnitMng unit = new UnitMng();
        PropertiesMng mng = new PropertiesMng();

        ReservationMng reservation = getReservation(reservationID);
        for (PropertyMng propertyMng : reservation.getConfigurationProperties().getProperty()) {
            if (propertyMng.getName().equals("unit.vlan.url")) {
                mng.getProperty().add(propertyMng);
                PropertyMng propertyMng1 = new PropertyMng();
                propertyMng1.setName("unit.vlan.tag");
                propertyMng1.setValue("137"); // can probably be anything
                mng.getProperty().add(propertyMng1);
            }
        }

        unit.setProperties(mng);
        unitMngList.add(unit);

        return unitMngList;
    }

    /**
     * This is currently only called in Test from testModifySliceWithModifyRemove, but it doesn't really seem to change
     * the result of the test whether or not this function is implemented. (Besides cleaning up some error logs).
     *
     * Stealing code ServiceManager.modify()
     */
    @Override
    public boolean modifyReservation(ReservationID reservationID, Properties modifyProperties) {
        LeaseReservationMng reservation = (LeaseReservationMng) getReservation(reservationID);

        // Merging modifyProperties into ConfigurationProperties
        PropertiesMng configurationProperties = reservation.getConfigurationProperties();
        Properties currConfigProps = OrcaConverter.fill(configurationProperties);
        PropList.mergePropertiesPriority(modifyProperties, currConfigProps);
        reservation.setConfigurationProperties(OrcaConverter.fill(currConfigProps));

        return true;
    }

    /**
     * We do not currently have any tests where we need this to fail.
     *
     * @param reservation
     *            ignored
     * @param newEndTime
     *            ignored
     * @return always true
     */
    @Override
    public boolean extendReservation(ReservationID reservation, Date newEndTime) {
        return true;
    }

}
