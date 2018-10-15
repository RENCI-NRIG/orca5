/**
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and/or hardware specification (the �Work�) to deal in the Work without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to
 * the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Work.
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
 * IN THE WORK.
 */

package orca.handlers.nlr;

import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaDescKeyword;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaIntAProperty;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaIntZProperty;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaLoginProperty;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaNodeAProperty;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaNodeZProperty;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaPasswordProperty;
import static orca.handlers.nlr.tasks.GenericSherpaTask.SherpaWorkgroupProperty;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orca.handlers.nlr.SherpaAPI;
import orca.handlers.nlr.SherpaSession;
import orca.handlers.nlr.SherpaAPIResponse.EntityDefinition;
import orca.handlers.nlr.SherpaAPIResponse.SPDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanGetReservationDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanIdDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanStatusDefinition;
import orca.util.PropertiesTestHelper;

public class SherpaTest extends TestCase {
    // NOTE: these don't actually contain real login and password.
    // For test purposes it comes out of System properties
    public static final String PrivateSherpaPropertiesFile = "/tmp/Sherpa.properties";

    private PropertiesTestHelper helper;
    private Properties p, pp;

    private String login;
    private String pass;
    private int wg;

    public SherpaTest() {
        helper = new PropertiesTestHelper(PrivateSherpaPropertiesFile);
        helper.process();

        p = helper.getProperties();
        pp = helper.getPrivateProperties();

    }

    // test the environment and that properties are there
    public void setEnv() {

        // login and password should come from PrivateSherpaPropertiesFile
        if (pp != null) {
            login = pp.getProperty(SherpaLoginProperty);
            pass = pp.getProperty(SherpaPasswordProperty);
            wg = Integer.valueOf(pp.getProperty(SherpaWorkgroupProperty));
        }
    }

    protected void setUp() {
        setEnv();
    }

    // test that we can login and execute a simple command
    public void testLogin() {
        System.err.println("Testing simple Sherpa login");
        if (login == null) {
            System.err.println("No login specified, skipping.");
            return;
        }

        SherpaSession ss = new SherpaSession(login, pass, null);
        SherpaAPI sapi = new SherpaAPI(ss, wg, null);

        try {
            sapi.get_available_vlan_id();
        } catch (Exception e) {
            Assert.fail("Exception encountered in get_available_vlan: " + e.getMessage());
        }
    }

    // try a vlan reservation
    public void testVlanReservation() {
        System.err.println("Testing Sherpa VLAN reservation");
        if (login == null) {
            System.err.println("No login specified, skipping.");
            return;
        }

        SherpaSession ss = new SherpaSession(login, pass, null);
        SherpaAPI sapi = new SherpaAPI(ss, wg, null);

        try {
            VlanIdDefinition avlan = sapi.get_available_vlan_id();
            Assert.assertNotNull(avlan);

            System.out.println("  Aquired available VLAN tag " + avlan.vlan_id);

            // try to reserve
            Assert.assertTrue("Unable to reserve a VLAN", sapi.add_reservation(avlan.vlan_id, "Automated reservation"));

            System.out.println("  Aquiring existing reservations");
            List<VlanGetReservationDefinition> defs = sapi.get_all_reservations();
            Iterator<VlanGetReservationDefinition> it5 = defs.iterator();
            while (it5.hasNext()) {
                VlanGetReservationDefinition resd = it5.next();
                System.out.print("    " + resd.description + " " + resd.vlan_id);
                System.out.println();
            }

            System.out.println("  Remove reservation for vlan " + avlan.vlan_id);
            Assert.assertTrue("Unable to remove VLAN reservation", sapi.remove_reservation(avlan.vlan_id));
            System.out.println("Done");
        } catch (Exception e) {
            Assert.fail("Exception encountered in VLAN reservation: " + e.getMessage());
        }
    }

    // try to provision a Vlan and then release it
    public void testVlanProvisioning() {
        System.err.println("Testing Sherpa VLAN provisioning");
        if (login == null) {
            System.err.println("No login specified, skipping.");
            return;
        }

        SherpaSession ss = new SherpaSession(login, pass, null);
        SherpaAPI sapi = new SherpaAPI(ss, wg, null);
        VlanIdDefinition avlan = new VlanIdDefinition();
        avlan.vlan_id = 0;

        try {
            String nodeA = pp.getProperty(SherpaNodeAProperty);
            String nodeZ = pp.getProperty(SherpaNodeZProperty);
            String intA = pp.getProperty(SherpaIntAProperty);
            String intZ = pp.getProperty(SherpaIntZProperty);

            // reserve a vlan id
            avlan = sapi.get_available_vlan_id();
            Assert.assertNotNull(avlan);
            // try to reserve
            Assert.assertTrue("Unable to reserve a VLAN", sapi.add_reservation(avlan.vlan_id, "Automated reservation"));
            System.out.println("  Aquired available VLAN tag " + avlan.vlan_id);

            // get the entity (grab first one)
            System.out.println("Getting entities");
            List<EntityDefinition> entities = sapi.get_entities();

            // get the shortest path
            System.out.println("Getting SP");
            List<SPDefinition> sp = sapi.get_shortest_path(nodeA, nodeZ, 0);

            System.out.print("Calculated shortest path: ");
            Iterator<SPDefinition> cktIt = sp.iterator();
            while (cktIt.hasNext())
                System.out.print(" " + cktIt.next().circuit_id);
            System.out.println();

            System.out.println("Provisioning VLAN between " + nodeA + "/" + intA + " and " + nodeZ + "/" + intZ);
            Assert.assertTrue(sapi.provision_vlan(nodeA, intA, nodeZ, intZ, sp, avlan.vlan_id, 0, SherpaDescKeyword, 1,
                    entities.get(0).entity_id));
            System.out.println("VLAN Provisioning has begun");

            System.out.println("Getting status");
            List<VlanStatusDefinition> statuses = sapi.get_status(avlan.vlan_id, 1);
            Iterator<VlanStatusDefinition> stIt = statuses.iterator();
            while (stIt.hasNext()) {
                VlanStatusDefinition stDef = stIt.next();
                System.out.println("Date: " + stDef.date + ": " + stDef.state);
            }

            System.out.println("Removing VLAN");
            Assert.assertTrue(sapi.remove_vlan(avlan.vlan_id, 1));

        } catch (Exception e) {
            Assert.fail("Exception encountered in VLAN provisioning: " + e.getMessage());
        }
    }

    public static Test suite() {
        return new TestSuite(SherpaTest.class);
    }
}