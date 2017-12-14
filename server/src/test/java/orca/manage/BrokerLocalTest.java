package orca.manage;

public class BrokerLocalTest extends BrokerTest {
    public BrokerLocalTest() {
        super(BROKER_GUID, BROKER_NAME);
    }

    public IOrcaContainer connect() {
        return Orca.connect();
    }
}