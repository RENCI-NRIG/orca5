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

package orca.shirako.container;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import orca.ektorp.actor;
import orca.ektorp.client.OrcaStdHttpClient;
import orca.ektorp.repository.ActorRepository;

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
    private String registryUrl=null;
    private String registryMethod=null;

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
        public void run() {
           
            try {
            	
                Vector<String> params = new Vector<String>();
                // This order of insert is very important, because the xml-rpc
                // server expects the strings in this order
                params.addElement(act_guid);

                SimpleHttpConnectionManager connMgr = new SimpleHttpConnectionManager(true);
                // Connect timeout, 10 seconds; Read timeout, 30 seconds; Close timeout, 1 second.
                connMgr.getParams().setConnectionTimeout(10*1000);
                connMgr.getParams().setSoTimeout(30*1000);
                connMgr.getParams().setLinger(1);
                HttpClient httpClient = new HttpClient(connMgr);

                try {
                    Globals.Log.debug("Liveness check for Actor:" +  act_guid);

                    // set the identity for SSL 
                    RemoteRegistryCache.getMultiKeyManager().setCurrentGuid(act_guid);
                    
                    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                    config.setServerURL(new URL(registryUrl + "/"));
                    XmlRpcClient client = new XmlRpcClient();
                    client.setConfig(config);

                    // We use the XmlRpcCommonsTransportFactory so that SSLContexts work.
                    // We use our own HttpClient, created above, with an HttpConnectionManager
                    // configured with timeouts, so we don't block the liveness thread forever.
                    XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
                    f.setHttpClient(httpClient);
                    client.setTransportFactory(f);
                    
                    String status = (String)client.execute(registryMethod, params);
                    // FIXME: should parse the return (STATUS: SUCCESS or STATUS: ERROR)
                    Globals.Log.info("XMLRPC registry returned  heartbeat status: " + status);
                } catch (Exception ex) {
                	/* Commented out on 02/03/11
		       			Don't need to cancel the timer if heartbeat insertion fails
                	 */
                    //timer.cancel();
                    Globals.Log.error("Could not connect to the registry server; registry server may be down", ex);
                }
                finally {
                    connMgr.shutdown();
                }
            } catch (Exception e) {
            	/* Commented out on 02/03/11
		   			Don't need to cancel the timer if heartbeat insertion fails
            	 */
                //timer.cancel();
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
