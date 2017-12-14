package orca.manage;

import java.security.cert.X509Certificate;
import java.util.List;

import junit.framework.Assert;
import orca.manage.beans.ClientMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.shirako.common.SliceID;
import orca.util.ID;

import org.junit.Test;

public abstract class ServerActorTest extends ActorTest {
    public ServerActorTest(ID actorGuid, String actorName) {
        super(actorGuid, actorName);
    }

    protected IOrcaServerActor getServerActor() {
        return (IOrcaServerActor) getActor();
    }

    @Test
    public void testGetBrokerReservations() {
        IOrcaServerActor actor = getServerActor();
        List<ReservationMng> list = actor.getBrokerReservations();
        Assert.assertNotNull(list);
    }

    @Test
    public void testGetInventorySlices() {
        IOrcaServerActor actor = getServerActor();
        List<SliceMng> list = actor.getInventorySlices();
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() >= 1);

        for (SliceMng s : list) {
            List<ReservationMng> rs = actor.getInventoryReservations(new SliceID(s.getSliceID()));
            Assert.assertNotNull(rs);
            Assert.assertTrue(rs.size() > 0);
        }
    }

    @Test
    public void testGetInventoryReservations() {
        IOrcaServerActor actor = getServerActor();
        List<ReservationMng> list = actor.getInventoryReservations();
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() >= 1);
    }

    @Test
    public void testGetClientSlices() {
        IOrcaServerActor actor = getServerActor();
        List<SliceMng> list = actor.getClientSlices();
        Assert.assertNotNull(list);
        if (actor.getName().equals(SITE_NAME)) {
            Assert.assertTrue(list.size() > 0);
        }
    }

    @Test
    public void testGetClients() {
        IOrcaServerActor actor = getServerActor();
        List<ClientMng> list = actor.getClients();
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() > 0);
    }

    @Test
    public void testGetClient() {
        IOrcaServerActor actor = getServerActor();
        List<ClientMng> list = actor.getClients();
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() > 0);
        for (ClientMng c : list) {
            ClientMng client = actor.getClient(new ID(c.getGuid()));
            Assert.assertNotNull(client);
            Assert.assertEquals(c.getGuid(), client.getGuid());
            Assert.assertEquals(c.getName(), client.getName());
        }
    }

    @Test
    public void testGetClientCertificate() {
        IOrcaServerActor actor = getServerActor();
        List<ClientMng> list = actor.getClients();
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() > 0);
        for (ClientMng c : list) {
            ClientMng client = actor.getClient(new ID(c.getGuid()));
            Assert.assertNotNull(client);
            Assert.assertEquals(c.getGuid(), client.getGuid());
            Assert.assertEquals(c.getName(), client.getName());
            X509Certificate cert = (X509Certificate) actor.getClientCertificate(new ID(c.getGuid()));
            Assert.assertNotNull(cert);
            System.out.println(cert.getSubjectDN());
        }
    }
}