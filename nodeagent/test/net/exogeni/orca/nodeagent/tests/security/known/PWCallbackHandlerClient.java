/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tests.security.known;

import org.apache.ws.security.WSPasswordCallback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

public class PWCallbackHandlerClient implements CallbackHandler {
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException, IOException {
        String clientPassword = "clientkeypass";

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback callback = (WSPasswordCallback) callbacks[i];

                System.out.println("Callback type " + callback.getClass().getName());
                System.out.println("Callback usage " + callback.getUsage());

                if (callback.getUsage() == WSPasswordCallback.SIGNATURE) {
                    System.out.println("callback of type SIGNATURE");
                    callback.setPassword(clientPassword);
                }

                if (callback.getUsage() == WSPasswordCallback.KEY_NAME) {
                    System.out.println("Callback for setting key not implemented!");
                }

                if (callback.getUsage() == WSPasswordCallback.DECRYPT) {
                    System.out.println("callback of type DECRYPT");
                    callback.setPassword(clientPassword);
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i],
                        "Unrecognized Callback of type " + (callbacks[i]).getClass().getName());
            }
        }

        return;
    }
}