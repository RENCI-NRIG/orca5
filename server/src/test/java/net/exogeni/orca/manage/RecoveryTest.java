package net.exogeni.orca.manage;

import java.util.HashSet;
import java.util.List;

import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.util.ID;

import org.junit.Assert;

public abstract class RecoveryTest {
    public static final ID SITE_GUID = new ID("c48e2cb1-e00e-4ed2-918b-8fe4368c8c02");
    public static final String SITE_NAME = "ben-vlan-site";
    public static final ID BROKER_GUID = new ID("ebeb5bf1-4751-410a-94bf-0075297c162d");
    public static final String BROKER_NAME = "ben-vlan-broker";
    public static final ID SM_GUID = new ID("46AEA103-1185-4E69-8AFA-53194582F5A9");
    public static final String SM_NAME = "service";
    public static final String USER = "admin";
    public static final String PASS = "login";
    public static final String TEST_SLICE_NAME = "test-slice";

    public static final String SOAP_URL = "soap://http://localhost:8080/net.exogeni.orca/spring-services/";

    public static final HashSet<ID> ActorGuids = new HashSet<ID>();

    static {
        ActorGuids.add(SITE_GUID);
        ActorGuids.add(BROKER_GUID);
        ActorGuids.add(SM_GUID);
    }

    protected void awaitNoPendingReservations() throws InterruptedException {
        IActor[] actors = ActorRegistry.getActors();
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < actors.length; ++j) {
                actors[j].awaitNoPendingReservations();
            }
        }
    }

    protected void createTestSlice(IOrcaServiceManager sm) {
        SliceMng slice = new SliceMng();
        slice.setName(TEST_SLICE_NAME);
        SliceID id = sm.addSlice(slice);
        Assert.assertNotNull(id);
    }

    protected SliceMng getTestSlice(IOrcaServiceManager sm) {
        List<SliceMng> slices = sm.getSlices();
        for (SliceMng s : slices) {
            if (s.getName().equals(TEST_SLICE_NAME)) {
                return s;
            }
        }
        Assert.fail("Could not obtain test slice");
        return null;
    }

    protected IOrcaContainer connect() {
        return Orca.connect(SOAP_URL, USER, PASS);
    }
}
