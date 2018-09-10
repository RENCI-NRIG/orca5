/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.manage.internal.local;

import java.util.List;

import net.exogeni.orca.manage.IOrcaComponent;
import net.exogeni.orca.manage.OrcaError;
import net.exogeni.orca.manage.beans.ProtocolProxyMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.internal.Converter;
import net.exogeni.orca.manage.internal.ManagementObject;
import net.exogeni.orca.security.AuthToken;

public class LocalProxy implements IOrcaComponent {
	protected ManagementObject manager;
	protected AuthToken auth;
	protected ResultMng lastStatus;
	protected Exception lastException;

	public LocalProxy(ManagementObject manager, AuthToken auth) {
		this.manager = manager;
		this.auth = auth;
	}

	protected void clearLast() {
		lastStatus = new ResultMng();
		lastException = null;
	}
	
	public OrcaError getLastError() {
		return new OrcaError(lastStatus, lastException);
	}
	
	public ManagementObject getManagerObject() {
		return manager;
	}
	
	public List<ProtocolProxyMng> getProtocols() {
		return Converter.fill(manager.getProxies());
	}

	public String getTypeID() {
		String result = null;

		if (manager.getTypeID() != null) {
			result = manager.getTypeID().toString();
		}

		return result;
	}
	
	public static <T> T getFirst(List<T> list){
		if (list == null || list.size() == 0) {return null;}
		return list.get(0);
	}
}
