package orca.shirako.common.meta;

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.shirako.common.ConfigurationException;
import orca.util.PropList;
import orca.util.ResourceType;

public class ResourcePoolDescriptor
{
    public static final String PropertyType = "type";
    public static final String PropertyLabel = "label";
    public static final String PropertyDescription = "description";
    public static final String PropertyAttributesPrefix = "attribute.";
    public static final String PropertyAttributesCount = "attributescount";
    public static final String PropertyKey = "key";

    private HashMap<String, ResourcePoolAttributeDescriptor> attributes;

    private int units;
    private Date start;
    private Date end;
    private String handlerPath;
    private PackageId handlerPackageId;
    private PluginId handlerPluginId;
    private Properties handlerProperties = new Properties();
    private Properties poolProperties = new Properties();       // local: not serialized and passed between actors

    private ResourceType resourceType;
    private String resourceTypeLabel;
    private String description;
    private String inventory;

    private String poolFactory;

    public ResourcePoolDescriptor()
    {
        attributes = new HashMap<String, ResourcePoolAttributeDescriptor>();
    }

    /**
     * Makes a copy of this resource pool descriptor.
     * Only data intended to be passed from one actor to another
     * is copied.
     */
    public ResourcePoolDescriptor clone() {
        Properties temp = new Properties();
        save(temp, null);
        ResourcePoolDescriptor copy = new ResourcePoolDescriptor();
        try {            
            copy.reset(temp, null);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Unexpected error during deserialization", e);
        }
        return copy;
    }
            
    /**
     * @return the resourceType
     */
    public ResourceType getResourceType()
    {
        return this.resourceType;
    }

    /**
     * @param resourceType the resourceType to set
     */
    public void setResourceType(ResourceType resourceType)
    {
        this.resourceType = resourceType;
    }

    /**
     * @return the handlerPath
     */
    public String getHandlerPath()
    {
        return this.handlerPath;
    }

    /**
     * @param handlerPath the handlerPath to set
     */
    public void setHandlerPath(String handlerPath)
    {
        this.handlerPath = handlerPath;
    }

    /**
     * @return the handlerPackageId
     */
    public PackageId getHandlerPackageId()
    {
        return this.handlerPackageId;
    }

    /**
     * @param handlerPackageId the handlerPackageId to set
     */
    public void setHandlerPackageId(PackageId handlerPackageId)
    {
        this.handlerPackageId = handlerPackageId;
    }

    /**
     * @return the handlerPluginId
     */
    public PluginId getHandlerPluginId()
    {
        return this.handlerPluginId;
    }

    /**
     * @param handlerPluginId the handlerPluginId to set
     */
    public void setHandlerPluginId(PluginId handlerPluginId)
    {
        this.handlerPluginId = handlerPluginId;
    }

    /**
     * @return the handlerProperties
     */
    public Properties getHandlerProperties()
    {
        return this.handlerProperties;
    }

    public Properties getPoolProperties()
    {
        return this.poolProperties;
    }

    /**
     * @return the resourceTypeLabel
     */
    public String getResourceTypeLabel()
    {
        return this.resourceTypeLabel;
    }

    /**
     * @param resourceTypeLabel the resourceTypeLabel to set
     */
    public void setResourceTypeLabel(String resourceTypeLabel)
    {
        this.resourceTypeLabel = resourceTypeLabel;
    }

    /**
     * @return the units
     */
    public int getUnits()
    {
        return this.units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(int units)
    {
        this.units = units;
    }

    /**
     * @return the start
     */
    public Date getStart()
    {
        return this.start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Date start)
    {
        this.start = start;
    }

    /**
     * @return the end
     */
    public Date getEnd()
    {
        return this.end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(Date end)
    {
        this.end = end;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the inventory
     */
    public String getInventory()
    {
        return this.inventory;
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(String inventory)
    {
        this.inventory = inventory;
    }

    public String getPoolFactory()
    {
        return poolFactory;
    }

    public void setPoolFactory(String poolFactory)
    {
        this.poolFactory = poolFactory;
    }

    public ResourcePoolAttributeDescriptor getAttribute(String key)
    {
        return attributes.get(key);
    }

    public Iterable<ResourcePoolAttributeDescriptor> getAttributes()
    {
        return attributes.values();
    }

    public void addAttribute(ResourcePoolAttributeDescriptor attribute)
    {
        if (attributes.containsKey(attribute.getKey())) {
            throw new IllegalArgumentException("Attribute: " + attribute.getKey() + " is already present");
        }
        attributes.put(attribute.getKey(), attribute);
    }

    public void save(Properties p, String prefix)
    {
        String pref = prefix;
        if (pref == null) {
            pref = "";
        }

        // note: pool factory should not be serialized
        p.setProperty(pref + PropertyType, resourceType.toString());
        p.setProperty(pref + PropertyLabel, resourceTypeLabel);
        if (description != null) {
            p.setProperty(pref + PropertyDescription, description);
        }

        PropList.setProperty(p, pref + PropertyAttributesCount, attributes.size());
        int i = 0;
        for (ResourcePoolAttributeDescriptor att : attributes.values()) {
            p.setProperty(pref + PropertyAttributesPrefix + i + "." + PropertyKey, att.getKey());
            String temp = null;
            if (pref.length() > 0) {
                temp = pref + att.getKey() + ".";
            } else {
                temp = att.getKey() + ".";
            }
            att.save(p, temp);
            i++;
        }
    }

    public void reset(Properties p) throws ConfigurationException {
    	reset(p, null);
    }
    
    public void reset(Properties p, String prefix) throws ConfigurationException
    {
        String pref = prefix;
        if (pref == null) {
            pref = "";
        }

        String temp = p.getProperty(pref + PropertyType);
        if (temp == null) {
            throw new ConfigurationException("Missing resource type");
        }
        resourceType = new ResourceType(temp);
        temp = p.getProperty(pref + PropertyLabel);
        if (temp == null) {
            throw new ConfigurationException("Missing resource type label");
        }
        resourceTypeLabel = temp;
        description = p.getProperty(pref + PropertyDescription);
        // note: pool factory should not be deserialized
        temp = p.getProperty(pref + PropertyAttributesCount);
        if (temp == null) {
            throw new ConfigurationException("Missing attributes count");
        }
        int count = 0;
        try {
            count = Integer.parseInt(temp);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Cannot parse attributes count", e);
        }

        for (int i = 0; i < count; i++) {
            String key = pref + PropertyAttributesPrefix + i + "." + PropertyKey;
            String value = p.getProperty(key);
            if (value == null) {
                throw new ConfigurationException("Could not find key for attribute #" + i);
            }
            if (pref.length() > 0) {
                temp = pref + value + ".";
            } else {
                temp = value + ".";
            }
            ResourcePoolAttributeDescriptor ad = new ResourcePoolAttributeDescriptor();
            ad.reset(p, temp);
            ad.setKey(value);
            addAttribute(ad);
        }
    }
}
