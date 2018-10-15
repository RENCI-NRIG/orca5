package net.exogeni.orca.manage;

public class BrokerSoapTest extends BrokerTest {
    public BrokerSoapTest() {
        super(BROKER_GUID, BROKER_NAME);
    }

    public IOrcaContainer connect() {
        return Orca.connect(SOAP_URL, USER, PASS);
    }
}
