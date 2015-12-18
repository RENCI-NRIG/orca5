package orca.controllers;

import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class OrcaXmlrpcServlet extends XmlRpcServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static ThreadLocal<String> clientIpAddress = new ThreadLocal<String>();
	private static ThreadLocal<String> sslSessionId = new ThreadLocal<String>();
	private static ThreadLocal<X509Certificate[]> clientCertificateChain = new ThreadLocal<X509Certificate[]>();
	private static Logger Log = OrcaController.getLogger("xmlrpc");
	
	/**
	 * This class synchronizes access to an internal property handler map. Best
	 * guess as to which methods needed to be overridden
	 * @author ibaldin
	 *
	 */
    private static class OrcaPropertyHandlerMapping extends PropertyHandlerMapping {
    	private static final OrcaPropertyHandlerMapping instance = new OrcaPropertyHandlerMapping();
    	private String defaultNS = null;
    	
    	public static OrcaPropertyHandlerMapping getInstance() {
    		return instance;
    	}
    	
    	@Override
    	public void load(ClassLoader pcl, Map m) throws XmlRpcException{
    		return;
    	}
    	
    	@Override
    	public void load(ClassLoader pcl, String res) throws XmlRpcException {
    		return;
    	}
    	
    	@Override
    	public void load(ClassLoader pcl, URL url) throws XmlRpcException {
    		return;
    	}
    	
    	/**
    	 * Allow to skip namespace (for GENI). First match wins.
    	 */
    	@Override
    	public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
    		synchronized(instance) {
    			try {
    				return super.getHandler(handlerName);
    			} catch (XmlRpcNoSuchHandlerException ee) {
    				// try a default namespace
    				return super.getHandler(defaultNS + "." + handlerName);
    			}        			
    		}
    	}
    	
    	@Override
    	public void addHandler(String k, Class c) throws XmlRpcException {
    		synchronized(instance) {
    			// save namespace
    			super.addHandler(k, c);
    		}
    	}
    	
    	@Override
    	public void removeHandler(String k) {
    		synchronized(instance) {
    			// remove namespace
    			super.removeHandler(k);
    		}
    	}
    	
    	/**
    	 * One name space gets to be special and can be invoked as methodName and namespace.methodName 
    	 * @param k
    	 */
    	public void addDefaultNS(String k) {
    		defaultNS = k;
    	}
    }
    
    public static String getClientIpAddress() {
  	  	return (String) clientIpAddress.get();
    }
    
    public static String getSslSessionId() {
  	  	return (String) sslSessionId.get();
    }
    
    public static X509Certificate[] getClientCertificateChain() {
    	return clientCertificateChain.get();
    }
    
    @Override
    public void doPost(HttpServletRequest pRequest,
			HttpServletResponse pResponse) throws IOException, ServletException {
    	
    	clientIpAddress.set(pRequest.getRemoteAddr());
     
    	// if comms are secure, session id will be set
    	sslSessionId.set((String)pRequest.getAttribute("javax.servlet.request.ssl_session_id"));
     
    	// insecure comms are not allowed
    	if (sslSessionId.get() == null) {
    		Log.debug("Client " + pRequest.getRemoteAddr() + " is not using secure communications");
    	}else{
    		Log.debug("Client " + pRequest.getRemoteAddr() + " is using secure communications");
    		/*
    		Log.debug("Available attributes: --------");
        	Enumeration<?> names = pRequest.getAttributeNames();
        	while(names.hasMoreElements()){
        		Log.debug(names.nextElement());
        	}
        	Log.debug("--------");
         	*/
    		
        	X509Certificate[] certChain = (X509Certificate[]) pRequest.getAttribute("javax.servlet.request.X509Certificate");
        	clientCertificateChain.set(certChain);
        	
//        	if (certChain != null)
//        		Globals.Log.debug("Certificate chain in POST of the client " + pRequest.getRemoteAddr() + " is of type " + certChain.getClass().getCanonicalName());
//        	else
//        		Globals.Log.debug("Certificate chain in POST of the client " + pRequest.getRemoteAddr() + " is null");
//         
//        	String sslID = (String)pRequest.getAttribute("javax.servlet.request.ssl_session");
//        	if (sslID == null)
//        		Globals.Log.debug("ssl id in POST of the client " + pRequest.getRemoteAddr() + " is null");
//        	else
//        		Globals.Log.debug("ssl id in POST of the client " + pRequest.getRemoteAddr() + " is " + sslID);
    	}
    	
    	super.doPost(pRequest, pResponse);
    }
    
	@Override
	protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
		return newPropertyHandlerMapping(null);
	}
	
	/**
	 * Construct a property handler mapping based on globally registered XMLRPC components
	 */
	@Override
	protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws XmlRpcException {
		Log.debug("In newPropertyHandlerMapping for " + url);
		PropertyHandlerMapping phm = OrcaPropertyHandlerMapping.getInstance();

		phm.setVoidMethodEnabled(true);

		// add introspection, so we can call 'system.listMethods'
        XmlRpcSystemImpl.addSystemHandler(phm);
        
        // context
        ServletContext ctx = this.getServletContext();
        
        // add an attribute if you need to store something on the context
        // e.g. in need of cleanup on shutdown
        //ctx.setAttribute(this.class.getCanonicalName(), s);
        
        return phm;
	}
	
    public static void addXmlrpcHandler(String key, Class<?> clazz, boolean defNS) throws XmlRpcException{
    	OrcaPropertyHandlerMapping.getInstance().addHandler(key, clazz);
    	if (defNS)
    		OrcaPropertyHandlerMapping.getInstance().addDefaultNS(key);
    }
    
    public static void delXmlrpcHandler(String key) {
    	OrcaPropertyHandlerMapping.getInstance().removeHandler(key);
    }
}
