/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.openflow;

import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IController;
import orca.shirako.api.IReservation;
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
import orca.util.ID;

public class OpenFlowController implements IController, OpenFlowControllerConstants {
    protected IBrokerProxy openflowBrokerProxy;
    protected IBrokerProxy vmBrokerProxy;

    protected IServiceManager sm;
    protected ISlice slice;
    protected String root;
    protected ActorClock clock;

    protected Logger logger = null;
    protected String noopConfigFile = null;

    private boolean initialized = false;

    public class OpenFlowRequest {
        public ID requestId;
        public boolean closed = false;

        public IServiceManagerReservation openflowReservation;
        public IServiceManagerReservation vmReservationRenci;
        public IServiceManagerReservation vmReservationDuke;

        public boolean isAcive() {
            return openflowReservation.isActive() && (vmReservationRenci != null && vmReservationRenci.isActive())
                    && (vmReservationDuke != null && vmReservationDuke.isActive());
        }

        public boolean isTerminal() {
            return openflowReservation.isTerminal() || (vmReservationDuke != null && vmReservationDuke.isTerminal())
                    || (vmReservationDuke != null && vmReservationRenci.isTerminal());
        }
    }

    protected HashMap<ID, OpenFlowRequest> requests;

    public OpenFlowController() {
        requests = new HashMap<ID, OpenFlowRequest>();
    }

    protected void getBrokers() {
        openflowBrokerProxy = sm.getBroker(OpenFlowBrokerName);
        if (openflowBrokerProxy == null) {
            throw new RuntimeException("missing OpenFlow broker proxy");
        }
        vmBrokerProxy = sm.getBroker(VMBrokerName);
        if (vmBrokerProxy == null) {
            throw new RuntimeException("missing vm broker proxy");
        }
    }

    public IBrokerProxy getVMBroker() {
        return vmBrokerProxy;
    }

    public IBrokerProxy getOpenFlowBroker() {
        return openflowBrokerProxy;
    }

    // override initialize to call getBrokers
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

    public ID addRequest(Term term, int vmsDuke, int vmsRenci) {
        OpenFlowRequest request = new OpenFlowRequest();
        request.requestId = new ID();

        request.openflowReservation = getOpenFlowReservation(term);
        if (vmsRenci > 0) {
            request.vmReservationRenci = getVMReservation(term, ResourceTypeVmRenci, vmsRenci);
        }
        if (vmsDuke > 0) {
            request.vmReservationDuke = getVMReservation(term, ResourceTypeVmDuke, vmsDuke);
        }

        /** change parameter */
        // BenRequestIDProperty -> OpenFlowRequestIDProperty
        request.openflowReservation.getResources().getLocalProperties().setProperty(OpenFlowRequestIDProperty,
                request.requestId.toString());
        if (vmsRenci > 0) {
            request.vmReservationRenci.getResources().getLocalProperties().setProperty(OpenFlowRequestIDProperty,
                    request.requestId.toString());
        }
        if (vmsDuke > 0) {
            request.vmReservationDuke.getResources().getLocalProperties().setProperty(OpenFlowRequestIDProperty,
                    request.requestId.toString());
        }

        // set the predecessor relationship and the filter
        Properties filter = new Properties();
        /** Change parameters */
        // UnitProperties.UnitVlanTag -> UnitOfSlice
        filter.setProperty(UnitOfSlice, UnitOfSlice);
        if (vmsRenci > 0) {
            request.vmReservationRenci.addRedeemPredecessor(request.openflowReservation, filter);
        }
        if (vmsDuke > 0) {
            request.vmReservationDuke.addRedeemPredecessor(request.openflowReservation, filter);
        }

        /** Demand resources */
        try {
            sm.demand(request.openflowReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand OpenFlow reservation", e);
        }

        if (vmsRenci > 0) {
            try {
                sm.demand(request.vmReservationRenci);
            } catch (Exception e) {
                throw new RuntimeException("Failed to demand vm reservation (RENCI)", e);
            }
        }

        if (vmsDuke > 0) {
            try {
                sm.demand(request.vmReservationDuke);
            } catch (Exception e) {
                throw new RuntimeException("Failed to demand vm reservation (DUKE)", e);
            }
        }
        requests.put(request.requestId, request);
        return request.requestId;
    }

    protected IServiceManagerReservation getOpenFlowReservation(Term term) {
        return getOpenFlowReservation(term, ResourceTypeOpenflow, openflowBrokerProxy);
    }

    protected IServiceManagerReservation getOpenFlowReservation(Term term, ResourceType type, IBrokerProxy proxy) {
        ResourceSet rset = new ResourceSet(1, type);
        rset.getLocalProperties().setProperty(AntConfig.PropertyXmlFile, noopConfigFile);
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance()
                .create(rset, term, slice, proxy);
        r.setRenewable(true);
        return r;
    }

    /**
     * Creates a reservation for virtual machines
     * 
     * @param term
     *            term
     * @param type
     *            resource type (where to get the virtual machines from)
     * @param units
     *            number of virtual machines
     * @return
     */
    protected IServiceManagerReservation getVMReservation(Term term, ResourceType type, int units) {
        ResourceSet rset = new ResourceSet(units, type);
        IServiceManagerReservation r = (IServiceManagerReservation) ServiceManagerReservationFactory.getInstance()
                .create(rset, term, slice, vmBrokerProxy);
        rset.getLocalProperties().setProperty(AntConfig.PropertyXmlFile, noopConfigFile);
        r.setRenewable(true);
        return r;
    }

    public OpenFlowRequest[] getRequests() {
        OpenFlowRequest[] result = new OpenFlowRequest[requests.size()];
        requests.values().toArray(result);
        return result;
    }

    public OpenFlowRequest getRequest(ID id) {
        return requests.get(id);
    }

    public ISlice getSlice() {
        return slice;
    }

    public void setSlice(ISlice slice) {
        this.slice = slice;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setActor(IActor actor) {
        this.sm = (IServiceManager) actor;
    }

    public IActor getActor() {
        return sm;
    }

    public void tick(long cycle) {
    }

    public void reset(Properties properties) throws Exception {
    }

    public Properties save() throws Exception {
        Properties p = new Properties();
        save(p);

        return p;
    }

    public void save(Properties properties) throws Exception {
    }

    protected void close(IReservation r) {
        try {
            if (r != null) {
                sm.close(r);
            }
        } catch (Exception e) {
        }
    }

    public void close(ID id) {
        OpenFlowRequest req = getRequest(id);

        if (req == null) {
            return;
        }

        req.closed = true;

        if (req.vmReservationDuke != null) {
            close(req.vmReservationDuke);
        }
        if (req.vmReservationRenci != null) {
            close(req.vmReservationRenci);
        }
        close(req.openflowReservation);
    }

}
