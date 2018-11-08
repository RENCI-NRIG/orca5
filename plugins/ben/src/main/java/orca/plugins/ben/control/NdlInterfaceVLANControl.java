package orca.plugins.ben.control;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Map.Entry;

import orca.ndl.DomainResource;
import orca.ndl.NdlCommons;
import orca.ndl.elements.LabelSet;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.util.ResourceData;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

public class NdlInterfaceVLANControl extends NdlVLANControl {
    private static final String ConfigInterfaceUrl2 = "config.interface.url.2";
    private static final String ConfigInterfaceUrl1 = "config.interface.url.1";

    // NOTE: needs restoring from revisit
    @NotPersistent
    BitSet usedTagBitSet = new BitSet(NdlCommons.max_vlan_tag);

    /**
     * {@inheritDoc}
     */

    public NdlInterfaceVLANControl() {
        super();
    }

    public ResourceSet assign(IAuthorityReservation r) throws Exception {
        /*
         * Send back reservations if they have a deficit. For now we want to avoid reservations stuck forever looping
         * between allocation and priming with failures, eventually exhausting the inventory.
         */
        ResourceSet r_set = null;

        r.setSendWithDeficit(true);

        if (tags.size() == 0) {
            throw new Exception("no inventory");
        }

        // get the requested resources
        ResourceSet requested = r.getRequestedResources();
        // get the requested resource type
        ResourceType type = requested.getType();

        // get the currently assigned resources (if any)
        ResourceSet current = r.getResources();
        // get the ticket
        Ticket ticket = (Ticket) requested.getResources();

        ResourceTicket rt = ticket.getTicket();
        Properties ticketProperties = rt.getProperties();

        // COMMENTED OUT SINCE NOT USED /ib 04/09/14
        // get the requested lease term
        // Term term = r.getRequestedTerm();
        // convert to cycles
        // long start = authority.getActorClock().cycle(term.getNewStartTime());
        // long end = authority.getActorClock().cycle(term.getEndTime());

        Properties configuration_properties = requested.getConfigurationProperties();

        // determine if this is a new lease or an extension request
        Integer tag = null;
        if (current == null) {
            // this is a new request
            // how much does the client want?
            long needed = ticket.getUnits();
            if (needed > 1) {
                r.fail("Cannot assign more than 1 VLAN per reservation");
            }

            String configTag = configuration_properties.getProperty(ConfigUnitTag);

            Integer staticTag = null;
            if (configTag != null) {
                staticTag = Integer.valueOf(configTag);
            }
            if (tags.getFree() > 0) {
                if (staticTag != null) {
                    String key = null, value = null, pdomain = null;
                    int index = 0;
                    logger.debug("NdlInterfaceControl.convertEdgeProperties(): configuration_properties="
                            + configuration_properties.size());
                    for (Entry<Object, Object> entry : configuration_properties.entrySet()) {
                        key = (String) entry.getKey();
                        value = (String) entry.getValue();
                        if (key.contains(EdgeTagSuffix)) {
                            logger.debug("  Match  properties: key =" + key + ";value=" + value);
                            staticTag = Integer.valueOf(value);
                            break;
                        }
                    }
                    tag = tags.allocate(staticTag, true);
                    logger.debug("NdlInterfaceVLANControl.assign(): config tag=" + staticTag);
                } else { // do per interface tag assignment
                    BitSet bit_set_1 = null, bit_set_2 = null, bit_set = null;
                    // String edge_intf_1 = configuration_properties.getProperty("config.interface.1");
                    // String edge_intf_2 = configuration_properties.getProperty("config.interface.2");
                    String edge_intf_1_str = configuration_properties.getProperty(ConfigInterfaceUrl1);
                    String edge_intf_2_str = configuration_properties.getProperty(ConfigInterfaceUrl2);

                    DomainResource d_r_1 = null, d_r_2 = null;
                    if (drs == null) {
                        throw new Exception(
                                "NdlInterfaceVLANControl.assign(): DomainResources is null, unable to proceed");
                    }
                    if (edge_intf_1_str != null && drs.getResource(edge_intf_1_str) != null) {
                        d_r_1 = drs.getResource(edge_intf_1_str);
                        logger.debug("NdlInterfaceVLANControl.assitn(): edge_intf_1_str=" + edge_intf_1_str
                                + ";domain resource=" + d_r_1);
                        bit_set_1 = getLabelSet(d_r_1);
                    }

                    if (edge_intf_2_str != null && drs.getResource(edge_intf_2_str) != null) {
                        d_r_2 = drs.getResource(edge_intf_2_str);
                        logger.debug("NdlInterfaceVLANControl.assign(): edge_intf_2_str=" + edge_intf_2_str
                                + ";domain resource=" + d_r_2);
                        bit_set_2 = getLabelSet(d_r_2);
                    }

                    if (bit_set_1 != null && bit_set_2 != null) {
                        bit_set_1.and(bit_set_2);
                        bit_set = bit_set_1;
                    } else {
                        if (bit_set_1 != null)
                            bit_set = bit_set_1;
                        else
                            bit_set = bit_set_2;
                    }

                    if (bit_set != null) {
                        bit_set.andNot(usedTagBitSet);
                        staticTag = bit_set.nextSetBit(0);
                        tag = tags.allocate(staticTag, true);
                    } else
                        tag = tags.allocate();
                    logger.debug("NdlInterfaceVLANControl.assign(): assigned tag=" + tag + "intf1=" + edge_intf_1_str
                            + ";intf2=" + edge_intf_2_str);
                }

                logger.debug("NdlInterfaceVLANControl.assign(): assigned tag=" + tag);

                usedTagBitSet.set(tag);

                ResourceData rd = new ResourceData();
                PropList.setProperty(rd.getResourceProperties(), UnitVlanTag, tag);
                UnitSet gained = new UnitSet(authority.getShirakoPlugin());
                Unit u = new Unit();
                u.setResourceType(type);
                u.setProperty(UnitVlanTag, tag.toString());
                // if the broker allocated specific bandwidth, set it as a
                // property on the unit
                if (ticketProperties.containsKey(ResourceProperties.ResourceBandwidth)) {

                    long bw = Long.parseLong(ticketProperties.getProperty(ResourceProperties.ResourceBandwidth));
                    long burst = bw / 8;
                    u.setProperty(UnitVlanQoSRate, Long.toString(bw));
                    u.setProperty(UnitVlanQoSBurstSize, Long.toString(burst));
                } else {
                    // System.out.println("ticketProperties doesn't contain bandwidth key");
                }
                gained.add(u);
                r_set = new ResourceSet(gained, null, null, type, rd);
                if (r_set != null) {
                    convertEdgeProperties(configuration_properties, tag);
                }
            } else {
                // no resource - delay the allocation
                r_set = null;
            }
        } else {
            // extend automatically
            r_set = new ResourceSet(null, null, null, type, null);
        }
        return r_set;
    }

    protected void free(Units set) throws Exception {
        // FIXME: some of the units in the set can be in the failed state
        // how should this control handle failed units?

        if (set != null) {
            Iterator<?> i = set.iterator();
            while (i.hasNext()) {
                Unit u = (Unit) i.next();
                try {
                    Integer tag = getTag(u);
                    if (tag == null) {
                        logger.error("NdlInterfaceVLANControl.free(): attempted to free a unit with a missing tag");
                    } else {
                        logger.debug("NdlInterfaceVLANControl.free(): freeing tag: " + tag);
                    }
                    tags.free(tag);
                    usedTagBitSet.clear(tag);
                } catch (Exception e) {
                    logger.error("NdlInterfaceVLANControl.free(): Failed to release vlan tag", e);
                }
            }
        }
    }

    public BitSet getLabelSet(DomainResource d_r) {
        int min_label, max_label;
        BitSet bit_set = new BitSet(NdlCommons.max_vlan_tag);
        if (d_r == null)
            return null;
        LinkedList<LabelSet> l_1 = d_r.getLabel_list();
        if (l_1 != null) {
            for (LabelSet ls : l_1) {
                min_label = (int) ls.getMinLabel_ID();
                max_label = (int) ls.getMaxLabe_ID();
                if ((min_label == max_label) || (max_label == 0))
                    bit_set.set(min_label);
                else
                    bit_set.set(min_label, max_label + 1);
            }
        }
        return bit_set;
    }

    // almost same as VlanControl, with the exception of updating
    // usedTagBitSet
    @Override
    public void revisit(IReservation r) throws Exception {
        // Recovery policy: we start by assuming that no resources are
        // allocated.
        // As we see recovered reservations, we mark as in use all vlan tags for
        // nodes that are not marked as CLOSED
        UnitSet uset = (UnitSet) (r.getResources().getResources());
        Units set = uset.getSet();

        for (Unit u : set) {
            try {
                switch (u.getState()) {
                case DEFAULT:
                case FAILED:
                case CLOSING:
                case PRIMING:
                case ACTIVE:
                case MODIFYING:
                    Integer tag = getTag(u);
                    if (tag == null) {
                        logger.error("NdlInterfaceVLANControl.revisit(): Recoverying a unit without a tag");
                    } else {
                        logger.debug("NdlInterfaceVLANControl.revisit(): reserving tag " + tag + " during recovery");
                        tags.allocate(tag, true);
                        usedTagBitSet.set(tag);
                    }
                    break;
                case CLOSED:
                    logger.debug("NdlInterfaceVLANControl.revisit(): node is closed. Nothing to recover");
                    break;

                }
            } catch (Exception e) {
                fail(u, "revisit with NdlInterfaceVLANControl", e);
            }
        }
    }

    @Override
    public void recoveryStarting() {
        logger.info("Beginning NdlInterfaceVLANControl recovery");
    }

    @Override
    public void recoveryEnded() {
        logger.info("Completing NdlInterfaceVLANControl recovery");
        logger.debug("Restored NdlInterfaceVlanControl DomainResources: " + drs);
        logger.debug("Restored NdlInterfaceVlanControl tags: " + tags);
    }
}
