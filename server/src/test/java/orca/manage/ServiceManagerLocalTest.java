package orca.manage;

public class ServiceManagerLocalTest extends ServiceManagerTest {
	public ServiceManagerLocalTest() {
		super(SM_GUID, SM_NAME);
	}
	public IOrcaContainer connect() {
		return Orca.connect();
	}
}