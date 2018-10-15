package net.exogeni.orca.ektorp.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Random;

import javax.net.ssl.TrustManager;

import net.exogeni.orca.ektorp.actor;
import net.exogeni.orca.ektorp.repository.ActorRepository;
import net.exogeni.orca.util.ssl.MultiKeyManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;


public class driverTest {

	/**
	 * @param args arguments
	 * @throws MalformedURLException in case of malformed URL
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws MalformedURLException {
		
		
		
		SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		X509HostnameVerifier hostnameVerifier=
		          org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);

		// pass the multikeymanager
		HttpClient sslClient = new OrcaStdHttpClient.Builder()
           // .url("http://tamu-hn.exogeni.net")
		 .url("http://wsu-hn.exogeni.net")
                .username("admin")
                .password("X0admin")
               // .enableSSL(true)
               //.keyManager(new MultiKeyManager())
                 //.relaxedSSLSettings(true)
                // .trustManager(new TrustManager())
                .port(5984)
                .build();
		
		
	
		
	
		
		CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
		CouchDbConnector db = dbInstance.createConnector("actor", true);
		
		
		
		ActorRepository repo = new ActorRepository(db);
		List<actor> actorList_ = repo.getAll();
		for (actor y : actorList_) {
			System.out.println("Removing actor  "+y.getName());
		    repo.remove(y);
		}
		System.exit(1);
		actor x = null;
		boolean existing=true;
		String identification = "12312321312312";
	try {
		x = repo.get(identification);
		/*Do not change the Verified field*/
		String verified = x.getVerified();
		x.setVerified(verified);
		repo.update(x);	
		System.out.println("Actor "+x.getId() + " has been updated.");
	} catch (org.ektorp.DocumentNotFoundException e) {
		/*Add the actor. It is a new actor*/
		actor newactor = new actor();
		newactor.setCert("mycert");
		newactor.setCert("This is first description");
		Random rnd = new Random();
		newactor.setId(identification);
		newactor.setName(newactor.getId());
		newactor.setSoapURL("http://check.com");
		newactor.setType("mytype");
		newactor.setVerified("N");
		repo.add(newactor);	
		System.out.println("Actor "+ identification + " has been created and added to registry.");
	}
	
		System.exit(1);
	
		
		
	
		
		actor myactor = repo.get(x.getId());
		System.out.println("Date : " + myactor.getAlive());
		
		
		List<String> theids = db.getAllDocIds();
		
		
		ViewQuery query = new ViewQuery()
        .designDocId("_design/actor")
        .viewName("byName")
        .key("myname");
		
		List<actor> thelist  = db.queryView(query, actor.class);
		System.out.println("size "+thelist.size());
		System.out.println("HERE");
	
		
		
		System.exit(1);
		ViewQuery verifiedQuery = new ViewQuery()
		.designDocId("_design/actor")
		.viewName("verifiedOnly");
		
		System.out.println("All the actors that are verified");
		System.exit(10);
		List<actor> verifiedList = db.queryView(verifiedQuery,actor.class);
		for (actor actor : verifiedList) {
			System.out.println(" actor: " + '\t' + actor.getId() +'\t' + actor.getName() + '\t'+ actor.getVerified());
		}
		
		
		System.out.println("List of all actors");
		
		List<actor> actorList = repo.getAll();
		for (actor actor : actorList) {
			System.out.println("Actor : "+actor.getName());
		
		}
		
		
		System.out.println("Done");
		System.exit(1);
		
		
		actor y = repo.get("myid");
		y.getId();
		System.out.println("Id "+ y.getId() + "revision "+y.getRevision());
		
	
		
		
		
	
	}

}
