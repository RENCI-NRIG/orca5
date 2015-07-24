/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.log4j.Logger;
import org.apache.ws.security.WSPasswordCallback;


public class PWCallbackHandlerServer implements CallbackHandler
{
    private static String serverPassword = "serverkeypass";
    protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    public void handle(Callback[] callbacks) throws UnsupportedCallbackException, IOException
    {
        logger.debug("CallbackHandler invoked on server side");

        /*
         * // this part is used to load a key in case callback.getUsage() ==
         * WSPasswordCallback.KEY_NAME // however keys are identified by the
         * user/encryptionUser tags in services.xml or axis2.xml keyStorePath =
         * NodeManagerService.getInstance().getKeyStorePath(); byte[] clientKey =
         * null; String clientStorePassword = "clientstorepass"; char[]
         * clientStorePasswordChar = clientStorePassword.toCharArray(); String
         * clientPassword = "clientkeypass"; try { Security.addProvider(new
         * BouncyCastleProvider()); KeyStore ks = KeyStore.getInstance("JKS");
         * FileInputStream fis = new
         * FileInputStream("/home/ionut/eclipse/workspace/nodemanager/src/keystores/server.jks");
         * FileInputStream fis = new FileInputStream("server.jks");
         * FileInputStream fis = new
         * FileInputStream("/home/ionut/eclipse/workspace/nodemanager/src/keystores/client.jks");
         * ks.load(fis, serverStorePasswordChar);
         * ks.load(fis,clientStorePasswordChar); fis.close(); clientKey =
         * ks.getCertificate("clientkey").getEncoded(); } catch (Exception ex) {
         * logger.debug("Exception at keystore/keys"); ex.printStackTrace();
         * return; }
         */
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback callback = (WSPasswordCallback) callbacks[i];
                logger.debug("Callback is of type " + callback.getClass().getName());
                logger.debug("Callback usage is " + callback.getUsage());

                logger.debug(
                    "Callback has set " + "Identifier " + callback.getIdentifer() + "\n" +
                    "Password " + callback.getPassword() + "\n" + "PasswordType" +
                    callback.getPasswordType() + "\n" + "Key " + callback.getKey());

                if (callback.getUsage() == WSPasswordCallback.KEY_NAME) {
                    logger.debug("callback usage KEY_NAME");
                }

                if (callback.getUsage() == WSPasswordCallback.SIGNATURE) {
                    logger.debug("callback usage SIGNATURE");
                    callback.setPassword(serverPassword); // password
                                                          // protecting the
                                                          // server key
                                                          // (alias:
                                                          // "serverkey") for
                                                          // signing (the
                                                          // response)
                }

                if (callback.getUsage() == WSPasswordCallback.DECRYPT) {
                    logger.debug("callback usage DECRYPT");
                    callback.setPassword(serverPassword); // password for key
                                                          // alias for
                                                          // decrypting
                                                          // messages
                }

                if (callback.getUsage() == WSPasswordCallback.SECURITY_CONTEXT_TOKEN) {
                    logger.debug("callback usage SECURITY_CONTEXT_TOKEN");
                }

                if (callback.getUsage() == WSPasswordCallback.USERNAME_TOKEN) {
                    logger.debug("callback usage USERNAME_TOKEN");
                }

                if (callback.getUsage() == WSPasswordCallback.UNKNOWN) {
                    logger.debug("callback usage UNKNOWN");
                }

                if (callback.getUsage() == WSPasswordCallback.USERNAME_TOKEN_UNKNOWN) {
                    logger.debug("callback usage USERNAME_TOKEN_UNKNOWN");
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i],
                                                       "Unrecognized Callback of type " +
                                                       (callbacks[i]).getClass().getName());
            }
        }

        return;
    }
}