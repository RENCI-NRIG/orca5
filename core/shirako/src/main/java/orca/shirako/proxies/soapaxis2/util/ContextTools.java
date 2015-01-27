/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.proxies.soapaxis2.util;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Vector;

import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.container.Globals;

import org.apache.axis2.context.MessageContext;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;

public class ContextTools
{
    /**
     * The property name under which the authorization token is stored in the
     * MessageContext
     */
    public static final String AUTHTOKEN_CLIENT = "authtoken.client";
    public static final String AUTHTOKEN_MINE = "authtoken.mine";
    public static final String SIGNED_BY = "signed.by";

    /**
     * Attaches the authtoken of an actor that is about to send a message to
     * another actor to the specified call
     * @param call
     * @param authToken
     */
    public static void setMyAuthToken(MessageContext context, AuthToken authToken)
    {
        context.setProperty(AUTHTOKEN_MINE, authToken);
    }

    public static void setMyAuthToken(AuthToken authToken)
    {
        MessageContext.getCurrentMessageContext().setProperty(AUTHTOKEN_MINE, authToken);
    }

    /**
     * Attaches the auth token of a caller to the message context
     * @param context
     * @param authToken
     */
    public static void setClientAuthToken(MessageContext context, AuthToken authToken)
    {
        context.setProperty(AUTHTOKEN_CLIENT, authToken);
    }

    /**
     * Retrieves the auth token of local actor making the current call
     * @param context
     * @return
     */
    public static AuthToken getMyAuthToken(MessageContext context)
    {
        return (AuthToken) context.getProperty(AUTHTOKEN_MINE);
    }

    /**
     * Retrieves the auth token of the remote client that has initiated the
     * current request.
     * @return
     */
    public static AuthToken getClientAuthToken()
    {
        return (AuthToken) MessageContext.getCurrentMessageContext().getProperty(AUTHTOKEN_CLIENT);
    }

    public static Certificate getSignedBy()
    {
        return (Certificate)MessageContext.getCurrentMessageContext().getProperty(SIGNED_BY);
    }
    
    public static boolean verifySignedBy(IActor serviceActor)
    {
        AuthToken auth = getClientAuthToken();
        Certificate cert = getSignedBy();
        
        if (auth == null){
            return false;
        }
        
        // FIXME: need to be careful here if the call takes place before the
        // container has been initialized.
        
        if (Globals.getConfiguration().isSecureActorCommunication()){
            // when using secure communication, rampart verifies that the signature
            // on the soap message is correct: i.e., the attached certificate represents the keypair
            // used to sign the message. Note that we modified rampart not to verify the trust path
            // since, we were not able to give rampart dynamic access to each actor's keystore.
            // so here, we need to make sure that the cert in the message is the same as the cert we have for
            // the actor.
            
            
            if (cert == null){
                return false;
            }
            // Certs are indexed by actor/client guid
            String alias = auth.getGuid().toString();
            // get the certificate from the store
            Certificate realCert = serviceActor.getShirakoPlugin().getKeyStore().getCertificate(alias);
            // no certificate: we do not know about this actor
            if (realCert == null){
                Globals.Log.error("Received SOAP call for an actor with a certificate not present in my keystore: remote actor=" + alias + " local actor=" + serviceActor.getName());
                return false;
            }
            
            // compare both certificates
            boolean result =  cert.equals(realCert);

            if (!result) {
                Globals.Log.info("Incoming certificate does not match local certificate. Actor alias=" + alias);
            }
            auth.setCertificate((X509Certificate)cert);
            setClientAuthToken(MessageContext.getCurrentMessageContext(), auth);
            return result;
            
            //boolean result =  realCert.getPublicKey().equals(cert.getPublicKey());
            //Globals.Log.debug("Comparing certificate keys: result=" + result + " real=" + realCert.getPublicKey() + " incoming=" + cert.getPublicKey());
            //return result;
        } else {
            // not using secure communication: just accept the token at face value
            return true;
        }
    }
    
    public static AuthToken temp() throws Exception
    {
        MessageContext context = MessageContext.getCurrentMessageContext();

        try {
            Vector result = (Vector) context.getProperty(WSHandlerConstants.RECV_RESULTS);
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    WSHandlerResult res = (WSHandlerResult) result.get(i);
                    for (int j = 0; j < res.getResults().size(); j++) {
                        WSSecurityEngineResult secRes = (WSSecurityEngineResult) res.getResults().get(j);
                        int action = secRes.getAction();

                        // SIGNATURE
                        if ((action & WSConstants.SIGN) > 0) {
                            // X509Certificate
                            // cert =
                            // secRes.getCertificate();
                            // X500Name
                            // principal =
                            // (X500Name)
                            // secRes.getPrincipal();
                            // // Do
                            // something
                            // whith cert
                            // Log.info("Signature
                            // for : " +
                            // principal.getCommonName());
                            // context.getInMessage().setProperty("net.gicm.astral.commonname",
                            // principal.getCommonName());
                            // }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return null;
    }
}
