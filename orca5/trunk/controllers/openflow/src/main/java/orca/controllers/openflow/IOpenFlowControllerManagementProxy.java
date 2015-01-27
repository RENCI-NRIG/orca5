/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.openflow;

import orca.controllers.openflow.OpenFlowControllerManagerObject.ResultOpenFlowRequestMng;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.manage.extensions.standard.beans.ResultProxyMng;
import orca.security.AuthToken;

public interface IOpenFlowControllerManagementProxy {
    
    ResultStringMng addRequest(String start, String end, int vmsDuke, int vmsRenci, AuthToken caller) throws Exception;

    ResultOpenFlowRequestMng getRequests(AuthToken caller) throws Exception;

    ResultOpenFlowRequestMng getRequests(String id, AuthToken caller) throws Exception;
    
    ResultMng closeRequest(String id, AuthToken caller) throws Exception;
    
    /**
     * Returns the details of the VM proxy.
     * @return
     * @throws Exception
     */
    ResultProxyMng getVMBroker(AuthToken caller) throws Exception;
    
    /**
     * Returns the details of the OpenFlow proxy. 
     * @return
     * @throws Exception
     */
    ResultProxyMng getOpenFlowBroker(AuthToken caller) throws Exception;

}
