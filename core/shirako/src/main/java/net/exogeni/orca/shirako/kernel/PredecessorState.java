/**
 * 
 */
package net.exogeni.orca.shirako.kernel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.shirako.api.IConcreteSet;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitSet;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.PersistenceUtils;
import net.exogeni.orca.util.persistence.Persistent;

/**
 * @author aydan
 */
class PredecessorState implements Persistable {
    public static final String PredecessorPrefix = "predecessor.";
    
    @Persistent (reference=true)
    private IKernelServiceManagerReservation r;
 
    @Persistent (key = "filter")
    private Properties filter;

    protected PredecessorState() {
    }
    
    public PredecessorState(IKernelServiceManagerReservation r) {
        this(r, null);
    }

    public PredecessorState(IKernelServiceManagerReservation r, Properties filter) {
        if (r == null) {
        	throw new IllegalArgumentException("r cannot be null");
        }
    	this.r = r;
        this.filter = filter;
    }

    public void setProperties(Properties dest) {
        Vector<Properties> vector = PredecessorState.getUnitProperties(r);

        if (filter != null) {
            Iterator<?> iter = filter.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
                String from = (String) entry.getKey();
                String to = (String) entry.getValue();

                if ((to == null) || to.equals("")) {
                    to = from;
                }

                String value = getPropertyString(vector, from);

                if (!value.equals("")) {
                    String cval = dest.getProperty(to);
                    if (cval != null) {
                        value = cval + "," + value;
                    }
                    if (Globals.Log.isDebugEnabled()){
                    	Globals.Log.debug("Setting predecessor property: " + to + "=" + value);
                    }
                    dest.setProperty(to, value);
                }
            }
        } else {
            setProperties(dest, vector); 
        }
    }

    /**
     * Goes through each property list in vector and groups properties with the
     * same name in a comma separated list. Finally, sets the comma separated
     * lists as properties of destination.
     * @param destination
     * @param vector A vector of properties lists
     */
    private void setProperties(Properties destination, Vector vector) {
        HashMap<String, StringBuffer> map = new HashMap<String, StringBuffer>();

        /*
         * Go through the vector of property lists and merge the properties
         */
        for (int i = 0; i < vector.size(); i++) {
            Properties properties = (Properties) vector.get(i);
            Iterator<?> iter = properties.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
                String name = (String) entry.getKey();
                String value = (String) entry.getValue();

                if (value != null) {
                    StringBuffer sb = (StringBuffer) map.get(name);

                    if (sb == null) {
                        sb = new StringBuffer();
                        map.put(name, sb);
                    }

                    if (sb.length() > 0) {
                        sb.append(",");
                    }

                    sb.append(value);
                }
            }
        }

        /*
         * Go through the map of StringBuffers and extract their contents
         */
        Iterator<?> iter = map.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            String name = (String) entry.getKey();
            StringBuffer sb = (StringBuffer) entry.getValue();
            String value = sb.toString();
            String key = PredecessorPrefix + name;
            String cval = destination.getProperty(key);
            if (cval != null) {
                value = cval + "," + value;
            }
            if (Globals.Log.isDebugEnabled()){
            	Globals.Log.debug("Setting predecessor property: " + key + "=" + value);
            }
            destination.setProperty(key, value);
        }
    }

    public static Vector<Properties> getUnitProperties(IServiceManagerReservation reservation) {
        Vector<Properties> result = new Vector<Properties>();

        ResourceSet rset = reservation.getLeasedResources();
        if (rset == null) {
            return result;
        }

        IConcreteSet cset = rset.getResources();
        result = new Vector<Properties>(cset.getUnits() + 1);
        result.add(rset.getResourceProperties());
        if (cset instanceof UnitSet) {
            UnitSet uset = (UnitSet) rset.getResources();
            for (Unit u : uset.getSet()) {
                try {
                    Properties p = PersistenceUtils.save(u);
                    result.add(p);
                } catch (Exception e) {
                    Globals.Log.error("error obtaining properties", e);
                }
            }
        }
        return result;
    }

    /**
     * Concatenates all values of the given property in a comma delimited string
     * @param vector
     * @param name
     * @return
     */
    public static String getPropertyString(Vector<Properties> vector, String name) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < vector.size(); i++) {
            Properties p = vector.get(i);
            String value = p.getProperty(name);

            if (value != null) {
                if (sb.length() > 0) {
                    sb.append(",");
                }

                sb.append(value);
            }
        }

        return sb.toString();
    }

    public IKernelServiceManagerReservation getReservation() {
    	return r;
    }
}
