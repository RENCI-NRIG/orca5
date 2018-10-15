package net.exogeni.orca.shirako.common.meta;

import java.util.Properties;

import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.util.PropList;

public class ResourcePoolAttributeDescriptor {
    public static final String PropertyType = "type";
    public static final String PropertyLabel = "label";
    public static final String PropertyUnit = "unit";
    public static final String PropertyMin = "min";
    public static final String PropertyMax = "max";
    public static final String PropertyValue = "value";

    private String key;
    private ResourcePoolAttributeType type;
    private String label;
    private String unit;
    private long min;
    private long max;
    private String value;

    public ResourcePoolAttributeDescriptor() {
    }

    /**
     * @return the type
     */
    public ResourcePoolAttributeType getType() {
        return this.type;
    }

    /**
     * @param type the type to set
     */
    public void setType(ResourcePoolAttributeType type) {
        this.type = type;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * @param unit the unit to set
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the min
     */
    public long getMin() {
        return this.min;
    }

    /**
     * @param min the min to set
     */
    public void setMin(long min) {
        this.min = min;
    }

    /**
     * @return the max
     */
    public long getMax() {
        return this.max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(long max) {
        this.max = max;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return this.value;
    }

    public int getIntValue() throws IllegalStateException {
        if (type != ResourcePoolAttributeType.INTEGER) {
            throw new IllegalStateException("Expected attribute of type INTEGER, but found: " +type);
        }
        return Integer.parseInt(value);
    }
    
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void save(Properties p, String prefix) {
        String pref = prefix;
        if (pref == null) {
            pref = "";
        }

        if (label != null) {
            p.setProperty(pref + PropertyLabel, label);
        }
        PropList.setProperty(p, pref + PropertyType, type.ordinal());
        if (min > 0) {
            PropList.setProperty(p, pref + PropertyMin, min);
        }
        if (max > 0) {
            PropList.setProperty(p, pref + PropertyMax, max);
        }
        if (unit != null) {
            p.setProperty(pref + PropertyUnit, unit);
        }
        if (value != null) {
            p.setProperty(pref + PropertyValue, value);
        }
    }

    public void reset(Properties p, String prefix) throws ConfigurationException {
        String pref = prefix;
        if (pref == null) {
            pref = "";
        }

        label = p.getProperty(pref + PropertyLabel, label);
        String temp = p.getProperty(pref + PropertyType);
        type = ResourcePoolAttributeType.convert(Integer.valueOf(temp));
        try {
            min = PropList.getLongProperty(p, pref + PropertyMin);
        } catch (Exception e) {
            throw new ConfigurationException("Could not obtain value of min", e);
        }
        try {
            max = PropList.getLongProperty(p, pref + PropertyMax);
        } catch (Exception e) {
            throw new ConfigurationException("Could not obtain value of max", e);
        }
        unit = p.getProperty(pref + PropertyUnit, unit);
        value = p.getProperty(pref + PropertyValue, value);
    }
    
    public String toString(){
    	return this.key+":"+this.label+":"+this.value+":"+this.unit+":"+this.type;					
    }
}
