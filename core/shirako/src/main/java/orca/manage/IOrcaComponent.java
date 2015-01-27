/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage;

import java.util.List;

import orca.manage.beans.ProtocolProxyMng;

/**
 * <code>IOrcaComponent</code> represents the interface for generic Orca component.
 */
public interface IOrcaComponent {
	/**
	 * Protocols supported by this components
	 * @return an list of <code>ProtocolProxyMng</code>
	 */
	public List<ProtocolProxyMng> getProtocols();
	/**
	 * Type identifier for the component.
	 * @return
	 */
	public String getTypeID();
	/**
	 * Returns the last error/success for this component.
	 * @return
	 */
	public OrcaError getLastError();
}