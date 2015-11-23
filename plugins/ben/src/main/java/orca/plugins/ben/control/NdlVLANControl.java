package orca.plugins.ben.control;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Properties;

import orca.embed.workflow.Domain;
import orca.ndl.DomainResource;
import orca.ndl.DomainResources;
import orca.ndl.elements.Label;
import orca.ndl.elements.LabelSet;
import orca.policy.core.VlanControl;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.util.ResourceData;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;

import com.hp.hpl.jena.ontology.OntModel;

public class NdlVLANControl extends VlanControl {
	public static final String EdgeIntfSuffix = ".edge.interface";
    public static final String EdgeIntfUrlSuffix = ".edge.interface.url";
    public static final String EdgeTagSuffix = "." + UnitProperties.UnitVlanTag;
    public static final String ConfigIntfSuffix = "config.interface.";
    public static final String ConfigTagSuffix = "config.vlan.tag.";
    
    // restored in donate
    @NotPersistent
    protected DomainResources drs;

    public NdlVLANControl() {
        super();
    }

    // FIXME: supports only a single source reservation!
    @Override
    public void donate(IClientReservation r) throws Exception {
        super.donate(r);

        String substrateFile = getSubstrateFile(r);
        Domain d = new Domain(substrateFile);
        OntModel delegateModel = d.delegateDomainModel(r.getType().toString());
        drs = d.getDomainResources(delegateModel, this.tags.size());
    }

    @Override
    public ResourceSet assign(IAuthorityReservation r) throws Exception {
        /*
         * Send back reservations if they have a deficit. For now we want to
         * avoid reservations stuck forever looping between allocation and
         * priming with failures, eventually exhausting the inventory.
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

        // The following stuff isn't used, so commenting out /ib 04/01/14
        // get the requested lease term
        //Term term = r.getRequestedTerm();
        // convert to cycles
        //long start = authority.getActorClock().cycle(term.getNewStartTime());
        //long end = authority.getActorClock().cycle(term.getEndTime());
        
        Properties configuration_properties = requested.getConfigurationProperties();

        // determine if this is a new lease or an extension request
        Integer tag = null;
        logger.info("current resource set is " + current);
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
                if (staticTag == null)
                    tag = tags.allocate();
                else
                    tag = tags.allocate(staticTag, true);
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

    protected void convertEdgeProperties(Properties configuration_properties, Integer tag) {
        Properties config_properties = new Properties();
        HashSet<String> pdomainSet = new HashSet<String>();

        String key = null, value = null, pdomain = null;
        int index = 0;
        logger.debug("NdlVLANControl.convertEdgeProperties(): configuration_properties="
                + configuration_properties.size());
        for (Entry<Object,Object> entry : configuration_properties.entrySet()) {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
            if (key.contains(EdgeTagSuffix) || key.contains(EdgeIntfSuffix)
                    || key.contains(EdgeIntfUrlSuffix)) {
                logger.debug("  Match  properties: key =" + key + ";value=" + value);
                config_properties.put(key, value);
                index = key.indexOf(EdgeIntfUrlSuffix);
                if (index > 0) {
                    pdomain = key.substring(0, index);
                    pdomainSet.add(pdomain);
                    logger.debug("    pdomain =" + pdomain);
                }
            }
        }
        logger.debug("NdlVLANControl.convertEdgeProperties(): pdomainSet size=" + pdomainSet.size());
        int i = 0;
        Iterator<String> pdomainSetIt = pdomainSet.iterator();
        while (pdomainSetIt.hasNext()) {
            i++;
            pdomain = pdomainSetIt.next();
            String intf_url = null, p_tag = null;
            for (Entry<Object,Object> entry : config_properties.entrySet()) {
                key = (String) entry.getKey();
                value = (String) entry.getValue();
                if (key.contains(pdomain)) {
                    if (key.endsWith(EdgeIntfUrlSuffix))
                        intf_url = value;
                    if (key.endsWith(EdgeTagSuffix))
                        p_tag = value;
                    if (key.endsWith(EdgeIntfSuffix))
                        configuration_properties.put(ConfigIntfSuffix + String.valueOf(i), value);
                }
            }
            if (p_tag == null) {
                p_tag = tag.toString();
            }
            String real_tag = tagSwap(intf_url, p_tag);
           	logger.debug("NdlVLANControl.convertEdgeProperties(): real_tag=" + real_tag);
            if (real_tag != null) {
                configuration_properties.put(ConfigTagSuffix + String.valueOf(i), real_tag);
            }
        }
    }

    public String tagSwap(String intf_url, String p_tag) {
        String real_tag = p_tag;
        if ((drs == null) || (p_tag == null) || (intf_url == null)) {
            return real_tag;
        }
        DomainResource dr = drs.getResource(intf_url);
        if (dr == null) {
            return real_tag;
        }

        logger.debug("NdlVLANControl.tagSwap(): dr=" + dr.toString());
        LinkedList<LabelSet> label_list = dr.getLabel_list();
        if (label_list == null) {
        	logger.warn("NdlVLANControl.tagSwap():No labellist, intf="+intf_url);
            return real_tag;
        }
        for (LabelSet l_s : label_list) {
            Label min_label = l_s.getMinLabel();
            if (min_label.label == Float.valueOf(p_tag).floatValue()) {
                logger.debug("NdlVLANControl.tagSwap(): min_label=" + min_label.label + ";swap="
                        + min_label.swap);
                if (min_label.swap != 0)
                    real_tag = String.valueOf(min_label.swap.intValue());
                break;
            }
        }

        return real_tag;
    }
    
	@Override
	public void recoveryStarting() {
		logger.info("Beginning NdlVlanControl recovery");
	}

	@Override
	public void recoveryEnded() {
		logger.info("Completing NdlVlanControl recovery");
		logger.debug("Restored NdlVlanControl tags: " + tags);
		logger.debug("Restored NdlVlanControl DomainResources: " + drs);
	}
}
