package orca.manage;

import java.util.Date;
import java.util.List;

import junit.framework.Assert;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;

import org.junit.Test;

public abstract class ExportClaimTest extends ManagementTest {
    @Test
    public void testExport() {
        IOrcaContainer cont = connect();
        IOrcaAuthority site = cont.getAuthority(SITE_GUID);
        Assert.assertNotNull(site);
        IOrcaBroker broker = cont.getBroker(BROKER_GUID);
        Assert.assertNotNull(broker);

        List<SliceMng> slices = site.getInventorySlices();
        Assert.assertNotNull(slices);
        Assert.assertTrue(slices.size() >= 1);

        SliceID sliceId = new SliceID(slices.get(0).getSliceID());
        List<ReservationMng> tickets = site.getInventoryReservations(sliceId);
        Assert.assertNotNull(tickets);
        Assert.assertTrue(tickets.size() >= 1);

        ReservationID rid = new ReservationID(tickets.get(0).getReservationID());

        Date start = new Date();
        Date end = new Date(start.getTime() + 100000);

        ReservationID exported = site.exportResources(sliceId, start, end, 10, null, null, rid,
                new AuthToken(BROKER_NAME, BROKER_GUID));
        ReservationMng exp = site.getReservation(exported);
        Assert.assertNotNull(exp);

        System.out.println(
                "Exported reservation: rid=" + exp.getReservationID() + " rtype=" + exp.getResourceType() + " units="
                        + exp.getUnits() + " start=" + (new Date(exp.getStart())) + " end=" + (new Date(exp.getEnd())));

        Assert.assertNotNull(exported);
        System.out.println("Exported rid=" + exported);

        List<SliceMng> brokerInventorySlices = broker.getInventorySlices();
        Assert.assertNotNull(brokerInventorySlices);
        Assert.assertTrue(brokerInventorySlices.size() >= 1);

        // SliceID poolID = new SliceID(brokerInventorySlices.get(0).getSliceID());

        ReservationMng claimed = broker.claimResources(SITE_GUID, exported);
        Assert.assertNotNull(claimed);
        System.out.println("Claimed reservation: rid=" + claimed.getReservationID() + " rtype="
                + claimed.getResourceType() + " units=" + claimed.getUnits() + " start="
                + (new Date(claimed.getStart())) + " end=" + (new Date(claimed.getEnd())));
    }
}