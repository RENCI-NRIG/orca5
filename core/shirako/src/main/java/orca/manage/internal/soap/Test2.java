package orca.manage.internal.soap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import orca.manage.beans.EventMng;
import orca.manage.beans.GenericEventMng;
import orca.manage.beans.ReservationStateTransitionEventMng;
import orca.manage.beans.ResultMng;
import orca.manage.proxies.soap.beans.actor.DrainEventsResponse;

import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

public class Test2 {
	public static void main(String[] args) throws Exception {
		DrainEventsResponse resp = new DrainEventsResponse();
		resp.setStatus(new ResultMng());

		EventMng e = new GenericEventMng();
		e.setActorId("foo");
		resp.getEvents().add(e);
		
		e = new ReservationStateTransitionEventMng();
		e.setActorId("foo-foo");
		resp.getEvents().add(e);

		JAXBContext context = JAXBContext.newInstance(DrainEventsResponse.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		 
		StringResult r = new StringResult();
        m.marshal(resp, r);
        
        String output = r.toString();
        System.out.println(output);
        
        StringSource s = new StringSource(output);

        Unmarshaller un = context.createUnmarshaller();
        DrainEventsResponse ur = (DrainEventsResponse)un.unmarshal(s);
        
        System.out.println("Found " + ur.getEvents().size() + " events");
        for (EventMng ee : ur.getEvents()){
        	System.out.println("Found event of type: " + ee.getClass().getCanonicalName());
        }
    }
}