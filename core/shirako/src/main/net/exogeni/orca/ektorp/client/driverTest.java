package net.exogeni.orca.ektorp.client;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Random;

import net.exogeni.orca.ektorp.actor;
import net.exogeni.orca.ektorp.repository.ActorRepository;
import net.exogeni.orca.util.ssl.MultiKeyManager;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;


public class driverTest {

	/**
	 * @param args
	 * @throws MalformedURLException 
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws MalformedURLException {
		
		
		
		SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		X509HostnameVerifier hostnameVerifier=
		          org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);

		
		// pass the multikeymanager
		HttpClient sslClient = new OrcaStdHttpClient.Builder()
               // .url("https://slookup.exogeni.net")
		 	.url("http://slookup.exogeni.net")
                .username("admin")
                .password("exoadmin")
                //.enableSSL(true)
                //.keyManager(new MultiKeyManager())
                 //.relaxedSSLSettings(true)
                .port(5984)
                .build();
	
		
		CouchDbInstance dbInstance = new StdCouchDbInstance(sslClient);
		CouchDbConnector db = dbInstance.createConnector("actor", true);

		
		
		ActorRepository repo = new ActorRepository(db);
		actor x = new actor();
		x.setCert("mycert");
		x.setCert("This is first description");
		Random rnd = new Random();
		x.setId(String.valueOf(rnd.nextInt()));
		x.setName(x.getId());
		x.setSoapURL("http://check.com");
		x.setType("mytype");
		x.setVerified("Y");
		repo.add(x);	
		
		
		repo.update(x);
		
		actor myactor = repo.get(x.getId());
		System.out.println("Date : " + myactor.getAlive());
		System.exit(0);
		
		List<String> theids = db.getAllDocIds();
		for (String string : theids) {
			System.out.println("ids "+string);
		}
		
		
		
		ViewQuery query = new ViewQuery()
        .designDocId("_design/actor")
        .viewName("byName")
        .key("myname");
		
		List<actor> thelist  = db.queryView(query, actor.class);
		System.out.println("size "+thelist.size());
		
		
		ViewQuery verifiedQuery = new ViewQuery()
		.designDocId("_design/actor")
		.viewName("verifiedOnly");
		
		System.out.println("All the actors that are verified");
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
