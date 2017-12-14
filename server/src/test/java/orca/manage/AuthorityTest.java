package orca.manage;

import java.util.List;

import junit.framework.Assert;
import orca.manage.beans.ReservationMng;
import orca.util.ID;

import org.junit.Test;

public abstract class AuthorityTest extends ServerActorTest {
    public AuthorityTest(ID actorGuid, String actorName) {
        super(actorGuid, actorName);
    }

    protected IOrcaAuthority getAuthority() {
        return (IOrcaAuthority) getActor();
    }

    @Test
    public void testGetAuthorityReservations() {
        IOrcaAuthority site = getAuthority();
        List<ReservationMng> list = site.getAuthorityReservations();
        Assert.assertNotNull(list);
    }
}