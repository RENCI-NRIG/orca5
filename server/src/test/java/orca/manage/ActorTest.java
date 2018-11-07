package orca.manage;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.shirako.common.SliceID;
import orca.util.ID;

import org.junit.Test;

public abstract class ActorTest extends ManagementTest {
    public static final String TEST_ALIAS = "FOO-BAR";
    public static final String TEST_SLICE_NAME = "TEST_SLICE";
    protected ID actorGuid;
    protected String actorName;

    public ActorTest(ID actorGuid, String actorName) {
        this.actorGuid = actorGuid;
        this.actorName = actorName;
    }

    protected IOrcaActor getActor() {
        IOrcaContainer cont = connect();
        Assert.assertNotNull(cont);

        IOrcaActor actor = cont.getActor(actorGuid);
        Assert.assertNotNull(actor);
        Assert.assertEquals(actorGuid, actor.getGuid());
        Assert.assertEquals(actorName, actor.getName());
        return actor;
    }

    @Test
    public void testGetCertificate() {
        IOrcaActor actor = getActor();
        X509Certificate cert = (X509Certificate) actor.getCertificate();
        Assert.assertNotNull(cert);
        Assert.assertEquals(actorGuid.toString(), getCN(cert.getSubjectDN().toString()));
    }

    @Test
    public void testRegisterUnregisterCertificate() {
        IOrcaActor actor = getActor();
        // just for cleanup, no asserts
        actor.unregisterCertificate(TEST_ALIAS);

        // try to get the cert: should not be present
        Certificate cert = actor.getCertificate(TEST_ALIAS);
        Assert.assertNull(cert);

        // add the actor cert under a different alias
        Certificate actorCert = actor.getCertificate();
        Assert.assertNotNull(actorCert);
        Assert.assertTrue(actor.registerCertificate(actorCert, TEST_ALIAS));

        // fetch it: should work
        Certificate other = actor.getCertificate(TEST_ALIAS);
        Assert.assertNotNull(other);
        Assert.assertEquals(actorCert, other);

        // remove it and make sure it's gone
        Assert.assertTrue(actor.unregisterCertificate(TEST_ALIAS));
        Assert.assertNull(actor.getCertificate(TEST_ALIAS));
    }

    @Test
    public void testGetSlices() {
        IOrcaActor actor = getActor();
        // fetch all slices
        List<SliceMng> slices = actor.getSlices();
        Assert.assertNotNull(slices);
        // every actor has at least one slice
        Assert.assertTrue(slices.size() >= 1);

        for (SliceMng slice : slices) {
            // fetch the slice using its id and compare the result
            SliceMng s = actor.getSlice(new SliceID(slice.getSliceID()));
            Assert.assertNotNull(s);
            Assert.assertEquals(slice.getSliceID(), s.getSliceID());
            Assert.assertEquals(slice.getName(), s.getName());
        }
    }

    @Test
    public void testAddUpdateRemoveSlice() {
        IOrcaActor actor = getActor();
        // create the new slice
        SliceMng slice = new SliceMng();
        slice.setName(TEST_SLICE_NAME);
        // attach a property
        PropertiesMng props = new PropertiesMng();
        PropertyMng p = new PropertyMng();
        p.setName("FOO");
        p.setValue("BAR");
        props.getProperty().add(p);
        slice.setLocalProperties(props);
        // now add the slice
        SliceID id = actor.addSlice(slice);
        Assert.assertNotNull(id);
        // make sure that the id is also attached to the slice
        Assert.assertNotNull(slice.getSliceID());
        Assert.assertEquals(id.toString(), slice.getSliceID());
        // fetch the slice
        SliceMng s = actor.getSlice(id);
        Assert.assertNotNull(s);
        Assert.assertEquals(TEST_SLICE_NAME, s.getName());
        Assert.assertEquals(id.toString(), s.getSliceID());
        // check the properties
        props = s.getLocalProperties();
        Assert.assertNotNull(props);
        Assert.assertEquals(1, props.getProperty().size());
        Assert.assertEquals("FOO", props.getProperty().get(0).getName());
        Assert.assertEquals("BAR", props.getProperty().get(0).getValue());
        // add one more local property
        p = new PropertyMng();
        p.setName("FOO2");
        p.setValue("BAR2");
        props.getProperty().add(p);
        // update the slice
        Assert.assertTrue(actor.updateSlice(s));
        // validate the update
        s = actor.getSlice(id);
        Assert.assertNotNull(s);
        // check the slice name and id: should not be changed
        Assert.assertEquals(TEST_SLICE_NAME, s.getName());
        Assert.assertEquals(id.toString(), s.getSliceID());
        // check the new properties
        props = s.getLocalProperties();
        Assert.assertNotNull(props);
        Assert.assertEquals(2, props.getProperty().size());
        Properties pp = OrcaConverter.fill(props);
        Assert.assertNotNull(pp);
        Assert.assertEquals("BAR", pp.getProperty("FOO"));
        Assert.assertEquals("BAR2", pp.getProperty("FOO2"));
        // remove the slice
        Assert.assertTrue(actor.removeSlice(id));
        Assert.assertNull(actor.getSlice(id));
    }

    @Test
    public void testGetReservations() {
        IOrcaActor actor = getActor();
        List<ReservationMng> rs = actor.getReservations();
        Assert.assertNotNull(rs);
        if (!actor.getName().equals(SM_NAME)) {
            Assert.assertTrue(rs.size() > 0);
        }
    }

    // @Test
    // public void testEvents() throws Exception {
    // IOrcaActor actor = getActor();
    // IEventHandler handler = new IEventHandler() {
    // public void handle(EventMng e) {
    // System.out.println("Received an event: " + e.getClass().getName());
    // }
    //
    // public void error(OrcaError error) {
    // System.err.println("An error occurred: " + error);
    // }
    // };
    //
    // LocalEventManager m = new LocalEventManager(actor, handler);
    // m.start();
    // Thread.sleep(60000);
    // m.stop();
    // }
}