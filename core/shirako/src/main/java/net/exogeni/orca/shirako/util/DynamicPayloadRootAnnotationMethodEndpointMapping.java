package net.exogeni.orca.shirako.util;

import javax.xml.namespace.QName;

import org.springframework.beans.BeansException;
import org.springframework.ws.server.endpoint.MethodEndpoint;
import org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping;

public class DynamicPayloadRootAnnotationMethodEndpointMapping extends PayloadRootAnnotationMethodEndpointMapping {
	@Override 
	protected synchronized void registerEndpoint(QName key, MethodEndpoint endpoint) throws BeansException {
		try {
			super.registerEndpoint(key, endpoint);
		}
		catch (Exception e){
			e.printStackTrace();
			throw e;
		}

	}
	
	@Override
	protected synchronized MethodEndpoint lookupEndpoint(QName key) {
		return super.lookupEndpoint(key);
	}
	
	public void registerEndpoint(final String beanName) {
		super.registerMethods(beanName);
	}
}
