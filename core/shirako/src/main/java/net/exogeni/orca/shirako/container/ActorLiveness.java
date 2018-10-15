/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Anirban Mandal
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.exogeni.orca.ektorp.actor;
import net.exogeni.orca.ektorp.client.OrcaStdHttpClient;
import net.exogeni.orca.ektorp.repository.ActorRepository;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
import org.ektorp.impl.StdCouchDbInstance;

/**
 *
 * @author anirban
 * @author claris (Add logic for distributed acto registry with Couchdb Backend)
 */
public class ActorLiveness {

    private static ArrayList<Timer> timers = new ArrayList<Timer>();
    private static boolean noStart = false;
    private String act_guid;
    private String registryUrl = null;
    private String registryMethod = null;

    public ActorLiveness (String input_act_guid, String input_registryUrl, String input_registryMethod) {
        act_guid = input_act_guid;
        registryUrl = input_registryUrl;
        registryMethod = input_registryMethod;

    	Timer timer = null;
    	synchronized(timers) {
    		if (noStart)
    			return;
    		timer = new Timer("ActorLiveness: " + act_guid, true);
    		timers.add(timer);
    	}
        timer.schedule(new TalkToRegistry(), 60*1000, 60*1000);
    }
    
    public ActorLiveness (String input_act_guid, String input_registryUrl) {
        act_guid = input_act_guid;
        registryUrl = input_registryUrl;
       

    	Timer timer = null;
    	synchronized(timers) {
    		if (noStart)
    			return;
    		timer = new Timer("ActorLiveness: " + act_guid, true);
    		timers.add(timer);
    	}
        timer.schedule(new TalkToRegistry2(), 60*1000, 60*1000);
    }
    public static void allStop() {
    	Globals.Log.info("Shutting down liveness threads");
    	synchronized(timers) {
    		noStart=true;
    		for (Timer t: timers) {
    			t.cancel();
    		}
    		timers.clear();
    	}
    }
    
    class TalkToRegistry extends TimerTask {
        private SimpleHttpConnectionManager connMgr;
        private HttpClient httpClient;
        private XmlRpcClient xmlrpcClient;
        private XmlRpcClientConfigImpl xmlrpcClientConfig;
        private XmlRpcCommonsTransportFactory transportFactory;

        protected TalkToRegistry() {
            super();

            // We use our own HttpClient with an HttpConnectionManager
            // configured with timeouts, so we don't block the liveness
            // thread forever.

            // Connect timeout, 10 seconds; Read timeout, 5 seconds; Close timeout, 3 seconds.
            connMgr = new SimpleHttpConnectionManager(true);
            connMgr.getParams().setConnectionTimeout(10*1000);
            connMgr.getParams().setSoTimeout(5*1000);
            connMgr.getParams().setLinger(3);

            httpClient = new HttpClient(connMgr);

            // We use XmlRpcCommonsTransportFactory, initialized
            // using the HttpClient from above, so that SSLContexts
            // work.
            xmlrpcClient = new XmlRpcClient();
            xmlrpcClientConfig = new XmlRpcClientConfigImpl();
            transportFactory =
                new XmlRpcCommonsTransportFactory(xmlrpcClient);
            transportFactory.setHttpClient(httpClient);
            xmlrpcClient.setTransportFactory(transportFactory);

            // Finally - since the SimpleHttpConnectionManager only
            // provides one backend HttpConnection, ensure that the
            // XmlRpcClient only has one backend worker available to
            // consume it.
            xmlrpcClient.setMaxThreads(1);
        }

        public boolean cancel() {
            connMgr.shutdown();
            return super.cancel();
        }

        public void run() {
            try {
                Vector<String> params = new Vector<String>();
                // This order of insert is very important, because
                // the xml-rpc server expects the strings in this
                // order
                params.addElement(act_guid);

                try {
                    Globals.Log.debug("Liveness check for Actor:" +  act_guid);

                    xmlrpcClientConfig.setServerURL(new URL(registryUrl + "/"));

                    // Set identity for SSL.
                    RemoteRegistryCache.getMultiKeyManager().setCurrentGuid(act_guid);
                    
                    // FIXME: Parse the return? (STATUS: SUCCESS or STATUS: ERROR)
                    String status = (String) xmlrpcClient.execute(xmlrpcClientConfig,
                                                                  registryMethod, params);

                    Globals.Log.info("XMLRPC registry returned  heartbeat status: " + status);
                }
                catch (MalformedURLException mue) {
                    Globals.Log.error("Registry server URL is invalid.", mue);
                }
                catch (Exception ex) {
                    Globals.Log.error("Could not connect to the registry server; registry server may be down", ex);
                }
            } catch (Exception e) {
                Globals.Log.error("Registry1: An error occurred while attempting to send heartbeats for actor: " + act_guid + " to external registry", e);
            }
        }
    }
    
    
    class TalkToRegistry2 extends TimerTask {
        public void run() {
            try {
            	
            	
            	try {
            		String workingActor = DistributedRemoteRegistryCache.getWorkingActorRegistry(Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_1));
        			org.ektorp.http.HttpClient sslClient = new OrcaStdHttpClient.Builder()
        			//.url(Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_1))
        			.url(workingActor.trim())
        			.username(Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCouchDBUsername).trim())
        			.password(Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCouchDBPassword).trim())
        			.enableSSL(true)
        			.keyManager(DistributedRemoteRegistryCache.getMultiKeyManager())
        			.trustManager(DistributedRemoteRegistryCache.getTrustManager())
        			.socketTimeout(60000)
        			.relaxedSSLSettings(true)
        			.port(6984)
        			.build();
        		
        		CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
        		CouchDbConnector db = dbInstance.createConnector("actor", true);
        		ActorRepository repo = new ActorRepository(db);
        		actor toupdate =repo.get(act_guid);
        		repo.update(toupdate);
        		String replicationMode = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyReplicationMode);
        
        		if (replicationMode!=null && replicationMode.contains("client"))
        		{
        			DistributedRemoteRegistryCache.replicateToAllExcept(Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryUrl_1).trim(), workingActor, act_guid, dbInstance);

        		}  
        	
    		
        		sslClient.shutdown();
        		
        		} catch (MalformedURLException e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
            
             
            } catch (Exception e) {
           
                Globals.Log.error("Registry An error occurred while attempting to send heartbeats for actor: " + act_guid + " to external registry", e);
            }
        }
    }
}
