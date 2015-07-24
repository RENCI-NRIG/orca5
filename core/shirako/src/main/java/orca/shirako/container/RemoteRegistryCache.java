/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/

package orca.shirako.container;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import orca.manage.IOrcaActor;
import orca.manage.IOrcaClientActor;
import orca.manage.IOrcaContainer;
import orca.manage.IOrcaServerActor;
import orca.manage.Orca;
import orca.manage.OrcaConstants;
import orca.manage.beans.ActorMng;
import orca.manage.beans.ClientMng;
import orca.manage.beans.ProxyMng;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorIdentity;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.common.ConfigurationException;
import orca.shirako.container.api.IOrcaConfiguration;
import orca.shirako.core.ActorIdentity;
import orca.shirako.proxies.ActorLocation;
import orca.util.Base64;
import orca.util.ID;
import orca.util.ssl.ContextualSSLProtocolSocketFactory;
import orca.util.ssl.MultiKeyManager;
import orca.util.ssl.MultiKeySSLContextFactory;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

/**
 * This class implements a cache of the XMLRPC actor registry
 * @author ibaldin
 *
 */
public class RemoteRegistryCache extends TimerTask {
	private static final int REFRESH_PERIOD = 60000;
	private static final RemoteRegistryCache instance = new RemoteRegistryCache();
	
	private final Timer timer = new Timer("RemoteRegistryCache", true);
	private final Logger logger = Globals.Log; 
	private final String registryQueryMethod="registryService.getActorsVerifiedOtherThan";
	private static String registryUrl = null;
	private Map<String, Map<String, String>> cache = new HashMap<String, Map<String, String>>();
	//private Map<String, Set<String>> knownEdges = new HashMap<String, Set<String>>();
	private Set<String> localActorGuids = new HashSet<String>();
	
    /* 
     * common actor properties to put into maps (to get from e.g. XMLRPC registry, matched registry definitions
     * in DatabaseOperations)
     */
    public static final String ActorName="NAME";
    public static final String ActorGuid="GUID";
    public static final String ActorLocation="LOCATION";
    public static final String ActorProtocol="PROTOCOL";
    public static final String ActorCert64="CERT";
    public static final String ActorClazz="CLASS";
    public static final String ActorPubkey="PUBKEY";
    public static final String ActorType="TYPE";
    public static final String ActorMapperclass="MAPPERCLASS";    
    public static final String ActorAllocunits = "ALLOCUNITS";
	public static final String ActorFullRDF = "FULLRDF";
	public static final String ActorAbstractRDF = "ABSRDF";
	
	private static boolean configuredSSL = false;
	private static boolean sslError = false;
	private static boolean threadStarted = false;
	private static byte[] registryCertDigest = new byte[16];
	private static MultiKeyManager mkm = null;
	
	private RemoteRegistryCache() {

	}
	
	public static RemoteRegistryCache getInstance() {
		return instance;
	}

	public static MultiKeyManager getMultiKeyManager() {
		return mkm;
	}
	
	public void start() {
		if (threadStarted) {
			logger.info("XMLRPC registry thread already started, ignoring");
			return;
		}
		logger.info("Starting periodic XMLRPC registry caching thread");
		if (!configuredSSL) {
			logger.error("SSL is not configured for XMLRPC registry, thread will not start");
		}
		if (sslError) {
			logger.error("SSL error encountered, thread will not start");
			return;
		}
		if (registryUrl != null)
			timer.scheduleAtFixedRate(this, 0, REFRESH_PERIOD);
		threadStarted = true;
	}
	
	public void stop() {
		logger.info("Stopping XMLRPC registry caching thread");
		timer.cancel();
	}
	
	/**
	 * provide a list of guids known to the cache
	 * @return
	 */
	public List<String> knownGuids() {
		synchronized(cache) {
			return new ArrayList<String>(cache.keySet());
		}
	}
	
	/**
	 * manually add a local entry to cache
	 * @param guid
	 * @param entry
	 */
	public void addLocalCacheEntry(String guid, Map<String, String> entry) {
		if ((guid == null) || (entry == null)) 
			return;
		synchronized(cache) {
			nonMtCacheMerge(guid.trim(), entry);
		}
		synchronized(localActorGuids) {
			localActorGuids.add(guid);
		}
	}
	
	/**
	 * If the entry does not have location and cert, remove it -
	 * we will have to ask the registry again
	 * @param guid
	 */
	public void checkToRemoveEntry(String guid) {
		if (guid == null)
			return;
		synchronized (cache) {
			Map<String, String> en = cache.get(guid);
			if (en != null) {
				if ((en.get(ActorLocation) == null) || (en.get(ActorCert64) == null))
					cache.remove(guid);
			}
		}
	}
	
	/**
	 * Add a remote entry manually (e.g. based on vertex info)
	 * @param guid
	 * @param entry
	 */
	public void addPartialCacheEntry(String guid, Map<String, String> entry) {
		if ((guid == null) || (entry == null)) 
			return;
		synchronized(cache) {
			nonMtCacheMerge(guid.trim(), entry);
		}
	}
	
	/** 
	 * Remove entry of an actor
	 * @param guid
	 */
	public void removeCacheEntry(String guid) {
		synchronized(cache) {
			cache.remove(guid.trim());
		}
	}
	
	/**
	 * return an entry if it exists (safe copy)
	 * @param guid
	 * @return
	 */
	public Map<String, String> getCacheEntryCopy(String guid) {
		Map<String, String> ret = null;
		synchronized(cache) {
			ret = cache.get(guid.trim());
			if (ret != null)
				return new HashMap<String, String>(ret);
		}
		return ret;
	}
	
	/**
	 * Perform a single query of the XMLRPC registry
	 */
	@SuppressWarnings("unchecked")
	public List<String> singleQuery() {
		
		if (registryUrl == null) {
			logger.info("No registry URL specified, skipping query");
			return null;
		}
		logger.info("Contacting external registry at " + registryUrl);

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(registryUrl));
		} catch (MalformedURLException e) {
			logger.error("Invalid registry URL " + registryUrl + ", caching thread terminating");
			return null;
		}	
		
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		
        // set this transport factory for host-specific SSLContexts to work
        XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
		client.setTransportFactory(f);
		
		// set null identity - queries are not checked at the registry
		mkm.setCurrentGuid(null);
		
		Vector<Object> params = new Vector<Object>();
		// add a list of guids we already know		
		params.addElement(knownGuids());
		// we only want essential info on the actors (name, guid, location, cert)
		params.addElement(true);
		
		try {
			Map<String, Map<String, String>> mapResult = (Map<String, Map<String, String>>) client.execute(registryQueryMethod, params);
			// merge the results
			synchronized(cache) {
				// ignore the status entry if present
				mapResult.remove("STATUS");
				for(Map.Entry<String, Map<String, String>> entry: mapResult.entrySet()) {
					logger.info("Merging entry for actor " + entry.getKey());
					if (localActorGuids.contains(entry.getKey()))
						logger.info("Registry returned local actor from query: " + entry.getKey() + ", skipping");
					else
						nonMtCacheMerge(entry.getKey(), entry.getValue());
				}
			}
			return new ArrayList(mapResult.keySet()); // return list of new guids
		} catch (XmlRpcException e) {
			logger.error("Error querying XMLRPC registry, continuing");
			return null;
		}
	}

	private boolean checkEdge(IOrcaActor fromActor, ID fromGuid, IOrcaActor toActor, ID toGuid) throws Exception {

		try {
			// if to actor is local
			if (toActor != null) {
				// get the client matching guid
				if (((IOrcaServerActor)toActor).getClient(fromGuid) != null) {
					logger.debug("Edge between " + fromGuid + " and " + toGuid + " exists (client)");
					return true;
				}
			} else if (fromActor != null) {
				if (((IOrcaClientActor)fromActor).getBroker(toGuid) != null) {
					logger.debug("Edge between " + fromGuid + " and " + toGuid + " exists (broker)");
					return true;
				}
			}
		} catch (ClassCastException e) {
			throw new Exception("checkEdge(): Unable to cast actor " + fromGuid + " or " + toGuid);
		}
		logger.debug("Edge between " + fromGuid + " and " + toGuid + " does not exist");
		return false;
	}

	/**
	 * Establish and edge between two actors (local-local, local-remote); skip if one exists already
	 * @param from
	 * @param fromGuid
	 * @param to
	 * @param toGuid
	 * @return
	 * @throws Exception
	 */
	public ClientMng establishEdge(ID fromGuid, ID toGuid)  throws Exception {
        if ((fromGuid == null) || (toGuid == null) ) 
        	logger.error("establishEdgePrivate(): Cannot establish edge when either guid is not known");
        
		try {
			IOrcaContainer cont = Orca.connect();
			IOrcaActor fromActor = cont.getActor(fromGuid);
			IOrcaActor toActor = cont.getActor(toGuid);
						
			if (!checkEdge(fromActor, fromGuid, toActor, toGuid)) {
				//addEdges(fromGuid, toGuid); // if it fails, let's not try it again
				ClientMng cl = establishEdgePrivate(fromActor, fromGuid, toActor, toGuid);
				checkToRemoveEntry(fromGuid.toString());
				checkToRemoveEntry(toGuid.toString());
				return cl;
			}
		} catch (Exception e) {
			logger.error("Error establishing edge from " + fromGuid + " to " + toGuid + " : " + e.toString());
		}
		return null;
	}
	
	/*
     * Establish an edge between two actors. From and To are assumed to be well-configured and
     * conforming to the assumption that it is either broker->site or service->broker edge
     */
    private ClientMng establishEdgePrivate(IOrcaActor fromActor, ID fromGuid, IOrcaActor toActor, ID toGuid) throws Exception {
        ClientMng client = null;

        Map<String, String> fromMap = RemoteRegistryCache.getInstance().getCacheEntryCopy(fromGuid.toString());
        Map<String, String> toMap = RemoteRegistryCache.getInstance().getCacheEntryCopy(toGuid.toString());
        
        if (fromMap == null) {
        	throw new ConfigurationException("establishEdgePrivate(): Actor " + fromActor.getName() + " does not have a registry cache entry");
        }
        if (toMap == null) {
        	throw new ConfigurationException("establishEdgePrivate(): Actor " + toActor.getName() + " does not have a registry cache entry");
        }
        
        /*
         * Register the proxies and record exports.
         */
        if (fromActor != null) {
        	logger.debug("From actor " + fromActor.getName() + " is local");
        	// if from is a local actor (it is a broker or SM)
        	//
            String protocol = OrcaConstants.ProtocolLocal;
            ActorLocation location = new ActorLocation();

            // fill in location of other side from topology edge information
            // NOTE: prefers the protocol used by the topology edge, even if both actors are in the same container.
            if ((toMap.get(ActorLocation) != null)) {
                protocol = toMap.get(ActorProtocol);
                if (protocol == null) {
                	throw new ConfigurationException("establishEdgePrivate(): Actor " + toMap.get(ActorName) + " does not specify communications protocol (local/soapaxis2/xmlrpc)"); 
                }
                location.setLocation(toMap.get(ActorLocation));
                logger.debug("Added To actor location (non-local) " + toMap.get(ActorLocation));
            }

            IActorIdentity identity = new ActorIdentity(toMap.get(ActorName), toGuid);

            // add the remote broker's certificate to local actor's keystore (from is local)
            Certificate brokerCert = null;
            if (toActor != null) {
                logger.debug("Getting local toActor certificate");
            	// either broker is local
                brokerCert = toActor.getCertificate();
                if (brokerCert == null) {
                	throw new ConfigurationException("establishEdgePrivate(): Unable to find certificate for local actor " + fromMap.get(ActorName));
                }
            } else {
            	logger.debug("Getting remote toActor certificate from cache");
            	// or it is remote, so lookup cert from config file if possible
                if (toMap.get(ActorCert64) != null) {
                	try {
                		logger.debug("Decoding remote actor cert");
                		brokerCert = OrcaContainer.decodeCertificate(Base64.decode(toMap.get(ActorCert64)));
                	} catch (Exception e) {
                		throw new ConfigurationException(
                				"establishEdgePrivate(): Could not decode certificate for actor: " + 
                				toMap.get(ActorName), e);
                	}
                }
            }

            // add cert to from actor
            if (brokerCert != null) {
            	logger.debug("Certificate is available, registering proxy and cert");

            	logger.debug("registering broker proxy");
               	ProxyMng p = new ProxyMng();
               	p.setProtocol(protocol);
               	p.setGuid(identity.getGuid().toString());
               	p.setName(identity.getName());
               	p.setType(toMap.get(ActorType));
               	p.setUrl(location.getLocation());
               	
            	if (!((IOrcaClientActor)fromActor).addBroker(p)) {
            		throw new ConfigurationException("Could not register broker", fromActor.getLastError());
            	}
            	logger.debug("Registering broker certificate");
            	
            	if (!fromActor.registerCertificate(brokerCert, toGuid.toString())) {
                	throw new ConfigurationException("Could not register the broker cert with actor: " + fromGuid, fromActor.getLastError());
                }
            } else {
            	logger.debug("Not adding broker to actor at this time because the remote actor actor certificate is not available");
            }
            
            if (toActor != null) { // brokerCert should also be non-null here
            	logger.debug("Creating a client for local to actor");
                // register the from actor as a client with the to actor if to is also local (to is site or broker, from is broker or service)
                Certificate fromCert = fromActor.getCertificate();
                client = new ClientMng();
                client.setName(fromActor.getName());
                client.setGuid(fromActor.getGuid().toString());
                try {
                    ((IOrcaServerActor) toActor).registerClient(client, fromCert);
                } catch (Exception e) {
                    throw new ConfigurationException("establishEdgePrivate(): Could not register actor: " + client.getName() + " as a client of actor: " + toActor.getName() + ": " + e.toString());
                }
            }

        } else { 
        	// fromActor is remote: toActor must be local
            // no-need to create any proxies
            // we only need to register clients
            if (toActor == null) {
                throw new ConfigurationException("establishEdgePrivate(): Both edge endpoints are non local actors: (" + fromMap.get(ActorName) + "," + 
                		toMap.get(ActorName) + ")");
            }

            if (fromMap.get(ActorGuid) == null) {
                throw new ConfigurationException("establishEdgePrivate(): Missing guid for remote actor: " + fromMap.get(ActorName));
            }
            // register a client if cert is available 
            logger.debug("From actor was remote, to actor " + toActor.getName() + " is local");
            if (fromMap.get(ActorCert64) != null) {
            	logger.debug("from actor has certificate from cache");
            	Certificate fromCert = null;
            	try {
            		fromCert = OrcaContainer.decodeCertificate(Base64.decode(fromMap.get(ActorCert64)));
            	} catch (Exception e) {
            		throw new ConfigurationException("establishEdgePrivate(): Could not decode certificate for remote actor: " + fromMap.get(ActorName));
            	}
            	logger.debug("Creating client for from actor " + fromMap.get(ActorName));
            	client = new ClientMng();
            	client.setName(fromMap.get(ActorName));
            	client.setGuid(fromMap.get(ActorGuid));
            	try {
            		((IOrcaServerActor) toActor).registerClient(client, fromCert);
            	} catch (Exception e) {
            		throw new ConfigurationException("establishEdgePrivate(): Could not register actor: " + client.getName() + " as a client of actor: " + toActor.getName());
            	}
            } else
            	logger.debug("Not adding client to actor at this time - remote actor certificate not available");
        }
        return client;
    }
    
    /**
     * process last query using internally stored list of known guids
     */
    public void singleQueryProcess() {
    	singleQueryProcess(knownGuids());
    }
	
	/**
	 * Perform a single query on the remote registry and process the results (i.e. add new edges)
	 */
	public synchronized void singleQueryProcess(List<String> newGuids) {
		logger.debug("Processing XMLRPC registry response");
		if (newGuids == null)
			return;
		try {
			IOrcaContainer cont = Orca.connect();
			for (String remoteGuid: newGuids) {
				// these should be non-local but worth checking
				Map<String, String> cacheEntry = getCacheEntryCopy(remoteGuid);
				if (!cacheEntry.containsKey(ActorName) || !cacheEntry.containsKey(ActorGuid) ||
						!cacheEntry.containsKey(ActorType)) {
					logger.error("Invalid cache entry, ignoring");
					continue;
				}
				IOrcaActor actor = cont.getActor(new ID(remoteGuid));
				if (actor != null) {
					logger.warn("singleQueryProcess(): Registry returned a known local actor, skipping");
					continue;
				}

				// new remote SERVICE
				if (cacheEntry.get(ActorType).equalsIgnoreCase(OrcaConstants.SM) || 
						cacheEntry.get(ActorType).equalsIgnoreCase(OrcaConstants.SERVICE)) {
					logger.debug("New remote SM reported by registry: " + remoteGuid);
					List<ActorMng> brokers = cont.getBrokers();
					for (ActorMng a : brokers) {
						logger.info("Adding a new edge between remote SM " + remoteGuid + " and local Broker " + a.getID());
						establishEdge(new ID(remoteGuid), new ID(a.getID()));
					}
				}
				// new remote SITE
				if (cacheEntry.get(ActorType).equalsIgnoreCase(OrcaConstants.SITE)) {
					logger.debug("New remote site reported by registry: " + remoteGuid);
					List<ActorMng> brokers = cont.getBrokers();
					for (ActorMng a : brokers) {
						establishEdge(new ID(a.getID()), new ID(remoteGuid));
					}	
				}

				// new remote BROKER
				if (cacheEntry.get(ActorType).equalsIgnoreCase(OrcaConstants.BROKER)) {
					logger.debug("New remote broker reported by registry: " + remoteGuid);
					List<ActorMng> actors = cont.getAuthorities();
					// if there are local SITEs create edges TO them
					for (ActorMng a : actors) {
						establishEdge(new ID(remoteGuid), new ID(a.getID()));
					}
					// if there are local SMs create edges FROM them
					actors = cont.getServiceManagers();
					for (ActorMng a : actors) {
						establishEdge(new ID(a.getID()), new ID(remoteGuid));
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception encountered while processing output from XMLRPC remote registry: " + e.toString());
		}
	}
	
	/**
	 * Add new entry to cache by merging of values
	 */
	protected void nonMtCacheMerge(String guid, Map<String, String> val) {
		Map<String, String> cur = cache.get(guid);

		if (cur == null) {
			logger.debug("Inserting new entry for " + guid);
			cache.put(guid, val);
			return;
		}
		//logger.debug("Merging " + val.toString() + " with existing entry " + cur.toString() + " for " + guid);
		// merge two maps, old values take precedence over new ones
		for(Map.Entry<String, String> e: val.entrySet()) {
			if (!cur.containsKey(e.getKey()))
				cur.put(e.getKey(), e.getValue());
		}
	}
	
	@Override
	/**
	 * Perform a single query and process a result on a timeout
	 */
	public void run() {
		singleQueryProcess(singleQuery());
	}
		

	/**
	 * set up client-side SSL parameters
	 */
    public static void configureSSL() {
    	
    	if (configuredSSL) {
    		return;
    	}
    	
    	configuredSSL = true;
    	
        registryUrl = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl);
        if (registryUrl == null ) {
            Globals.Log.info("No external registry is specified.");
            return;
        }

        URL registryURL = null;
        try {
        	registryURL = new URL(registryUrl);
        } catch (MalformedURLException e) {
        	Globals.Log.info("Unable to parse registry URL: " + registryUrl);
        	return;
        }
        
    	// load registry cert fingerprint
    	Globals.Log.debug("Loading registry certificate fingerprint");
    	String registryCertFingerprint = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCertFingerprint);
    	if (registryCertFingerprint == null) {
    		Globals.Log.info("Registry certificate fingerprint property (" + OrcaContainer.PropertyRegistryCertFingerprint + ") is not specified, skipping registry SSL configuration");
    		return;
    	}
    	
    	// convert to byte array
    	String[] fingerPrintBytes = registryCertFingerprint.split(":");
 
    	for (int i = 0; i < 16; i++ )
    		registryCertDigest[i] = (byte)(Integer.parseInt(fingerPrintBytes[i], 16) & 0xFF);

    	// Create a trust manager that does not validate certificate chains
    	TrustManager[] trustRegistryCert = new TrustManager[] {
    			new X509TrustManager() {
    				public X509Certificate[] getAcceptedIssuers() {
    					// return 0 size array, not null, per spec
    					return new X509Certificate[0];
    				}

    				public void checkClientTrusted(X509Certificate[] certs, String authType) {
    					// Trust always
    				}

    				public void checkServerTrusted(X509Certificate[] certs, String authType) {
    					// Trust always
    					MessageDigest md = null;
    					try {
    						md = MessageDigest.getInstance("MD5");

    						if (certs.length == 0) 
    							throw new CertificateException();

    						byte[] certDigest = md.digest(certs[0].getEncoded());
    						if (!Arrays.equals(certDigest, registryCertDigest)) {
    							Globals.Log.error("Certificate presented by registry does not match local copy, communications with registry is not possible");
    							sslError = true;
    							if (threadStarted) 
    								RemoteRegistryCache.getInstance().stop();
    							ActorLiveness.allStop();
    							throw new CertificateException();
    						}
    					} catch (NoSuchAlgorithmException e) {

    					} catch (Exception e) {
    						Globals.Log.error("Unable to compare server certificate digest to the existing registry digest: " + e.toString());
    						sslError = true;
							if (threadStarted) 
								RemoteRegistryCache.getInstance().stop();
							ActorLiveness.allStop();
    					}
    				}

    			}
    	};
    	
    	Globals.Log.info("Creating a multikey manager for registry communications");
    	// create multikeymanager
    	mkm = new MultiKeyManager();
    	
    	// register a new protocol
    	ContextualSSLProtocolSocketFactory regSslFact = 
    		new ContextualSSLProtocolSocketFactory();

    	// add this multikey context factory for the registry host/port
    	regSslFact.addHostContextFactory(new MultiKeySSLContextFactory(mkm, trustRegistryCert), 
    			registryURL.getHost(), registryURL.getPort());

    	// register the protocol (Note: All xmlrpc clients must use XmlRpcCommonsTransportFactory
    	// for this to work). See ContextualSSLProtocolSocketFactory.
		Protocol reghhttps = new Protocol("https", (ProtocolSocketFactory)regSslFact, 443); 
		Protocol.registerProtocol("https", reghhttps);
    }

    /**
     * Register an actor with the registry
     * @param actor
     */
    public static void registerWithRegistry(IActor actor) {
        // External actor registry operations
        // If actor needs to be registered at external registry (i.e
        // PropertyRegistryUrl is set to the url for the registry),
        // register actor with external registry

    	// NOTE: we register with local cache no matter what (if registry is specified or not)
        try {
            String act_name = actor.getName();
            String act_type = (new Integer(actor.getType())).toString();
            String act_guid = actor.getGuid().toString();
            String act_desc = actor.getDescription();

            String act_soapaxis2url = Globals.getContainer().getConfiguration().getProperty(IOrcaConfiguration.PropertySoapAxis2Url);
            if (act_soapaxis2url != null) {
                act_soapaxis2url += "/services/" + actor.getName();
            } else {
                act_soapaxis2url = "None";
            }

            String act_class = actor.getClass().getCanonicalName();
            String act_mapper_class = actor.getPolicy().getClass().getCanonicalName();
            
            String act_pubkey = actor.getShirakoPlugin().getKeyStore().getActorCertificate().getPublicKey().toString();
            if (act_pubkey == null) {
                act_pubkey = "None";
            }

            // get actor cert
            Certificate cert = actor.getShirakoPlugin().getKeyStore().getActorCertificate();
            byte[] bytes = null;

            try {
                bytes = cert.getEncoded();
            }catch (CertificateEncodingException e) {
                throw new RuntimeException("Failed to encode the certificate");
            }

            String base64 = Base64.encodeBytes(bytes);
            //Globals.Log.info("Actor certificate in BASE64: \n" + base64);
            String act_cert64 = base64;
            
            // register with local cache regardless of whether external registry is used
        	HashMap<String, String> res = new HashMap<String, String>();
            res.put(RemoteRegistryCache.ActorName, act_name);
            res.put(RemoteRegistryCache.ActorGuid, act_guid);
            String actor_type = null;

            if(act_type.equalsIgnoreCase("1")){
                actor_type = OrcaConstants.SM;
            }
            if(act_type.equalsIgnoreCase("2")){
                actor_type = OrcaConstants.BROKER;
            }
            if(act_type.equalsIgnoreCase("3")){
                actor_type = OrcaConstants.SITE;
            }
        	res.put(RemoteRegistryCache.ActorType, actor_type);
        	res.put(RemoteRegistryCache.ActorLocation, act_soapaxis2url);
        	res.put(RemoteRegistryCache.ActorProtocol, OrcaConstants.ProtocolSoapAxis2);
        	res.put(RemoteRegistryCache.ActorCert64, act_cert64);
        	RemoteRegistryCache.getInstance().addLocalCacheEntry(act_guid, res);
            
        	// now try registering with remote
            String registryUrl = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl);
            String registryMethod = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryMethod);
            if (registryUrl == null || registryMethod == null) {
                Globals.Log.info("No external registry is specified.");
                return;
            }
            
            Globals.Log.info("Registering actor with external registry at " + registryUrl + " using " + registryMethod);
            
            Vector<String> params = new Vector<String>();
            // This order of insert is very important, because the xml-rpc
            // server expects the strings in this order
            params.addElement(act_name);
            params.addElement(act_type);
            params.addElement(act_guid);
            params.addElement(act_desc);
            params.addElement(act_soapaxis2url);
            params.addElement(act_class);
            params.addElement(act_mapper_class);
            params.addElement(act_pubkey);
            params.addElement(act_cert64);
            try {
                Globals.Log.debug("Inserting into registry - Actor:" + act_name + " | Type:" + act_type + " | GUID: " + 
                		act_guid + " | Description: " + act_desc + " | soapaxis2url: " + act_soapaxis2url + " | ActorClass: " + 
                		act_class + " | ActorMapperClass: " + act_mapper_class);
                
                // add actor key and cert into the multikeymanager
                mkm.addPrivateKey(act_guid,
                		actor.getShirakoPlugin().getKeyStore().getActorPrivateKey(), 
                		actor.getShirakoPlugin().getKeyStore().getActorCertificate());
                		
                // before we do SSL to registry, set our identity
                mkm.setCurrentGuid(act_guid);
                
                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL(registryUrl + "/"));
                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);
                
                // set this transport factory for host-specific SSLContexts to work
                XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
    			client.setTransportFactory(f);
    			
                String stat = (String)client.execute(registryMethod, params);
                Globals.Log.info("Registry returned: " + stat);                
            } catch (Exception ex) {
                Globals.Log.error("Could not connect to the registry server " + registryUrl + ": ", ex);
            }
            // start update thread
            ActorLiveness checkActorLive = new ActorLiveness(actor.getGuid().toString(), registryUrl, registryMethod);
        } catch (Exception e) {
            Globals.Log.error("An error occurred whle attempting to register actor " + actor.getName() + " with external registry", e);
        }
    }
    
    /*
     * Register full and abstract rdfs to the registry
     * fullModel is the String representing the full rdf for the domain
     * abstractModel is the String representing the abstract rdf for the domain
     */

    public static void registerNDLToRegistry(IAuthorityProxy proxy, String fullModel, String abstractModel){

    	String registryUrl = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl);
    	String registryMethod = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryMethod);

    	if (registryUrl == null || registryMethod == null) {
    		Globals.Log.info("No external registry specified for registering NDL.");
    		return;
    	}

    	//Globals.Log.debug("registryUrl = " + registryUrl + " ; registryMethod = " + registryMethod);


    	try {
    		String act_guid = proxy.getGuid().toString();
    		String act_abstract_rdf = abstractModel;
    		//Globals.Log.debug(abstractModel);
    		String act_full_rdf = fullModel;
    		//Globals.Log.debug(fullModel);

    		Globals.Log.info("Registering ndls with external registry at " + registryUrl + " using " + registryMethod + " for actor " + act_guid);

    		Vector<String> params = new Vector<String>();
    		// This order of insert is very important, because the xml-rpc
    		// server expects the strings in this order
    		params.addElement(act_guid);
    		params.addElement(act_abstract_rdf);
    		params.addElement(act_full_rdf);

    		try {
    			//Globals.Log.debug("Inserting into registry - ActorGUID: " + act_guid + " | AbstractRDF: " + act_abstract_rdf + " | FullRDF: " + act_full_rdf);
    			Globals.Log.debug("Inserting Abstract and Full NDL into registry for actor with ActorGUID: " + act_guid);

    			// set current identity for SSL connection to registry
    			mkm.setCurrentGuid(act_guid);
    			
    			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    			config.setServerURL(new URL(registryUrl + "/"));
    			XmlRpcClient client = new XmlRpcClient();
    			client.setConfig(config);
    			
                // set this transport factory for host-specific SSLContexts to work
                XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
    			client.setTransportFactory(f);
    			
    			String stat = (String) client.execute(registryMethod, params);
    			Globals.Log.info("Registry returned: " + stat); 
    		} catch (Exception ex) {
    			Globals.Log.error("Could not connect to the registry server", ex);
    		}
    	} catch (Exception e) {
    		Globals.Log.error("An error occurred whle attempting to register Ndls with external registry", e);
    	}
    }
}
