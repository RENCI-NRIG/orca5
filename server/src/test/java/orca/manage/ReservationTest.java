package orca.manage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.manage.beans.EventMng;
import orca.manage.beans.LeaseReservationMng;
import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationPredecessorMng;
import orca.manage.beans.ReservationStateMng;
import orca.manage.beans.ReservationStateTransitionEventMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;

import org.junit.Assert;
import org.junit.Test;

public abstract class ReservationTest extends ManagementTest {
	@Test
	public void testBrokerInventory() {
		IOrcaContainer cont = connect();
		IOrcaBroker broker = cont.getBroker(BROKER_GUID);
		
		List<SliceMng> slices = broker.getInventorySlices();
		Assert.assertNotNull(slices);
		System.out.println("Broker slices:");
		for (SliceMng s : slices){
			System.out.println("\t" + s.getName() + "(" + s.getSliceID() + ")");
		}
		
		List<ReservationMng> rs = broker.getInventoryReservations();
		Assert.assertNotNull(rs);
		
		System.out.println("Broker inventory:");
		for (ReservationMng r : rs){
			System.out.println("\t(" + r.getReservationID() + ") type=" + r.getResourceType() + " start: " + r.getStart() + " stop:" + r.getEnd() + " units=" + r.getUnits());
		}
	}
	
	
	@Test
	public void testGetBrokerInventory() {
		IOrcaContainer cont = connect();
		IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);
		
		List<PoolInfoMng> pools = sm.getPoolInfo(BROKER_GUID);
		Assert.assertNotNull(pools);
		System.out.println("Inventory for broker: " + BROKER_GUID);
		for (PoolInfoMng p : pools){
			Properties pp = OrcaConverter.fill(p.getProperties());
			System.out.println(pp);
		}
	}	
		
	@Test
	public void testMultipleReservations() throws Exception {
		IOrcaContainer cont = connect();
		IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);

		// create a new slice
		SliceMng slice = new SliceMng();
		slice.setName("test-slice");
		SliceID id = sm.addSlice(slice);
		Assert.assertNotNull(id);
		
		int count = 2;
		List<TicketReservationMng> list = new ArrayList<TicketReservationMng>(count);
		for (int i = 0; i < count; i++){
			// create the reservation request
			TicketReservationMng r = new LeaseReservationMng();
			r.setStart(System.currentTimeMillis());
			r.setEnd(System.currentTimeMillis()+1000*20);
			r.setUnits(1);
			r.setResourceType("foo");
			r.setSliceID(id.toString());
			list.add(r);
		}
		
		List<ReservationID> rids = sm.addReservations(list);
		Assert.assertNotNull(rids);
		Assert.assertEquals(rids.size(), list.size());
		for (int i = 0; i < rids.size(); i++){
			Assert.assertEquals(rids.get(i).toString(), list.get(i).getReservationID());
		}
	
		List<ReservationStateMng> states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after add");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}
		
		for (ReservationID rid : rids){
			// demand it
			Assert.assertTrue(sm.demand(rid));
		}
		
		System.out.println("Waiting for the reservations to become active");
		Assert.assertTrue(OrcaConverter.awaitActive(rids, sm));
		states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after demand");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}
		
		for(ReservationID rid : rids){
			LeaseReservationMng r = (LeaseReservationMng)sm.getReservation(rid);
			Assert.assertEquals(1, r.getLeasedUnits());
		}
		
		
		
		// close all reservations in the slice
		sm.closeReservations(id);
		Assert.assertTrue(OrcaConverter.awaitClosed(rids, sm));
		states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after close");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}		
	}

	
	
	@Test
	public void testRequestResources() throws Exception {
		IOrcaContainer cont = connect();
		IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);
		final Object testDone = new Object();
				
		IOrcaEventHandler handler = new IOrcaEventHandler() {
			public void handle(EventMng e) {
				System.out.println("Received an event: " + e.getClass().getName());
				if (e instanceof ReservationStateTransitionEventMng){
					ReservationStateTransitionEventMng ste = (ReservationStateTransitionEventMng)e;
					System.out.println("Reservation #" + ste.getReservationId() + " transitioned into: " + OrcaConverter.getState(ste.getState()));
					if (OrcaConverter.hasNothingPending(ste.getState()) && OrcaConverter.isActive(ste.getState()) && !OrcaConverter.isActiveTicketed(ste.getState())){
						synchronized (testDone) {
							System.out.println("Reservation is active.");								
							testDone.notify();
						}
					}
				}
			}
			
			public void error(OrcaError error) {
				System.err.println("An error occurred: " + error);
			}
		};
	
		LocalEventManager m = new LocalEventManager(sm, handler);
		m.start();
		
		// create a new slice
		SliceMng slice = new SliceMng();
		slice.setName("test-slice");
		SliceID id = sm.addSlice(slice);
		Assert.assertNotNull(id);
		
		// create the reservation request
		TicketReservationMng r = new LeaseReservationMng();
		r.setStart(System.currentTimeMillis());
		r.setEnd(System.currentTimeMillis()+1000*100); // 30 seconds // 30 for extend, 100 for modify
		r.setUnits(1);
		r.setResourceType("foo");
		//r.setRenewable(true);
		r.setSliceID(id.toString());
		
		// add the request
		ReservationID rid = sm.addReservation(r);
		Assert.assertNotNull(rid);
		Assert.assertEquals(rid.toString(), r.getReservationID());

		ReservationStateMng state = sm.getReservationState(rid);
		Assert.assertNotNull(state);
		System.out.println("State after add: " + OrcaConverter.getState(state));
		
		// demand it
		System.out.println("State before sm.demand(): " + OrcaConverter.getState(state));
		Assert.assertTrue(sm.demand(r));
		synchronized(testDone){
			System.out.println("*** testDone triggered after demand");
			testDone.wait(60000);
		}
		
		state = sm.getReservationState(rid);
		Assert.assertNotNull(state);
		System.out.println("State before extend: " + OrcaConverter.getState(state));

		// extend the reservation
		Properties request = new Properties();
		request.setProperty("new-request-property", "true");
		Properties config = new Properties();
		config.setProperty("new-config-property", "false");
		Assert.assertTrue(sm.extendReservation(rid, new Date(r.getEnd() + 100000), request, config));
		
		synchronized(testDone){
			System.out.println("*** testDone triggered after extendReservation");
			testDone.wait(60000);
		}
		
		state = sm.getReservationState(rid);
		Assert.assertNotNull(state);
		System.out.println("State before close: " + OrcaConverter.getState(state));
		
		Assert.assertTrue(sm.closeReservation(rid));
		state = sm.getReservationState(rid);
		System.out.println("State after close: " + OrcaConverter.getState(state));
		
		while (!OrcaConverter.isClosed(state)){
			Thread.sleep(1000);
			state = sm.getReservationState(rid);
		}
		
		m.stop();
	}

	
	@Test
	public void testModifyResources() throws Exception {
		IOrcaContainer cont = connect();
		IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);
		IOrcaAuthority am = cont.getAuthority(SITE_GUID);
		
		final Object testDone = new Object();
				
		IOrcaEventHandler handler = new IOrcaEventHandler() {
			public void handle(EventMng e) {
				System.out.println("Received an event: " + e.getClass().getName());
				if (e instanceof ReservationStateTransitionEventMng){
					ReservationStateTransitionEventMng ste = (ReservationStateTransitionEventMng)e;
					System.out.println("Reservation #" + ste.getReservationId() + " transitioned into: " + OrcaConverter.getState(ste.getState()));
					if (OrcaConverter.hasNothingPending(ste.getState()) && OrcaConverter.isActive(ste.getState()) && !OrcaConverter.isActiveTicketed(ste.getState())){
						synchronized (testDone) {
							System.out.println("Reservation is active.");								
							testDone.notify();
						}
					}
				}
			}
			
			public void error(OrcaError error) {
				System.err.println("An error occurred: " + error);
			}
		};
	
		LocalEventManager m = new LocalEventManager(sm, handler);
		m.start();
		
		// create a new slice
		SliceMng slice = new SliceMng();
		slice.setName("test-slice");
		SliceID id = sm.addSlice(slice);
		Assert.assertNotNull(id);
		
		// create the reservation request
		TicketReservationMng r = new LeaseReservationMng();
		r.setStart(System.currentTimeMillis());
		r.setEnd(System.currentTimeMillis()+1000*100); // 30 seconds // 30 for extend, 100 for modify
		r.setUnits(1);
		r.setResourceType("foo");
		//r.setRenewable(true);
		r.setSliceID(id.toString());
		
		// add the request
		ReservationID rid = sm.addReservation(r);
		Assert.assertNotNull(rid);
		Assert.assertEquals(rid.toString(), r.getReservationID());

		ReservationStateMng state = sm.getReservationState(rid);
		Assert.assertNotNull(state);
		System.out.println("State after add: " + OrcaConverter.getState(state));
		
		// demand it
		System.out.println("State before sm.demand(): " + OrcaConverter.getState(state));
		Assert.assertTrue(sm.demand(r));
		synchronized(testDone){
			System.out.println("*** testDone triggered after demand");
			testDone.wait(60000);
		}
		
		state = sm.getReservationState(rid);
		Assert.assertNotNull(state);
		System.out.println("State before modify: " + OrcaConverter.getState(state));

		// modify the reservation	
		Properties modifyProps = new Properties();
		modifyProps.setProperty("new-modify-property1", "value1");
		modifyProps.setProperty("modify.subcommand.0", "modify.ssh");
		modifyProps.setProperty("modify.subcommand.1", "modify.restar");
		modifyProps.setProperty("modify.subcommand.2", "modify.restart");
		Assert.assertTrue(sm.modifyReservation(rid, modifyProps));
		
		synchronized(testDone){
			System.out.println("*** testDone triggered after modifyReservation");
			testDone.wait(60000);
		}
		
		//System.out.println("Unit properties = " + sm.getUnits(rid).get(0).getProperties());
		
		ArrayList<PropertyMng> listPmngConfig = (ArrayList<PropertyMng>) sm.getReservation(rid).getConfigurationProperties().getProperty();
	
		System.out.println("Printing SM reservation config properties");
		for(PropertyMng item:listPmngConfig){
			System.out.println(item.getName() + " = " + item.getValue());
		}
		
		ArrayList<PropertyMng> listPmng = (ArrayList<PropertyMng>) sm.getUnits(rid).get(0).getProperties().getProperty();
		
		System.out.println("Printing SM unit properties");
		for(PropertyMng item:listPmng){
			System.out.println(item.getName() + " = " + item.getValue());
		}
		
		ArrayList<PropertyMng> listPmngConfigAM = (ArrayList<PropertyMng>) am.getReservation(rid).getConfigurationProperties().getProperty();
		
		System.out.println("Printing AM reservation config properties");
		for(PropertyMng item:listPmngConfigAM){
			System.out.println(item.getName() + " = " + item.getValue());
		}
		
		ArrayList<PropertyMng> listPmngAM = (ArrayList<PropertyMng>) am.getUnits(rid).get(0).getProperties().getProperty();
		
		System.out.println("Printing AM unit properties");
		for(PropertyMng item:listPmngAM){
			System.out.println(item.getName() + " = " + item.getValue());
		}
		
		
		state = sm.getReservationState(rid);
		Assert.assertNotNull(state);
		System.out.println("State before close: " + OrcaConverter.getState(state));
		
		Assert.assertTrue(sm.closeReservation(rid));
		state = sm.getReservationState(rid);
		System.out.println("State after close: " + OrcaConverter.getState(state));
		
		while (!OrcaConverter.isClosed(state)){
			Thread.sleep(1000);
			state = sm.getReservationState(rid);
		}
		
		m.stop();
	}
	

	
	@Test
	public void testRedeemPredecessorNoFilter() throws Exception {
		IOrcaContainer cont = connect();
		IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);

		// create a new slice
		SliceMng slice = new SliceMng();
		slice.setName("test-slice");
		SliceID id = sm.addSlice(slice);
		Assert.assertNotNull(id);
		
		int count = 3;
		List<TicketReservationMng> list = new ArrayList<TicketReservationMng>(count);
		for (int i = 0; i < count; i++){
			// create the reservation request
			TicketReservationMng r = new LeaseReservationMng();
			r.setStart(System.currentTimeMillis());
			r.setEnd(System.currentTimeMillis()+1000*20);
			r.setUnits(1);
			r.setResourceType("foo");
			r.setSliceID(id.toString());
			list.add(r);
		}
		
		// add the reservations and ensure the ids are set
		List<ReservationID> rids = sm.addReservations(list);
		Assert.assertNotNull(rids);
		Assert.assertEquals(rids.size(), list.size());
		for (int i = 0; i < rids.size(); i++){
			Assert.assertEquals(rids.get(i).toString(), list.get(i).getReservationID());
		}
	
		LeaseReservationMng first = (LeaseReservationMng)list.get(0);
		for (int i=1; i < count; i++){
			ReservationPredecessorMng pred = new ReservationPredecessorMng();
			pred.setReservationID(list.get(i).getReservationID());
			// WRITEME: add filter
			first.getRedeemPredecessors().add(pred);
		}
		
		List<ReservationStateMng> states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after add");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}
	
		for (ReservationMng r : list){
			// demand it
			Assert.assertTrue(sm.demand(r));
		}
		
		System.out.println("Waiting for the reservations to become active");
		Assert.assertTrue(OrcaConverter.awaitActive(rids, sm));
		states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after demand");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}
		
		ReservationMng refreshed = sm.getReservation(new ReservationID(first.getReservationID()));
		Assert.assertNotNull(refreshed.getConfigurationProperties());
		System.out.println("Configuration properties");
		int total = 0;
		PropertiesMng config = refreshed.getConfigurationProperties();
		for (PropertyMng prop : config.getProperty()) {
			System.out.println(prop.getName() + "=" + prop.getValue());
			if (prop.getName().startsWith("predecessor.")){
				total++;
			}
		}
		
		System.out.println("Found " + total +" predecessor properties");
		Assert.assertTrue(total > 0);
		
		// close all reservations in the slice
		sm.closeReservations(id);
		Assert.assertTrue(OrcaConverter.awaitClosed(rids, sm));
		states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after close");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}		
	}

	@Test
	public void testRedeemPredecessorWithFilter() throws Exception {
		IOrcaContainer cont = connect();
		IOrcaServiceManager sm = cont.getServiceManager(SM_GUID);

		// create a new slice
		SliceMng slice = new SliceMng();
		slice.setName("test-slice");
		SliceID id = sm.addSlice(slice);
		Assert.assertNotNull(id);
		
		int count = 3;
		List<TicketReservationMng> list = new ArrayList<TicketReservationMng>(count);
		for (int i = 0; i < count; i++){
			// create the reservation request
			TicketReservationMng r = new LeaseReservationMng();
			r.setStart(System.currentTimeMillis());
			r.setEnd(System.currentTimeMillis()+1000*20);
			r.setUnits(1);
			r.setResourceType("foo");
			r.setSliceID(id.toString());
			list.add(r);
		}
		
		// add the reservations and ensure the ids are set
		List<ReservationID> rids = sm.addReservations(list);
		Assert.assertNotNull(rids);
		Assert.assertEquals(rids.size(), list.size());
		for (int i = 0; i < rids.size(); i++){
			Assert.assertEquals(rids.get(i).toString(), list.get(i).getReservationID());
		}
	
		Properties filter = new Properties();
		filter.setProperty("unit.vlan.tag", "vlan.tags");
		PropertiesMng mngFiler = OrcaConverter.fill(filter);
		
		LeaseReservationMng first = (LeaseReservationMng)list.get(0);
		for (int i=1; i < count; i++){
			ReservationPredecessorMng pred = new ReservationPredecessorMng();
			pred.setReservationID(list.get(i).getReservationID());
			pred.setFilter(mngFiler);
			first.getRedeemPredecessors().add(pred);
		}
		
		List<ReservationStateMng> states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after add");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}
	
		for (ReservationMng r : list){
			// demand it
			Assert.assertTrue(sm.demand(r));
		}
		
		System.out.println("Waiting for the reservations to become active");
		Assert.assertTrue(OrcaConverter.awaitActive(rids, sm));
		states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after demand");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}
		
		ReservationMng refreshed = sm.getReservation(new ReservationID(first.getReservationID()));
		Assert.assertNotNull(refreshed.getConfigurationProperties());
		System.out.println("Configuration properties");
		int total = 0;
		PropertiesMng config = refreshed.getConfigurationProperties();
		for (PropertyMng prop : config.getProperty()) {
			System.out.println(prop.getName() + "=" + prop.getValue());
			if (prop.getName().equals("vlan.tags")){
				total++;
			}
		}
		
		Assert.assertTrue(total==1);
		
		// close all reservations in the slice
		sm.closeReservations(id);
		Assert.assertTrue(OrcaConverter.awaitClosed(rids, sm));
		states = sm.getReservationState(rids);
		Assert.assertNotNull(states);
		System.out.println("State after close");
		for (ReservationStateMng state : states){
			System.out.println("\t" + OrcaConverter.getState(state));
		}		
	}

}