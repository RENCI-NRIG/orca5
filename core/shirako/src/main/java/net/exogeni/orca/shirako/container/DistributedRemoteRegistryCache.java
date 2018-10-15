/*
 * Copyright (c) 2011 RENCI/UNC Chapel Hill 
 *
 * @author Ilia Baldine
 * @author Claris Castillo 
 * 
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

package net.exogeni.orca.shirako.container;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
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

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import net.exogeni.orca.ektorp.actor;
import net.exogeni.orca.ektorp.client.OrcaStdHttpClient;
import net.exogeni.orca.ektorp.repository.ActorRepository;
import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.IOrcaClientActor;
import net.exogeni.orca.manage.IOrcaContainer;
import net.exogeni.orca.manage.IOrcaServerActor;
import net.exogeni.orca.manage.Orca;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.beans.ActorMng;
import net.exogeni.orca.manage.beans.ClientMng;
import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IActorIdentity;
import net.exogeni.orca.shirako.api.IAuthorityProxy;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.container.api.IOrcaConfiguration;
import net.exogeni.orca.shirako.core.ActorIdentity;
import net.exogeni.orca.shirako.proxies.ActorLocation;
import net.exogeni.orca.shirako.util.SSLRestHttpClient;
import net.exogeni.orca.util.Base64;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ssl.MultiKeyManager;

import org.apache.log4j.Logger;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This class implements a cache of the XMLRPC actor registry
 * @author ibaldin
 *
 */
public class DistributedRemoteRegistryCache extends TimerTask {
	private static final int REFRESH_PERIOD = 60000;
	private static final DistributedRemoteRegistryCache instance = new DistributedRemoteRegistryCache();
	private static final String SOAPAXIS2_PROTOCOL = "soapaxis2";
	private final Timer timer = new Timer("DistributedRemoteRegistryCache", true);
	private final Logger logger = Globals.Log; 
	private static String registryUrl = null;
	private static String registryUrl_1=null;
	private static String registryUrl_2=null;
	private static String registryUsername=null;
	private static String registryPassword=null;
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
	private static TrustManager tm = null;
	

	private DistributedRemoteRegistryCache() {

	}

	public static DistributedRemoteRegistryCache getInstance() {
		return instance;
	}

	public static MultiKeyManager getMultiKeyManager() {
		return mkm;
	}
	
	public static TrustManager getTrustManager() {
		return tm;
	}

	public void start() {
		
	
		if (threadStarted) {
			logger.info("Registry thread already started, ignoring");
			return;
		}
		logger.info("Starting periodic  registry caching thread");
		if (!configuredSSL) {
			logger.error("SSL is not configured for  registry, thread will not start");
		}
		if (sslError) {
			logger.error("SSL error encountered, thread will not start");
			return;
		}
		if (registryUrl_1 != null)
			timer.scheduleAtFixedRate(this, 0, REFRESH_PERIOD);
		threadStarted = true;
	}

	public void stop() {
		logger.info("Stopping  registry caching thread");
		timer.cancel();
	}

	/**
	 * provide a list of guids known to the cache
	 * @return list of guids known to the cache
	 */
	public List<String> knownGuids() {
		synchronized(cache) {
			return new ArrayList<String>(cache.keySet());
		}
	}

	/**
	 * manually add a local entry to cache
	 * @param guid guid
	 * @param entry entry
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
	 * @param guid guid
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
	 * @param guid guid
	 * @param entry entry
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
	 * @param guid guid
	 */
	public void removeCacheEntry(String guid) {
		synchronized(cache) {
			cache.remove(guid.trim());
		}
	}

	/**
	 * return an entry if it exists (safe copy)
	 * @param guid guid
	 * @return an entry if it exists (safe copy)
	 */
	public Map<String, String> getCacheEntryCopy(String guid) {
		Map<String, String> ret = null;
		synchronized(cache) {
			ret = cache.get(guid.trim());
			if (ret != null)
			{
				return new HashMap<String, String>(ret);
			}
		}
		return ret;
	}

	/**
	 * Perform a single query of the XMLRPC registry
     * @return list of result
	 */
	
	public List<String> singleQuery() {
		Map<String,Map<String,String>> results = new HashMap<String, Map<String, String>>();
		
		
			mkm.setCurrentGuid(null); 
			logger.info("singleQuery: Contacting external couchdb registry at " + registryUrl_1);

			String selectedActor = getWorkingActorRegistry(registryUrl_1);
			HttpClient sslClient = null;
			try {
				sslClient = new OrcaStdHttpClient.Builder()
				.url(selectedActor)
				.username(registryUsername)
				.password(registryPassword)
				.enableSSL(true)
				.keyManager(mkm)
				.trustManager(tm)
				.socketTimeout(60000)
				.relaxedSSLSettings(true)
				.port(6984)
				.build();
			} catch (MalformedURLException e2) {
				logger.error("MalformedURLException when building OrcaStdHttpClient.");
				e2.printStackTrace();
			} catch (Exception e) {
				logger.error("There has been a problem building the OrcaStdHttpClient. See stack trace.");
				e.printStackTrace();
			}

		
		try {
		//now lets connect to the database with the sslClient
			CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
			CouchDbConnector db = dbInstance.createConnector("actor", true);
		
			//queryVerified. Lets query the view for actors that have been verified.
			ViewQuery verifiedQuery = new ViewQuery()
			.designDocId("_design/actor")
			.viewName("verifiedOnly");

			List<actor> verifiedList = db.queryView(verifiedQuery,actor.class);
			for (actor actorEntry : verifiedList) {
				Map <String,String> subMap = new HashMap<String,String>();
				subMap.put(ActorLocation, actorEntry.getSoapURL());
				subMap.put(ActorType, actorEntry.getType());
				subMap.put(ActorCert64, actorEntry.getCert());
				subMap.put(ActorName, actorEntry.getName());
				subMap.put(ActorProtocol, SOAPAXIS2_PROTOCOL);
				String act_type = actorEntry.getType();
			
				if(act_type.compareTo("1")==0) {
					act_type = OrcaConstants.SM;
				} else if (act_type.compareTo("2")==0) {
					act_type = OrcaConstants.BROKER;				
				} else if(act_type.compareTo("3")==0) {
					act_type = OrcaConstants.SITE;
				} 
				
				
				subMap.put(ActorType, act_type);
				subMap.put(ActorGuid, actorEntry.getId().trim());
				results.put(actorEntry.getId().trim(), subMap);
				logger.info("SingleQuery:\t"+actorEntry.getId()+"\t"+actorEntry.getType()+"\t"+actorEntry.getCert());
			}

			sslClient.shutdown();
		
		} catch (Exception e ) {
			sslClient.shutdown();
			logger.error("SingleQuery failed to execute likely because of a time out error. See stack trace below. It will retry in a minute.");
			e.printStackTrace();
		}

			// merge the results
			Map<String, Map<String,String>> mapResult = results;
			synchronized(cache) {
				for(Map.Entry<String, Map<String, String>> entry: mapResult.entrySet()) {
					logger.info("Merging entry for actor " + entry.getKey());
					if (localActorGuids.contains(entry.getKey()))
					{
						logger.info("Registry returned local actor from query: " + entry.getKey() + ", skipping");
					}
					else
					{
						nonMtCacheMerge(entry.getKey(), entry.getValue());
					}
				}
			}

			return new ArrayList<String>(mapResult.keySet()); // return list of new guids
		


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
	 * @param fromGuid fromGuid
	 * @param toGuid toGuid
	 * @return returns ClientMng
	 * @throws Exception in case of error
	 */
	public ClientMng establishEdge(ID fromGuid, ID toGuid)  throws Exception {
		if ((fromGuid == null) || (toGuid == null) ) 
			logger.error("establishEdgePrivate(): Cannot establish edge when either guid is not known");

		try {
			IOrcaContainer cont = Orca.connect();
			IOrcaActor fromActor = cont.getActor(fromGuid);
			IOrcaActor toActor = cont.getActor(toGuid);

		
			
			if (!checkEdge(fromActor, fromGuid, toActor, toGuid)) {
				
		
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
     * @param fromActor
     * @param fromGuid
     * @param toActor
     * @param toGuid
     * @return returns ClientMng
     * @throws Exception in case of error
	 */
	private ClientMng establishEdgePrivate(IOrcaActor fromActor, ID fromGuid, IOrcaActor toActor, ID toGuid) throws Exception {
		ClientMng client = null;
		
		DistributedRemoteRegistryCache instance = DistributedRemoteRegistryCache.getInstance();
		
		Map<String, String> fromMap = instance.getCacheEntryCopy(fromGuid.toString());
		Map<String, String> toMap = instance.getCacheEntryCopy(toGuid.toString());
		    
		if (fromMap == null) {
			throw new ConfigurationException("establishEdgePrivate(): Actor " + fromGuid.toString() + " does not have a registry cache entry");
		}
		if (toMap == null) {
			throw new ConfigurationException("establishEdgePrivate(): Actor " + toGuid.toString() + " does not have a registry cache entry");
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
     * @param newGuids newGuids
	 */
	public synchronized void singleQueryProcess(List<String> newGuids) {
		logger.debug("Processing  registry response");
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
     * @param guid guid
     * @param val val
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

		//    registryUrl = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl);
		registryUrl_1 = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_1).trim();
		registryUrl_2 = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_2).trim();
		registryUsername = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCouchDBUsername).trim();
		registryPassword = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCouchDBPassword).trim();

		if (registryUrl_1 == null) {
			Globals.Log.info("No external registry is specified.");
			return;
		}

		

		URL registryURL = null;
	

		
		try {
			registryURL = new URL(registryUrl_1);
		} catch (MalformedURLException e) {
			Globals.Log.info("Unable to parse couchdb registry URL: " + registryUrl_1);
			return;
		}
		
		if(registryUrl_2 !=null)  {
			try {
				registryURL = new URL(registryUrl_2);
			} catch (MalformedURLException e) {
				Globals.Log.info("Unable to parse couchdb registry URL: " + registryUrl_2);
				return;
			}
		}
		// load registry cert fingerprint
		Globals.Log.debug("Loading registry certificate fingerprint");
		String registryCertFingerprint = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCertFingerprint_1).trim();
		if (registryCertFingerprint == null) {
			Globals.Log.info("Registry certificate fingerprint property (" + OrcaContainer.PropertyRegistryCertFingerprint_1 + ") is not specified, skipping registry SSL configuration");
			return;
		}

		// convert to byte array
		String[] fingerPrintBytes = registryCertFingerprint.split(":");

		for (int i = 0; i < 16; i++ )
		{
			registryCertDigest[i] = (byte)(Integer.parseInt(fingerPrintBytes[i], 16) & 0xFF);
		}

		Globals.Log.info("Creating a trust manager for registry communications");
		tm = new X509TrustManager() {
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
							DistributedRemoteRegistryCache.getInstance().stop();
						ActorLiveness.allStop();
						throw new CertificateException();
					}
				} catch (NoSuchAlgorithmException e) {

				} catch (Exception e) {
					Globals.Log.error("Unable to compare server certificate digest to the existing registry digest: " + e.toString());
					sslError = true;
					if (threadStarted) 
						DistributedRemoteRegistryCache.getInstance().stop();
					ActorLiveness.allStop();
				}
			}

		};
		Globals.Log.info("Creating a multikey manager for registry communications");
		// create multikeymanager
		mkm = new MultiKeyManager();
	
	}

	/**
	 * Register an actor with the registry
	 * @param actor actor
	 */
	public static void registerWithRegistry(IActor actor) {
		// External actor registry operations
		// If actor needs to be registered at external registry (i.e
		// PropertyRegistryUrl is set to the url for the registry),
		// register actor with external registry
		String act_name = actor.getName();
		String act_type = (new Integer(actor.getType())).toString();
		String act_guid = actor.getGuid().toString();
		String act_desc = actor.getDescription();

		String act_soapaxis2url = Globals.getContainer().getConfiguration().getProperty(IOrcaConfiguration.PropertySoapAxis2Url).trim();
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
	
		String act_cert64 = base64;



		// NOTE: we register with local cache no matter what (if registry is specified or not)
		try {

			// register with local cache regardless of whether external registry is used
			HashMap<String, String> res = new HashMap<String, String>();
			res.put(DistributedRemoteRegistryCache.ActorName, act_name);
			res.put(DistributedRemoteRegistryCache.ActorGuid, act_guid);
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
			res.put(DistributedRemoteRegistryCache.ActorType, actor_type);
			res.put(DistributedRemoteRegistryCache.ActorLocation, act_soapaxis2url);
			res.put(DistributedRemoteRegistryCache.ActorProtocol, OrcaConstants.ProtocolSoapAxis2);
			res.put(DistributedRemoteRegistryCache.ActorCert64, act_cert64);
			DistributedRemoteRegistryCache.getInstance().addLocalCacheEntry(act_guid, res);

		


			// now try registering with remote
		
			String registryUrl_1 = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_1);
	
			if (registryUrl_1 == null) {
				Globals.Log.info("No external registry is specified.");
				return;
			}

			
			Globals.Log.info("Registering actor with external registry at " + registryUrl_1);


			Globals.Log.debug("Registering with  registry - Actor:" + act_name + " | Type:" + act_type + " | GUID: " + 
					act_guid + " | Description: " + act_desc + " | soapaxis2url: " + act_soapaxis2url + " | ActorClass: " + 
					act_class + " | ActorMapperClass: " + act_mapper_class);

			
			//Check if this actor has an account in CouchDB server
			String selectedRegistry = getWorkingActorRegistry(registryUrl_1);
			String trimmedregistry = selectedRegistry.replace("https://", "");
			String userinfo = registryUsername+":"+registryPassword;
			String url = "https://"+userinfo+"@"+trimmedregistry+":6984/actor/_security";
			String method = "GET";
			String data = null;
			SSLRestHttpClient curl = new SSLRestHttpClient();
			
			String result = curl.doHttpCall(url, method, data);
			if(!result.contains(act_guid)) {
				
				//add user
				
				MessageDigest md = MessageDigest.getInstance("SHA1");		
				byte[] certDigest = md.digest(actor.getShirakoPlugin().getKeyStore().getActorPrivateKey().getEncoded());
				
				url = "https://"+userinfo+"@"+trimmedregistry+":6984/_users/org.couchdb.user:"+act_guid;
		        method = "PUT";
		        		 data =  " {\"_id\":\"org.couchdb.user:"+act_guid+"\","
		        	              + "\"name\":\""+act_guid+ "\","
		        	              + "\"roles\":[],"
		        	              + "\"type\":\"user\","
		        	              + "\"password\":\""+certDigest.toString()+"\"}";
		     
		       String response = curl.doHttpCall(url, method, data);
		       //replicate to other couchdb servers
		       replicateUserDatabase(registryUrl_1, selectedRegistry,registryUsername, registryPassword);
		       //membership modified
		    url = "https://"+userinfo+"@"+trimmedregistry+":6984/actor/_security";
       	    method = "GET";
       	    data = null;
       	    result = curl.doHttpCall(url, method, data);
       	    JsonNode actualObj= null;
       	    try {
       	    	ObjectMapper mapper = new ObjectMapper();
					actualObj = mapper.readTree(result);
					JsonNode members = actualObj.get("members");
					JsonNode names = members.get("names");
					
					if(names.isArray()) 
					{
						ArrayNode arraynode = (ArrayNode)names;
						arraynode.add(act_guid);
					}
					
				
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
       
       	     url = "https://"+userinfo+"@"+trimmedregistry+":6984/actor/_security";
	         method = "PUT";
	        
	        		 data =  actualObj.toString();
	        	     result = curl.doHttpCall(url, method, data);
			}
			
			
			//replicate to other couchdb servers
			replicateSecurityDatabase(registryUrl_1, trimmedregistry, registryUsername, registryPassword);

			
			// add actor key and cert into the multikeymanager
//			mkm.addPrivateKey(act_guid,
//					actor.getShirakoPlugin().getKeyStore().getActorPrivateKey(), 
//					actor.getShirakoPlugin().getKeyStore().getActorCertificate());

			// before we do SSL to registry, set our identity
//			mkm.setCurrentGuid(act_guid);

			try {
				//At every ssliClient connection check if the CouchDB is up and working
				String workingActorRegistry = getWorkingActorRegistry(registryUrl_1);
				HttpClient sslClient = new OrcaStdHttpClient.Builder()
				.url(workingActorRegistry)
				.username(registryUsername)
				.password(registryPassword)
				.socketTimeout(60000)
				.enableSSL(true)
				.keyManager(mkm)
				.trustManager(tm)
				.relaxedSSLSettings(true)
				.port(6984)
				.build();


				CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
				CouchDbConnector db = dbInstance.createConnector("actor", false);
				ActorRepository repo = new ActorRepository(db);
				actor thisActor = null;
				try{
				thisActor = repo.get(act_guid);
					/*This is an existing actor coming back to live*/
					/*Lets udpate */
				thisActor.setName(act_name);
				thisActor.setType(act_type);
				thisActor.setDescription(act_desc);
				thisActor.setSoapURL(act_soapaxis2url);
				thisActor.setPutKey(act_pubkey);
				thisActor.setCert(act_cert64);
					repo.update(thisActor);
				} catch (org.ektorp.DocumentNotFoundException e) {
					/*This is a new actor*/
					 thisActor = new actor();
						thisActor.setName(act_name);
						thisActor.setType(act_type);
						thisActor.setId(act_guid);
						thisActor.setDescription(act_desc);
						thisActor.setSoapURL(act_soapaxis2url);
						thisActor.setPutKey(act_pubkey);
						thisActor.setCert(act_cert64);
						thisActor.setVerified("N");
						Globals.Log.info("Inserting into  couchdb registry - Actor:" + act_name + " | Type:" + act_type + " | GUID: " + 
						act_guid + " | Description: " + act_desc + " | soapaxis2url: " + act_soapaxis2url);
						repo.add(thisActor);
							
				}
		
				replicateToAllExcept(registryUrl_1, workingActorRegistry, act_guid, dbInstance);

					sslClient.shutdown();

			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}catch (Exception ex) {
				
				Globals.Log.error("Could not connect to the registry server " + registryUrl + ": ", ex);
				ex.printStackTrace();
			}
			// start update thread
			ActorLiveness checkActorLive = new ActorLiveness(actor.getGuid().toString(), registryUrl);
			


		} catch (Exception e) {
			Globals.Log.error("An error occurred whle attempting to register actor " + actor.getName() + " with external registry", e);
		}

	}
	
	
	public static void replicateSecurityDatabase(String allregistries, String exception, String username, String password) {
		String userinfo = username+":"+password;
		String [] parts = allregistries.split(",");
		for (String aRegistry : parts) {
			if(aRegistry.contains(exception)){ continue; }
		
			String domain = aRegistry.replace("https://","");
			String trimmedException = exception.replace("https://","");
			String method = "GET";
			String data = null;
			String url = "https://"+userinfo+"@"+trimmedException+":6984/actor/_security";
			SSLRestHttpClient sslCurl = new SSLRestHttpClient();
			String response = sslCurl.doHttpCall(url, method, data);
			 url = "https://"+userinfo+"@"+domain+":6984/actor/_security";
			 method = "PUT";
			data = response;
		    Globals.Log.debug("Replicating security information from "+ exception + " to " + domain);
			sslCurl.doHttpCall(url, method, data);
			 
		}
		
	}
	public static void replicateUserDatabase(String allregistries, String exception, String username, String password) {
		
		String userinfo = username+":"+password;
		String [] parts = allregistries.split(",");
		for (String aRegistry : parts) {
			if(aRegistry.contains(exception))
				{
				continue;
				}
			String domain = aRegistry.replace("https://","");
			String trimmedException = exception.replace("https://","");
			String method = "POST";
		    String url = "https://"+userinfo+"@"+domain+":6984/_replicate";
		    String sourceserver="https://"+userinfo+"@"+trimmedException+":6984/_users";
		    String targetserver="https://"+userinfo+"@"+domain+":6984/_users";
		    String data = "{\"source\":\""+sourceserver+"\",\"target\":\""+targetserver+"\"}";
		   
		    SSLRestHttpClient SSLCurl = new SSLRestHttpClient();
		    Globals.Log.debug("Replication user information from "+trimmedException + " to " + domain);
		    SSLCurl.doHttpCall(url, method, data);
		}
	}
	/**
	 * Utility function to trigger replication in a per doc_id basis to all the actor registries available.
	 * @param allregistries allregistries
	 * @param exception exception
	 * @param docid docid
	 * @param dbInstance dbInstance
	 */
	
	public static void replicateToAllExcept(String allregistries, String exception, String docid, CouchDbInstance dbInstance) {
		
		Set<String> docids = new HashSet<String>();
		docids.add(docid);
		String [] parts = allregistries.split(",");
		for (String aRegistry : parts) {
			if(aRegistry.contains(exception))
				{
				continue;
				}
			String user = registryUsername;
			String password = registryPassword;
			
			String domain = aRegistry.replace("https://","");
			String target = "https://"+registryUsername+":"+registryPassword+"@"+domain+":6984/actor";
			try {
			ReplicationCommand cmd = new ReplicationCommand.Builder()
		    .source("actor")
		    .target(target)
		    .docIds(docids)
		    .build();
			 ReplicationStatus status = dbInstance.replicate(cmd);
			 
			 if(status.isOk()) {
					Globals.Log.debug("Replication succeeded.");
				} else {
					Globals.Log.error("Replication to actor registry failed.");
				}
			 
			} catch (org.ektorp.DbAccessException e) {
				
				Globals.Log.error("Actor Registry at " + exception + " was unable to replicate to Actor Registry at "+domain +" docid: " + docid );
				e.printStackTrace();
			}
			
		}
		
	}

	public static String getWorkingActorRegistry(String actorRegistries) 
	{
		String selectedRegistry=null;
		String [] parts = actorRegistries.split(",");
		int numberOfRegistries = parts.length;
		int counter = 0;
		for (String aRegistry : parts) {
			counter++;
			try {
				selectedRegistry=aRegistry;
				HttpClient sslClient = new OrcaStdHttpClient.Builder()
				.url(aRegistry.trim())
				.username(registryUsername.trim())
				.password(registryPassword.trim())
				.enableSSL(true)
				.keyManager(mkm)
				.trustManager(tm)
				.relaxedSSLSettings(true)
				.port(6984)
				.build();

				CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
				CouchDbConnector db = dbInstance.createConnector("actor", true);
				
				break;
				
			} catch (Exception e) {
				Globals.Log.error("Unable to reach Actor Registry at " + aRegistry+":"+"6984" );
				
				if(counter==numberOfRegistries) {
					try {
						throw new Exception("All Actor Registries are down");
					} catch (Exception e1) {
						Globals.Log.error("Throwing the exception for all actor registries down failed");
					}
					
				}
			
			}
			
		}
		
		return  selectedRegistry;
	}
	/*
	 * Register full and abstract rdfs to the registry
	 * fullModel is the String representing the full rdf for the domain
	 * abstractModel is the String representing the abstract rdf for the domain
     * @param proxy proxy
     * @param  fullModel fullModel
     * @param abstractModel abstractModel
	 */

	public static void registerNDLToRegistry(IAuthorityProxy proxy, String fullModel, String abstractModel){

		//	String registryUrl = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl);
		//	String registryMethod = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryMethod);
		String registryUrl_1 = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_1).trim();
		String registryUsername = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCouchDBUsername).trim();
		String registryPassword = Globals.getAdminConfiguration().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCouchDBPassword).trim();

		if (registryUrl_1 == null) {
			Globals.Log.info("No external registry specified for registering NDL.");
			return;
		}


		String act_guid = proxy.getGuid().toString();
		String act_abstract_rdf = abstractModel;

		String act_full_rdf = fullModel;


		//	Globals.Log.info("Registering ndls with external registry at " + registryUrl + " using " + registryMethod + " for actor " + act_guid);


		// set current identity for SSL connection to registry
		mkm.setCurrentGuid(act_guid);

		Globals.Log.debug("Inserting Abstract and Full NDL into registry for actor with ActorGUID: " + act_guid);


		try {
			String workingRegistry = getWorkingActorRegistry(registryUrl_1);
			HttpClient sslClient = new OrcaStdHttpClient.Builder()
			.url(workingRegistry)
			.username(registryUsername)
			.password(registryPassword)
			.enableSSL(true)
			.keyManager(mkm)
			.socketTimeout(60000)
			.trustManager(tm)
			.relaxedSSLSettings(true)
			.port(6984)
			.build();


			CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
			CouchDbConnector db = dbInstance.createConnector("actor", false);
			ActorRepository repo = new ActorRepository(db);
			actor actorToUpdate = repo.get(act_guid);
			actorToUpdate.setAbstNDL(act_abstract_rdf);
			actorToUpdate.setFullNDL(act_full_rdf);
			repo.update(actorToUpdate);
			
			replicateToAllExcept(registryUrl_1, workingRegistry, act_guid, dbInstance);
			sslClient.shutdown();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (Exception e) {
			Globals.Log.error("An error occurred whle attempting to register Ndls with external registry", e);
		}//outter try

	}
}
