package orca.shirako.persistence;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.kernel.AuthorityReservationFactory;
import orca.shirako.kernel.BrokerReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;
import orca.util.ID;
import orca.util.ResourceType;
import orca.util.persistence.PersistenceUtils;

import org.junit.Assert;
import org.junit.Test;

public class TestPersistence {
	static {
		Term.SetCycles = false;
	}
	
	private <V> void test(V original) throws Exception {
		Properties p = PersistenceUtils.persistObject(original);
		Assert.assertNotNull(p);
		V restored = (V)PersistenceUtils.restoreObject(p);
		Assert.assertNotNull(restored);
		Assert.assertEquals(original, restored);	
	}
	
	// TODO: test an x509 certificate
	
	
	@Test
	public void testID() throws Exception {
		test(new ID());
	}
	
	@Test
	public void testString() throws Exception {
		test(new String("hello there"));
	}
	
	@Test
	public void testInteger() throws Exception {
		//test(new Integer(-76543));
		test((int)34569);
	}
	
	@Test
	public void testLong() throws Exception {
		test(new Long(Long.MAX_VALUE));
		test(Long.MIN_VALUE);
	}

	@Test
	public void testBoolean() throws Exception {
		test(Boolean.TRUE);
		test(Boolean.FALSE);
		test(true);
		test(false);
	}

	enum Foo {
		a, b, c, d, e	
	};
	
	@Test
	public void testEnum() throws Exception {
		test(Foo.a);
		test(Foo.b);
		test(Foo.c);
		test(Foo.d);
		test(Foo.e);		
	}
	
	@Test
	public void testStringHashSet() throws Exception {
		HashSet<String> set = new HashSet<String>();
		for (int i = 0; i < 10; ++i){
			set.add("item." + i);
		}
		test(set);
	}

	@Test
	public void testIntHashSet() throws Exception {
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < 10; ++i){
			set.add(i*10);
		}
		test(set);
	}
	
	@Test
	public void testHashMap() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < 13; ++i) {
			map.put("key" + i, "value" + (i*10));
		}
		test(map);
	}
	
	@Test
	public void testProperties() throws Exception {
		Properties p = new Properties();
		for (int i = 0; i < 33; ++i) {
			p.setProperty("key" + i, "value" + (i*10));
		}
		test(p);
	}
	
	@Test
	public void testSlice() throws Exception {
		ISlice slice = SliceFactory.getInstance().create("test-slice");
		slice.setDescription("This is my slice");
		
		Properties p = PersistenceUtils.save(slice);
		Assert.assertNotNull(p);
		
		ISlice other = PersistenceUtils.restore(p);		
		PersistenceUtils.validateRestore(slice,  other);
	}
	
	@Test
	public void testServiceManagerReservation() throws Exception {
		ISlice slice = SliceFactory.getInstance().create("slice");

		ResourceType rtype = new ResourceType("type");
		ResourceData rdata = new ResourceData();
		rdata.getLocalProperties().setProperty("local.key", "local.value");
		ResourceSet rset = new ResourceSet(100, rtype, rdata);
		Term term = new Term(new Date(), 10000000);
		
		IServiceManagerReservation r = ServiceManagerReservationFactory.getInstance().create(rset, term, slice);
		
		Properties p = PersistenceUtils.save(r);
		
		IServiceManagerReservation restored = PersistenceUtils.restore(p);
		PersistenceUtils.validateRestore(r,  restored);
	}
	
	public void testBrokerReservation() throws Exception {
		ISlice slice = SliceFactory.getInstance().create("slice");

		ResourceType rtype = new ResourceType("type");
		ResourceData rdata = new ResourceData();
		rdata.getResourceProperties().setProperty("resource.key", "resource.value");
		ResourceSet resources = new ResourceSet(100, rtype, rdata);
		
		Term term = new Term(new Date(), 10000000);
		
		IBrokerReservation r = BrokerReservationFactory.getInstance().create(resources, term, slice);
		Properties p = PersistenceUtils.save(r);
		IBrokerReservation restored = PersistenceUtils.restore(p);
		PersistenceUtils.validateRestore(r,  restored);
	}
	
	@Test
	public void testAuthorityReservation() throws Exception {
		ISlice slice = SliceFactory.getInstance().create("slice");

		ResourceType rtype = new ResourceType("type");
		ResourceData rdata = new ResourceData();
		rdata.getResourceProperties().setProperty("resource.key", "resource.value");
		ResourceSet resources = new ResourceSet(100, rtype, rdata);
		
		Term term = new Term(new Date(), 10000000);
		
		IAuthorityReservation r = AuthorityReservationFactory.getInstance().create(resources, term, slice);
		Properties p = PersistenceUtils.save(r);
		IAuthorityReservation restored = PersistenceUtils.restore(p);
		PersistenceUtils.validateRestore(r,  restored);
	}
}