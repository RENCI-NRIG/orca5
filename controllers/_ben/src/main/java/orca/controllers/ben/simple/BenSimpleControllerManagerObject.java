/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.ben.simple;

import orca.controllers.ben.BenController;
import orca.controllers.ben.BenControllerManagerObject;
import orca.controllers.ben.simple.BenSimpleController.BenRequest;
import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.extensions.api.ManageExtensionsApiConstants;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.extensions.standard.Converter;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.meta.UnitProperties;
import orca.shirako.time.Term;
import orca.util.ID;

public class BenSimpleControllerManagerObject extends BenControllerManagerObject {
    public BenSimpleControllerManagerObject(BenController controller, IActor actor) {
        super(controller, actor);
    }

    @Override
    protected void registerProtocols() {
        OrcaProxyProtocolDescriptor p = new OrcaProxyProtocolDescriptor(ManageExtensionsApiConstants.ProtocolLocal, LocalBenSimpleControllerManagementProxy.class.getCanonicalName());
        proxies = new OrcaProxyProtocolDescriptor[] { p };
    }

    public ResultStringMng addRequest(String start, String end, int vmsDuke, int vmsRenci, AuthToken caller) {
        ResultStringMng result = new ResultStringMng();
        result.setStatus(new ResultMng());

        if ((start == null) || (end == null) || vmsDuke <= 0 || vmsRenci <= 0) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                Term term = Converter.getTerm(start, end);
                ID id = ((BenSimpleController) controller).addRequest(term, vmsDuke, vmsRenci);
                result.setResult(id.toString());
            } catch (Exception e) {
                logger.error("addRequest", e);
                result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result.getStatus(), e);
            }
        }
        return result;
    }

    public class BenRequestMng {
        public String id;
        public String start;
        public String end;
        public String vlanTag;
        public String ridDuke;
        public String ridRenci;
        public int vmsduke;
        public int vmsrenci;

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
         * @return the vlanTag
         */
        public String getVlanTag() {
            return this.vlanTag;
        }

        /**
         * @param vlanTag the vlanTag to set
         */
        public void setVlanTag(String vlanTag) {
            this.vlanTag = vlanTag;
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
         * @return the vmsduke
         */
        public int getVmsDuke() {
            return this.vmsduke;
        }

        /**
         * @param vmsduke the vmsduke to set
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
         * @param vmsrenci the vmsrenci to set
         */
        public void setVmsRenci(int vmsrenci) {
            this.vmsrenci = vmsrenci;
        }
    }

    public class ResultBenRequestMng {
        public ResultMng status;
        public BenRequestMng[] result;

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
        public BenRequestMng[] getResult() {
            return this.result;
        }

        /**
         * @param result the result to set
         */
        public void setResult(BenRequestMng[] result) {
            this.result = result;
        }

    }

    public BenRequestMng fill(BenRequest r) {
        BenRequestMng result = new BenRequestMng();

        result.id = r.requestId.toString();
        result.start = r.vlanReservation.getTerm().getStartTime().toString();
        result.end = r.vlanReservation.getTerm().getEndTime().toString();
        try {
            ResourceSet set = r.vlanReservation.getLeasedResources();
            UnitSet uset = (UnitSet) set.getResources();
            Unit u = uset.getSet().iterator().next();
            result.vlanTag = u.getProperty(UnitProperties.UnitVlanTag);
        } catch (Exception e) {
        }

        result.ridDuke = r.vmReservationDuke.getReservationID().toString();
        result.ridRenci = r.vmReservationRenci.getReservationID().toString();
        result.vmsduke = r.vmReservationDuke.getUnits();
        result.vmsrenci = r.vmReservationRenci.getUnits();

        return result;
    }

    public ResultBenRequestMng getRequests(AuthToken caller) {
        ResultBenRequestMng result = new ResultBenRequestMng();
        result.setStatus(new ResultMng());
        try {
            BenRequest[] reqs = ((BenSimpleController) controller).getRequests();
            BenRequestMng[] arr = new BenRequestMng[reqs.length];
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

    public ResultBenRequestMng getRequests(String id, AuthToken caller) {
        ResultBenRequestMng result = new ResultBenRequestMng();
        result.setStatus(new ResultMng());
        if (id == null) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                BenRequest req = ((BenSimpleController) controller).getRequest(new ID(id));
                if (req != null) {
                    BenRequestMng mng = fill(req);
                    result.setResult(new BenRequestMng[] { mng });
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
