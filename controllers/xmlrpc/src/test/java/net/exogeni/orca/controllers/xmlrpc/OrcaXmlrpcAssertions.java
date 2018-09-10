package net.exogeni.orca.controllers.xmlrpc;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import net.exogeni.orca.embed.workflow.ManifestParserListener;
import net.exogeni.orca.embed.workflow.RequestWorkflow;
import net.exogeni.orca.manage.OrcaConverter;
import net.exogeni.orca.manage.beans.PropertiesMng;
import net.exogeni.orca.manage.beans.PropertyMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.ndl.NdlException;
import net.exogeni.orca.ndl.NdlManifestParser;
import net.exogeni.orca.ndl.elements.Interface;
import net.exogeni.orca.ndl.elements.NetworkElement;
import net.exogeni.orca.ndl.DomainResourceType;
import net.exogeni.orca.shirako.container.Globals;
import org.apache.log4j.Logger;
import net.exogeni.orca.shirako.common.meta.UnitProperties;
import net.exogeni.orca.util.PropList;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.exogeni.orca.controllers.xmlrpc.ReservationConverter.PropertyUnitEC2InstanceType;
import static net.exogeni.orca.shirako.common.meta.RequestProperties.RequestBandwidth;
import static net.exogeni.orca.shirako.common.meta.RequestProperties.RequestNumCPUCores;
import static net.exogeni.orca.shirako.common.meta.UnitProperties.*;
import static org.junit.Assert.*;
import static net.exogeni.orca.ndl.DomainResourceType.VLAN_RESOURCE_TYPE;

public class OrcaXmlrpcAssertions {

    /**
     * Check all VM reservations for the correct number of localProperties, and fail test by assertion if incorrect.
     *
     * @param computedReservations
     * @param propCountMap
     */
    protected static void assertExpectedPropertyCounts(List<TicketReservationMng> computedReservations,
            Map<String, Integer> propCountMap) {
        for (TicketReservationMng reservation : computedReservations) {
            Properties localProperties = OrcaConverter.fill(reservation.getLocalProperties());

            // VLANs don't have consistent IDs from the request
            String hostname = OrcaConverter.getLocalProperty(reservation, UnitHostName);
            Integer expected = propCountMap.get(hostname);
            if (expected == null) {
                continue;
            }

            System.out.println("hostname " + hostname + " had localProperties count " + localProperties.size() + " ("
                    + reservation.getReservationID() + ")");
            System.out.println(localProperties.toString().replaceAll(",", ",\n").replaceAll("}", "\n}"));
            assertEquals(
                    "Incorrect number of localProperties for " + hostname + " (" + reservation.getReservationID() + ")",
                    (long) expected, (long) localProperties.size());
        }
    }

    /**
     * Check that properties have specific values. Should be useful to ensure e.g. eth1 and eth2 IP addresses do not
     * change.
     *
     * @param computedReservations
     * @param reservationProperties
     */
    protected static void assertExpectedPropertyValues(List<TicketReservationMng> computedReservations,
            Map<String, PropertiesMng> reservationProperties) {
        for (TicketReservationMng reservation : computedReservations) {
            final String hostname = OrcaConverter.getLocalProperty(reservation, UnitHostName);

            final PropertiesMng propertiesMng = reservationProperties.get(hostname);
            if (null == propertiesMng) {
                continue;
            }

            List<PropertyMng> expectedProperties = propertiesMng.getProperty();
            if (null == expectedProperties) {
                continue;
            }

            System.out.println("Verifying " + expectedProperties.size() + " expected Properties for " + hostname);
            for (PropertyMng property : expectedProperties) {
                final String propertyName = property.getName();
                final String expectedValue = property.getValue();

                final String actualValue = OrcaConverter.getLocalProperty(reservation, propertyName);

                assertEquals("localProperty " + propertyName + " did not match", expectedValue, actualValue);
            }
        }
    }

    /**
     * In the case where interfaces are created at the same time, the ordering cannot be guaranteed. However, the
     * relationship between Link Parent and IP address should be guaranteed.
     *
     * @param computedReservations
     * @param nodeLinkIPsMap
     */
    protected static void assertLinkMatchesIPProperty(List<TicketReservationMng> computedReservations,
            Map<String, Map<String, String>> nodeLinkIPsMap) {
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            final String hostname = OrcaConverter.getLocalProperty(reservation, UnitHostName);

            final Map<String, String> linkIPsMap = nodeLinkIPsMap.get(hostname);
            if (null == linkIPsMap) {
                continue;
            }

            System.out.println("Verifying " + linkIPsMap.size() + " expected Links for " + hostname);
            for (String link : linkIPsMap.keySet()) {
                final String expectedIP = linkIPsMap.get(link);

                // find the matching Link and IP in Properties based on eth number
                for (PropertyMng localProperty : localProperties) {
                    if (link.equals(localProperty.getValue())) {
                        final String pattern = UnitEthPrefix + "(\\d+)" + UnitEthParentUrlSuffix;
                        final Pattern r = Pattern.compile(pattern);
                        final Matcher m = r.matcher(localProperty.getName());

                        if (m.find()) {
                            final String ethNumber = m.group(1);

                            final String ipName = UnitEthPrefix + ethNumber + UnitEthIPSuffix;

                            final String actualIP = OrcaConverter.getLocalProperty(reservation, ipName);

                            assertEquals("eth IP did not match for link " + link, expectedIP, actualIP);
                        } else {
                            fail("Property name did not match pattern: " + pattern);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check for the presence of Netmask property in VM reservations, and fail test by assertion if not present.
     * 
     * @param computedReservations
     */
    protected static void assertNetmaskPropertyPresent(List<TicketReservationMng> computedReservations) {
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            // System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " +
            // localProperties.size());

            // only check VMs for Netmask
            // System.out.println(reservation.getResourceType());
            if (!reservation.getResourceType().endsWith("vm")) {
                continue;
            }

            // every VM in our current tests should have a netmask
            // check for netmask in modified reservation
            String netmask = OrcaConverter.getLocalProperty(reservation, UnitEthPrefix + "1" + UnitEthNetmaskSuffix);
            assertNotNull("Could not find netmask value in computed reservation " + reservation.getReservationID(),
                    netmask);

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
        assertReservationsHaveNetworkInterface(computedReservations, null);
    }

    /**
     * Check for network interface(s) being present, and optionally for the correct number of interfaces
     * 
     * @param computedReservations
     * @param interfaceCountMap
     */
    protected static void assertReservationsHaveNetworkInterface(List<TicketReservationMng> computedReservations,
            Map<String, Integer> interfaceCountMap) {
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            // System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count " +
            // localProperties.size());

            // only check VMs
            // System.out.println(reservation.getResourceType());
            if (!reservation.getResourceType().endsWith("vm")) {
                continue;
            }

            // minimally, the value should not be null
            String hostname = OrcaConverter.getLocalProperty(reservation, UnitHostName);
            String numInterfaces = OrcaConverter.getLocalProperty(reservation, UnitNumberInterface);
            assertNotNull("No network interfaces found for " + hostname + " (" + reservation.getReservationID() + ")",
                    numInterfaces);

            // optionally check for a specific value
            if (null == interfaceCountMap) {
                continue;
            }
            Integer expected = interfaceCountMap.get(hostname);
            if (expected == null) {
                continue;
            }

            System.out.println("hostname " + hostname + " had Interfaces count " + numInterfaces + " ("
                    + reservation.getReservationID() + ")");
            assertEquals("Incorrect number of Interfaces for " + hostname + " (" + reservation.getReservationID() + ")",
                    (long) expected, (long) Integer.parseInt(numInterfaces));
        }
    }

    /**
     * Verify that the EC2 Instance type was present From Issue #106
     *
     * @param computedReservations
     */
    protected static void assertEc2InstanceTypePresent(List<TicketReservationMng> computedReservations) {
        for (TicketReservationMng reservation : computedReservations) {
            // only check VMs for EC2 Instance Type
            System.out.println(reservation.getResourceType());
            if (!reservation.getResourceType().endsWith("vm")) {
                continue;
            }

            String ec2InstanceType = OrcaConverter.getConfigurationProperty(reservation, PropertyUnitEC2InstanceType);
            assertNotNull("Could not find EC2 Instance Type in reservation " + reservation.getReservationID(),
                    ec2InstanceType);
        }
    }

    /**
     * Verify that the resulting manifest will process. Catches errors such as "net.exogeni.orca.ndl.NdlException: Path has 1 (odd
     * number) of endpoints"
     *
     * @param slice
     */
    protected static void assertManifestWillProcess(XmlrpcControllerSlice slice) {
        Logger logger = Globals.getLogger(OrcaXmlrpcAssertions.class.getSimpleName());

        RequestWorkflow workflow = slice.getWorkflow();
        List<? extends ReservationMng> computedReservations = slice.getComputedReservations();
        OntModel manifestModel = workflow.getManifestModel();
        LinkedList<OntResource> domainInConnectionList = workflow.getDomainInConnectionList();
        Collection<NetworkElement> boundElements = workflow.getBoundElements();

        // get the manifest from the created slice
        String manifest = slice.getOrc().getManifest(manifestModel, domainInConnectionList, boundElements,
                (List<ReservationMng>) computedReservations);

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
    protected static void assertBootscriptVelocityTemplating(List<TicketReservationMng> computedReservations) {
        for (TicketReservationMng reservation : computedReservations) {
            List<PropertyMng> localProperties = reservation.getLocalProperties().getProperty();
            System.out.println("reservation: " + reservation.getReservationID() + " had localProperties count "
                    + localProperties.size());

            // only check VMs for Bootscript
            if (!reservation.getResourceType().endsWith("vm")) {
                continue;
            }

            for (PropertyMng property : localProperties) {
                // System.out.println(property.getName() + ": " + property.getValue());
                if (property.getName().equals("unit.instance.config")) {
                    String bootscript = property.getValue();

                    assertFalse("Bootscript was not properly templated by Velocity: " + bootscript,
                            bootscript.contains("$self"));

                    // don't need to check any other properties for this reservation
                    break;
                }
            }
        }
    }

    /**
     * Detects duplicate interface naming from Issue #137
     *
     * @param slice
     */
    protected static void assertSliceHasNoDuplicateInterfaces(XmlrpcControllerSlice slice) {
        Logger logger = Globals.getLogger(OrcaXmlrpcAssertions.class.getSimpleName());

        final RequestWorkflow workflow = slice.getWorkflow();
        final Collection<NetworkElement> boundElements = workflow.getBoundElements();

        assertNotNull(boundElements);

        HashSet<String> interfaceSet = new HashSet<>();
        for (NetworkElement element : boundElements) {
            final LinkedList<Interface> interfaces = element.getClientInterface();

            // VLANs don't have this list, we can skip them
            if (null == interfaces) {
                continue;
            }

            for (Interface clientInterface : interfaces) {
                final String interfaceURI = clientInterface.getURI();
                final boolean isUnique = interfaceSet.add(interfaceURI);
                assertTrue("Duplicate clientInterface detected for " + element.getName() + " " + interfaceURI,
                        isUnique);
            }
        }

    }
    /**
     * Detects slice has expected vlans from Issue #208
     *
     * @param slice
     */
    protected static void assertSliceHasExpectedVlans(XmlrpcControllerSlice slice, 
                                                      List<TicketReservationMng> computedReservations, 
                                                      int expectedInterfaceCount) {
        Logger logger = Globals.getLogger(OrcaXmlrpcAssertions.class.getSimpleName());

        final RequestWorkflow workflow = slice.getWorkflow();
        final Collection<NetworkElement> boundElements = workflow.getBoundElements();

        assertNotNull(boundElements);
        int countVlans=0;
        HashSet<String> interfaceSet = new HashSet<>();
        for (NetworkElement element : boundElements) {
            if(element.getResourceType().getResourceType()==DomainResourceType.VLAN_RESOURCE_TYPE) {
                countVlans++;
            }
        }
        assertEquals("Number intefaces did not match expected value",
                     expectedInterfaceCount,
                     countVlans);
        int skipCreateSliceReservation=0;
        for (TicketReservationMng reservation : computedReservations) {
            // Skip non vm reservations
            if(!reservation.getResourceType().matches("(.*).vm.vm")) {
                continue;
            }
            else {
                if(skipCreateSliceReservation==0) {
                    ++skipCreateSliceReservation;
                    continue;
                }
            }
            // Code updates the config properties which are not updated in AUT framework 
            // Removing the validation of local properties
            // Control properties validated with manual test on inno and docker
            /*
            Properties localProperties = OrcaConverter.fill(reservation.getLocalProperties());
            assertNotNull("Reservation UID " + reservation.getReservationID() + " is missing unit.num.interface: "
                    + expectedInterfaceCount, localProperties.getProperty(UnitProperties.UnitNumberInterface));
            assertEquals("Reservation UID " + reservation.getReservationID() + " is not as expected",
                         String.valueOf(expectedInterfaceCount),
                         localProperties.getProperty(UnitProperties.UnitNumberInterface));
            */
        }
    }

    /**
     * Controller needs to assigned Core Resource Constraints to the Reservation
     *
     * @param computedReservations
     */
    protected static void assertReservationsHaveResourceConstraints(List<TicketReservationMng> computedReservations) {
        for (TicketReservationMng reservation : computedReservations) {
            Properties requestProperties = OrcaConverter.fill(reservation.getRequestProperties());

            // Skip any VLAN reservations
            if (null != requestProperties.getProperty(RequestBandwidth)) {
                continue;
            }
            assertNotNull("Reservation UID " + reservation.getReservationID() + " is missing core constraint: "
                    + RequestNumCPUCores, requestProperties.getProperty(RequestNumCPUCores));
        }

    }

    /**
     * Detects incorrect sameAs / Parent interface names from #178
     *
     * @param slice
     */
    protected static void assertNodeGroupReservationsHaveCorrectInterfaceNames(XmlrpcControllerSlice slice) {
        Logger logger = Globals.getLogger(OrcaXmlrpcAssertions.class.getSimpleName());

        final RequestWorkflow workflow = slice.getWorkflow();
        final Collection<NetworkElement> boundElements = workflow.getBoundElements();

        assertNotNull(boundElements);

        for (NetworkElement element : boundElements) {
            final LinkedList<Interface> interfaces = element.getClientInterface();

            // VLANs don't have this list, we can skip them
            if (null == interfaces) {
                continue;
            }

            element.getResource().getProperty(NdlCommons.OWL_sameAs);
            for (Interface clientInterface : interfaces) {
                final String parentUri = clientInterface.getResource().getProperty(NdlCommons.OWL_sameAs).getResource().getURI();
                // OK: http://geni-orca.renci.org/owl/713280a5-582a-4b8f-ba31-d03cebf8ba58#VLAN0-NodeGroup0
                // Not OK: http://geni-orca.renci.org/owl/713280a5-582a-4b8f-ba31-d03cebf8ba58#VLAN0-NodeGroup0/0
                assertFalse("Invalid parent URI name for " + element.getName() + " " + parentUri,
                        parentUri.matches(".*NodeGroup\\d+/\\d+.*"));

                // OK: http://geni-orca.renci.org/owl/713280a5-582a-4b8f-ba31-d03cebf8ba58#VLAN0-NodeGroup0/1/intf
                // OK: http://geni-orca.renci.org/owl/713280a5-582a-4b8f-ba31-d03cebf8ba58#VLAN0-NodeGroup0/021a98a79-b840-432e-9ee7-4ed15a9be83f/intf
                // Not OK: http://geni-orca.renci.org/owl/713280a5-582a-4b8f-ba31-d03cebf8ba58#VLAN0-NodeGroup0/0/1/intf
                assertTrue("Invalid interface name for " + element.getName() + " " + clientInterface.getName(),
                        clientInterface.getName().matches(".*NodeGroup\\d+/[\\w-]+/intf"));
            }
        }

    }

    /**
     * The list of NdlCommons.computeElementClass includes both the actual node elements, and each unique domain for
     * those node elements. In #157, a domain with both VM and BareMetal node elements would "lose" track of one of
     * those type of elements, which was exhibited in the domain for the "missing" one to not be present in this list.
     *
     * @param manifestModel
     * @param expectedComputeElements
     */
    protected static void assertManifestHasNumberOfComputeElements(OntModel manifestModel,
            int expectedComputeElements) {
        final List<Individual> individuals = manifestModel.listIndividuals(NdlCommons.computeElementClass).toList();

        assertEquals("List of Compute Elements did not match expected size.", expectedComputeElements,
                individuals.size());
    }
}
