package orca.manage;

public class AuthoritySoapTest extends BrokerTest {
    public AuthoritySoapTest() {
        super(SITE_GUID, SITE_NAME);
    }

    public IOrcaContainer connect() {
        return Orca.connect(SOAP_URL, USER, PASS);
    }
}