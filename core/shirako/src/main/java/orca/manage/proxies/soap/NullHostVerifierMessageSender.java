package orca.manage.proxies.soap;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.ws.transport.http.HttpsUrlConnectionMessageSender;
 
/**
 * From https://nairshibu.wordpress.com/2014/12/31/bypass-spring-web-service-webservicetemplate-ssl-validation/
 * @author geni-orca
 *
 */
public class NullHostVerifierMessageSender extends HttpsUrlConnectionMessageSender {
 
    private NullHostnameVerifier _hostnameVerifier;
    private SSLSocketFactory _sslSocketFactory;
    
    public class NullHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public NullHostVerifierMessageSender() {
 
        _hostnameVerifier = new NullHostnameVerifier();
    }
 
    public void myInit() {
    	afterPropertiesSet();
    }
    
    @Override
    public void afterPropertiesSet()  {
        setHostnameVerifier(_hostnameVerifier);
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
        setTrustManagers(trustAllCerts);
        setSslProtocol("TLS");
    }
 
}