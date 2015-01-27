package orca.util.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * This is a derivative of AuthSSLProtocolSocketFactory so all usage cases apply
 * <p>
 * Example of using custom protocol socket factory for a specific host:
 *     <pre>
 *     ContextualSSLSocketFactory csf = new ContextualSSLSocketFactory();
 *     FancyContextFactory fcf = new FancyContextFactory() // implements ContextualSSLSocketFactory.SSLContextFactory
 *     csf.addHostSocketFactory(fcf, myhost, myport);
 *     
 *     Protocol hostSpecificHttps = new Protocol("https", csf, 443);
 *     Protocol.registerProtocol("https", hostSpecificHttps);
 *     </pre>
 * </p>
 * <p>
 * When using with apache XMLRPC, remember to use <b>XmlRpcCommonsTransportFactory</b> with client,
 * otherwise this will be ignored and default JSSE mechanisms will be used
 * </p>
 */
public class ContextualSSLProtocolSocketFactory implements
		SecureProtocolSocketFactory {
    private Map<HostPortPair, SSLContextFactory> contexts = new HashMap<HostPortPair, SSLContextFactory>();
    private SSLContext defaultContext;

    /**
     * Comparable host/port pair
     * @author ibaldin
     *
     */
    public static class HostPortPair implements Comparable<HostPortPair>{
    	private final InetAddress host;
    	private final int port;
    	
    	public HostPortPair(String h, int p) {
    		InetAddress th;
    		try {
    			th = InetAddress.getByName(h);
    		} catch (Exception e) {
    			th = null;
    		}
    		host = th;
    		port = p;
    	}
    	
    	public HostPortPair(InetAddress h, int p) {
    		host = h;
    		port = p;
    	}
    	
    	public int compareTo(HostPortPair other) {
    		if (other.port > port)
    			return -1;
    		
    		if (other.port < port)
    			return 1;
    		
    		if ((host == null) && (other.host == null))
    			return 0;
    		
    		if ((host == null) && (other.host != null))
    			return 1;

    		if (other.host == null)
    			return 1;

    		return Arrays.toString(other.host.getAddress()).compareTo(Arrays.toString(host.getAddress()));
    	}
    	
    	@Override
    	public boolean equals(Object otherO) {
    		
    		if (!(otherO instanceof HostPortPair))
    			return false;
    		HostPortPair other = (HostPortPair)otherO;
    		
    		if (other == null)
    			return false;
    		
    		if ((Arrays.equals(host.getAddress(), other.host.getAddress())) && (port == other.port))
    			return true;
    		
    		return false;
    	}
    	
    	@Override
    	public int hashCode() {
    		if (host == null)
    			return port;
    		return port | Arrays.toString(host.getAddress()).hashCode();
    	}
    	
    	@Override
    	public String toString() {
    		return host + ":" + port;
    	}
    }
    
    /**
     * Simple factory interface to generate ssl contexts
     * @author ibaldin
     *
     */
    public interface SSLContextFactory {
    	SSLContext createSSLContext();
    }
    
    public ContextualSSLProtocolSocketFactory() {
    	defaultContext = getDefaultSSLContext();
    }
    
    /**
     * Associate a custom context factory with host:port
     * @param fact
     * @param host
     * @param port
     */
    public synchronized void addHostContextFactory(SSLContextFactory fact, String host, int port) {
    	contexts.put(new HostPortPair(host, port), fact);
    }
    
    private SSLContext getDefaultSSLContext() {
    	SSLContext ret = null;
    	try {
    		ret = SSLContext.getInstance("TLS");
    		ret.init(null, null, null);
    	} catch (NoSuchAlgorithmException e) {
    		;
    	} catch (KeyManagementException e1) {
    		;
    	}
    	return ret;
    }
    
    /**
     * This does all the magic - returns custom context
     * for specific host/port, default otherwise
     * @param - host connected to
     * @param - port port connected to
     * @return
     */
    private synchronized SSLContext getSSLContext(String h, int p) {
    	HostPortPair check = new HostPortPair(h, p);

    	if (contexts.containsKey(check)) {
    		return contexts.get(check).createSSLContext();
    	}
    	else {
    		return defaultContext;
    	}
    }

    /**
     * Attempts to get a new socket connection to the given host within the given time limit.
     * <p>
     * To circumvent the limitations of older JREs that do not support connect timeout a 
     * controller thread is executed. The controller thread attempts to create a new socket 
     * within the given limit of time. If socket constructor does not return until the 
     * timeout expires, the controller terminates and throws an {@link ConnectTimeoutException}
     * </p>
     *  
     * @param host the host name/IP
     * @param port the port on the host
     * @param clientHost the local host name/IP to bind the socket to
     * @param clientPort the port on the local machine
     * @param params {@link HttpConnectionParams Http connection parameters}
     * 
     * @return Socket a new socket
     * 
     * @throws IOException if an I/O error occurs while creating the socket
     * @throws UnknownHostException if the IP address of the host cannot be
     * determined
     */
    public Socket createSocket(
        final String host,
        final int port,
        final InetAddress localAddress,
        final int localPort,
        final HttpConnectionParams params
    ) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        SocketFactory socketfactory = getSSLContext(host, port).getSocketFactory();
        if (timeout == 0) {
            return socketfactory.createSocket(host, port, localAddress, localPort);
        } else {
            Socket socket = socketfactory.createSocket();
            SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
            SocketAddress remoteaddr = new InetSocketAddress(host, port);
            socket.bind(localaddr);
            socket.connect(remoteaddr, timeout);
            return socket;
        }
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(
        String host,
        int port,
        InetAddress clientHost,
        int clientPort)
        throws IOException, UnknownHostException
   {
       return getSSLContext(host, port).getSocketFactory().createSocket(
            host,
            port,
            clientHost,
            clientPort
        );
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        return getSSLContext(host, port).getSocketFactory().createSocket(
            host,
            port
        );
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
     */
    public Socket createSocket(
        Socket socket,
        String host,
        int port,
        boolean autoClose)
        throws IOException, UnknownHostException
    {
        return getSSLContext(host, port).getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose
        );
    }
}
