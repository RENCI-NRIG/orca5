package net.exogeni.orca.controllers.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.exogeni.orca.embed.RequestWorkflowTest;
import net.exogeni.orca.embed.policyhelpers.DomainResourcePools;
import net.exogeni.orca.embed.policyhelpers.RequestMappingException;
import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.ndl.NdlException;
import net.exogeni.orca.ndl.NdlModel;
import net.exogeni.orca.ndl.elements.NetworkElement;

import net.exogeni.orca.ndl.elements.OrcaReservationTerm;
import net.exogeni.orca.shirako.common.meta.ConfigurationProperties;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

public class ReservationConverterTest extends RequestWorkflowTest {

    // String manifestRecoverFile =
    // "/Users/yxin/ORCA/controller-recovery/embed/src/test/resources/net/exogeni/orca/embed/ng-1-manifest.rdf";
    String manifestRecoverFile = "../../ndl/src/test/resources/shared-vlan.rdf";

    @Before
    public void setUp() throws Exception {
        ORCA_SRC_HOME = "../../"; // calling tests outside this package
        super.setUp();
        // requestFileGush = "/Users/yxin/ORCA/controller-recovery/embed/src/test/resources/net/exogeni/orca/embed/SFDemo_RLS.rdf";
        requestFileGush = "../../embed/src/test/resources/net/exogeni/orca/embed/TS2/TS2-8.rdf";
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testRecover() throws NdlException {
        String manifestStr = NdlCommons.readFile(manifestRecoverFile);
        ByteArrayInputStream modelStream = new ByteArrayInputStream(manifestStr.getBytes());
        OntModel manifestModel = NdlModel.getModelFromStream(modelStream, OntModelSpec.OWL_MEM_RDFS_INF, true);

        ReservationConverter orc = new ReservationConverter();
        workflow.recover(Logger.getLogger(this.getClass()), null, null, manifestModel);
        orc.recover(workflow);

        ReservationElementCollection ec = orc.recoverElementCollection(workflow.getBoundElements());
    }

    @Test
    public void testRun() throws IOException, RequestMappingException, NdlException {
        String reqStr = NdlCommons.readFile(requestFileGush);
        getAbstractModels();
        DomainResourcePools drp = new DomainResourcePools();
        drp.getDomainResourcePools(pools);

        workflow.run(drp, abstractModels, reqStr, null, null, "slice-id");

        System.out.println(workflow.getErrorMsg());

        LinkedList<NetworkElement> connection = (LinkedList<NetworkElement>) workflow.getBoundElements();

        print(connection);

        // fail("Not yet implemented");
    }

    /**
     * Set the lease term to the maximum allowable. Should succeed without changes.
     *
     */
    public void testSetLeaseTermValid() {
        ReservationConverter orc = new ReservationConverter();
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());

        // trying to set to exactly the Maximum can cause discrepancies between Days and Milliseconds
        // (e.g. near Daylight Savings Time changes)
        final long maxDays = TimeUnit.MILLISECONDS.toDays(ReservationConverter.getMaxDuration());
        term.setDuration(Math.toIntExact(maxDays - 1), 0, 0, 0);

        orc.setLeaseTerm(term);

        assertTrue("Valid reservation term should not have been modified", term.getEnd() == orc.leaseEnd);
        System.out.println("Term end date set to: " + orc.leaseEnd);
    }

    /**
     * Attempt to set the lease term to greater than the maximum allowable. The lease term should be updated without
     * errors, but only up to the maximum allowable.
     *
     */
    public void testSetLeaseTermInvalid() {
        ReservationConverter orc = new ReservationConverter();
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());

        final long maxDays = TimeUnit.MILLISECONDS.toDays(ReservationConverter.getMaxDuration());
        term.setDuration(Math.toIntExact(maxDays + 1), 0, 0, 0);

        orc.setLeaseTerm(term);

        assertFalse("Invalid reservation term not updated", term.getEnd() == orc.leaseEnd);
        System.out.println("Term end date updated to: " + orc.leaseEnd);
    }

    /**
     * Test that SSH key property is generated from UsersMap. This function passes the keys in the Map as a List, as
     * used by most of our Unit Tests.
     *
     * @throws ReservationConverter.ReservationConverterException
     */
    public void testGenerateSSHPropertiesWithListKey() throws ReservationConverter.ReservationConverterException {
        List<Map<String, ?>> users = OrcaXmlrpcHandlerTest.getUsersMap();

        final Properties sshProperties = ReservationConverter.generateSSHProperties(users);

        assertNotNull("Generated SSH Properties were null", sshProperties);

        final String keyProperty = sshProperties
                .getProperty(String.format(ConfigurationProperties.ConfigSSHKeyPattern, 1));

        assertNotNull("Returned SSH key was null", keyProperty);
    }

    /**
     * Test that SSH key property is generated from UsersMap. This function passes the keys in the Map as an Object[],
     * which is how they appear from XMLRPC.
     *
     * @throws ReservationConverter.ReservationConverterException
     */
    public void testGenerateSSHPropertiesWithObjectKey() throws ReservationConverter.ReservationConverterException {
        List<Map<String, ?>> users = getUsersMapWithObjects();

        final Properties sshProperties = ReservationConverter.generateSSHProperties(users);

        assertNotNull("Generated SSH Properties were null", sshProperties);

        final String keyProperty = sshProperties
                .getProperty(String.format(ConfigurationProperties.ConfigSSHKeyPattern, 1));

        assertNotNull("Returned SSH key was null", keyProperty);
    }

    /**
     * Test that SSH key property is generated from UsersMap. This function passes the keys in the Map as a String
     * object.
     *
     * @throws ReservationConverter.ReservationConverterException
     */
    public void testGenerateSSHPropertiesWithStringKey() throws ReservationConverter.ReservationConverterException {
        List<Map<String, ?>> users = getUsersMapWithString();

        final Properties sshProperties = ReservationConverter.generateSSHProperties(users);

        assertNotNull("Generated SSH Properties were null", sshProperties);

        final String keyProperty = sshProperties
                .getProperty(String.format(ConfigurationProperties.ConfigSSHKeyPattern, 1));

        assertNotNull("Returned SSH key was null", keyProperty);
    }

    /**
     * Similar to function in OrcaXmlrpcHandlerTest, this is a slightly modified version explicity for testing
     * ReservationConverter
     *
     * Craft a userMap required by createSlice() and modifySlice().
     *
     * @return a UserMap with junk values
     */
    private static List<Map<String, ?>> getUsersMapWithObjects() {
        List<Map<String, ?>> users = new ArrayList<>();
        Map<String, Object> userEntry = new HashMap<>();
        Object[] keys = { "ssh-rsa this is not a key" };
        userEntry.put("keys", keys);
        userEntry.put("login", "root");
        userEntry.put("sudo", false);
        users.add(userEntry);
        return users;
    }

    /**
     * Similar to function in OrcaXmlrpcHandlerTest, this is a slightly modified version explicity for testing
     * ReservationConverter
     *
     * Craft a userMap required by createSlice() and modifySlice().
     *
     * @return a UserMap with junk values
     */
    private static List<Map<String, ?>> getUsersMapWithString() {
        List<Map<String, ?>> users = new ArrayList<>();
        Map<String, Object> userEntry = new HashMap<>();
        String keys = "ssh-rsa this is not a key";
        userEntry.put("keys", keys);
        userEntry.put("login", "root");
        userEntry.put("sudo", false);
        users.add(userEntry);
        return users;
    }
}
