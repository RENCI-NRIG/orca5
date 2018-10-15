package net.exogeni.orca.manage;

public class AuthorityLocalTest extends BrokerTest {
    public AuthorityLocalTest() {
        super(SITE_GUID, SITE_NAME);
    }

    public IOrcaContainer connect() {
        return Orca.connect();
    }
}
