/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.ben.nlr;

import orca.controllers.ben.BenController;
import orca.controllers.ben.BenControllerManagerObject;
import orca.controllers.ben.nlr.BenNlrController.BenNlrRequest;
import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.extensions.api.ManageExtensionsApiConstants;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.extensions.standard.Converter;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.time.Term;
import orca.util.ID;

public class BenNlrControllerManagerObject extends BenControllerManagerObject {
    public BenNlrControllerManagerObject(BenController controller, IActor actor) {
        super(controller, actor);
    }

    @Override
    protected void registerProtocols() {
        OrcaProxyProtocolDescriptor p = new OrcaProxyProtocolDescriptor(ManageExtensionsApiConstants.ProtocolLocal, LocalBenNlrControllerManagementProxy.class.getCanonicalName());
        proxies = new OrcaProxyProtocolDescriptor[] { p };
    }

    public ResultStringMng addRequest(String start, String end, int vmsDuke, int vmsRenci, int vmsUnc, String request, AuthToken caller) {
        ResultStringMng result = new ResultStringMng();
        result.setStatus(new ResultMng());

        if ((start == null) || (end == null) || vmsDuke <= 0 || vmsRenci <= 0 || vmsUnc <= 0 || request == null) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                Term term = Converter.getTerm(start, end);
                ID id = ((BenNlrController) controller).addRequest(term, vmsDuke, vmsRenci, vmsUnc, request);
                result.setResult(id.toString());
            } catch (Exception e) {
                logger.error("addRequest", e);
                result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result.getStatus(), e);
            }
        }
        return result;
    }

    public ResultMng closeRequest(String id, AuthToken caller) {
        ResultMng result = new ResultMng();
        if (id == null || caller == null) {
            result.setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                ((BenNlrController) controller).close(new ID(id));
            } catch (Exception e) {
                logger.error("closeRequest", e);
                result.setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result, e);
            }
        }
        return result;
    }

    public class BenNlrRequestMng {
        /**
         * @return the id
         */
        public String getId() {
            return this.id;
        }

        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the start
         */
        public String getStart() {
            return this.start;
        }

        /**
         * @param start the start to set
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
         * @param end the end to set
         */
        public void setEnd(String end) {
            this.end = end;
        }

        /**
         * @return the vlanTagDuke
         */
        public String getVlanTagDuke() {
            return this.vlanTagDuke;
        }

        /**
         * @param vlanTagDuke the vlanTagDuke to set
         */
        public void setVlanTagDuke(String vlanTagDuke) {
            this.vlanTagDuke = vlanTagDuke;
        }

        /**
         * @return the vlanTagNlr
         */
        public String getVlanTagNlr() {
            return this.vlanTagNlr;
        }

        /**
         * @param vlanTagNlr the vlanTagNlr to set
         */
        public void setVlanTagNlr(String vlanTagNlr) {
            this.vlanTagNlr = vlanTagNlr;
        }

        /**
         * @return the vlanTagBen
         */
        public String getVlanTagBen() {
            return this.vlanTagBen;
        }

        /**
         * @param vlanTagBen the vlanTagBen to set
         */
        public void setVlanTagBen(String vlanTagBen) {
            this.vlanTagBen = vlanTagBen;
        }

        /**
         * @return the ridDuke
         */
        public String getRidDuke() {
            return this.ridDuke;
        }

        /**
         * @param ridDuke the ridDuke to set
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
         * @param ridRenci the ridRenci to set
         */
        public void setRidRenci(String ridRenci) {
            this.ridRenci = ridRenci;
        }

        /**
         * @return the ridUnc
         */
        public String getRidUnc() {
            return this.ridUnc;
        }

        /**
         * @param ridUnc the ridUnc to set
         */
        public void setRidUnc(String ridUnc) {
            this.ridUnc = ridUnc;
        }

        /**
         * @return the ridDukeNet
         */
        public String getRidDukeNet() {
            return this.ridDukeNet;
        }

        /**
         * @param ridDukeNet the ridDukeNet to set
         */
        public void setRidDukeNet(String ridDukeNet) {
            this.ridDukeNet = ridDukeNet;
        }

        /**
         * @return the ridBen
         */
        public String getRidBen() {
            return this.ridBen;
        }

        /**
         * @param ridBen the ridBen to set
         */
        public void setRidBen(String ridBen) {
            this.ridBen = ridBen;
        }

        /**
         * @return the ridNlr
         */
        public String getRidNlr() {
            return this.ridNlr;
        }

        /**
         * @param ridNlr the ridNlr to set
         */
        public void setRidNlr(String ridNlr) {
            this.ridNlr = ridNlr;
        }

        /**
         * @return the vmsduke
         */
        public int getVmsduke() {
            return this.vmsduke;
        }

        /**
         * @param vmsduke the vmsduke to set
         */
        public void setVmsduke(int vmsduke) {
            this.vmsduke = vmsduke;
        }

        /**
         * @return the vmsrenci
         */
        public int getVmsrenci() {
            return this.vmsrenci;
        }

        /**
         * @param vmsrenci the vmsrenci to set
         */
        public void setVmsrenci(int vmsrenci) {
            this.vmsrenci = vmsrenci;
        }

        /**
         * @return the vmsunc
         */
        public int getVmsunc() {
            return this.vmsunc;
        }

        /**
         * @param vmsunc the vmsunc to set
         */
        public void setVmsunc(int vmsunc) {
            this.vmsunc = vmsunc;
        }
        public String id;
        public String start;
        public String end;
        public String vlanTagDuke;
        public String vlanTagNlr;
        public String vlanTagBen;
        public String ridDuke;
        public String ridRenci;
        public String ridUnc;
        public String ridDukeNet;
        public String ridBen;
        public String ridNlr;

        public int vmsduke;
        public int vmsrenci;
        public int vmsunc;

        public boolean closed;

        public boolean getClosed() {
            return closed;
        }
    }

    public class ResultBenNlrRequestMng {
        public ResultMng status;
        public BenNlrRequestMng[] result;

        /**
         * @return the status
         */
        public ResultMng getStatus() {
            return this.status;
        }

        /**
         * @param status the status to set
         */
        public void setStatus(ResultMng status) {
            this.status = status;
        }

        /**
         * @return the result
         */
        public BenNlrRequestMng[] getResult() {
            return this.result;
        }

        /**
         * @param result the result to set
         */
        public void setResult(BenNlrRequestMng[] result) {
            this.result = result;
        }

    }

    public BenNlrRequestMng fill(BenNlrRequest r) {
        BenNlrRequestMng result = new BenNlrRequestMng();

        result.id = r.requestId.toString();
        result.start = r.benReservation.getTerm().getStartTime().toString();
        result.end = r.benReservation.getTerm().getEndTime().toString();

        result.ridDuke = r.vmReservationDuke.getReservationID().toString();
        result.ridRenci = r.vmReservationRenci.getReservationID().toString();
        result.ridUnc = r.vmReservationUnc.getReservationID().toString();

        result.ridNlr = r.nlrReservation.getReservationID().toString();
        result.ridDukeNet = r.dukeReservation.getReservationID().toString();
        result.ridBen = r.benReservation.getReservationID().toString();

        result.vlanTagBen = getVlanTag(r.benReservation);
        result.vlanTagDuke = getVlanTag(r.dukeReservation);
        result.vlanTagNlr = getVlanTag(r.nlrReservation);

        result.vmsduke = r.vmReservationDuke.getUnits();
        result.vmsrenci = r.vmReservationRenci.getUnits();
        result.vmsunc = r.vmReservationUnc.getUnits();

        result.closed = r.closed;

        return result;
    }

    public ResultBenNlrRequestMng getRequests(AuthToken caller) {
        ResultBenNlrRequestMng result = new ResultBenNlrRequestMng();
        result.setStatus(new ResultMng());
        try {
            BenNlrRequest[] reqs = ((BenNlrController) controller).getRequests();
            BenNlrRequestMng[] arr = new BenNlrRequestMng[reqs.length];
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

    public ResultBenNlrRequestMng getRequests(String id, AuthToken caller) {
        ResultBenNlrRequestMng result = new ResultBenNlrRequestMng();
        result.setStatus(new ResultMng());
        if (id == null) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                BenNlrRequest req = ((BenNlrController) controller).getRequest(new ID(id));
                if (req != null) {
                    BenNlrRequestMng mng = fill(req);
                    result.setResult(new BenNlrRequestMng[] { mng });
                }
            } catch (Exception e) {
                logger.error("getRequest", e);
                result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result.getStatus(), e);
            }
        }
        return result;
    }

}
