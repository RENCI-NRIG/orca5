/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage.proxies.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Marshaller;

import orca.manage.IOrcaComponent;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.OrcaError;
import orca.manage.beans.AuthTokenMng;
import orca.manage.beans.ProtocolProxyMng;
import orca.manage.beans.ResultMng;
import orca.security.AuthToken;
import orca.util.ID;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

public class SoapProxy implements IOrcaComponent {
	protected WebServiceTemplate client;
	protected AuthToken auth;
	protected String url;
	protected ResultMng lastStatus;
	protected Exception lastException;
	protected AuthTokenMng authMng;
	protected boolean loggedIn;
	protected final ID managementID;
	
	public SoapProxy(ID managementID, String url, AuthToken auth) {
		this.managementID = managementID;
		this.url = url;
		this.auth = auth;
		this.authMng = OrcaConverter.fill(auth);
		initClient();
	}

	protected void initClient() {
		client = new WebServiceTemplate();
		Jaxb2Marshaller m = new Jaxb2Marshaller();
		m.setContextPaths(
				"orca.manage.beans", 
				"orca.manage.proxies.soap.beans.actor",
				"orca.manage.proxies.soap.beans.authority",				
				//"orca.manage.proxies.soap.beans.broker",
				"orca.manage.proxies.soap.beans.clientactor",
				"orca.manage.proxies.soap.beans.container",
				"orca.manage.proxies.soap.beans.serveractor",
				"orca.manage.proxies.soap.beans.servicemanager"
				);
		Map<String, Object> mp = new HashMap<String, Object>();
		mp.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.setMarshallerProperties(mp);
		client.setMarshaller(m);
		client.setUnmarshaller(m);
		client.setDefaultUri(url);
	}
	
	protected void clearLast() {
		lastStatus = new ResultMng();
		lastException = null;
	}
	
	public OrcaError getLastError() {
		return new OrcaError(lastStatus, lastException);
	}
	

	public List<ProtocolProxyMng> getProtocols() {
		ProtocolProxyMng proto = new ProtocolProxyMng();
		proto.setProtocol(OrcaConstants.ProtocolSoap);
		proto.setProxyClass(this.getClass().getName());
		List<ProtocolProxyMng> result = new ArrayList<ProtocolProxyMng>();
		result.add(proto);
		return result;
	}

	public String getTypeID() {
		throw new RuntimeException("Not implemented");
	}
}