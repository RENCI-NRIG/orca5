package orca.util.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * <p>
 * This class implements an ContextualSSLProtocolSocketFactory.SSLContextFactory interface
 * by using a MultiKeyManager for key management and either a default all-trusting trust-manager
 * or a custom trust manager. 
 * </p>
 * <p>
 * This is meant to be used with the ContextualSSLProtocolSocketFactory in XMLRPC calls
 * </p>
 * @author ibaldin
 *
 */
public class MultiKeySSLContextFactory implements ContextualSSLProtocolSocketFactory.SSLContextFactory {
	private final MultiKeyManager mkm;
	private final TrustManager[] tms;
	
	// Create a trust manager that does not validate certificate chains
	TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}
			}
	};
	
	/**
	 * Will use an all-trusting trust manager (no validation)
	 * @param m (can be null)
	 */
	public MultiKeySSLContextFactory(MultiKeyManager m) {
		mkm = m;
		tms = trustAllCerts;
	}
	
	/**
	 * Will use a custom trust manager
	 * @param m (can be null)
	 * @param tm
	 */
	public MultiKeySSLContextFactory(MultiKeyManager m, TrustManager[] tm) {
		mkm = m;
		tms = tm;
	}
	
	public SSLContext createSSLContext() {
    	// create our own context for this host/port 
    	SSLContext sslcontext = null;
    	try {
    		sslcontext = SSLContext.getInstance("TLS");
    		KeyManager[] kms = null;
    		if (mkm != null) {
    			 KeyManager[] kms1 = { mkm };
    			 kms = kms1;
    		}
    		sslcontext.init(kms, tms, new java.security.SecureRandom());
    	} catch (NoSuchAlgorithmException e1) {
    		;
    	} catch (KeyManagementException e2) {
    		;
    	}
        return sslcontext;
	}
	
}
