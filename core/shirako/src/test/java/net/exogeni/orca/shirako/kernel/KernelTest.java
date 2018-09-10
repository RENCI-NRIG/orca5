/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.kernel;

import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.security.AccessMonitor;
import net.exogeni.orca.security.Guard;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IDatabase;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.container.OrcaTestCase;
import net.exogeni.orca.shirako.container.api.IOrcaContainerDatabase;


/**
 * Kernel unit tests.
 */
public class KernelTest extends OrcaTestCase
{
    /**
     * Checks if the slice is in the database.
     * @param db
     * @param rid
     * @throws Exception
     */
    protected void enforceExistsInDatabase(IDatabase db, ReservationID rid)
                                    throws Exception
    {
        Vector<Properties> v = db.getReservation(rid);
        assertNotNull(v);
        assertEquals(1, v.size());
    }

    /**
     * Checks if the slice is in the database.
     * @param db
     * @param sliceID
     * @throws Exception
     */
    protected void enforceExistsInDatabase(IDatabase db, SliceID sliceID) throws Exception
    {
        Vector<Properties> v = db.getSlice(sliceID);
        assertNotNull(v);
        assertEquals(1, v.size());
    }

    /**
     * Checks if the slice is not in the database.
     * @param db
     * @throws Exception
     */
    protected void enforceNotExistsInDatabase(IDatabase db, ReservationID rid)
                                       throws Exception
    {
        Vector<Properties> v = db.getReservation(rid);
        assertNotNull(v);

        assertEquals(0, v.size());
    }

    /**
     * Checks if the slice is not in the database.
     * @param db
     * @param sliceID
     * @throws Exception
     */
    protected void enforceNotExistsInDatabase(IDatabase db, SliceID sliceID)
                                       throws Exception
    {
        Vector<Properties> v = db.getSlice(sliceID);
        assertNotNull(v);

        assertEquals(0, v.size());
    }

    /**
     * Creates a new "fresh" actor and returns its kernel.
     * @return
     * @throws Exception
     */
    protected KernelWrapper getFreshKernelWrapper() throws Exception
    {
        IActor actor = prepareActor();

        KernelWrapper wrapper = new KernelWrapper(actor,
                                                   actor.getShirakoPlugin(),
                                                   actor.getPolicy(),
                                                   new AccessMonitor(),
                                                   new Guard());

        return wrapper;
    }

    /**
     * Creates a new kernel for the specified actor.
     * @param actor
     * @return
     * @throws Exception
     */
    protected KernelWrapper getKernelWrapper(IActor actor) throws Exception
    {
        KernelWrapper wrapper = new KernelWrapper(actor,
                                                   actor.getShirakoPlugin(),
                                                   actor.getPolicy(),
                                                   new AccessMonitor(),
                                                   new Guard());

        return wrapper;
    }

    /**
     * Creates a new actor: resets all database state.
     * @return
     * @throws Exception
     */
    protected IActor prepareActor() throws Exception
    {
        // get the container database
        IOrcaContainerDatabase db = getContainerDatabase();

        // create the actor instance
        IActor actor = getActor();
        // remove any previous database state
        db.removeActorDatabase(actor.getName());
        // add the actor to the database
        db.addActor(actor);
        // tell the actor it has been added to the database
        actor.actorAdded();

        return actor;
    }

    /**
     * Tests reservation registration.
     * @throws Exception
     */
    public void testRegisterReservation() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper kernel = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create a slice
        ISlice slice = SliceFactory.getInstance().create("testslice");
        // register it with the kernel
        kernel.registerSlice(slice);

        IReservation[] reservations = new IReservation[10];

        for (int i = 0; i < 10; i++) {
            IReservation reservation = ServiceManagerReservationFactory.getInstance().create();
            reservation.setSlice(slice);
            reservations[i] = reservation;
            // register the reservation
            kernel.registerReservation(reservation);
            // get it back
            assertNotNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceExistsInDatabase(db, reservation.getReservationID());

            // repeated registration should fail
            boolean failed = false;

            try {
                kernel.registerReservation(reservation);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            // get it back
            assertNotNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceExistsInDatabase(db, reservation.getReservationID());
        }
    }

    /**
     * Tests reservation registration in the presence of database errors.
     * @throws Exception
     */
    public void testRegisterReservationError() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper kernel = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create a slice
        ISlice slice = SliceFactory.getInstance().create("testslice");
        // register it with the kernel
        kernel.registerSlice(slice);

        /*
         * Set the database to null to cause errors while registering slices.
         */
        actor.getShirakoPlugin().setDatabase(null);

        IReservation[] reservations = new IReservation[10];

        for (int i = 0; i < 10; i++) {
            IReservation reservation = ServiceManagerReservationFactory.getInstance().create();
            reservation.setSlice(slice);
            reservations[i] = reservation;

            // register: should fail
            boolean failed = false;

            try {
                kernel.registerReservation(reservation);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            // get it back
            assertNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceNotExistsInDatabase(db, reservation.getReservationID());
        }
    }

    public void testRegisterSliceClient() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper w = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        ISlice[] slices = w.getSlices();

        assertNotNull(slices);
        assertEquals(0, slices.length);

        for (int i = 0; i < 10; i++) {
            ISlice slice = SliceFactory.getInstance().create("Slice: " + i);
            // mark as client
            slice.setClient();
            // register the slice
            w.registerSlice(slice);

            // try to get it back
            ISlice check = w.getSlice(slice.getSliceID());
            assertNotNull(check);
            assertSame(check, slice);

            // get all slices and check their number
            ISlice[] tmp = w.getSlices();
            assertNotNull(tmp);
            assertEquals(i + 1, tmp.length);
            // get all inventory slices
            tmp = w.getClientSlices();
            assertNotNull(tmp);
            assertEquals(i + 1, tmp.length);
            // now check the database and make sure the slice is there
            enforceExistsInDatabase(db, slice.getSliceID());

            // now get all slices
            Vector<Properties> v = db.getSlices();
            assertNotNull(v);
            assertEquals(1 + i, v.size());
            // now get all inventory slices
            v = db.getClientSlices();
            assertNotNull(v);
            assertEquals(1 + i, v.size());

            // add again and make sure the attempt fails
            boolean failed = false;

            try {
                w.registerSlice(slice);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);

            // make sure the slice is still there
            ISlice s = w.getSlice(slice.getSliceID());
            assertNotNull(s);
            assertSame(s, slice);
            v = db.getSlice(slice.getSliceID());
            assertNotNull(v);
            assertEquals(1, v.size());
        }
    }

    /**
     * Tests slice re-registration in the presence of database errors.
     * @throws Exception
     */
    public void testRegisterSliceError() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper w = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();
        /*
         * Set the database to null. This should cause a null pointer when
         * trying to add the slice record to the database.
         */
        actor.getShirakoPlugin().setDatabase(null);

        // create 10 slices and register them with the kernel
        ISlice[] slices = new ISlice[10];

        for (int i = 0; i < 10; i++) {
            ISlice slice = SliceFactory.getInstance().create("Slice: " + i);
            slices[i] = slice;

            boolean failed = false;

            try {
                w.registerSlice(slice);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);

            /*
             * Make sure that the previous attempt did not leave the slice in
             * the kernel data structures.
             */
            ISlice check = w.getSlice(slice.getSliceID());
            assertNull(check);
            // make sure the slice is not in the database
            enforceNotExistsInDatabase(db, slice.getSliceID());
        }
    }

    public void testRegisterSliceInventory() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper w = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        ISlice[] slices = w.getSlices();

        assertNotNull(slices);
        assertEquals(0, slices.length);

        for (int i = 0; i < 10; i++) {
            ISlice slice = SliceFactory.getInstance().create("Slice: " + i);
            // mark as inventory
            slice.setInventory(true);
            // register the slice
            w.registerSlice(slice);

            // try to get it back
            ISlice check = w.getSlice(slice.getSliceID());
            assertNotNull(check);
            assertSame(check, slice);

            // get all slices and check their number
            ISlice[] tmp = w.getSlices();
            assertNotNull(tmp);
            assertEquals(i + 1, tmp.length);
            // get all inventory slices
            tmp = w.getInventorySlices();
            assertNotNull(tmp);
            assertEquals(i + 1, tmp.length);
            // now check the database and make sure the slice is there
            enforceExistsInDatabase(db, slice.getSliceID());

            // now get all slices
            Vector<Properties> v = db.getSlices();
            assertNotNull(v);
            assertEquals(1 + i, v.size());
            // now get all inventory slices
            v = db.getInventorySlices();
            assertNotNull(v);
            assertEquals(1 + i, v.size());

            // add again and make sure the attempt fails
            boolean failed = false;

            try {
                w.registerSlice(slice);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);

            // make sure the slice is still there
            ISlice s = w.getSlice(slice.getSliceID());
            assertNotNull(s);
            assertSame(s, slice);
            v = db.getSlice(slice.getSliceID());
            assertNotNull(v);
            assertEquals(1, v.size());
        }
    }

    /**
     * Tests slice removal.
     * @throws Exception
     */
    public void testRemoveSliceEmpty() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper w = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create 10 slices and register them with the kernel
        ISlice[] slices = new ISlice[10];

        for (int i = 0; i < 10; i++) {
            ISlice slice = SliceFactory.getInstance().create("Slice: " + i);
            slices[i] = slice;
            w.registerSlice(slice);
            enforceExistsInDatabase(db, slice.getSliceID());
        }

        // remove the slice
        for (int i = 0; i < slices.length; i++) {
            ISlice slice = slices[i];
            w.removeSlice(slice.getSliceID());

            // make sure not in kernel
            ISlice check = w.getSlice(slice.getSliceID());
            assertNull(check);
            // make sure not in in database
            enforceNotExistsInDatabase(db, slice.getSliceID());
            // removing again should not throw an error
            w.removeSlice(slice.getSliceID());
            enforceNotExistsInDatabase(db, slice.getSliceID());
            // try to register the slice
            w.registerSlice(slice);
            // make sure it is in the kernel
            assertNotNull(w.getSlice(slice.getSliceID()));
            // make sure it is in the database
            enforceExistsInDatabase(db, slice.getSliceID());
        }
    }

    /**
     * Tests reservation re-registration.
     * @throws Exception
     */
    public void testReregisterReservation() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper kernel = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create a slice
        ISlice slice = SliceFactory.getInstance().create("testslice");
        IReservation[] reservations = new IReservation[10];

        for (int i = 0; i < 10; i++) {
            IReservation reservation = ServiceManagerReservationFactory.getInstance().create();
            reservation.setSlice(slice);
            reservations[i] = reservation;

            // registering a reservation, whose slice has not been registered
            // yet should fail
            boolean failed = false;

            try {
                kernel.registerReservation(reservation);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            // get it back
            assertNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceNotExistsInDatabase(db, reservation.getReservationID());
            // make sure slice is not present
            assertNull(kernel.getSlice(slice.getSliceID()));
            enforceNotExistsInDatabase(db, slice.getSliceID());
        }

        // register the slice with the kernel
        kernel.registerSlice(slice);

        for (int i = 0; i < 10; i++) {
            IReservation reservation = reservations[i];
            // get it back
            assertNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceNotExistsInDatabase(db, reservation.getReservationID());

            // re-register on a new reservation should fail
            boolean failed = false;

            try {
                kernel.reregisterReservation(reservation);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            // get it back
            assertNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceNotExistsInDatabase(db, reservation.getReservationID());
            // register the reservation
            kernel.registerReservation(reservation);
            // get it back
            assertNotNull(kernel.getReservation(reservation.getReservationID()));
            // make sure it is in the database
            enforceExistsInDatabase(db, reservation.getReservationID());
        }

        /*
         * Create another kernel wrapper. This will be "equivalent" to starting
         * with a clean kernel.
         */
        KernelWrapper newKernel = getKernelWrapper(actor);
        // re-register the slice: this will clear any previous state created
        // by the first kernel
        newKernel.reregisterSlice(slice);

        for (int i = 0; i < 10; i++) {
            IReservation reservation = reservations[i];
            // make sure the new kernel does not know about this reservation
            assertNull(newKernel.getReservation(reservation.getReservationID()));
            // make sure the reservation is in the database
            enforceExistsInDatabase(db, reservation.getReservationID());

            /*
             * Try to register the reservation with the new kernel. This should
             * fail since there is already a record in the database.
             */
            boolean failed = false;

            try {
                newKernel.registerReservation(reservation);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            /*
             * Check that the previous attempt did not leave the reservation in
             * the kernel data structures and that it did not affect the
             * database.
             */

            // make sure the new kernel does not know about this reservation
            assertNull(newKernel.getReservation(reservation.getReservationID()));
            // make sure the reservation is in the database
            enforceExistsInDatabase(db, reservation.getReservationID());
            /*
             * re-register the reservation.
             */
            newKernel.reregisterReservation(reservation);
            /*
             * Check if registered.
             */
            assertNotNull(newKernel.getReservation(reservation.getReservationID()));
            /*
             * Check the database.
             */
            enforceExistsInDatabase(db, reservation.getReservationID());
        }
    }

    /**
     * Tests slice re-registration.
     * @throws Exception
     */
    public void testReregisterSlice() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper w = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create 10 slices and register them with the kernel
        ISlice[] slices = new ISlice[10];

        for (int i = 0; i < 10; i++) {
            ISlice slice = SliceFactory.getInstance().create("Slice: " + i);
            slices[i] = slice;

            /*
             * Calling re-register on a new slice should fail.
             */
            boolean failed = false;

            try {
                w.reregisterSlice(slice);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            // make sure no database record
            enforceNotExistsInDatabase(db, slice.getSliceID());
            // make sure not present in kernel data structures
            assertNull(w.getSlice(slice.getSliceID()));
            // register
            w.registerSlice(slice);
        }

        /*
         * Create another kernel wrapper. This will be "equivalent" starting
         * with a clean kernel.
         */
        KernelWrapper newKernel = getKernelWrapper(actor);

        for (int i = 0; i < 10; i++) {
            ISlice slice = slices[i];

            // make sure the new kernel does not know about this slice
            ISlice check = newKernel.getSlice(slice.getSliceID());
            assertNull(check);
            // make sure the slice is in the database
            enforceExistsInDatabase(db, slice.getSliceID());

            /*
             * Try to register the slice with the new kernel. This should fail
             * since there is already a record in the database.
             */
            boolean failed = false;

            try {
                newKernel.registerSlice(slice);
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            /*
             * That the previous attempt did not leave the slice in the kernel
             * data structures.
             */
            check = newKernel.getSlice(slice.getSliceID());
            assertNull(check);
            /*
             * re-register the slice.
             */
            newKernel.reregisterSlice(slice);
            /*
             * Check if registered.
             */
            check = newKernel.getSlice(slice.getSliceID());
            assertNotNull(check);
            /*
             * Check the database.
             */
            enforceExistsInDatabase(db, slice.getSliceID());
        }
    }

    /**
     * Tests reservation un-registration.
     * @throws Exception
     */
    public void testUnregisterReservation() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper kernel = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create a slice
        ISlice slice = SliceFactory.getInstance().create("testslice");
        // register the slice with the kernel
        kernel.registerSlice(slice);

        // create some reservations and register them
        IReservation[] reservations = new IReservation[10];

        for (int i = 0; i < 10; i++) {
            IReservation reservation = ServiceManagerReservationFactory.getInstance().create();
            reservation.setSlice(slice);
            reservations[i] = reservation;
            // make sure not in the database
            assertNull(kernel.getReservation(reservation.getReservationID()));
            enforceNotExistsInDatabase(db, reservation.getReservationID());

            // unregistering something that is not there should throw an exception
            boolean failed = false;

            try {
                kernel.unregisterReservation(reservation.getReservationID());
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            // make sure not in the database
            assertNull(kernel.getReservation(reservation.getReservationID()));
            enforceNotExistsInDatabase(db, reservation.getReservationID());
            kernel.registerReservation(reservation);
            assertNotNull(kernel.getReservation(reservation.getReservationID()));
            enforceExistsInDatabase(db, reservation.getReservationID());
        }

        // unregister the reservations
        for (int i = 0; i < reservations.length; i++) {
            IReservation reservation = reservations[i];
            assertNotNull(kernel.getSlice(slice.getSliceID()));
            enforceExistsInDatabase(db, slice.getSliceID());
            /*
             * We can only unregister failed/closed reservations.
             */

            // fail the reservation
            reservation.fail("forced");
            // attempt to unregister
            kernel.unregisterReservation(reservation.getReservationID());
            // make sure not in kernel
            assertNull(kernel.getReservation(reservation.getReservationID()));
            // make sure in database
            enforceExistsInDatabase(db, reservation.getReservationID());
        }

        IReservation[] check = kernel.getReservations(slice.getSliceID());
        assertNotNull(check);
        assertEquals(0, check.length);
    }

    /**
     * Tests empty slice un-registration
     * @throws Exception
     */
    public void testUnregisterSliceEmpty() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper w = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create 10 slices and register them with the kernel
        ISlice[] slices = new ISlice[10];

        for (int i = 0; i < 10; i++) {
            ISlice slice = SliceFactory.getInstance().create("Slice: " + i);
            slices[i] = slice;
            w.registerSlice(slice);
        }

        // unregisters the slices
        for (int i = 0; i < slices.length; i++) {
            ISlice slice = slices[i];
            w.unregisterSlice(slice.getSliceID());

            // make sure not in kernel
            ISlice check = w.getSlice(slice.getSliceID());
            assertNull(check);
            // make sure still in database
            enforceExistsInDatabase(db, slice.getSliceID());

            // make sure an exception is thrown if trying to unregsiter again
            boolean failed = false;

            try {
                w.unregisterSlice(slice.getSliceID());
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            enforceExistsInDatabase(db, slice.getSliceID());
        }
    }

    /**
     * Tests reservation re-registration.
     * @throws Exception
     */
    public void testUnregisterSliceFull() throws Exception
    {
        IActor actor = prepareActor();
        KernelWrapper kernel = getKernelWrapper(actor);
        IDatabase db = actor.getShirakoPlugin().getDatabase();

        // create a slice
        ISlice slice = SliceFactory.getInstance().create("testslice");
        // register the slice with the kernel
        kernel.registerSlice(slice);

        // create some registrations and register them
        IReservation[] reservations = new IReservation[10];

        for (int i = 0; i < 10; i++) {
            IReservation reservation = ServiceManagerReservationFactory.getInstance().create();
            reservation.setSlice(slice);
            reservations[i] = reservation;
            kernel.registerReservation(reservation);
        }

        // attempt to unregister the slice: should fail
        // unregister one reservation
        for (int i = 0; i < reservations.length; i++) {
            IReservation reservation = reservations[i];
            assertNotNull(kernel.getSlice(slice.getSliceID()));
            enforceExistsInDatabase(db, slice.getSliceID());

            boolean failed = false;

            try {
                kernel.unregisterSlice(slice.getSliceID());
            } catch (Exception e) {
                failed = true;
            }

            assertTrue(failed);
            assertNotNull(kernel.getSlice(slice.getSliceID()));
            enforceExistsInDatabase(db, slice.getSliceID());
            /*
             * We can only unregister failed/closed reservations.
             */

            // fail the reservation
            reservation.fail("forced");
            // attempt to unregister
            kernel.unregisterReservation(reservation.getReservationID());
        }

        // by now there should be no more reservations in this slice
        kernel.unregisterSlice(slice.getSliceID());
        assertNull(kernel.getSlice(slice.getSliceID()));
        enforceExistsInDatabase(db, slice.getSliceID());
        // re-regiser the slice
        kernel.reregisterSlice(slice);
        assertNotNull(kernel.getSlice(slice.getSliceID()));
        enforceExistsInDatabase(db, slice.getSliceID());
    }
}
