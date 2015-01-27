package orca.shirako.container;

import junit.framework.Assert;

public class ContainerManagerTest extends OrcaTestCase {

    public void testGetInstance() {
        Assert.assertNotNull(Globals.getContainer());
    }
}