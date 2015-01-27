package orca.controllers.ben;

import orca.manage.extensions.api.ManageExtensionsApiConstants;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.standard.Converter;
import orca.manage.extensions.standard.beans.ProxyMng;
import orca.manage.extensions.standard.beans.ResultProxyMng;
import orca.manage.extensions.standard.controllers.ControllerManagerObject;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.meta.UnitProperties;

public class BenControllerManagerObject extends ControllerManagerObject {
    public BenControllerManagerObject(BenController controller, IActor actor) {
        super(controller, actor);
    }

    public ResultProxyMng getVMBroker(AuthToken caller) {
        ResultProxyMng result = new ResultProxyMng();
        result.setStatus(new ResultMng());

        try {
            IBrokerProxy proxy = ((BenController) controller).getVMBroker();
            result.setResult(new ProxyMng[] { Converter.fill(proxy) });
        } catch (Exception e) {
            logger.error("getVMBroker", e);
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
            setExceptionDetails(result.getStatus(), e);
        }

        return result;
    }

    public ResultProxyMng getVlanBroker(AuthToken caller) {
        ResultProxyMng result = new ResultProxyMng();
        result.setStatus(new ResultMng());

        try {
            IBrokerProxy proxy = ((BenController) controller).getVlanBroker();
            result.setResult(new ProxyMng[] { Converter.fill(proxy) });
        } catch (Exception e) {
            logger.error("getVlanBroker", e);
            result.getStatus().setCode(ManageExtensionsApiConstants.ErrorInternalError);
            setExceptionDetails(result.getStatus(), e);
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
