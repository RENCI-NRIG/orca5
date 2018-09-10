/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.container;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.container.api.IOrcaContainerDatabase;
import net.exogeni.orca.shirako.core.ActorTest;

import java.util.Properties;
import java.util.Vector;


public abstract class ContainerDatabaseTest extends OrcaTestCase
{
    public abstract IOrcaContainerDatabase getCleanDatabase() throws Exception;

    public void testCreate() throws Exception
    {
        IOrcaContainerDatabase db = getCleanDatabase();

        Vector<Properties> p = db.getActors();
        assertEquals(0, p.size());

        p = db.getContainerProperties();
        assertEquals(0, p.size());

        p = db.getTime();
        assertEquals(0, p.size());
    }

    /**
     * Tests add/remove actor.
     * @throws Exception
     */
    public void testAddRemoveActor() throws Exception
    {
        IOrcaContainerDatabase db = getCleanDatabase();

        ActorTest at = new ActorTest();
        IActor actor = at.getActor();

        Vector<Properties> v = db.getActor(ActorName);
        assertNotNull(v);
        assertEquals(1, v.size());

        v = db.getActors();
        assertNotNull(v);
        assertEquals(1, v.size());

        db.removeActor(ActorName);

        v = db.getActor(ActorName);
        assertEquals(0, v.size());

        v = db.getActors();
        assertEquals(0, v.size());
    }

    /**
     * Tests adding an actor and the processing of the add notification.
     */
    public void testAddActor() throws Exception
    {
        IOrcaContainerDatabase db = getCleanDatabase();
        // note: also adds the actor to the database
        IActor actor = getActor();

        Vector<Properties> v = db.getActor(ActorName);
        assertNotNull(v);
        assertEquals(1, v.size());
    }
}
