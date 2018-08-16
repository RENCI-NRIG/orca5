/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package orca.ektorp.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import orca.util.ssl.ContextualSSLProtocolSocketFactory.HostPortPair;
import orca.util.ssl.ContextualSSLProtocolSocketFactory.SSLContextFactory;

import org.apache.http.HttpHost;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpInetSocketAddress;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.scheme.LayeredSchemeSocketFactory;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.SchemeLayeredSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.conn.ssl.*;

/**
 * Layered socket factory for TLS/SSL connections.
 * <p>
 * SSLSocketFactory can be used to validate the identity of the HTTPS server against a list of
 * trusted certificates and to authenticate to the HTTPS server using a private key.
 * <p>
 * SSLSocketFactory will enable server authentication when supplied with
 * a {@link KeyStore trust-store} file containing one or several trusted certificates. The client
 * secure socket will reject the connection during the SSL session handshake if the target HTTPS
 * server attempts to authenticate itself with a non-trusted certificate.
 * <p>
 * Use JDK keytool utility to import a trusted certificate and generate a trust-store file:
 *    <pre>
 *     keytool -import -alias "my server cert" -file server.crt -keystore my.truststore
 *    </pre>
 * <p>
 * In special cases the standard trust verification process can be bypassed by using a custom
 * {@link TrustStrategy}. This interface is primarily intended for allowing self-signed
 * certificates to be accepted as trusted without having to add them to the trust-store file.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#CONNECTION_TIMEOUT}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#SO_TIMEOUT}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#SO_REUSEADDR}</li>
 * </ul>
 * <p>
 * SSLSocketFactory will enable client authentication when supplied with
 * a {@link KeyStore key-store} file containing a private key/public certificate
 * pair. The client secure socket will use the private key to authenticate
 * itself to the target HTTPS server during the SSL session handshake if
 * requested to do so by the server.
 * The target HTTPS server will in its turn verify the certificate presented
 * by the client in order to establish client's authenticity
 * <p>
 * Use the following sequence of actions to generate a key-store file
 * </p>
 *   <ul>
 *     <li>
 *      <p>
 *      Use JDK keytool utility to generate a new key
 *      <pre>keytool -genkey -v -alias "my client key" -validity 365 -keystore my.keystore</pre>
 *      For simplicity use the same password for the key as that of the key-store
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *      Issue a certificate signing request (CSR)
 *      <pre>keytool -certreq -alias "my client key" -file mycertreq.csr -keystore my.keystore</pre>
 *     </p>
 *     </li>
 *     <li>
 *      <p>
 *      Send the certificate request to the trusted Certificate Authority for signature.
 *      One may choose to act as her own CA and sign the certificate request using a PKI
 *      tool, such as OpenSSL.
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *       Import the trusted CA root certificate
 *       <pre>keytool -import -alias "my trusted ca" -file caroot.crt -keystore my.keystore</pre>
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *       Import the PKCS#7 file containg the complete certificate chain
 *       <pre>keytool -import -alias "my client key" -file mycert.p7 -keystore my.keystore</pre>
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *       Verify the content the resultant keystore file
 *       <pre>keytool -list -v -keystore my.keystore</pre>
 *      </p>
 *     </li>
 *   </ul>
 *
 * @since 4.0
 */

/**
 * 
 * @author claris
 * TODO: See DecorateTrustManager below. It needs to be uncomment. Eventually I will have to compile this class against the source code of HTTP-component.
 */
@SuppressWarnings("deprecation")
@ThreadSafe
public class ContextualSSLSocketFactory implements SchemeLayeredSocketFactory,
                                         LayeredSchemeSocketFactory, LayeredSocketFactory {

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
	    
    public static final String TLS   = "TLS";
    public static final String SSL   = "SSL";
    public static final String SSLV2 = "SSLv2";

    public static final X509HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER
        = new AllowAllHostnameVerifier();

    public static final X509HostnameVerifier BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
        = new BrowserCompatHostnameVerifier();

    public static final X509HostnameVerifier STRICT_HOSTNAME_VERIFIER
        = new StrictHostnameVerifier();

    /**
     * Obtains default SSL socket factory with an SSL context based on the standard JSSE
     * trust material (<code>cacerts</code> file in the security properties directory).
     * System properties are not taken into consideration.
     *
     * @return default SSL socket factory
     */
    public static ContextualSSLSocketFactory getSocketFactory() throws SSLInitializationException {
        SSLContext sslcontext;
        try {
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, null, null);
            return new ContextualSSLSocketFactory(
                sslcontext,
                BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        } catch (NoSuchAlgorithmException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        } catch (KeyManagementException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        }
    }

    /**
     * Obtains default SSL socket factory with an SSL context based on system properties
     * as described in
     * <a href="http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/JSSERefGuide.html">
     * "JavaTM Secure Socket Extension (JSSE) Reference Guide for the JavaTM 2 Platform
     * Standard Edition 5</a>
     *
     * @return default system SSL socket factory
     */
    public static ContextualSSLSocketFactory getSystemSocketFactory() throws SSLInitializationException {
        return new ContextualSSLSocketFactory(
            (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault(),
            BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    private final javax.net.ssl.SSLSocketFactory socketfactory;
    private final HostNameResolver nameResolver;
    // TODO: make final
    private volatile X509HostnameVerifier hostnameVerifier;

    private static SSLContext createSSLContext(
            String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final TrustStrategy trustStrategy)
                throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        if (algorithm == null) {
            algorithm = TLS;
        }
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, keystorePassword != null ? keystorePassword.toCharArray(): null);
        KeyManager[] keymanagers =  kmfactory.getKeyManagers();
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(truststore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        if (trustmanagers != null && trustStrategy != null) {
            for (int i = 0; i < trustmanagers.length; i++) {
                TrustManager tm = trustmanagers[i];
                /*
                 * @TODO: I need to uncomment the 3 lines below. TrustManagerDecorator is not public (package visibility)
                 */
               // if (tm instanceof X509TrustManager) {
                //    trustmanagers[i] = new TrustManagerDecorator(
                //            (X509TrustManager) tm, trustStrategy);
                //}
            }
        }

        SSLContext sslcontext = SSLContext.getInstance(algorithm);
        sslcontext.init(keymanagers, trustmanagers, random);
        return sslcontext;
    }
    
    /**
     * @deprecated Use {@link SSLSocketFactory#SSLSocketFactory(String, KeyStore, String, KeyStore, SecureRandom, X509HostnameVerifier)}
     * @param algorithm string specifying algorithm
     * @param keystore key store
     * @param keystorePassword string containing key store password
     * @param truststore trust store
     * @param random random
     * @param nameResolver host name resolver
     * @throws NoSuchAlgorithmException in case of invalid algorithm
     * @throws KeyManagementException in case of invalid key
     * @throws KeyStoreException in case of invalid key 
     * @throws UnrecoverableKeyException in case of unrecoverable key     
     */
    @Deprecated
    public ContextualSSLSocketFactory(
            final String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final HostNameResolver nameResolver)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(createSSLContext(
                algorithm, keystore, keystorePassword, truststore, random, null),
                nameResolver);
    }

    /**
     * @since 4.1
     * @param algorithm string specifying algorithm
     * @param keystore key store
     * @param keystorePassword string containing key store password
     * @param truststore trust store
     * @param random random
     * @param hostnameVerifier host name verifier 
     * @throws NoSuchAlgorithmException in case of invalid algorithm
     * @throws KeyManagementException in case of invalid key
     * @throws KeyStoreException in case of invalid key 
     * @throws UnrecoverableKeyException in case of unrecoverable key     
     */
    public ContextualSSLSocketFactory(
            String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final X509HostnameVerifier hostnameVerifier)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(createSSLContext(
                algorithm, keystore, keystorePassword, truststore, random, null),
                hostnameVerifier);
    }

    /**
     * @since 4.1
     * @param algorithm string specifying algorithm
     * @param keystore key store
     * @param keystorePassword string containing key store password
     * @param truststore trust store
     * @param random random
     * @param trustStrategy trust strategy 
     * @param hostnameVerifier host name verifier 
     * @throws NoSuchAlgorithmException in case of invalid algorithm
     * @throws KeyManagementException in case of invalid key
     * @throws KeyStoreException in case of invalid key 
     * @throws UnrecoverableKeyException in case of unrecoverable key     
     */
    public ContextualSSLSocketFactory(
            String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final TrustStrategy trustStrategy,
            final X509HostnameVerifier hostnameVerifier)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(createSSLContext(
                algorithm, keystore, keystorePassword, truststore, random, trustStrategy),
                hostnameVerifier);
    }

    public ContextualSSLSocketFactory(
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, keystore, keystorePassword, truststore, null, null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    public ContextualSSLSocketFactory(
            final KeyStore keystore,
            final String keystorePassword)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException{
        this(TLS, keystore, keystorePassword, null, null, null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    public ContextualSSLSocketFactory(
            final KeyStore truststore)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, null, null, truststore, null, null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    /**
     * @since 4.1
     * @param trustStrategy trust strategy 
     * @param hostnameVerifier host name verifier 
     * @throws NoSuchAlgorithmException in case of invalid algorithm
     * @throws KeyManagementException in case of invalid key
     * @throws KeyStoreException in case of invalid key 
     * @throws UnrecoverableKeyException in case of unrecoverable key     
     */
    public ContextualSSLSocketFactory(
            final TrustStrategy trustStrategy,
            final X509HostnameVerifier hostnameVerifier)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, null, null, null, null, trustStrategy, hostnameVerifier);
    }

    /**
     * @since 4.1
     * @param trustStrategy trust strategy 
     * @throws NoSuchAlgorithmException in case of invalid algorithm
     * @throws KeyManagementException in case of invalid key
     * @throws KeyStoreException in case of invalid key 
     * @throws UnrecoverableKeyException in case of unrecoverable key     
     */
    public ContextualSSLSocketFactory(
            final TrustStrategy trustStrategy)
                throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, null, null, null, null, trustStrategy, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    public ContextualSSLSocketFactory(final SSLContext sslContext) {
        this(sslContext, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    /**
     * @deprecated Use {@link SSLSocketFactory#SSLSocketFactory(SSLContext)}
     * @param sslContext SSL Context
     * @param nameResolver Host Name Resolver
     */
    @Deprecated
    public ContextualSSLSocketFactory(
            final SSLContext sslContext, final HostNameResolver nameResolver) {
        super();
        this.socketfactory = sslContext.getSocketFactory();
        this.hostnameVerifier = BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        this.nameResolver = nameResolver;
    }

    /**
     * @since 4.1
     * @param sslContext SSL Context
     * @param hostnameVerifier Host Name Verifier 
     */
    public ContextualSSLSocketFactory(
            final SSLContext sslContext, final X509HostnameVerifier hostnameVerifier) {
        super();
        if (sslContext == null) {
            throw new IllegalArgumentException("SSL context may not be null");
        }
        this.socketfactory = sslContext.getSocketFactory();
        this.hostnameVerifier = hostnameVerifier;
        this.nameResolver = null;
    }

    /**
     * @since 4.2
     * @param socketfactory SSL Socket Factory
     * @param hostnameVerifier Host Name Verifier 
     */
    public ContextualSSLSocketFactory(
            final javax.net.ssl.SSLSocketFactory socketfactory, 
            final X509HostnameVerifier hostnameVerifier) {
        if (socketfactory == null) {
            throw new IllegalArgumentException("SSL socket factory may not be null");
        }
        this.socketfactory = socketfactory;
        this.hostnameVerifier = hostnameVerifier;
        this.nameResolver = null;
    }

    /**
     * @param params Optional parameters. Parameters passed to this method will have no effect.
     *               This method will create a unconnected instance of {@link Socket} class.
     * @since 4.1
     * @throws IOException in case of error
     * @return returns the created socket
     */
    public Socket createSocket(final HttpParams params) throws IOException {
    	//Here!
        SSLSocket sock = (SSLSocket) this.socketfactory.createSocket();
        prepareSocket(sock);
        return sock;
    }

    @Deprecated
    public Socket createSocket() throws IOException {
    	//Here
        SSLSocket sock = (SSLSocket) this.socketfactory.createSocket();
        prepareSocket(sock);
        return sock;
    }

    /**
     * @since 4.1
     * @param socket socket
     * @param remoteAddress remote address
     * @param localAddress local address
     * @param params HTTP params
     * @throws IOException in case of IO error
     * @throws UnknownHostException in case of unknonwn host
     * @throws ConnectTimeoutException in case of connect timeout
     * @return returns the socket
     */
    public Socket connectSocket(
            final Socket socket,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Remote address may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        //Here!
        Socket sock = socket != null ? socket : this.socketfactory.createSocket();
        if (localAddress != null) {
            sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
            sock.bind(localAddress);
        }

        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        try {
            sock.setSoTimeout(soTimeout);
            sock.connect(remoteAddress, connTimeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }

        String hostname;
        if (remoteAddress instanceof HttpInetSocketAddress) {
            hostname = ((HttpInetSocketAddress) remoteAddress).getHttpHost().getHostName();
        } else {
            hostname = remoteAddress.getHostName();
        }

        SSLSocket sslsock;
        // Setup SSL layering if necessary
        if (sock instanceof SSLSocket) {
            sslsock = (SSLSocket) sock;
        } else {
            int port = remoteAddress.getPort();
            sslsock = (SSLSocket) this.socketfactory.createSocket(sock, hostname, port, true);
            prepareSocket(sslsock);
        }
        if (this.hostnameVerifier != null) {
            try {
                this.hostnameVerifier.verify(hostname, sslsock);
                // verifyHostName() didn't blowup - good!
            } catch (IOException iox) {
                // close the socket before re-throwing the exception
                try { sslsock.close(); } catch (Exception x) { /*ignore*/ }
                throw iox;
            }
        }
        return sslsock;
    }


    /**
     * Checks whether a socket connection is secure.
     * This factory creates TLS/SSL socket connections
     * which, by default, are considered secure.
     * &lt;br/&gt;
     * Derived classes may override this method to perform
     * runtime checks, for example based on the cypher suite.
     *
     * @param sock      the connected socket
     *
     * @return  <code>true</code>
     *
     * @throws IllegalArgumentException if the argument is invalid
     */
    public boolean isSecure(final Socket sock) throws IllegalArgumentException {
        if (sock == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        // This instanceof check is in line with createSocket() above.
        if (!(sock instanceof SSLSocket)) {
            throw new IllegalArgumentException("Socket not created by this factory");
        }
        // This check is performed last since it calls the argument object.
        if (sock.isClosed()) {
            throw new IllegalArgumentException("Socket is closed");
        }
        return true;
    }

    /**
     * @since 4.2
     * @param socket socket
     * @param host host 
     * @param port port
     * @param params HTTP params
     * @throws IOException in case of IO error
     * @throws UnknownHostException in case of unknonwn host
     * @return returns the socket
     */
    public Socket createLayeredSocket(
        final Socket socket,
        final String host,
        final int port,
        final HttpParams params) throws IOException, UnknownHostException {
    	//Here!
        SSLSocket sslSocket = (SSLSocket) this.socketfactory.createSocket(
              socket,
              host,
              port,
              true);
        prepareSocket(sslSocket);
        if (this.hostnameVerifier != null) {
            this.hostnameVerifier.verify(host, sslSocket);
        }
        // verifyHostName() didn't blowup - good!
        return sslSocket;
    }

    /**
     * @deprecated use {@link #createLayeredSocket(Socket, String, int, HttpParams)}
     * @param socket socket
     * @param host host 
     * @param port port
     * @param autoClose flag indicating if socket should be autoClosed
     * @throws IOException in case of IO error
     * @throws UnknownHostException in case of unknonwn host
     * @return returns the socket
     */
    public Socket createLayeredSocket(
        final Socket socket,
        final String host,
        final int port,
        final boolean autoClose) throws IOException, UnknownHostException {
    	//Here
        SSLSocket sslSocket = (SSLSocket) this.socketfactory.createSocket(
              socket,
              host,
              port,
              autoClose
        );
        prepareSocket(sslSocket);
        if (this.hostnameVerifier != null) {
            this.hostnameVerifier.verify(host, sslSocket);
        }
        // verifyHostName() didn't blowup - good!
        return sslSocket;
    }

    @Deprecated
    public void setHostnameVerifier(X509HostnameVerifier hostnameVerifier) {
        if ( hostnameVerifier == null ) {
            throw new IllegalArgumentException("Hostname verifier may not be null");
        }
        this.hostnameVerifier = hostnameVerifier;
    }

    public X509HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    /**
     * @deprecated Use {@link #connectSocket(Socket, InetSocketAddress, InetSocketAddress, HttpParams)}
     * @param socket socket
     * @param host host 
     * @param port port
     * @param localAddress local address
     * @param localPort local port
     * @param params HTTP params
     * @throws IOException in case of IO error
     * @throws UnknownHostException in case of unknonwn host
     * @throws ConnectTimeoutException in case of connect timeout 
     * @return returns the socket
     */
    @Deprecated
    public Socket connectSocket(
            final Socket socket,
            final String host, int port,
            final InetAddress localAddress, int localPort,
            final HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        InetSocketAddress local = null;
        if (localAddress != null || localPort > 0) {
            // we need to bind explicitly
            if (localPort < 0) {
                localPort = 0; // indicates "any"
            }
            local = new InetSocketAddress(localAddress, localPort);
        }
        InetAddress remoteAddress;
        if (this.nameResolver != null) {
            remoteAddress = this.nameResolver.resolve(host);
        } else {
            remoteAddress = InetAddress.getByName(host);
        }
        InetSocketAddress remote = new HttpInetSocketAddress(new HttpHost(host, port), remoteAddress, port);
        return connectSocket(socket, remote, local, params);
    }

    /**
     * @deprecated Use {@link #createLayeredSocket(Socket, String, int, boolean)}
     * @param socket socket
     * @param host host 
     * @param port port
     * @param autoClose flag indicating if socket should be autoClosed
     * @throws IOException in case of IO error
     * @throws UnknownHostException in case of unknonwn host
     * @return returns the socket
     */
    @Deprecated
    public Socket createSocket(
            final Socket socket,
            final String host, int port,
            boolean autoClose) throws IOException, UnknownHostException {
        return createLayeredSocket(socket, host, port, autoClose);
    }

    /**
     * Performs any custom initialization for a newly created SSLSocket
     * (before the SSL handshake happens).
     *
     * The default implementation is a no-op, but could be overriden to, e.g.,
     * call {@link SSLSocket#setEnabledCipherSuites(java.lang.String[])}.
     *
     * @since 4.2
     * @param socket SSL socket
     * @throws IOException in case of error
     */
    protected void prepareSocket(final SSLSocket socket) throws IOException {
    }
}
