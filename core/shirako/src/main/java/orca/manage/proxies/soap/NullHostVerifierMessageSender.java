package orca.manage.proxies.soap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import org.springframework.ws.transport.http.CommonsHttpMessageSender;
 
/**
 * From https://nairshibu.wordpress.com/2014/12/31/bypass-spring-web-service-webservicetemplate-ssl-validation/
 * @author geni-orca
 *
 * Re-vamped by vjo, after discovering bugs in JVM KeepAliveCache implementation.
 *
 */
public class NullHostVerifierMessageSender extends CommonsHttpMessageSender {
    private static final int DEFAULT_CONNECTION_TIMEOUT_MILLISECONDS = (10 * 1000);
    private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (5 * 1000);
    private static final int CONNECTION_EXPIRY_MILLISECONDS = (15 * 1000);
    private static final int CONNECTION_EXPIRE_AGE_MILLISECONDS = (20 * 1000);
    private final Timer expiryTimer = new Timer("MessageSenderExpiry", true);
    private final Protocol nullhttps = new Protocol("https",
                                                    new NullSSLProtocolSocketFactory(),
                                                    443);

    public NullHostVerifierMessageSender() {
        setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_MILLISECONDS);
        setReadTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS);
    }
 
    public void myInit() {
    	afterPropertiesSet();
    }
    
    @Override
    public void afterPropertiesSet() {
        HttpClient hc = getHttpClient();
        NullHostConfiguration nhc = new NullHostConfiguration(hc.getHostConfiguration());
        hc.setHostConfiguration(nhc);
        if (getCredentials() != null) {
            hc.getState().setCredentials(getAuthScope(), getCredentials());
            hc.getParams().setAuthenticationPreemptive(true);
        }
        expiryTimer.schedule(new ConnectionExpiryTask(),
                             CONNECTION_EXPIRY_MILLISECONDS,
                             CONNECTION_EXPIRY_MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
        expiryTimer.cancel();
        super.destroy();
    }

    private class ConnectionExpiryTask extends TimerTask {
        public void run() {
            HttpConnectionManager connMgr =
                getHttpClient().getHttpConnectionManager();
            connMgr.closeIdleConnections(CONNECTION_EXPIRE_AGE_MILLISECONDS);
        }
    }

    private class NullSSLProtocolSocketFactory
        implements SecureProtocolSocketFactory {

        private SSLContext sslcontext = null;

        public NullSSLProtocolSocketFactory() {
        }

        private SSLContext createNullSSLContext() {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
 
                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }
 
                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }
                } };

            try {
                SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(
                    null,
                    trustAllCerts,
                    null);
                return context;
            } catch (Exception e) {
                throw new HttpClientError(e.toString());
            }
        }

        private SSLContext getSSLContext() {
            if (this.sslcontext == null) {
                this.sslcontext = createNullSSLContext();
            }
            return this.sslcontext;
        }

        public Socket createSocket(
            String host,
            int port,
            InetAddress clientHost,
            int clientPort)
            throws IOException, UnknownHostException {

            return getSSLContext().getSocketFactory().createSocket(
                host,
                port,
                clientHost,
                clientPort
                );
        }

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
            SocketFactory socketfactory = getSSLContext().getSocketFactory();
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

        public Socket createSocket(String host, int port)
            throws IOException, UnknownHostException {
            return getSSLContext().getSocketFactory().createSocket(
                host,
                port
                );
        }

        public Socket createSocket(
            Socket socket,
            String host,
            int port,
            boolean autoClose)
            throws IOException, UnknownHostException {
            return getSSLContext().getSocketFactory().createSocket(
                socket,
                host,
                port,
                autoClose
                );
        }

        public boolean equals(Object obj) {
            return ((obj != null) && obj.getClass().equals(NullSSLProtocolSocketFactory.class));
        }

        public int hashCode() {
            return NullSSLProtocolSocketFactory.class.hashCode();
        }
    }

    private class NullHostConfiguration
        extends HostConfiguration {

        public NullHostConfiguration() {
        }

        public NullHostConfiguration(HostConfiguration hostConfiguration) {
            super(hostConfiguration);
            overrideHttps();
        }

        public Object clone() {
            return new NullHostConfiguration(this);
        }

        public synchronized void setHost(final HttpHost host) {
            super.setHost(host);
            overrideHttps();
        }

        public synchronized void setHost(final String host, int port, final String protocol) {
            super.setHost(host, port, protocol);
            overrideHttps();
        }

        private synchronized void overrideHttps() {
            Protocol proto = getProtocol();
            String host = getHost();
            if (null != proto && null != host) {
                if ("https" == proto.getScheme()) {
                    super.setHost(host,
                                  getPort(),
                                  nullhttps);
                }
            }
        }
    }
}
