package orca.controllers.ben;

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IController;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ResourceType;
import orca.shirako.container.Globals;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;

import org.apache.log4j.Logger;

public abstract class BenController implements IController, BenConstants {

    protected IServiceManager sm;
    protected ISlice slice;
    protected String root;
    protected String benRoot;
    protected ActorClock clock;

    protected Logger logger = null;
    protected String noopConfigFile = null;
    private boolean initialized = false;

    protected IBrokerProxy vmBrokerProxy;
    protected IBrokerProxy vlanBrokerProxy;

    protected void getBrokers() {
        vmBrokerProxy = sm.getBroker(VMBrokerName);
        if (vmBrokerProxy == null) {
            throw new RuntimeException("missing vm broker proxy");
        }
        vlanBrokerProxy = sm.getBroker(VlanBrokerName);
        if (vlanBrokerProxy == null) {
            throw new RuntimeException("missing vlan broker proxy");
        }
    }

    public void initialize() throws Exception {
        if (!initialized) {
            if (sm == null) {
                throw new Exception("Missing actor");
            }

            if (slice == null) {
                throw new Exception("Missing slice");
            }

            clock = sm.getActorClock();
            logger = sm.getLogger();
            root = Globals.getContainer().getPackageRootFolder(MyPackageId);

            getBrokers();

            noopConfigFile = Globals.LocalRootDirectory + "/handlers/common/noop.xml";
            initialized = true;
        }
    }

    public void tick(long cycle) {
    }

    /**
     * Creates a reservation for a BEN VLAN
     * @param term
     * @return
     */
    protected IServiceManagerReservation getBenVlanReservation(Term term) {
        return getVlanReservation(term, ResourceTypeBenVlan, vlanBrokerProxy);
    }

    /**
     * Creates a vlan reservation.
     * @param term
     * @param type
     * @return
     */
    protected IServiceManagerReservation getVlanReservation(Term term, ResourceType type, IBrokerProxy proxy) {
        ResourceSet rset = new ResourceSet(1, type);
        rset.getLocalProperties().setProperty(AntConfig.PropertyXmlFile, noopConfigFile);
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance().create(rset, term, slice, proxy);
        r.setRenewable(true);
        return r;
    }

    /**
     * Creates a reservation for virtual machines
     * @param term term
     * @param type resource type (where to get the virtual machines from)
     * @param units number of virtual machines
     * @return
     */
    protected IServiceManagerReservation getVMReservation(Term term, ResourceType type, int units) {
        ResourceSet rset = new ResourceSet(units, type);
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance().create(rset, term, slice, vmBrokerProxy);
        rset.getLocalProperties().setProperty(AntConfig.PropertyXmlFile, noopConfigFile);
        r.setRenewable(true);
        return r;
    }

    public void setActor(IActor sm) {
        this.sm = (IServiceManager) sm;
    }

    public void setSlice(ISlice slice) {
        this.slice = slice;
    }

    public IActor getActor() {
        return sm;
    }

    public ISlice getSlice() {
        return slice;
    }

    public Logger getLogger() {
        return logger;
    }

    public IBrokerProxy getVMBroker() {
        return vmBrokerProxy;
    }

    public IBrokerProxy getVlanBroker() {
        return vlanBrokerProxy;
    }

    public void reset(Properties p) throws Exception {
    }

    public Properties save() throws Exception {
        Properties p = new Properties();
        save(p);

        return p;
    }

    public void save(Properties p) throws Exception {
    }
}
