/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.ben.interdomain;

import java.util.ArrayList;
import java.util.Iterator;

import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.extensions.api.ManageExtensionsApiConstants;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.extensions.standard.Converter;
import orca.manage.extensions.standard.controllers.ControllerManagerObject;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.meta.UnitProperties;
import orca.shirako.time.Term;
import orca.util.ID;

/**
 *
 * @author anirban
 */
public class InterDomainControllerManagerObject extends ControllerManagerObject {

    public InterDomainControllerManagerObject(InterDomainController controller, IActor actor) {
        super(controller, actor);
    }

    @Override
    protected void registerProtocols() {
        OrcaProxyProtocolDescriptor p = new OrcaProxyProtocolDescriptor(ManageExtensionsApiConstants.ProtocolLocal, LocalInterDomainControllerManagementProxy.class.getCanonicalName());
        proxies = new OrcaProxyProtocolDescriptor[] { p };
    }

    public ResultStringMng addRequest(String ndl, String start, String end, AuthToken caller) {

        ResultStringMng result = new ResultStringMng();
        result.setStatus(new ResultMng());

        if (ndl == null) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                Term term = Converter.getTerm(start, end);
                ID idRequest = ((InterDomainController) controller).addRequest(ndl, term);
                result.setResult(idRequest.toString());
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
                ((InterDomainController) controller).close(new ID(id));
            } catch (Exception e) {
                logger.error("closeRequest", e);
                result.setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result, e);
            }
        }
        return result;
    }



    public class InterDomainRequestMng {

        public String id;
        public boolean closed;
        public String start;
        public String end;

        public ArrayList<String> listResId = new ArrayList<String>(); 
        public ArrayList<String> listVlanTag = new ArrayList<String>();
        public ArrayList<Integer> listVmCounts = new ArrayList<Integer>();

        public boolean isClosed() {
            return closed;
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ArrayList<String> getListResId() {
            return listResId;
        }

        public void setListResId(ArrayList<String> listResId) {
            this.listResId = listResId;
        }

        public ArrayList<String> getListVlanTag() {
            return listVlanTag;
        }

        public void setListVlanTag(ArrayList<String> listVlanTag) {
            this.listVlanTag = listVlanTag;
        }

        public ArrayList<Integer> getListVmCounts() {
            return listVmCounts;
        }

        public void setListVmCounts(ArrayList<Integer> listVmCounts) {
            this.listVmCounts = listVmCounts;
        }
        
        public String getStart() {
            return start;
        }
        
        public String getEnd() {
            return end;
        }
    }

    public class ResultInterDomainRequestMng {
        public ResultMng status;
        public InterDomainRequestMng[] result;


        public ResultMng getStatus() {
            return this.status;
        }


        public void setStatus(ResultMng status) {
            this.status = status;
        }


        public InterDomainRequestMng[] getResult() {
            return this.result;
        }

        
        public void setResult(InterDomainRequestMng[] result) {
            this.result = result;
        }

    }


    public InterDomainRequestMng fill(InterDomainRequest r) {

        InterDomainRequestMng result = new InterDomainRequestMng();

        result.id = r.requestId.toString();
        result.closed = r.closed;

        Iterator<IServiceManagerReservation> it = r.listInterDomainReservations.iterator();
        int i = 0;
        while(it.hasNext()){
            IServiceManagerReservation currRes = (IServiceManagerReservation) it.next();
            result.listResId.add(currRes.getReservationID().toString());

            if (i == 0){
                result.start = currRes.getTerm().getStartTime().toString();
                result.end = currRes.getTerm().getEndTime().toString();
            }
            
            if(getVlanTag(currRes) != null){
                result.listVlanTag.add(getVlanTag(currRes));
            }
            else{
                result.listVlanTag.add("NoVlanTag");
            }

            if(currRes.getUnits() > 0){
                result.listVmCounts.add(new Integer(currRes.getUnits()));
            }
            else{
                result.listVmCounts.add(new Integer(0));
            }
            i++;
        }


        return result;

    }


    public ResultInterDomainRequestMng getRequests(AuthToken caller) {

        ResultInterDomainRequestMng result = new ResultInterDomainRequestMng();
        result.setStatus(new ResultMng());
        try {
            InterDomainRequest[] reqs = ((InterDomainController) controller).getRequests();
            InterDomainRequestMng[] arr = new InterDomainRequestMng[reqs.length];
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

    public ResultInterDomainRequestMng getRequests(String id, AuthToken caller) {

        ResultInterDomainRequestMng result = new ResultInterDomainRequestMng();
        result.setStatus(new ResultMng());
        if (id == null) {
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInvalidArguments);
        } else {
            try {
                InterDomainRequest req = ((InterDomainController) controller).getRequest(new ID(id));
                if (req != null) {
                    InterDomainRequestMng mng = fill(req);
                    result.setResult(new InterDomainRequestMng[] { mng });
                }
            } catch (Exception e) {
                logger.error("getRequest", e);
                result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
                setExceptionDetails(result.getStatus(), e);
            }
        }
        return result;

    }

    protected String getVlanTag(IServiceManagerReservation r) {
        String result = null;
        try {
            ResourceSet set = r.getLeasedResources();
            UnitSet uset = (UnitSet) set.getResources();
            Unit u = uset.getSet().iterator().next();
            result = u.getProperty(UnitProperties.UnitVlanTag);
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

}
