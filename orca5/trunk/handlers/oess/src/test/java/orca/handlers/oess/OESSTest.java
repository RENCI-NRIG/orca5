/**
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and/or hardware specification (the “Work”) to deal in the Work without restriction, including
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

package orca.handlers.oess;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static orca.handlers.oess.tasks.GenericOESSTask.OESSDescKeyword;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSIntAProperty;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSIntZProperty;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSLoginProperty;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSNodeAProperty;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSNodeZProperty;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSPasswordProperty;
import static orca.handlers.oess.tasks.GenericOESSTask.OESSWorkgroupProperty;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orca.handlers.oess.OESSAPIResponse.EntityDefinition;
import orca.handlers.oess.OESSAPIResponse.ExistingCircuitReservation;
import orca.handlers.oess.OESSAPIResponse.SPDefinition;
import orca.handlers.oess.OESSAPIResponse.VlanIdDefinition;
import orca.handlers.oess.OESSAPIResponse.VlanStatusDefinition;
import orca.util.PropertiesTestHelper;

public class OESSTest extends TestCase {
	// NOTE: these don't actually contain real login and password.
	// For test purposes it comes out of System properties
    public static final String PrivateOESSPropertiesFile = "/tmp/OESS.properties";
    
    private PropertiesTestHelper helper;
    private Properties p, pp;
    
    private String login;
    private String pass;
    private int wg;
    
    public OESSTest() {
        helper = new PropertiesTestHelper(PrivateOESSPropertiesFile);
        helper.process();

        p = helper.getProperties();
        pp = helper.getPrivateProperties();

    }
    
    // test the environment and that properties are there
    public void setEnv() {
    	
        // login and password should come from PrivateOESSPropertiesFile
    	if (pp !=null) {
    		login = pp.getProperty(OESSLoginProperty);
    		pass = pp.getProperty(OESSPasswordProperty);
    		wg = Integer.valueOf(pp.getProperty(OESSWorkgroupProperty));
    	}
    }
    
    protected void setUp() {
    	setEnv();
    }
    
    // test that we can login and execute a simple command
    public void testLogin() {
    	System.err.println("Testing simple OESS login");
    	if (login == null) {
    		System.err.println("No login specified, skipping.");
    		return;
    	}
    	
		OESSSession ss = new OESSSession(login, pass, null);
		OESSAPI sapi = new OESSAPI(ss, wg, null);
		
		try {
			sapi.get_workgroups();
		} catch(Exception e) {
			Assert.fail("Exception encountered in get_workgroups: " + e.getMessage());
		}
    }
    
    // try a vlan reservation 
//    public void testVlanReservation() {
//    	System.err.println("Testing OESS VLAN reservation");
//    	if (login == null) {
//    		System.err.println("No login specified, skipping.");
//    		return;
//    	}
//
//    	OESSSession ss = new OESSSession(login, pass, null);
//    	OESSAPI sapi = new OESSAPI(ss, wg, null);
//
//    	try {
//    		VlanIdDefinition avlan = sapi.get_available_vlan_id();
//    		Assert.assertNotNull(avlan);
//    		
//    		System.out.println("  Aquired available VLAN tag " + avlan.vlan_id);
//
//    		// try to reserve
//    		Assert.assertTrue("Unable to reserve a VLAN", sapi.add_reservation(avlan.vlan_id, "Automated reservation")); 
//
//    		System.out.println("  Aquiring existing reservations");
//    		List<ExistingCircuitReservation> defs = sapi.get_all_reservations();
//    		Iterator<ExistingCircuitReservation> it5 = defs.iterator();
//    		while(it5.hasNext()) {
//    			ExistingCircuitReservation resd = it5.next();
//    			System.out.print("    " + resd.description + " " + resd.vlan_id);
//    			System.out.println();
//    		}
//
//    		System.out.println("  Remove reservation for vlan " + avlan.vlan_id);
//    		Assert.assertTrue("Unable to remove VLAN reservation", sapi.remove_reservation(avlan.vlan_id));
//    		System.out.println("Done");
//    	} catch (Exception e) {
//    		Assert.fail("Exception encountered in VLAN reservation: " + e.getMessage());
//    	}
//    }
    
    // try to provision a Vlan and then release it
//    public void testVlanProvisioning() {
//    	System.err.println("Testing OESS VLAN provisioning");
//    	if (login == null) {
//    		System.err.println("No login specified, skipping.");
//    		return;
//    	}
//    	
//    	OESSSession ss = new OESSSession(login, pass, null);
//    	OESSAPI sapi = new OESSAPI(ss, wg, null);
//    	VlanIdDefinition avlan = new VlanIdDefinition();
//    	avlan.vlan_id = 0;
//    	
//    	try {
//    		String nodeA = pp.getProperty(OESSNodeAProperty);
//    		String nodeZ = pp.getProperty(OESSNodeZProperty);
//    		String intA = pp.getProperty(OESSIntAProperty);
//    		String intZ = pp.getProperty(OESSIntZProperty);
//    		
//    		// reserve a vlan id
//       		avlan = sapi.get_available_vlan_id();
//    		Assert.assertNotNull(avlan);
//    		// try to reserve
//    		Assert.assertTrue("Unable to reserve a VLAN", sapi.add_reservation(avlan.vlan_id, "Automated reservation")); 
//    		System.out.println("  Aquired available VLAN tag " + avlan.vlan_id);
//
//    		// get the entity (grab first one)
//    		System.out.println("Getting entities");
//    		List<EntityDefinition> entities = sapi.get_entities();
//    		
//    		// get the shortest path
//    		System.out.println("Getting SP");
//    		List<SPDefinition> sp = sapi.get_shortest_path(nodeA, nodeZ, 0);
//    		
//    		System.out.print("Calculated shortest path: ");
//    		Iterator<SPDefinition> cktIt = sp.iterator();
//    		while(cktIt.hasNext())
//    			System.out.print(" " + cktIt.next().circuit_id);
//    		System.out.println();
//    		
//    		System.out.println("Provisioning VLAN between " + nodeA + "/" + intA + " and " + nodeZ + "/" + intZ);
//    		Assert.assertTrue(sapi.provision_vlan(nodeA, intA, nodeZ, intZ, sp, avlan.vlan_id, 0, 
//    				OESSDescKeyword, 1, entities.get(0).entity_id));
//    		System.out.println("VLAN Provisioning has begun");
//    		
//    		System.out.println("Getting status");
//    		List<VlanStatusDefinition> statuses = sapi.get_status(avlan.vlan_id, 1);
//    		Iterator<VlanStatusDefinition> stIt = statuses.iterator();
//    		while(stIt.hasNext()) {
//    			VlanStatusDefinition stDef = stIt.next();
//    			System.out.println("Date: " + stDef.date + ": " + stDef.state);
//    		}
//    		
//    		System.out.println("Removing VLAN");
//    		Assert.assertTrue(sapi.remove_vlan(avlan.vlan_id, 1));
//    		
//    	} catch (Exception e) {
//    		Assert.fail("Exception encountered in VLAN provisioning: " + e.getMessage());
//    	}
//    }
    
    public static Test suite() {
        return new TestSuite(OESSTest.class);
    }
}