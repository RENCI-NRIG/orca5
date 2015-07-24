package orca.controllers.ben.nlr;

import java.util.HashMap;
import java.util.Properties;

import orca.controllers.ben.BenController;
import orca.controllers.ben.control.BenNdlControl;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.meta.UnitProperties;
import orca.shirako.time.Term;
import orca.util.ID;

public class BenNlrController extends BenController {
    public class BenNlrRequest {
        public ID requestId;
        public IServiceManagerReservation nlrReservation;
        public IServiceManagerReservation benReservation;
        public IServiceManagerReservation dukeReservation;
        public IServiceManagerReservation vmReservationDuke;
        public IServiceManagerReservation vmReservationUnc;
        public IServiceManagerReservation vmReservationRenci;
        public boolean closed = false;

        public boolean isActive() {
            return nlrReservation.isActive() && benReservation.isActive() && dukeReservation.isActive() && vmReservationDuke.isActive() && vmReservationUnc.isActive() && vmReservationRenci.isActive();
        }

        public boolean isTerminal() {
            return nlrReservation.isTerminal() && benReservation.isTerminal() && dukeReservation.isTerminal() && vmReservationDuke.isTerminal() && vmReservationUnc.isTerminal() && vmReservationRenci.isTerminal();
        }

        public boolean isClosed() {
            return nlrReservation.isClosed() && benReservation.isClosed() && dukeReservation.isClosed() && vmReservationDuke.isClosed() && vmReservationUnc.isClosed() && vmReservationRenci.isClosed();
        }

        public boolean hasAtLeastOneTerminal() {
            return nlrReservation.isTerminal() || benReservation.isTerminal() || dukeReservation.isTerminal() || vmReservationDuke.isTerminal() || vmReservationUnc.isTerminal() || vmReservationRenci.isTerminal();
        }

        public boolean hasAtLeastOneFailed() {
            return nlrReservation.isFailed() || benReservation.isFailed() || dukeReservation.isFailed() || vmReservationDuke.isFailed() || vmReservationUnc.isFailed() || vmReservationRenci.isFailed();
        }
    }

    protected HashMap<ID, BenNlrRequest> requests;

    public BenNlrController() {
        requests = new HashMap<ID, BenNlrRequest>();
    }

    /**
     * Creates a reservation for an NLR VLAN
     * @param term
     * @return
     */
    public ID addRequest(Term term, int vmsRenci, int vmsDuke, int vmsUnc, String benNdl) {
        BenNlrRequest request = new BenNlrRequest();
        request.requestId = new ID();

        // NLR VLAN
        request.nlrReservation = getVlanReservation(term, ResourceTypeNlrVlan, vlanBrokerProxy);
        request.nlrReservation.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());
        // BEN VLAN
        request.benReservation = getBenVlanReservation(term);
        request.benReservation.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());
        request.benReservation.getResources().getConfigurationProperties().setProperty(BenNdlControl.PropertyRequestNdl, benNdl);
        // DUKE VLAN
        request.dukeReservation = getVlanReservation(term, ResourceTypeDukeVlan, vlanBrokerProxy);
        request.dukeReservation.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());

        // virtual machines
        // RENCI
        request.vmReservationRenci = getVMReservation(term, ResourceTypeVmRenci, vmsRenci);
        request.vmReservationRenci.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());

        // request.vmReservationRenci.getResources().getConfigurationProperties().setProperty(VlanControl.PropertyVlanTag,
        // "100");

        // Duke
        request.vmReservationDuke = getVMReservation(term, ResourceTypeVmDuke, vmsDuke);
        request.vmReservationDuke.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());
        // request.vmReservationDuke.getResources().getConfigurationProperties().setProperty(VlanControl.PropertyVlanTag,
        // "100");

        // UNC
        request.vmReservationUnc = getVMReservation(term, ResourceTypeVmUnc, vmsUnc);
        request.vmReservationUnc.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());
        // request.vmReservationUnc.getResources().getConfigurationProperties().setProperty(VlanControl.PropertyVlanTag,
        // "100");

        // set the predecessor relationship and the filter
        Properties filter = new Properties();
        filter.setProperty(UnitProperties.UnitVlanTag, PropertyNlrTagRenci);

        // ben depends on nlr and needs the nlr vlan tag
        request.benReservation.addRedeemPredecessor(request.nlrReservation, filter);

        filter = new Properties();
        filter.setProperty(UnitProperties.UnitVlanTag, PropertyNlrTagDuke);
        // duke net depends on NLR and needs the NLR vlan tag
        request.dukeReservation.addRedeemPredecessor(request.nlrReservation, filter);

        filter = new Properties();
        filter.setProperty(UnitProperties.UnitVlanTag, UnitProperties.UnitVlanTag);

        // vms at UNC depend on ben vlan and need the ben vlan tag
        request.vmReservationUnc.addRedeemPredecessor(request.benReservation, (Properties) filter.clone());

        // vms at RENCI depend on ben vlan and need the ben vlan tag
        request.vmReservationRenci.addRedeemPredecessor(request.benReservation, (Properties) filter.clone());

        // vms at Duke depend on DUKE NET and need the DUKE net VLAN tag
        request.vmReservationDuke.addRedeemPredecessor(request.dukeReservation, (Properties) filter.clone());

        try {
            sm.demand(request.nlrReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand NLR reservation", e);
        }

        try {
            sm.demand(request.benReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand BEN reservation", e);
        }

        try {
            sm.demand(request.dukeReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand Duke net reservation", e);
        }

        try {
            sm.demand(request.vmReservationRenci);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vm reservation (RENCI)", e);
        }

        try {
            sm.demand(request.vmReservationDuke);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vm reservation (DUKE)", e);
        }

        try {
            sm.demand(request.vmReservationUnc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vm reservation (UNC)", e);
        }

        requests.put(request.requestId, request);
        return request.requestId;
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
        BenNlrRequest req = getRequest(id);

        if (req == null) {
            return;
        }

        req.closed = true;

        close(req.vmReservationDuke);
        close(req.vmReservationRenci);
        close(req.vmReservationUnc);

        close(req.nlrReservation);
        close(req.benReservation);
        close(req.dukeReservation);
    }

    public BenNlrRequest[] getRequests() {
        BenNlrRequest[] result = new BenNlrRequest[requests.size()];
        requests.values().toArray(result);
        return result;
    }

    public BenNlrRequest getRequest(ID id) {
        return requests.get(id);
    }
}
