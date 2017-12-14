package orca.manage;

public class ReservationSoapTest extends ReservationTest {
    public IOrcaContainer connect() {
        return Orca.connect(SOAP_URL, USER, PASS);
    }
}