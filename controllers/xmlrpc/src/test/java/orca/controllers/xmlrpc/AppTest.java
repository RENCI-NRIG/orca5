package orca.controllers.xmlrpc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import orca.shirako.common.meta.UnitProperties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName
     *            name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public static void main(String[] argv) {
        Pattern pattern = Pattern.compile(UnitProperties.UnitEthPrefix + "[\\d]+" + UnitProperties.UnitEthMacSuffix);

        Matcher m = pattern.matcher("unit.eth.mac");

        if (m.matches())
            System.out.println("YES");
        else
            System.out.println("NO");

    }
}
