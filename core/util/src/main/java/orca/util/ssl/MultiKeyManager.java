package orca.util.ssl;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

/**
 * <p>
 * This class implements an X509KeyManager based on an ephemeral keystore
 * that is filled with certificates/private keys corresponding to unique strings/guids
 * Which client key is returned depends on a threadlocal variable guid that can be
 * set via setCurrentGuid(String guid) call. 
 * </p>
 * <p>
 * The intent of the class is to be used together with the ContextualSSLProtocolFactory
 * which can then be used with apache XMLRPC client. By calling setCurrentGuid() immediately
 * prior to making XMLRPC call, you can ensure that the XMLRPC client uses a specific identity.
 * </p>
 * <p>
 * The implementation is thread safe
 * </p>
 * @author ibaldin
 *
 */
public class MultiKeyManager implements X509KeyManager {
	private final X509KeyManager jvmKeyManager;
	private KeyStore cks;
	private static ThreadLocal<String> guid = new ThreadLocal<String>();
	private X509KeyManager customX509KeyManager;
	
	public MultiKeyManager() {
		String alg = KeyManagerFactory.getDefaultAlgorithm();
		X509KeyManager km = null;
		try {
			// default key manager
			KeyManagerFactory dkmFact = KeyManagerFactory.getInstance(alg); 
			dkmFact.init(null,null);  

			km = getX509KeyManager(alg, dkmFact);
		} catch (Exception e) {
			;
		}
		jvmKeyManager = km;
		
		// create an ephemeral keystore for custom keys
		try {
			cks = KeyStore.getInstance("JKS");
			cks.load(null, null);
		} catch (Exception e) {
			;
		}
		
		try {
			KeyManagerFactory kmFact = KeyManagerFactory.getInstance(alg);
			kmFact.init(cks, "".toCharArray());
			customX509KeyManager = MultiKeyManager.getX509KeyManager(alg, kmFact);
		} catch (Exception e) {
			;
		}
	}
	
	public void setCurrentGuid(String g) {
		if (g != null)
			guid.set(g);
	}
	
	/**
	 * Add a private key with specific alias/guid to multikey entry
	 * @param g guid
	 * @param k key
	 * @param c certificate
	 */
	public synchronized void addPrivateKey(String g, PrivateKey k, Certificate c) {
		Certificate[] chain = { c };
		try {
			cks.setKeyEntry(g, k, "".toCharArray(), chain);
		} catch (KeyStoreException e) {
			System.out.println("KeyStoreException " + e);
		}
		// any time we add the key, we should do this
		try {
			String alg = KeyManagerFactory.getDefaultAlgorithm();
			KeyManagerFactory kmFact = KeyManagerFactory.getInstance(alg);
			kmFact.init(cks, "".toCharArray());
			customX509KeyManager = getX509KeyManager(alg, kmFact);
		} catch (Exception e) {
			;
		}
		// set to last caller
		guid.set(g);
	}
	
	/**
	 * Add a private key with a certificate chain.
	 * @param g guid
	 * @param k key
	 * @param chain certificate array
	 */
	public synchronized void addPrivateKey(String g, PrivateKey k, Certificate[] chain) {
		try {
			cks.setKeyEntry(g, k, "".toCharArray(), chain);
		} catch (KeyStoreException e) {
			System.out.println("KeyStoreException " + e);
		}
		// any time we add the key, we should do this
		try {
			String alg = KeyManagerFactory.getDefaultAlgorithm();
			KeyManagerFactory kmFact = KeyManagerFactory.getInstance(alg);
			kmFact.init(cks, "".toCharArray());
			customX509KeyManager = getX509KeyManager(alg, kmFact);
		} catch (Exception e) {
			;
		}
		// set to last caller
		guid.set(g);
	}
	
	/**
	 * Add a trusted certificate to multikey store
	 * @param g guid
	 * @param c certificate
	 */
	public synchronized void addTrustedCert(String g, Certificate c) {
		try {
			cks.setCertificateEntry(g, c);
		} catch (KeyStoreException e) {
			System.out.println("KeyStoreException " + e); 
		}
		
		// any time we add the key, we should do this
		try {
			String alg = KeyManagerFactory.getDefaultAlgorithm();
			KeyManagerFactory kmFact = KeyManagerFactory.getInstance(alg);
			kmFact.init(cks, "".toCharArray());
			customX509KeyManager = getX509KeyManager(alg, kmFact);
		} catch (Exception e) {
			;
		}
	}
	
	private synchronized X509Certificate[] getCustomCertificateChainMT(String alias) {		
		return customX509KeyManager.getCertificateChain(alias);
	}
	
	private synchronized PrivateKey getCustomPrivateKeyMT(String alias) {
		return customX509KeyManager.getPrivateKey(alias);
	}
	
	private synchronized String[] getCustomAliasesMT(String keyType, Principal[] issuers) {
		return customX509KeyManager.getClientAliases(keyType, issuers);
	}
	
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		// return the selected guid
		return guid.get();
	}

	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		// just return whatever default key manager says
		return jvmKeyManager.chooseServerAlias(keyType, issuers, socket);
	}

	/**
	 * Get cert chain based on selected guid
	 */
	public X509Certificate[] getCertificateChain(String alias) {

		X509Certificate[] x509chain = null;
		
		x509chain = getCustomCertificateChainMT(alias);
		
		if( x509chain == null || x509chain.length == 0) {
			x509chain = jvmKeyManager.getCertificateChain(alias);
		} 
		
		return x509chain;
	}

	/**
	 * collect all aliases
	 */
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		
		return getCustomAliasesMT(keyType, issuers);
	}

	/**
	 * Get private key from custom key store or JVM
	 */
	public PrivateKey getPrivateKey(String alias) {
		// pick the right key store/manager and return the key chain
		PrivateKey key = null;
		key = getCustomPrivateKeyMT(alias);
		
		if( key == null ) {
			key = jvmKeyManager.getPrivateKey(alias);
		} 
		return key;
	}

	/**
	 * Only JVM aliases are returned
	 */
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		// only return JVM aliases here
		return jvmKeyManager.getServerAliases(keyType, issuers);
	}
	
	/**
	 * Find a X509 Key Manager compatible with a particular algorithm
	 * @param algorithm algorithm
	 * @param kmFact key manager factory
	 * @return returns X509 Key Manager
	 * @throws NoSuchAlgorithmException in case of failure to generate key manager
	 */
	public static X509KeyManager getX509KeyManager(String algorithm, KeyManagerFactory kmFact)
	throws NoSuchAlgorithmException {
		KeyManager[] keyManagers = kmFact.getKeyManagers();

		if (keyManagers == null || keyManagers.length == 0) {
			throw new NoSuchAlgorithmException("The default algorithm :" + algorithm + " produced no key managers");
		}

		X509KeyManager x509KeyManager = null;

		for (int i = 0; i < keyManagers.length; i++) {
			if (keyManagers[i] instanceof X509KeyManager) {
				x509KeyManager = (X509KeyManager) keyManagers[i];
				break;
			}
		}

		if (x509KeyManager == null) {
			throw new NoSuchAlgorithmException("The default algorithm :"+ algorithm + " did not produce a X509 Key manager");
		}
		return x509KeyManager;
	}

}
