package orca.manage;

import java.security.cert.X509Certificate;
import java.util.List;

import junit.framework.Assert;
import orca.manage.beans.ActorMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.UserMng;
import orca.util.ID;

import org.junit.Test;

public abstract class ContainerTest extends ManagementTest {
    @Test
    public void testConnect() throws Exception {
        IOrcaContainer cont = connect();
        Assert.assertTrue(cont.isLogged());
    }

    @Test
    public void testGetCertificate() throws Exception {
        IOrcaContainer cont = connect();
        X509Certificate cert = (X509Certificate) cont.getCertificate();
        Assert.assertNotNull(cert);
        Assert.assertEquals("admin", getCN(cert.getSubjectDN().toString()));
    }

    @Test
    public void testGetActorCertificate() throws Exception {
        IOrcaContainer cont = connect();

        X509Certificate cert = (X509Certificate) cont.getCertificate(BROKER_GUID);
        Assert.assertNotNull(cert);
        Assert.assertEquals(BROKER_GUID.toString(), getCN(cert.getSubjectDN().toString()));

        cert = (X509Certificate) cont.getCertificate(SITE_GUID);
        Assert.assertNotNull(cert);
        Assert.assertEquals(SITE_GUID.toString(), getCN(cert.getSubjectDN().toString()));

        cert = (X509Certificate) cont.getCertificate(SM_GUID);
        Assert.assertNotNull(cert);
        Assert.assertEquals(SM_GUID.toString(), getCN(cert.getSubjectDN().toString()));
    }

    @Test
    public void testGetUsers() throws Exception {
        IOrcaContainer cont = connect();
        List<UserMng> users = cont.getUsers();
        Assert.assertNotNull(users);
        for (UserMng user : users) {
            Assert.assertNotNull(user.getLogin());
            UserMng u = cont.getUser(user.getLogin());
            Assert.assertNotNull(u);
            Assert.assertEquals(user.getFirst(), u.getFirst());
            Assert.assertEquals(user.getLogin(), u.getLogin());
        }
    }

    @Test
    public void testGetActors() throws Exception {
        IOrcaContainer cont = connect();
        List<ActorMng> actors = cont.getActors();
        Assert.assertNotNull(actors);
        Assert.assertEquals(3, actors.size());

        for (ActorMng actor : actors) {
            Assert.assertNotNull(actor.getID());
            IOrcaActor a = cont.getActor(new ID(actor.getID()));
            Assert.assertNotNull(a);
            Assert.assertEquals(actor.getID(), a.getGuid().toString());
        }

        actors = cont.getAuthorities();
        Assert.assertNotNull(actors);
        Assert.assertEquals(1, actors.size());
        Assert.assertEquals(SITE_GUID.toString(), actors.get(0).getID());

        actors = cont.getBrokers();
        Assert.assertNotNull(actors);
        Assert.assertEquals(1, actors.size());
        Assert.assertEquals(BROKER_GUID.toString(), actors.get(0).getID());

        actors = cont.getServiceManagers();
        Assert.assertNotNull(actors);
        Assert.assertEquals(1, actors.size());
        Assert.assertEquals(SM_GUID.toString(), actors.get(0).getID());

    }

    @Test
    public void testGetActorsFromDatabase() throws Exception {
        IOrcaContainer cont = connect();
        List<ActorMng> actors = cont.getActorsFromDatabase();
        Assert.assertNotNull(actors);
        Assert.assertEquals(3, actors.size());

        for (ActorMng actor : actors) {
            Assert.assertNotNull(actor.getID());
            IOrcaActor a = cont.getActor(new ID(actor.getID()));
            Assert.assertNotNull(a);
            Assert.assertEquals(actor.getID(), a.getGuid().toString());
        }
    }

    @Test
    public void testGetActor() throws Exception {
        IOrcaContainer cont = connect();

        // sm
        IOrcaActor actor = cont.getActor(SM_GUID);
        Assert.assertNotNull(actor);
        Assert.assertEquals(SM_GUID, actor.getGuid());

        IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);
        Assert.assertNotNull(sm);
        Assert.assertEquals(SM_GUID, sm.getGuid());
        Assert.assertSame(actor.getClass(), sm.getClass());

        sm = cont.getServiceManager(BROKER_GUID);
        Assert.assertNull(sm);
        Assert.assertNotNull(cont.getLastError().getException());

        sm = cont.getServiceManager(SITE_GUID);
        Assert.assertNull(sm);
        Assert.assertNotNull(cont.getLastError().getException());

        // broker

        actor = cont.getActor(BROKER_GUID);
        Assert.assertNotNull(actor);
        Assert.assertEquals(BROKER_GUID, actor.getGuid());

        IOrcaBroker broker = cont.getBroker(BROKER_GUID);
        Assert.assertNotNull(broker);
        Assert.assertEquals(BROKER_GUID, broker.getGuid());
        Assert.assertSame(actor.getClass(), broker.getClass());

        broker = cont.getBroker(SM_GUID);
        Assert.assertNull(broker);
        Assert.assertNotNull(cont.getLastError().getException());

        broker = cont.getBroker(SITE_GUID);
        Assert.assertNull(broker);
        Assert.assertNotNull(cont.getLastError().getException());

        // site
        actor = cont.getActor(SITE_GUID);
        Assert.assertNotNull(actor);
        Assert.assertEquals(SITE_GUID, actor.getGuid());

        IOrcaAuthority site = cont.getAuthority(SITE_GUID);
        Assert.assertNotNull(site);
        Assert.assertEquals(SITE_GUID, site.getGuid());
        Assert.assertSame(actor.getClass(), site.getClass());

        site = cont.getAuthority(SM_GUID);
        Assert.assertNull(site);
        Assert.assertNotNull(cont.getLastError().getException());

        site = cont.getAuthority(BROKER_GUID);
        Assert.assertNull(site);
        Assert.assertNotNull(cont.getLastError().getException());
    }

    @Test
    public void testConfigure() {
        // FIXME: not implemented yet
    }

    @Test
    public void testAddRemoveActor() {
        // FIXME: not implemented yet
    }

    @Test
    public void testStartStopActor() {
        // FIXME: not implemented yet
    }

    @Test
    public void testProxies() throws Exception {
        IOrcaContainer cont = connect();
        List<ProxyMng> proxies = cont.getProxies(OrcaConstants.ProtocolLocal);
        Assert.assertNotNull(proxies);
        // 1 for the broker, 1 for the site
        Assert.assertEquals(2, proxies.size());

        proxies = cont.getBrokerProxies(OrcaConstants.ProtocolLocal);
        Assert.assertNotNull(proxies);
        // 1 for the broker, 1 for the site [the site acting as a broker]
        Assert.assertEquals(2, proxies.size());

        proxies = cont.getAuthorityProxies(OrcaConstants.ProtocolLocal);
        Assert.assertNotNull(proxies);
        // 1 for the site
        Assert.assertEquals(1, proxies.size());
        Assert.assertEquals(SITE_GUID.toString(), proxies.get(0).getGuid());
    }

    // TODO: packages, plugins, and inventory
}