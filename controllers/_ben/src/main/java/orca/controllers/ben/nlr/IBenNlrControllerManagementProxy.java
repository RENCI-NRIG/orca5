/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.controllers.ben.nlr;

import orca.controllers.ben.IBenControllerManagementProxy;
import orca.controllers.ben.nlr.BenNlrControllerManagerObject.ResultBenNlrRequestMng;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.security.AuthToken;


public interface IBenNlrControllerManagementProxy  extends IBenControllerManagementProxy {
    /**
     * Adds a new request
     * @param term term for the lease
     * @param vmsDuke number of vms from duke
     * @param vmsRenci number of vms from renci
     * @return
     * @throws Exception
     */
    ResultStringMng addRequest(String start, String end, int vmsDuke, int vmsRenci, int vmsUnc, String ndl, AuthToken caller) throws Exception;

    /**
     * Obtains a list of all requests.
     * @param caller
     * @return
     * @throws Exception
     */
    ResultBenNlrRequestMng getRequests(AuthToken caller) throws Exception;
    
    /**
     * Obtains the specified request.
     * @param id
     * @param caller
     * @return
     * @throws Exception
     */
    ResultBenNlrRequestMng getRequests(String id, AuthToken caller) throws Exception;
    
    /**
     * Closes the specified request.
     * @param id
     * @param caller
     * @return
     * @throws Exception
     */
    ResultMng closeRequest(String id, AuthToken caller) throws Exception;
}