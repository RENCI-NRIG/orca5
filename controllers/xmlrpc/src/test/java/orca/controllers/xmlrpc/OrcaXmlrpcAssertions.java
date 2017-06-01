package orca.controllers.xmlrpc;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import orca.embed.workflow.ManifestParserListener;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.OrcaConverter;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.TicketReservationMng;
import orca.ndl.NdlException;
import orca.ndl.NdlManifestParser;
import orca.ndl.elements.NetworkElement;
import orca.shirako.container.Globals;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static orca.controllers.xmlrpc.ReservationConverter.PropertyUnitEC2InstanceType;
import static orca.shirako.common.meta.UnitProperties.*;
import static org.junit.Assert.*;

public class OrcaXmlrpcAssertions {

    /**
     * Check all VM reservations for the correct number of localProperties, and fail test by assertion if incorrect.
     *
     * @param computedReservations
     * @param propCountMap
     */
    protected static void assertExpectedPropertyCounts(List<TicketReservationMng> computedReservations, Map<String, Integer> propCountMap){
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " + localProperties.size());

            // we probably need better checks on Properties

            // VLANs don't have consistent IDs from the request
            Integer expected = propCountMap.get(reservation.getReservationID());
            if (expected == null) {
                continue;
            }

            assertEquals("Incorrect number of localProperties for reservation " + reservation.getReservationID(),
                    (long) expected,
                    (long) localProperties.size());
        }
    }


    /**
     * Check for the presence of Netmask property in VM reservations, and fail test by assertion if not present.
     * @param computedReservations
     */
    protected static void
    assertNetmaskPropertyPresent(List<TicketReservationMng> computedReservations){
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " + localProperties.size());

            // only check VMs for Netmask
            System.out.println(reservation.getResourceType());
            if (!reservation.getResourceType().endsWith("vm")){
                continue;
            }

            // every VM in our current tests should have a netmask
            // check for netmask in modified reservation
            String netmask = OrcaConverter.getLocalProperty(reservation, UnitEthPrefix + "1" + UnitEthNetmaskSuffix);
            assertNotNull("Could not find netmask value in computed reservation " + reservation.getReservationID(), netmask);

            String address = OrcaConverter.getLocalProperty(reservation, UnitEthPrefix + "1" + UnitEthIPSuffix);
            if (null != address) {
                assertTrue("Address property should contain CIDR", address.contains("/"));
            }
        }
    }

    /**
     * Check for a network interface being present in all VM reservations.
     *
     * @param computedReservations
     */
    protected static void assertReservationsHaveNetworkInterface(List<TicketReservationMng> computedReservations) {
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " + localProperties.size());

            // only check VMs
            System.out.println(reservation.getResourceType());
            if (!reservation.getResourceType().endsWith("vm")){
                continue;
            }

            String numInterfaces = OrcaConverter.getLocalProperty(reservation, UnitNumberInterface);
            assertNotNull("No network interfaces found in reservation " + reservation.getReservationID(), numInterfaces);
        }
    }

    /**
     * Verify that the EC2 Instance type was present
     * From Issue #106
     *
     * @param computedReservations
     */
    protected static void assertEc2InstanceTypePresent(List<TicketReservationMng> computedReservations) {
        for (TicketReservationMng reservation : computedReservations){
            // only check VMs for EC2 Instance Type
            System.out.println(reservation.getResourceType());
            if (!reservation.getResourceType().endsWith("vm")){
                continue;
            }

            String ec2InstanceType = OrcaConverter.getConfigurationProperty(reservation, PropertyUnitEC2InstanceType);
            assertNotNull("Could not find EC2 Instance Type in reservation " + reservation.getReservationID(), ec2InstanceType);
        }
    }

    /**
     * Verify that the resulting manifest will process.
     * Catches errors such as "orca.ndl.NdlException: Path has 1 (odd number) of endpoints"
     *
     * @param slice
     */
    protected static void assertManifestWillProcess(XmlrpcControllerSlice slice) {
        Logger logger = Globals.getLogger(OrcaRegressionTest.class.getSimpleName());

        RequestWorkflow workflow = slice.getWorkflow();
        List<? extends ReservationMng> computedReservations = slice.getComputedReservations();
        OntModel manifestModel = workflow.getManifestModel();
        LinkedList<OntResource> domainInConnectionList = workflow.getDomainInConnectionList();
        Collection<NetworkElement> boundElements = workflow.getBoundElements();

        // get the manifest from the created slice
        String manifest = slice.getOrc().getManifest(manifestModel, domainInConnectionList, boundElements, (List<ReservationMng>) computedReservations);

        ManifestParserListener parserListener = new ManifestParserListener(logger);
        try {
            NdlManifestParser ndlManifestParser = new NdlManifestParser(manifest, parserListener);

            // verify that the manifest can process
            ndlManifestParser.processManifest();
        } catch (NdlException e) {
            fail(e.toString());
        }
    }

    /**
     * Check all VM reservations for Bootscripts that have been properly templated by Velocity
     *
     * @param computedReservations
     */
    protected static void assertBootscriptVelocityTemplating(List<TicketReservationMng> computedReservations){
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " + localProperties.size());

            // only check VMs for Bootscript
            if (!reservation.getResourceType().endsWith("vm")){
                continue;
            }

            for (PropertyMng property : localProperties) {
                //System.out.println(property.getName() + ": " + property.getValue());
                if (property.getName().equals("unit.instance.config")) {
                    String bootscript = property.getValue();

                    assertFalse("Bootscript was not properly templated by Velocity: " + bootscript, bootscript.contains("$self"));

                    // don't need to check any other properties for this reservation
                    break;
                }
            }
        }
    }
}