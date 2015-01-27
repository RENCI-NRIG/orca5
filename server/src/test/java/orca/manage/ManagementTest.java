package orca.manage;

import java.util.HashSet;

import orca.server.OrcaTestServer;
import orca.util.ID;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class ManagementTest {
	public static final ID SITE_GUID = new ID("c48e2cb1-e00e-4ed2-918b-8fe4368c8c02");
	public static final String SITE_NAME = "ben-vlan-site";
	public static final ID BROKER_GUID = new ID("ebeb5bf1-4751-410a-94bf-0075297c162d");
	public static final String BROKER_NAME = "ben-vlan-broker";
	public static final ID SM_GUID = new ID("46AEA103-1185-4E69-8AFA-53194582F5A9");
	public static final String SM_NAME = "service";
	public static final String USER = "admin";
	public static final String PASS = "login";

	public static final String SOAP_URL = "soap://http://localhost:8080/orca/spring-services/";

	public static final HashSet<ID> ActorGuids = new HashSet<ID>();
	
	static {
		ActorGuids.add(SITE_GUID);
		ActorGuids.add(BROKER_GUID);
		ActorGuids.add(SITE_GUID);
	}
	
	@BeforeClass
	public static void startServer() throws Exception {
		OrcaTestServer.startServer();
	}

	@AfterClass
	public static void stopServer() throws Exception {
		OrcaTestServer.stopServer();
	}
	
	protected abstract IOrcaContainer connect();
	
	public String getCN(String principal){
		String[] groups = principal.split(",");
		for (String g : groups){
			int index = g.indexOf("CN=");
			if (index >= 0){
				return g.substring(index+3);
			}
		}
		return null;
	}
}