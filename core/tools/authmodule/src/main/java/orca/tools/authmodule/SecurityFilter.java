/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.tools.authmodule;

import java.security.cert.X509Certificate;
import java.util.Vector;

import javax.xml.namespace.QName;

import orca.security.AuthToken;
import orca.util.ID;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;

class SecurityFilter
{
    /**
     * The property name under which the authorization token is stored in the
     * MessageContext
     */
    public static final String AUTHTOKEN_CLIENT = "authtoken.client";
    public static final String AUTHTOKEN_MINE = "authtoken.mine";
    public static final String SIGNED_BY = "signed.by";
    
    public static String SECURITY_NS = "http://issg.cs.duke.edu/sharp/sec";
    public static String HEADER_NAME = "AuthToken";
    public static String NAME_ATTRIBUTE = "name";
    public static String GUID_ATTRIBUTE = "guid";

    public static void attachToken(MessageContext context) throws Exception
    {
        try {
            AuthToken token = (AuthToken) context.getProperty(AUTHTOKEN_MINE);

            if (token != null) {
                SOAPEnvelope env = context.getEnvelope();
                SOAPHeader header = null;

                if (token.getName() == null || token.getGuid() == null){
                    throw new Exception("Invalid auth token");
                }
                
                SOAPFactory factory = OMAbstractFactory.getSOAP12Factory();
                OMNamespace omNs = factory.createOMNamespace(SECURITY_NS, "sec");

                header = env.getHeader();
                if (header == null) {
                    header = factory.createSOAPHeader(env);
                }
                SOAPHeaderBlock block = factory.createSOAPHeaderBlock(HEADER_NAME, omNs);

                block.addAttribute(NAME_ATTRIBUTE, token.getName(), omNs);
                block.addAttribute(GUID_ATTRIBUTE, token.getGuid().toString(), omNs);

                header.addChild(block);
            }
        } catch (Exception e) {
            System.err.println("Exception in filter: " + e.getMessage());
        }
    }

    public static void detachToken(MessageContext context) throws Exception
    {
        try {
            SOAPEnvelope env = context.getEnvelope();
            if (env.getHeader() != null) {
                SOAPHeaderBlock node = (SOAPHeaderBlock) env.getHeader().getFirstChildWithName(new QName(SECURITY_NS, HEADER_NAME, "sec"));
                if (node != null) {
                    String name = null;
                    String guid = null;
                    OMAttribute attribute = node.getAttribute(new QName(SECURITY_NS, NAME_ATTRIBUTE, "sec"));
                    if (attribute != null) {
                        name = attribute.getAttributeValue();
                    }
                    attribute = node.getAttribute(new QName(SECURITY_NS, GUID_ATTRIBUTE, "sec"));
                    if (attribute != null) {
                        guid = attribute.getAttributeValue();
                    }
                    if (name == null || guid == null) {
                        throw new Exception("invalid auth token");
                    }
                    
                    AuthToken authToken = new AuthToken(name, new ID(guid));
                    context.setProperty(AUTHTOKEN_CLIENT, authToken);
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in filter: " + e.getMessage());
        }
    }
    
    public static void checkSecurity(MessageContext context) throws Exception
    {
        Vector<?> results = null;
        if ((results = (Vector<?>) context.getProperty(WSHandlerConstants.RECV_RESULTS)) == null) {
            return;
        } else {
            for (int i = 0; i < results.size(); i++) {
                //Get hold of the WSHandlerResult instance      
                WSHandlerResult rResult = (WSHandlerResult) results.get(i);     
                Vector<?> wsSecEngineResults = rResult.getResults();       
                for (int j = 0; j < wsSecEngineResults.size(); j++) {           
                    //Get hold of the WSSecurityEngineResult instance           
                    WSSecurityEngineResult wser = (WSSecurityEngineResult)wsSecEngineResults.get(j);   
                    if( ( wser.getAction() & WSConstants.SIGN ) > 0 ){
                        X509Certificate cert = wser.getCertificate();
                        if (cert != null){
                            context.setProperty(SIGNED_BY, cert);
                        }
                    }                    
                }
            }
        }
    }
}
