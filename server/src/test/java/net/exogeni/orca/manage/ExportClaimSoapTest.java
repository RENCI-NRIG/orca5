package net.exogeni.orca.manage;

public class ExportClaimSoapTest extends ExportClaimTest {
    public IOrcaContainer connect() {
        return Orca.connect(SOAP_URL, USER, PASS);
    }
}
