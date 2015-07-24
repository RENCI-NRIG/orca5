package orca.manage;

public class ContainerSoapTest extends ContainerTest {
	public IOrcaContainer connect() {
		return Orca.connect(SOAP_URL, USER, PASS);
	}
}