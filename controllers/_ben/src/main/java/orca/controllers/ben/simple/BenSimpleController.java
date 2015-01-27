/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.ben.simple;

import java.util.HashMap;
import java.util.Properties;

import orca.controllers.ben.BenController;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.meta.UnitProperties;
import orca.shirako.time.Term;
import orca.util.ID;

public class BenSimpleController extends BenController implements BenSimpleControllerConstants
{
    public class BenRequest{
        public ID requestId;
        public IServiceManagerReservation vlanReservation;
        public IServiceManagerReservation vmReservationRenci;
        public IServiceManagerReservation vmReservationDuke;
        
        
        public boolean isAcive() {
            return vlanReservation.isActive() && vmReservationRenci.isActive() &&
            vmReservationDuke.isActive();
        }
        
        public boolean isTerminal() {
            return vlanReservation.isTerminal() ||
            vmReservationDuke.isTerminal() ||
            vmReservationRenci.isTerminal();
        }
    }
      

    protected HashMap<ID, BenRequest> requests;
    
    public BenSimpleController()
    {        
        requests = new HashMap<ID, BenRequest>(); 
    }

    public ID addRequest(Term term, int vmsDuke, int vmsRenci) 
    {
        BenRequest request = new BenRequest();
        request.requestId = new ID();
        request.vlanReservation = getBenVlanReservation(term);
        request.vlanReservation.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());

        request.vmReservationRenci = getVMReservation(term, ResourceTypeVmRenci, vmsRenci);
        request.vmReservationRenci.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());
        
        request.vmReservationDuke = getVMReservation(term, ResourceTypeVmDuke, vmsDuke);
        request.vmReservationDuke.getResources().getLocalProperties().setProperty(BenRequestIDProperty, request.requestId.toString());

        // set the predecessor relationship and the filter
        Properties filter = new Properties();
        filter.setProperty(UnitProperties.UnitVlanTag, UnitProperties.UnitVlanTag);
        request.vmReservationRenci.addRedeemPredecessor(request.vlanReservation, filter);
        request.vmReservationDuke.addRedeemPredecessor(request.vlanReservation, filter);

        try {
            sm.demand(request.vlanReservation);
        } catch (Exception e) {
            throw new RuntimeException("Failed to demand vlan reservation", e);
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
        
        requests.put(request.requestId, request);
        return request.requestId;
    }

    public BenRequest[] getRequests()
    {
        BenRequest[] result = new BenRequest[requests.size()];
        requests.values().toArray(result);
        return result;
    }
    
    public BenRequest getRequest(ID id)
    {
        return requests.get(id);
    }
}
