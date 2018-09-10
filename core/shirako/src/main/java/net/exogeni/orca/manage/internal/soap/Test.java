package net.exogeni.orca.manage.internal.soap;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import net.exogeni.orca.manage.beans.EventMng;
import net.exogeni.orca.manage.beans.GenericEventMng;
import net.exogeni.orca.manage.beans.ReservationStateTransitionEventMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.proxies.soap.beans.actor.DrainEventsResponse;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

public class Test {
	public static void main(String[] args) throws Exception {
		DrainEventsResponse resp = new DrainEventsResponse();
		resp.setStatus(new ResultMng());

		EventMng e = new GenericEventMng();
		e.setActorId("foo");
		resp.getEvents().add(e);
		
		e = new ReservationStateTransitionEventMng();
		e.setActorId("foo-foo");
		resp.getEvents().add(e);

		Jaxb2Marshaller m = new Jaxb2Marshaller();
		m.setContextPaths(
				"net.exogeni.orca.manage.beans", 
				"net.exogeni.orca.manage.proxies.soap.beans.actor",
				"net.exogeni.orca.manage.proxies.soap.beans.authority",				
				//"net.exogeni.orca.manage.proxies.soap.beans.broker",
				"net.exogeni.orca.manage.proxies.soap.beans.clientactor",
				"net.exogeni.orca.manage.proxies.soap.beans.container",
				"net.exogeni.orca.manage.proxies.soap.beans.serveractor",
				"net.exogeni.orca.manage.proxies.soap.beans.servicemanager"
				);
		Map<String, Object> mp = new HashMap<String, Object>();
		mp.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.setMarshallerProperties(mp);
		 
		StringResult r = new StringResult();
        m.marshal(resp, r);
        
        String output = r.toString();
        System.out.println(output);
        
        StringSource s = new StringSource(output);
        
        DrainEventsResponse ur = (DrainEventsResponse)m.unmarshal(s);
        
        System.out.println("Found " + ur.getEvents().size() + " events");
        for (EventMng ee : ur.getEvents()){
        	System.out.println("Found event of type: " + ee.getClass().getCanonicalName());
        }
    }
}
