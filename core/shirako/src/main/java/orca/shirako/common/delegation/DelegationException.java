/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.common.delegation;

public class DelegationException extends Exception {
	public static final long serialVersionUID = 1L;

	/**
	 * Creates a default instance.
	 */
	public DelegationException() {
	}

	/**
	 * Creates a new instance with the specified message.
	 * 
	 * @param message
	 *            message to use
	 */
	public DelegationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance with the specified message wrapping the given
	 * exception.
	 * 
	 * @param message
	 *            message to use
	 * @param exception
	 *            exception to wrap
	 */
	public DelegationException(final String message, final Throwable exception) {
		super(message, exception);
	}
}