package orca.manage;

public class ServiceManagerSoapTest extends ServiceManagerTest {
	public ServiceManagerSoapTest() {
		super(SM_GUID, SM_NAME);
	}
	public IOrcaContainer connect() {
		return Orca.connect(SOAP_URL, USER, PASS);
	}
}