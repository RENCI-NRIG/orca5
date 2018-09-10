package net.exogeni.orca.policy.core;

import java.util.HashSet;
import java.util.Properties;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.api.IAuthorityReservation;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.meta.ConfigurationProperties;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;
import net.exogeni.orca.shirako.common.meta.UnitProperties;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitSet;
import net.exogeni.orca.shirako.core.Units;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.plugins.config.ConfigToken;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

public abstract class ResourceControl implements IResourceControl, ResourceProperties,
        UnitProperties, ConfigurationProperties {
    
    public static final String PropertyResourceTypesCount = "resource.types.count";
    public static final String PropertyResourceTypePrefix = "resource.type.";
    public static final String PropertyGuid = "guid";
    public static final String PropertyControlResourceTypes = "resource.types";
    public static final String PropertySubstrateFile = "substrate.file";

    public static String getSubstrateFile(IClientReservation r) throws Exception {
        ResourceSet set = r.getResources();
        // ResourceType rtype = r.getType();
        // note: ignoring term

        // FIXME: nasty side effect!!!
        // CHECKBEFORESHIPPING
        // type = rtype;

        // note: old demo required this as a resource property
        // new setup (gec7) requires it as a local property.
        String substrateFile = PropList.getRequiredProperty(set.getResourceProperties(),
                PropertySubstrateFile);
        if (substrateFile == null) {
            substrateFile = PropList.getRequiredProperty(set.getLocalProperties(),
                    PropertySubstrateFile);
        }

        if (substrateFile == null) {
            throw new ConfigurationException("Missing substrate file property");
        }
        return substrateFile;
    }

    /**
     * The control's unique ID.
     */
    @Persistent(key = PropertyGuid)
    protected ID guid;

    @Persistent(key = "types")
    protected HashSet<ResourceType> types = new HashSet<ResourceType>();

    /***
     * Authority associated with this control
     */
    @NotPersistent
    protected IAuthority authority;
    /**
     * Logger object.
     */
    @NotPersistent
    protected Logger logger;
    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized;

    public ResourceControl() {
        guid = new ID();
    }

    public void initialize() throws OrcaException {
        if (!initialized) {
            if (authority == null) {
                throw new OrcaException("authority is not set");
            }
            this.logger = authority.getLogger();
            initialized = true;
        }
    }

    public void addType(ResourceType type) {
        types.add(type);
    }

    public void removeType(ResourceType type) {
        types.remove(type);
    }

    public void donate(IClientReservation r) throws Exception {
    }

    public void donate(ResourceSet set) throws Exception {
    }

    public void available(ResourceSet rset) throws Exception {
        // not supported
    }

    public int unavailable(ResourceSet rset) throws Exception {
        return -1;
    }

    public void eject(ResourceSet rset) throws Exception {
        // not supported
    }

    public void failed(ResourceSet set) throws Exception {
        // not supported
    }

    public void recovered(ResourceSet set) throws Exception {
        // not supported
    }

    public void freed(ResourceSet set) throws Exception {
        UnitSet group = (UnitSet) set.getResources();
        if (group == null) {
            throw new Exception("Missing concrete set");
        }
        free(group.getSet());
    }

    public void release(ResourceSet resources) throws Exception {
        UnitSet group = (UnitSet) resources.getResources();
        if (group == null) {
            throw new Exception("Missing concrete set");
        }
        free(group.getSet());
    }

    public ResourceSet correctDeficit(IAuthorityReservation reservation) throws Exception {
        return assign(reservation);
    }

    public void configurationComplete(String action, ConfigToken token, Properties outProperties) {
    }

    public void close(IReservation reservation) {
    }
    
    public void recoveryStarting() {
        // no-op
    }
    
    public void recoveryEnded() {
    }

    protected abstract void free(Units set) throws Exception;

    protected void fail(Unit u, String message) {
        fail(u, message, null);
    }

    protected void fail(Unit u, String message, Exception e) {
        if (e != null) {
            logger.error(message, e);
        } else {
            logger.error(message);
        }
        u.fail(message, e);
    }

    public void configure(Properties p) throws Exception {
        String temp = p.getProperty(PropertyControlResourceTypes);
        if (temp != null) {
            types.clear();
            temp = temp.trim();
            String[] names = temp.split(",");
            for (int i = 0; i < names.length; i++) {
                addType(new ResourceType(names[i]));
            }
        }
    }

    public ResourceType[] getTypes() {
        return types.toArray(new ResourceType[types.size()]);
    }

    public void registerType(ResourceType type) {
        types.add(type);
    }

    public void setActor(IActor actor) {
        this.authority = (IAuthority) actor;
    }

    public ID getGuid() {
        return guid;
    }
}
