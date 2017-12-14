/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.openflow;

import orca.controllers.openflow.OpenFlowController.OpenFlowRequest;
import orca.manage.extensions.api.ManageExtensionsApiConstants;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.extensions.api.proxies.ProxyProtocolDescriptor;
import orca.manage.extensions.standard.Converter;
import orca.manage.extensions.standard.beans.ProxyMng;
import orca.manage.extensions.standard.beans.ResultProxyMng;
import orca.manage.extensions.standard.controllers.ControllerManagerObject;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.util.ID;

public class OpenFlowControllerManagerObject extends ControllerManagerObject {
    public OpenFlowControllerManagerObject(OpenFlowController controller, IActor actor) {
        super(controller, actor);
    }

    @Override
    protected void registerProtocols() {
        ProxyProtocolDescriptor p = new ProxyProtocolDescriptor(ManageExtensionsApiConstants.ProtocolLocal,
                LocalOpenFlowControllerManagementProxy.class.getCanonicalName());
        proxies = new ProxyProtocolDescriptor[] { p };
    }

    public ResultStringMng addRequest(String start, String end, int vmsDuke, int vmsRenci, AuthToken caller) {
        ResultStringMng result = new ResultStringMng();
        result.setStatus(new ResultMng());

        if ((start == null) || (end == null) || vmsDuke < 0 || vmsRenci < 0 || (vmsDuke + vmsRenci == 0)) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                Term term = Converter.getTerm(start, end);
                ID id = ((OpenFlowController) controller).addRequest(term, vmsDuke, vmsRenci);
                result.setResult(id.toString());
            } catch (Exception e) {
                logger.error("addRequest", e);
                result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result.getStatus(), e);
            }
        }
        return result;
    }

    // Change class name: BenRequestMng -> OpenFlowRequestMng
    public class OpenFlowRequestMng {
        public String id;
        public String start;
        public String end;
        public String ofSlice;
        public String ridDuke;
        public String ridRenci;
        public int vmsduke;
        public int vmsrenci;
        public boolean closed;

        /**
         * @return the id
         */
        public String getId() {
            return this.id;
        }

        /**
         * @param id
         *            the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * R
         * 
         * @return the start
         */
        public String getStart() {
            return this.start;
        }

        /**
         * @param start
         *            the start to set
         */
        public void setStart(String start) {
            this.start = start;
        }

        /**
         * @return the end
         */
        public String getEnd() {
            return this.end;
        }

        /**
         * @param end
         *            the end to set
         */
        public void setEnd(String end) {
            this.end = end;
        }

        /**
         * @return the OpenFlow Slice
         */
        /** Rename method: getVlanTag -> getOpenFlowSlice */
        public String getOfSlice() {
            return this.ofSlice;
        }

        /**
         * @param ofSlice
         *            the OpenFlow Slice to set
         */
        /** Rename method: setVlanTag -> setOpenFlowSlice */
        public void setOfSlice(String ofSlice) {
            this.ofSlice = ofSlice;
        }

        /**
         * @return the ridDuke
         */
        public String getRidDuke() {
            return this.ridDuke;
        }

        /**
         * @param ridDuke
         *            the ridDuke to set
         */
        public void setRidDuke(String ridDuke) {
            this.ridDuke = ridDuke;
        }

        /**
         * @return the ridRenci
         */
        public String getRidRenci() {
            return this.ridRenci;
        }

        /**
         * @param ridRenci
         *            the ridRenci to set
         */
        public void setRidRenci(String ridRenci) {
            this.ridRenci = ridRenci;
        }

        /**
         * @return the vmsduke
         */
        public int getVmsDuke() {
            return this.vmsduke;
        }

        /**
         * @param vmsduke
         *            the vmsduke to set
         */
        public void setVmsDuke(int vmsduke) {
            this.vmsduke = vmsduke;
        }

        /**
         * @return the vmsrenci
         */
        public int getVmsRenci() {
            return this.vmsrenci;
        }

        /**
         * @param vmsrenci
         *            the vmsrenci to set
         */
        public void setVmsRenci(int vmsrenci) {
            this.vmsrenci = vmsrenci;
        }

        public boolean getClosed() {
            return closed;
        }

    }

    /** Change class name */
    // ResultBenRequestMng -> ResultOpenFlowRequestMng
    public class ResultOpenFlowRequestMng {
        public ResultMng status;
        public OpenFlowRequestMng[] result;

        /**
         * @return the status
         */
        public ResultMng getStatus() {
            return this.status;
        }

        /**
         * @param status
         *            the status to set
         */
        public void setStatus(ResultMng status) {
            this.status = status;
        }

        /**
         * @return the result
         */
        public OpenFlowRequestMng[] getResult() {
            return this.result;
        }

        /**
         * @param result
         *            the result to set
         */
        public void setResult(OpenFlowRequestMng[] result) {
            this.result = result;
        }

    }

    /** Change parameter: BenRequest -> OpenFlowRequest */
    public OpenFlowRequestMng fill(OpenFlowRequest r) {
        OpenFlowRequestMng result = new OpenFlowRequestMng();

        result.id = r.requestId.toString();

        /** vlanReservation -> openflowReservation */
        result.start = r.openflowReservation.getTerm().getStartTime().toString();
        result.end = r.openflowReservation.getTerm().getEndTime().toString();
        try {
            ResourceSet set = r.openflowReservation.getLeasedResources();
            UnitSet uset = (UnitSet) set.getResources();
            Unit u = uset.getSet().iterator().next();
            result.ofSlice = u.getProperty(OpenFlowControllerConstants.UnitOfSlice); // Use UnitOfSlice instead of Vlan
        } catch (Exception e) {
        }

        if (r.vmReservationDuke != null) {
            result.ridDuke = r.vmReservationDuke.getReservationID().toString();
            result.vmsduke = r.vmReservationDuke.getUnits();
        }
        if (r.vmReservationRenci != null) {
            result.ridRenci = r.vmReservationRenci.getReservationID().toString();
            result.vmsrenci = r.vmReservationRenci.getUnits();
        }

        result.closed = r.closed;
        return result;
    }

    public ResultOpenFlowRequestMng getRequests(AuthToken caller) {
        ResultOpenFlowRequestMng result = new ResultOpenFlowRequestMng();
        result.setStatus(new ResultMng());
        try {
            OpenFlowRequest[] reqs = ((OpenFlowController) controller).getRequests();
            OpenFlowRequestMng[] arr = new OpenFlowRequestMng[reqs.length];
            for (int i = 0; i < reqs.length; i++) {
                arr[i] = fill(reqs[i]);
            }
            result.setResult(arr);
        } catch (Exception e) {
            logger.error("getRequests", e);
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
            setExceptionDetails(result.getStatus(), e);
        }

        return result;
    }

    public ResultOpenFlowRequestMng getRequests(String id, AuthToken caller) {
        ResultOpenFlowRequestMng result = new ResultOpenFlowRequestMng();
        result.setStatus(new ResultMng());
        if (id == null) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                OpenFlowRequest req = ((OpenFlowController) controller).getRequest(new ID(id));
                if (req != null) {
                    OpenFlowRequestMng mng = fill(req);
                    result.setResult(new OpenFlowRequestMng[] { mng });
                }
            } catch (Exception e) {
                logger.error("getRequest", e);
                result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result.getStatus(), e);
            }
        }
        return result;
    }

    public ResultProxyMng getVMBroker(AuthToken caller) {
        ResultProxyMng result = new ResultProxyMng();
        result.setStatus(new ResultMng());

        try {
            IBrokerProxy proxy = ((OpenFlowController) controller).getVMBroker();
            result.setResult(new ProxyMng[] { Converter.fill(proxy) });
        } catch (Exception e) {
            logger.error("getVMBroker", e);
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
            setExceptionDetails(result.getStatus(), e);
        }

        return result;
    }

    public ResultProxyMng getOpenFlowBroker(AuthToken caller) {
        ResultProxyMng result = new ResultProxyMng();
        result.setStatus(new ResultMng());

        try {
            IBrokerProxy proxy = ((OpenFlowController) controller).getOpenFlowBroker();
            result.setResult(new ProxyMng[] { Converter.fill(proxy) });
        } catch (Exception e) {
            logger.error("getOpenFlowBroker", e);
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
            setExceptionDetails(result.getStatus(), e);
        }

        return result;
    }

    public ResultMng closeRequest(String id, AuthToken caller) {
        ResultMng result = new ResultMng();
        if (id == null || caller == null) {
            result.setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                ((OpenFlowController) controller).close(new ID(id));
            } catch (Exception e) {
                logger.error("closeRequest", e);
                result.setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result, e);
            }
        }
        return result;
    }
}
